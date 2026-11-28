package live.mehiz.mpvkt.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Soft Porcelain Mist (柔雾瓷白) signature palette
val PorcelainBg = Color(0xFFF4F6FA)
val PorcelainSurface = Color(0xFFFFFFFF)
val PorcelainContainerLow = Color(0xFFF8FAFC)
val PorcelainContainer = Color(0xFFEDF1F7)
val PorcelainContainerHigh = Color(0xFFE5ECF5)
val PorcelainContainerHighest = Color(0xFFDCE5F0)
val PorcelainTrack = Color(0xFFCBD5E1)
val PorcelainCurtain = Color(0xFFE2E8F0)

val PorcelainBlue = Color(0xFF3578B8)
val PorcelainBlueDeep = Color(0xFF24598F)
val PorcelainBlueSoft = Color(0xFFDAE6F3)
val PorcelainBlueTint = Color(0xFFEBF2FA)

val PorcelainInk = Color(0xFF1E293B)
val PorcelainMuted = Color(0xFF64748B)
val PorcelainOutline = Color(0xFF94A3B8)
val PorcelainOutlineVariant = Color(0xFFD8E1EC)

val PorcelainSuccess = Color(0xFF2A9D6F)
val PorcelainError = Color(0xFFD6455D)
val PorcelainErrorSoft = Color(0xFFFDE8EC)

val primaryLight = PorcelainBlue
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = PorcelainBlueSoft
val onPrimaryContainerLight = Color(0xFF102A43)
val secondaryLight = Color(0xFF4B6B8A)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = PorcelainBlueTint
val onSecondaryContainerLight = PorcelainInk
val tertiaryLight = PorcelainBlue
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = PorcelainBlueSoft
val onTertiaryContainerLight = Color(0xFF102A43)
val errorLight = PorcelainError
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = PorcelainErrorSoft
val onErrorContainerLight = Color(0xFF5F1422)
val backgroundLight = PorcelainBg
val onBackgroundLight = PorcelainInk
val surfaceLight = PorcelainBg
val onSurfaceLight = PorcelainInk
val surfaceVariantLight = PorcelainContainer
val onSurfaceVariantLight = PorcelainMuted
val outlineLight = PorcelainOutline
val outlineVariantLight = PorcelainOutlineVariant
val scrimLight = Color(0xFF0F172A)
val inverseSurfaceLight = PorcelainInk
val inverseOnSurfaceLight = PorcelainBg
val inversePrimaryLight = Color(0xFF93C5FD)
val surfaceDimLight = Color(0xFFE6ECF4)
val surfaceBrightLight = PorcelainSurface
val surfaceContainerLowestLight = PorcelainSurface
val surfaceContainerLowLight = PorcelainSurface
val surfaceContainerLight = Color(0xFFF0F4FA)
val surfaceContainerHighLight = PorcelainContainerHigh
val surfaceContainerHighestLight = PorcelainContainerHighest

// Midnight Slate (暮夜深蓝) dark palette
val primaryDark = Color(0xFF6CA8E8)
val onPrimaryDark = Color(0xFF0D1B2A)
val primaryContainerDark = Color(0xFF213954)
val onPrimaryContainerDark = Color(0xFFDCEBFA)
val secondaryDark = Color(0xFFA2BCD6)
val onSecondaryDark = Color(0xFF14273A)
val secondaryContainerDark = Color(0xFF26384E)
val onSecondaryContainerDark = Color(0xFFDCEBFA)
val tertiaryDark = Color(0xFF6CA8E8)
val onTertiaryDark = Color(0xFF0D1B2A)
val tertiaryContainerDark = Color(0xFF213954)
val onTertiaryContainerDark = Color(0xFFDCEBFA)
val errorDark = Color(0xFFE86B7E)
val onErrorDark = Color(0xFF3E0A14)
val errorContainerDark = Color(0xFF6E1C2D)
val onErrorContainerDark = PorcelainErrorSoft
val backgroundDark = Color(0xFF111824)
val onBackgroundDark = Color(0xFFE6EDF6)
val surfaceDark = Color(0xFF161E2B)
val onSurfaceDark = Color(0xFFE6EDF6)
val surfaceVariantDark = Color(0xFF243144)
val onSurfaceVariantDark = Color(0xFF96A8BE)
val outlineDark = Color(0xFF64778F)
val outlineVariantDark = Color(0xFF334358)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFE6EDF6)
val inverseOnSurfaceDark = Color(0xFF1E293B)
val inversePrimaryDark = PorcelainBlue
val surfaceDimDark = Color(0xFF0E141F)
val surfaceBrightDark = Color(0xFF2A374C)
val surfaceContainerLowestDark = Color(0xFF0C111B)
val surfaceContainerLowDark = Color(0xFF1A2434)
val surfaceContainerDark = Color(0xFF212C3E)
val surfaceContainerHighDark = Color(0xFF28354A)
val surfaceContainerHighestDark = Color(0xFF314059)

