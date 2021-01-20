package util

import org.openrndr.draw.RenderTarget
import java.io.File

fun saveToFile(rt: RenderTarget, filename: String) {
  val targetFile = File(filename)
  targetFile.parentFile?.let { file ->
    if (!file.exists()) {
      file.mkdirs()
    }
  }
  rt.colorBuffer(0).saveToFile(targetFile, async = false)
  println("[io] Saved file to $filename")
}
