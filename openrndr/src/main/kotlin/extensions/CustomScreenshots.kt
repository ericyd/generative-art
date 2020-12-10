package extensions

// TODO: open PR against openrndr for new functionality
// and consider adding an additional "Named" property
// and consider docs

/**
 * This file is basically stolen directly from
 * https://github.com/openrndr/openrndr/blob/50554b34160aa40bdc9b18bf9e5e79ba310d3973/openrndr-extensions/src/main/kotlin/org/openrndr/extensions/Screenshots.kt
 *
 * Only reason I'm adding it here is to give immediate access to "trigger" override, because I want to be able to name
 * things on the fly
 */
import extensions.CreateScreenshot.AutoNamed
import extensions.CreateScreenshot.Named
import extensions.CreateScreenshot.None
import mu.KotlinLogging
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.renderTarget
import org.openrndr.events.Event
import java.io.File
import java.time.LocalDateTime

/**
 * Use to automatically generate a String like
 * `path/basename-date.extension`
 * when saving files like videos, images, json, etc.
 * `basename` defaults to the current program or window name.
 * Used by ScreenRecorder, Screenshots and available to the user.
 *
 * STOLEN FROM
 * https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-core/src/main/kotlin/org/openrndr/utils/NamedTimestamp.kt
 * for import reasons and adding appended
 */
fun Program.namedTimestamp(extension: String = "", folder: String? = null, appended: String = ""):
  String {
    val now = LocalDateTime.now()
    val basename = this.name.ifBlank { this.window.title.ifBlank { "untitled" } }
    val path = when {
      folder.isNullOrBlank() -> ""
      folder.endsWith("/") -> folder
      else -> "$folder/"
    }
    val ext = when {
      extension.isEmpty() -> ""
      extension.startsWith(".") -> extension
      else -> ".$extension"
    }

    return "$path$basename-%04d-%02d-%02d-%02d.%02d.%02d$appended$ext".format(
      now.year, now.month.value, now.dayOfMonth,
      now.hour, now.minute, now.second
    )
  }

private val logger = KotlinLogging.logger {}

internal sealed class CreateScreenshot {
  object None : CreateScreenshot()
  object AutoNamed : CreateScreenshot()
  data class Named(val name: String) : CreateScreenshot()
}

data class ScreenshotEvent(val basename: String)

/**
 * an extension that takes screenshots when [key] (default is spacebar) is pressed
 */
open class CustomScreenshots : Extension {

  /**
   * Event that is triggered just before drawing the contents for the screenshot
   */
  val beforeScreenshot = Event<ScreenshotEvent>()

  /**
   * Event that is triggered after contents have been drawn and the screenshot has been committed to file
   */
  val afterScreenshot = Event<ScreenshotEvent>()

  override var enabled: Boolean = true

  /**
   * scale can be se to be greater than 1.0 for higher resolution screenshots
   */
  var scale = 1.0

  /**
   * should saving be performed asynchronously?
   */
  var async: Boolean = true

  /**
   * multisample settings
   */
  var multisample: BufferMultisample = BufferMultisample.Disabled

  /**
   * delays the screenshot for a number of frames.
   * useful to let visuals build up in automated screenshots.
   */

  var delayFrames = 0

  /**
   * should the program quit after taking a screenshot?
   */
  var quitAfterScreenshot = false

  /**
   * the key that should be pressed to take a screenshot
   */
  var key: String = "space"

  /**
   * the folder where the screenshot will be saved to. Default value is "screenshots", saves in current working
   * directory when set to null.
   */
  var folder: String? = "screenshots"

  /**
   * text that should be appended to the filename when it is auto-generated
   */
  var append: String = ""

  /**
   * when true, capture every frame.
   * when false, only capture on keypress.
   */
  var captureEveryFrame: Boolean = false
    set(value) {
      field = value
      if (value) createScreenshot = AutoNamed
    }

  /**
   * override automatic naming for screenshot
   */
  var name: String? = null

  internal var createScreenshot: CreateScreenshot = None

  private var target: RenderTarget? = null
  private var resolved: ColorBuffer? = null

  private var programRef: Program? = null

  override fun setup(program: Program) {
    programRef = program
    program.keyboard.keyDown.listen {
      if (!it.propagationCancelled) {
        if (it.name == key) {
          trigger(append)
        }
      }
    }
  }

  /**
   * Trigger screenshot creation
   */
  fun trigger(appended: String? = null) {
    if (!appended.isNullOrBlank()) {
      append = appended
    }
    createScreenshot = if (name.isNullOrBlank()) AutoNamed else Named(name!!)
    programRef?.window?.requestDraw()
  }

  private var filename: String? = null
  override fun beforeDraw(drawer: Drawer, program: Program) {
    if (createScreenshot != None && delayFrames-- <= 0) {
      val targetWidth = (program.width * scale).toInt()
      val targetHeight = (program.height * scale).toInt()

      target = renderTarget(targetWidth, targetHeight, multisample = multisample) {
        colorBuffer()
        depthBuffer()
      }
      resolved = when (multisample) {
        BufferMultisample.Disabled -> null
        is BufferMultisample.SampleCount -> colorBuffer(targetWidth, targetHeight)
      }
      target?.bind()

      filename = when (val cs = createScreenshot) {
        None -> throw IllegalStateException("")
        AutoNamed -> if (name.isNullOrBlank()) program.namedTimestamp("png", folder, append) else name
        is Named -> cs.name
      }

      filename?.let {
        beforeScreenshot.trigger(ScreenshotEvent(it.dropLast(4)))
      }

      program.backgroundColor?.let {
        drawer.clear(it)
      }
    }
  }

  override fun afterDraw(drawer: Drawer, program: Program) {
    filename?.let { fn ->
      filename = null
      val targetFile = File(fn)

      drawer.shadeStyle = null
      target?.unbind()

      target?.let {
        drawer.defaults()
        targetFile.parentFile?.let { file ->
          if (!file.exists()) {
            file.mkdirs()
          }
        }

        val resolved = resolved
        if (resolved == null) {
          it.colorBuffer(0).saveToFile(targetFile, async = async)
          drawer.image(it.colorBuffer(0), it.colorBuffer(0).bounds, drawer.bounds)
        } else {
          target?.let { rt ->
            rt.colorBuffer(0).copyTo(resolved)
            resolved.saveToFile(targetFile, async = async)
            drawer.image(resolved, resolved.bounds, drawer.bounds)
          }
        }
        logger.info("[Screenshots] saved to: ${targetFile.relativeTo(File("."))}")
        afterScreenshot.trigger(ScreenshotEvent(fn.dropLast(4)))
      }

      target?.destroy()
      resolved?.destroy()

      if (!this.captureEveryFrame) {
        this.createScreenshot = None
      }

      if (quitAfterScreenshot) {
        program.application.exit()
      }
    }
  }
}
