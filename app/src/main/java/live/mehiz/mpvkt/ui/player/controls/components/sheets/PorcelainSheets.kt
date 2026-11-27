package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.bangumi.BangumiLibrarySnapshot
import live.mehiz.mpvkt.bangumi.BangumiLibraryStore
import live.mehiz.mpvkt.preferences.ChapterSkipMode
import live.mehiz.mpvkt.preferences.MobileShaderPreset
import live.mehiz.mpvkt.presentation.components.PlayerSheet
import live.mehiz.mpvkt.ui.home.SmartTitleParser
import live.mehiz.mpvkt.ui.player.manager.DetectedSkipChapter
import live.mehiz.mpvkt.ui.player.manager.PlaylistUiState

@Composable
fun ShaderPresetSheet(
  selectedPreset: MobileShaderPreset,
  debandEnabled: Boolean,
  onSelectPreset: (MobileShaderPreset) -> Unit,
  onDebandChange: (Boolean) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  PlayerSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(cs.surfaceContainer)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.AutoFixHigh,
            contentDescription = null,
            tint = cs.primary,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = "画质预设",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface,
          )
        }
        Text(
          text = selectedPreset.title,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = cs.primary,
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      MobileShaderPreset.entries.forEach { preset ->
        val isSelected = preset == selectedPreset
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) cs.primary else cs.surfaceBright)
            .clickable { onSelectPreset(preset) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = preset.title,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) cs.onPrimary else cs.onSurface,
            )
            Text(
              text = preset.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = if (isSelected) cs.onPrimary.copy(alpha = 0.85f) else cs.onSurfaceVariant,
            )
          }
          Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (isSelected) cs.onPrimary else cs.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
          )
        }
      }

      HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = cs.outlineVariant,
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(cs.surfaceBright)
          .clickable { onDebandChange(!debandEnabled) }
          .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "去色带 (Deband)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = cs.onSurface,
          )
          Text(
            text = "平滑暗部与渐变色阶断层",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
          )
        }
        Switch(
          checked = debandEnabled,
          onCheckedChange = onDebandChange,
        )
      }
    }
  }
}

@Composable
fun ChapterSkipSheet(
  mode: ChapterSkipMode,
  skipOp: Boolean,
  skipEd: Boolean,
  skipPreview: Boolean,
  detectedChapters: List<DetectedSkipChapter>,
  onModeChange: (ChapterSkipMode) -> Unit,
  onSkipOpChange: (Boolean) -> Unit,
  onSkipEdChange: (Boolean) -> Unit,
  onSkipPreviewChange: (Boolean) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  PlayerSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(cs.surfaceContainer)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "跳过 OP / ED",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = cs.onSurface,
      )

      // 3-way mode selector pills
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ChapterSkipMode.entries.forEach { item ->
          val active = item == mode
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(11.dp))
              .background(if (active) cs.primary else cs.surfaceBright)
              .clickable { onModeChange(item) }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = item.title,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
              color = if (active) cs.onPrimary else cs.onSurface,
            )
          }
        }
      }

      SkipToggleRow("跳过片头 (OP)", skipOp, onSkipOpChange)
      SkipToggleRow("跳过片尾 (ED)", skipEd, onSkipEdChange)
      SkipToggleRow("跳过预告", skipPreview, onSkipPreviewChange)

      val summaryText = if (detectedChapters.isEmpty()) {
        "当前视频未检测到 OP/ED 章节标注"
      } else {
        "已识别：" + detectedChapters.joinToString(" · ") {
          "${it.kind.label} ${Utils.prettyTime(it.startSec)}-${Utils.prettyTime(it.endSec)}"
        }
      }
      Text(
        text = summaryText,
        style = MaterialTheme.typography.bodySmall,
        color = cs.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp),
      )
    }
  }
}

@Composable
private fun SkipToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  val cs = MaterialTheme.colorScheme
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(cs.surfaceBright)
      .clickable { onCheckedChange(!checked) }
      .padding(horizontal = 14.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = cs.onSurface,
    )
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
  }
}

@Composable
fun PlaylistSheet(
  playlist: PlaylistUiState,
  onSelectIndex: (Int) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  val context = LocalContext.current
  var bgmSnapshot by remember {
    mutableStateOf(BangumiLibrarySnapshot(emptyMap(), emptyMap(), emptyMap()))
  }
  LaunchedEffect(playlist) {
    bgmSnapshot = withContext(Dispatchers.IO) {
      BangumiLibraryStore.loadSnapshot(context, App.graph.json)
    }
  }
  PlayerSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(cs.surfaceContainer)
        .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "选集列表",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = cs.onSurface,
        )
        Text(
          text = "${playlist.currentIndex + 1} / ${playlist.names.size}",
          style = MaterialTheme.typography.labelMedium,
          color = cs.primary,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        itemsIndexed(playlist.names) { index, rawName ->
          val current = index == playlist.currentIndex
          val parsed = SmartTitleParser.parse(rawName)
          val resolved = bgmSnapshot.resolveVideoTitle(rawName, playlist.folderKey)
          val epNumBadge = resolved?.episodeNumber ?: parsed.episodeNumber ?: "${index + 1}"
          val itemTitle = resolved?.displayTitle ?: parsed.displayTitle
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(if (current) cs.primary else cs.surfaceBright)
              .clickable {
                onSelectIndex(index)
                onDismissRequest()
              }
              .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (current) cs.onPrimary else cs.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              if (current) {
                Icon(
                  imageVector = Icons.Filled.PlayArrow,
                  contentDescription = null,
                  tint = cs.primary,
                  modifier = Modifier.size(16.dp),
                )
              } else {
                Text(
                  text = epNumBadge,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = cs.onPrimaryContainer,
                  maxLines = 1,
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = itemTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                color = if (current) cs.onPrimary else cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = rawName,
                style = MaterialTheme.typography.labelSmall,
                color = if (current) cs.onPrimary.copy(alpha = 0.8f) else cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}
