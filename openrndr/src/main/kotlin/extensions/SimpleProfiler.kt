/**
 * Taken directly from
 * https://github.com/davidohana/SimpleProfiler/blob/9672ce53baa630692e82081b1de7fecaed2515cf/src/main/kotlin/davidoh/profiling/SimpleProfiler.kt
 *
 * I just couldn't find it in maven repo so copy/pasta it is!
 */
package extensions

/**
Description : A section-based runtime profiler.
Author      : david.ohana@ibm.com
License     : Apache v2
 **/

interface Profiler {
  fun startSection(name: String)
  fun endSection(name: String = "")
  fun report(periodSec: Int = 30): Boolean
}

/**
 * A no-op profiler. Use it instead of SimpleProfiler in case you want to disable profiling.
 */
class NullProfiler : Profiler {
  override fun startSection(name: String) {
  }

  override fun endSection(name: String) {
  }

  override fun report(periodSec: Int) = false
}

class SimpleProfiler(
  val resetAfterSampleCount: Long = 0,
  val enclosingSectionName: String = "total",
  val printer: (String) -> Unit = ::println
) : Profiler {
  private val sectionToStats = mutableMapOf<String, ProfiledSectionStats>()
  private var lastReportTimeMillis = System.currentTimeMillis()
  private var lastStartedSectionName = ""

  /**
   * Start measuring a section.
   */
  override fun startSection(name: String) {
    require(name.isNotBlank())
    lastStartedSectionName = name

    val section = sectionToStats.getOrPut(name, { ProfiledSectionStats(name) })
    if (section.startTimeNano != 0L)
      throw IllegalArgumentException("Section $name already started")
    section.startTimeNano = System.nanoTime()
  }

  /**
   * End measuring a section.
   *
   * @param name name of the section to end. When empty or omitted, the last started section will be ended.
   */
  override fun endSection(name: String) {
    val nowNano = System.nanoTime()

    var sectionName = name
    if (name == "")
      sectionName = lastStartedSectionName

    check(sectionName.isNotBlank())

    val section = sectionToStats[sectionName]
      ?: throw IllegalArgumentException("Section $name does not exist")

    if (section.startTimeNano == 0L)
      throw IllegalStateException("section $name was not started")

    val tookNano = nowNano - section.startTimeNano
    if (resetAfterSampleCount > 0 && section.sampleCount == resetAfterSampleCount) {
      section.sampleCountBatch = 0
      section.totalTimeNanoBatch = 0
    }
    section.sampleCount++
    section.totalTimeNano += tookNano
    section.sampleCountBatch++
    section.totalTimeNanoBatch += tookNano
    section.startTimeNano = 0
  }

  /**
   * Print results using [printer] function. By default prints to stdout.
   */
  override fun report(periodSec: Int): Boolean {
    if (System.currentTimeMillis() - lastReportTimeMillis < periodSec * 1000)
      return false

    var enclosingTimeNano = 0L
    if (enclosingSectionName.isNotEmpty()) {
      val enclosingSection = sectionToStats[enclosingSectionName]
      if (enclosingSection != null)
        enclosingTimeNano = enclosingSection.totalTimeNano
    }

    val includeBatchRates = resetAfterSampleCount > 0
    val text = sectionToStats.values
      .sortedByDescending { it.totalTimeNano }
      .map { it.toString(enclosingTimeNano, includeBatchRates) }
      .joinToString(System.lineSeparator())
    printer(text)

    lastReportTimeMillis = System.currentTimeMillis()
    return true
  }
}


data class ProfiledSectionStats(
  val sectionName: String,
  var startTimeNano: Long = 0,
  var sampleCount: Long = 0,
  var totalTimeNano: Long = 0,
  var sampleCountBatch: Long = 0,
  var totalTimeNanoBatch: Long = 0
) {
  fun toString(enclosingTimeNano: Long, includeBatchRates: Boolean): String {
    var tookSecText = (totalTimeNano / 1000000000.0).format(6, 2) + " s"
    if (enclosingTimeNano > 0) {
      val contributionPercent = (100.0 * totalTimeNano / enclosingTimeNano).format(3, 2)
      tookSecText += " ($contributionPercent%)"
    }
    val sampleCountText = sampleCount.format(10)
    var msPerKSamples = (totalTimeNano / (1000.0 * sampleCount.toDouble())).format(6, 2)
    var samplesPerSec = (sampleCount.toDouble() / totalTimeNano * 1000000000).format(10, 2)
    if (includeBatchRates) {
      msPerKSamples += " (${(totalTimeNanoBatch / (1000.0 * sampleCountBatch.toDouble())).format(6, 2)})"
      samplesPerSec += " (${(sampleCountBatch.toDouble() / totalTimeNanoBatch * 1000000000).format(10, 2)})"
    }

    return "${sectionName.padEnd(20)}: took $tookSecText, $sampleCountText samples, " +
      "$msPerKSamples ms / 1000 samples, $samplesPerSec hz"
  }
}

fun Number.format(
  charsAboveZero: Int = 1, charsBelowZero: Int = 0,
  isSigned: Boolean = false, leadingZeros: Boolean = false
): String {
  val isInteger = this is Long || this is Int || this is Short

  if (isInteger && charsBelowZero > 0)
    return this.toDouble().format(charsAboveZero, charsBelowZero, isSigned, leadingZeros)

  val leadingZero = if (leadingZeros) "0" else ""
  val sign = if (isSigned) "+" else ""
  val length = charsAboveZero + charsBelowZero + (if (charsBelowZero > 0) 1 else 0) + sign.length
  val fraction = if (isInteger) "" else ".$charsBelowZero"
  val type = if (isInteger) "d" else "f"
  val template = "%$sign$leadingZero,$length$fraction$type"
  return template.format(this)
}

