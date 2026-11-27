package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.components.LeftSideOvalShape
import live.mehiz.mpvkt.presentation.components.RightSideOvalShape
import live.mehiz.mpvkt.ui.player.Panels
import live.mehiz.mpvkt.ui.player.PlayerUpdates
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.VideoAspect
import live.mehiz.mpvkt.ui.player.controls.components.DoubleTapSeekTriangles
import live.mehiz.mpvkt.ui.theme.playerRippleConfiguration
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class ActiveDragMode {
  None,
  HorizontalSeek,
  VerticalBrightness,
  VerticalVolume,
  Pinch,
}

@Suppress("CyclomaticComplexMethod", "MultipleEmitters")
@Composable
fun GestureHandler(
  viewModel: PlayerViewModel,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
) {
  val playerPreferences = App.graph.playerPreferences
  val audioPreferences = App.graph.audioPreferences
  val panelShown by viewModel.panelShown.collectAsState()
  val allowGesturesInPanels by playerPreferences.allowGesturesInPanels.collectAsState()
  val playback = viewModel.playerManager.playback
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val seekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val isSeekingForwards by viewModel.isSeekingForwards.collectAsState()
  var isDoubleTapSeeking by remember { mutableStateOf(false) }
  LaunchedEffect(seekAmount) {
    delay(800)
    isDoubleTapSeeking = false
    viewModel.updateSeekAmount(0)
    viewModel.updateSeekText(null)
    delay(100)
    viewModel.hideSeekBar()
  }
  val multipleSpeedGesture by playerPreferences.holdForMultipleSpeed.collectAsState()
  val brightnessGesture by playerPreferences.brightnessGesture.collectAsState()
  val volumeGesture by playerPreferences.volumeGesture.collectAsState()
  val swapVolumeAndBrightness by playerPreferences.swapVolumeAndBrightness.collectAsState()
  val seekGesture by playerPreferences.horizontalSeekGesture.collectAsState()
  val preciseSeeking by playerPreferences.preciseSeeking.collectAsState()
  val showSeekbarWhenSeeking by playerPreferences.showSeekBarWhenSeeking.collectAsState()
  var isLongPressing by remember { mutableStateOf(false) }
  val volumeBoostingCap by audioPreferences.volumeBoostCap.collectAsState()
  val haptics = LocalHapticFeedback.current
  Box(
    modifier = modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeGestures)
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = {
            if (controlsShown) viewModel.hideControls() else viewModel.showControls()
          },
          onDoubleTap = {
            if (areControlsLocked || isDoubleTapSeeking) return@detectTapGestures
            if (it.x > size.width * 3 / 5) {
              if (!isSeekingForwards) viewModel.updateSeekAmount(0)
              viewModel.handleRightDoubleTap()
              isDoubleTapSeeking = true
            } else if (it.x < size.width * 2 / 5) {
              if (isSeekingForwards) viewModel.updateSeekAmount(0)
              viewModel.handleLeftDoubleTap()
              isDoubleTapSeeking = true
            } else {
              viewModel.handleCenterDoubleTap()
            }
          },
          onPress = {
            if (panelShown != Panels.None && !allowGesturesInPanels) {
              viewModel.panelShown.update { Panels.None }
            }
            if (!areControlsLocked && isDoubleTapSeeking && seekAmount != 0) {
              if (it.x > size.width * 3 / 5) {
                if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                viewModel.handleRightDoubleTap()
              } else if (it.x < size.width * 2 / 5) {
                if (isSeekingForwards) viewModel.updateSeekAmount(0)
                viewModel.handleLeftDoubleTap()
              } else {
                viewModel.handleCenterDoubleTap()
              }
            } else {
              isDoubleTapSeeking = false
            }
            val press = PressInteraction.Press(
              it.copy(x = if (it.x > size.width * 3 / 5) it.x - size.width * 0.6f else it.x),
            )
            interactionSource.emit(press)
            tryAwaitRelease()
            if (isLongPressing) {
              isLongPressing = false
              playback.releaseHeldSpeed()
              viewModel.playerUpdate.update { PlayerUpdates.None }
            }
            interactionSource.emit(PressInteraction.Release(press))
          },
          onLongPress = {
            if (multipleSpeedGesture == 0f || areControlsLocked) return@detectTapGestures
            if (!isLongPressing && !playback.paused.value) {
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              isLongPressing = true
              playback.holdSpeed(multipleSpeedGesture)
              viewModel.playerUpdate.update { PlayerUpdates.MultipleSpeed }
            }
          },
        )
      }
      .pointerInput(
        areControlsLocked,
        seekGesture,
        brightnessGesture,
        volumeGesture,
        swapVolumeAndBrightness,
        preciseSeeking,
        showSeekbarWhenSeeking,
        volumeBoostingCap,
      ) {
        if (areControlsLocked) return@pointerInput
        var lastPinchTriggerMs = 0L

        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          val downPos = down.position
          val touchSlop = viewConfiguration.touchSlop

          var mode = ActiveDragMode.None
          var seekActive = false
          var startingPosition = 0
          var startingX = 0f
          var wasPlayerAlreadyPaused = false
          var lastSeekTarget = Int.MIN_VALUE

          var startingY = 0f
          var mpvVolumeStartingY = 0f
          var originalVolume = 0
          var originalMPVVolume = 100
          var originalBrightness = 0f

          var cumulativeZoom = 1f

          fun finishHorizontalSeek() {
            if (seekActive) {
              seekActive = false
              viewModel.gestureSeekAmount.update { null }
              viewModel.hideSeekBar()
              if (!wasPlayerAlreadyPaused) viewModel.unpause()
            }
          }

          try {
            while (true) {
              val event = awaitPointerEvent()
              val pressed = event.changes.filter { it.pressed }
              if (pressed.isEmpty()) break

              // Multi-touch (>= 2 fingers): pinch-to-zoom Fit <-> Crop without interfering with 1-finger drags
              if (pressed.size >= 2) {
                if (mode != ActiveDragMode.Pinch) {
                  finishHorizontalSeek()
                  mode = ActiveDragMode.Pinch
                  cumulativeZoom = 1f
                }
                val zoom = event.calculateZoom()
                pressed.forEach { if (it.positionChanged()) it.consume() }
                if (zoom != 1f) {
                  val now = System.currentTimeMillis()
                  if (now - lastPinchTriggerMs >= 500L) {
                    cumulativeZoom *= zoom
                    if (cumulativeZoom >= 1.16f) {
                      if (viewModel.playerManager.video.aspect.value != VideoAspect.Crop) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.changeVideoAspect(VideoAspect.Crop)
                      }
                      cumulativeZoom = 1f
                      lastPinchTriggerMs = now
                    } else if (cumulativeZoom <= 0.86f) {
                      if (viewModel.playerManager.video.aspect.value != VideoAspect.Fit) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.changeVideoAspect(VideoAspect.Fit)
                      }
                      cumulativeZoom = 1f
                      lastPinchTriggerMs = now
                    }
                  }
                }
                continue
              }

              // Wait for final finger release after a pinch so lifting one finger doesn't trigger seek/volume
              if (mode == ActiveDragMode.Pinch) {
                pressed.forEach { if (it.positionChanged()) it.consume() }
                continue
              }

              // Don't interrupt long-press 2x hold speed if the user's thumb drifts slightly
              if (isLongPressing) continue

              val change = pressed.first()
              if (mode == ActiveDragMode.None) {
                val totalDx = change.position.x - downPos.x
                val totalDy = change.position.y - downPos.y
                if (hypot(totalDx, totalDy) < touchSlop) continue

                if (abs(totalDx) >= abs(totalDy)) {
                  if (seekGesture) {
                    val liveDur = playback.duration.value
                    if (liveDur > 0) {
                      mode = ActiveDragMode.HorizontalSeek
                      startingPosition = playback.position.value.coerceIn(0, liveDur)
                      startingX = change.position.x
                      wasPlayerAlreadyPaused = playback.paused.value
                      lastSeekTarget = startingPosition
                      seekActive = true
                      viewModel.pause()
                      change.consume()
                    }
                  }
                } else if (brightnessGesture || volumeGesture) {
                  val onLeftHalf = downPos.x < size.width / 2f
                  val chooseBrightness = when {
                    volumeGesture && brightnessGesture -> if (swapVolumeAndBrightness) !onLeftHalf else onLeftHalf
                    brightnessGesture -> true
                    else -> false
                  }
                  mode = if (chooseBrightness) {
                    ActiveDragMode.VerticalBrightness
                  } else {
                    ActiveDragMode.VerticalVolume
                  }
                  startingY = change.position.y
                  mpvVolumeStartingY = 0f
                  originalVolume = viewModel.currentVolume.value
                  originalMPVVolume = viewModel.playerManager.audio.mpvVolume.value
                  originalBrightness = viewModel.currentBrightness.value
                  change.consume()
                }
              }

              when (mode) {
                ActiveDragMode.HorizontalSeek -> {
                  change.consume()
                  val liveDur = playback.duration.value
                  if (liveDur > 0) {
                    // Swipe right (deltaX > 0) = seek forward, swipe left (deltaX < 0) = seek backward
                    val secondsPerPx = (120f / size.width.coerceAtLeast(1)).coerceIn(0.04f, 0.16f)
                    val deltaX = change.position.x - startingX
                    val targetPos = (startingPosition + (deltaX * secondsPerPx).roundToInt())
                      .coerceIn(0, liveDur)
                    val deltaSec = targetPos - startingPosition
                    viewModel.gestureSeekAmount.update { Pair(startingPosition, deltaSec) }
                    if (targetPos != lastSeekTarget) {
                      lastSeekTarget = targetPos
                      viewModel.seekTo(targetPos, preciseSeeking)
                    }
                    if (showSeekbarWhenSeeking) viewModel.showSeekBar()
                  }
                }

                ActiveDragMode.VerticalBrightness -> {
                  change.consume()
                  val sens = (1f / (size.height * 0.72f).coerceAtLeast(1f)).coerceIn(0.0008f, 0.003f)
                  val newBrightness = calculateNewVerticalGestureValue(
                    originalBrightness,
                    startingY,
                    change.position.y,
                    sens,
                  )
                  viewModel.changeBrightnessTo(newBrightness)
                  viewModel.displayBrightnessSlider()
                }

                ActiveDragMode.VerticalVolume -> {
                  change.consume()
                  val stepDeltaY = change.position.y - change.previousPosition.y
                  val liveVol = viewModel.currentVolume.value
                  val liveMpvVol = viewModel.playerManager.audio.mpvVolume.value
                  val isIncreasingBoost = volumeBoostingCap > 0 &&
                    liveVol >= viewModel.maxVolume &&
                    liveMpvVol - 100 < volumeBoostingCap &&
                    stepDeltaY < 0f
                  val isDecreasingBoost = volumeBoostingCap > 0 &&
                    liveVol >= viewModel.maxVolume &&
                    liveMpvVol - 100 in 1..volumeBoostingCap &&
                    stepDeltaY > 0f

                  if (isIncreasingBoost || isDecreasingBoost) {
                    if (mpvVolumeStartingY == 0f) {
                      startingY = 0f
                      originalVolume = liveVol
                      mpvVolumeStartingY = change.position.y
                    }
                    val boostSens = 0.15f
                    val newMpvVol = calculateNewVerticalGestureValue(
                      originalMPVVolume,
                      mpvVolumeStartingY,
                      change.position.y,
                      boostSens,
                    ).coerceIn(100, volumeBoostingCap + 100)
                    viewModel.changeMPVVolumeTo(newMpvVol)
                  } else {
                    if (startingY == 0f) {
                      mpvVolumeStartingY = 0f
                      originalMPVVolume = liveMpvVol
                      startingY = change.position.y
                    }
                    val volSens = (viewModel.maxVolume.toFloat() / (size.height * 0.68f).coerceAtLeast(1f))
                      .coerceIn(0.015f, 0.06f)
                    val newVol = originalVolume + ((startingY - change.position.y) * volSens).roundToInt()
                    viewModel.changeVolumeTo(newVol)
                  }
                  viewModel.displayVolumeSlider()
                }

                else -> {}
              }
            }
          } finally {
            finishHorizontalSeek()
          }
        }
      },
  )
}

