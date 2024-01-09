package weaver
package scalacheck

case class CheckConfig(
    minimumSuccessful: Int,
    maximumDiscardRatio: Int,
    maximumGeneratorSize: Int,
    perPropertyParallelism: Int,
    initialSeed: Option[Long]
) {
  assert(maximumDiscardRatio >= 0)
  assert(maximumDiscardRatio <= 100)
  assert(minimumSuccessful > 0)

  def maximumDiscarded = minimumSuccessful * maximumDiscardRatio / 100
}

object CheckConfig {
  def default: CheckConfig = CheckConfig(
    minimumSuccessful = 80,
    maximumDiscardRatio = 5,
    maximumGeneratorSize = 100,
    perPropertyParallelism = 10,
    initialSeed = None
  )
}
