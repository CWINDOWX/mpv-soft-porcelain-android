package live.mehiz.mpvkt.bangumi

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import live.mehiz.mpvkt.preferences.BangumiSyncMode
import live.mehiz.mpvkt.preferences.PorcelainPreferences
import live.mehiz.mpvkt.ui.home.SmartTitleParser
import live.mehiz.mpvkt.ui.player.manager.PlaybackState
import java.util.concurrent.atomic.AtomicInteger

data class BangumiMatchState(
  val subjectId: Int,
  val subjectName: String,
  val season: Int = 1,
  val episodeNumber: Double? = null,
  val episodeTitle: String = "",
  val fromFolderBinding: Boolean = false,
) {
  val episodeLabel: String
    get() {
      val ep = episodeNumber ?: return ""
      val numStr = if (ep % 1.0 == 0.0) ep.toInt().toString() else ep.toString()
      return "第${numStr}话"
    }

  val richTopBarTitle: String
    get() = buildString {
      append(subjectName)
      if (episodeLabel.isNotBlank()) {
        append(" · ")
        append(episodeLabel)
      }
      if (episodeTitle.isNotBlank()) {
        append(" · ")
        append(episodeTitle)
      }
    }
}

class BangumiManager(
  private val context: Context,
  private val playback: PlaybackState,
  private val preferences: PorcelainPreferences,
  private val json: Json,
  private val scope: CoroutineScope,
) {
  companion object {
    private const val END_PROMPT_REMAINING_SEC = 90
    private const val MIN_DURATION_FOR_END_SYNC_SEC = 180
  }

  private val api = BangumiApi(json) { preferences.resolveBangumiApiBaseUrl() }
  private val loadGeneration = AtomicInteger(0)
  private val busyTokenSeq = AtomicInteger(0)
  @Volatile private var activeBusyToken: Int = 0

  private var currentFolderKey: String = ""
  private var currentFolderName: String = ""
  private var currentFileName: String = ""
  private var currentParsedQuery: String = ""
  private var endSyncTriggeredForFile: Boolean = false

  private val _user = MutableStateFlow(loadStoredUser())
  val user = _user.asStateFlow()

  private val _syncMode = MutableStateFlow(preferences.bangumiSyncMode.get())
  val syncMode = _syncMode.asStateFlow()

  private val _apiServer = MutableStateFlow(preferences.resolveBangumiApiBaseUrl())
  val apiServer = _apiServer.asStateFlow()

  private val _match = MutableStateFlow<BangumiMatchState?>(null)
  val match = _match.asStateFlow()

  private val _markedWatched = MutableStateFlow(false)
  val markedWatched = _markedWatched.asStateFlow()

  private val _showEndPrompt = MutableStateFlow(false)
  val showEndPrompt = _showEndPrompt.asStateFlow()

  private val _busy = MutableStateFlow(false)
  val busy = _busy.asStateFlow()

  private val _inlineStatus = MutableStateFlow<String?>(null)
  val inlineStatus = _inlineStatus.asStateFlow()

  private val _inlineError = MutableStateFlow<String?>(null)
  val inlineError = _inlineError.asStateFlow()

  private val _searchResults = MutableStateFlow<List<BangumiSubject>>(emptyList())
  val searchResults = _searchResults.asStateFlow()

  private val _searching = MutableStateFlow(false)
  val searching = _searching.asStateFlow()

  var onToast: (String) -> Unit = {}
  var onChineseTitleResolved: (chineseTitle: String, season: Int?, episode: Double?) -> Unit = { _, _, _ -> }

  init {
    combine(playback.position, playback.duration) { pos, dur -> pos to dur }
      .onEach { (pos, dur) -> evaluateEndSync(pos, dur) }
      .launchIn(scope)
  }

  val isLoggedIn: Boolean get() = _user.value != null && preferences.bangumiAccessToken.get().isNotBlank()

  private fun loadStoredUser(): BangumiUser? {
    val token = preferences.bangumiAccessToken.get().trim()
    val username = preferences.bangumiUsername.get().trim()
    if (token.isEmpty() || username.isEmpty()) return null
    val nickname = preferences.bangumiNickname.get().trim().ifBlank { username }
    return BangumiUser(username = username, nickname = nickname)
  }

  fun setSyncMode(mode: BangumiSyncMode) {
    _syncMode.value = mode
    preferences.bangumiSyncMode.set(mode)
    if (mode == BangumiSyncMode.Off) _showEndPrompt.value = false
  }

  fun setApiServer(rawUrl: String) {
    val normalized = PorcelainPreferences.normalizeMirrorUrl(rawUrl)
    if (normalized == null) {
      _inlineError.value = "请输入有效的 API / 镜像地址"
      return
    }
    _apiServer.value = normalized
    preferences.bangumiApiServer.set(normalized)
    _inlineError.value = null
    _inlineStatus.value = "已保存接口地址：$normalized"
  }

  fun resetApiServer() {
    val def = PorcelainPreferences.DEFAULT_BANGUMI_API
    _apiServer.value = def
    preferences.bangumiApiServer.set(def)
    _inlineError.value = null
    _inlineStatus.value = "已恢复官方地址：$def"
  }

  private fun acquireBusy(): Int {
    val token = busyTokenSeq.incrementAndGet()
    activeBusyToken = token
    _busy.value = true
    return token
  }

  private fun releaseBusy(token: Int) {
    if (activeBusyToken == token) {
      activeBusyToken = 0
      _busy.value = false
    }
  }

  private fun clearBusyForFileChange() {
    activeBusyToken = 0
    _busy.value = false
  }

  fun testMirrorConnection() {
    val target = preferences.resolveBangumiApiBaseUrl()
    val busyToken = acquireBusy()
    _inlineError.value = null
    _inlineStatus.value = "正在测试连通性：$target …"
    scope.launch(Dispatchers.IO) {
      try {
        api.testConnection()
          .onSuccess { ms ->
            _inlineStatus.value = "连接正常：$target ($ms ms)"
            _inlineError.value = null
          }
          .onFailure { e ->
            _inlineStatus.value = null
            _inlineError.value = "连接失败（$target）：${e.message ?: "无法访问"}"
          }
      } finally {
        releaseBusy(busyToken)
      }
    }
  }

  fun dismissEndPrompt() {
    _showEndPrompt.value = false
    endSyncTriggeredForFile = true
  }

  fun resetForNewFile() {
    loadGeneration.incrementAndGet()
    clearBusyForFileChange()
    endSyncTriggeredForFile = false
    _match.value = null
    _markedWatched.value = false
    _showEndPrompt.value = false
    _inlineStatus.value = null
    _inlineError.value = null
    _searchResults.value = emptyList()
  }

  fun onVideoLoaded(fileName: String, folderKey: String?, folderName: String?) {
    val gen = loadGeneration.incrementAndGet()
    clearBusyForFileChange()
    val normalizedFolderKey = BangumiLibraryStore.normalizeKey(folderKey)
    val resolvedFolderName = folderName.orEmpty()
    currentFileName = fileName
    currentFolderKey = normalizedFolderKey
    currentFolderName = resolvedFolderName
    endSyncTriggeredForFile = false
    _match.value = null
    _markedWatched.value = false
    _showEndPrompt.value = false
    _inlineStatus.value = null
    _inlineError.value = null
    _searchResults.value = emptyList()

    val parsed = SmartTitleParser.parse(fileName)
    val season = SmartTitleParser.extractSeason(fileName)
    val episode = parsed.episodeNumber?.toDoubleOrNull()
    val queryTitle = parsed.animeName.takeIf { it.length >= 2 }
      ?: folderName?.takeIf { it.isNotBlank() }
      ?: parsed.animeName
    currentParsedQuery = queryTitle

    scope.launch(Dispatchers.IO) {
      val token = preferences.bangumiAccessToken.get().takeIf { it.isNotBlank() }

      // 1. Check folder binding cache first
      if (normalizedFolderKey.isNotBlank()) {
        val binding = BangumiLibraryStore.loadFolderMap(context, json)[normalizedFolderKey]
        if (binding != null && gen == loadGeneration.get()) {
          val epTitle = if (episode != null) {
            resolveEpisodeInfo(binding.subjectId, episode, token, allowCache = true).getOrNull()?.displayTitle.orEmpty()
          } else {
            ""
          }
          if (gen != loadGeneration.get()) return@launch
          val matched = BangumiMatchState(
            subjectId = binding.subjectId,
            subjectName = binding.subjectName,
            season = season ?: 1,
            episodeNumber = episode,
            episodeTitle = epTitle,
            fromFolderBinding = true,
          )
          persistResolvedVideoMeta(fileName, normalizedFolderKey, matched)
          _match.value = matched
          onChineseTitleResolved(matched.subjectName, matched.season, matched.episodeNumber)
          return@launch
        }
      }

      // 2. Auto-search Bangumi by parsed title
      if (queryTitle.isBlank()) return@launch
      val queryWithSeason = if (season != null && season > 1 && !Regex("""第.{1,3}[季部期]|Season\s*\d""", RegexOption.IGNORE_CASE).containsMatchIn(queryTitle)) {
        "$queryTitle 第${season}季"
      } else {
        queryTitle
      }

      var items = api.searchSubjects(queryWithSeason, token).getOrNull().orEmpty()
      if (items.isEmpty() && queryWithSeason != queryTitle) {
        items = api.searchSubjects(queryTitle, token).getOrNull().orEmpty()
      }
      if (items.isEmpty() || gen != loadGeneration.get()) return@launch

      val best = api.pickBestSubject(items, season)
      val epTitle = if (episode != null) {
        resolveEpisodeInfo(best.id, episode, token, allowCache = true).getOrNull()?.displayTitle.orEmpty()
      } else {
        ""
      }
      if (gen != loadGeneration.get()) return@launch

      val matched = BangumiMatchState(
        subjectId = best.id,
        subjectName = best.displayName,
        season = season ?: 1,
        episodeNumber = episode,
        episodeTitle = epTitle,
        fromFolderBinding = false,
      )
      persistResolvedVideoMeta(fileName, normalizedFolderKey, matched)
      // Also cache auto-matched folder title for dedicated anime folders (skip generic dirs like Download/Movies)
      if (normalizedFolderKey.isNotBlank() && !BangumiLibraryStore.isGenericFolderName(resolvedFolderName)) {
        BangumiLibraryStore.saveFolderBinding(
          context = context,
          json = json,
          folderKey = normalizedFolderKey,
          binding = BangumiFolderBinding(
            subjectId = best.id,
            subjectName = best.displayName,
            isManual = false,
            updatedAt = System.currentTimeMillis(),
          ),
        )
      }
      _match.value = matched
      onChineseTitleResolved(matched.subjectName, matched.season, matched.episodeNumber)
    }
  }

  fun loginWithToken(rawToken: String) {
    val clean = rawToken.trim()
    if (clean.isEmpty()) {
      _inlineError.value = "请粘贴从 Bangumi 生成的 Access Token"
      return
    }
    val busyToken = acquireBusy()
    _inlineError.value = null
    _inlineStatus.value = "正在验证 Token…"
    scope.launch(Dispatchers.IO) {
      try {
        api.verifyToken(clean)
          .onSuccess { u ->
            preferences.bangumiAccessToken.set(clean)
            preferences.bangumiUsername.set(u.username)
            preferences.bangumiNickname.set(u.nickname)
            _user.value = u
            _inlineStatus.value = "已登录：${u.nickname} (@${u.username})"
            _inlineError.value = null
            withContext(Dispatchers.Main) { onToast("Bangumi 登录成功：${u.nickname}") }
          }
          .onFailure { e ->
            _inlineStatus.value = null
            _inlineError.value = e.message ?: "登录验证失败"
          }
      } finally {
        releaseBusy(busyToken)
      }
    }
  }

  fun logout() {
    preferences.bangumiAccessToken.set("")
    preferences.bangumiUsername.set("")
    preferences.bangumiNickname.set("")
    _user.value = null
    _showEndPrompt.value = false
    _inlineStatus.value = "已退出 Bangumi 登录"
    _inlineError.value = null
  }

  fun searchSubjects(keyword: String) {
    val q = keyword.trim()
    if (q.isEmpty()) return
    _searching.value = true
    _inlineError.value = null
    scope.launch(Dispatchers.IO) {
      val token = preferences.bangumiAccessToken.get().takeIf { it.isNotBlank() }
      api.searchSubjects(q, token)
        .onSuccess { list ->
          _searchResults.value = list
          if (list.isEmpty()) _inlineError.value = "未搜索到匹配的 Bangumi 条目"
        }
        .onFailure { e ->
          _searchResults.value = emptyList()
          _inlineError.value = e.message ?: "搜索失败"
        }
      _searching.value = false
    }
  }

  fun bindSubjectToCurrentFolder(subject: BangumiSubject) {
    // Advance generation so any slower in-flight auto-match cannot overwrite this manual binding
    val gen = loadGeneration.incrementAndGet()
    val capturedFileName = currentFileName
    val capturedFolderKey = currentFolderKey
    val parsed = SmartTitleParser.parse(capturedFileName)
    val episode = _match.value?.episodeNumber ?: parsed.episodeNumber?.toDoubleOrNull()
    val season = _match.value?.season ?: SmartTitleParser.extractSeason(capturedFileName) ?: 1

    val busyToken = acquireBusy()
    _inlineError.value = null
    _inlineStatus.value = "正在绑定《${subject.displayName}》…"

    scope.launch(Dispatchers.IO) {
      try {
        if (capturedFolderKey.isNotBlank()) {
          BangumiLibraryStore.saveFolderBinding(
            context = context,
            json = json,
            folderKey = capturedFolderKey,
            binding = BangumiFolderBinding(
              subjectId = subject.id,
              subjectName = subject.displayName,
              isManual = true,
              updatedAt = System.currentTimeMillis(),
            ),
          )
        }
        val token = preferences.bangumiAccessToken.get().takeIf { it.isNotBlank() }
        // Always ensure the subject's full episode list is fetched & cached so the video list shows episode titles too!
        val epTitle = if (episode != null) {
          resolveEpisodeInfo(subject.id, episode, token, allowCache = true).getOrNull()?.displayTitle.orEmpty()
        } else {
          resolveEpisodeInfo(subject.id, 1.0, token, allowCache = true)
          ""
        }
        if (gen != loadGeneration.get()) return@launch
        val updated = BangumiMatchState(
          subjectId = subject.id,
          subjectName = subject.displayName,
          season = season,
          episodeNumber = episode,
          episodeTitle = epTitle,
          fromFolderBinding = capturedFolderKey.isNotBlank(),
        )
        persistResolvedVideoMeta(capturedFileName, capturedFolderKey, updated)
        _match.value = updated
        _markedWatched.value = false
        _inlineStatus.value = if (capturedFolderKey.isNotBlank()) {
          "已绑定目录到《${subject.displayName}》并同步列表标题"
        } else {
          "已切换到《${subject.displayName}》并同步标题"
        }
        onChineseTitleResolved(updated.subjectName, updated.season, updated.episodeNumber)
        withContext(Dispatchers.Main) { onToast("已绑定：${subject.displayName}") }
      } finally {
        releaseBusy(busyToken)
      }
    }
  }

  fun markCurrentEpisodeWatched(fromAuto: Boolean = false) {
    val u = _user.value
    val token = preferences.bangumiAccessToken.get().trim()
    if (u == null || token.isEmpty()) {
      _inlineError.value = "请先粘贴 Access Token 登录 Bangumi"
      return
    }
    val m = _match.value
    if (m == null) {
      _inlineError.value = "尚未匹配到番剧，请先搜索并绑定"
      return
    }
    val epNum = m.episodeNumber
    if (epNum == null) {
      _inlineError.value = "未能从文件名识别当前集数"
      return
    }

    val gen = loadGeneration.get()
    val capturedFileName = currentFileName
    val capturedFolderKey = currentFolderKey
    val busyToken = acquireBusy()
    _inlineError.value = null
    _inlineStatus.value = "正在同步进度到 Bangumi…"
    _showEndPrompt.value = false

    scope.launch(Dispatchers.IO) {
      try {
        var epRes = resolveEpisodeInfo(m.subjectId, epNum, token, allowCache = true)
        if (epRes.isSuccess && epRes.getOrNull() == null) {
          epRes = resolveEpisodeInfo(m.subjectId, epNum, token, allowCache = false)
        }
        val epObj = epRes.getOrElse { e ->
          if (gen == loadGeneration.get()) {
            _inlineStatus.value = null
            _inlineError.value = "获取剧集失败：${e.message}"
          }
          return@launch
        }
        if (epObj == null) {
          if (gen == loadGeneration.get()) {
            _inlineStatus.value = null
            _inlineError.value = "未在 Bangumi 找到${m.episodeLabel}"
          }
          return@launch
        }

        api.markEpisodeWatched(u.username, token, m.subjectId, epObj.id)
          .onSuccess {
            val refreshed = if (epObj.displayTitle.isNotBlank() && m.episodeTitle.isBlank()) {
              m.copy(episodeTitle = epObj.displayTitle)
            } else {
              m
            }
            // Persist metadata under the episode that was actually marked watched (captured at request start)
            persistResolvedVideoMeta(capturedFileName, capturedFolderKey, refreshed)
            val msg = if (fromAuto) {
              "Bangumi 已自动同步：${refreshed.subjectName} ${refreshed.episodeLabel}"
            } else {
              "已同步：${refreshed.subjectName} ${refreshed.episodeLabel}"
            }
            // Only mutate live UI state if the user is still watching the same episode & subject
            if (gen == loadGeneration.get() && _match.value?.subjectId == m.subjectId && _match.value?.episodeNumber == epNum) {
              if (refreshed != m) _match.value = refreshed
              _markedWatched.value = true
              endSyncTriggeredForFile = true
              _inlineStatus.value = msg
              _inlineError.value = null
            }
            withContext(Dispatchers.Main) { onToast(msg) }
          }
          .onFailure { e ->
            if (gen == loadGeneration.get()) {
              _inlineStatus.value = null
              _inlineError.value = e.message ?: "同步失败"
              if (!fromAuto) {
                withContext(Dispatchers.Main) { onToast("同步失败：${e.message}") }
              }
            }
          }
      } finally {
        releaseBusy(busyToken)
      }
    }
  }

  fun initialSearchKeyword(): String =
    _match.value?.subjectName?.ifBlank { currentParsedQuery } ?: currentParsedQuery

  private fun persistResolvedVideoMeta(fileName: String, folderKey: String?, state: BangumiMatchState) {
    if (fileName.isBlank()) return
    BangumiLibraryStore.saveVideoMeta(
      context = context,
      json = json,
      fileName = fileName,
      folderKey = folderKey,
      meta = BangumiVideoMeta(
        subjectId = state.subjectId,
        subjectName = state.subjectName,
        episodeNumber = state.episodeNumber,
        episodeTitle = state.episodeTitle,
        updatedAt = System.currentTimeMillis(),
      ),
    )
  }

  private fun evaluateEndSync(positionSec: Int, durationSec: Int) {
    if (endSyncTriggeredForFile || _markedWatched.value) return
    val mode = _syncMode.value
    if (mode == BangumiSyncMode.Off) return
    if (!isLoggedIn) return
    val m = _match.value ?: return
    if (m.episodeNumber == null) return
    if (durationSec < MIN_DURATION_FOR_END_SYNC_SEC) return
    val remaining = durationSec - positionSec
    if (remaining in 1..END_PROMPT_REMAINING_SEC) {
      endSyncTriggeredForFile = true
      when (mode) {
        BangumiSyncMode.Auto -> markCurrentEpisodeWatched(fromAuto = true)
        BangumiSyncMode.Prompt -> _showEndPrompt.value = true
        BangumiSyncMode.Off -> {}
      }
    }
  }

  private suspend fun resolveEpisodeInfo(
    subjectId: Int,
    targetEp: Double,
    token: String?,
    allowCache: Boolean,
  ): Result<BangumiEpisode?> {
    val cache = BangumiLibraryStore.loadEpisodesCache(context, json)
    val sidKey = subjectId.toString()
    val nowSec = System.currentTimeMillis() / 1000L

    if (allowCache) {
      val entry = cache[sidKey]
      if (entry != null) {
        val age = nowSec - entry.updatedAtSec
        val found = api.findEpisodeInList(entry.episodes, targetEp)
        if (found != null) {
          val hasTitle = found.displayTitle.isNotBlank()
          if ((hasTitle && age < 7 * 86_400L) || age < 3 * 3600L) {
            return Result.success(found)
          }
        } else if (entry.complete && age < 3 * 3600L) {
          return Result.success(null)
        }
      }
    }

    val fetched = api.fetchEpisodes(subjectId, token)
    return fetched.fold(
      onSuccess = { res ->
        cache[sidKey] = BangumiEpisodesCacheEntry(
          updatedAtSec = nowSec,
          episodes = res.episodes,
          complete = res.complete,
        )
        BangumiLibraryStore.saveEpisodesCache(context, json, cache)
        Result.success(api.findEpisodeInList(res.episodes, targetEp))
      },
      onFailure = { err ->
        val stale = cache[sidKey]?.let { api.findEpisodeInList(it.episodes, targetEp) }
        if (stale != null) Result.success(stale) else Result.failure(err)
      },
    )
  }
}
