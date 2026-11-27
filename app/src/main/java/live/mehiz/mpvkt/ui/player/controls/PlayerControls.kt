package live.mehiz.mpvkt.ui.player.controls

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.ChapterSkipMode
import live.mehiz.mpvkt.preferences.MobileShaderPreset
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.preferences.preference.minusAssign
import live.mehiz.mpvkt.preferences.preference.plusAssign
import live.mehiz.mpvkt.ui.custombuttons.getButtons
import live.mehiz.mpvkt.ui.player.Decoder
import live.mehiz.mpvkt.ui.player.Panels
import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.ui.player.PlayerUpdates
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.VideoAspect
import live.mehiz.mpvkt.ui.player.controls.components.BrightnessSlider
import live.mehiz.mpvkt.ui.player.controls.components.ChapterSkipOverlay
import live.mehiz.mpvkt.ui.player.controls.components.MultipleSpeedPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.PorcelainPillButton
import live.mehiz.mpvkt.ui.player.controls.components.PorcelainSpeedPopup
import live.mehiz.mpvkt.ui.player.controls.components.SeekbarWithTimers
import live.mehiz.mpvkt.ui.player.controls.components.TextPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.ThumbnailPreview
import live.mehiz.mpvkt.ui.player.controls.components.VolumeSlider
import live.mehiz.mpvkt.ui.player.controls.components.formatSpeedBadge
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubColorType
import live.mehiz.mpvkt.ui.player.controls.components.sheets.BangumiEndPromptOverlay
import live.mehiz.mpvkt.ui.player.controls.components.sheets.DanmakuStylePanelOverlay
import live.mehiz.mpvkt.ui.player.controls.components.sheets.toFixed
import live.mehiz.mpvkt.ui.theme.playerRippleConfiguration
import live.mehiz.mpvkt.ui.theme.spacing
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Suppress("CompositionLocalAllowlist")
val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