enum class AppTheme(
  val displayName: String,
  val subtitle: String,
  val isDark: Boolean,
  val previewBg: Color,
  val previewSurface: Color,
  val previewPrimary: Color,
  val previewInk: Color,
) {
  Porcelain(
    displayName = "柔雾瓷白",
    subtitle = "瓷白浅底 · 静谧钴蓝",
    isDark = false,
    previewBg = Color(0xFFF4F6FA),
    previewSurface = Color(0xFFFFFFFF),
    previewPrimary = Color(0xFF3578B8),
    previewInk = Color(0xFF1E293B),
  ),
  Sakura(
    displayName = "樱雾浅粉",
    subtitle = "樱雾奶白 · 淡雅绯樱",
    isDark = false,
    previewBg = Color(0xFFFBF4F6),
    previewSurface = Color(0xFFFFFFFF),
    previewPrimary = Color(0xFFC85A7C),
    previewInk = Color(0xFF2E1E25),
  ),
  Celadon(
    displayName = "青瓷抹茶",
    subtitle = "冰青瓷白 · 温润竹绿",
    isDark = false,
    previewBg = Color(0xFFF2F7F4),
    previewSurface = Color(0xFFFFFFFF),
    previewPrimary = Color(0xFF2E8B6E),
    previewInk = Color(0xFF1B2D26),
  ),
  Amber(
    displayName = "暖纸琥珀",
    subtitle = "暖杏米纸 · 温润琥珀",
    isDark = false,
    previewBg = Color(0xFFFAF6EF),
    previewSurface = Color(0xFFFFFDF9),
    previewPrimary = Color(0xFFC2762A),
    previewInk = Color(0xFF2D241C),
  ),
  Midnight(
    displayName = "暮夜深蓝",
    subtitle = "暮夜深蓝 · 晴空柔蓝",
    isDark = true,
    previewBg = Color(0xFF161E2B),
    previewSurface = Color(0xFF212C3E),
    previewPrimary = Color(0xFF6CA8E8),
    previewInk = Color(0xFFE6EDF6),
  ),
  Obsidian(
    displayName = "极夜墨黑",
    subtitle = "极夜深墨 · 冰雾蓝白",
    isDark = true,
    previewBg = Color(0xFF0E1117),
    previewSurface = Color(0xFF191F29),
    previewPrimary = Color(0xFF7EB8F2),
    previewInk = Color(0xFFEDF2F8),
  ),
}

