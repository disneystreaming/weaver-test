package weaver.intellij.runner

// TODO get rid of Any
class TeamCityEvent(
    eventName: String,
    nodeId: Int,
    attributes: Seq[(String, Any)]) {

  def show = {
    s"##teamcity[$eventName nodeId='$nodeId' ${attributes.map(showAttr).mkString(" ")}]"
  }

  private def showAttr(attr: (String, Any)): String = {
    val (key, value) = attr
    s"$key='${escapeString(value.toString())}'"
  }

  private def escapeString(str: String): String = {
    if (str != null)
      str
        .replaceAll("[|]", "||")
        .replaceAll("[']", "|'")
        .replaceAll("[\n]", "|n")
        .replaceAll("[\r]", "|r")
        .replaceAll("]", "|]")
        .replaceAll("\\[", "|[")
    else ""
  }

}

object TeamCityEvent {
  def apply(
      eventName: String,
      id: Int,
      attributes: (String, Any)*): TeamCityEvent =
    new TeamCityEvent(eventName, id, attributes)

}
