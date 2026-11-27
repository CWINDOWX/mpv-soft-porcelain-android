package live.mehiz.mpvkt.ui.player.manager

import android.content.Context
import android.net.Uri
import `is`.xyz.mpv.MPV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import live.mehiz.mpvkt.bangumi.BangumiManager
import live.mehiz.mpvkt.danmaku.DanmakuManager
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.di.AppGraph

data class PlaylistUiState(
  val uris: List<String> = emptyList(),
  val names: List<String> = emptyList(),
  val paths: List<String> = emptyList(),
  val folderKey: String? = null,
  val folderName: String? = null,
  val currentIndex: Int = -1,
) {
  val hasPlaylist: Boolean get() = uris.size > 1 && currentIndex in uris.indices
  val hasPrevious: Boolean get() = hasPlaylist && currentIndex > 0
  val hasNext: Boolean get() = hasPlaylist && currentIndex < uris.lastIndex
}

class PlayerManager(private val mpv: MPV, context: Context, graph: AppGraph, scope: CoroutineScope) {
  val playback = PlaybackState(mpv, context, graph.playerPreferences, graph.json, scope)
  val tracks = TracksState(mpv, context, graph.json, scope)
  val subtitles = SubtitleSettings(mpv, graph.subtitlesPreferences, scope)
  val audio = AudioSettings(mpv, context, graph.audioPreferences, scope)
  val video = VideoSettings(mpv, graph.decoderPreferences, graph.advancedPreferences, graph.playerPreferences, scope)
  val shaders = ShaderPresetManager(mpv, context, graph.porcelainPreferences, scope)
  val chapterSkip = ChapterSkipManager(playback, graph.porcelainPreferences, scope)
  val danmaku = DanmakuManager(context, graph.porcelainPreferences, graph.json, scope)
  val bangumi = BangumiManager(context, playback, graph.porcelainPreferences, graph.json, scope)
  val lua = LuaManager(mpv, playback, graph.playerPreferences, scope)
  val thumbnailer = Thumbnailer(mpv, context, graph.decoderPreferences, scope)

  private val _playlist = MutableStateFlow(PlaylistUiState())
  val playlist = _playlist.asStateFlow()

  internal var onSelectPlaylistIndex: (Int) -> Unit = {}

  init {
    // When Bangumi resolves a Chinese title (e.g. for English/Romaji filenames),
    // automatically back-fill danmaku if hash + filename search missed.
    bangumi.onChineseTitleResolved = danmaku::onBangumiChineseTitleFallback
  }

  fun resetForNewFile() {
    chapterSkip.resetForNewFile()
    danmaku.resetForNewFile()
    bangumi.resetForNewFile()
  }

  fun onMediaLoaded(
    uri: Uri?,
    fileName: String,
    folderKey: String?,
    folderName: String?,
    filePath: String?,
  ) {
    shaders.applyCurrent()
    danmaku.onVideoLoaded(uri = uri, fileName = fileName, filePath = filePath)
    bangumi.onVideoLoaded(fileName = fileName, folderKey = folderKey, folderName = folderName)
  }

  fun updatePlaylist(state: PlaylistUiState) {
    _playlist.value = state
  }

  fun playPlaylistIndex(index: Int) {
    onSelectPlaylistIndex(index)
  }

  fun playPrevious() {
    val st = _playlist.value
    if (st.hasPrevious) onSelectPlaylistIndex(st.currentIndex - 1)
  }

  fun playNext() {
    val st = _playlist.value
    if (st.hasNext) onSelectPlaylistIndex(st.currentIndex + 1)
  }

  fun keypress(keyCode: String) {
    mpv.command("keypress", keyCode)
  }

  fun runCustomButton(button: CustomButtonEntity, longPress: Boolean = false) {
    mpv.command("script-message", "call_button_${button.id}" + if (longPress) "_long" else "")
  }
}
