package live.mehiz.mpvkt.bangumi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import live.mehiz.mpvkt.network.PorcelainHttpClient
import java.net.URLEncoder
import kotlin.math.abs

@Serializable
data class BangumiUser(
  val username: String,
  val nickname: String,
)

@Serializable
data class BangumiSubject(
  val id: Int,
  val name: String,
  val nameCn: String,
  val date: String = "",
  val eps: Int = 0,
) {
  val displayName: String get() = nameCn.ifBlank { name }
}

@Serializable
data class BangumiEpisode(
  val id: Int,
  val ep: Double? = null,
  val sort: Double? = null,
  val nameCn: String = "",
  val name: String = "",
) {
  val displayTitle: String get() = nameCn.ifBlank { name }.trim()
}

@Serializable
data class BangumiFolderBinding(
  val subjectId: Int,
  val subjectName: String,
  val updatedAt: Long = 0L,
)

@Serializable
data class BangumiEpisodesCacheEntry(
  val updatedAtSec: Long,
  val episodes: List<BangumiEpisode>,
)

class BangumiApi(private val json: Json) {
  companion object {
    private const val API_BASE = "https://api.bgm.tv"
    private const val USER_AGENT = "PorcelainMPV-Android/0.2 (https://github.com/bangumi/api)"
    const val ACCESS_TOKEN_URL = "https://next.bgm.tv/demo/access-token"
  }

  private fun headers(token: String?): Map<String, String> = buildMap {
    put("User-Agent", USER_AGENT)
    if (!token.isNullOrBlank()) put("Authorization", "Bearer ${token.trim()}")
  }

  private fun extractError(code: Int, body: String, fallback: String?): String {
    if (code == 0) return fallback ?: "网络请求失败，请检查网络或代理"
    if (code == 401) return "Token 无效或已过期，请重新生成"
    val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
    val msg = parsed?.get("description")?.jsonPrimitive?.contentOrNull
      ?: parsed?.get("message")?.jsonPrimitive?.contentOrNull
    return msg?.takeIf { it.isNotBlank() } ?: "HTTP $code"
  }

  suspend fun verifyToken(token: String): Result<BangumiUser> {
    val clean = token.trim()
    if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Access Token 不能为空"))
    val resp = PorcelainHttpClient.request(
      method = "GET",
      url = "$API_BASE/v0/me",
      headers = headers(clean),
      retries = 1,
    )
    if (!resp.isSuccessful) {
      return Result.failure(IllegalStateException(extractError(resp.code, resp.body, resp.error)))
    }
    val obj = runCatching { json.parseToJsonElement(resp.body).jsonObject }.getOrNull()
      ?: return Result.failure(IllegalStateException("返回数据格式异常"))
    val username = obj["username"]?.jsonPrimitive?.contentOrNull.orEmpty()
    if (username.isBlank()) return Result.failure(IllegalStateException("未能识别用户名"))
    val nickname = obj["nickname"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { username }
    return Result.success(BangumiUser(username = username, nickname = nickname))
  }

  suspend fun searchSubjects(keyword: String, token: String? = null): Result<List<BangumiSubject>> {
    val q = keyword.trim()
    if (q.isEmpty()) return Result.success(emptyList())
    val body = buildJsonObject {
      put("keyword", q)
      put(
        "filter",
        buildJsonObject {
          put("type", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(2)) })
        },
      )
    }.toString()

    var resp = PorcelainHttpClient.request(
      method = "POST",
      url = "$API_BASE/v0/search/subjects?limit=12",
      headers = headers(token),
      jsonBody = body,
      retries = 1,
    )
    if (resp.code == 401 && !token.isNullOrBlank()) {
      resp = PorcelainHttpClient.request(
        method = "POST",
        url = "$API_BASE/v0/search/subjects?limit=12",
        headers = headers(null),
        jsonBody = body,
        retries = 1,
      )
    }
    if (!resp.isSuccessful) {
      return Result.failure(IllegalStateException(extractError(resp.code, resp.body, resp.error)))
    }
    val root = runCatching { json.parseToJsonElement(resp.body).jsonObject }.getOrNull()
      ?: return Result.success(emptyList())
    val data = root["data"] as? JsonArray ?: return Result.success(emptyList())
    val out = ArrayList<BangumiSubject>(data.size)
    for (el in data) {
      val obj = el as? JsonObject ?: continue
      val id = obj["id"]?.jsonPrimitive?.intOrNull ?: continue
      val name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
      val nameCn = obj["name_cn"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { name }
      val date = obj["date"]?.jsonPrimitive?.contentOrNull.orEmpty()
      val eps = obj["eps"]?.jsonPrimitive?.intOrNull ?: obj["total_episodes"]?.jsonPrimitive?.intOrNull ?: 0
      out.add(BangumiSubject(id = id, name = name, nameCn = nameCn, date = date, eps = eps))
    }
    return Result.success(out)
  }