@Composable
@Suppress("CyclomaticComplexMethod", "ViewModelForwarding")
fun PlayerControls(viewModel: PlayerViewModel, onBackPress: () -> Unit, modifier: Modifier = Modifier) {
  val spacing = MaterialTheme.spacing
  val playerPreferences = App.graph.playerPreferences
  val audioPreferences = App.graph.audioPreferences
  val playerManager = viewModel.playerManager
  val playback = playerManager.playback
  val interactionSource = remember { MutableInteractionSource() }
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val seekBarShown by viewModel.seekBarShown.collectAsState()
  val pausedForCache by playback.pausedForCache.collectAsState()
  val paused by playback.paused.collectAsState()
  val duration by playback.duration.collectAsState()
  val position by playback.position.collectAsState()
  val playbackSpeed by playback.speed.collectAsState()
  val gestureSeekAmount by viewModel.gestureSeekAmount.collectAsState()
  val doubleTapSeekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val showDoubleTapOvals by playerPreferences.showDoubleTapOvals.collectAsState()
  val showSeekIcon by playerPreferences.showSeekIcon.collectAsState()
  val showSeekTime by playerPreferences.showSeekTimeWhileSeeking.collectAsState()
  var seekbarDragPosition by remember { mutableStateOf<Float?>(null) }
  val gestureSeekPosition = gestureSeekAmount?.let { (it.first + it.second).toFloat() }
  val seekPreviewPosition = seekbarDragPosition ?: gestureSeekPosition
  val thumbnailer = playerManager.thumbnailer
  val thumbnailAspect by thumbnailer.aspect.collectAsState()
  LaunchedEffect(seekPreviewPosition) { thumbnailer.request(seekPreviewPosition?.roundToInt()) }
  var resetControls by remember { mutableStateOf(true) }
  var speedPopupOpen by remember { mutableStateOf(false) }

  val seekText by viewModel.seekText.collectAsState()
  val currentChapter by playback.chapter.collectAsState()
  val decoder by playerManager.video.decoder.collectAsState()
  val playerTimeToDisappear by playerPreferences.playerTimeToDisappear.collectAsState()
  val chapters by playback.chapters.collectAsState()
  val playlist by playerManager.playlist.collectAsState()
  val shaderPreset by playerManager.shaders.preset.collectAsState()
  val shaderDeband by playerManager.shaders.deband.collectAsState()
  val skipMode by playerManager.chapterSkip.mode.collectAsState()
  val activeSkipPrompt by playerManager.chapterSkip.activePrompt.collectAsState()
  val undoSkipBanner by playerManager.chapterSkip.undoBanner.collectAsState()
  val dmEnabled by playerManager.danmaku.enabled.collectAsState()
  val dmSource by playerManager.danmaku.sourceInfo.collectAsState()
  val dmStyle by playerManager.danmaku.style.collectAsState()
  val bgmMatch by playerManager.bangumi.match.collectAsState()
  val bgmMarkedWatched by playerManager.bangumi.markedWatched.collectAsState()
  val bgmShowEndPrompt by playerManager.bangumi.showEndPrompt.collectAsState()
  val panelShown by viewModel.panelShown.collectAsState()

  LaunchedEffect(playerManager) {
    playerManager.danmaku.onToast = { msg ->
      viewModel.playerUpdate.update { PlayerUpdates.ShowText(msg) }
    }
    playerManager.bangumi.onToast = { msg ->
      viewModel.playerUpdate.update { PlayerUpdates.ShowText(msg) }
    }
  }

  val onOpenSheet: (Sheets) -> Unit = {
    speedPopupOpen = false
    viewModel.sheetShown.update { _ -> it }
    if (it == Sheets.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.panelShown.update { Panels.None }
    }
  }
  val onOpenPanel: (Panels) -> Unit = {
    speedPopupOpen = false
    viewModel.panelShown.update { _ -> it }
    if (it == Panels.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.sheetShown.update { Sheets.None }
    }
  }

  LaunchedEffect(controlsShown) {
    if (!controlsShown) speedPopupOpen = false
  }

  LaunchedEffect(
    controlsShown,
    paused,
    seekbarDragPosition,
    resetControls,
    speedPopupOpen,
  ) {
    if (controlsShown && !paused && seekbarDragPosition == null && !speedPopupOpen) {
      delay(playerTimeToDisappear.toLong().milliseconds)
      viewModel.hideControls()
    }
  }

  BackHandler(enabled = speedPopupOpen) {
    speedPopupOpen = false
  }

  val scrimAlpha by animateFloatAsState(
    if (controlsShown && !areControlsLocked) 0.36f else 0f,
    animationSpec = playerControlsExitAnimationSpec(),
    label = "controls_transparent_overlay",
  )

  GestureHandler(
    viewModel = viewModel,
    interactionSource = interactionSource,
  )
  DoubleTapToSeekOvals(doubleTapSeekAmount, seekText, showDoubleTapOvals, showSeekIcon, showSeekTime, interactionSource)

  val cs = MaterialTheme.colorScheme
  CompositionLocalProvider(
    LocalRippleConfiguration provides playerRippleConfiguration,
    LocalPlayerButtonsClickEvent provides { resetControls = !resetControls },
    LocalContentColor provides cs.onSurface,
  ) {
    CompositionLocalProvider(
      LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
      Box(modifier = modifier.fillMaxSize()) {
        // Click scrim to dismiss inline speed slider without losing controls
        if (speedPopupOpen) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .zIndex(2f)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { speedPopupOpen = false },
              ),
          )
        }

        ConstraintLayout(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.72f),
                0.18f to Color.Transparent,
                0.72f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.78f),
              ),
              alpha = scrimAlpha,
            )
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          val (topLeftControls, topRightControls) = createRefs()
          val (volumeSlider, brightnessSlider) = createRefs()
          val unlockControlsButton = createRef()
          val bottomDock = createRef()
          val speedPopup = createRef()
          val chapterSkipCard = createRef()
          val centerOverlay = createRef()
          val seekbar = createRef()
          val thumbnailPreview = createRef()
          val playerUpdates = createRef()

          val isBrightnessSliderShown by viewModel.isBrightnessSliderShown.collectAsState()
          val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsState()
          val brightness by viewModel.currentBrightness.collectAsState()
          val volume by viewModel.currentVolume.collectAsState()
          val mpvVolume by playerManager.audio.mpvVolume.collectAsState()
          val swapVolumeAndBrightness by playerPreferences.swapVolumeAndBrightness.collectAsState()
          val reduceMotion by playerPreferences.reduceMotion.collectAsState()

          LaunchedEffect(volume, mpvVolume, isVolumeSliderShown) {
            delay(1800)
            if (isVolumeSliderShown) viewModel.isVolumeSliderShown.update { false }
          }
          LaunchedEffect(brightness, isBrightnessSliderShown) {
            delay(1800)
            if (isBrightnessSliderShown) viewModel.isBrightnessSliderShown.update { false }
          }

          AnimatedVisibility(
            isBrightnessSliderShown,
            enter = if (!reduceMotion) {
              slideInHorizontally(playerControlsEnterAnimationSpec()) { if (swapVolumeAndBrightness) -it else it } +
                fadeIn(playerControlsEnterAnimationSpec())
            } else {
              fadeIn(playerControlsEnterAnimationSpec())
            },
            exit = if (!reduceMotion) {
              slideOutHorizontally(playerControlsExitAnimationSpec()) { if (swapVolumeAndBrightness) -it else it } +
                fadeOut(playerControlsExitAnimationSpec())
            } else {
              fadeOut(playerControlsExitAnimationSpec())
            },
            modifier = Modifier.constrainAs(brightnessSlider) {
              if (swapVolumeAndBrightness) {
                start.linkTo(parent.start, 8.dp)
              } else {
                end.linkTo(parent.end, 8.dp)
              }
              top.linkTo(parent.top)
              bottom.linkTo(parent.bottom)
            },
          ) { BrightnessSlider(brightness, 0f..1f) }

          AnimatedVisibility(
            isVolumeSliderShown,
            enter = if (!reduceMotion) {
              slideInHorizontally(playerControlsEnterAnimationSpec()) { if (swapVolumeAndBrightness) it else -it } +
                fadeIn(playerControlsEnterAnimationSpec())
            } else {
              fadeIn(playerControlsEnterAnimationSpec())
            },
            exit = if (!reduceMotion) {
              slideOutHorizontally(playerControlsExitAnimationSpec()) { if (swapVolumeAndBrightness) it else -it } +
                fadeOut(playerControlsExitAnimationSpec())
            } else {
              fadeOut(playerControlsExitAnimationSpec())
            },
            modifier = Modifier.constrainAs(volumeSlider) {
              if (swapVolumeAndBrightness) {
                end.linkTo(parent.end, 8.dp)
              } else {
                start.linkTo(parent.start, 8.dp)
              }
              top.linkTo(parent.top)
              bottom.linkTo(parent.bottom)
            },
          ) {
            val boostCap by audioPreferences.volumeBoostCap.collectAsState()
            val displayVolumeAsPercentage by playerPreferences.displayVolumeAsPercentage.collectAsState()
            VolumeSlider(
              volume,
              mpvVolume = mpvVolume,
              range = 0..viewModel.maxVolume,
              boostRange = if (boostCap > 0) 0..audioPreferences.volumeBoostCap.get() else null,
              displayAsPercentage = displayVolumeAsPercentage,
            )
          }

          val holdForMultipleSpeed by playerPreferences.holdForMultipleSpeed.collectAsState()
          val currentPlayerUpdate by viewModel.playerUpdate.collectAsState()
          val aspectRatio by playerManager.video.aspect.collectAsState()
          LaunchedEffect(currentPlayerUpdate, aspectRatio) {
            if (currentPlayerUpdate is PlayerUpdates.MultipleSpeed || currentPlayerUpdate is PlayerUpdates.None) {
              return@LaunchedEffect
            }
            delay(1800)
            viewModel.playerUpdate.update { PlayerUpdates.None }
          }
          AnimatedVisibility(
            currentPlayerUpdate !is PlayerUpdates.None,
            enter = fadeIn(playerControlsEnterAnimationSpec()),
            exit = fadeOut(playerControlsExitAnimationSpec()),
            modifier = Modifier.constrainAs(playerUpdates) {
              linkTo(parent.start, parent.end)
              linkTo(parent.top, parent.bottom, bias = 0.16f)
            },
          ) {
            when (currentPlayerUpdate) {
              is PlayerUpdates.MultipleSpeed -> MultipleSpeedPlayerUpdate(currentSpeed = holdForMultipleSpeed)
              is PlayerUpdates.AspectRatio -> TextPlayerUpdate(stringResource(aspectRatio.titleRes))
              is PlayerUpdates.ShowText -> TextPlayerUpdate((currentPlayerUpdate as PlayerUpdates.ShowText).value)
              else -> {}
            }
          }

          // Unlock button when screen is locked
          AnimatedVisibility(
            controlsShown && areControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.constrainAs(unlockControlsButton) {
              top.linkTo(parent.top, 4.dp)
              start.linkTo(parent.start, 4.dp)
            },
          ) {
            PorcelainPillButton(
              icon = Icons.Filled.Lock,
              contentDescription = "解锁",
              onClick = { viewModel.unlockControls() },
              active = true,
              size = 42.dp,
            )
          }

          // Center indicator: gesture seek pill, buffering spinner, or subtle play/pause button
          AnimatedVisibility(
            visible = ((controlsShown && !areControlsLocked && !speedPopupOpen) || gestureSeekAmount != null) || pausedForCache,
            enter = fadeIn(playerControlsEnterAnimationSpec()),
            exit = fadeOut(playerControlsExitAnimationSpec()),
            modifier = Modifier.constrainAs(centerOverlay) {
              end.linkTo(parent.end)
              start.linkTo(parent.start)
              top.linkTo(parent.top)
              bottom.linkTo(parent.bottom)
            },
          ) {
            val showLoadingCircle by playerPreferences.showLoadingCircle.collectAsState()
            when {
              gestureSeekAmount != null -> {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.surfaceContainer.copy(alpha = 0.94f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                  Text(
                    text = stringResource(
                      R.string.player_gesture_seek_indicator,
                      if (gestureSeekAmount!!.second >= 0) '+' else '-',
                      Utils.prettyTime(abs(gestureSeekAmount!!.second)),
                      Utils.prettyTime(gestureSeekAmount!!.first + gestureSeekAmount!!.second),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                  )
                }
              }

              pausedForCache && showLoadingCircle -> {
                Box(
                  modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceContainer.copy(alpha = 0.90f)),
                  contentAlignment = Alignment.Center,
                ) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    color = cs.primary,
                    strokeWidth = 3.5.dp,
                  )
                }
              }

              controlsShown && !areControlsLocked && !speedPopupOpen -> {
                Box(
                  modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceContainer.copy(alpha = 0.92f))
                    .clickable(onClick = viewModel::pauseUnpause),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (paused) "播放" else "暂停",
                    tint = cs.onSurface,
                    modifier = Modifier.size(34.dp),
                  )
                }
              }
            }
          }

          // Top bar: Left Smart Title pill
          val mediaTitle by playback.mediaTitle.collectAsState()
          AnimatedVisibility(
            controlsShown && !areControlsLocked,
            enter = if (!reduceMotion) {
              slideInVertically(playerControlsEnterAnimationSpec()) { -it } + fadeIn(playerControlsEnterAnimationSpec())
            } else {
              fadeIn(playerControlsEnterAnimationSpec())
            },
            exit = if (!reduceMotion) {
              slideOutVertically(playerControlsExitAnimationSpec()) { -it } + fadeOut(playerControlsExitAnimationSpec())
            } else {
              fadeOut(playerControlsExitAnimationSpec())
            },
            modifier = Modifier.constrainAs(topLeftControls) {
              top.linkTo(parent.top)
              start.linkTo(parent.start)
              width = Dimension.fillToConstraints
              end.linkTo(topRightControls.start, 10.dp)
            },
          ) {
            TopLeftPlayerControls(
              mediaTitle = mediaTitle,
              richTitle = bgmMatch?.richTopBarTitle,
              onBackClick = onBackPress,
            )
          }

          // Top bar: Right Clock/Battery + Lock + Rotation + PiP + More
          AnimatedVisibility(
            controlsShown && !areControlsLocked,
            enter = if (!reduceMotion) {
              slideInVertically(playerControlsEnterAnimationSpec()) { -it } + fadeIn(playerControlsEnterAnimationSpec())
            } else {
              fadeIn(playerControlsEnterAnimationSpec())
            },
            exit = if (!reduceMotion) {
              slideOutVertically(playerControlsExitAnimationSpec()) { -it } + fadeOut(playerControlsExitAnimationSpec())
            } else {
              fadeOut(playerControlsExitAnimationSpec())
            },
            modifier = Modifier.constrainAs(topRightControls) {
              top.linkTo(parent.top)
              end.linkTo(parent.end)
            },
          ) {
            val activity = LocalActivity.current as? PlayerActivity
            TopRightPlayerControls(
              isPipAvailable = activity?.isPipSupported == true,
              onLockControls = {
                speedPopupOpen = false
                viewModel.lockControls()
              },
              onCycleRotation = viewModel::cycleScreenRotations,
              onPipClick = {
                activity?.let { act ->
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    act.enterPictureInPictureMode(act.createPipParams())
                  } else {
                    @Suppress("DEPRECATION")
                    act.enterPictureInPictureMode()
                  }
                }
              },
              onMoreClick = { onOpenSheet(Sheets.More) },
              onMoreLongClick = { onOpenPanel(Panels.VideoFilters) },
            )
          }

          // Bottom Row 2: Floating Rounded Porcelain Capsule Seekbar
          AnimatedVisibility(
            visible = (controlsShown || seekBarShown) && !areControlsLocked,
            enter = if (!reduceMotion) {
              slideInVertically(playerControlsEnterAnimationSpec()) { it } + fadeIn(playerControlsEnterAnimationSpec())
            } else {
              fadeIn(playerControlsEnterAnimationSpec())
            },
            exit = if (!reduceMotion) {
              slideOutVertically(playerControlsExitAnimationSpec()) { it } + fadeOut(playerControlsExitAnimationSpec())
            } else {
              fadeOut(playerControlsExitAnimationSpec())
            },
            modifier = Modifier.constrainAs(seekbar) {
              bottom.linkTo(parent.bottom)
              start.linkTo(parent.start)
              end.linkTo(parent.end)
              width = Dimension.fillToConstraints
            },
          ) {
            val invertDuration by playerPreferences.invertDuration.collectAsState()
            val readAhead by playback.readAhead.collectAsState()
            val preciseSeeking by playerPreferences.preciseSeeking.collectAsState()
            SeekbarWithTimers(
              position = seekPreviewPosition ?: position.toFloat(),
              duration = duration.toFloat(),
              readAheadValue = readAhead,
              onValueChange = { seekbarDragPosition = it },
              onValueChangeFinished = {
                seekbarDragPosition?.let { viewModel.seekTo(it.roundToInt(), preciseSeeking) }
                seekbarDragPosition = null
              },
              timersInverted = Pair(false, invertDuration),
              durationTimerOnCLick = { playerPreferences.invertDuration.set(!invertDuration) },
              positionTimerOnClick = {},
              chapters = chapters,
            )
          }

          // Bottom Row 1: Floating Frosted Porcelain Controls Dock (matching PC uosc dock!)
          AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = if (!reduceMotion) {
              slideInVertically(playerControlsEnterAnimationSpec()) { it } + fadeIn(playerControlsEnterAnimationSpec())
            } else {
              fadeIn(playerControlsEnterAnimationSpec())
            },
            exit = if (!reduceMotion) {
              slideOutVertically(playerControlsExitAnimationSpec()) { it } + fadeOut(playerControlsExitAnimationSpec())
            } else {
              fadeOut(playerControlsExitAnimationSpec())
            },
            modifier = Modifier
              .zIndex(3f)
              .constrainAs(bottomDock) {
                bottom.linkTo(seekbar.top, 8.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
              },
          ) {
            val isCompact = LocalConfiguration.current.screenWidthDp < 640
            val btnSize = if (isCompact) 34.dp else 38.dp
            val btnGap = if (isCompact) 4.dp else 6.dp
            val isCustomSpeed = abs(playbackSpeed - 1.0f) >= 0.005f
            val isShaderActive = shaderPreset != MobileShaderPreset.None || shaderDeband

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surfaceContainer.copy(alpha = 0.88f))
                .then(if (isCompact) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(btnGap),
            ) {
              // Left: Playback + Folder Playlist + Chapter Skip
              PorcelainPillButton(
                icon = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (paused) "播放" else "暂停",
                onClick = viewModel::pauseUnpause,
                size = btnSize,
              )
              if (playlist.hasPlaylist) {
                PorcelainPillButton(
                  icon = Icons.Filled.SkipPrevious,
                  contentDescription = "上一集",
                  enabled = playlist.hasPrevious,
                  onClick = playerManager::playPrevious,
                  size = btnSize,
                )
                PorcelainPillButton(
                  icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                  contentDescription = "选集",
                  onClick = { onOpenSheet(Sheets.Playlist) },
                  size = btnSize,
                )
                PorcelainPillButton(
                  icon = Icons.Filled.SkipNext,
                  contentDescription = "下一集",
                  enabled = playlist.hasNext,
                  onClick = playerManager::playNext,
                  size = btnSize,
                )
              }
              PorcelainPillButton(
                icon = Icons.Filled.FastForward,
                contentDescription = "跳过 OP/ED",
                active = skipMode != ChapterSkipMode.Off,
                badge = if (skipMode == ChapterSkipMode.Auto) "自动" else null,
                onClick = { onOpenSheet(Sheets.ChapterSkip) },
                size = btnSize,
              )

              if (!isCompact) {
                Spacer(modifier = Modifier.weight(1f))
              }

              // Right: Danmaku + Bangumi + Standalone Speed Slider + Mobile Shaders + Subtitles + Audio + Chapters + Aspect
              val dmBadge = when {
                !dmEnabled -> null
                dmSource != null && dmSource!!.totalCount >= 1000 ->
                  String.format(java.util.Locale.US, "%.1fk", dmSource!!.totalCount / 1000f)
                dmSource != null && dmSource!!.totalCount > 0 -> dmSource!!.totalCount.toString()
                else -> "弹"
              }
              PorcelainPillButton(
                icon = Icons.AutoMirrored.Filled.Comment,
                contentDescription = "弹幕",
                active = dmEnabled && (dmSource != null || panelShown == Panels.DanmakuStyle),
                badge = dmBadge,
                onClick = { onOpenSheet(Sheets.Danmaku) },
                onLongClick = { playerManager.danmaku.toggleEnabled(showToast = true) },
                size = btnSize,
              )
              PorcelainPillButton(
                icon = if (bgmMarkedWatched) Icons.Filled.CloudDone else Icons.Filled.CloudSync,
                contentDescription = "Bangumi 同步",
                active = bgmMarkedWatched || bgmMatch != null,
                badge = if (bgmMarkedWatched) "已看" else null,
                onClick = { onOpenSheet(Sheets.Bangumi) },
                size = btnSize,
              )
              PorcelainPillButton(
                icon = Icons.Filled.Speed,
                contentDescription = "倍速",
                active = isCustomSpeed || speedPopupOpen,
                badge = if (isCustomSpeed) formatSpeedBadge(playbackSpeed) else null,
                onClick = { speedPopupOpen = !speedPopupOpen },
                size = btnSize,
              )
              PorcelainPillButton(
                icon = Icons.Filled.AutoFixHigh,
                contentDescription = "画质预设",
                active = isShaderActive,
                badge = shaderPreset.badge ?: if (shaderDeband) "DB" else null,
                onClick = { onOpenSheet(Sheets.ShaderPresets) },
                size = btnSize,
              )
              PorcelainPillButton(
                icon = Icons.Filled.Subtitles,
                contentDescription = "字幕",
                onClick = { onOpenSheet(Sheets.SubtitleTracks) },
                onLongClick = { onOpenPanel(Panels.SubtitleSettings) },
                size = btnSize,
              )
              PorcelainPillButton(
                icon = Icons.Filled.Audiotrack,
                contentDescription = "音轨",
                onClick = { onOpenSheet(Sheets.AudioTracks) },
                onLongClick = { onOpenPanel(Panels.AudioDelay) },
                size = btnSize,
              )
              if (chapters.isNotEmpty()) {
                PorcelainPillButton(
                  icon = Icons.Filled.Bookmarks,
                  contentDescription = "章节",
                  onClick = { onOpenSheet(Sheets.Chapters) },
                  size = btnSize,
                )
              }
              PorcelainPillButton(
                icon = Icons.Filled.AspectRatio,
                contentDescription = "画面比例",
                active = aspectRatio != VideoAspect.Fit,
                onClick = {
                  viewModel.changeVideoAspect(
                    when (aspectRatio) {
                      VideoAspect.Fit -> VideoAspect.Crop
                      VideoAspect.Crop -> VideoAspect.Stretch
                      VideoAspect.Stretch -> VideoAspect.Fit
                    },
                  )
                },
                size = btnSize,
              )
            }
          }

          // Inline Floating Speed Slider Popup (anchored neatly above bottom dock!)
          AnimatedVisibility(
            visible = speedPopupOpen && controlsShown && !areControlsLocked,
            enter = slideInVertically(tween(150)) { it / 3 } + fadeIn(tween(150)),
            exit = slideOutVertically(tween(130)) { it / 3 } + fadeOut(tween(130)),
            modifier = Modifier
              .zIndex(4f)
              .constrainAs(speedPopup) {
                bottom.linkTo(bottomDock.top, 10.dp)
                end.linkTo(parent.end, 16.dp)
              },
          ) {
            PorcelainSpeedPopup(
              speed = playbackSpeed,
              onSpeedChange = { playback.setSpeed(it.toFixed(2)) },
              onReset = { playback.setSpeed(1.0f) },
            )
          }

          // Floating OP / ED Skip & Bangumi 90s Sync Capsules (work even when main controls are hidden!)
          Box(
            modifier = Modifier
              .zIndex(5f)
              .constrainAs(chapterSkipCard) {
                if (controlsShown && !areControlsLocked) {
                  bottom.linkTo(bottomDock.top, 12.dp)
                } else {
                  bottom.linkTo(parent.bottom, 16.dp)
                }
                end.linkTo(parent.end, 4.dp)
              },
          ) {
            if (!speedPopupOpen && !areControlsLocked && panelShown != Panels.DanmakuStyle) {
              Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                BangumiEndPromptOverlay(
                  visible = bgmShowEndPrompt && !bgmMarkedWatched,
                  match = bgmMatch,
                  onSyncNow = {
                    resetControls = !resetControls
                    playerManager.bangumi.markCurrentEpisodeWatched(fromAuto = false)
                  },
                  onDismiss = {
                    resetControls = !resetControls
                    playerManager.bangumi.dismissEndPrompt()
                  },
                )
                ChapterSkipOverlay(
                  activePrompt = activeSkipPrompt,
                  undoBanner = undoSkipBanner,
                  onSkip = {
                    resetControls = !resetControls
                    playerManager.chapterSkip.skipActive()
                  },
                  onDismiss = {
                    resetControls = !resetControls
                    playerManager.chapterSkip.dismissActive()
                  },
                  onUndo = {
                    resetControls = !resetControls
                    playerManager.chapterSkip.undoLastSkip()
                  },
                )
              }
            }
          }

          val showThumbPreview = seekPreviewPosition != null && !speedPopupOpen
          ThumbnailPreview(
            thumbnailer = thumbnailer,
            visible = showThumbPreview,
            aspect = thumbnailAspect,
            position = seekPreviewPosition ?: position.toFloat(),
            duration = duration.toFloat(),
            modifier = Modifier
              .zIndex(if (showThumbPreview) 6f else 0.5f)
              .constrainAs(thumbnailPreview) {
                bottom.linkTo(if (controlsShown) bottomDock.top else seekbar.top, spacing.small)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
              },
          )
        }
      }
    }
    PlayerSheetsHost(viewModel, chapters, currentChapter, decoder, playbackSpeed, onOpenSheet, onOpenPanel)
    PlayerPanelsHost(viewModel, onOpenPanel)
    if (panelShown == Panels.DanmakuStyle) {
      DanmakuStylePanelOverlay(
        enabled = dmEnabled,
        style = dmStyle,
        onToggleEnabled = { playerManager.danmaku.setEnabled(it, showToast = false) },
        onUpdateStyle = playerManager.danmaku::updateStyle,
        onApplyPreset = playerManager.danmaku::applyPreset,
        onResetStyle = playerManager.danmaku::resetStyle,
        onDismissRequest = { onOpenPanel(Panels.None) },
      )
    }
  }
}

