package live.mehiz.mpvkt.ui.preferences

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.danmaku.DanmakuFontOption
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.presentation.preferences.MultiChoiceSegmentedButton
import live.mehiz.mpvkt.ui.theme.AppTheme
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

    val appTheme by preferences.appTheme.collectAsState()
    val darkMode by preferences.darkMode.collectAsState()
    val materialYou by preferences.materialYou.collectAsState()
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
            title = { Text(text = "主题配色") },
          )
          val isSystemDark = isSystemInDarkTheme()
          val isMaterialYouActive = materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
          ThemePaletteGrid(
            selected = appTheme,
            materialYouActive = isMaterialYouActive,
            onSelect = { picked ->
              preferences.appTheme.set(picked)
              if (materialYou) preferences.materialYou.set(false)
              preferences.darkMode.set(if (picked.isDark) DarkMode.Dark else DarkMode.Light)
            },
          )
          if (isMaterialYouActive) {
            Text(
              text = "当前已开启 Material You 动态取色，点击任意内置配色可立即切换",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
          } else if (darkMode == DarkMode.System && appTheme.isDark != isSystemDark) {
            val hint = if (isSystemDark) {
              "当前跟随系统深色模式，已自动以「${appTheme.displayName}」暗色变体显示"
            } else {
              "当前跟随系统浅色模式，暂以「柔雾瓷白」显示，夜间将自动切换为「${appTheme.displayName}」"
            }
            Text(
              text = hint,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
          }

          PreferenceCategory(
            title = { Text(text = stringResource(id = R.string.pref_appearance_category_theme)) },
          )
          MultiChoiceSegmentedButton(
            choices = DarkMode.entries.map { context.getString(it.titleRes) }.toImmutableList(),
            selectedIndices = persistentListOf(DarkMode.entries.indexOf(darkMode)),
            onClick = { index ->
              val mode = DarkMode.entries[index]
              preferences.darkMode.set(mode)
              if (mode == DarkMode.Light && appTheme.isDark) {
                preferences.appTheme.set(AppTheme.Porcelain)
              } else if (mode == DarkMode.Dark && appTheme == AppTheme.Porcelain) {
                preferences.appTheme.set(AppTheme.Midnight)
              }
            },
          )
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
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

@Composable
private fun ThemePaletteGrid(
  selected: AppTheme,
  materialYouActive: Boolean,
  onSelect: (AppTheme) -> Unit,
) {
  val rows = AppTheme.entries.chunked(2)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .alpha(if (materialYouActive) 0.72f else 1f),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    rows.forEach { pair ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        pair.forEach { theme ->
          val isSelected = !materialYouActive && theme == selected
          ThemePaletteCard(
            theme = theme,
            selected = isSelected,
            onClick = { onSelect(theme) },
            modifier = Modifier.weight(1f),
          )
        }
        if (pair.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun ThemePaletteCard(
  theme: AppTheme,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = if (selected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.outlineVariant
  }
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = theme.previewBg,
    border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        // Mini dual-swatch pill (elevated surface + primary accent)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Box(
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .background(theme.previewPrimary),
          )
          Box(
            modifier = Modifier
              .width(24.dp)
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(theme.previewSurface),
          )
        }
        if (selected) {
          Box(
            modifier = Modifier
              .size(20.dp)
              .clip(CircleShape)
              .background(theme.previewPrimary),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = null,
              tint = if (theme.isDark) theme.previewBg else androidx.compose.ui.graphics.Color.White,
              modifier = Modifier.size(13.dp),
            )
          }
        }
      }
      Text(
        text = theme.displayName,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = theme.previewInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = theme.subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = theme.previewInk.copy(alpha = 0.68f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
