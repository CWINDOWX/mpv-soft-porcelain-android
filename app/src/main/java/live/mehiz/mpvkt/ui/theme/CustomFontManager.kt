package live.mehiz.mpvkt.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

enum class CustomFontSlot(val prefix: String) {
  Ui("ui_font"),
  Danmaku("danmaku_font"),
}

data class ImportedFont(
  val path: String,
  val displayName: String,
)

object CustomFontManager {
  private val ALLOWED_EXTENSIONS = setOf("ttf", "otf", "ttc")

  private fun fontsDir(context: Context): File =
    File(context.filesDir, "custom_fonts").apply { if (!exists()) mkdirs() }

  fun importFont(context: Context, uri: Uri, slot: CustomFontSlot): Result<ImportedFont> = runCatching {
    val rawName = resolveDisplayName(context, uri)
    val ext = rawName.substringAfterLast('.', "").lowercase()
    require(ext in ALLOWED_EXTENSIONS) { "请选择 .ttf / .otf / .ttc 字体文件" }

    val dir = fontsDir(context)
    val tmp = File(dir, "${slot.prefix}_tmp_${System.currentTimeMillis()}.$ext")
    try {
      context.contentResolver.openInputStream(uri)?.use { input ->
        tmp.outputStream().use { output -> input.copyTo(output) }
      } ?: error("无法读取所选文件")

      require(tmp.length() in 1024L..(64L * 1024L * 1024L)) { "字体文件大小异常" }

      // Validate that Android can actually parse this font file before committing
      Typeface.createFromFile(tmp)

      // Remove old files for this slot
      dir.listFiles()?.filter { it.name.startsWith("${slot.prefix}_") && it != tmp }?.forEach { it.delete() }

      val finalFile = File(dir, "${slot.prefix}_${System.currentTimeMillis()}.$ext")
      if (!tmp.renameTo(finalFile)) {
        tmp.copyTo(finalFile, overwrite = true)
        tmp.delete()
      }
      val cleanName = rawName.substringBeforeLast('.').ifBlank { rawName }
      ImportedFont(path = finalFile.absolutePath, displayName = cleanName)
    } catch (e: Throwable) {
      tmp.delete()
      throw e
    }
  }

  fun clearFont(context: Context, slot: CustomFontSlot) {
    runCatching {
      fontsDir(context).listFiles()
        ?.filter { it.name.startsWith("${slot.prefix}_") }
        ?.forEach { it.delete() }
    }
  }

  fun loadTypeface(path: String): Typeface? {
    if (path.isBlank()) return null
    val file = File(path)
    if (!file.isFile || file.length() == 0L) return null
    return runCatching { Typeface.createFromFile(file) }.getOrNull()
  }

  private fun resolveDisplayName(context: Context, uri: Uri): String {
    val fromCursor = runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
      }
    }.getOrNull()
    return fromCursor?.takeIf { it.isNotBlank() }
      ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
      ?: "custom.ttf"
  }
}
