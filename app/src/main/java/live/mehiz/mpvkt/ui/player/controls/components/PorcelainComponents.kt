package live.mehiz.mpvkt.ui.player.controls.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import live.mehiz.mpvkt.ui.player.controls.LocalPlayerButtonsClickEvent
import live.mehiz.mpvkt.ui.player.manager.DetectedSkipChapter
import live.mehiz.mpvkt.ui.player.manager.UndoSkipInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PorcelainPillButton(
  icon: ImageVector,
  contentDescription: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  active: Boolean = false,
  enabled: Boolean = true,
  badge: String? = null,
  size: Dp = 38.dp,
) {
  val cs = MaterialTheme.colorScheme
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val bgColor = when {
    !enabled -> cs.surfaceBright.copy(alpha = 0.45f)
    active -> cs.primary
    else -> cs.surfaceBright.copy(alpha = 0.94f)
  }
  val iconColor = when {
    !enabled -> cs.onSurfaceVariant.copy(alpha = 0.5f)
    active -> cs.onPrimary
    else -> cs.onSurface
  }

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(size)
        .clip(RoundedCornerShape(11.dp))
        .background(bgColor)
        .combinedClickable(
          enabled = enabled,
          onClick = {
            clickEvent()
            onClick()
          },
          onLongClick = onLongClick?.let { cb ->
            {
              clickEvent()
              cb()
            }
          },
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = iconColor,
        modifier = Modifier
          .size(if (badge != null) 18.dp else 20.dp)
          .offset(y = if (badge != null) (-2).dp else 0.dp),
      )
    }

    if (!badge.isNullOrBlank()) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = 2.dp, y = 2.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(if (active) cs.onPrimary else cs.primary)
          .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = badge,
          fontSize = 8.5.sp,
          lineHeight = 9.5.sp,
          fontWeight = FontWeight.Bold,
          color = if (active) cs.primary else cs.onPrimary,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
fun PorcelainClockBatteryPill(modifier: Modifier = Modifier) {
  val cs = MaterialTheme.colorScheme
  val context = LocalContext.current
  var timeText by remember { mutableStateOf("") }
  var batteryPct by remember { mutableIntStateOf(-1) }
  var isCharging by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    while (true) {
      timeText = fmt.format(Date())
      delay(15_000L)
    }
  }

  DisposableEffect(context) {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level >= 0 && scale > 0) {
          batteryPct = ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
        }
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
          status == BatteryManager.BATTERY_STATUS_FULL
      }
    }
    val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    receiver.onReceive(context, sticky)
    onDispose {
      runCatching { context.unregisterReceiver(receiver) }
    }
  }

  Row(
    modifier = modifier
      .clip(RoundedCornerShape(11.dp))
      .background(cs.surfaceContainer.copy(alpha = 0.90f))
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text = timeText,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = cs.onSurface,
    )
    if (batteryPct >= 0) {
      Text(
        text = "·",
        style = MaterialTheme.typography.labelMedium,
        color = cs.onSurfaceVariant,
      )
      Icon(
        imageVector = when {
          isCharging -> Icons.Filled.BatteryChargingFull
          batteryPct >= 85 -> Icons.Filled.BatteryFull
          else -> Icons.Filled.BatteryStd
        },
        contentDescription = null,
        tint = if (isCharging) cs.primary else cs.onSurface,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = "$batteryPct%",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = cs.onSurface,
      )
    }
  }
}

private data class SpeedSnap(
  val value: Float,
  val label: String?,
  val major: Boolean,
  val radius: Float,
)

private val SPEED_SNAPS = listOf(
  SpeedSnap(0.25f, "0.25x", true, 0.024f),
  SpeedSnap(0.5f, "0.5x", true, 0.024f),
  SpeedSnap(0.75f, null, false, 0.015f),
  SpeedSnap(1.0f, "1.0x", true, 0.030f),
  SpeedSnap(1.25f, null, false, 0.015f),
  SpeedSnap(1.5f, null, false, 0.015f),
  SpeedSnap(2.0f, "2.0x", true, 0.024f),
  SpeedSnap(3.0f, null, false, 0.015f),
  SpeedSnap(4.0f, "4.0x", true, 0.024f),
)

