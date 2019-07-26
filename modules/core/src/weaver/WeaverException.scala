package weaver

abstract class WeaverException(
    message: String,
    cause: Option[Throwable],
    location: SourceLocation)
    extends RuntimeException(message, cause.orNull) {

  def getLocation: SourceLocation = location

}
