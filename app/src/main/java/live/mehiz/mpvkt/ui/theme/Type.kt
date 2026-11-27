package live.mehiz.mpvkt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

private val defaultTypography = Typography()

fun buildPorcelainTypography(customFontPath: String): Typography {
  val customTypeface = CustomFontManager.loadTypeface(customFontPath) ?: return defaultTypography
  val family = FontFamily(customTypeface)
  return Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = family),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = family),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = family),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = family),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = family),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = family),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = family),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = family),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = family),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = family),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = family),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = family),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = family),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = family),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = family),
  )
}
