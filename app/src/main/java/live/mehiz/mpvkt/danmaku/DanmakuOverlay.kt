package live.mehiz.mpvkt.danmaku

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.res.ResourcesCompat
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.ui.player.manager.PlaybackState
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

private class ActiveDanmaku(
  val item: DanmakuItem,
  val displayText: String,
  val startTime: Float,
  val duration: Float,
  val lane: Int,
  val textWidth: Float,
)

private class ScrollLaneSlot(
  var startTime: Float = -9999f,
  var duration: Float = 1f,
  var textWidth: Float = 0f,
)

/**
 * Pure-memory, zero-allocation-per-frame 120Hz danmaku layout & rendering engine.
 */
private class DanmakuCanvasEngine {
  private val active = ArrayList<ActiveDanmaku>(160)
  private var scrollLanes = Array(40) { ScrollLaneSlot() }
  private var topLaneUntil = FloatArray(40) { -9999f }
  private var bottomLaneUntil = FloatArray(40) { -9999f }

  private var nextIndex = 0
  private var lastTimeSec = Float.NEGATIVE_INFINITY
  private var lastCommentsRef: List<DanmakuItem>? = null
  private var lastLayoutKey = 0L
  private var lastWidth = 0f
  private var lastHeight = 0f

  val fillPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
  }
  val strokePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
  }
  val shadowPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
  }

  private var textSizePx = 42f
  private var laneHeight = 52f
  private var baselineOffset = 38f
  private var maxLanes = 10
  private var densityScale = 2.5f

  fun configureStyle(
    style: DanmakuStyleConfig,
    typeface: Typeface,
    width: Float,
    height: Float,
    density: Float,
  ) {
    densityScale = density
    val newTextSize = (height * (style.fontSize / 720f)).coerceIn(18f, height * 0.14f)
    val styledTypeface = if (style.bold) Typeface.create(typeface, Typeface.BOLD) else typeface

    fillPaint.textSize = newTextSize
    fillPaint.typeface = styledTypeface
    fillPaint.isFakeBoldText = style.bold

    strokePaint.textSize = newTextSize
    strokePaint.typeface = styledTypeface
    strokePaint.isFakeBoldText = style.bold
    strokePaint.strokeWidth = (style.outline * density * 1.15f).coerceAtLeast(0f)

    shadowPaint.textSize = newTextSize
    shadowPaint.typeface = styledTypeface
    shadowPaint.isFakeBoldText = style.bold

    val fm = fillPaint.fontMetrics
    val glyphHeight = fm.descent - fm.ascent
    textSizePx = newTextSize
    laneHeight = max(glyphHeight * 1.18f, newTextSize * 1.24f)
    baselineOffset = -fm.ascent + (laneHeight - glyphHeight) * 0.5f

    val topMargin = height * 0.025f
    val usableHeight = (height * style.displayArea - topMargin).coerceAtLeast(laneHeight)
    maxLanes = floor(usableHeight / laneHeight).toInt().coerceAtLeast(1)

    if (scrollLanes.size < maxLanes) {
      scrollLanes = Array(maxLanes + 8) { ScrollLaneSlot() }
      topLaneUntil = FloatArray(maxLanes + 8) { -9999f }
      bottomLaneUntil = FloatArray(maxLanes + 8) { -9999f }
    }

    val layoutKey = (
      style.fontSize.toLong() * 31L +
        (style.displayArea * 100f).roundToInt() * 131L +
        style.scrollSeconds * 977L +
        (if (style.bold) 1 else 0) * 1993L +
        (if (style.blockTop) 1 else 0) * 4001L +
        (if (style.blockBottom) 1 else 0) * 8009L +
        (if (style.blockScroll) 1 else 0) * 16001L +
        (if (style.blockColor) 1 else 0) * 32003L +
        style.maxOnScreen * 65537L +
        style.fontChoice.hashCode().toLong()
      )

    if (layoutKey != lastLayoutKey || abs(width - lastWidth) > 2f || abs(height - lastHeight) > 2f) {
      lastLayoutKey = layoutKey
      lastWidth = width
      lastHeight = height
      lastTimeSec = Float.NEGATIVE_INFINITY // Trigger clean instant relayout at current timestamp
    }
  }

  fun drawFrame(
    canvas: android.graphics.Canvas,
    comments: List<DanmakuItem>,
    style: DanmakuStyleConfig,
    currentTimeSec: Float,
    width: Float,
    height: Float,
  ) {
    if (comments.isEmpty() || width <= 4f || height <= 4f) return

    val listChanged = lastCommentsRef !== comments
    val jumped = listChanged ||
      !lastTimeSec.isFinite() ||
      currentTimeSec < lastTimeSec - 0.15f ||
      currentTimeSec > lastTimeSec + 1.8f

    if (jumped) {
      rebuildAt(comments, style, currentTimeSec, width)
      lastCommentsRef = comments
    } else {
      advanceTo(comments, style, currentTimeSec, width)
    }
    lastTimeSec = currentTimeSec

    val alphaInt = (style.opacity.coerceIn(0.15f, 1.0f) * 255f).roundToInt().coerceIn(20, 255)
    val strokeAlpha = (alphaInt * 0.88f).roundToInt().coerceIn(0, 255)
    val shadowAlpha = (alphaInt * 0.55f).roundToInt().coerceIn(0, 255)
    val drawStroke = style.outline > 0.05f
    val drawShadow = style.shadow > 0.05f
    val shadowOffset = style.shadow * densityScale * 0.85f

    strokePaint.color = Color.argb(strokeAlpha, 0, 0, 0)
    shadowPaint.color = Color.argb(shadowAlpha, 0, 0, 0)

    val topMargin = height * 0.025f
    val areaBottom = (height * style.displayArea).coerceIn(topMargin + laneHeight, height)

    for (i in 0 until active.size) {
      val dm = active[i]
      val elapsed = currentTimeSec - dm.startTime
      if (elapsed < 0f || elapsed > dm.duration) continue

      val x: Float
      val y: Float
      when (dm.item.mode) {
        DanmakuMode.TOP -> {
          x = (width - dm.textWidth) * 0.5f
          y = topMargin + dm.lane * laneHeight + baselineOffset
        }

        DanmakuMode.BOTTOM -> {
          x = (width - dm.textWidth) * 0.5f
          y = areaBottom - (dm.lane + 1) * laneHeight + baselineOffset
        }

        else -> {
          val progress = elapsed / dm.duration
          x = width - progress * (width + dm.textWidth)
          y = topMargin + dm.lane * laneHeight + baselineOffset
        }
      }

      if (drawShadow) {
        canvas.drawText(dm.displayText, x + shadowOffset, y + shadowOffset, shadowPaint)
      }
      if (drawStroke) {
        canvas.drawText(dm.displayText, x, y, strokePaint)
      }
      fillPaint.color = (alphaInt shl 24) or (dm.item.colorRgb and 0xFFFFFF)
      canvas.drawText(dm.displayText, x, y, fillPaint)
    }
  }

  private fun clearLanes() {
    active.clear()
    for (i in scrollLanes.indices) {
      scrollLanes[i].startTime = -9999f
      scrollLanes[i].duration = 1f
      scrollLanes[i].textWidth = 0f
    }
    topLaneUntil.fill(-9999f)
    bottomLaneUntil.fill(-9999f)
  }

  private fun rebuildAt(
    comments: List<DanmakuItem>,
    style: DanmakuStyleConfig,
    currentTimeSec: Float,
    width: Float,
  ) {
    clearLanes()
    val windowStart = (currentTimeSec - style.scrollSeconds.toFloat() - 0.5f).coerceAtLeast(0f)
    var idx = lowerBound(comments, windowStart)
    val n = comments.size
    while (idx < n && comments[idx].timeSec <= currentTimeSec) {
      val item = comments[idx]
      idx++
      val dur = durationFor(item, style)
      if (item.timeSec + dur <= currentTimeSec) continue
      trySpawn(item, style, width)
    }
    nextIndex = idx
  }

  private fun advanceTo(
    comments: List<DanmakuItem>,
    style: DanmakuStyleConfig,
    currentTimeSec: Float,
    width: Float,
  ) {
    // Prune expired items in-place (swap-remove is avoided to keep draw z-order stable)
    var write = 0
    for (read in 0 until active.size) {
      val dm = active[read]
      if (currentTimeSec <= dm.startTime + dm.duration) {
        if (write != read) active[write] = dm
        write++
      }
    }
    while (active.size > write) {
      active.removeAt(active.size - 1)
    }

    val n = comments.size
    while (nextIndex < n && comments[nextIndex].timeSec <= currentTimeSec) {
      val item = comments[nextIndex]
      nextIndex++
      val dur = durationFor(item, style)
      if (item.timeSec + dur > currentTimeSec) {
        trySpawn(item, style, width)
      }
    }
  }

  private fun durationFor(item: DanmakuItem, style: DanmakuStyleConfig): Float = when (item.mode) {
    DanmakuMode.TOP, DanmakuMode.BOTTOM -> 5.0f
    else -> style.scrollSeconds.toFloat().coerceIn(5f, 22f)
  }

  private fun trySpawn(item: DanmakuItem, style: DanmakuStyleConfig, width: Float) {
    if (style.blockScroll && item.mode == DanmakuMode.SCROLL) return
    if (style.blockTop && item.mode == DanmakuMode.TOP) return
    if (style.blockBottom && item.mode == DanmakuMode.BOTTOM) return
    if (style.blockColor && (item.colorRgb and 0xFFFFFF) != 0xFFFFFF) return

    val cap = if (style.maxOnScreen > 0) style.maxOnScreen else 180
    if (active.size >= cap) return

    val text = item.displayText
    val textW = fillPaint.measureText(text)
    val dur = durationFor(item, style)
    val t = item.timeSec

    val lane = when (item.mode) {
      DanmakuMode.TOP -> allocateFixedLane(topLaneUntil, t, dur)
      DanmakuMode.BOTTOM -> allocateFixedLane(bottomLaneUntil, t, dur)
      else -> allocateScrollLane(t, dur, textW, width)
    } ?: return

    active.add(
      ActiveDanmaku(
        item = item,
        displayText = text,
        startTime = t,
        duration = dur,
        lane = lane,
        textWidth = textW,
      ),
    )
  }

  private fun allocateFixedLane(laneUntil: FloatArray, startTime: Float, duration: Float): Int? {
    for (i in 0 until maxLanes) {
      if (startTime >= laneUntil[i]) {
        laneUntil[i] = startTime + duration
        return i
      }
    }
    return null
  }

  private fun allocateScrollLane(
    startTime: Float,
    duration: Float,
    textWidth: Float,
    screenWidth: Float,
  ): Int? {
    val gapPx = textSizePx * 0.9f
    val newSpeed = (screenWidth + textWidth) / duration
    val newTimeToLeftEdge = screenWidth / newSpeed

    for (i in 0 until maxLanes) {
      val slot = scrollLanes[i]
      val prevEnd = slot.startTime + slot.duration
      if (startTime >= prevEnd) {
        slot.startTime = startTime
        slot.duration = duration
        slot.textWidth = textWidth
        return i
      }
      val prevElapsed = startTime - slot.startTime
      val prevSpeed = (screenWidth + slot.textWidth) / slot.duration
      val prevTailX = screenWidth - prevElapsed * prevSpeed + slot.textWidth
      val prevRemaining = prevEnd - startTime
      // Tail has fully entered screen with margin AND new danmaku won't overtake it before left exit
      if (prevTailX <= screenWidth - gapPx && newTimeToLeftEdge >= prevRemaining) {
        slot.startTime = startTime
        slot.duration = duration
        slot.textWidth = textWidth
        return i
      }
    }
    return null
  }

  private fun lowerBound(list: List<DanmakuItem>, targetTime: Float): Int {
    var lo = 0
    var hi = list.size
    while (lo < hi) {
      val mid = (lo + hi) ushr 1
      if (list[mid].timeSec < targetTime) lo = mid + 1 else hi = mid
    }
    return lo
  }
}

