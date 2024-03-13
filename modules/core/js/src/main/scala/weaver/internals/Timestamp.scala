package weaver.internals

import scalajs.js.Date

private[weaver] object Timestamp {

  def format(l: Long): String = {
    val date = new Date(0)

    date.setMilliseconds(l.toDouble)

    val hour    = date.getHours()
    val minutes = date.getMinutes()
    val seconds = date.getSeconds()

    s"$hour:$minutes:$seconds"
  }

  def now(): Long = Date.now().toLong

  def localTime(hours: Int, minutes: Int, seconds: Int): Long = {
    val date = new Date(Date.now())

    date.setHours(hours.toDouble,
                  min = minutes.toDouble,
                  sec = seconds.toDouble)

    date.getTime().toLong
  }

}
