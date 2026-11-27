package live.mehiz.mpvkt.preferences

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import live.mehiz.mpvkt.preferences.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.preference.getEnum

enum class MobileShaderPreset(
  val id: String,
  val title: String,
  val subtitle: String,
  val badge: String?,
  val shaderFiles: List<String>,
) {
  None(
    id = "none",
    title = "原画",
    subtitle = "关闭着色器 · 最省电",
    badge = null,
    shaderFiles = emptyList(),
  ),
  QcomSgeds(
    id = "qcom",
    title = "轻锐化",
    subtitle = "QCOM SGEDS · 极低负载",
    badge = "Q",
    shaderFiles = listOf("QCOM_SGEDS_ms_RT.glsl"),
  ),
  Anime4kFast(
    id = "a4k_fast",
    title = "Anime4K 轻量",
    subtitle = "Mode A · 推荐日常追番",
    badge = "A4K",
    shaderFiles = listOf("Anime4K_Restore_CNN_M.glsl", "Anime4K_Upscale_CNN_x2_M.glsl"),
  ),
  Anime4kClear(
    id = "a4k_clear",
    title = "Anime4K 清晰",
    subtitle = "Mode A+ · 线条增强",
    badge = "A4K+",
    shaderFiles = listOf("Anime4K_Restore_CNN_L.glsl", "Anime4K_Upscale_CNN_x2_M.glsl"),
  ),
  Anime4kDenoise(
    id = "a4k_denoise",
    title = "Anime4K 降噪",
    subtitle = "Mode B · 老番噪点修复",
    badge = "FIX",
    shaderFiles = listOf(
      "Anime4K_Denoise_Bilateral_Mode.glsl",
      "Anime4K_Restore_CNN_M.glsl",
      "Anime4K_Upscale_CNN_x2_M.glsl",
    ),
  ),
}

enum class ChapterSkipMode(val title: String, val shortLabel: String) {
  Prompt("询问跳过", "询问"),
  Auto("自动跳过", "自动"),
  Off("关闭", "关"),
}

enum class BangumiSyncMode(val title: String, val shortLabel: String) {
  Prompt("片尾询问", "询问"),
  Auto("自动同步", "自动"),
  Off("仅手动", "手动"),
}

@Inject
@SingleIn(AppScope::class)
class PorcelainPreferences(preferenceStore: PreferenceStore) {
  // M1: Mobile shaders & chapter skip
  val shaderPreset = preferenceStore.getEnum("porcelain_shader_preset", MobileShaderPreset.Anime4kFast)
  val shaderDeband = preferenceStore.getBoolean("porcelain_shader_deband", false)

  val chapterSkipMode = preferenceStore.getEnum("porcelain_chapter_skip_mode", ChapterSkipMode.Prompt)
  val skipOp = preferenceStore.getBoolean("porcelain_skip_op", true)
  val skipEd = preferenceStore.getBoolean("porcelain_skip_ed", true)
  val skipPreview = preferenceStore.getBoolean("porcelain_skip_preview", false)

  // M2: Native 120Hz Danmaku engine & style
  val danmakuEnabled = preferenceStore.getBoolean("porcelain_dm_enabled", true)
  val danmakuAutoLoad = preferenceStore.getBoolean("porcelain_dm_auto_load", true)
  val danmakuApiServer = preferenceStore.getString("porcelain_dm_api_server", "https://danmaku-api.152468.xyz")
  val danmakuFontSize = preferenceStore.getInt("porcelain_dm_font_size", 36)
  val danmakuOpacity = preferenceStore.getFloat("porcelain_dm_opacity", 0.78f)
  val danmakuDisplayArea = preferenceStore.getFloat("porcelain_dm_display_area", 0.72f)
  val danmakuScrollSeconds = preferenceStore.getInt("porcelain_dm_scroll_seconds", 12)
  val danmakuOutline = preferenceStore.getFloat("porcelain_dm_outline", 1.0f)
  val danmakuShadow = preferenceStore.getFloat("porcelain_dm_shadow", 0.5f)
  val danmakuMaxOnScreen = preferenceStore.getInt("porcelain_dm_max_screen", 0)
  val danmakuBold = preferenceStore.getBoolean("porcelain_dm_bold", true)
  val danmakuBlockTop = preferenceStore.getBoolean("porcelain_dm_block_top", false)
  val danmakuBlockBottom = preferenceStore.getBoolean("porcelain_dm_block_bottom", false)
  val danmakuBlockScroll = preferenceStore.getBoolean("porcelain_dm_block_scroll", false)
  val danmakuBlockColor = preferenceStore.getBoolean("porcelain_dm_block_color", false)
  val danmakuMergeDuplicates = preferenceStore.getBoolean("porcelain_dm_merge_dup", true)
  val danmakuChConvert = preferenceStore.getInt("porcelain_dm_ch_convert", 1)
  val danmakuFontChoice = preferenceStore.getString("porcelain_dm_font", "lxgw")

  // M2: Bangumi progress sync
  val bangumiAccessToken = preferenceStore.getString("porcelain_bgm_token", "")
  val bangumiUsername = preferenceStore.getString("porcelain_bgm_username", "")
  val bangumiNickname = preferenceStore.getString("porcelain_bgm_nickname", "")
  val bangumiSyncMode = preferenceStore.getEnum("porcelain_bgm_sync_mode", BangumiSyncMode.Prompt)
}