@Composable
@Suppress("ViewModelForwarding")
private fun PlayerSheetsHost(
  viewModel: PlayerViewModel,
  chapters: ImmutableList<Segment>,
  currentChapter: Int,
  decoder: Decoder,
  playbackSpeed: Float,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
) {
  val playerPreferences = App.graph.playerPreferences
  val playerManager = viewModel.playerManager
  val sheetShown by viewModel.sheetShown.collectAsState()
  val subtitles by playerManager.tracks.subtitles.collectAsState()
  val audioTracks by playerManager.tracks.audio.collectAsState()
  val sleepTimerTimeRemaining by playerManager.playback.sleepTimerRemaining.collectAsState()
  val speedPresets by playerPreferences.speedPresets.collectAsState()
  val backgroundPlayback by playerPreferences.automaticBackgroundPlayback.collectAsState()
  val statisticsPage by playerManager.video.statisticsPage.collectAsState()
  val audioChannels by playerManager.audio.channels.collectAsState()
  val pitchCorrection by playerManager.audio.pitchCorrection.collectAsState()
  val customButtons by viewModel.customButtons.collectAsState()
  val shaderPreset by playerManager.shaders.preset.collectAsState()
  val shaderDeband by playerManager.shaders.deband.collectAsState()
  val chapterSkipMode by playerManager.chapterSkip.mode.collectAsState()
  val skipOp by playerManager.chapterSkip.skipOp.collectAsState()
  val skipEd by playerManager.chapterSkip.skipEd.collectAsState()
  val skipPreview by playerManager.chapterSkip.skipPreview.collectAsState()
  val detectedSkipChapters by playerManager.chapterSkip.detectedChapters.collectAsState()
  val playlist by playerManager.playlist.collectAsState()

  PlayerSheets(
    sheetShown = sheetShown,
    subtitles = subtitles,
    onAddSubtitle = playerManager.tracks::addSubtitle,
    onSelectSubtitle = playerManager.tracks::selectSubtitle,
    audioTracks = audioTracks,
    onAddAudio = playerManager.tracks::addAudio,
    onSelectAudio = { playerManager.tracks.toggleAudio(it.id) },
    chapter = chapters.getOrNull(currentChapter),
    chapters = chapters,
    onSeekToChapter = playerManager.playback::seekToChapter,
    decoder = decoder,
    onUpdateDecoder = playerManager.video::setDecoder,
    speed = playbackSpeed,
    onSpeedChange = { playerManager.playback.setSpeed(it.toFixed(2)) },
    onMakeDefaultSpeed = { playerManager.playback.setDefaultSpeed(it.toFixed(2)) },
    onAddSpeedPreset = { playerPreferences.speedPresets += it.toFixed(2).toString() },
    onRemoveSpeedPreset = { playerPreferences.speedPresets -= it.toFixed(2).toString() },
    onResetSpeedPresets = playerPreferences.speedPresets::delete,
    speedPresets = speedPresets.map { it.toFloat() }.sorted(),
    onResetDefaultSpeed = playerManager.playback::resetSpeed,
    // Shader preset sheet
    shaderPreset = shaderPreset,
    shaderDeband = shaderDeband,
    onSelectShaderPreset = {
      playerManager.shaders.selectPreset(it)
      viewModel.playerUpdate.update { _ -> PlayerUpdates.ShowText("画质：${it.title}") }
    },
    onShaderDebandChange = {
      playerManager.shaders.setDeband(it)
      viewModel.playerUpdate.update { _ -> PlayerUpdates.ShowText(if (it) "去色带：开" else "去色带：关") }
    },
    // Chapter skip sheet
    chapterSkipMode = chapterSkipMode,
    skipOp = skipOp,
    skipEd = skipEd,
    skipPreview = skipPreview,
    detectedSkipChapters = detectedSkipChapters,
    onChapterSkipModeChange = playerManager.chapterSkip::setMode,
    onSkipOpChange = playerManager.chapterSkip::setSkipOp,
    onSkipEdChange = playerManager.chapterSkip::setSkipEd,
    onSkipPreviewChange = playerManager.chapterSkip::setSkipPreview,
    // Playlist sheet
    playlist = playlist,
    onSelectPlaylistIndex = playerManager::playPlaylistIndex,
    // Danmaku & Bangumi managers
    danmakuManager = playerManager.danmaku,
    bangumiManager = playerManager.bangumi,
    mediaTitle = playerManager.playback.mediaTitle.collectAsState().value,
    // More sheet state
    backgroundPlayback = backgroundPlayback,
    statisticsPage = statisticsPage,
    audioChannels = audioChannels,
    sleepTimerTimeRemaining = sleepTimerTimeRemaining,
    onStartSleepTimer = playerManager.playback::startSleepTimer,
    onBackgroundPlaybackChange = playerPreferences.automaticBackgroundPlayback::set,
    onStatisticsPageChange = playerManager.video::setStatisticsPage,
    onAudioChannelsChange = playerManager.audio::setChannels,
    onCustomButtonClick = playerManager::runCustomButton,
    onCustomButtonLongClick = { playerManager.runCustomButton(it, longPress = true) },
    buttons = customButtons.getButtons().toImmutableList(),
    onOpenPanel = onOpenPanel,
    onPitchCorrectionChange = playerManager.audio::setPitchCorrection,
    pitchCorrection = pitchCorrection,
    onDismissRequest = { onOpenSheet(Sheets.None) },
  )
}

