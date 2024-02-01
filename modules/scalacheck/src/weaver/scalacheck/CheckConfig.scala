package weaver
package scalacheck

import org.scalacheck.rng.Seed

final class CheckConfig(
    val minimumSuccessful: Int,
    val maximumDiscardRatio: Int,
    val maximumGeneratorSize: Int,
    val perPropertyParallelism: Int,
    val initialSeed: Option[Seed]) extends Product with Serializable {
  assert(maximumDiscardRatio >= 0)
  assert(maximumDiscardRatio <= 100)
  assert(minimumSuccessful > 0)

  def maximumDiscarded = minimumSuccessful * maximumDiscardRatio / 100

  def withMinimumSuccessful(minimumSuccessful: Int) = new CheckConfig(
    minimumSuccessful = minimumSuccessful,
    maximumDiscardRatio = this.maximumDiscardRatio,
    maximumGeneratorSize = this.maximumGeneratorSize,
    perPropertyParallelism = this.perPropertyParallelism,
    initialSeed = this.initialSeed
  )

  def withMaximumDiscardRatio(maximumDiscardRatio: Int) = new CheckConfig(
    minimumSuccessful = this.minimumSuccessful,
    maximumDiscardRatio = maximumDiscardRatio,
    maximumGeneratorSize = this.maximumGeneratorSize,
    perPropertyParallelism = this.perPropertyParallelism,
    initialSeed = this.initialSeed
  )

  def withMaximumGeneratorSize(maximumGeneratorSize: Int) = new CheckConfig(
    minimumSuccessful = this.minimumSuccessful,
    maximumDiscardRatio = this.maximumDiscardRatio,
    maximumGeneratorSize = maximumGeneratorSize,
    perPropertyParallelism = this.perPropertyParallelism,
    initialSeed = this.initialSeed
  )

  def withPerPropertyParallelism(perPropertyParallelism: Int) = new CheckConfig(
    minimumSuccessful = this.minimumSuccessful,
    maximumDiscardRatio = this.maximumDiscardRatio,
    maximumGeneratorSize = this.maximumGeneratorSize,
    perPropertyParallelism = perPropertyParallelism,
    initialSeed = this.initialSeed
  )

  def withInitialSeed(initialSeed: Option[Seed]) = new CheckConfig(
    minimumSuccessful = this.minimumSuccessful,
    maximumDiscardRatio = this.maximumDiscardRatio,
    maximumGeneratorSize = this.maximumGeneratorSize,
    perPropertyParallelism = this.perPropertyParallelism,
    initialSeed = initialSeed
  )

  override def toString: String = {
    val b = new StringBuilder("CheckConfig(")
    b.append(String.valueOf(this.minimumSuccessful))
    b.append(", ")
    b.append(String.valueOf(this.maximumDiscardRatio))
    b.append(", ")
    b.append(String.valueOf(this.maximumGeneratorSize))
    b.append(", ")
    b.append(String.valueOf(this.perPropertyParallelism))
    b.append(", ")
    b.append(String.valueOf(this.initialSeed))
    b.append(")")
    b.toString()
  }

  override def canEqual(obj: Any): Boolean =
    obj != null && obj.isInstanceOf[CheckConfig]

  override def equals(obj: Any): Boolean = canEqual(obj) && {
    val other = obj.asInstanceOf[CheckConfig]
    this.minimumSuccessful == other.minimumSuccessful && this.maximumDiscardRatio == other.maximumDiscardRatio && this.maximumGeneratorSize == other.maximumGeneratorSize &&
    this.perPropertyParallelism == other.perPropertyParallelism && this.initialSeed == other.initialSeed
  }

  override def hashCode: Int = {
    var code = 17 + "CheckConfig".##
    code = 37 * code + this.minimumSuccessful.##
    code = 37 * code + this.maximumDiscardRatio.##
    code = 37 * code + this.maximumGeneratorSize.##
    code = 37 * code + this.perPropertyParallelism.##
    code = 37 * code + this.initialSeed.##
    37 * code
  }

  override def productPrefix: String = "CheckConfig"

  override def productArity: Int = 5

  override def productElement(n: Int): Any = n match {
    case 0 => this.minimumSuccessful
    case 1 => this.maximumDiscardRatio
    case 2 => this.maximumGeneratorSize
    case 3 => this.perPropertyParallelism
    case 4 => this.initialSeed
    case n => throw new IndexOutOfBoundsException(n.toString)
  }
}

object CheckConfig {
  def default: CheckConfig = CheckConfig(
    minimumSuccessful = 80,
    maximumDiscardRatio = 5,
    maximumGeneratorSize = 100,
    perPropertyParallelism = 10,
    initialSeed = None
  )

  def apply(
      minimumSuccessful: Int,
      maximumDiscardRatio: Int,
      maximumGeneratorSize: Int,
      perPropertyParallelism: Int,
      initialSeed: Option[Seed]): CheckConfig =
    new CheckConfig(minimumSuccessful,
                    maximumDiscardRatio,
                    maximumGeneratorSize,
                    perPropertyParallelism,
                    initialSeed)
}
