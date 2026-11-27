package live.mehiz.mpvkt.ui.player.manager

import dev.vivvvek.seeker.Segment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.preferences.ChapterSkipMode
import live.mehiz.mpvkt.preferences.PorcelainPreferences
import java.util.Locale

enum class SkipChapterKind(val label: String) {
  OP("片头 (OP)"),
  ED("片尾 (ED)"),
  Preview("预告"),
}

data class DetectedSkipChapter(
  val index: Int,
  val kind: SkipChapterKind,
  val rawTitle: String,
  val startSec: Int,
  val endSec: Int,
  val isLast: Boolean,
)

data class UndoSkipInfo(
  val fromSec: Int,
  val label: String,
)

class ChapterSkipManager(
  private val playback: PlaybackState,
  private val preferences: PorcelainPreferences,
  private val scope: CoroutineScope,
) {
  val mode: StateFlow<ChapterSkipMode> = preferences.chapterSkipMode.stateIn(scope)
  val skipOp: StateFlow<Boolean> = preferences.skipOp.stateIn(scope)
  val skipEd: StateFlow<Boolean> = preferences.skipEd.stateIn(scope)
  val skipPreview: StateFlow<Boolean> = preferences.skipPreview.stateIn(scope)

  private val _detectedChapters = MutableStateFlow<List<DetectedSkipChapter>>(emptyList())
  val detectedChapters = _detectedChapters.asStateFlow()

  private val _activePrompt = MutableStateFlow<DetectedSkipChapter?>(null)
  val activePrompt = _activePrompt.asStateFlow()

  private val _undoBanner = MutableStateFlow<UndoSkipInfo?>(null)
  val undoBanner = _undoBanner.asStateFlow()

  private val handledIndices = mutableSetOf<Int>()
  private var undoClearJob: Job? = null

  init {
    combine(playback.chapters, playback.duration) { chapters, duration ->
      detectChapters(chapters, duration)
    }.onEach { detected ->
      _detectedChapters.value = detected
    }.launchIn(scope)

    combine(
      playback.position,
      _detectedChapters,
      mode,
      skipOp,
      skipEd,
    ) { pos, detected, curMode, opOn, edOn ->
      EvalState(pos, detected, curMode, opOn, edOn, skipPreview.value)
    }.onEach { st ->
      evaluatePosition(st)
    }.launchIn(scope)
  }

  private data class EvalState(
    val positionSec: Int,
    val detected: List<DetectedSkipChapter>,
    val mode: ChapterSkipMode,
    val skipOp: Boolean,
    val skipEd: Boolean,
    val skipPreview: Boolean,
  )

  fun resetForNewFile() {
    handledIndices.clear()
    _activePrompt.value = null
    _undoBanner.value = null
    undoClearJob?.cancel()
  }

  fun setMode(newMode: ChapterSkipMode) {
    preferences.chapterSkipMode.set(newMode)
    if (newMode == ChapterSkipMode.Off) {
      _activePrompt.value = null
    }
  }

  fun setSkipOp(enabled: Boolean) = preferences.skipOp.set(enabled)
  fun setSkipEd(enabled: Boolean) = preferences.skipEd.set(enabled)
  fun setSkipPreview(enabled: Boolean) = preferences.skipPreview.set(enabled)

  fun skipActive() {
    val ch = _activePrompt.value ?: return
    executeSkip(ch, showUndo = false)
  }

  fun dismissActive() {
    val ch = _activePrompt.value ?: return
    handledIndices.add(ch.index)
    _activePrompt.value = null
  }

  fun undoLastSkip() {
    val info = _undoBanner.value ?: return
    undoClearJob?.cancel()
    _undoBanner.value = null
    playback.seekTo(info.fromSec, precise = true)
  }

  private fun executeSkip(chapter: DetectedSkipChapter, showUndo: Boolean) {
    val fromSec = playback.position.value
    handledIndices.add(chapter.index)
    _activePrompt.value = null
    val duration = playback.duration.value
    val target = if (chapter.isLast && duration > 2) {
      (duration - 2).coerceAtLeast(chapter.startSec)
    } else {
      chapter.endSec.coerceAtMost((duration - 1).coerceAtLeast(0))
    }
    playback.seekTo(target, precise = true)
    if (showUndo) {
      _undoBanner.value = UndoSkipInfo(fromSec = fromSec, label = chapter.kind.label)
      undoClearJob?.cancel()
      undoClearJob = scope.launch {
        delay(5000L)
        _undoBanner.value = null
      }
    }
  }

  private fun isKindEnabled(kind: SkipChapterKind, st: EvalState): Boolean = when (kind) {
    SkipChapterKind.OP -> st.skipOp
    SkipChapterKind.ED -> st.skipEd
    SkipChapterKind.Preview -> st.skipPreview
  }

  private fun evaluatePosition(st: EvalState) {
    if (st.mode == ChapterSkipMode.Off || st.detected.isEmpty()) {
      _activePrompt.value = null
      return
    }
    val current = st.detected.firstOrNull { ch ->
      isKindEnabled(ch.kind, st) &&
        st.positionSec >= ch.startSec &&
        st.positionSec < (ch.endSec - 2).coerceAtLeast(ch.startSec + 1)
    }
    if (current == null) {
      _activePrompt.value = null
      return
    }
    if (current.index in handledIndices) {
      if (_activePrompt.value?.index == current.index) _activePrompt.value = null
      return
    }
    when (st.mode) {
      ChapterSkipMode.Prompt -> _activePrompt.value = current
      ChapterSkipMode.Auto -> executeSkip(current, showUndo = true)
      ChapterSkipMode.Off -> _activePrompt.value = null
    }
  }

  companion object {
    private val opExactRegex = Regex("""^op([\s\d._\-:]+|$)""")
    private val opParenRegex = Regex("""^op\d*\s*[\-(（【]""")
    private val opWordRegex = Regex("""\b(opening|intro|title\s*sequence)\b""")
    private val edExactRegex = Regex("""^ed([\s\d._\-:]+|$)""")
    private val edParenRegex = Regex("""^ed\d*\s*[\-(（【]""")
    private val edWordRegex = Regex("""\b(ending|outro|credits?|closing)\b""")
    private val nextEpRegex = Regex("""next\s+ep(isode)?""")

    fun detectChapters(chapters: List<Segment>, durationSec: Int): List<DetectedSkipChapter> {
      if (chapters.isEmpty()) return emptyList()
      val out = mutableListOf<DetectedSkipChapter>()
      for (i in chapters.indices) {
        val seg = chapters[i]
        val start = seg.start.toInt().coerceAtLeast(0)
        val end = if (i + 1 < chapters.size) {
          chapters[i + 1].start.toInt()
        } else {
          durationSec
        }
        val segDur = end - start
        if (segDur < 15 || segDur > 240) continue
        if (durationSec > 0 && segDur > durationSec * 0.45f) continue

        val kind = classifyTitle(seg.name, start, durationSec) ?: continue
        out.add(
          DetectedSkipChapter(
            index = i,
            kind = kind,
            rawTitle = seg.name,
            startSec = start,
            endSec = end,
            isLast = i == chapters.lastIndex,
          ),
        )
      }
      return out
    }

    private fun classifyTitle(title: String, startSec: Int, durationSec: Int): SkipChapterKind? {
      val trimmed = title.trim()
      if (trimmed.isEmpty()) return null
      val low = trimmed.lowercase(Locale.ROOT)

      val isPreview = low.contains("preview") ||
        nextEpRegex.containsMatchIn(low) ||
        trimmed.contains("预告") ||
        trimmed.contains("預告") ||
        trimmed.contains("次回")
      if (isPreview && (durationSec <= 0 || startSec >= durationSec * 0.55f)) {
        return SkipChapterKind.Preview
      }

      val isOp = opExactRegex.matches(low) ||
        opParenRegex.containsMatchIn(low) ||
        opWordRegex.containsMatchIn(low) ||
        low.startsWith("ncop") ||
        trimmed.contains("片头") ||
        trimmed.contains("片頭") ||
        trimmed.contains("主题曲") ||
        trimmed.contains("主題曲") ||
        trimmed.contains("オープニング")
      if (isOp && (durationSec <= 0 || startSec <= durationSec * 0.55f)) {
        return SkipChapterKind.OP
      }

      val isEd = edExactRegex.matches(low) ||
        edParenRegex.containsMatchIn(low) ||
        edWordRegex.containsMatchIn(low) ||
        low.startsWith("nced") ||
        trimmed.contains("片尾") ||
        trimmed.contains("职员表") ||
        trimmed.contains("職員表") ||
        trimmed.contains("エンディング")
      if (isEd && (durationSec <= 0 || startSec >= durationSec * 0.40f)) {
        return SkipChapterKind.ED
      }

      return null
    }
  }
}
