package live.mehiz.mpvkt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import live.mehiz.mpvkt.R

val PorcelainFontFamily = FontFamily(
  Font(R.font.lxgw_wenkai_mono_lite, FontWeight.Normal),
  Font(R.font.lxgw_wenkai_mono_lite, FontWeight.Medium),
  Font(R.font.lxgw_wenkai_mono_lite, FontWeight.SemiBold),
  Font(R.font.lxgw_wenkai_mono_lite, FontWeight.Bold),
)

private val defaultTypography = Typography()

val PorcelainTypography = Typography(
  displayLarge = defaultTypography.displayLarge.copy(fontFamily = PorcelainFontFamily),
  displayMedium = defaultTypography.displayMedium.copy(fontFamily = PorcelainFontFamily),
  displaySmall = defaultTypography.displaySmall.copy(fontFamily = PorcelainFontFamily),
  headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = PorcelainFontFamily),
  headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = PorcelainFontFamily),
  headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = PorcelainFontFamily),
  titleLarge = defaultTypography.titleLarge.copy(fontFamily = PorcelainFontFamily),
  titleMedium = defaultTypography.titleMedium.copy(fontFamily = PorcelainFontFamily),
  titleSmall = defaultTypography.titleSmall.copy(fontFamily = PorcelainFontFamily),
  bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = PorcelainFontFamily),
  bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = PorcelainFontFamily),
  bodySmall = defaultTypography.bodySmall.copy(fontFamily = PorcelainFontFamily),
  labelLarge = defaultTypography.labelLarge.copy(fontFamily = PorcelainFontFamily),
  labelMedium = defaultTypography.labelMedium.copy(fontFamily = PorcelainFontFamily),
  labelSmall = defaultTypography.labelSmall.copy(fontFamily = PorcelainFontFamily),
)