@Composable
fun DanmakuOverlay(
  danmakuManager: DanmakuManager,
  playback: PlaybackState,
  modifier: Modifier = Modifier,
) {
  val enabled by danmakuManager.enabled.collectAsState()
  val comments by danmakuManager.comments.collectAsState()
  val style by danmakuManager.style.collectAsState()
  val timeOffsetSec by danmakuManager.timeOffsetSec.collectAsState()
  val paused by playback.paused.collectAsState()
  val speed by playback.speed.collectAsState()
  val precisePos by playback.precisePosition.collectAsState()

  if (!enabled || comments.isEmpty()) return

  val context = LocalContext.current
  val density = LocalDensity.current.density
  val lxgwTypeface = remember(context) {
    runCatching { ResourcesCompat.getFont(context, R.font.lxgw_wenkai_mono_lite) }.getOrNull()
      ?: Typeface.DEFAULT
  }
  val activeTypeface = remember(style.fontChoice, lxgwTypeface) {
    when (DanmakuFontOption.fromId(style.fontChoice)) {
      DanmakuFontOption.Lxgw -> lxgwTypeface
      DanmakuFontOption.Sans -> Typeface.SANS_SERIF
      DanmakuFontOption.Serif -> Typeface.SERIF
      DanmakuFontOption.Mono -> Typeface.MONOSPACE
    }
  }

  val engine = remember { DanmakuCanvasEngine() }

  // Smooth wall-clock anchor synced to mpv's precise time-pos (eliminates coarse time-pos stutter!)
  val clockState = remember {
    object {
      var anchorVideoSec: Double = 0.0
      var anchorNanos: Long = 0L
      var lastSampledSec: Double = 0.0
    }
  }

  // Re-anchor cleanly whenever mpv reports a new position, pause state, or speed change
  LaunchedEffect(precisePos, paused, speed) {
    val now = System.nanoTime()
    if (paused || clockState.anchorNanos == 0L) {
      clockState.anchorVideoSec = precisePos
      clockState.anchorNanos = now
      clockState.lastSampledSec = precisePos
    } else {
      val predicted = clockState.anchorVideoSec + ((now - clockState.anchorNanos) / 1_000_000_000.0) * speed
      val drift = precisePos - predicted
      if (abs(drift) > 0.32) {
        // Seek / chapter skip / stall -> hard re-anchor
        clockState.anchorVideoSec = precisePos
        clockState.anchorNanos = now
      } else if (abs(drift) > 0.06) {
        // Gentle phase lock (25% step) so 120Hz scrolling stays silky with zero jitter
        clockState.anchorVideoSec = predicted + drift * 0.25
        clockState.anchorNanos = now
      }
      clockState.lastSampledSec = precisePos
    }
  }

  var frameTick by remember { mutableLongStateOf(0L) }
  LaunchedEffect(enabled, paused, comments.isNotEmpty()) {
    if (!paused) {
      while (true) {
        withFrameNanos { frameNanos ->
          frameTick = frameNanos
        }
      }
    } else {
      frameTick = System.nanoTime()
    }
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    // Read frameTick inside draw phase so Compose re-draws at native display VSYNC (120Hz) without recomposing!
    val tick = frameTick
    val videoTimeSec = if (paused || clockState.anchorNanos == 0L) {
      clockState.anchorVideoSec
    } else {
      val dt = ((tick - clockState.anchorNanos).coerceAtLeast(0L)) / 1_000_000_000.0
      // Guard against runaway extrapolation if mpv buffers silently
      val clampedDt = dt.coerceAtMost(2.5)
      clockState.anchorVideoSec + clampedDt * speed
    }
    val effectiveTime = (videoTimeSec + timeOffsetSec).toFloat().coerceAtLeast(0f)

    engine.configureStyle(
      style = style,
      typeface = activeTypeface,
      width = size.width,
      height = size.height,
      density = density,
    )
    drawIntoCanvas { canvas ->
      engine.drawFrame(
        canvas = canvas.nativeCanvas,
        comments = comments,
        style = style,
        currentTimeSec = effectiveTime,
        width = size.width,
        height = size.height,
      )
    }
  }
}