val porcelainLightScheme: ColorScheme = lightColorScheme(
  primary = primaryLight,
  onPrimary = onPrimaryLight,
  primaryContainer = primaryContainerLight,
  onPrimaryContainer = onPrimaryContainerLight,
  secondary = secondaryLight,
  onSecondary = onSecondaryLight,
  secondaryContainer = secondaryContainerLight,
  onSecondaryContainer = onSecondaryContainerLight,
  tertiary = tertiaryLight,
  onTertiary = onTertiaryLight,
  tertiaryContainer = tertiaryContainerLight,
  onTertiaryContainer = onTertiaryContainerLight,
  error = errorLight,
  onError = onErrorLight,
  errorContainer = errorContainerLight,
  onErrorContainer = onErrorContainerLight,
  background = backgroundLight,
  onBackground = onBackgroundLight,
  surface = surfaceLight,
  onSurface = onSurfaceLight,
  surfaceVariant = surfaceVariantLight,
  onSurfaceVariant = onSurfaceVariantLight,
  outline = outlineLight,
  outlineVariant = outlineVariantLight,
  scrim = scrimLight,
  inverseSurface = inverseSurfaceLight,
  inverseOnSurface = inverseOnSurfaceLight,
  inversePrimary = inversePrimaryLight,
  surfaceDim = surfaceDimLight,
  surfaceBright = surfaceBrightLight,
  surfaceContainerLowest = surfaceContainerLowestLight,
  surfaceContainerLow = surfaceContainerLowLight,
  surfaceContainer = surfaceContainerLight,
  surfaceContainerHigh = surfaceContainerHighLight,
  surfaceContainerHighest = surfaceContainerHighestLight,
)

val sakuraLightScheme: ColorScheme = lightColorScheme(
  primary = Color(0xFFC85A7C),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFF6DCE4),
  onPrimaryContainer = Color(0xFF471226),
  secondary = Color(0xFF8C5A6B),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFF7E8EE),
  onSecondaryContainer = Color(0xFF2E1E25),
  tertiary = Color(0xFFC85A7C),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFF6DCE4),
  onTertiaryContainer = Color(0xFF471226),
  error = Color(0xFFD6455D),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFFDE8EC),
  onErrorContainer = Color(0xFF5F1422),
  background = Color(0xFFFBF4F6),
  onBackground = Color(0xFF2E1E25),
  surface = Color(0xFFFBF4F6),
  onSurface = Color(0xFF2E1E25),
  surfaceVariant = Color(0xFFF2E6EA),
  onSurfaceVariant = Color(0xFF7A606A),
  outline = Color(0xFFB0939E),
  outlineVariant = Color(0xFFE5D2D9),
  scrim = Color(0xFF1F1117),
  inverseSurface = Color(0xFF2E1E25),
  inverseOnSurface = Color(0xFFFBF4F6),
  inversePrimary = Color(0xFFF4A8BF),
  surfaceDim = Color(0xFFF0E2E7),
  surfaceBright = Color(0xFFFFFFFF),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFFFFAFC),
  surfaceContainer = Color(0xFFF6ECEF),
  surfaceContainerHigh = Color(0xFFEFE1E6),
  surfaceContainerHighest = Color(0xFFE8D6DC),
)

val celadonLightScheme: ColorScheme = lightColorScheme(
  primary = Color(0xFF2E8B6E),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFD5EDE4),
  onPrimaryContainer = Color(0xFF0E3528),
  secondary = Color(0xFF4C7568),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFE4F0EA),
  onSecondaryContainer = Color(0xFF1B2D26),
  tertiary = Color(0xFF2E8B6E),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFD5EDE4),
  onTertiaryContainer = Color(0xFF0E3528),
  error = Color(0xFFD6455D),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFFDE8EC),
  onErrorContainer = Color(0xFF5F1422),
  background = Color(0xFFF2F7F4),
  onBackground = Color(0xFF1B2D26),
  surface = Color(0xFFF2F7F4),
  onSurface = Color(0xFF1B2D26),
  surfaceVariant = Color(0xFFE6EFEA),
  onSurfaceVariant = Color(0xFF5B746A),
  outline = Color(0xFF8CA89D),
  outlineVariant = Color(0xFFCEDFD7),
  scrim = Color(0xFF0E1D17),
  inverseSurface = Color(0xFF1B2D26),
  inverseOnSurface = Color(0xFFF2F7F4),
  inversePrimary = Color(0xFF85D4B8),
  surfaceDim = Color(0xFFE1ECE6),
  surfaceBright = Color(0xFFFFFFFF),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFF8FCF9),
  surfaceContainer = Color(0xFFEBF3EE),
  surfaceContainerHigh = Color(0xFFE2ECE7),
  surfaceContainerHighest = Color(0xFFD7E5DE),
)

