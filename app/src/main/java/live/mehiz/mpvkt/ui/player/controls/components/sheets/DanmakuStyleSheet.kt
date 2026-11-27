package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.danmaku.DANMAKU_PRESETS
import live.mehiz.mpvkt.danmaku.DanmakuChConvert
import live.mehiz.mpvkt.danmaku.DanmakuFontOption
import live.mehiz.mpvkt.danmaku.DanmakuPreset
import live.mehiz.mpvkt.danmaku.DanmakuStyleConfig
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private data class MagneticSnapPoint(
  val value: Float,
  val tag: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DanmakuStylePanelOverlay(
  enabled: Boolean,
  style: DanmakuStyleConfig,
  onToggleEnabled: (Boolean) -> Unit,
  onUpdateStyle: ((DanmakuStyleConfig) -> DanmakuStyleConfig) -> Unit,
  onApplyPreset: (DanmakuPreset) -> Unit,
  onResetStyle: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onDismissRequest)
  val cs = MaterialTheme.colorScheme
  val screenWidthDp = LocalConfiguration.current.screenWidthDp
  val isLandscapeWide = screenWidthDp >= 560

  // Transparent scrim on the left so user watches 120Hz danmaku update live while dragging sliders!
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.16f))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismissRequest,
      )
      .windowInsetsPadding(WindowInsets.displayCutout)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    contentAlignment = if (isLandscapeWide) Alignment.CenterEnd else Alignment.BottomCenter,
  ) {
    Surface(
      modifier = Modifier
        .then(
          if (isLandscapeWide) {
            Modifier
              .width(368.dp)
              .fillMaxHeight()
          } else {
            Modifier
              .fillMaxWidth()
              .fillMaxHeight(0.76f)
          },
        )
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {},
        ),
      color = cs.surfaceContainer.copy(alpha = 0.94f),
      shape = RoundedCornerShape(18.dp),
      shadowElevation = 10.dp,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        // Header: title + master switch + reset + close
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "弹幕样式",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = cs.onSurface,
            )
            Switch(
              checked = enabled,
              onCheckedChange = onToggleEnabled,
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(cs.surfaceBright)
                .clickable(onClick = onResetStyle)
                .padding(horizontal = 9.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "恢复默认",
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "默认",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
              )
            }
            Box(
              modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(cs.surfaceBright)
                .clickable(onClick = onDismissRequest),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // 5 Quick Presets
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DANMAKU_PRESETS.forEach { preset ->
              val applied = preset.apply(style)
              val isActive = applied == style
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (isActive) cs.primary else cs.surfaceBright)
                  .clickable { onApplyPreset(preset) }
                  .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = preset.label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                  color = if (isActive) cs.onPrimary else cs.onSurface,
                  maxLines = 1,
                )
              }
            }
          }

          // 7 Magnetic Sliders (matching PC style_panel.lua ranges & curated snap points!)
          // 1. Font Size
          MagneticSnapSlider(
            label = "字体大小",
            value = style.fontSize.toFloat(),
            min = 20f,
            max = 60f,
            step = 1f,
            valueText = "${style.fontSize} px",
            snaps = listOf(
              MagneticSnapPoint(24f, "24"),
              MagneticSnapPoint(30f, "30"),
              MagneticSnapPoint(36f, "适中"),
              MagneticSnapPoint(44f, "44"),
              MagneticSnapPoint(56f, "56"),
            ),
            onValueChange = { v -> onUpdateStyle { it.copy(fontSize = v.roundToInt().coerceIn(20, 60)) } },
          )

          // 2. Opacity
          MagneticSnapSlider(
            label = "不透明度",
            value = style.opacity,
            min = 0.15f,
            max = 1.00f,
            step = 0.01f,
            valueText = "${(style.opacity * 100f).roundToInt()}%",
            snaps = listOf(
              MagneticSnapPoint(0.30f, "30%"),
              MagneticSnapPoint(0.45f, "护眼"),
              MagneticSnapPoint(0.65f, "65%"),
              MagneticSnapPoint(0.78f, "推荐"),
              MagneticSnapPoint(1.00f, "100%"),
            ),
            onValueChange = { v ->
              val rounded = (v * 100f).roundToInt() / 100f
              onUpdateStyle { it.copy(opacity = rounded.coerceIn(0.15f, 1.0f)) }
            },
          )

          // 3. Display Area
          val areaPct = (style.displayArea * 100f).roundToInt()
          MagneticSnapSlider(
            label = "显示区域",
            value = style.displayArea,
            min = 0.20f,
            max = 1.00f,
            step = 0.01f,
            valueText = if (areaPct == 72) "72% 避字幕" else "$areaPct%",
            snaps = listOf(
              MagneticSnapPoint(0.25f, "顶"),
              MagneticSnapPoint(0.50f, "半屏"),
              MagneticSnapPoint(0.72f, "避字幕"),
              MagneticSnapPoint(0.85f, "85%"),
              MagneticSnapPoint(1.00f, "全屏"),
            ),
            onValueChange = { v ->
              val rounded = (v * 100f).roundToInt() / 100f
              onUpdateStyle { it.copy(displayArea = rounded.coerceIn(0.20f, 1.0f)) }
            },
          )

          // 4. Scroll Speed (inverted: right = faster = fewer seconds!)
          val speedTag = when {
            style.scrollSeconds >= 17 -> "慢"
            style.scrollSeconds >= 14 -> "柔和"
            style.scrollSeconds >= 11 -> "标准"
            style.scrollSeconds >= 8 -> "较快"
            else -> "极速"
          }
          MagneticSnapSlider(
            label = "滚动速度",
            value = style.scrollSeconds.toFloat(),
            min = 6f,
            max = 20f,
            step = 1f,
            inverted = true,
            valueText = "$speedTag (${style.scrollSeconds}s)",
            snaps = listOf(
              MagneticSnapPoint(18f, "慢"),
              MagneticSnapPoint(15f, "柔和"),
              MagneticSnapPoint(12f, "标准"),
              MagneticSnapPoint(9f, "快"),
              MagneticSnapPoint(6f, "飞速"),
            ),
            onValueChange = { v -> onUpdateStyle { it.copy(scrollSeconds = v.roundToInt().coerceIn(6, 20)) } },
          )

          // 5. Outline
          MagneticSnapSlider(
            label = "描边粗细",
            value = style.outline,
            min = 0.0f,
            max = 3.5f,
            step = 0.1f,
            valueText = String.format(Locale.US, "%.1f", style.outline),
            snaps = listOf(
              MagneticSnapPoint(0.0f, "无"),
              MagneticSnapPoint(0.5f, "细"),
              MagneticSnapPoint(1.0f, "标准"),
              MagneticSnapPoint(1.8f, "粗"),
              MagneticSnapPoint(2.8f, "厚"),
            ),
            onValueChange = { v ->
              val rounded = (v * 10f).roundToInt() / 10f
              onUpdateStyle { it.copy(outline = rounded.coerceIn(0f, 3.5f)) }
            },
          )

          // 6. Shadow
          MagneticSnapSlider(
            label = "阴影深度",
            value = style.shadow,
            min = 0.0f,
            max = 2.5f,
            step = 0.1f,
            valueText = String.format(Locale.US, "%.1f", style.shadow),
            snaps = listOf(
              MagneticSnapPoint(0.0f, "无"),
              MagneticSnapPoint(0.5f, "轻"),
              MagneticSnapPoint(1.2f, "中"),
              MagneticSnapPoint(2.0f, "深"),
            ),
            onValueChange = { v ->
              val rounded = (v * 10f).roundToInt() / 10f
              onUpdateStyle { it.copy(shadow = rounded.coerceIn(0f, 2.5f)) }
            },
          )

          // 7. Max On-Screen Density (120 maps to 0 = unlimited!)
          val sliderDensity = if (style.maxOnScreen <= 0 || style.maxOnScreen >= 120) 120f else style.maxOnScreen.toFloat()
          MagneticSnapSlider(
            label = "同屏上限",
            value = sliderDensity,
            min = 15f,
            max = 120f,
            step = 5f,
            valueText = if (sliderDensity >= 118f) "不限" else "${sliderDensity.roundToInt()} 条",
            snaps = listOf(
              MagneticSnapPoint(20f, "20"),
              MagneticSnapPoint(40f, "40"),
              MagneticSnapPoint(60f, "60"),
              MagneticSnapPoint(90f, "90"),
              MagneticSnapPoint(120f, "不限"),
            ),
            onValueChange = { v ->
              val n = v.roundToInt()
              onUpdateStyle { it.copy(maxOnScreen = if (n >= 118) 0 else n.coerceIn(15, 115)) }
            },
          )

          HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.7f))

          // Font selection row
          Text(
            text = "字体",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurfaceVariant,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DanmakuFontOption.entries.forEach { font ->
              val selected = style.fontChoice == font.id
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(9.dp))
                  .background(if (selected) cs.primary else cs.surfaceBright)
                  .clickable { onUpdateStyle { it.copy(fontChoice = font.id) } }
                  .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = font.label,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) cs.onPrimary else cs.onSurface,
                  maxLines = 1,
                )
              }
            }
          }

          // 6 Filter & Style Toggle Chips
          Text(
            text = "显示与屏蔽",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurfaceVariant,
          )
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterToggleChip("粗体", style.bold) { onUpdateStyle { it.copy(bold = !it.bold) } }
            FilterToggleChip("去重合并", style.mergeDuplicates) {
              onUpdateStyle { it.copy(mergeDuplicates = !it.mergeDuplicates) }
            }
            FilterToggleChip("屏蔽顶部", style.blockTop) { onUpdateStyle { it.copy(blockTop = !it.blockTop) } }
            FilterToggleChip("屏蔽底部", style.blockBottom) { onUpdateStyle { it.copy(blockBottom = !it.blockBottom) } }
            FilterToggleChip("屏蔽滚动", style.blockScroll) { onUpdateStyle { it.copy(blockScroll = !it.blockScroll) } }
            FilterToggleChip("屏蔽彩色", style.blockColor) { onUpdateStyle { it.copy(blockColor = !it.blockColor) } }
          }

          // Traditional / Simplified Conversion
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "简繁转换",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = cs.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              DanmakuChConvert.entries.forEach { mode ->
                val selected = style.chConvert == mode.code
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) cs.primary else cs.surfaceBright)
                    .clickable { onUpdateStyle { it.copy(chConvert = mode.code) } }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                  Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) cs.onPrimary else cs.onSurface,
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
        }
      }
    }
  }
}

