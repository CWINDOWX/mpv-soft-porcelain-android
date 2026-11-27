package live.mehiz.mpvkt.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.R
import kotlin.math.abs
import kotlin.math.roundToInt

fun percentage(value: Float, range: ClosedFloatingPointRange<Float>): Float =
  ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

fun percentage(value: Int, range: ClosedRange<Int>): Float =
  ((value - range.start - 0f) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

@Composable
fun VerticalSlider(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
  overflowValue: Float? = null,
  overflowRange: ClosedFloatingPointRange<Float>? = null,
) {
  val cs = MaterialTheme.colorScheme
  val safeValue = value.coerceIn(range.start, range.endInclusive)
  Box(
    modifier = modifier
      .height(112.dp)
      .width(10.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(cs.outlineVariant),
    contentAlignment = Alignment.BottomCenter,
  ) {
    val targetHeight by animateFloatAsState(percentage(safeValue, range), label = "vsliderheight")
    Box(
      Modifier
        .fillMaxWidth()
        .fillMaxHeight(targetHeight)
        .clip(RoundedCornerShape(8.dp))
        .background(cs.primary),
    )
    if (overflowRange != null && overflowValue != null && overflowValue > overflowRange.start) {
      val overflowHeight by animateFloatAsState(
        percentage(overflowValue.coerceIn(overflowRange.start, overflowRange.endInclusive), overflowRange),
        label = "vslideroverflowheight",
      )
      Box(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(overflowHeight)
          .clip(RoundedCornerShape(8.dp))
          .background(cs.error.copy(alpha = 0.85f)),
      )
    }
  }
}

@Composable
fun VerticalSlider(
  value: Int,
  range: ClosedRange<Int>,
  modifier: Modifier = Modifier,
  overflowValue: Int? = null,
  overflowRange: ClosedRange<Int>? = null,
) {
  val cs = MaterialTheme.colorScheme
  val safeValue = value.coerceIn(range.start, range.endInclusive)
  Box(
    modifier = modifier
      .height(112.dp)
      .width(10.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(cs.outlineVariant),
    contentAlignment = Alignment.BottomCenter,
  ) {
    val targetHeight by animateFloatAsState(percentage(safeValue, range), label = "vsliderheight")
    Box(
      Modifier
        .fillMaxWidth()
        .fillMaxHeight(targetHeight)
        .clip(RoundedCornerShape(8.dp))
        .background(cs.primary),
    )
    if (overflowRange != null && overflowValue != null && overflowValue > overflowRange.start) {
      val overflowHeight by animateFloatAsState(
        percentage(overflowValue.coerceIn(overflowRange.start, overflowRange.endInclusive), overflowRange),
        label = "vslideroverflowheight",
      )
      Box(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(overflowHeight)
          .clip(RoundedCornerShape(8.dp))
          .background(cs.error.copy(alpha = 0.85f)),
      )
    }
  }
}

@Composable
fun BrightnessSlider(brightness: Float, range: ClosedFloatingPointRange<Float>, modifier: Modifier = Modifier) {
  val cs = MaterialTheme.colorScheme
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(cs.surfaceContainer.copy(alpha = 0.92f))
      .padding(horizontal = 10.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = "${(brightness * 100).roundToInt()}%",
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = cs.onSurface,
    )
    VerticalSlider(brightness, range)
    Icon(
      imageVector = when (percentage(brightness, range)) {
        in 0f..0.3f -> Icons.Default.BrightnessLow
        in 0.3f..0.6f -> Icons.Default.BrightnessMedium
        else -> Icons.Default.BrightnessHigh
      },
      contentDescription = null,
      tint = cs.primary,
      modifier = Modifier.size(18.dp),
    )
  }
}

@Composable
fun VolumeSlider(
  volume: Int,
  mpvVolume: Int,
  range: ClosedRange<Int>,
  boostRange: ClosedRange<Int>?,
  modifier: Modifier = Modifier,
  displayAsPercentage: Boolean = false,
) {
  val cs = MaterialTheme.colorScheme
  val pct = (percentage(volume, range) * 100).roundToInt()
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(cs.surfaceContainer.copy(alpha = 0.92f))
      .padding(horizontal = 10.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val boostVolume = mpvVolume - 100
    Text(
      text = getVolumeSliderText(volume, mpvVolume, boostVolume, pct, displayAsPercentage),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = cs.onSurface,
    )
    VerticalSlider(
      if (displayAsPercentage) pct else volume,
      if (displayAsPercentage) 0..100 else range,
      overflowValue = boostVolume,
      overflowRange = boostRange,
    )
    Icon(
      imageVector = when (pct) {
        0 -> Icons.AutoMirrored.Default.VolumeOff
        in 1..30 -> Icons.AutoMirrored.Default.VolumeMute
        in 31..60 -> Icons.AutoMirrored.Default.VolumeDown
        else -> Icons.AutoMirrored.Default.VolumeUp
      },
      contentDescription = null,
      tint = cs.primary,
      modifier = Modifier.size(18.dp),
    )
  }
}

val getVolumeSliderText: @Composable (Int, Int, Int, Int, Boolean) -> String =
  { volume, mpvVolume, boostVolume, percentage, displayAsPercentage ->
    when (mpvVolume - 100) {
      0 -> if (displayAsPercentage) {
        stringResource(R.string.value_percentage_int, percentage)
      } else {
        "$volume"
      }

      in 0..1000 -> {
        if (displayAsPercentage) {
          stringResource(R.string.volume_slider_percentage_positive_boost, percentage, boostVolume)
        } else {
          stringResource(R.string.volume_slider_steps_positive_boost, volume, boostVolume)
        }
      }

      in -100..-1 -> {
        if (displayAsPercentage) {
          stringResource(R.string.volume_slider_percentage_negative_boost, percentage, abs(boostVolume))
        } else {
          stringResource(R.string.volume_slider_steps_negative_boost, volume, abs(boostVolume))
        }
      }

      else -> {
        if (displayAsPercentage) {
          stringResource(R.string.volume_slider_percentage_other, percentage, boostVolume)
        } else {
          stringResource(R.string.volume_slider_steps_other, volume, boostVolume)
        }
      }
    }
  }
