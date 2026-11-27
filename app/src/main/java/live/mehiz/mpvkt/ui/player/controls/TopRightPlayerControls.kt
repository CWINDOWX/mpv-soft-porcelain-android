package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.ui.player.controls.components.PorcelainClockBatteryPill
import live.mehiz.mpvkt.ui.player.controls.components.PorcelainPillButton

@Composable
fun TopRightPlayerControls(
  isPipAvailable: Boolean,
  onLockControls: () -> Unit,
  onCycleRotation: () -> Unit,
  onPipClick: () -> Unit,
  onMoreClick: () -> Unit,
  onMoreLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    PorcelainClockBatteryPill()
    PorcelainPillButton(
      icon = Icons.Filled.LockOpen,
      contentDescription = "锁屏",
      onClick = onLockControls,
      size = 36.dp,
    )
    PorcelainPillButton(
      icon = Icons.Filled.ScreenRotation,
      contentDescription = "旋转",
      onClick = onCycleRotation,
      size = 36.dp,
    )
    if (isPipAvailable) {
      PorcelainPillButton(
        icon = Icons.Filled.PictureInPictureAlt,
        contentDescription = "画中画",
        onClick = onPipClick,
        size = 36.dp,
      )
    }
    PorcelainPillButton(
      icon = Icons.Filled.MoreVert,
      contentDescription = "更多",
      onClick = onMoreClick,
      onLongClick = onMoreLongClick,
      size = 36.dp,
    )
  }
}
