package live.mehiz.mpvkt.ui.preferences

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.danmaku.DanmakuFontOption
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.presentation.preferences.MultiChoiceSegmentedButton
import live.mehiz.mpvkt.ui.theme.CustomFontManager
import live.mehiz.mpvkt.ui.theme.CustomFontSlot
import live.mehiz.mpvkt.ui.theme.DarkMode
import live.mehiz.mpvkt.ui.utils.LocalBackStack
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference

@Serializable
object AppearancePreferencesScreen : Screen {
  @Composable
  override fun Content() {
    val preferences = App.graph.appearancePreferences
    val porcelainPrefs = App.graph.porcelainPreferences
    val context = LocalContext.current
    val backstack = LocalBackStack.current

    val uiFontPath by preferences.customFontPath.collectAsState()
    val uiFontName by preferences.customFontName.collectAsState()
    val dmFontPath by porcelainPrefs.danmakuCustomFontPath.collectAsState()
    val dmFontName by porcelainPrefs.danmakuCustomFontName.collectAsState()

    val uiFontPicker = rememberLauncherForActivityResult(
      ActivityResultContracts.OpenDocument(),
    ) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      CustomFontManager.importFont(context, uri, CustomFontSlot.Ui)
        .onSuccess { imported ->
          preferences.customFontPath.set(imported.path)
          preferences.customFontName.set(imported.displayName)
          Toast.makeText(context, "已切换界面字体：${imported.displayName}", Toast.LENGTH_SHORT).show()
        }
        .onFailure { err ->
          Toast.makeText(context, err.message ?: "字体加载失败", Toast.LENGTH_SHORT).show()
        }
    }

    val dmFontPicker = rememberLauncherForActivityResult(
      ActivityResultContracts.OpenDocument(),
    ) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      CustomFontManager.importFont(context, uri, CustomFontSlot.Danmaku)
        .onSuccess { imported ->
          porcelainPrefs.danmakuCustomFontPath.set(imported.path)
          porcelainPrefs.danmakuCustomFontName.set(imported.displayName)
          porcelainPrefs.danmakuFontChoice.set(DanmakuFontOption.Custom.id)
          Toast.makeText(context, "已切换弹幕字体：${imported.displayName}", Toast.LENGTH_SHORT).show()
        }
        .onFailure { err ->
          Toast.makeText(context, err.message ?: "字体加载失败", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(R.string.pref_appearance_title)) },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding),
        ) {
          PreferenceCategory(
            title = { Text(text = stringResource(id = R.string.pref_appearance_category_theme)) },
          )
          val darkMode by preferences.darkMode.collectAsState()
          MultiChoiceSegmentedButton(
            choices = DarkMode.entries.map { context.getString(it.titleRes) }.toImmutableList(),
            selectedIndices = persistentListOf(DarkMode.entries.indexOf(darkMode)),
            onClick = { preferences.darkMode.set(DarkMode.entries[it]) },
          )
          val materialYou by preferences.materialYou.collectAsState()
          val isMaterialYouAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
          SwitchPreference(
            value = materialYou,
            onValueChange = { preferences.materialYou.set(it) },
            title = { Text(text = stringResource(id = R.string.pref_appearance_material_you_title)) },
            summary = {
              Text(
                text = stringResource(
                  if (isMaterialYouAvailable) {
                    R.string.pref_appearance_material_you_summary
                  } else {
                    R.string.pref_appearance_material_you_summary_disabled
                  },
                ),
              )
            },
            enabled = isMaterialYouAvailable,
          )

          PreferenceCategory(
            title = { Text(text = "字体") },
          )
          Preference(
            title = { Text(text = "界面字体") },
            summary = {
              Text(
                text = if (uiFontPath.isNotBlank()) {
                  "当前：${uiFontName.ifBlank { "自定义字体" }}（点击更换 TTF / OTF）"
                } else {
                  "当前：系统默认（点击导入本地 TTF / OTF 字体）"
                },
              )
            },
            onClick = { uiFontPicker.launch(arrayOf("font/*", "application/octet-stream", "*/*")) },
          )
          if (uiFontPath.isNotBlank()) {
            Preference(
              title = { Text(text = "恢复默认界面字体") },
              summary = { Text(text = "清除已导入的界面字体，改回系统默认") },
              onClick = {
                CustomFontManager.clearFont(context, CustomFontSlot.Ui)
                preferences.customFontPath.set("")
                preferences.customFontName.set("")
                Toast.makeText(context, "已恢复系统默认界面字体", Toast.LENGTH_SHORT).show()
              },
            )
          }
          Preference(
            title = { Text(text = "弹幕自定义字体") },
            summary = {
              Text(
                text = if (dmFontPath.isNotBlank()) {
                  "已导入：${dmFontName.ifBlank { "自定义字体" }}（点击更换，亦可在弹幕样式面板切换）"
                } else {
                  "未导入（默认使用系统字体，点击可独立导入本地 TTF / OTF）"
                },
              )
            },
            onClick = { dmFontPicker.launch(arrayOf("font/*", "application/octet-stream", "*/*")) },
          )
          if (dmFontPath.isNotBlank()) {
            Preference(
              title = { Text(text = "清除弹幕自定义字体") },
              summary = { Text(text = "移除已导入的弹幕字体并改回系统默认") },
              onClick = {
                CustomFontManager.clearFont(context, CustomFontSlot.Danmaku)
                porcelainPrefs.danmakuCustomFontPath.set("")
                porcelainPrefs.danmakuCustomFontName.set("")
                if (porcelainPrefs.danmakuFontChoice.get() == DanmakuFontOption.Custom.id) {
                  porcelainPrefs.danmakuFontChoice.set(DanmakuFontOption.System.id)
                }
                Toast.makeText(context, "已恢复系统默认弹幕字体", Toast.LENGTH_SHORT).show()
              },
            )
          }
        }
      }
    }
  }
}
