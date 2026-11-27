package live.mehiz.mpvkt.bangumi

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import live.mehiz.mpvkt.ui.home.SmartTitleParser
import java.io.File
import java.util.Locale
import kotlin.math.abs

@Serializable
data class BangumiVideoMeta(
  val subjectId: Int,
  val subjectName: String,
  val episodeNumber: Double? = null,
  val episodeTitle: String = "",
  val updatedAt: Long = 0L,
)

data class ResolvedBangumiTitle(
  val subjectName: String,
  val episodeNumber: String?,
  val episodeTitle: String,
  val displayTitle: String,
)

data class BangumiLibrarySnapshot(
  val folderMap: Map<String, BangumiFolderBinding>,
  val videoTitles: Map<String, BangumiVideoMeta>,
  val episodesCache: Map<String, BangumiEpisodesCacheEntry>,
) {
  fun resolveFolderName(folderKey: String?): String? {
    val key = BangumiLibraryStore.normalizeKey(folderKey)
    if (key.isEmpty()) return null
    return folderMap[key]?.subjectName?.takeIf { it.isNotBlank() }
  }

  fun resolveVideoTitle(fileName: String, folderKey: String? = null): ResolvedBangumiTitle? {
    val fileKey = BangumiLibraryStore.normalizeKey(fileName)
    val fKey = BangumiLibraryStore.normalizeKey(folderKey)
    val folderBinding = if (fKey.isNotEmpty()) folderMap[fKey] else null
    val videoMeta = if (fileKey.isNotEmpty()) videoTitles[fileKey] else null

    // Manual folder binding wins so re-binding a folder immediately fixes all episodes in it.
    if (folderBinding != null && folderBinding.isManual) {
      return buildFromBinding(folderBinding, fileName)
    }

    if (videoMeta != null) {
      val epNum = videoMeta.episodeNumber ?: SmartTitleParser.parse(fileName).episodeNumber?.toDoubleOrNull()
      val epTitle = videoMeta.episodeTitle.ifBlank { lookupCachedEpisodeTitle(videoMeta.subjectId, epNum) }
      return formatResolved(videoMeta.subjectName, epNum, epTitle)
    }

    if (folderBinding != null) {
      return buildFromBinding(folderBinding, fileName)
    }

    return null
  }

  private fun buildFromBinding(binding: BangumiFolderBinding, fileName: String): ResolvedBangumiTitle {
    val parsed = SmartTitleParser.parse(fileName)
    val epNum = parsed.episodeNumber?.toDoubleOrNull()
    val epTitle = lookupCachedEpisodeTitle(binding.subjectId, epNum)
    return formatResolved(binding.subjectName, epNum, epTitle)
  }

  private fun lookupCachedEpisodeTitle(subjectId: Int, epNum: Double?): String {
    if (epNum == null) return ""
    val episodes = episodesCache[subjectId.toString()]?.episodes ?: return ""
    var sortMatch: BangumiEpisode? = null
    for (ep in episodes) {
      if (ep.ep != null && abs(ep.ep - epNum) < 1e-4) return ep.displayTitle
      if (sortMatch == null && ep.sort != null && abs(ep.sort - epNum) < 1e-4) sortMatch = ep
    }
    return sortMatch?.displayTitle.orEmpty()
  }

  private fun formatResolved(
    subjectName: String,
    epNum: Double?,
    epTitle: String,
  ): ResolvedBangumiTitle {
    val epStr = epNum?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
    val display = buildString {
      append(subjectName)
      if (!epStr.isNullOrBlank()) {
        append(" · 第")
        append(epStr)
        append("话")
      }
      if (epTitle.isNotBlank()) {
        append(" · ")
        append(epTitle)
      }
    }
    return ResolvedBangumiTitle(
      subjectName = subjectName,
      episodeNumber = epStr,
      episodeTitle = epTitle,
      displayTitle = display,
    )
  }
}

object BangumiLibraryStore {
  private const val MAX_VIDEO_ENTRIES = 1000
  private const val MAX_CACHED_SUBJECTS = 200
  private val lock = Any()

  private val genericFolderNames = setOf(
    "download",
    "downloads",
    "movies",
    "movie",
    "video",
    "videos",
    "dcim",
    "camera",
    "pictures",
    "telegram",
    "telegram video",
    "qqfile_recv",
    "weixin",
    "wechat",
    "0",
    "emulated",
    "storage",
    "root",
  )

