package extensions

import java.time.LocalDateTime

fun timestamp(time: LocalDateTime = LocalDateTime.now()): String =
  "%04d-%02d-%02dT%02d.%02d.%02d".format(
    time.year, time.month.value, time.dayOfMonth,
    time.hour, time.minute, time.second
  )
