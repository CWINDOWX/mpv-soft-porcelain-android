package live.mehiz.mpvkt.ui.preferences

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.bangumi.BangumiApi
import live.mehiz.mpvkt.danmaku.DanmakuChConvert
import live.mehiz.mpvkt.preferences.BangumiSyncMode
import live.mehiz.mpvkt.preferences.PorcelainPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.utils.LocalBackStack

@Serializable
object PorcelainSettingsScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val prefs = App.graph.porcelainPreferences
    val bgmApi = remember { BangumiApi(App.graph.json) { prefs.resolveBangumiApiBaseUrl() } }
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    val dmEnabled by prefs.danmakuEnabled.collectAsState()
    val dmAutoLoad by prefs.danmakuAutoLoad.collectAsState()
    val dmApiServer by prefs.danmakuApiServer.collectAsState()
    val dmChConvert by prefs.danmakuChConvert.collectAsState()
    val bgmToken by prefs.bangumiAccessToken.collectAsState()
    val bgmUsername by prefs.bangumiUsername.collectAsState()
    val bgmNickname by prefs.bangumiNickname.collectAsState()
    val bgmSyncMode by prefs.bangumiSyncMode.collectAsState()
    val bgmApiServer by prefs.bangumiApiServer.collectAsState()

    var serverInput by remember(dmApiServer) { mutableStateOf(dmApiServer) }
    var bgmApiInput by remember(bgmApiServer) { mutableStateOf(bgmApiServer) }
    var tokenInput by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val activeBgmApiUrl = remember(bgmApiServer) { prefs.resolveBangumiApiBaseUrl() }

    Scaffold(
      containerColor = cs.background,
      topBar = {
        TopAppBar(
          title = { Text("弹幕与 Bangumi", fontWeight = FontWeight.Bold) },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
            }
          },
        )
      },
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Bangumi Account & Mirror Card
        Surface(
          color = cs.surfaceContainerLow,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Bangumi 账号与镜像站",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = cs.onSurface,
            )

            if (!statusMsg.isNullOrBlank()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(cs.primaryContainer)
                  .padding(horizontal = 12.dp, vertical = 8.dp),
              ) {
                Text(statusMsg!!, style = MaterialTheme.typography.bodySmall, color = cs.onPrimaryContainer)
              }
            }
            if (!errorMsg.isNullOrBlank()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(cs.errorContainer)
                  .padding(horizontal = 12.dp, vertical = 8.dp),
              ) {
                Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = cs.onErrorContainer)
              }
            }

            // Custom Bangumi API / Mirror address
            OutlinedTextField(
              value = bgmApiInput,
              onValueChange = { bgmApiInput = it },
              label = { Text("Bangumi API / 镜像地址") },
              placeholder = { Text("https://api.bgm.tv") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(11.dp),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = {
                  val normalized = PorcelainPreferences.normalizeMirrorUrl(bgmApiInput)
                  if (normalized != null) {
                    bgmApiInput = normalized
                    prefs.bangumiApiServer.set(normalized)
                    errorMsg = null
                    statusMsg = "已保存接口地址：$normalized"
                  } else {
                    errorMsg = "请输入有效的 API / 镜像地址"
                  }
                },
                enabled = bgmApiInput.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
              ) {
                Text("保存地址")
              }
              OutlinedButton(
                onClick = {
                  val def = PorcelainPreferences.DEFAULT_BANGUMI_API
                  bgmApiInput = def
                  prefs.bangumiApiServer.set(def)
                  errorMsg = null
                  statusMsg = "已恢复官方地址：$def"
                },
                shape = RoundedCornerShape(10.dp),
              ) {
                Text("恢复官方")
              }
              OutlinedButton(
                onClick = {
                  val normalized = PorcelainPreferences.normalizeMirrorUrl(bgmApiInput)
                  if (normalized != null && normalized != activeBgmApiUrl) {
                    bgmApiInput = normalized
                    prefs.bangumiApiServer.set(normalized)
                  }
                  val target = prefs.resolveBangumiApiBaseUrl()
                  busy = true
                  statusMsg = "正在测试 $target …"
                  errorMsg = null
                  scope.launch {
                    bgmApi.testConnection()
                      .onSuccess { ms ->
                        statusMsg = "连接正常：$target ($ms ms)"
                      }
                      .onFailure { e ->
                        statusMsg = null
                        errorMsg = "连接失败（$target）：${e.message}"
                      }
                    busy = false
                  }
                },
                enabled = !busy,
                shape = RoundedCornerShape(10.dp),
              ) {
                Text("测试")
              }
            }

            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.6f))

            if (bgmToken.isNotBlank() && bgmUsername.isNotBlank()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column {
                  Text(
                    text = bgmNickname.ifBlank { bgmUsername },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                  )
                  Text(
                    text = "@$bgmUsername · 已登录",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.primary,
                  )
                }
                OutlinedButton(
                  onClick = {
                    prefs.bangumiAccessToken.set("")
                    prefs.bangumiUsername.set("")
                    prefs.bangumiNickname.set("")
                    statusMsg = "已退出 Bangumi 登录"
                    errorMsg = null
                  },
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Text("退出登录")
                }
              }
            } else {
              OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("Access Token") },
                placeholder = { Text("粘贴 Bangumi 个人令牌") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = {
                    busy = true
                    statusMsg = null
                    errorMsg = null
                    scope.launch {
                      bgmApi.verifyToken(tokenInput)
                        .onSuccess { user ->
                          prefs.bangumiAccessToken.set(tokenInput.trim())
                          prefs.bangumiUsername.set(user.username)
                          prefs.bangumiNickname.set(user.nickname)
                          tokenInput = ""
                          statusMsg = "登录成功：${user.nickname} (@${user.username})"
                        }
                        .onFailure { e ->
                          errorMsg = e.message ?: "验证失败"
                        }
                      busy = false
                    }
                  },
                  enabled = !busy && tokenInput.isNotBlank(),
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp),
                ) {
                  if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = cs.onPrimary)
                  } else {
                    Text("验证并登录")
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
                  Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("生成 Token")
                }
              }
            }

            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.6f))

            Text(
              text = "片尾 90 秒同步策略",
              style = MaterialTheme.typography.labelMedium,
              color = cs.onSurfaceVariant,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              BangumiSyncMode.entries.forEach { mode ->
                val active = mode == bgmSyncMode
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) cs.primary else cs.surfaceContainerHigh)
                    .clickable { prefs.bangumiSyncMode.set(mode) }
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
          }
        }

        // Danmaku Engine Card
        Surface(
          color = cs.surfaceContainerLow,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "弹幕引擎设置",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = cs.onSurface,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("默认开启弹幕显示", style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                Text("播放时启用 120Hz 原生弹幕层", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
              }
              Switch(checked = dmEnabled, onCheckedChange = prefs.danmakuEnabled::set)
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("打开视频自动匹配弹幕", style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                Text("依次尝试同目录 XML、16MB 哈希与番名检索", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
              }
              Switch(checked = dmAutoLoad, onCheckedChange = prefs.danmakuAutoLoad::set)
            }

            Text(
              text = "弹幕简繁转换",
              style = MaterialTheme.typography.labelMedium,
              color = cs.onSurfaceVariant,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              DanmakuChConvert.entries.forEach { mode ->
                val active = dmChConvert == mode.code
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) cs.primary else cs.surfaceContainerHigh)
                    .clickable { prefs.danmakuChConvert.set(mode.code) }
                    .padding(vertical = 9.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) cs.onPrimary else cs.onSurface,
                  )
                }
              }
            }

            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.6f))

            OutlinedTextField(
              value = serverInput,
              onValueChange = { serverInput = it },
              label = { Text("弹弹play 中继服务器") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(11.dp),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = {
                  val cleaned = serverInput.trim().trimEnd('/').ifBlank { "https://danmaku-api.152468.xyz" }
                  serverInput = cleaned
                  prefs.danmakuApiServer.set(cleaned)
                  statusMsg = "已保存弹幕服务器地址"
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
              ) {
                Text("保存地址")
              }
              OutlinedButton(
                onClick = {
                  val def = "https://danmaku-api.152468.xyz"
                  serverInput = def
                  prefs.danmakuApiServer.set(def)
                },
                shape = RoundedCornerShape(10.dp),
              ) {
                Text("恢复默认")
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}