@Composable
fun DoubleTapToSeekOvals(
  amount: Int,
  text: String?,
  showOvals: Boolean,
  showSeekIcon: Boolean,
  showSeekTime: Boolean,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
) {
  val alpha by animateFloatAsState(if (amount == 0) 0f else 0.2f, label = "double_tap_animation_alpha")
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = if (amount > 0) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    CompositionLocalProvider(
      LocalRippleConfiguration provides playerRippleConfiguration,
    ) {
      if (amount != 0) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.4f), // 2 fifths
          contentAlignment = Alignment.Center,
        ) {
          if (showOvals) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(if (amount > 0) RightSideOvalShape else LeftSideOvalShape)
                .background(Color.White.copy(alpha))
                .indication(interactionSource, ripple()),
            )
          }
          if (showSeekIcon || showSeekTime) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              DoubleTapSeekTriangles(isForward = amount > 0)
              Text(
                text = text ?: pluralStringResource(R.plurals.seconds, amount, amount),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = Color.White,
              )
            }
          }
        }
      }
    }
  }
}

fun calculateNewVerticalGestureValue(originalValue: Int, startingY: Float, newY: Float, sensitivity: Float): Int =
  originalValue + ((startingY - newY) * sensitivity).toInt()

fun calculateNewVerticalGestureValue(originalValue: Float, startingY: Float, newY: Float, sensitivity: Float): Float =
  originalValue + ((startingY - newY) * sensitivity)

fun calculateNewHorizontalGestureValue(originalValue: Int, startingX: Float, newX: Float, sensitivity: Float): Int =
  originalValue + ((newX - startingX) * sensitivity).toInt()

fun calculateNewHorizontalGestureValue(originalValue: Float, startingX: Float, newX: Float, sensitivity: Float): Float =
  originalValue + ((newX - startingX) * sensitivity)
