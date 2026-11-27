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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `is`.xyz.mpv.Utils
import live.mehiz.mpvkt.preferences.ChapterSkipMode
import live.mehiz.mpvkt.preferences.MobileShaderPreset
import live.mehiz.mpvkt.presentation.components.PlayerSheet
import live.mehiz.mpvkt.ui.home.SmartTitleParser
import live.mehiz.mpvkt.ui.player.manager.DetectedSkipChapter
import live.mehiz.mpvkt.ui.player.manager.PlaylistUiState
import live.mehiz.mpvkt.ui.theme.PorcelainBg
import live.mehiz.mpvkt.ui.theme.PorcelainBlue
import live.mehiz.mpvkt.ui.theme.PorcelainBlueSoft
import live.mehiz.mpvkt.ui.theme.PorcelainInk
import live.mehiz.mpvkt.ui.theme.PorcelainMuted
import live.mehiz.mpvkt.ui.theme.PorcelainOutlineVariant
import live.mehiz.mpvkt.ui.theme.PorcelainSurface

@Composable
fun ShaderPresetSheet(
  selectedPreset: MobileShaderPreset,
  debandEnabled: Boolean,
  onSelectPreset: (MobileShaderPreset) -> Unit,
  onDebandChange: (Boolean) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PlayerSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(PorcelainBg)
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
            tint = PorcelainBlue,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = "画质预设",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PorcelainInk,
          )
        }
        Text(
          text = selectedPreset.title,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = PorcelainBlue,
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      MobileShaderPreset.entries.forEach { preset ->
        val isSelected = preset == selectedPreset
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PorcelainBlue else PorcelainSurface)
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
              color = if (isSelected) PorcelainSurface else PorcelainInk,
            )
            Text(
              text = preset.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = if (isSelected) PorcelainSurface.copy(alpha = 0.85f) else PorcelainMuted,
            )
          }
          Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (isSelected) PorcelainSurface else PorcelainMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
          )
        }
      }

      HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = PorcelainOutlineVariant,
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(PorcelainSurface)
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
            color = PorcelainInk,
          )
          Text(
            text = "平滑暗部与渐变色阶断层",
            style = MaterialTheme.typography.bodySmall,
            color = PorcelainMuted,
          )
        }
        Switch(
          checked = debandEnabled,
          onCheckedChange = onDebandChange,
          colors = SwitchDefaults.colors(
            checkedThumbColor = PorcelainSurface,
            checkedTrackColor = PorcelainBlue,
          ),
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
  PlayerSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(PorcelainBg)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "跳过 OP / ED",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = PorcelainInk,
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
              .background(if (active) PorcelainBlue else PorcelainSurface)
              .clickable { onModeChange(item) }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = item.title,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
              color = if (active) PorcelainSurface else PorcelainInk,
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
        color = PorcelainMuted,
        modifier = Modifier.padding(top = 2.dp),
      )
    }
  }
}

@Composable
private fun SkipToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(PorcelainSurface)
      .clickable { onCheckedChange(!checked) }
      .padding(horizontal = 14.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = PorcelainInk,
    )
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = PorcelainSurface,
        checkedTrackColor = PorcelainBlue,
      ),
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
  PlayerSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(PorcelainBg)
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
          color = PorcelainInk,
        )
        Text(
          text = "${playlist.currentIndex + 1} / ${playlist.names.size}",
          style = MaterialTheme.typography.labelMedium,
          color = PorcelainBlue,
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
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(if (current) PorcelainBlue else PorcelainSurface)
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
                .background(if (current) PorcelainSurface else PorcelainBlueSoft),
              contentAlignment = Alignment.Center,
            ) {
              if (current) {
                Icon(
                  imageVector = Icons.Filled.PlayArrow,
                  contentDescription = null,
                  tint = PorcelainBlue,
                  modifier = Modifier.size(16.dp),
                )
              } else {
                Text(
                  text = parsed.episodeNumber ?: "${index + 1}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = PorcelainBlue,
                  maxLines = 1,
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = parsed.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                color = if (current) PorcelainSurface else PorcelainInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = rawName,
                style = MaterialTheme.typography.labelSmall,
                color = if (current) PorcelainSurface.copy(alpha = 0.8f) else PorcelainMuted,
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
