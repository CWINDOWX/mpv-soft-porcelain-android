package live.mehiz.mpvkt.danmaku

import kotlinx.serialization.Serializable

object DanmakuMode {
  const val SCROLL = 1
  const val BOTTOM = 4
  const val TOP = 5
}

data class DanmakuItem(
  val timeSec: Float,
  val mode: Int,
  val colorRgb: Int,
  val text: String,
  val repeatCount: Int = 1,
) {
  val displayText: String
    get() = if (repeatCount > 1) "$text ×$repeatCount" else text
}

enum class DanmakuFontOption(val id: String, val label: String) {
  Lxgw("lxgw", "霞鹜文楷"),
  Sans("sans", "无衬线"),
  Serif("serif", "衬线体"),
  Mono("mono", "等宽体"),
  ;

  companion object {
    fun fromId(id: String): DanmakuFontOption = entries.firstOrNull { it.id == id } ?: Lxgw
  }
}

enum class DanmakuChConvert(val code: Int, val label: String) {
  Original(0, "原样"),
  Simplified(1, "简体"),
  Traditional(2, "繁体"),
  ;

  companion object {
    fun fromCode(code: Int): DanmakuChConvert = entries.firstOrNull { it.code == code } ?: Simplified
  }
}

data class DanmakuStyleConfig(
  val fontSize: Int = 36,
  val opacity: Float = 0.78f,
  val displayArea: Float = 0.72f,
  val scrollSeconds: Int = 12,
  val outline: Float = 1.0f,
  val shadow: Float = 0.5f,
  val maxOnScreen: Int = 0, // 0 = unlimited
  val bold: Boolean = true,
  val blockTop: Boolean = false,
  val blockBottom: Boolean = false,
  val blockScroll: Boolean = false,
  val blockColor: Boolean = false,
  val mergeDuplicates: Boolean = true,
  val chConvert: Int = 1,
  val fontChoice: String = "lxgw",
)

data class DanmakuPreset(
  val id: String,
  val label: String,
  val apply: (DanmakuStyleConfig) -> DanmakuStyleConfig,
)

val DANMAKU_PRESETS = listOf(
  DanmakuPreset("comfort", "清爽") { cur ->
    cur.copy(
      fontSize = 36,
      opacity = 0.78f,
      displayArea = 0.72f,
      scrollSeconds = 12,
      bold = true,
      outline = 1.0f,
      shadow = 0.5f,
      maxOnScreen = 0,
      mergeDuplicates = true,
      blockTop = false,
      blockBottom = false,
      blockScroll = false,
      blockColor = false,
    )
  },
  DanmakuPreset("soft", "护眼") { cur ->
    cur.copy(
      fontSize = 32,
      opacity = 0.50f,
      displayArea = 0.50f,
      scrollSeconds = 15,
      bold = false,
      outline = 0.8f,
      shadow = 0.4f,
      maxOnScreen = 40,
      blockBottom = true,
    )
  },
  DanmakuPreset("large", "大字") { cur ->
    cur.copy(
      fontSize = 46,
      opacity = 0.88f,
      displayArea = 0.72f,
      scrollSeconds = 11,
      bold = true,
      outline = 1.6f,
      shadow = 0.9f,
    )
  },
  DanmakuPreset("minimal", "极简") { cur ->
    cur.copy(
      fontSize = 30,
      opacity = 0.62f,
      displayArea = 0.35f,
      scrollSeconds = 14,
      bold = false,
      outline = 0.8f,
      shadow = 0.4f,
      maxOnScreen = 25,
      mergeDuplicates = true,
      blockTop = true,
      blockBottom = true,
      blockColor = true,
    )
  },
  DanmakuPreset("party", "狂欢") { cur ->
    cur.copy(
      fontSize = 38,
      opacity = 0.88f,
      displayArea = 0.95f,
      scrollSeconds = 10,
      bold = true,
      outline = 1.0f,
      shadow = 0.5f,
      maxOnScreen = 0,
      mergeDuplicates = false,
      blockTop = false,
      blockBottom = false,
      blockScroll = false,
      blockColor = false,
    )
  },
)

data class DanmakuSourceInfo(
  val sourceType: String, // "dandanplay" | "local_xml"
  val animeTitle: String,
  val episodeTitle: String,
  val episodeId: Long? = null,
  val totalCount: Int = 0,
)

data class DanmakuSearchAnime(
  val bangumiId: String,
  val animeTitle: String,
  val type: String,
  val typeDescription: String,
  val episodeCount: Int,
)

data class DanmakuSearchEpisode(
  val episodeId: Long,
  val episodeNumber: String,
  val episodeTitle: String,
)

@Serializable
data class DanmakuHistoryEntry(
  val episodeId: Long,
  val animeTitle: String = "",
  val episodeTitle: String = "",
  val updatedAt: Long = 0L,
)
