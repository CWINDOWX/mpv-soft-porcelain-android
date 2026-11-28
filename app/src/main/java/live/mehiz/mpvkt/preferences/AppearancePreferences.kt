package live.mehiz.mpvkt.preferences

import android.os.Build
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import live.mehiz.mpvkt.preferences.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.preference.getEnum
import live.mehiz.mpvkt.ui.theme.AppTheme
import live.mehiz.mpvkt.ui.theme.DarkMode

@Inject
@SingleIn(AppScope::class)
class AppearancePreferences(preferenceStore: PreferenceStore) {
  val appTheme = preferenceStore.getEnum("appearance_app_theme", AppTheme.Porcelain)
  val darkMode = preferenceStore.getEnum("dark_mode", DarkMode.Light)
  val materialYou = preferenceStore.getBoolean("material_you", false)
  val customFontPath = preferenceStore.getString("appearance_custom_font_path", "")
  val customFontName = preferenceStore.getString("appearance_custom_font_name", "")
}