@Composable
@Suppress("ViewModelForwarding")
private fun PlayerPanelsHost(viewModel: PlayerViewModel, onOpenPanel: (Panels) -> Unit) {
  val subtitles = viewModel.playerManager.subtitles
  val audio = viewModel.playerManager.audio
  val video = viewModel.playerManager.video
  val panel by viewModel.panelShown.collectAsState()
  var subtitleColorType by remember { mutableStateOf(SubColorType.Text) }
  val currentSubtitleColor by subtitles.colors.getValue(subtitleColorType).collectAsState()
  val debandSettings = video.debandSettings.mapValues { it.value.collectAsState().value }
  val filterValues = video.filters.mapValues { it.value.collectAsState().value }

  PlayerPanels(
    panelShown = panel,
    onDismissRequest = { onOpenPanel(Panels.None) },
    // Subtitle settings panel state
    isBold = subtitles.bold.collectAsState().value,
    isItalic = subtitles.italic.collectAsState().value,
    subJustify = subtitles.justification.collectAsState().value,
    subFont = subtitles.font.collectAsState().value,
    subFontSize = subtitles.fontSize.collectAsState().value,
    subBorderStyle = subtitles.borderStyle.collectAsState().value,
    subBorderSize = subtitles.borderSize.collectAsState().value,
    subShadowOffset = subtitles.shadowOffset.collectAsState().value,
    subColor = subtitleColorType,
    currentSubtitleColor = currentSubtitleColor,
    overrideAssSubs = subtitles.overrideAss.collectAsState().value,
    subScale = subtitles.scale.collectAsState().value,
    subPos = subtitles.position.collectAsState().value,
    onSubBoldChange = subtitles::setBold,
    onSubItalicChange = subtitles::setItalic,
    onSubJustifyChange = subtitles::setJustification,
    onSubFontChange = subtitles::setFont,
    onSubFontSizeChange = subtitles::setFontSize,
    onSubBorderStyleChange = subtitles::setBorderStyle,
    onSubBorderSizeChange = subtitles::setBorderSize,
    onSubShadowOffsetChange = subtitles::setShadowOffset,
    onSubColorChange = { subtitles.setColor(subtitleColorType, it) },
    onOverrideAssSubsChange = subtitles::setOverrideAss,
    onSubScaleChange = subtitles::setScale,
    onSubPosChange = subtitles::setPosition,
    onSubColorTypeChange = { subtitleColorType = it },
    onSubColorReset = { subtitles.resetColor(subtitleColorType) },
    onSubtitleSettingsReset = subtitles::resetTypography,
    onSubtitleMiscReset = subtitles::resetMisc,
    subDelayMsPrimary = subtitles.delayMs.collectAsState().value,
    subDelayMsSecondary = subtitles.secondaryDelayMs.collectAsState().value,
    subSpeed = subtitles.speed.collectAsState().value,
    onSubDelayPrimaryChange = subtitles::setDelay,
    onSubDelaySecondaryChange = subtitles::setSecondaryDelay,
    onSubSpeedChange = subtitles::setSpeed,
    onSubDelayApply = subtitles::saveDelaysAsDefault,
    onSubDelayReset = subtitles::resetTiming,
    audioDelayMs = audio.delayMs.collectAsState().value,
    onAudioDelayChange = audio::setDelay,
    onAudioDelayApply = audio::saveDelayAsDefault,
    onAudioDelayReset = audio::resetDelay,
    onDebandChange = video::setDebanding,
    onDebandReset = video::resetDebanding,
    onDebandSettingsChange = video::setDebandSetting,
    onVideoFilterChange = video::setFilter,
    onFilterReset = video::resetFilters,
    deband = video.debanding.collectAsState().value,
    isGpuNextEnabled = video.isGpuNext.collectAsState().value,
    filterValue = { filterValues[it] ?: 0 },
    debandSettings = { debandSettings[it] ?: 0 },
    modifier = Modifier,
  )
}

fun <T> playerControlsExitAnimationSpec(): FiniteAnimationSpec<T> = tween(
  durationMillis = 260,
  easing = FastOutSlowInEasing,
)

fun <T> playerControlsEnterAnimationSpec(): FiniteAnimationSpec<T> = tween(
  durationMillis = 120,
  easing = LinearOutSlowInEasing,
)
