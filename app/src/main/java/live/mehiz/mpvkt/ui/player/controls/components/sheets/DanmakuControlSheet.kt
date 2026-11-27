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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.danmaku.DanmakuSearchAnime
import live.mehiz.mpvkt.danmaku.DanmakuSearchEpisode
import live.mehiz.mpvkt.danmaku.DanmakuSourceInfo
import live.mehiz.mpvkt.presentation.components.PlayerSheet
import java.util.Locale

@Composable
fun DanmakuControlSheet(
  enabled: Boolean,
  loading: Boolean,
  sourceInfo: DanmakuSourceInfo?,
  statusMessage: String?,
  timeOffsetSec: Float,
  initialKeyword: String,
  searchResults: List<DanmakuSearchAnime>,
  selectedAnime: Pair<DanmakuSearchAnime, List<DanmakuSearchEpisode>>?,
  searching: Boolean,
  searchError: String?,
  onToggleEnabled: (Boolean) -> Unit,
  onOpenStylePanel: () -> Unit,
  onImportLocalXml: () -> Unit,
  onAdjustOffset: (Float) -> Unit,
  onResetOffset: () -> Unit,
  onSearch: (String) -> Unit,
  onSelectAnime: (DanmakuSearchAnime) -> Unit,
  onBackToSearchList: () -> Unit,
  onSelectEpisode: (String, DanmakuSearchEpisode) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  var query by remember(initialKeyword) { mutableStateOf(initialKeyword) }

  PlayerSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(cs.surfaceContainer)
        .padding(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Header row with master toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "弹幕",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface,
          )
          val subtitle = when {
            loading -> "正在加载弹幕…"
            sourceInfo != null -> "${sourceInfo.animeTitle} · ${sourceInfo.episodeTitle} (${sourceInfo.totalCount} 条)"
            !statusMessage.isNullOrBlank() -> statusMessage
            else -> "未加载弹幕，可在下方搜索或导入 XML"
          }
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = if (sourceInfo != null) cs.primary else cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = enabled, onCheckedChange = onToggleEnabled)
      }

      // Quick actions: Style studio panel + Local XML import
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(11.dp))
            .background(cs.primary)
            .clickable(onClick = onOpenStylePanel)
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = null,
            tint = cs.onPrimary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "样式与过滤调节",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = cs.onPrimary,
          )
        }
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(cs.surfaceBright)
            .clickable(onClick = onImportLocalXml)
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = cs.onSurface,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "导入 XML",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
          )
        }
      }

      // Time sync offset stepper
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(11.dp))
          .background(cs.surfaceBright)
          .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "时间偏移",
          style = MaterialTheme.typography.labelMedium,
          color = cs.onSurfaceVariant,
          fontWeight = FontWeight.Medium,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OffsetPill("-1s") { onAdjustOffset(-1.0f) }
          OffsetPill("-0.5s") { onAdjustOffset(-0.5f) }
          Text(
            text = String.format(Locale.US, "%+.1fs", timeOffsetSec),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (timeOffsetSec == 0f) cs.onSurface else cs.primary,
            modifier = Modifier
              .clip(RoundedCornerShape(7.dp))
              .clickable(onClick = onResetOffset)
              .padding(horizontal = 6.dp, vertical = 3.dp),
          )
          OffsetPill("+0.5s") { onAdjustOffset(0.5f) }
          OffsetPill("+1s") { onAdjustOffset(1.0f) }
        }
      }

      HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.7f))

      // Manual Dandanplay search section
      if (selectedAnime != null) {
        val (anime, episodes) = selectedAnime
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          IconButton(onClick = onBackToSearchList, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = cs.onSurface)
          }
          Text(
            text = anime.animeTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "${episodes.size} 话",
            style = MaterialTheme.typography.labelSmall,
            color = cs.primary,
          )
        }

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(episodes, key = { it.episodeId }) { ep ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(cs.surfaceBright)
                .clickable {
                  onSelectEpisode(anime.animeTitle, ep)
                  onDismissRequest()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(7.dp))
                  .background(cs.primaryContainer)
                  .padding(horizontal = 8.dp, vertical = 3.dp),
              ) {
                Text(
                  text = ep.episodeNumber.ifBlank { "#" },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = cs.onPrimaryContainer,
                )
              }
              Text(
                text = ep.episodeTitle.ifBlank { "第${ep.episodeNumber}话" },
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("输入番名搜索弹幕库") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(11.dp),
          )
          Button(
            onClick = { onSearch(query) },
            enabled = !searching && query.isNotBlank(),
            shape = RoundedCornerShape(11.dp),
          ) {
            if (searching) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = cs.onPrimary,
              )
            } else {
              Icon(Icons.Filled.Search, contentDescription = "搜索", modifier = Modifier.size(18.dp))
            }
          }
        }

        if (!searchError.isNullOrBlank()) {
          Text(
            text = searchError,
            style = MaterialTheme.typography.bodySmall,
            color = cs.error,
          )
        }

        if (searchResults.isNotEmpty()) {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 210.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(searchResults, key = { it.bangumiId }) { anime ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(cs.surfaceBright)
                  .clickable { onSelectAnime(anime) }
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = anime.animeTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  val meta = listOfNotNull(
                    anime.typeDescription.takeIf { it.isNotBlank() },
                    if (anime.episodeCount > 0) "共 ${anime.episodeCount} 话" else null,
                  ).joinToString(" · ")
                  if (meta.isNotBlank()) {
                    Text(
                      text = meta,
                      style = MaterialTheme.typography.labelSmall,
                      color = cs.onSurfaceVariant,
                    )
                  }
                }
                Icon(
                  imageVector = Icons.Filled.ChevronRight,
                  contentDescription = null,
                  tint = cs.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OffsetPill(label: String, onClick: () -> Unit) {
  val cs = MaterialTheme.colorScheme
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(7.dp))
      .background(cs.surfaceContainer)
      .clickable(onClick = onClick)
      .padding(horizontal = 7.dp, vertical = 4.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = cs.onSurface,
    )
  }
}
