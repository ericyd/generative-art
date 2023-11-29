package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.*
import org.openrndr.shape.*
import util.rotatePoint
import util.timestamp
import kotlin.math.*
import kotlin.random.Random

fun printingthings() {
  println("org.openrndr.application = ${System.getProperty("org.openrndr.application")}")
}
fun main() = application {

  configure {
    width = 800
    height = 800
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      contentScale = 3.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    val bg = ColorRGBa.WHITE
    backgroundColor = bg

    // this basically generates a wave function inspired by fBm
    fun generateWaveFn(oscillatorCount: Int, amplitude: Double, scale: Double, rng: Random): (x: Double) -> Double {
      // next level = allow oscillator amplitudes to vary over time -- i.e. oscillatorAmplitude is actually a function
      val oscillators = List(oscillatorCount) { index ->
        val i = index + 1.0
        val oscillatorScale = scale * random(i.pow(2.0) * 2.5, i.pow(2.0) * 3.5, rng)
        val phase = random(-PI, PI, rng)
        val oscillatorAmplitude = { x: Double ->
          // this is completely arbitrary...
          // higher is "more attenuated at higher frequencies"
          val amplitudeBase = 30.0
          val variation = sin(x * scale / 3.0 + (phase - PI * i * 0.5))
          variation * amplitudeBase.pow(1.0 / i) / amplitudeBase
        }
        Triple(oscillatorScale, oscillatorAmplitude, phase)
      }

      return { x: Double ->
        oscillators.sumOf { (scale, amplitude, phase) -> sin(x * scale + phase) * amplitude(x) } * amplitude
      }
    }

    // generates a line based on the defined wave function
    fun baseline(waveBaseline: Double, wave: (x: Double) -> Double): List<Vector2> {
      val min = width / -2
      val max = width * 3 / 2
      return List(max - min) {
        val x = it.toDouble()
        val y = wave(x) + waveBaseline
        Vector2(x, y)
      }
    }

    extend {
      seed = 1862747061
      val rng = Random(seed)
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 1.0

      /* wave props */
      val majorScale = 1.0 / width * 1.25
      val waveAmplitude = height / 3.0
      val wave = generateWaveFn(3, waveAmplitude, majorScale, rng)
      var offset = random(height * 0.1, height * 0.5, rng)
      /* end wave props */
      val waveAmplitudes = List(2) { random(height * 0.01, height * 0.1,  rng) }
      val baselines = waveAmplitudes.map {
        offset += it
        baseline(offset, wave)
      }

      // draw base contours
      drawer.contours(baselines.map { ShapeContour.fromPoints(it, closed = false) })

      val top = baselines[0]
      val bottom = baselines[1]
      val slopeSize = 40
      val stepSize = 10
      for (index in 0..top.size step stepSize) {
        // don't overflow; we're connecting top to "bottom + slopeSize"
        if (index + slopeSize > bottom.size - 1) {
          continue
        }
        // the higher this is, the more "ease" is in the curve
        val rotation = PI * -0.2
        drawer.contour(contour {
          val start = top[index]
          moveTo(start)
          val end = bottom[index + slopeSize]
          val diff = end - start
          val ctrl1 = rotatePoint(diff * 0.3 + start, rotation, start)
          val ctrl2 = rotatePoint(diff * 0.7 + start, rotation, end)
          curveTo(ctrl1, ctrl2, end)
        })
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

///Users/eric.dauenhauer/Library/Java/JavaVirtualMachines/corretto-19.0.2/Contents/Home/bin/java -javaagent:/Applications/IntelliJ IDEA CE.app/Contents/lib/idea_rt.jar=60488:/Applications/IntelliJ IDEA CE.app/Contents/bin -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
// -classpath /Users/eric.dauenhauer/dev/generative-art/build/classes/kotlin/main
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/2.0.7/41eb7184ea9d556f23e18b5cb99cad1f8581fc00/slf4j-api-2.0.7.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-ffmpeg/0.4.3/70ae4df506a2c15975f81a1f2cac227426db0643/openrndr-ffmpeg-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-gui/0.4.3/a3141cc118ce2c644adfecdb8ad4880eb17de405/orx-gui-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk8/1.8.21/67f57e154437cd9e6e9cf368394b95814836ff88/kotlin-stdlib-jdk8-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-dialogs/0.4.3/d96d239698531db2d4b23fa5e1f2f147d6786177/openrndr-dialogs-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-olive/0.4.3/fdabcc4ac0f7564d34fa735edb3a8f29fa5e1364/orx-olive-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-panel/0.4.3/d546a4eac5c36bdc50c78fe10cc6fe55879b35bb/orx-panel-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-video-profiles/0.4.3/4a17c4009a9c19b1f9855f5b9e5289b3f8a22791/orx-video-profiles-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-script-runtime/1.8.21/bf43f76b6e29bdc09b6c8a4511a9118b0ab12b1f/kotlin-script-runtime-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/io.github.microutils/kotlin-logging-jvm/3.0.5/82f2256aeedccfd9c27ea585274a50bf06517383/kotlin-logging-jvm-3.0.5.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-openal-jvm/0.4.3/432167c5d81e7b7c73fa02ec83db4ebed6e41505/openrndr-openal-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-application-jvm/0.4.3/95ab1f3aa9cd693bbe47bb2ffe6cad08b8e98254/openrndr-application-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-svg-jvm/0.4.3/f0c13032a14e057b4efa420285fa9cb15eb410e8/openrndr-svg-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-animatable-jvm/0.4.3/29ab378ba6bd5222d76586b0bc7e453e1e942bba/openrndr-animatable-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-extensions-jvm/0.4.3/adcc58e6d0c9b71c524b1563a96c92d9a44a80f1/openrndr-extensions-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-filter-jvm/0.4.3/2cea3e767c26e1a26cd2600a0cd315636bc9cb95/openrndr-filter-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-camera-jvm/0.4.3/1e44ea98a9146cce81ec3e885fe72a3d91a6af54/orx-camera-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-color-jvm/0.4.3/90d0715a4a678d5b2582d3f89f87e44054ff4de3/orx-color-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-compositor-jvm/0.4.3/7ec25aa1d82e3fba5b9503c23b0e6d15bca7cf30/orx-compositor-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-fx-jvm/0.4.3/2775478b48c2c71552617e100be772d2ef513b8d/orx-fx-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-image-fit-jvm/0.4.3/e143b237be793c8afc7e37498a1beda2c7881840/orx-image-fit-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-no-clear-jvm/0.4.3/9f994640cbd3b9f2cfad1e990bb60ef844b8ad3a/orx-no-clear-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-noise-jvm/0.4.3/9234cdfec9a823736fcfc604c0143cbbd5ea68ca/orx-noise-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-shade-styles-jvm/0.4.3/5bcf58b00c4ec9e5450f92c1d593f95190061191/orx-shade-styles-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-shapes-jvm/0.4.3/2989ac89064b30475a942ecced83132a0b8b4509/orx-shapes-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-view-box-jvm/0.4.3/7598f3e6bb29a63f630b9612a07661f0e826ca70/orx-view-box-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk7/1.8.21/7473b8cd3c0ef9932345baf569bc398e8a717046/kotlin-stdlib-jdk7-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.8.21/43d50ab85bc7587adfe3dda3dbe579e5f8d51265/kotlin-stdlib-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-common/1.8.21/d749cd5ae25da36d06e5028785038e24f9d37976/kotlin-stdlib-common-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr.extra/orx-parameters-jvm/0.4.3/65703b614f399c028d679459f640cfd28567a050/orx-parameters-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-draw-jvm/0.4.3/e3ed7e1e6c85a2bb3a7399d9c50871333c0350e9/openrndr-draw-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-math-jvm/0.4.3/4fba99aa8bcd6c2668ab6fc114955691e7205ae/openrndr-math-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-event-jvm/0.4.3/d64525590999b1e7cfdef25df21b86fc8ba41b74/openrndr-event-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-shape-jvm/0.4.3/216d99ce82ea11b86bee8efb5edd5d50e2918d4b/openrndr-shape-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-color-jvm/0.4.3/c77e645e14508deb2d55af2e72fa5cc0eec8cb71/openrndr-color-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-utils-jvm/0.4.3/a5646eefc18e36ecd8bad497d2669e5eab926494/openrndr-utils-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-ktessellation-jvm/0.4.3/892b557743e81a6a09db15f830fde0e4a7f848ce/openrndr-ktessellation-jvm-0.4.3.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-jvm/1.8.21/b532e6495c5cb85a2f73a4db4ae98f292ea1d30f/kotlin-scripting-jvm-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.7.0/239d3e685a54ae7c75a03708b78f3a810b5fb360/kotlinx-coroutines-core-jvm-1.7.0.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-compiler-embeddable/1.8.21/e44ae93ebb13d8e98ca3d0fc91e2b7de77fd97b1/kotlin-scripting-compiler-embeddable-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler-embeddable/1.8.21/a4a682d55fb788432e4b7bf2ab3acb7a7365fb35/kotlin-compiler-embeddable-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-common/1.8.21/18a869ce1cc76c771d7e3f0a2604f1a32127b558/kotlin-scripting-common-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-compiler-impl-embeddable/1.8.21/c6ce78a24d04f46fe0b1974deedcfe6356177a43/kotlin-scripting-compiler-impl-embeddable-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-daemon-embeddable/1.8.21/882003c48db58d16c8fb5a933abf08b5974a1a19/kotlin-daemon-embeddable-1.8.21.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.intellij.deps/trove4j/1.0.20200330/3afb14d5f9ceb459d724e907a21145e8ff394f02/trove4j-1.0.20200330.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/net.java.dev.jna/jna/5.6.0/330f2244e9030119ab3030fc3fededc86713d9cc/jna-5.6.0.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-serialization-core-jvm/1.5.0/d701e8cccd443a7cc1a0bcac53432f2745dcdbda/kotlinx-serialization-core-jvm-1.5.0.jar
// :/Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-kartifex-jvm/0.4.3/bfd9cfee6e1844a42b345af5a2762dd2d979c6f4/openrndr-kartifex-jvm-0.4.3.jar

//org.openrndr.internal.gl3.ApplicationBaseGLFWGL3
///Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-application-jvm/0.4.3/95ab1f3aa9cd693bbe47bb2ffe6cad08b8e98254/openrndr-application-jvm-0.4.3.jar!/org/openrndr/ApplicationBase.class
///Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-application-jvm/0.4.3/95ab1f3aa9cd693bbe47bb2ffe6cad08b8e98254/openrndr-application-jvm-0.4.3.jar!/org/openrndr/ApplicationBuilderJVM.class
///Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-application-jvm/0.4.3/95ab1f3aa9cd693bbe47bb2ffe6cad08b8e98254/openrndr-application-jvm-0.4.3.jar!/org/openrndr/ApplicationBuilder.class
///Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-application-jvm/0.4.3/95ab1f3aa9cd693bbe47bb2ffe6cad08b8e98254/openrndr-application-jvm-0.4.3.jar!/org/openrndr/Display.class
///Users/eric.dauenhauer/.gradle/caches/modules-2/files-2.1/org.openrndr/openrndr-draw-jvm/0.4.3/e3ed7e1e6c85a2bb3a7399d9c50871333c0350e9/openrndr-draw-jvm-0.4.3.jar!/org/openrndr/internal/Driver.class

// sketch.S53_PulseWave4Kt
