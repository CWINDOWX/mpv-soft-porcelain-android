package live.mehiz.mpvkt.ui.player.controls.components.sheets

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import live.mehiz.mpvkt.bangumi.BangumiApi
import live.mehiz.mpvkt.bangumi.BangumiMatchState
import live.mehiz.mpvkt.bangumi.BangumiSubject
import live.mehiz.mpvkt.bangumi.BangumiUser
import live.mehiz.mpvkt.preferences.BangumiSyncMode
import live.mehiz.mpvkt.presentation.components.PlayerSheet

@Composable
fun BangumiSyncSheet(
  user: BangumiUser?,
  syncMode: BangumiSyncMode,
  apiServer: String,
  match: BangumiMatchState?,
  markedWatched: Boolean,
  busy: Boolean,
  inlineStatus: String?,
  inlineError: String?,
  initialKeyword: String,
  searchResults: List<BangumiSubject>,
  searching: Boolean,
  onLoginToken: (String) -> Unit,
  onLogout: () -> Unit,
  onSyncModeChange: (BangumiSyncMode) -> Unit,
  onSaveApiServer: (String) -> Unit,
  onResetApiServer: () -> Unit,
  onTestConnection: () -> Unit,
  onMarkWatched: () -> Unit,
  onSearchSubjects: (String) -> Unit,
  onBindSubject: (BangumiSubject) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  val context = LocalContext.current
  var tokenInput by remember { mutableStateOf("") }
  var searchQuery by remember(initialKeyword) { mutableStateOf(initialKeyword) }
  var apiInput by remember(apiServer) { mutableStateOf(apiServer) }

  PlayerSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(cs.surfaceContainer)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Title & Login Status Header
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
            imageVector = if (markedWatched) Icons.Filled.CloudDone else Icons.Filled.CloudSync,
            contentDescription = null,
            tint = cs.primary,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = "Bangumi 同步",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface,
          )
        }
        if (user != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = user.nickname,
              style = MaterialTheme.typography.labelMedium,
              color = cs.primary,
              fontWeight = FontWeight.SemiBold,
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(cs.surfaceBright)
                .clickable(onClick = onLogout)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Text(
                text = "退出",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Inline feedback banners
      if (!inlineStatus.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cs.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
          Text(
            text = inlineStatus,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onPrimaryContainer,
            fontWeight = FontWeight.Medium,
          )
        }
      }
      if (!inlineError.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cs.errorContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
          Text(
            text = inlineError,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onErrorContainer,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      // Custom API / Mirror endpoint input
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(cs.surfaceBright)
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "API / 镜像地址",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(cs.surfaceContainer)
              .clickable(enabled = !busy) {
                if (apiInput.isNotBlank() && apiInput.trim() != apiServer) {
                  onSaveApiServer(apiInput)
                }
                onTestConnection()
              }
              .padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            Text(
              text = "测试连通",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = cs.primary,
            )
          }
        }

        OutlinedTextField(
          value = apiInput,
          onValueChange = { apiInput = it },
          placeholder = { Text("https://api.bgm.tv（可填写镜像地址）") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onSaveApiServer(apiInput) },
            enabled = apiInput.isNotBlank(),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text("保存地址")
          }
          OutlinedButton(
            onClick = onResetApiServer,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text("恢复官方")
          }
        }
      }

      // Login section when logged out
      if (user == null) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceBright)
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "粘贴 Access Token 登录",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
          )
          OutlinedTextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            placeholder = { Text("生成个人令牌并粘贴到此处") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { onLoginToken(tokenInput) },
              enabled = !busy && tokenInput.isNotBlank(),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
            ) {
              if (busy) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = cs.onPrimary,
                )
              } else {
                Text("验证并登录", fontWeight = FontWeight.SemiBold)
              }
            }
            OutlinedButton(
              onClick = {
                runCatching {
                  context.startActivity(Intent(Intent.ACTION_VIEW, BangumiApi.ACCESS_TOKEN_URL.toUri()))
                }
              },
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("获取 Token")
            }
          }
        }
      }

      // Current matched anime card & Mark Watched button
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(cs.surfaceBright)
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = match?.subjectName ?: "尚未识别当前番剧",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Bold,
              color = cs.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            val epDesc = match?.let {
              listOf(it.episodeLabel, it.episodeTitle).filter { s -> s.isNotBlank() }.joinToString(" · ")
            }.orEmpty()
            Text(
              text = epDesc.ifBlank { "可在下方搜索番名并绑定当前文件夹" },
              style = MaterialTheme.typography.bodySmall,
              color = cs.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (match != null) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(cs.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
              Text(
                text = if (match.fromFolderBinding) "已绑定目录" else "自动识别",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = cs.onPrimaryContainer,
              )
            }
          }
        }

        if (user != null && match != null && match.episodeNumber != null) {
          Button(
            onClick = onMarkWatched,
            enabled = !busy && !markedWatched,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
          ) {
            if (busy) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = cs.onPrimary,
              )
            } else {
              Icon(
                imageVector = if (markedWatched) Icons.Filled.Check else Icons.Filled.CloudSync,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (markedWatched) "已同步 ${match.episodeLabel} 为看过" else "标记 ${match.episodeLabel} 为已看",
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      // Sync mode 3-way selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        BangumiSyncMode.entries.forEach { mode ->
          val active = mode == syncMode
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (active) cs.primary else cs.surfaceBright)
              .clickable { onSyncModeChange(mode) }
              .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = mode.title,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
              color = if (active) cs.onPrimary else cs.onSurface,
            )
          }
        }
      }

      HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.7f))

      // Search & Bind Folder section
      Text(
        text = "搜索并绑定当前目录番剧",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = cs.onSurface,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("输入中文或日文番名") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onSearchSubjects(searchQuery) }),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
        )
        Button(
          onClick = { onSearchSubjects(searchQuery) },
          enabled = !searching && searchQuery.isNotBlank(),
          shape = RoundedCornerShape(10.dp),
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

      if (searchResults.isNotEmpty()) {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(searchResults, key = { it.id }) { subject ->
            val selected = match?.subjectId == subject.id
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) cs.primary else cs.surfaceBright)
                .clickable { onBindSubject(subject) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = subject.displayName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = if (selected) cs.onPrimary else cs.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val sub = listOfNotNull(
                  subject.name.takeIf { it.isNotBlank() && it != subject.displayName },
                  subject.date.takeIf { it.isNotBlank() },
                  if (subject.eps > 0) "${subject.eps} 话" else null,
                ).joinToString(" · ")
                if (sub.isNotBlank()) {
                  Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) cs.onPrimary.copy(alpha = 0.85f) else cs.onSurfaceVariant,
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
  }
}

@Composable
fun BangumiEndPromptOverlay(
  visible: Boolean,
  match: BangumiMatchState?,
  onSyncNow: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  AnimatedVisibility(
    visible = visible && match != null,
    enter = slideInVertically { it / 2 } + fadeIn(),
    exit = slideOutVertically { it / 2 } + fadeOut(),
    modifier = modifier,
  ) {
    match?.let { m ->
      Surface(
        color = cs.surfaceContainer.copy(alpha = 0.94f),
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
      ) {
        Row(
          modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(cs.primary)
              .clickable(onClick = onSyncNow)
              .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CloudSync,
              contentDescription = null,
              tint = cs.onPrimary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "同步 ${m.episodeLabel} 已看",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = cs.onPrimary,
            )
          }
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(cs.surfaceBright)
              .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "关闭",
              tint = cs.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }
  }
}