  suspend fun fetchEpisodes(subjectId: Int, token: String? = null): Result<List<BangumiEpisode>> {
    val all = ArrayList<BangumiEpisode>()
    var offset = 0
    var effToken = token
    for (page in 0 until 20) {
      var resp = PorcelainHttpClient.request(
        method = "GET",
        url = "$API_BASE/v0/episodes?subject_id=$subjectId&type=0&limit=100&offset=$offset",
        headers = headers(effToken),
        retries = 1,
      )
      if (resp.code == 401 && !effToken.isNullOrBlank()) {
        effToken = null
        resp = PorcelainHttpClient.request(
          method = "GET",
          url = "$API_BASE/v0/episodes?subject_id=$subjectId&type=0&limit=100&offset=$offset",
          headers = headers(null),
          retries = 1,
        )
      }
      if (!resp.isSuccessful) {
        return Result.failure(IllegalStateException(extractError(resp.code, resp.body, resp.error)))
      }
      val root = runCatching { json.parseToJsonElement(resp.body).jsonObject }.getOrNull()
        ?: break
      val batch = root["data"] as? JsonArray ?: break
      for (el in batch) {
        val obj = el as? JsonObject ?: continue
        val id = obj["id"]?.jsonPrimitive?.intOrNull ?: continue
        all.add(
          BangumiEpisode(
            id = id,
            ep = obj["ep"]?.jsonPrimitive?.doubleOrNull,
            sort = obj["sort"]?.jsonPrimitive?.doubleOrNull,
            nameCn = obj["name_cn"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(),
            name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(),
          ),
        )
      }
      offset += batch.size
      val total = root["total"]?.jsonPrimitive?.intOrNull
      if (batch.isEmpty() || (total != null && offset >= total)) break
    }
    return Result.success(all)
  }

  suspend fun markEpisodeWatched(
    username: String,
    token: String,
    subjectId: Int,
    episodeId: Int,
  ): Result<Unit> {
    val encodedUser = URLEncoder.encode(username, "UTF-8")
    val colCheck = PorcelainHttpClient.request(
      method = "GET",
      url = "$API_BASE/v0/users/$encodedUser/collections/$subjectId",
      headers = headers(token),
      retries = 1,
    )
    if (colCheck.code == 404) {
      PorcelainHttpClient.request(
        method = "POST",
        url = "$API_BASE/v0/users/-/collections/$subjectId",
        headers = headers(token),
        jsonBody = """{"type":3}""",
        retries = 0,
      )
    }

    val markResp = PorcelainHttpClient.request(
      method = "PUT",
      url = "$API_BASE/v0/users/-/collections/-/episodes/$episodeId",
      headers = headers(token),
      jsonBody = """{"type":2}""",
      retries = 0,
    )
    return if (markResp.isSuccessful) {
      Result.success(Unit)
    } else {
      Result.failure(IllegalStateException(extractError(markResp.code, markResp.body, markResp.error)))
    }
  }

  fun pickBestSubject(items: List<BangumiSubject>, season: Int?): BangumiSubject {
    val cnNums = mapOf(2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六")
    val sequelRe = Regex(
      """(第\s*[2-9二三四五六七八九]\s*[期季部]|[2-9]\s*期|Season\s*[2-9]|2nd|3rd|4th|\bII\b|\bIII\b)""",
      RegexOption.IGNORE_CASE,
    )
    val spinoffRe = Regex(
      """(～|迷你|小剧场|OVA|OAD|剧场版|总集篇|特典|PV|CM|番外)""",
      RegexOption.IGNORE_CASE,
    )

    return items.withIndex().maxByOrNull { (idx, item) ->
      val text = "${item.nameCn} ${item.name}"
      var score = 100 - idx * 3
      if (item.eps >= 8) score += 12 else if (item.eps == 0) score -= 15
      if (spinoffRe.containsMatchIn(text)) score -= 35

      if (season != null && season >= 2) {
        val cn = cnNums[season] ?: season.toString()
        val pat = Regex("""(第\s*($season|$cn)\s*[期季部]|$season\s*期|Season\s*$season)""", RegexOption.IGNORE_CASE)
        if (pat.containsMatchIn(text)) {
          score += 55
        } else if (!sequelRe.containsMatchIn(text)) {
          score -= 20
        }
      } else {
        if (sequelRe.containsMatchIn(text)) score -= 45 else score += 20
      }
      score
    }?.value ?: items.first()
  }

  fun findEpisodeInList(episodes: List<BangumiEpisode>, targetEp: Double): BangumiEpisode? {
    var sortMatch: BangumiEpisode? = null
    for (ep in episodes) {
      if (ep.ep != null && abs(ep.ep - targetEp) < 1e-4) return ep
      if (sortMatch == null && ep.sort != null && abs(ep.sort - targetEp) < 1e-4) sortMatch = ep
    }
    return sortMatch
  }
}
