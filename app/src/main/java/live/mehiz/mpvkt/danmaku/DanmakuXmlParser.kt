package live.mehiz.mpvkt.danmaku

import android.util.Xml
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.abs

object DanmakuXmlParser {
  private val controlCharRegex = Regex("[\\u0000-\\u001F]")

  fun parseBilibiliXml(input: InputStream): List<DanmakuItem> {
    val result = ArrayList<DanmakuItem>(1024)
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(InputStreamReader(input, Charsets.UTF_8))

    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG && parser.name.equals("d", ignoreCase = true)) {
        val pAttr = parser.getAttributeValue(null, "p")
        val text = runCatching { parser.nextText() }.getOrNull().orEmpty()
        parseXmlEntry(pAttr, text)?.let { result.add(it) }
      }
      eventType = runCatching { parser.next() }.getOrElse { XmlPullParser.END_DOCUMENT }
    }
    result.sortBy { it.timeSec }
    return result
  }

  private fun parseXmlEntry(pAttr: String?, rawText: String): DanmakuItem? {
    if (pAttr.isNullOrBlank()) return null
    val cleaned = cleanText(rawText)
    if (cleaned.isEmpty()) return null
    val parts = pAttr.split(',')
    if (parts.size < 4) return null
    val timeSec = parts[0].trim().toFloatOrNull() ?: return null
    if (timeSec < 0f || !timeSec.isFinite()) return null
    val rawMode = parts[1].trim().toIntOrNull() ?: DanmakuMode.SCROLL
    val mode = normalizeMode(rawMode) ?: return null
    val color = parts[3].trim().toLongOrNull()?.toInt()?.and(0xFFFFFF) ?: 0xFFFFFF
    return DanmakuItem(timeSec = timeSec, mode = mode, colorRgb = color, text = cleaned)
  }

  fun parseDandanplayComments(jsonStr: String, json: Json): List<DanmakuItem> {
    val root = runCatching { json.parseToJsonElement(jsonStr).jsonObject }.getOrNull() ?: return emptyList()
    val comments = root["comments"] as? JsonArray ?: return emptyList()
    val result = ArrayList<DanmakuItem>(comments.size)
    for (el in comments) {
      val obj = el as? JsonObject ?: continue
      val p = obj["p"]?.jsonPrimitive?.contentOrNull ?: continue
      val rawText = obj["m"]?.jsonPrimitive?.contentOrNull ?: continue
      val cleaned = cleanText(rawText)
      if (cleaned.isEmpty()) continue
      val shift = obj["shift"]?.jsonPrimitive?.doubleOrNull ?: 0.0
      val fields = p.split(',')
      if (fields.size < 2) continue
      val timeSec = ((fields[0].trim().toDoubleOrNull() ?: continue) + shift).toFloat()
      if (timeSec < 0f || !timeSec.isFinite()) continue
      val rawMode = fields[1].trim().toIntOrNull() ?: DanmakuMode.SCROLL
      val mode = normalizeMode(rawMode) ?: continue
      val color = fields.getOrNull(2)?.trim()?.toLongOrNull()?.toInt()?.and(0xFFFFFF) ?: 0xFFFFFF
      result.add(DanmakuItem(timeSec = timeSec, mode = mode, colorRgb = color, text = cleaned))
    }
    result.sortBy { it.timeSec }
    return result
  }

  fun mergeDuplicates(sortedItems: List<DanmakuItem>, toleranceSec: Float = 5f): List<DanmakuItem> {
    if (sortedItems.size <= 1) return sortedItems
    val result = ArrayList<DanmakuItem>(sortedItems.size)
    // Track last index in `result` for each normalized key (text + mode)
    val lastSeenIndex = HashMap<String, Int>(256)
    for (item in sortedItems) {
      val key = "${item.mode}|${item.text.trim().lowercase()}"
      val prevIdx = lastSeenIndex[key]
      if (prevIdx != null) {
        val prev = result[prevIdx]
        if (abs(item.timeSec - prev.timeSec) <= toleranceSec) {
          result[prevIdx] = prev.copy(repeatCount = prev.repeatCount + item.repeatCount)
          continue
        }
      }
      lastSeenIndex[key] = result.size
      result.add(item)
    }
    return result
  }

  private fun normalizeMode(raw: Int): Int? = when (raw) {
    1, 2, 3, 6 -> DanmakuMode.SCROLL
    4 -> DanmakuMode.BOTTOM
    5 -> DanmakuMode.TOP
    else -> null // Skip scripted/positioned advanced danmaku (mode 7/8/9)
  }

  private fun cleanText(raw: String): String =
    controlCharRegex.replace(raw, " ").replace(Regex("\\s+"), " ").trim()
}
