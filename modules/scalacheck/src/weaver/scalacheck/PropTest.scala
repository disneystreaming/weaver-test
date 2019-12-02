package weaver.scalacheck

import cats.effect.Sync
import cats.implicits._

import org.scalacheck.Prop
import org.scalacheck.Test._
import org.scalacheck.Gen
import org.scalacheck.util.FreqMap
import cats.effect.concurrent.Ref
import cats.Parallel

object PropTest {

  def check[F[_]: Sync: Parallel](params: Parameters, prop: Prop): F[Result] = {
    import params._

    val iterations = math.ceil(minSuccessfulTests / workers.toDouble)
    val sizeStep   = (maxSize - minSize) / (iterations * workers)
    val threshold  = params.minSuccessfulTests * params.maxDiscardRatio

    val genPrms = Gen.Parameters.default
      .withLegacyShrinking(params.useLegacyShrinking)
      .withInitialSeed(params.initialSeed)

    def workerFun(globalStop: Ref[F, Boolean])(workerIdx: Int): F[Result] = {
      val finalStateF = Sync[F].iterateUntilM(PropTestState()) { state =>
        val size    = minSize.toDouble + (sizeStep * (workerIdx + (workers * (state.passed + state.discarded))))
        val propRes = prop(genPrms.withSize(size.round.toInt))
        val newFreqMap =
          if (propRes.collected.isEmpty) state.freqMap
          else state.freqMap + propRes.collected
        globalStop.get.map(propRes.status -> _).map {
          case (_, true) => state.copy(stop = true)
          case (Prop.Undecided, false) if (state.discarded + 1 > threshold) =>
            state.copy(discarded = state.discarded + 1,
                       status = Exhausted.some,
                       freqMap = newFreqMap)
          case (Prop.Undecided, false) =>
            state.copy(discarded = state.discarded + 1, freqMap = newFreqMap)
          case (Prop.True, false) =>
            state.copy(passed = state.passed + 1, freqMap = newFreqMap)
          case (Prop.Proof, false) =>
            state.copy(passed = state.passed + 1,
                       status = Proved(propRes.args).some,
                       freqMap = newFreqMap,
                       stop = true)
          case (Prop.False, false) =>
            state.copy(status = Failed(propRes.args, propRes.labels).some,
                       stop = true)
          case (Prop.Exception(e), false) =>
            val status = PropException(propRes.args, e, propRes.labels)
            state.copy(status = status.some, freqMap = newFreqMap, stop = true)
        }
      } { state =>
        state.stop ||
        state.status.isDefined ||
        state.discarded > threshold ||
        state.passed > iterations
      }
      for {
        finalState <- finalStateF
        _          <- globalStop.update(current => finalState.stop || current)
      } yield {
        val status = finalState.status match {
          case Some(st)                                 => st
          case None if finalState.discarded > threshold => Exhausted
          case None                                     => Passed
        }
        Result(status,
               finalState.passed,
               finalState.discarded,
               finalState.freqMap)
      }
    }

    def mergeResults(r1: Result, r2: Result): Result = {
      val Result(st1, s1, d1, fm1, _) = r1
      val Result(st2, s2, d2, fm2, _) = r2
      if (st1 != Passed && st1 != Exhausted)
        Result(st1, s1 + s2, d1 + d2, fm1 ++ fm2, 0)
      else if (st2 != Passed && st2 != Exhausted)
        Result(st2, s1 + s2, d1 + d2, fm1 ++ fm2, 0)
      else {
        if (s1 + s2 >= minSuccessfulTests && maxDiscardRatio * (s1 + s2) >= (d1 + d2))
          Result(Passed, s1 + s2, d1 + d2, fm1 ++ fm2, 0)
        else
          Result(Exhausted, s1 + s2, d1 + d2, fm1 ++ fm2, 0)
      }
    }

    val zeroRes = Result(Passed, 0, 0, FreqMap.empty[Set[Any]], 0)
    for {
      globalStop <- Ref[F].of(false)
      results    <- List.range(0, workers).parTraverse(workerFun(globalStop))
    } yield results.foldLeft(zeroRes)(mergeResults)
  }

  private final case class PropTestState(
      passed: Int = 0,
      discarded: Int = 0,
      freqMap: FreqMap[Set[Any]] = FreqMap.empty,
      stop: Boolean = false,
      status: Option[org.scalacheck.Test.Status] = None)

}
