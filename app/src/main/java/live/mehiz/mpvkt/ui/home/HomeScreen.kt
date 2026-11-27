package live.mehiz.mpvkt.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.github.k1rakishou.fsaf.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.bangumi.BangumiLibrarySnapshot
import live.mehiz.mpvkt.bangumi.BangumiLibraryStore
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.ui.preferences.PreferencesScreen
import live.mehiz.mpvkt.ui.utils.LocalBackStack

@Serializable
object HomeScreen : Screen {
  private fun requiredVideoPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.READ_MEDIA_VIDEO
  } else {
    Manifest.permission.READ_EXTERNAL_STORAGE
  }

  private fun hasVideoPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, requiredVideoPermission()) == PackageManager.PERMISSION_GRANTED

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val playbackRepo = App.graph.playbackStateRepository
    val cs = MaterialTheme.colorScheme

    var permissionGranted by remember { mutableStateOf(hasVideoPermission(context)) }
    var folders by remember { mutableStateOf<List<LocalVideoFolder>>(emptyList()) }
    var watchedTitles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var bgmSnapshot by remember {
      mutableStateOf(BangumiLibrarySnapshot(emptyMap(), emptyMap(), emptyMap()))
    }
    var loading by remember { mutableStateOf(permissionGranted) }
    var refreshTick by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
      ActivityResultContracts.RequestPermission(),
    ) { granted ->
      permissionGranted = granted
      if (granted) refreshTick++
    }

    val documentPicker = rememberLauncherForActivityResult(
      ActivityResultContracts.OpenDocument(),
    ) { uri ->
      if (uri != null) playFile(uri.toString(), context)
    }

    val fileManager = remember { FileManager(context) }
    val directoryPicker = rememberLauncherForActivityResult(
      ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
      if (uri != null) {
        fileManager.fromUri(uri)?.getFullPath()?.let { path ->
          backstack.add(FilePickerScreen(path))
        }
      }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
      val nowGranted = hasVideoPermission(context)
      permissionGranted = nowGranted
      if (nowGranted) refreshTick++
    }

    LaunchedEffect(permissionGranted, refreshTick) {
      if (!permissionGranted) {
        loading = false
        return@LaunchedEffect
      }
      loading = folders.isEmpty()
      folders = LocalVideoLibrary.scanFolders(context)
      watchedTitles = withContext(Dispatchers.IO) {
        playbackRepo.getAllPlaybackStates()
          .filter { it.lastPosition > 0 }
          .map { it.mediaTitle }
          .toSet()
      }
      bgmSnapshot = withContext(Dispatchers.IO) {
        BangumiLibraryStore.loadSnapshot(context, App.graph.json)
      }
      loading = false
    }

    Scaffold(
      containerColor = cs.background,
      topBar = {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = cs.background,
            titleContentColor = cs.onSurface,
            actionIconContentColor = cs.onSurface,
          ),
          navigationIcon = {
            Image(
              painter = painterResource(id = R.drawable.ic_launcher_foreground),
              contentDescription = null,
              modifier = Modifier
                .padding(start = 8.dp)
                .size(36.dp),
            )
          },
          title = {
            Text(
              text = stringResource(id = R.string.app_name),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
          },
          actions = {
            if (permissionGranted) {
              IconButton(onClick = { refreshTick++ }) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
              }
            }
            IconButton(onClick = { documentPicker.launch(arrayOf("video/*", "*/*")) }) {
              Icon(Icons.Default.FileOpen, contentDescription = "选择视频")
            }
            IconButton(onClick = { directoryPicker.launch(null) }) {
              Icon(Icons.Default.FolderOpen, contentDescription = "浏览目录")
            }
            IconButton(onClick = { backstack.add(PreferencesScreen) }) {
              Icon(Icons.Default.Settings, contentDescription = "设置")
            }
          },
        )
      },
    ) { padding ->
      when {
        !permissionGranted -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding)
              .padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Surface(
              color = cs.surfaceContainerLow,
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.primaryContainer),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = cs.onPrimaryContainer,
                    modifier = Modifier.size(30.dp),
                  )
                }
                Text(
                  text = "本地番剧库",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = cs.onSurface,
                )
                Text(
                  text = "授权读取本地视频后，将按文件夹自动整理番剧并支持同目录连播与进度记录。",
                  style = MaterialTheme.typography.bodyMedium,
                  color = cs.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                  onClick = { permissionLauncher.launch(requiredVideoPermission()) },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Text("授权扫描本地视频", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                  onClick = { documentPicker.launch(arrayOf("video/*", "*/*")) },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("直接选择单个视频")
                }
              }
            }
          }
        }

        loading -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = cs.primary)
          }
        }

        folders.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding)
              .padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(
                text = "未扫描到本地视频",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "可点击右上角文件图标直接打开视频，或浏览指定目录",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
              )
              Button(
                onClick = { documentPicker.launch(arrayOf("video/*", "*/*")) },
                shape = RoundedCornerShape(12.dp),
              ) {
                Text("选择本地视频")
              }
            }
          }
        }

        else -> {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(folders, key = { it.bucketId }) { folder ->
              val watchedCount = remember(folder.videos, watchedTitles) {
                folder.videos.count { it.displayName in watchedTitles }
              }
              val bangumiName = remember(folder.bucketId, bgmSnapshot) {
                bgmSnapshot.resolveFolderName(folder.bucketId)
              }
              FolderCard(
                folder = folder,
                displayName = bangumiName ?: folder.name,
                watchedCount = watchedCount,
                onClick = {
                  backstack.add(FolderVideosScreen(folder.bucketId, folder.name))
                },
              )
            }
          }
        }
      }
    }
  }

  fun playFile(filepath: String, context: Context) {
    val i = Intent(Intent.ACTION_VIEW, filepath.toUri())
    i.setClass(context, PlayerActivity::class.java)
    context.startActivity(i)
  }
}

@Composable
private fun FolderCard(
  folder: LocalVideoFolder,
  displayName: String,
  watchedCount: Int,
  onClick: () -> Unit,
) {
  val cs = MaterialTheme.colorScheme
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick),
    color = cs.surfaceContainerLow,
    shape = RoundedCornerShape(16.dp),
    shadowElevation = 0.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(RoundedCornerShape(13.dp))
          .background(cs.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Folder,
          contentDescription = null,
          tint = cs.onPrimaryContainer,
          modifier = Modifier.size(24.dp),
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = displayName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = cs.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${folder.videoCount} 集",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
          )
          if (watchedCount > 0) {
            Text(
              text = "已看 $watchedCount",
              style = MaterialTheme.typography.labelSmall,
              color = cs.primary,
              fontWeight = FontWeight.SemiBold,
            )
          }
          if (folder.relativePath.isNotBlank()) {
            Text(
              text = folder.relativePath,
              style = MaterialTheme.typography.labelSmall,
              color = cs.onSurfaceVariant.copy(alpha = 0.78f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Icon(
        imageVector = Icons.Filled.ChevronRight,
        contentDescription = null,
        tint = cs.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