private const val SPEED_MIN = 0.25f
private const val SPEED_MAX = 4.0f
private const val LOG2_MIN = -2f
private const val LOG2_SPAN = 4f

private fun speedToNorm(speed: Float): Float {
  val safe = speed.coerceIn(SPEED_MIN, SPEED_MAX)
  val log2 = (ln(safe.toDouble()) / ln(2.0)).toFloat()
  return ((log2 - LOG2_MIN) / LOG2_SPAN).coerceIn(0f, 1f)
}

private fun normToSpeed(tRaw: Float): Pair<Float, Boolean> {
  val t = tRaw.coerceIn(0f, 1f)
  var bestSnap: SpeedSnap? = null
  var bestDist = Float.MAX_VALUE
  for (sp in SPEED_SNAPS) {
    val st = speedToNorm(sp.value)
    val d = abs(t - st)
    if (d < sp.radius && d < bestDist) {
      bestDist = d
      bestSnap = sp
    }
  }
  if (bestSnap != null) return bestSnap.value to true
  val raw = 2.0.pow((LOG2_MIN + t * LOG2_SPAN).toDouble()).toFloat()
  val step = if (raw >= 2.0f) 0.1f else 0.05f
  val quantized = (raw / step).roundToInt() * step
  val rounded = (quantized * 100f).roundToInt() / 100f
  return rounded.coerceIn(SPEED_MIN, SPEED_MAX) to false
}

fun formatSpeedReadout(speed: Float): String {
  val s = String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')
  return if (s.contains('.')) "${s}x" else "${s}.0x"
}

fun formatSpeedBadge(speed: Float): String {
  val s = String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')
  return "${s}x"
}

