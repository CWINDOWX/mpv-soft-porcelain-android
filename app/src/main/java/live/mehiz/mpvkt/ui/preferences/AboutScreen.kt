package live.mehiz.mpvkt.ui.preferences

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import compose.icons.SimpleIcons
import compose.icons.simpleicons.Github
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.BuildConfig
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import live.mehiz.mpvkt.ui.home.LocalVideoLibrary
import live.mehiz.mpvkt.ui.theme.spacing
import live.mehiz.mpvkt.ui.utils.LocalBackStack
import live.mehiz.mpvkt.update.GitHubReleaseInfo
import live.mehiz.mpvkt.update.GitHubUpdateChecker
import live.mehiz.mpvkt.update.UpdateCheckResult
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals

@Serializable
object AboutScreen : Screen {
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val scope = rememberCoroutineScope()

    var checkingUpdate by remember { mutableStateOf(false) }
    var updateStatusNote by remember { mutableStateOf<String?>(null) }
    var availableRelease by remember { mutableStateOf<GitHubReleaseInfo?>(null) }

    fun openUrl(url: String) {
      runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
      }.onFailure {
        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
      }
    }

    fun triggerUpdateCheck() {
      if (checkingUpdate) return
      checkingUpdate = true
      updateStatusNote = "正在检查 GitHub 更新…"
      scope.launch {
        when (val result = GitHubUpdateChecker.checkLatestRelease(App.graph.json)) {
          is UpdateCheckResult.UpdateAvailable -> {
            availableRelease = result.release
            updateStatusNote = "发现新版本：${result.release.tagName}"
          }

          is UpdateCheckResult.UpToDate -> {
            updateStatusNote = if (result.latestTag != null) {
              "已是最新版本（最新 ${result.latestTag}）"
            } else {
              "已是最新版本（仓库暂未发布 Release）"
            }
            Toast.makeText(context, "当前已是最新版本 (${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
          }

          is UpdateCheckResult.Error -> {
            updateStatusNote = result.message
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
          }
        }
        checkingUpdate = false
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(id = R.string.pref_about_title)) },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
            }
          },
        )
      },
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .padding(paddingValues)
          .verticalScroll(rememberScrollState()),
      ) {
        Column(
          Modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.spacing.large),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Image(
            painterResource(id = R.drawable.ic_launcher_foreground),
            null,
            modifier = Modifier.size(160.dp),
          )
          Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
          )
        }
        HorizontalDivider()
        ProvidePreferenceLocals {
          Preference(
            title = { Text(text = stringResource(id = R.string.pref_about_app_version)) },
            summary = {
              val versionLine = "v${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_SHA})"
              val note = updateStatusNote ?: "点击检查 GitHub 更新"
              Text(text = "$versionLine · $note")
            },
            widgetContainer = if (checkingUpdate) {
              {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                )
              }
            } else {
              null
            },
            onClick = ::triggerUpdateCheck,
          )
          Preference(
            title = { Text(text = "复制设备与版本信息") },
            summary = { Text(text = "用于反馈问题时附带系统环境") },
            onClick = {
              val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
              cm?.setPrimaryClip(ClipData.newPlainText("device_info", collectDeviceInfo()))
              Toast.makeText(context, "已复制设备信息", Toast.LENGTH_SHORT).show()
            },
          )
          Preference(
            title = { Text(text = stringResource(id = R.string.pref_about_oss_libraries)) },
            onClick = { backstack.add(LibrariesScreen) },
          )
          Preference(
            title = { Text(text = stringResource(id = R.string.pref_about_privacy_policy)) },
            onClick = { openUrl(context.getString(R.string.privacy_policy_url)) },
          )
        }
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
        ) {
          IconButton(onClick = { openUrl(context.getString(R.string.github_repo_url)) }) {
            Icon(imageVector = SimpleIcons.Github, contentDescription = "GitHub")
          }
        }
      }
    }

    availableRelease?.let { release ->
      AlertDialog(
        onDismissRequest = { availableRelease = null },
        title = { Text(text = "发现新版本 ${release.tagName}", fontWeight = FontWeight.Bold) },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 280.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            val sizeText = release.apkSizeBytes?.let { " · ${LocalVideoLibrary.formatSize(it)}" }.orEmpty()
            val dateText = release.publishedAt.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
            Text(
              text = "当前 v${BuildConfig.VERSION_NAME} → 最新 ${release.tagName}$sizeText$dateText",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = release.body.ifBlank { "暂无更新说明，可前往发布页查看详情。" },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
              availableRelease = null
              openUrl(release.apkDownloadUrl ?: release.htmlUrl)
            },
          ) {
            Text(if (release.apkDownloadUrl != null) "下载新版 APK" else "前往发布页")
          }
        },
        dismissButton = {
          Row {
            if (release.apkDownloadUrl != null) {
              TextButton(
                onClick = {
                  availableRelease = null
                  openUrl(release.htmlUrl)
                },
              ) {
                Text("发布页")
              }
            }
            TextButton(onClick = { availableRelease = null }) {
              Text("稍后")
            }
          }
        },
      )
    }
  }
}

@Serializable
object LibrariesScreen : Screen {
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(R.string.pref_about_oss_libraries)) },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
            }
          },
        )
      },
    ) { paddingValues ->
      val libraries by produceLibraries(R.raw.aboutlibraries)
      LibrariesContainer(
        libraries = libraries,
        modifier = Modifier.padding(paddingValues),
      )
    }
  }
}