  fun normalizeKey(raw: String?): String =
    raw.orEmpty().trim().replace('\\', '/').trimEnd('/').lowercase(Locale.ROOT)

  fun isGenericFolderName(folderName: String?): Boolean {
    val cleaned = folderName.orEmpty().trim().lowercase(Locale.ROOT)
    return cleaned.isEmpty() || cleaned in genericFolderNames
  }

  private fun storeDir(context: Context): File =
    File(context.filesDir, "porcelain").apply { mkdirs() }

  private fun mapFile(context: Context) = File(storeDir(context), "bangumi_map.json")
  private fun videoTitlesFile(context: Context) = File(storeDir(context), "bangumi_video_titles.json")
  private fun episodesCacheFile(context: Context) = File(storeDir(context), "bangumi_episodes_cache.json")

  fun loadSnapshot(context: Context, json: Json): BangumiLibrarySnapshot = synchronized(lock) {
    BangumiLibrarySnapshot(
      folderMap = loadFolderMapLocked(context, json),
      videoTitles = loadVideoTitlesLocked(context, json),
      episodesCache = loadEpisodesCacheLocked(context, json),
    )
  }

  fun loadFolderMap(context: Context, json: Json): MutableMap<String, BangumiFolderBinding> =
    synchronized(lock) { loadFolderMapLocked(context, json) }

  fun saveFolderBinding(
    context: Context,
    json: Json,
    folderKey: String,
    binding: BangumiFolderBinding,
  ) = synchronized(lock) {
    val key = normalizeKey(folderKey)
    if (key.isEmpty()) return@synchronized
    val map = loadFolderMapLocked(context, json)
    val existing = map[key]
    // Never let an automatic match overwrite a user's explicit manual folder binding
    if (!binding.isManual && existing?.isManual == true) return@synchronized
    map[key] = binding
    runCatching { mapFile(context).writeText(json.encodeToString(map)) }
  }

  fun saveVideoMeta(
    context: Context,
    json: Json,
    fileName: String,
    meta: BangumiVideoMeta,
  ) = synchronized(lock) {
    val key = normalizeKey(fileName)
    if (key.isEmpty()) return@synchronized
    val map = loadVideoTitlesLocked(context, json)
    map[key] = meta
    val trimmed = if (map.size > MAX_VIDEO_ENTRIES) {
      map.entries.sortedByDescending { it.value.updatedAt }.take(MAX_VIDEO_ENTRIES).associate { it.toPair() }
    } else {
      map
    }
    runCatching { videoTitlesFile(context).writeText(json.encodeToString(trimmed)) }
  }

  fun loadEpisodesCache(context: Context, json: Json): MutableMap<String, BangumiEpisodesCacheEntry> =
    synchronized(lock) { loadEpisodesCacheLocked(context, json) }

  fun saveEpisodesCache(
    context: Context,
    json: Json,
    cache: Map<String, BangumiEpisodesCacheEntry>,
  ) = synchronized(lock) {
    val trimmed = if (cache.size > MAX_CACHED_SUBJECTS) {
      cache.entries.sortedByDescending { it.value.updatedAtSec }.take(MAX_CACHED_SUBJECTS).associate { it.toPair() }
    } else {
      cache
    }
    runCatching { episodesCacheFile(context).writeText(json.encodeToString(trimmed)) }
  }

  private fun loadFolderMapLocked(context: Context, json: Json): MutableMap<String, BangumiFolderBinding> =
    runCatching {
      val f = mapFile(context)
      if (!f.exists()) return mutableMapOf()
      json.decodeFromString<Map<String, BangumiFolderBinding>>(f.readText()).toMutableMap()
    }.getOrElse { mutableMapOf() }

  private fun loadVideoTitlesLocked(context: Context, json: Json): MutableMap<String, BangumiVideoMeta> =
    runCatching {
      val f = videoTitlesFile(context)
      if (!f.exists()) return mutableMapOf()
      json.decodeFromString<Map<String, BangumiVideoMeta>>(f.readText()).toMutableMap()
    }.getOrElse { mutableMapOf() }

  private fun loadEpisodesCacheLocked(context: Context, json: Json): MutableMap<String, BangumiEpisodesCacheEntry> =
    runCatching {
      val f = episodesCacheFile(context)
      if (!f.exists()) return mutableMapOf()
      json.decodeFromString<Map<String, BangumiEpisodesCacheEntry>>(f.readText()).toMutableMap()
    }.getOrElse { mutableMapOf() }
}