@Composable
private fun FilterToggleChip(
  label: String,
  active: Boolean,
  onClick: () -> Unit,
) {
  val cs = MaterialTheme.colorScheme
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(9.dp))
      .background(if (active) cs.primary else cs.surfaceBright)
      .clickable(onClick = onClick)
      .padding(horizontal = 11.dp, vertical = 6.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
      color = if (active) cs.onPrimary else cs.onSurface,
    )
  }
}

@Composable
private fun MagneticSnapSlider(
  label: String,
  value: Float,
  min: Float,
  max: Float,
  step: Float,
  valueText: String,
  snaps: List<MagneticSnapPoint>,
  onValueChange: (Float) -> Unit,
  inverted: Boolean = false,
) {
  val cs = MaterialTheme.colorScheme
  val haptics = LocalHapticFeedback.current
  var lastSnapped by remember { mutableStateOf<Float?>(null) }

  fun valToNorm(v: Float): Float {
    val raw = ((v - min) / (max - min)).coerceIn(0f, 1f)
    return if (inverted) 1f - raw else raw
  }

  fun normToVal(tRaw: Float): Pair<Float, Boolean> {
    val t = tRaw.coerceIn(0f, 1f)
    var bestSnap: Float? = null
    var bestDist = 0.036f // 3.6% magnetic snap radius matching PC style_panel.lua
    for (sp in snaps) {
      val st = valToNorm(sp.value)
      val d = abs(t - st)
      if (d < bestDist) {
        bestDist = d
        bestSnap = sp.value
      }
    }
    if (bestSnap != null) return bestSnap to true
    val eff = if (inverted) 1f - t else t
    val raw = min + eff * (max - min)
    val stepped = min + floor((raw - min) / step + 0.5f) * step
    return stepped.coerceIn(min, max) to false
  }

  val norm = valToNorm(value)
  val trackColor = cs.outlineVariant
  val activeColor = cs.primary
  val mutedColor = cs.onSurfaceVariant
  val surfaceBrightColor = cs.surfaceBright
  val shadowColor = cs.onSurface.copy(alpha = 0.14f)

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = cs.onSurface,
      )
      Text(
        text = valueText,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = cs.primary,
      )
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(28.dp)
        .pointerInput(min, max, step, inverted) {
          detectTapGestures(
            onTap = { offset ->
              val padPx = 8.dp.toPx()
              val trackW = (size.width - padPx * 2).coerceAtLeast(1f)
              val t = ((offset.x - padPx) / trackW).coerceIn(0f, 1f)
              val (nv, snapped) = normToVal(t)
              if (snapped) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onValueChange(nv)
            },
          )
        }
        .pointerInput(min, max, step, inverted) {
          detectHorizontalDragGestures(
            onDragStart = { offset ->
              val padPx = 8.dp.toPx()
              val trackW = (size.width - padPx * 2).coerceAtLeast(1f)
              val t = ((offset.x - padPx) / trackW).coerceIn(0f, 1f)
              val (nv, snapped) = normToVal(t)
              lastSnapped = if (snapped) nv else null
              onValueChange(nv)
            },
            onHorizontalDrag = { change, _ ->
              change.consume()
              val padPx = 8.dp.toPx()
              val trackW = (size.width - padPx * 2).coerceAtLeast(1f)
              val t = ((change.position.x - padPx) / trackW).coerceIn(0f, 1f)
              val (nv, snapped) = normToVal(t)
              if (snapped && lastSnapped != nv) {
                lastSnapped = nv
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              } else if (!snapped) {
                lastSnapped = null
              }
              onValueChange(nv)
            },
          )
        },
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val thumbR = 7.5.dp.toPx()
        val trackH = 5.dp.toPx()
        val trAx = thumbR
        val trBx = size.width - thumbR
        val trW = (trBx - trAx).coerceAtLeast(1f)
        val cy = size.height / 2f
        val fillX = trAx + trW * norm

        drawRoundRect(
          color = trackColor,
          topLeft = Offset(trAx, cy - trackH / 2f),
          size = Size(trW, trackH),
          cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
        )
        if (fillX > trAx + 1f) {
          drawRoundRect(
            color = activeColor,
            topLeft = Offset(trAx, cy - trackH / 2f),
            size = Size(fillX - trAx, trackH),
            cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
          )
        }

        val dotR = 2.6.dp.toPx()
        for (sp in snaps) {
          val st = valToNorm(sp.value)
          val sx = trAx + trW * st
          val passed = st <= norm + 0.005f
          drawCircle(
            color = if (passed) activeColor else mutedColor.copy(alpha = 0.5f),
            radius = dotR + 0.9.dp.toPx(),
            center = Offset(sx, cy),
          )
          drawCircle(
            color = surfaceBrightColor,
            radius = dotR - 0.3.dp.toPx(),
            center = Offset(sx, cy),
          )
        }

        drawCircle(
          color = shadowColor,
          radius = thumbR + 1.2.dp.toPx(),
          center = Offset(fillX, cy + 0.8.dp.toPx()),
        )
        drawCircle(
          color = activeColor,
          radius = thumbR,
          center = Offset(fillX, cy),
        )
        drawCircle(
          color = surfaceBrightColor,
          radius = thumbR - 2.2.dp.toPx(),
          center = Offset(fillX, cy),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      snaps.forEach { sp ->
        val matched = abs(value - sp.value) < (step * 0.6f)
        Text(
          text = sp.tag,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (matched) FontWeight.Bold else FontWeight.Normal,
          color = if (matched) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.85f),
          modifier = Modifier.clickable {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onValueChange(sp.value)
          },
        )
      }
    }
  }
}
