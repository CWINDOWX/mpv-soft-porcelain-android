package live.mehiz.mpvkt.ui.player.manager

import android.content.Context
import `is`.xyz.mpv.MPV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.di.AppGraph

data class PlaylistUiState(
  val uris: List<String> = emptyList(),
  val names: List<String> = emptyList(),
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
  val lua = LuaManager(mpv, playback, graph.playerPreferences, scope)
  val thumbnailer = Thumbnailer(mpv, context, graph.decoderPreferences, scope)

  private val _playlist = MutableStateFlow(PlaylistUiState())
  val playlist = _playlist.asStateFlow()

  internal var onSelectPlaylistIndex: (Int) -> Unit = {}

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