val amberLightScheme: ColorScheme = lightColorScheme(
  primary = Color(0xFFC2762A),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFF4E1C8),
  onPrimaryContainer = Color(0xFF3E2207),
  secondary = Color(0xFF87684B),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFF3EAD9),
  onSecondaryContainer = Color(0xFF2D241C),
  tertiary = Color(0xFFC2762A),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFF4E1C8),
  onTertiaryContainer = Color(0xFF3E2207),
  error = Color(0xFFD24B4B),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFFCE8E8),
  onErrorContainer = Color(0xFF5C1515),
  background = Color(0xFFFAF6EF),
  onBackground = Color(0xFF2D241C),
  surface = Color(0xFFFAF6EF),
  onSurface = Color(0xFF2D241C),
  surfaceVariant = Color(0xFFF1EADF),
  onSurfaceVariant = Color(0xFF78695B),
  outline = Color(0xFFA89886),
  outlineVariant = Color(0xFFE2D7C8),
  scrim = Color(0xFF1E1710),
  inverseSurface = Color(0xFF2D241C),
  inverseOnSurface = Color(0xFFFAF6EF),
  inversePrimary = Color(0xFFF0B77D),
  surfaceDim = Color(0xFFEDE4D7),
  surfaceBright = Color(0xFFFFFDF9),
  surfaceContainerLowest = Color(0xFFFFFDF9),
  surfaceContainerLow = Color(0xFFFCF9F3),
  surfaceContainer = Color(0xFFF4EEE4),
  surfaceContainerHigh = Color(0xFFEDE5D8),
  surfaceContainerHighest = Color(0xFFE5DBCB),
)

val midnightDarkScheme: ColorScheme = darkColorScheme(
  primary = primaryDark,
  onPrimary = onPrimaryDark,
  primaryContainer = primaryContainerDark,
  onPrimaryContainer = onPrimaryContainerDark,
  secondary = secondaryDark,
  onSecondary = onSecondaryDark,
  secondaryContainer = secondaryContainerDark,
  onSecondaryContainer = onSecondaryContainerDark,
  tertiary = tertiaryDark,
  onTertiary = onTertiaryDark,
  tertiaryContainer = tertiaryContainerDark,
  onTertiaryContainer = onTertiaryContainerDark,
  error = errorDark,
  onError = onErrorDark,
  errorContainer = errorContainerDark,
  onErrorContainer = onErrorContainerDark,
  background = backgroundDark,
  onBackground = onBackgroundDark,
  surface = surfaceDark,
  onSurface = onSurfaceDark,
  surfaceVariant = surfaceVariantDark,
  onSurfaceVariant = onSurfaceVariantDark,
  outline = outlineDark,
  outlineVariant = outlineVariantDark,
  scrim = scrimDark,
  inverseSurface = inverseSurfaceDark,
  inverseOnSurface = inverseOnSurfaceDark,
  inversePrimary = inversePrimaryDark,
  surfaceDim = surfaceDimDark,
  surfaceBright = surfaceBrightDark,
  surfaceContainerLowest = surfaceContainerLowestDark,
  surfaceContainerLow = surfaceContainerLowDark,
  surfaceContainer = surfaceContainerDark,
  surfaceContainerHigh = surfaceContainerHighDark,
  surfaceContainerHighest = surfaceContainerHighestDark,
)

