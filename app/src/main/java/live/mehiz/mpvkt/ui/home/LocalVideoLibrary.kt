package live.mehiz.mpvkt.ui.home

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.mehiz.mpvkt.ui.player.PlayerActivity
import java.io.File
import java.util.Locale

data class LocalVideoItem(
  val id: Long,
  val uri: String,
  val displayName: String,
  val durationMs: Long,
  val sizeBytes: Long,
  val dateModifiedSec: Long,
  val folderId: String,
  val folderName: String,
  val relativePath: String,
  val filePath: String?,
)

data class LocalVideoFolder(
  val bucketId: String,
  val name: String,
  val relativePath: String,
  val videoCount: Int,
  val latestModifiedSec: Long,
  val totalDurationMs: Long,
  val videos: List<LocalVideoItem>,
)

object LocalVideoLibrary {
  const val EXTRA_PLAYLIST_URIS = "com.porcelain.mpv.extra.PLAYLIST_URIS"
  const val EXTRA_PLAYLIST_NAMES = "com.porcelain.mpv.extra.PLAYLIST_NAMES"
  const val EXTRA_PLAYLIST_PATHS = "com.porcelain.mpv.extra.PLAYLIST_PATHS"
  const val EXTRA_PLAYLIST_INDEX = "com.porcelain.mpv.extra.PLAYLIST_INDEX"
  const val EXTRA_FOLDER_KEY = "com.porcelain.mpv.extra.FOLDER_KEY"
  const val EXTRA_FOLDER_NAME = "com.porcelain.mpv.extra.FOLDER_NAME"

  suspend fun scanFolders(context: Context): List<LocalVideoFolder> = withContext(Dispatchers.IO) {
    val videos = queryAllVideos(context)
    videos
      .groupBy { it.folderId }
      .map { (bucketId, items) ->
        val sorted = items.sortedWith { a, b -> naturalCompare(a.displayName, b.displayName) }
        val first = sorted.first()
        LocalVideoFolder(
          bucketId = bucketId,
          name = first.folderName.ifBlank { "未命名文件夹" },
          relativePath = first.relativePath,
          videoCount = sorted.size,
          latestModifiedSec = sorted.maxOf { it.dateModifiedSec },
          totalDurationMs = sorted.sumOf { it.durationMs.coerceAtLeast(0L) },
          videos = sorted,
        )
      }
      .sortedByDescending { it.latestModifiedSec }
  }

  suspend fun queryFolderVideos(context: Context, bucketId: String): List<LocalVideoItem> =
    withContext(Dispatchers.IO) {
      queryAllVideos(context, bucketId)
        .sortedWith { a, b -> naturalCompare(a.displayName, b.displayName) }
    }

  private fun queryAllVideos(context: Context, onlyBucketId: String? = null): List<LocalVideoItem> {
    val result = mutableListOf<LocalVideoItem>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    val projection = buildList {
      add(MediaStore.Video.Media._ID)
      add(MediaStore.Video.Media.DISPLAY_NAME)
      add(MediaStore.Video.Media.DURATION)
      add(MediaStore.Video.Media.SIZE)
      add(MediaStore.Video.Media.DATE_MODIFIED)
      add(MediaStore.Video.Media.BUCKET_ID)
      add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(MediaStore.Video.Media.RELATIVE_PATH)
      }
      @Suppress("DEPRECATION")
      add(MediaStore.Video.Media.DATA)
    }.toTypedArray()

    val selection = onlyBucketId?.let { "${MediaStore.Video.Media.BUCKET_ID} = ?" }
    val selectionArgs = onlyBucketId?.let { arrayOf(it) }

