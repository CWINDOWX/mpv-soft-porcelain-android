package live.mehiz.mpvkt.ui.player.controls.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vivvvek.seeker.Seeker
import dev.vivvvek.seeker.SeekerDefaults
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import live.mehiz.mpvkt.ui.player.controls.LocalPlayerButtonsClickEvent
import live.mehiz.mpvkt.ui.theme.PorcelainBg
import live.mehiz.mpvkt.ui.theme.PorcelainBlue
import live.mehiz.mpvkt.ui.theme.PorcelainBlueSoft
import live.mehiz.mpvkt.ui.theme.PorcelainInk
import live.mehiz.mpvkt.ui.theme.PorcelainTrack

@Composable
fun SeekbarWithTimers(
  position: Float,
  duration: Float,
  readAheadValue: Float,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit,
  timersInverted: Pair<Boolean, Boolean>,
  positionTimerOnClick: () -> Unit,
  durationTimerOnCLick: () -> Unit,
  chapters: ImmutableList<Segment>,
  modifier: Modifier = Modifier,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val safeDuration = duration.coerceAtLeast(1f)

  // Floating rounded porcelain capsule progress bar aligned with bottom controls dock
  Row(
    modifier = modifier
      .height(40.dp)
      .clip(RoundedCornerShape(13.dp))
      .background(PorcelainBg.copy(alpha = 0.90f))
      .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    VideoTimer(
      value = position,
      isInverted = timersInverted.first,
      onClick = {
        clickEvent()
        positionTimerOnClick()
      },
      modifier = Modifier.widthIn(min = 52.dp),
    )
    Seeker(
      value = position.coerceIn(0f, safeDuration),
      range = 0f..safeDuration,
      onValueChange = onValueChange,
      onValueChangeFinished = onValueChangeFinished,
      readAheadValue = readAheadValue.coerceIn(0f, safeDuration),
      segments = chapters
        .filter { it.start in 0f..safeDuration }
        .let { if (it.isNotEmpty() && it[0].start != 0f) persistentListOf(Segment("", 0f)) + it else it },
      modifier = Modifier.weight(1f),
      colors = SeekerDefaults.seekerColors(
        progressColor = PorcelainBlue,
        thumbColor = PorcelainBlue,
        trackColor = PorcelainTrack,
        readAheadColor = PorcelainBlueSoft,
      ),
    )
    VideoTimer(
      value = if (timersInverted.second) position - duration else duration,
      isInverted = timersInverted.second,
      onClick = {
        clickEvent()
        durationTimerOnCLick()
      },
      modifier = Modifier.widthIn(min = 52.dp),
    )
  }
}

@Composable
fun VideoTimer(value: Float, isInverted: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
  val interactionSource = remember { MutableInteractionSource() }
  Text(
    modifier = modifier
      .fillMaxHeight()
      .clip(RoundedCornerShape(8.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = onClick,
      )
      .wrapContentHeight(Alignment.CenterVertically)
      .padding(horizontal = 4.dp),
    text = Utils.prettyTime(value.toInt(), isInverted),
    color = PorcelainInk,
    style = MaterialTheme.typography.labelMedium,
    fontWeight = FontWeight.SemiBold,
    textAlign = TextAlign.Center,
  )
}

@Preview
@Composable
private fun PreviewSeekBar() {
  SeekbarWithTimers(
    5f,
    20f,
    4f,
    {},
    {},
    Pair(false, true),
    {},
    {},
    persistentListOf<Segment>(),
  )
}
