package live.mehiz.mpvkt.ui.player.controls

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import live.mehiz.mpvkt.bangumi.BangumiManager
import live.mehiz.mpvkt.danmaku.DanmakuManager
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.preferences.AudioChannels
import live.mehiz.mpvkt.preferences.ChapterSkipMode
import live.mehiz.mpvkt.preferences.MobileShaderPreset
import live.mehiz.mpvkt.ui.home.SmartTitleParser
import live.mehiz.mpvkt.ui.player.Decoder
import live.mehiz.mpvkt.ui.player.Panels
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.TrackNode
import live.mehiz.mpvkt.ui.player.controls.components.sheets.AudioTracksSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.BangumiSyncSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.ChapterSkipSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.ChaptersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.DanmakuControlSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.DecodersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.MoreSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.PlaybackSpeedSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.PlaylistSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.ShaderPresetSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.SubtitlesSheet
import live.mehiz.mpvkt.ui.player.manager.DetectedSkipChapter
import live.mehiz.mpvkt.ui.player.manager.PlaylistUiState

@Composable
fun PlayerSheets(
  sheetShown: Sheets,

  // subtitles sheet
  subtitles: ImmutableList<TrackNode>,
  onAddSubtitle: (Uri) -> Unit,
  onSelectSubtitle: (Int) -> Unit,
  // audio sheet
  audioTracks: ImmutableList<TrackNode>,
  onAddAudio: (Uri) -> Unit,
  onSelectAudio: (TrackNode) -> Unit,
  // chapters sheet
  chapter: Segment?,
  chapters: ImmutableList<Segment>,
  onSeekToChapter: (Int) -> Unit,
  // Decoders sheet
  decoder: Decoder,
  onUpdateDecoder: (Decoder) -> Unit,
  // Speed sheet
  speed: Float,
  pitchCorrection: Boolean,
  speedPresets: List<Float>,
  onSpeedChange: (Float) -> Unit,
  onPitchCorrectionChange: (Boolean) -> Unit,
  onAddSpeedPreset: (Float) -> Unit,
  onRemoveSpeedPreset: (Float) -> Unit,
  onResetSpeedPresets: () -> Unit,
  onMakeDefaultSpeed: (Float) -> Unit,
  onResetDefaultSpeed: () -> Unit,
  // Shader preset sheet
  shaderPreset: MobileShaderPreset,
  shaderDeband: Boolean,
  onSelectShaderPreset: (MobileShaderPreset) -> Unit,
  onShaderDebandChange: (Boolean) -> Unit,
  // Chapter skip sheet
  chapterSkipMode: ChapterSkipMode,
  skipOp: Boolean,
  skipEd: Boolean,
  skipPreview: Boolean,
  detectedSkipChapters: List<DetectedSkipChapter>,
  onChapterSkipModeChange: (ChapterSkipMode) -> Unit,
  onSkipOpChange: (Boolean) -> Unit,
  onSkipEdChange: (Boolean) -> Unit,
  onSkipPreviewChange: (Boolean) -> Unit,
  // Playlist sheet
  playlist: PlaylistUiState,
  onSelectPlaylistIndex: (Int) -> Unit,
  // Danmaku & Bangumi managers
  danmakuManager: DanmakuManager,
  bangumiManager: BangumiManager,
  mediaTitle: String,
  // More sheet
  backgroundPlayback: Boolean,
  statisticsPage: Int,
  audioChannels: AudioChannels,
  sleepTimerTimeRemaining: Int,
  onStartSleepTimer: (Int) -> Unit,
  onBackgroundPlaybackChange: (Boolean) -> Unit,
  onStatisticsPageChange: (Int) -> Unit,
  onAudioChannelsChange: (AudioChannels) -> Unit,
  onCustomButtonClick: (CustomButtonEntity) -> Unit,
  onCustomButtonLongClick: (CustomButtonEntity) -> Unit,
  buttons: ImmutableList<CustomButtonEntity>,

  onOpenPanel: (Panels) -> Unit,
  onDismissRequest: () -> Unit,
) {
  when (sheetShown) {
    Sheets.None -> {}

    Sheets.Danmaku -> {
      val context = LocalContext.current
      val dmEnabled by danmakuManager.enabled.collectAsState()
      val dmLoading by danmakuManager.loading.collectAsState()
      val dmSource by danmakuManager.sourceInfo.collectAsState()
      val dmStatus by danmakuManager.statusMessage.collectAsState()
      val dmOffset by danmakuManager.timeOffsetSec.collectAsState()
      val dmSearchResults by danmakuManager.searchResults.collectAsState()
      val dmSelectedAnime by danmakuManager.selectedAnime.collectAsState()
      val dmSearching by danmakuManager.searching.collectAsState()
      val dmSearchError by danmakuManager.searchError.collectAsState()
      val bgmMatch by bangumiManager.match.collectAsState()

      val xmlPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
      ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val displayName = runCatching {
          context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
          }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "本地弹幕.xml"
        danmakuManager.loadLocalXmlUri(uri, displayName)
        onDismissRequest()
      }

      val defaultQuery = bgmMatch?.subjectName?.takeIf { it.isNotBlank() }
        ?: dmSource?.animeTitle?.takeIf { it.isNotBlank() && dmSource?.sourceType == "dandanplay" }
        ?: SmartTitleParser.parse(mediaTitle).animeName

      DanmakuControlSheet(
        enabled = dmEnabled,
        loading = dmLoading,
        sourceInfo = dmSource,
        statusMessage = dmStatus,
        timeOffsetSec = dmOffset,
        initialKeyword = defaultQuery,
        searchResults = dmSearchResults,
        selectedAnime = dmSelectedAnime,
        searching = dmSearching,
        searchError = dmSearchError,
        onToggleEnabled = { danmakuManager.setEnabled(it, showToast = false) },
        onOpenStylePanel = { onOpenPanel(Panels.DanmakuStyle) },
        onImportLocalXml = { xmlPicker.launch(arrayOf("text/xml", "application/xml", "*/*")) },
        onAdjustOffset = danmakuManager::adjustTimeOffset,
        onResetOffset = danmakuManager::resetTimeOffset,
        onSearch = danmakuManager::searchManual,
        onSelectAnime = danmakuManager::openAnimeEpisodes,
        onBackToSearchList = danmakuManager::clearSelectedAnime,
        onSelectEpisode = danmakuManager::selectSearchEpisode,
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.Bangumi -> {
      val user by bangumiManager.user.collectAsState()
      val syncMode by bangumiManager.syncMode.collectAsState()
      val apiServer by bangumiManager.apiServer.collectAsState()
      val match by bangumiManager.match.collectAsState()
      val markedWatched by bangumiManager.markedWatched.collectAsState()
      val busy by bangumiManager.busy.collectAsState()
      val inlineStatus by bangumiManager.inlineStatus.collectAsState()
      val inlineError by bangumiManager.inlineError.collectAsState()
      val searchResults by bangumiManager.searchResults.collectAsState()
      val searching by bangumiManager.searching.collectAsState()

      BangumiSyncSheet(
        user = user,
        syncMode = syncMode,
        apiServer = apiServer,
        match = match,
        markedWatched = markedWatched,
        busy = busy,
        inlineStatus = inlineStatus,
        inlineError = inlineError,
        initialKeyword = bangumiManager.initialSearchKeyword(),
        searchResults = searchResults,
        searching = searching,
        onLoginToken = bangumiManager::loginWithToken,
        onLogout = bangumiManager::logout,
        onSyncModeChange = bangumiManager::setSyncMode,
        onSaveApiServer = bangumiManager::setApiServer,
        onResetApiServer = bangumiManager::resetApiServer,
        onTestConnection = bangumiManager::testMirrorConnection,
        onMarkWatched = { bangumiManager.markCurrentEpisodeWatched(fromAuto = false) },
        onSearchSubjects = bangumiManager::searchSubjects,
        onBindSubject = bangumiManager::bindSubjectToCurrentFolder,
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.ShaderPresets -> {
      ShaderPresetSheet(
        selectedPreset = shaderPreset,
        debandEnabled = shaderDeband,
        onSelectPreset = onSelectShaderPreset,
        onDebandChange = onShaderDebandChange,
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.ChapterSkip -> {
      ChapterSkipSheet(
        mode = chapterSkipMode,
        skipOp = skipOp,
        skipEd = skipEd,
        skipPreview = skipPreview,
        detectedChapters = detectedSkipChapters,
        onModeChange = onChapterSkipModeChange,
        onSkipOpChange = onSkipOpChange,
        onSkipEdChange = onSkipEdChange,
        onSkipPreviewChange = onSkipPreviewChange,
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.Playlist -> {
      PlaylistSheet(
        playlist = playlist,
        onSelectIndex = onSelectPlaylistIndex,
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.SubtitleTracks -> {
      val subtitlesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
      ) {
        if (it == null) return@rememberLauncherForActivityResult
        onAddSubtitle(it)
      }
      SubtitlesSheet(
        tracks = subtitles.toImmutableList(),
        onSelect = onSelectSubtitle,
        onAddSubtitle = { subtitlesPicker.launch(arrayOf("*/*")) },
        onOpenSubtitleSettings = { onOpenPanel(Panels.SubtitleSettings) },
        onOpenSubtitleDelay = { onOpenPanel(Panels.SubtitleDelay) },
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.AudioTracks -> {
      val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
      ) {
        if (it == null) return@rememberLauncherForActivityResult
        onAddAudio(it)
      }
      AudioTracksSheet(
        tracks = audioTracks,
        onSelect = onSelectAudio,
        onAddAudioTrack = { audioPicker.launch(arrayOf("*/*")) },
        onOpenDelayPanel = { onOpenPanel(Panels.AudioDelay) },
        onDismissRequest,
      )
    }

    Sheets.Chapters -> {
      if (chapter == null) return
      ChaptersSheet(
        chapters,
        currentChapter = chapter,
        onClick = { onSeekToChapter(chapters.indexOf(it)) },
        onDismissRequest,
      )
    }

    Sheets.Decoders -> {
      DecodersSheet(
        selectedDecoder = decoder,
        onSelect = onUpdateDecoder,
        onDismissRequest,
      )
    }

    Sheets.More -> {
      MoreSheet(
        backgroundPlayback = backgroundPlayback,
        statisticsPage = statisticsPage,
        audioChannels = audioChannels,
        remainingTime = sleepTimerTimeRemaining,
        onStartTimer = onStartSleepTimer,
        onBackgroundPlaybackChange = onBackgroundPlaybackChange,
        onStatisticsPageChange = onStatisticsPageChange,
        onCustomButtonClick = onCustomButtonClick,
        onCustomButtonLongClick = onCustomButtonLongClick,
        onAudioChannelsChange = onAudioChannelsChange,
        onDismissRequest = onDismissRequest,
        onEnterFiltersPanel = { onOpenPanel(Panels.VideoFilters) },
        customButtons = buttons,
      )
    }

    Sheets.PlaybackSpeed -> {
      PlaybackSpeedSheet(
        speed,
        pitchCorrection = pitchCorrection,
        onPitchCorrectionChange = onPitchCorrectionChange,
        onSpeedChange = onSpeedChange,
        speedPresets = speedPresets,
        onAddSpeedPreset = onAddSpeedPreset,
        onRemoveSpeedPreset = onRemoveSpeedPreset,
        onResetPresets = onResetSpeedPresets,
        onMakeDefault = onMakeDefaultSpeed,
        onResetDefault = onResetDefaultSpeed,
        onDismissRequest = onDismissRequest,
      )
    }
  }
}
