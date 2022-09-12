package weaver.internals

import scala.scalanative.unsafe._
import scala.scalanative.posix
import posix.time
import posix.timeOps._

private[weaver] object Timestamp {

  def format(epochSecond: Long): String = Zone { implicit zone =>
    val out     = alloc[time.tm]()
    val timePtr = alloc[time.time_t]()
    !timePtr = epochSecond
    val gmTime: Ptr[time.tm] = time.localtime_r(timePtr, out)
    val hour                 = gmTime.tm_hour
    val minutes              = gmTime.tm_min
    val seconds              = gmTime.tm_sec
    s"$hour:$minutes:$seconds"
  }

  def localTime(hours: Int, minutes: Int, seconds: Int): Long = Zone {
    implicit zone =>
      val out     = alloc[time.tm]()
      val timePtr = alloc[time.time_t]()
      !timePtr = time.time(null)
      val gmTime: Ptr[time.tm] = time.gmtime_r(timePtr, out)

      gmTime.tm_hour = hours
      gmTime.tm_min = minutes
      gmTime.tm_sec = seconds
      gmTime.tm_isdst = -1; // Is DST on? 1 = yes, 0 = no, -1 = unknown
      time.mktime(gmTime).longValue()
  }
}
