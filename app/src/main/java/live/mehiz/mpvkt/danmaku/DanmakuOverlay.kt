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
import androidx.compose.ui.platform.LocalDensity
import live.mehiz.mpvkt.App
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.ui.player.manager.PlaybackState
import live.mehiz.mpvkt.ui.theme.CustomFontManager
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
  private var lastPaintKey = Long.MIN_VALUE

  fun configureStyle(
    style: DanmakuStyleConfig,
    typeface: Typeface,
    width: Float,
    height: Float,
    density: Float,
  ) {
    val heightBucket = height.roundToInt()
    val densityBucket = (density * 100f).roundToInt()
    val outlineBucket = (style.outline * 100f).roundToInt()
    val displayAreaBucket = (style.displayArea * 100f).roundToInt()
    val typefaceHash = typeface.hashCode().toLong()

    val paintKey = (
      style.fontSize.toLong() * 31L +
        (if (style.bold) 1L else 0L) * 1993L +
        outlineBucket.toLong() * 4001L +
        displayAreaBucket.toLong() * 8009L +
        heightBucket.toLong() * 16001L +
        densityBucket.toLong() * 32003L +
        style.fontChoice.hashCode().toLong() * 65537L +
        typefaceHash * 131071L
      )

    if (paintKey != lastPaintKey) {
      lastPaintKey = paintKey
      densityScale = density
      val newTextSize = (height * (style.fontSize / 720f)).coerceIn(18f, height * 0.14f)
      val styledTypeface = if (style.bold) Typeface.create(typeface, Typeface.BOLD) else typeface
      // Only enable synthetic bold when the font lacks a real bold weight (prevents double-bold blur)
      val needFakeBold = style.bold && !styledTypeface.isBold

      fillPaint.textSize = newTextSize
      fillPaint.typeface = styledTypeface
      fillPaint.isFakeBoldText = needFakeBold

      strokePaint.textSize = newTextSize
      strokePaint.typeface = styledTypeface
      strokePaint.isFakeBoldText = needFakeBold
      strokePaint.strokeWidth = (style.outline * density * 1.15f).coerceAtLeast(0f)

      shadowPaint.textSize = newTextSize
      shadowPaint.typeface = styledTypeface
      shadowPaint.isFakeBoldText = needFakeBold

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
    }

    val layoutKey = (
      style.fontSize.toLong() * 31L +
        displayAreaBucket * 131L +
        style.scrollSeconds * 977L +
        (if (style.bold) 1 else 0) * 1993L +
        (if (style.blockTop) 1 else 0) * 4001L +
        (if (style.blockBottom) 1 else 0) * 8009L +
        (if (style.blockScroll) 1 else 0) * 16001L +
        (if (style.blockColor) 1 else 0) * 32003L +
        style.maxOnScreen * 65537L +
        style.fontChoice.hashCode().toLong() +
        typefaceHash * 131071L
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
  val pausedForCache by playback.pausedForCache.collectAsState()
  val speed by playback.speed.collectAsState()
  val coarsePos by playback.position.collectAsState()

  if (!enabled || comments.isEmpty()) return

  val density = LocalDensity.current.density
  val customFontPath by App.graph.porcelainPreferences.danmakuCustomFontPath.collectAsState()
  val customTypeface = remember(customFontPath) {
    CustomFontManager.loadTypeface(customFontPath)
  }
  val activeTypeface = remember(style.fontChoice, customTypeface) {
    when (DanmakuFontOption.fromId(style.fontChoice)) {
      DanmakuFontOption.System -> Typeface.DEFAULT
      DanmakuFontOption.Serif -> Typeface.SERIF
      DanmakuFontOption.Mono -> Typeface.MONOSPACE
      DanmakuFontOption.Custom -> customTypeface ?: Typeface.DEFAULT
    }
  }

  val engine = remember { DanmakuCanvasEngine() }

  // Phase-locked 120Hz video clock: integrates per-VSYNC delta and gently syncs to live mpv time-pos.
  val clock = remember {
    object {
      var videoSec: Double = playback.currentTimePos()
      var lastFrameNanos: Long = 0L
      var lastSyncNanos: Long = 0L
      var lastPolledMpvPos: Double = videoSec
      var stagnantSyncCount: Int = 0
    }
  }

  var frameTick by remember { mutableLongStateOf(0L) }

  // Immediately snap clock on seeks / pauses / file transitions, and repaint when style or font changes while paused
  LaunchedEffect(coarsePos, paused, pausedForCache, comments, style, activeTypeface) {
    val livePos = playback.currentTimePos()
    if (paused || pausedForCache || abs(livePos - clock.videoSec) > 0.6) {
      clock.videoSec = livePos
      clock.lastPolledMpvPos = livePos
      clock.stagnantSyncCount = 0
      clock.lastFrameNanos = 0L
      frameTick = System.nanoTime()
    }
  }

  // Native VSYNC loop (120Hz / 90Hz / 60Hz) running only while actively playing
  LaunchedEffect(enabled, paused, pausedForCache, speed, comments.isNotEmpty()) {
    clock.videoSec = playback.currentTimePos()
    clock.lastPolledMpvPos = clock.videoSec
    clock.stagnantSyncCount = 0
    clock.lastFrameNanos = 0L
    clock.lastSyncNanos = 0L

    if (paused || pausedForCache) {
      frameTick = System.nanoTime()
      return@LaunchedEffect
    }

    while (true) {
      withFrameNanos { frameNanos ->
        val prevNanos = clock.lastFrameNanos
        clock.lastFrameNanos = frameNanos
        if (prevNanos != 0L && clock.stagnantSyncCount < 5) {
          val dtSec = ((frameNanos - prevNanos).coerceIn(0L, 100_000_000L)) / 1_000_000_000.0
          clock.videoSec += dtSec * speed
        }

        // Poll exact double time-pos from mpv every ~250ms to stay phase-locked with zero stutter
        if (clock.lastSyncNanos == 0L || frameNanos - clock.lastSyncNanos >= 250_000_000L) {
          val hadPriorSync = clock.lastSyncNanos != 0L
          clock.lastSyncNanos = frameNanos
          val mpvPos = playback.currentTimePos()
          if (hadPriorSync && abs(mpvPos - clock.lastPolledMpvPos) < 0.001) {
            clock.stagnantSyncCount = (clock.stagnantSyncCount + 1).coerceAtMost(10)
          } else {
            clock.stagnantSyncCount = 0
          }
          clock.lastPolledMpvPos = mpvPos

          val drift = mpvPos - clock.videoSec
          when {
            clock.stagnantSyncCount >= 5 -> clock.videoSec = mpvPos
            abs(drift) > 0.35 -> clock.videoSec = mpvPos
            abs(drift) > 0.02 -> clock.videoSec += drift * 0.20
          }
        }

        frameTick = frameNanos
      }
    }
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    // Reading frameTick invalidates only the Draw phase at display VSYNC without recomposing!
    if (frameTick == Long.MIN_VALUE) return@Canvas
    val effectiveTime = (clock.videoSec + timeOffsetSec).toFloat().coerceAtLeast(0f)

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
