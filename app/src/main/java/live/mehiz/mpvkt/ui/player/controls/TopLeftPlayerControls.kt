package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.ui.home.SmartTitleParser
import live.mehiz.mpvkt.ui.player.controls.components.PorcelainPillButton
import live.mehiz.mpvkt.ui.theme.PorcelainBg
import live.mehiz.mpvkt.ui.theme.PorcelainBlue
import live.mehiz.mpvkt.ui.theme.PorcelainInk

@Composable
fun TopLeftPlayerControls(
  mediaTitle: String,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val parsed = remember(mediaTitle) { SmartTitleParser.parse(mediaTitle) }
  var showRawFilename by remember(mediaTitle) { mutableStateOf(false) }
  val displayText = if (showRawFilename) mediaTitle else parsed.displayTitle

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PorcelainPillButton(
      icon = Icons.AutoMirrored.Default.ArrowBack,
      contentDescription = "返回",
      onClick = onBackClick,
      size = 36.dp,
    )
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(11.dp))
        .background(PorcelainBg.copy(alpha = 0.90f))
        .clickable {
          clickEvent()
          showRawFilename = !showRawFilename
        }
        .padding(horizontal = 12.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      if (!showRawFilename) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(PorcelainBlue),
        )
      }
      Text(
        text = displayText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = PorcelainInk,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
