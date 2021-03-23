package util

import kotlin.math.round

fun quantize(quantum: Double, value: Double): Double =
  round(value / quantum) * quantum
