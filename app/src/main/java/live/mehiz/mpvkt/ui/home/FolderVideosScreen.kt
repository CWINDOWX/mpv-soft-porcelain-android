package live.mehiz.mpvkt.ui.home

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.bangumi.BangumiLibrarySnapshot
import live.mehiz.mpvkt.bangumi.BangumiLibraryStore
import live.mehiz.mpvkt.bangumi.ResolvedBangumiTitle
import live.mehiz.mpvkt.database.entities.PlaybackStateEntity
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.utils.LocalBackStack

@Serializable
data class FolderVideosScreen(
  val bucketId: String,
  val folderName: String,
) : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val playbackRepo = App.graph.playbackStateRepository
    val cs = MaterialTheme.colorScheme

    var videos by remember { mutableStateOf<List<LocalVideoItem>>(emptyList()) }
    var statesByTitle by remember { mutableStateOf<Map<String, PlaybackStateEntity>>(emptyMap()) }
    var bgmSnapshot by remember {
      mutableStateOf(BangumiLibrarySnapshot(emptyMap(), emptyMap(), emptyMap()))
    }
    var loading by remember { mutableStateOf(true) }
    var refreshTick by remember { mutableStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
      refreshTick++
    }

    LaunchedEffect(bucketId, refreshTick) {
      loading = videos.isEmpty()
      videos = LocalVideoLibrary.queryFolderVideos(context, bucketId)
      statesByTitle = withContext(Dispatchers.IO) {
        playbackRepo.getAllPlaybackStates().associateBy { it.mediaTitle }
      }
      bgmSnapshot = withContext(Dispatchers.IO) {
        BangumiLibraryStore.loadSnapshot(context, App.graph.json)
      }
      loading = false
    }

    val resolvedFolderTitle = remember(bucketId, bgmSnapshot) {
      bgmSnapshot.resolveFolderName(bucketId) ?: folderName
    }

    Scaffold(
      containerColor = cs.background,
      topBar = {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = cs.background,
            titleContentColor = cs.onSurface,
            navigationIconContentColor = cs.onSurface,
          ),
          title = {
            Column {
              Text(
                text = resolvedFolderTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (!loading) {
                val sub = if (resolvedFolderTitle != folderName) {
                  "${videos.size} 个视频 · $folderName"
                } else {
                  "${videos.size} 个视频"
                }
                Text(
                  text = sub,
                  style = MaterialTheme.typography.bodySmall,
                  color = cs.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          },
          navigationIcon = {
            IconButton(onClick = { backstack.removeLastOrNull() }) {
              Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "返回")
            }
          },
        )
      },
    ) { padding ->
      when {
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

        videos.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "该文件夹下暂无视频",
              color = cs.onSurfaceVariant,
              style = MaterialTheme.typography.bodyLarge,
            )
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
            itemsIndexed(videos, key = { _, v -> v.id }) { index, video ->
              val compositeKey = remember(bucketId, video.displayName) {
                BangumiLibraryStore.videoCompositeKey(bucketId, video.displayName)
              }
              val saved = statesByTitle[compositeKey]
                ?: if (BangumiLibraryStore.isDistinctLegacyFileName(video.displayName)) {
                  statesByTitle[video.displayName]
                } else {
                  null
                }
              val progress = if (saved != null && video.durationMs > 0L && saved.lastPosition > 0) {
                ((saved.lastPosition * 1000f) / video.durationMs.toFloat()).coerceIn(0f, 1f)
              } else {
                0f
              }
              val bangumiTitle = remember(video.displayName, bucketId, bgmSnapshot) {
                bgmSnapshot.resolveVideoTitle(video.displayName, bucketId)
              }
              EpisodeVideoCard(
                video = video,
                bangumiTitle = bangumiTitle,
                progress = progress,
                lastPositionSec = saved?.lastPosition ?: 0,
                onClick = { LocalVideoLibrary.launchPlaylist(context, videos, index) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EpisodeVideoCard(
  video: LocalVideoItem,
  bangumiTitle: ResolvedBangumiTitle?,
  progress: Float,
  lastPositionSec: Int,
  onClick: () -> Unit,
) {
  val cs = MaterialTheme.colorScheme
  val parsed = remember(video.displayName) { SmartTitleParser.parse(video.displayName) }
  val displayTitle = bangumiTitle?.displayTitle ?: parsed.displayTitle
  val episodeBadge = bangumiTitle?.episodeNumber ?: parsed.episodeNumber

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick),
    color = cs.surfaceContainerLow,
    shape = RoundedCornerShape(16.dp),
    shadowElevation = 0.dp,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (progress > 0f) cs.primary else cs.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          if (episodeBadge != null) {
            Text(
              text = "E$episodeBadge",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = if (progress > 0f) cs.onPrimary else cs.onPrimaryContainer,
              maxLines = 1,
            )
          } else {
            Icon(
              imageVector = if (progress > 0f) Icons.Filled.PlayArrow else Icons.Filled.Movie,
              contentDescription = null,
              tint = if (progress > 0f) cs.onPrimary else cs.onPrimaryContainer,
              modifier = Modifier.size(22.dp),
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = displayTitle,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = video.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = LocalVideoLibrary.formatDuration(video.durationMs),
              style = MaterialTheme.typography.labelSmall,
              color = cs.onSurfaceVariant,
            )
            Text(
              text = LocalVideoLibrary.formatSize(video.sizeBytes),
              style = MaterialTheme.typography.labelSmall,
              color = cs.onSurfaceVariant,
            )
            if (lastPositionSec > 0) {
              Text(
                text = "已播 ${LocalVideoLibrary.formatDuration(lastPositionSec * 1000L)}",
                style = MaterialTheme.typography.labelSmall,
                color = cs.primary,
                fontWeight = FontWeight.Medium,
              )
            }
          }
        }
      }

      if (progress > 0f) {
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp),
          color = cs.primary,
          trackColor = cs.surfaceVariant.copy(alpha = 0.7f),
          drawStopIndicator = {},
        )
      }
    }
  }
}