@Composable
fun PorcelainSpeedPopup(
  speed: Float,
  onSpeedChange: (Float) -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val haptics = LocalHapticFeedback.current
  val isCustom = abs(speed - 1.0f) >= 0.005f
  val norm = speedToNorm(speed)
  var lastSnappedValue by remember { mutableStateOf<Float?>(null) }

  val trackColor = cs.outlineVariant
  val activeColor = cs.primary
  val mutedColor = cs.onSurfaceVariant
  val surfaceBrightColor = cs.surfaceBright
  val primaryContainerColor = cs.primaryContainer
  val shadowColor = cs.onSurface.copy(alpha = 0.14f)

  Surface(
    modifier = modifier
      .width(328.dp)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
      ),
    color = cs.surfaceContainer.copy(alpha = 0.96f),
    shape = RoundedCornerShape(16.dp),
    shadowElevation = 8.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "倍速",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = cs.onSurface,
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = formatSpeedReadout(speed),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCustom) cs.primary else cs.onSurface,
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(
                if (isCustom) cs.surfaceBright else cs.surfaceBright.copy(alpha = 0.45f),
              )
              .clickable(enabled = isCustom) {
                clickEvent()
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onReset()
              }
              .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "重置",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = if (isCustom) cs.onSurface else cs.onSurfaceVariant.copy(alpha = 0.5f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Tactile logarithmic slider with magnetic snap points
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(34.dp)
          .pointerInput(Unit) {
            detectTapGestures(
              onDoubleTap = {
                clickEvent()
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onReset()
              },
              onTap = { offset ->
                clickEvent()
                val padPx = 10.dp.toPx()
                val trackW = (size.width - padPx * 2).coerceAtLeast(1f)
                val t = ((offset.x - padPx) / trackW).coerceIn(0f, 1f)
                val (newVal, snapped) = normToSpeed(t)
                if (snapped) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSpeedChange(newVal)
              },
            )
          }
          .pointerInput(Unit) {
            detectHorizontalDragGestures(
              onDragStart = { offset ->
                clickEvent()
                val padPx = 10.dp.toPx()
                val trackW = (size.width - padPx * 2).coerceAtLeast(1f)
                val t = ((offset.x - padPx) / trackW).coerceIn(0f, 1f)
                val (newVal, snapped) = normToSpeed(t)
                lastSnappedValue = if (snapped) newVal else null
                onSpeedChange(newVal)
              },
              onHorizontalDrag = { change, _ ->
                change.consume()
                clickEvent()
                val padPx = 10.dp.toPx()
                val trackW = (size.width - padPx * 2).coerceAtLeast(1f)
                val t = ((change.position.x - padPx) / trackW).coerceIn(0f, 1f)
                val (newVal, snapped) = normToSpeed(t)
                if (snapped && lastSnappedValue != newVal) {
                  lastSnappedValue = newVal
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else if (!snapped) {
                  lastSnappedValue = null
                }
                onSpeedChange(newVal)
              },
            )
          },
      ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
          val thumbR = 9.dp.toPx()
          val trackH = 6.dp.toPx()
          val trAx = thumbR
          val trBx = size.width - thumbR
          val trW = (trBx - trAx).coerceAtLeast(1f)
          val cy = size.height / 2f
          val fillX = trAx + trW * norm

          // Base track
          drawRoundRect(
            color = trackColor,
            topLeft = Offset(trAx, cy - trackH / 2f),
            size = Size(trW, trackH),
            cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
          )
          // Active primary fill
          if (fillX > trAx + 1f) {
            drawRoundRect(
              color = activeColor,
              topLeft = Offset(trAx, cy - trackH / 2f),
              size = Size(fillX - trAx, trackH),
              cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
            )
          }

          // Snap dots
          val majorR = 3.5.dp.toPx()
          val minorR = 2.2.dp.toPx()
          for (sp in SPEED_SNAPS) {
            val st = speedToNorm(sp.value)
            val sx = trAx + trW * st
            val passed = st <= norm + 0.005f
            if (sp.major) {
              drawCircle(
                color = if (passed) activeColor else mutedColor.copy(alpha = 0.55f),
                radius = majorR + 1.2.dp.toPx(),
                center = Offset(sx, cy),
              )
              drawCircle(
                color = surfaceBrightColor,
                radius = majorR - 0.3.dp.toPx(),
                center = Offset(sx, cy),
              )
            } else {
              drawCircle(
                color = if (passed) primaryContainerColor else surfaceBrightColor.copy(alpha = 0.90f),
                radius = minorR,
                center = Offset(sx, cy),
              )
            }
          }

          // Concentric smooth thumb handle
          drawCircle(
            color = shadowColor,
            radius = thumbR + 1.5.dp.toPx(),
            center = Offset(fillX, cy + 1.dp.toPx()),
          )
          drawCircle(
            color = activeColor,
            radius = thumbR,
            center = Offset(fillX, cy),
          )
          drawCircle(
            color = surfaceBrightColor,
            radius = thumbR - 2.6.dp.toPx(),
            center = Offset(fillX, cy),
          )
        }
      }

      // Major tick labels evenly at 0 / 25 / 50 / 75 / 100%
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        SPEED_SNAPS.filter { it.major && it.label != null }.forEach { sp ->
          val matched = abs(speed - sp.value) < 0.005f
          Text(
            text = sp.label!!,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (matched) FontWeight.Bold else FontWeight.Normal,
            color = if (matched) cs.primary else cs.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
fun ChapterSkipOverlay(
  activePrompt: DetectedSkipChapter?,
  undoBanner: UndoSkipInfo?,
  onSkip: () -> Unit,
  onDismiss: () -> Unit,
  onUndo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    AnimatedVisibility(
      visible = activePrompt != null,
      enter = slideInVertically { it / 2 } + fadeIn(),
      exit = slideOutVertically { it / 2 } + fadeOut(),
    ) {
      activePrompt?.let { ch ->
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
                .clickable(onClick = onSkip)
                .padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.FastForward,
                contentDescription = null,
                tint = cs.onPrimary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "跳过${ch.kind.label}",
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
                contentDescription = "忽略",
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }
    }

    AnimatedVisibility(
      visible = undoBanner != null,
      enter = slideInVertically { it / 2 } + fadeIn(),
      exit = slideOutVertically { it / 2 } + fadeOut(),
    ) {
      undoBanner?.let { undo ->
        Surface(
          color = cs.surfaceContainer.copy(alpha = 0.94f),
          shape = RoundedCornerShape(14.dp),
          shadowElevation = 6.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "已跳过${undo.label}",
              style = MaterialTheme.typography.labelMedium,
              color = cs.onSurface,
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(cs.primary)
                .clickable(onClick = onUndo)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Text(
                text = "撤销",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onPrimary,
              )
            }
          }
        }
      }
    }
  }
}
