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

@Inject
@SingleIn(AppScope::class)
class PorcelainPreferences(preferenceStore: PreferenceStore) {
  val shaderPreset = preferenceStore.getEnum("porcelain_shader_preset", MobileShaderPreset.Anime4kFast)
  val shaderDeband = preferenceStore.getBoolean("porcelain_shader_deband", false)

  val chapterSkipMode = preferenceStore.getEnum("porcelain_chapter_skip_mode", ChapterSkipMode.Prompt)
  val skipOp = preferenceStore.getBoolean("porcelain_skip_op", true)
  val skipEd = preferenceStore.getBoolean("porcelain_skip_ed", true)
  val skipPreview = preferenceStore.getBoolean("porcelain_skip_preview", false)
}
