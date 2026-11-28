package live.mehiz.mpvkt.danmaku

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import live.mehiz.mpvkt.network.PorcelainHttpClient
import java.io.File
import java.io.FileInputStream
import java.net.URLEncoder
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

data class DandanMatchResult(
  val episodeId: Long,
  val animeTitle: String,
  val episodeTitle: String,
)

class DandanplayApi(
  private val json: Json,
  private val serverProvider: () -> String,
) {
  companion object {
    private const val HASH_SAMPLE_BYTES = 16 * 1024 * 1024
    private const val CHUNK_BYTES = 64 * 1024
    private const val USER_AGENT = "PorcelainMPV-Android/0.2 (mpv-danmaku)"
  }

  private val baseUrl: String
    get() = serverProvider().trim().trimEnd('/').ifBlank { "https://danmaku-api.152468.xyz" }

  private val defaultHeaders: Map<String, String>
    get() = mapOf("User-Agent" to USER_AGENT)

  /**
   * Computes Dandanplay's standard MD5 hash of the first 16 MiB in 64 KiB chunks.
   * Returns null if file is unreadable or shorter than 16 MiB.
   */
  suspend fun computeVideoHash(context: Context, uri: Uri?, filePath: String?): String? = withContext(Dispatchers.IO) {
    runCatching {
      val input = when {
        !filePath.isNullOrBlank() && File(filePath).let { it.exists() && it.canRead() } ->
          FileInputStream(filePath)
        uri != null -> context.contentResolver.openInputStream(uri)
        else -> null
      } ?: return@withContext null

      input.use { stream ->
        val md = MessageDigest.getInstance("MD5")
        val buf = ByteArray(CHUNK_BYTES)
        var remaining = HASH_SAMPLE_BYTES
        while (remaining > 0) {
          val toRead = min(buf.size, remaining)
          val n = stream.read(buf, 0, toRead)
          if (n <= 0) return@withContext null // Shorter than 16 MiB -> fall back to filename search
          md.update(buf, 0, n)
          remaining -= n
        }
        md.digest().joinToString("") { "%02x".format(it) }
      }
    }.getOrNull()
  }

  suspend fun matchByHash(fileName: String, fileHash: String): Result<DandanMatchResult?> {
    val body = buildJsonObject {
      put("fileName", fileName)
      put("fileHash", fileHash)
      put("matchMode", "hashAndFileName")
    }.toString()

    val resp = PorcelainHttpClient.request(
      method = "POST",
      url = "$baseUrl/api/v2/match",
      headers = defaultHeaders,
      jsonBody = body,
      retries = 1,
    )
    if (!resp.isSuccessful) {
      return Result.failure(IllegalStateException(resp.error ?: "匹配请求失败 (HTTP ${resp.code})"))
    }
    val root = runCatching { json.parseToJsonElement(resp.body).jsonObject }.getOrNull()
      ?: return Result.success(null)
    val isMatched = root["isMatched"]?.jsonPrimitive?.booleanOrNull == true
    val matches = root["matches"] as? JsonArray
    if (!isMatched || matches.isNullOrEmpty()) return Result.success(null)
    val first = matches[0] as? JsonObject ?: return Result.success(null)
    val epId = first["episodeId"]?.jsonPrimitive?.longOrNull ?: return Result.success(null)
    val animeTitle = first["animeTitle"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val episodeTitle = first["episodeTitle"]?.jsonPrimitive?.contentOrNull.orEmpty()
    return Result.success(DandanMatchResult(epId, animeTitle, episodeTitle))
  }

  suspend fun searchAnime(keyword: String): Result<List<DanmakuSearchAnime>> {
    val trimmed = keyword.trim()
    if (trimmed.isEmpty()) return Result.success(emptyList())
    val encoded = URLEncoder.encode(trimmed, "UTF-8")
    val resp = PorcelainHttpClient.request(
      method = "GET",
      url = "$baseUrl/api/v2/search/anime?keyword=$encoded",
      headers = defaultHeaders,
      retries = 1,
    )
    if (!resp.isSuccessful) {
      return Result.failure(IllegalStateException(resp.error ?: "搜索失败 (HTTP ${resp.code})"))
    }
    val root = runCatching { json.parseToJsonElement(resp.body).jsonObject }.getOrNull()
      ?: return Result.success(emptyList())
    val arr = root["animes"] as? JsonArray ?: return Result.success(emptyList())
    val out = ArrayList<DanmakuSearchAnime>(arr.size)
    for (el in arr) {
      val obj = el as? JsonObject ?: continue
      val bangumiId = obj["bangumiId"]?.jsonPrimitive?.contentOrNull
        ?: obj["animeId"]?.jsonPrimitive?.contentOrNull
        ?: continue
      val title = obj["animeTitle"]?.jsonPrimitive?.contentOrNull.orEmpty()
      if (title.isBlank()) continue
      out.add(
        DanmakuSearchAnime(
          bangumiId = bangumiId,
          animeTitle = title,
          type = obj["type"]?.jsonPrimitive?.contentOrNull.orEmpty(),
          typeDescription = obj["typeDescription"]?.jsonPrimitive?.contentOrNull.orEmpty(),
          episodeCount = obj["episodeCount"]?.jsonPrimitive?.intOrNull ?: 0,
        ),
      )
    }
    return Result.success(out)
  }

  suspend fun getEpisodes(bangumiId: String): Result<List<DanmakuSearchEpisode>> {
    val resp = PorcelainHttpClient.request(
      method = "GET",
      url = "$baseUrl/api/v2/bangumi/$bangumiId",
      headers = defaultHeaders,
      retries = 1,
    )
    if (!resp.isSuccessful) {
      return Result.failure(IllegalStateException(resp.error ?: "获取剧集失败 (HTTP ${resp.code})"))
    }
    val root = runCatching { json.parseToJsonElement(resp.body).jsonObject }.getOrNull()
      ?: return Result.success(emptyList())
    val bangumi = root["bangumi"] as? JsonObject ?: return Result.success(emptyList())
    val eps = bangumi["episodes"] as? JsonArray ?: return Result.success(emptyList())
    val out = ArrayList<DanmakuSearchEpisode>(eps.size)
    for (el in eps) {
      val obj = el as? JsonObject ?: continue
      val epId = obj["episodeId"]?.jsonPrimitive?.longOrNull ?: continue
      val epNum = obj["episodeNumber"]?.jsonPrimitive?.contentOrNull.orEmpty()
      val epTitle = obj["episodeTitle"]?.jsonPrimitive?.contentOrNull.orEmpty()
      out.add(DanmakuSearchEpisode(epId, epNum, epTitle))
    }
    return Result.success(out)
  }

  suspend fun fetchComments(episodeId: Long, chConvert: Int): Result<List<DanmakuItem>> {
    val safeConvert = chConvert.coerceIn(0, 2)
    val resp = PorcelainHttpClient.request(
      method = "GET",
      url = "$baseUrl/api/v2/comment/$episodeId?withRelated=true&chConvert=$safeConvert",
      headers = defaultHeaders,
      retries = 1,
    )
    if (!resp.isSuccessful) {
      return Result.failure(IllegalStateException(resp.error ?: "拉取弹幕失败 (HTTP ${resp.code})"))
    }
    return withContext(Dispatchers.Default) {
      DanmakuXmlParser.parseDandanplayCommentsResult(resp.body, json)
    }
  }

  suspend fun matchByTitle(title: String, season: Int?, episode: Double): Result<DandanMatchResult?> {
    val animes = searchAnime(title).getOrElse { return Result.failure(it) }
    if (animes.isEmpty()) return Result.success(null)

    val wantOva = title.contains("OVA", ignoreCase = true) || title.contains("OAD", ignoreCase = true)
    val preferredType = if (wantOva) "ova" else "tvseries"
    val typed = animes.filter { it.type.equals(preferredType, ignoreCase = true) }.ifEmpty { animes }

    val picked = if (typed.size == 1) {
      typed.first()
    } else {
      val effSeason = season ?: 1
      var target = title.trim()
      if (effSeason > 1 && !Regex("第.+[季部期]").containsMatchIn(target)) {
        target = "$target 第${toChineseNum(effSeason)}季"
      }
      typed.maxByOrNull { anime ->
        val cleanCandidate = anime.animeTitle.trim()
          .replace(Regex("\\s*\\([^)]*\\)\\s*$"), "")
          .replace(Regex("\\s*【[^】]*】.*$"), "")
        val compareTarget = if (effSeason == 1 && Regex("第一[季部]").containsMatchIn(cleanCandidate)) {
          "$title 第一季"
        } else {
          target
        }
        jaroWinkler(compareTarget.lowercase(), cleanCandidate.lowercase())
      } ?: typed.first()
    }

    val episodes = getEpisodes(picked.bangumiId).getOrElse { return Result.failure(it) }
    val matchedEp = episodes.firstOrNull { ep ->
      val num = ep.episodeNumber.toDoubleOrNull()
      num != null && kotlin.math.abs(num - episode) < 1e-4
    } ?: return Result.success(null)

    return Result.success(
      DandanMatchResult(
        episodeId = matchedEp.episodeId,
        animeTitle = picked.animeTitle,
        episodeTitle = matchedEp.episodeTitle,
      ),
    )
  }

  private fun toChineseNum(n: Int): String = when (n) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "七"
    8 -> "八"
    9 -> "九"
    else -> n.toString()
  }

  private fun jaroWinkler(s1: String, s2: String): Double {
    if (s1 == s2) return 1.0
    if (s1.isEmpty() || s2.isEmpty()) return 0.0
    val matchDist = max(s1.length, s2.length) / 2 - 1
    val s1Matches = BooleanArray(s1.length)
    val s2Matches = BooleanArray(s2.length)
    var matches = 0
    for (i in s1.indices) {
      val start = max(0, i - matchDist)
      val end = min(i + matchDist + 1, s2.length)
      for (j in start until end) {
        if (s2Matches[j] || s1[i] != s2[j]) continue
        s1Matches[i] = true
        s2Matches[j] = true
        matches++
        break
      }
    }
    if (matches == 0) return 0.0
    var transpositions = 0
    var k = 0
    for (i in s1.indices) {
      if (!s1Matches[i]) continue
      while (!s2Matches[k]) k++
      if (s1[i] != s2[k]) transpositions++
      k++
    }
    val m = matches.toDouble()
    val jaro = (m / s1.length + m / s2.length + (m - transpositions / 2.0) / m) / 3.0
    var prefix = 0
    for (i in 0 until min(4, min(s1.length, s2.length))) {
      if (s1[i] == s2[i]) prefix++ else break
    }
    return jaro + prefix * 0.1 * (1.0 - jaro)
  }
}