val obsidianDarkScheme: ColorScheme = darkColorScheme(
  primary = Color(0xFF7EB8F2),
  onPrimary = Color(0xFF09131F),
  primaryContainer = Color(0xFF1B2D42),
  onPrimaryContainer = Color(0xFFDFEFFF),
  secondary = Color(0xFF9EB5CC),
  onSecondary = Color(0xFF101D2B),
  secondaryContainer = Color(0xFF1E2936),
  onSecondaryContainer = Color(0xFFDFEFFF),
  tertiary = Color(0xFF7EB8F2),
  onTertiary = Color(0xFF09131F),
  tertiaryContainer = Color(0xFF1B2D42),
  onTertiaryContainer = Color(0xFFDFEFFF),
  error = Color(0xFFEB6E80),
  onError = Color(0xFF3E0913),
  errorContainer = Color(0xFF6B1A2B),
  onErrorContainer = PorcelainErrorSoft,
  background = Color(0xFF080A0F),
  onBackground = Color(0xFFEDF2F8),
  surface = Color(0xFF0E1117),
  onSurface = Color(0xFFEDF2F8),
  surfaceVariant = Color(0xFF1C232E),
  onSurfaceVariant = Color(0xFF8E9BAD),
  outline = Color(0xFF566275),
  outlineVariant = Color(0xFF2A3342),
  scrim = Color(0xFF000000),
  inverseSurface = Color(0xFFEDF2F8),
  inverseOnSurface = Color(0xFF0E1117),
  inversePrimary = Color(0xFF3578B8),
  surfaceDim = Color(0xFF06080C),
  surfaceBright = Color(0xFF232B38),
  surfaceContainerLowest = Color(0xFF05070B),
  surfaceContainerLow = Color(0xFF131820),
  surfaceContainer = Color(0xFF191F29),
  surfaceContainerHigh = Color(0xFF202734),
  surfaceContainerHighest = Color(0xFF283140),
)

// Tinted dark variants when a user on Sakura / Celadon / Amber toggles system/manual Dark mode
val sakuraDarkScheme: ColorScheme = midnightDarkScheme.copy(
  primary = Color(0xFFE88CA8),
  onPrimary = Color(0xFF350E1B),
  primaryContainer = Color(0xFF522334),
  onPrimaryContainer = Color(0xFFFBE1EA),
  secondary = Color(0xFFD1A4B3),
  secondaryContainer = Color(0xFF3E242E),
  tertiary = Color(0xFFE88CA8),
  tertiaryContainer = Color(0xFF522334),
  background = Color(0xFF171115),
  surface = Color(0xFF1D151A),
  surfaceContainerLow = Color(0xFF241B21),
  surfaceContainer = Color(0xFF2B2027),
  surfaceContainerHigh = Color(0xFF34272F),
  surfaceContainerHighest = Color(0xFF3F3039),
  outlineVariant = Color(0xFF473640),
)

val celadonDarkScheme: ColorScheme = midnightDarkScheme.copy(
  primary = Color(0xFF68C7A6),
  onPrimary = Color(0xFF092A1F),
  primaryContainer = Color(0xFF1B4739),
  onPrimaryContainer = Color(0xFFD6F2E8),
  secondary = Color(0xFF9AC2B4),
  secondaryContainer = Color(0xFF223B33),
  tertiary = Color(0xFF68C7A6),
  tertiaryContainer = Color(0xFF1B4739),
  background = Color(0xFF0F1714),
  surface = Color(0xFF141E1B),
  surfaceContainerLow = Color(0xFF1A2622),
  surfaceContainer = Color(0xFF202E29),
  surfaceContainerHigh = Color(0xFF273832),
  surfaceContainerHighest = Color(0xFF30443D),
  outlineVariant = Color(0xFF354A43),
)

val amberDarkScheme: ColorScheme = midnightDarkScheme.copy(
  primary = Color(0xFFE5A45C),
  onPrimary = Color(0xFF311B05),
  primaryContainer = Color(0xFF4E3114),
  onPrimaryContainer = Color(0xFFF9E6CE),
  secondary = Color(0xFFCDB396),
  secondaryContainer = Color(0xFF3D2E20),
  tertiary = Color(0xFFE5A45C),
  tertiaryContainer = Color(0xFF4E3114),
  background = Color(0xFF17130F),
  surface = Color(0xFF1E1914),
  surfaceContainerLow = Color(0xFF251F19),
  surfaceContainer = Color(0xFF2D261F),
  surfaceContainerHigh = Color(0xFF362E25),
  surfaceContainerHighest = Color(0xFF41372D),
  outlineVariant = Color(0xFF493E33),
)
