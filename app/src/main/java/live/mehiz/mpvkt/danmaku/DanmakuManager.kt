package live.mehiz.mpvkt.danmaku

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import live.mehiz.mpvkt.bangumi.BangumiLibraryStore
import live.mehiz.mpvkt.preferences.PorcelainPreferences
import live.mehiz.mpvkt.ui.home.SmartTitleParser
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class DanmakuManager(
  private val context: Context,
  private val preferences: PorcelainPreferences,
  private val json: Json,
  private val scope: CoroutineScope,
) {
  companion object {
    private const val TAG = "DanmakuManager"
    private const val MAX_HISTORY_ENTRIES = 300
  }

  private val api = DandanplayApi(json) { preferences.danmakuApiServer.get() }
  private val storeDir = File(context.filesDir, "porcelain").apply { mkdirs() }
  private val historyFile = File(storeDir, "danmaku_history.json")
  private val loadGeneration = AtomicInteger(0)

  private var currentFileName: String = ""
  private var currentHistoryKey: String = ""
  private var rawComments: List<DanmakuItem> = emptyList()
  private var currentEpisodeId: Long? = null

  private val _enabled = MutableStateFlow(preferences.danmakuEnabled.get())
  val enabled = _enabled.asStateFlow()

  private val _style = MutableStateFlow(loadStyleFromPrefs())
  val style = _style.asStateFlow()

  init {
    scope.launch {
      preferences.danmakuFontChoice.changes().collect { choice ->
        if (_style.value.fontChoice != choice) {
          _style.update { it.copy(fontChoice = choice) }
        }
      }
    }
  }

  private val _comments = MutableStateFlow<List<DanmakuItem>>(emptyList())
  val comments = _comments.asStateFlow()

  private val _sourceInfo = MutableStateFlow<DanmakuSourceInfo?>(null)
  val sourceInfo = _sourceInfo.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading = _loading.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage = _statusMessage.asStateFlow()

  private val _timeOffsetSec = MutableStateFlow(0f)
  val timeOffsetSec = _timeOffsetSec.asStateFlow()

  // Manual search sheet state
  private val _searchResults = MutableStateFlow<List<DanmakuSearchAnime>>(emptyList())
  val searchResults = _searchResults.asStateFlow()

  private val _selectedAnime = MutableStateFlow<Pair<DanmakuSearchAnime, List<DanmakuSearchEpisode>>?>(null)
  val selectedAnime = _selectedAnime.asStateFlow()

  private val _searching = MutableStateFlow(false)
  val searching = _searching.asStateFlow()

  private val _searchError = MutableStateFlow<String?>(null)
  val searchError = _searchError.asStateFlow()

  var onToast: (String) -> Unit = {}

  private fun loadStyleFromPrefs(): DanmakuStyleConfig = DanmakuStyleConfig(
    fontSize = preferences.danmakuFontSize.get().coerceIn(20, 60),
    opacity = preferences.danmakuOpacity.get().coerceIn(0.15f, 1.0f),
    displayArea = preferences.danmakuDisplayArea.get().coerceIn(0.20f, 1.0f),
    scrollSeconds = preferences.danmakuScrollSeconds.get().coerceIn(6, 20),
    outline = preferences.danmakuOutline.get().coerceIn(0f, 3.5f),
    shadow = preferences.danmakuShadow.get().coerceIn(0f, 2.5f),
    maxOnScreen = preferences.danmakuMaxOnScreen.get().coerceAtLeast(0),
    bold = preferences.danmakuBold.get(),
    blockTop = preferences.danmakuBlockTop.get(),
    blockBottom = preferences.danmakuBlockBottom.get(),
    blockScroll = preferences.danmakuBlockScroll.get(),
    blockColor = preferences.danmakuBlockColor.get(),
    mergeDuplicates = preferences.danmakuMergeDuplicates.get(),
    chConvert = preferences.danmakuChConvert.get().coerceIn(0, 2),
    fontChoice = preferences.danmakuFontChoice.get(),
  )

  private fun saveStyleToPrefs(cfg: DanmakuStyleConfig) {
    preferences.danmakuFontSize.set(cfg.fontSize)
    preferences.danmakuOpacity.set(cfg.opacity)
    preferences.danmakuDisplayArea.set(cfg.displayArea)
    preferences.danmakuScrollSeconds.set(cfg.scrollSeconds)
    preferences.danmakuOutline.set(cfg.outline)
    preferences.danmakuShadow.set(cfg.shadow)
    preferences.danmakuMaxOnScreen.set(cfg.maxOnScreen)
    preferences.danmakuBold.set(cfg.bold)
    preferences.danmakuBlockTop.set(cfg.blockTop)
    preferences.danmakuBlockBottom.set(cfg.blockBottom)
    preferences.danmakuBlockScroll.set(cfg.blockScroll)
    preferences.danmakuBlockColor.set(cfg.blockColor)
    preferences.danmakuMergeDuplicates.set(cfg.mergeDuplicates)
    preferences.danmakuChConvert.set(cfg.chConvert)
    preferences.danmakuFontChoice.set(cfg.fontChoice)
  }

  fun setEnabled(value: Boolean, showToast: Boolean = false) {
    _enabled.value = value
    preferences.danmakuEnabled.set(value)
    if (showToast) onToast(if (value) "弹幕：开" else "弹幕：关")
  }

  fun toggleEnabled(showToast: Boolean = true) = setEnabled(!_enabled.value, showToast)

  fun adjustTimeOffset(deltaSec: Float) {
    _timeOffsetSec.update { ((it + deltaSec) * 10f).toInt() / 10f }
  }

  fun resetTimeOffset() {
    _timeOffsetSec.value = 0f
  }

  fun updateStyle(transform: (DanmakuStyleConfig) -> DanmakuStyleConfig) {
    val prev = _style.value
    val next = transform(prev)
    _style.value = next
    saveStyleToPrefs(next)

    if (prev.mergeDuplicates != next.mergeDuplicates) {
      publishComments(rawComments, next.mergeDuplicates)
    }
    if (prev.chConvert != next.chConvert) {
      val epId = currentEpisodeId
      val info = _sourceInfo.value
      if (epId != null && info != null && info.sourceType == "dandanplay") {
        val gen = loadGeneration.get()
        scope.launch(Dispatchers.IO) {
          fetchAndActivateEpisode(gen, epId, info.animeTitle, info.episodeTitle, saveHistory = false)
        }
      }
    }
  }

  fun applyPreset(preset: DanmakuPreset) {
    updateStyle(preset.apply)
  }

  fun resetStyle() {
    updateStyle { DanmakuStyleConfig() }
  }

  fun resetForNewFile() {
    loadGeneration.incrementAndGet()
    currentFileName = ""
    currentHistoryKey = ""
    rawComments = emptyList()
    currentEpisodeId = null
    _comments.value = emptyList()
    _sourceInfo.value = null
    _loading.value = false
    _statusMessage.value = null
    _timeOffsetSec.value = 0f
    _searchResults.value = emptyList()
    _selectedAnime.value = null
    _searchError.value = null
  }

  fun onVideoLoaded(uri: Uri?, fileName: String, folderKey: String? = null, filePath: String? = null) {
    val gen = loadGeneration.incrementAndGet()
    val resolvedKey = BangumiLibraryStore.videoCompositeKey(folderKey, fileName, uri?.toString())
    currentFileName = fileName
    currentHistoryKey = resolvedKey
    rawComments = emptyList()
    currentEpisodeId = null
    _comments.value = emptyList()
    _sourceInfo.value = null
    _timeOffsetSec.value = 0f
    _searchResults.value = emptyList()
    _selectedAnime.value = null
    _searchError.value = null

    if (!preferences.danmakuAutoLoad.get()) {
      _loading.value = false
      _statusMessage.value = "自动加载已关闭，可手动搜索弹幕"
      return
    }

    _loading.value = true
    _statusMessage.value = "正在匹配弹幕…"

    scope.launch(Dispatchers.IO) {
      // 1. Check sibling local .xml danmaku file first if filesystem path is readable
      val siblingXml = findSiblingXml(filePath)
      if (siblingXml != null) {
        val items = runCatching {
          FileInputStream(siblingXml).use { DanmakuXmlParser.parseBilibiliXml(it) }
        }.getOrDefault(emptyList())
        if (items.isNotEmpty() && gen == loadGeneration.get()) {
          activateLocalItems(siblingXml.name, items)
          return@launch
        }
      }

      // 2. Check persisted episode history using directory-scoped key (only fallback to bare filename if distinctive)
      val historyMap = loadHistory()
      val cached = historyMap[resolvedKey]
        ?: if (BangumiLibraryStore.isDistinctLegacyFileName(fileName)) {
          historyMap[historyKey(fileName)]
        } else {
          null
        }
      if (cached != null && gen == loadGeneration.get()) {
        val ok = fetchAndActivateEpisode(
          gen = gen,
          episodeId = cached.episodeId,
          animeTitle = cached.animeTitle,
          episodeTitle = cached.episodeTitle,
          saveHistory = false,
        )
        if (ok) return@launch
      }

      // 3. Hash match (first 16 MiB in 64 KiB chunks) + cleaned filename
      val parsed = SmartTitleParser.parse(fileName)
      val hash = api.computeVideoHash(context, uri, filePath)
      if (gen != loadGeneration.get()) return@launch

      if (hash != null) {
        val cleanHintName = if (parsed.episodeNumber != null) {
          "${parsed.animeName} E${parsed.episodeNumber}"
        } else {
          parsed.animeName.ifBlank { fileName }
        }
        val hashRes = api.matchByHash(cleanHintName, hash)
        val matched = hashRes.getOrNull()
        if (matched != null && gen == loadGeneration.get()) {
          val ok = fetchAndActivateEpisode(
            gen = gen,
            episodeId = matched.episodeId,
            animeTitle = matched.animeTitle,
            episodeTitle = matched.episodeTitle,
            saveHistory = true,
          )
          if (ok) return@launch
        }
      }

      // 4. Fallback: search by parsed title + episode number
      val epNum = parsed.episodeNumber?.toDoubleOrNull()
      val seasonNum = SmartTitleParser.extractSeason(fileName)
      if (parsed.animeName.isNotBlank() && epNum != null && gen == loadGeneration.get()) {
        val titleRes = api.matchByTitle(parsed.animeName, seasonNum, epNum)
        val matched = titleRes.getOrNull()
        if (matched != null && gen == loadGeneration.get()) {
          val ok = fetchAndActivateEpisode(
            gen = gen,
            episodeId = matched.episodeId,
            animeTitle = matched.animeTitle,
            episodeTitle = matched.episodeTitle,
            saveHistory = true,
          )
          if (ok) return@launch
        }
      }

      if (gen == loadGeneration.get()) {
        _loading.value = false
        _statusMessage.value = "未自动匹配到弹幕，可点弹幕图标手动搜索"
      }
    }
  }

  /**
   * Called by BangumiManager when Bangumi resolves a Chinese anime title (e.g. for English/Romaji filenames)
   * and DanmakuManager has not loaded any danmaku yet.
   */
  fun onBangumiChineseTitleFallback(chineseTitle: String, season: Int?, episode: Double?) {
    if (chineseTitle.isBlank() || episode == null) return
    if (_sourceInfo.value != null) return // Already matched!
    val gen = loadGeneration.get()
    scope.launch(Dispatchers.IO) {
      if (_sourceInfo.value != null || gen != loadGeneration.get()) return@launch
      _loading.value = true
      _statusMessage.value = "通过 Bangumi 中文名补充匹配弹幕…"
      val matched = api.matchByTitle(chineseTitle, season, episode).getOrNull()
      if (matched != null && gen == loadGeneration.get() && _sourceInfo.value == null) {
        val ok = fetchAndActivateEpisode(
          gen = gen,
          episodeId = matched.episodeId,
          animeTitle = matched.animeTitle,
          episodeTitle = matched.episodeTitle,
          saveHistory = true,
        )
        if (ok) return@launch
      }
      if (gen == loadGeneration.get() && _sourceInfo.value == null) {
        _loading.value = false
        _statusMessage.value = "未自动匹配到弹幕，可点弹幕图标手动搜索"
      }
    }
  }

  fun searchManual(keyword: String) {
    val q = keyword.trim()
    if (q.isEmpty()) return
    _searching.value = true
    _searchError.value = null
    _selectedAnime.value = null
    scope.launch(Dispatchers.IO) {
      api.searchAnime(q)
        .onSuccess { list ->
          _searchResults.value = list
          if (list.isEmpty()) _searchError.value = "未搜索到相关番剧"
        }
        .onFailure { e ->
          _searchResults.value = emptyList()
          _searchError.value = e.message ?: "搜索失败"
        }
      _searching.value = false
    }
  }

  fun openAnimeEpisodes(anime: DanmakuSearchAnime) {
    _searching.value = true
    _searchError.value = null
    scope.launch(Dispatchers.IO) {
      api.getEpisodes(anime.bangumiId)
        .onSuccess { eps ->
          _selectedAnime.value = anime to eps
          if (eps.isEmpty()) _searchError.value = "该番剧暂无剧集列表"
        }
        .onFailure { e ->
          _searchError.value = e.message ?: "获取剧集失败"
        }
      _searching.value = false
    }
  }

  fun clearSelectedAnime() {
    _selectedAnime.value = null
    _searchError.value = null
  }

  fun selectSearchEpisode(animeTitle: String, episode: DanmakuSearchEpisode) {
    val gen = loadGeneration.get()
    _loading.value = true
    _statusMessage.value = "正在加载弹幕…"
    scope.launch(Dispatchers.IO) {
      val ok = fetchAndActivateEpisode(
        gen = gen,
        episodeId = episode.episodeId,
        animeTitle = animeTitle,
        episodeTitle = episode.episodeTitle.ifBlank { "第${episode.episodeNumber}话" },
        saveHistory = true,
      )
      if (ok && !_enabled.value) {
        setEnabled(true, showToast = false)
      }
    }
  }

  fun loadLocalXmlUri(uri: Uri, label: String = "本地弹幕") {
    val gen = loadGeneration.get()
    _loading.value = true
    _statusMessage.value = "正在解析本地弹幕…"
    scope.launch(Dispatchers.IO) {
      val parsed = runCatching {
        context.contentResolver.openInputStream(uri)?.use { DanmakuXmlParser.parseBilibiliXml(it) }
          ?: emptyList()
      }.getOrElse { e ->
        Log.e(TAG, "Failed to parse local XML", e)
        emptyList()
      }
      if (gen != loadGeneration.get()) return@launch
      if (parsed.isEmpty()) {
        _loading.value = false
        _statusMessage.value = "本地弹幕文件为空或格式不支持"
        withContext(Dispatchers.Main) { onToast("未能解析出弹幕内容") }
      } else {
        activateLocalItems(label, parsed)
        if (!_enabled.value) setEnabled(true, showToast = false)
      }
    }
  }

  private suspend fun fetchAndActivateEpisode(
    gen: Int,
    episodeId: Long,
    animeTitle: String,
    episodeTitle: String,
    saveHistory: Boolean,
  ): Boolean {
    val res = api.fetchComments(episodeId, _style.value.chConvert)
    if (gen != loadGeneration.get()) return false
    return res.fold(
      onSuccess = { list ->
        rawComments = list
        currentEpisodeId = episodeId
        publishComments(list, _style.value.mergeDuplicates)
        _sourceInfo.value = DanmakuSourceInfo(
          sourceType = "dandanplay",
          animeTitle = animeTitle,
          episodeTitle = episodeTitle,
          episodeId = episodeId,
          totalCount = list.size,
        )
        _loading.value = false
        _statusMessage.value = if (list.isEmpty()) {
          "已匹配《$animeTitle》，但该话暂无弹幕"
        } else {
          "已加载 ${list.size} 条弹幕"
        }
        val keyToSave = currentHistoryKey.ifBlank { historyKey(currentFileName) }
        if (saveHistory && keyToSave.isNotBlank()) {
          saveHistoryEntry(
            keyToSave,
            DanmakuHistoryEntry(
              episodeId = episodeId,
              animeTitle = animeTitle,
              episodeTitle = episodeTitle,
              updatedAt = System.currentTimeMillis(),
            ),
          )
        }
        withContext(Dispatchers.Main) {
          if (list.isNotEmpty()) onToast("弹幕已加载 · ${list.size} 条")
        }
        true
      },
      onFailure = { e ->
        _loading.value = false
        _statusMessage.value = e.message ?: "弹幕加载失败"
        false
      },
    )
  }

  private suspend fun activateLocalItems(label: String, items: List<DanmakuItem>) {
    rawComments = items
    currentEpisodeId = null
    publishComments(items, _style.value.mergeDuplicates)
    _sourceInfo.value = DanmakuSourceInfo(
      sourceType = "local_xml",
      animeTitle = label,
      episodeTitle = "本地 XML",
      episodeId = null,
      totalCount = items.size,
    )
    _loading.value = false
    _statusMessage.value = "已加载本地弹幕 ${items.size} 条"
    withContext(Dispatchers.Main) {
      onToast("已加载本地弹幕 · ${items.size} 条")
    }
  }

  private fun publishComments(raw: List<DanmakuItem>, merge: Boolean) {
    _comments.value = if (merge) DanmakuXmlParser.mergeDuplicates(raw, 5f) else raw
  }

  private fun findSiblingXml(filePath: String?): File? {
    if (filePath.isNullOrBlank()) return null
    val videoFile = File(filePath)
    val parent = videoFile.parentFile ?: return null
    if (!parent.exists() || !parent.canRead()) return null
    val exact = File(parent, "${videoFile.nameWithoutExtension}.xml")
    if (exact.exists() && exact.isFile && exact.length() > 0L) return exact
    return null
  }

  private fun historyKey(name: String): String = name.trim().lowercase(Locale.ROOT)

  private fun loadHistory(): MutableMap<String, DanmakuHistoryEntry> = runCatching {
    if (!historyFile.exists()) return mutableMapOf()
    json.decodeFromString<Map<String, DanmakuHistoryEntry>>(historyFile.readText()).toMutableMap()
  }.getOrElse { mutableMapOf() }

  private fun saveHistoryEntry(key: String, entry: DanmakuHistoryEntry) {
    runCatching {
      val norm = key.trim().lowercase(Locale.ROOT)
      if (norm.isEmpty()) return@runCatching
      val map = loadHistory()
      map[norm] = entry
      val trimmed = if (map.size > MAX_HISTORY_ENTRIES) {
        map.entries.sortedByDescending { it.value.updatedAt }.take(MAX_HISTORY_ENTRIES).associate { it.toPair() }
      } else {
        map
      }
      historyFile.writeText(json.encodeToString(trimmed))
    }
  }
}