    runCatching {
      context.contentResolver.query(
        collection,
        projection,
        selection,
        selectionArgs,
        "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
      )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
        val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
        } else {
          -1
        }
        @Suppress("DEPRECATION")
        val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idCol)
          val name = cursor.getString(nameCol) ?: continue
          if (name.startsWith(".")) continue
          val dur = cursor.getLong(durCol)
          val size = cursor.getLong(sizeCol)
          if (size <= 0L) continue
          val mod = cursor.getLong(modCol)
          val bId = cursor.getString(bucketIdCol) ?: "root"
          val bName = cursor.getString(bucketNameCol) ?: "本地视频"
          val rel = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty() else ""
          val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
          val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

          result.add(
            LocalVideoItem(
              id = id,
              uri = contentUri.toString(),
              displayName = name,
              durationMs = dur,
              sizeBytes = size,
              dateModifiedSec = mod,
              folderId = bId,
              folderName = bName,
              relativePath = rel.trimEnd('/'),
              filePath = dataPath,
            ),
          )
        }
      }
    }
    return result
  }

  fun launchPlaylist(context: Context, videos: List<LocalVideoItem>, startIndex: Int) {
    if (videos.isEmpty()) return
    val idx = startIndex.coerceIn(0, videos.lastIndex)
    val item = videos[idx]
    val intent = Intent(Intent.ACTION_VIEW, item.uri.toUri()).apply {
      setClass(context, PlayerActivity::class.java)
      putStringArrayListExtra(EXTRA_PLAYLIST_URIS, ArrayList(videos.map { it.uri }))
      putStringArrayListExtra(EXTRA_PLAYLIST_NAMES, ArrayList(videos.map { it.displayName }))
      putStringArrayListExtra(EXTRA_PLAYLIST_PATHS, ArrayList(videos.map { it.filePath.orEmpty() }))
      putExtra(EXTRA_PLAYLIST_INDEX, idx)
      putExtra(EXTRA_FOLDER_KEY, item.folderId)
      putExtra(EXTRA_FOLDER_NAME, item.folderName)
      // Auto-attach sibling external subtitles if accessible on filesystem
      val subs = findSiblingSubtitles(item.filePath)
      if (subs.isNotEmpty()) {
        val subUris = subs.map { Uri.fromFile(it) }.toTypedArray()
        putExtra("subs", subUris)
        putExtra("subs.enable", arrayOf(subUris.first()))
      }
    }
    context.startActivity(intent)
  }

  private fun findSiblingSubtitles(videoPath: String?): List<File> {
    if (videoPath.isNullOrBlank()) return emptyList()
    val videoFile = File(videoPath)
    val parent = videoFile.parentFile ?: return emptyList()
    if (!parent.exists() || !parent.canRead()) return emptyList()
    val base = videoFile.nameWithoutExtension
    val exts = setOf("ass", "ssa", "srt", "vtt", "sub")
    return parent.listFiles()
      ?.filter { f ->
        f.isFile &&
          f.extension.lowercase(Locale.ROOT) in exts &&
          f.nameWithoutExtension.startsWith(base, ignoreCase = true)
      }
      ?.sortedBy { it.name }
      .orEmpty()
  }

  fun naturalCompare(a: String, b: String): Int {
    var ia = 0
    var ib = 0
    val na = a.length
    val nb = b.length
    while (ia < na && ib < nb) {
      val ca = a[ia]
      val cb = b[ib]
      if (ca.isDigit() && cb.isDigit()) {
        var ja = ia
        while (ja < na && a[ja].isDigit()) ja++
        var jb = ib
        while (jb < nb && b[jb].isDigit()) jb++
        val sa = a.substring(ia, ja).trimStart('0')
        val sb = b.substring(ib, jb).trimStart('0')
        if (sa.length != sb.length) return sa.length.compareTo(sb.length)
        val cmp = sa.compareTo(sb)
        if (cmp != 0) return cmp
        ia = ja
        ib = jb
      } else {
        val cmp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
        if (cmp != 0) return cmp
        ia++
        ib++
      }
    }
    return (na - ia).compareTo(nb - ib)
  }

  fun formatDuration(durationMs: Long): String {
    val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
      String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
      String.format(Locale.US, "%02d:%02d", m, s)
    }
  }

  fun formatSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
      bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
      bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
      bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
      else -> "$bytes B"
    }
  }
}

data class ParsedAnimeTitle(
  val animeName: String,
  val episodeNumber: String?,
  val displayTitle: String,
)

object SmartTitleParser {
  private val leadingTagRegex = Regex("""^(\[[^\]]*]|\([^)]*\)|【[^】]*】)\s*""")
  private val inlineTagRegex = Regex("""(\[[^\]]*]|\([^)]*\)|【[^】]*】)""")
  private val techSuffixRegex = Regex(
    """\b(1080p|2160p|720p|4K|WEB-?DL|WEBRip|BDRip|BluRay|HEVC|H\.?264|H\.?265|x264|x265|AAC[\d.]*|FLAC|CHT|CHS|GB|BIG5|10bit|8bit)\b.*""",
    RegexOption.IGNORE_CASE,
  )
  private val patterns = listOf(
    Regex("""^(.+?)[._\s-]+S\d{1,2}[._\s-]*E(\d{1,3}(?:\.5)?)(?=[^0-9]|$)""", RegexOption.IGNORE_CASE),
    Regex("""^(.+?)[._\s-]+第\s*\d{1,2}\s*[季部][._\s-]+第?\s*(\d{1,3}(?:\.5)?)\s*[话集回]?"""),
    Regex("""^(.+?)[._\s-]+(?:EP?\.?\s*)(\d{1,3}(?:\.5)?)(?=[^0-9]|$)""", RegexOption.IGNORE_CASE),
    Regex("""^(.+?)[._\s-]*第\s*(\d{1,3}(?:\.5)?)\s*[话集回]"""),
    Regex("""^(.+?)\s*\[(\d{1,3}(?:\.5)?)(?:v\d+)?]""", RegexOption.IGNORE_CASE),
    Regex("""^(.+?)\s*-\s*(\d{1,3}(?:\.5)?)(?:\s|$)"""),
  )

  private fun cleanTitle(raw: String): String {
    var s = raw.trim()
    while (true) {
      val next = leadingTagRegex.replace(s, "").trim()
      if (next == s) break
      s = next
    }
    s = inlineTagRegex.replace(s, " ")
    s = techSuffixRegex.replace(s, "")
    s = s.replace(Regex("[._]+"), " ")
    return s.replace(Regex("\\s+"), " ").trim(' ', '-', '_')
  }

  fun parse(fileName: String): ParsedAnimeTitle {
    val base = fileName.substringBeforeLast('.').ifBlank { fileName }
    for (pat in patterns) {
      val m = pat.find(base)
      if (m != null) {
        val rawTitle = cleanTitle(m.groupValues[1])
        val epRaw = m.groupValues[2].trimStart('0').ifEmpty { "0" }
        val title = rawTitle.ifBlank { cleanTitle(base).ifBlank { base } }
        return ParsedAnimeTitle(
          animeName = title,
          episodeNumber = epRaw,
          displayTitle = "$title · 第${epRaw}话",
        )
      }
    }
    val cleaned = cleanTitle(base).ifBlank { base }
    return ParsedAnimeTitle(animeName = cleaned, episodeNumber = null, displayTitle = cleaned)
  }

  fun extractSeason(fileName: String): Int? {
    val m1 = Regex("""[._\s-]S(\d{1,2})[._\s-]*E\d""", RegexOption.IGNORE_CASE).find(fileName)
    if (m1 != null) return m1.groupValues[1].toIntOrNull()
    val m2 = Regex("""第\s*(\d{1,2})\s*[季部期]""").find(fileName)
    if (m2 != null) return m2.groupValues[1].toIntOrNull()
    return null
  }
}
