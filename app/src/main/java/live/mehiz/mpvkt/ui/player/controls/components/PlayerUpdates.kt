package live.mehiz.mpvkt.ui.player.controls.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.R

@Composable
fun PlayerUpdate(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f))
      .padding(vertical = 7.dp, horizontal = 14.dp)
      .animateContentSize(),
    contentAlignment = Alignment.Center,
  ) { content() }
}

@Composable
fun TextPlayerUpdate(text: String, modifier: Modifier = Modifier) {
  PlayerUpdate(modifier) {
    Text(
      text = text,
      color = MaterialTheme.colorScheme.onSurface,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
fun MultipleSpeedPlayerUpdate(currentSpeed: Float, modifier: Modifier = Modifier) {
  val cs = MaterialTheme.colorScheme
  PlayerUpdate(modifier) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = stringResource(R.string.player_speed, currentSpeed),
        color = cs.primary,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall,
      )
      Icon(
        imageVector = Icons.Filled.DoubleArrow,
        contentDescription = null,
        tint = cs.primary,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
@Preview
private fun PreviewMultipleSpeedPlayerUpdate() {
  MultipleSpeedPlayerUpdate(currentSpeed = 2f)
}
