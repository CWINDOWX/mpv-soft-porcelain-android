package live.mehiz.mpvkt.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import live.mehiz.mpvkt.BuildConfig
import live.mehiz.mpvkt.network.PorcelainHttpClient

data class GitHubReleaseInfo(
  val tagName: String,
  val name: String,
  val body: String,
  val htmlUrl: String,
  val publishedAt: String,
  val apkDownloadUrl: String?,
  val apkSizeBytes: Long?,
)

sealed interface UpdateCheckResult {
  data class UpdateAvailable(val release: GitHubReleaseInfo) : UpdateCheckResult
  data class UpToDate(val latestTag: String?) : UpdateCheckResult
  data class Error(val message: String) : UpdateCheckResult
}

object GitHubUpdateChecker {
  const val OWNER = "CWINDOWX"
  const val REPO = "mpv-soft-porcelain-android"
  const val REPO_HTML_URL = "https://github.com/$OWNER/$REPO"

  private val apiHeaders: Map<String, String>
    get() = mapOf(
      "Accept" to "application/vnd.github+json",
      "X-GitHub-Api-Version" to "2022-11-28",
      "User-Agent" to "PorcelainMPV-Android/${BuildConfig.VERSION_NAME}",
    )

  suspend fun checkLatestRelease(
    json: Json,
    currentVersion: String = BuildConfig.VERSION_NAME,
  ): UpdateCheckResult {
    // Try formal latest release first; fall back to most recent release (including pre-releases) if 404
    val latestResp = PorcelainHttpClient.request(
      method = "GET",
      url = "https://api.github.com/repos/$OWNER/$REPO/releases/latest",
      headers = apiHeaders,
      retries = 1,
    )

    val releaseObj: JsonObject? = when {
      latestResp.isSuccessful -> runCatching {
        json.parseToJsonElement(latestResp.body).jsonObject
      }.getOrNull()

      latestResp.code == 404 -> {
        val listResp = PorcelainHttpClient.request(
          method = "GET",
          url = "https://api.github.com/repos/$OWNER/$REPO/releases?per_page=1",
          headers = apiHeaders,
          retries = 1,
        )
        when {
          listResp.code == 404 -> return UpdateCheckResult.UpToDate(latestTag = null)
          !listResp.isSuccessful -> {
            return UpdateCheckResult.Error(listResp.error ?: "GitHub 请求失败 (HTTP ${listResp.code})")
          }
          else -> {
            val arr = runCatching { json.parseToJsonElement(listResp.body).jsonArray }.getOrNull()
            if (arr.isNullOrEmpty()) return UpdateCheckResult.UpToDate(latestTag = null)
            arr[0] as? JsonObject
          }
        }
      }

      latestResp.code == 403 -> return UpdateCheckResult.Error("GitHub API 请求频率受限，请稍后再试")

      else -> return UpdateCheckResult.Error(latestResp.error ?: "检查更新失败 (HTTP ${latestResp.code})")
    }

    if (releaseObj == null) return UpdateCheckResult.UpToDate(latestTag = null)
    val info = parseRelease(releaseObj) ?: return UpdateCheckResult.Error("版本信息解析失败")

    return if (isNewerVersion(remoteTag = info.tagName, currentVersion = currentVersion)) {
      UpdateCheckResult.UpdateAvailable(info)
    } else {
      UpdateCheckResult.UpToDate(latestTag = info.tagName)
    }
  }

  private fun parseRelease(obj: JsonObject): GitHubReleaseInfo? {
    val tag = obj["tag_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    if (tag.isEmpty()) return null
    val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().ifBlank { tag }
    val body = obj["body"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    val htmlUrl = obj["html_url"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
      .ifBlank { "$REPO_HTML_URL/releases/tag/$tag" }
    val publishedAt = obj["published_at"]?.jsonPrimitive?.contentOrNull?.substringBefore('T').orEmpty()

    val assets = obj["assets"] as? JsonArray
    var bestUrl: String? = null
    var bestSize: Long? = null
    if (assets != null) {
      val apkAssets = assets.mapNotNull { it as? JsonObject }.filter { asset ->
        asset["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true
      }
      val preferred = apkAssets.firstOrNull { asset ->
        asset["name"]?.jsonPrimitive?.contentOrNull?.contains("arm64", ignoreCase = true) == true
      } ?: apkAssets.firstOrNull()
      bestUrl = preferred?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
      bestSize = preferred?.get("size")?.jsonPrimitive?.longOrNull
    }

    return GitHubReleaseInfo(
      tagName = tag,
      name = name,
      body = body,
      htmlUrl = htmlUrl,
      publishedAt = publishedAt,
      apkDownloadUrl = bestUrl,
      apkSizeBytes = bestSize,
    )
  }

  internal fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
    val remote = parseSemVer(remoteTag)
    val current = parseSemVer(currentVersion)
    if (remote != null && current != null) {
      val maxLen = maxOf(remote.numbers.size, current.numbers.size)
      for (i in 0 until maxLen) {
        val r = remote.numbers.getOrElse(i) { 0 }
        val c = current.numbers.getOrElse(i) { 0 }
        if (r != c) return r > c
      }
      // Same core numeric version: stable (no suffix) > prerelease (with suffix)
      return when {
        remote.suffix.isEmpty() && current.suffix.isNotEmpty() -> true
        remote.suffix.isNotEmpty() && current.suffix.isEmpty() -> false
        else -> compareSuffix(remote.suffix, current.suffix) > 0
      }
    }
    val normRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
    val normCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
    return normRemote.isNotEmpty() && normRemote != normCurrent
  }

  private data class ParsedVer(val numbers: List<Int>, val suffix: String)

  private fun parseSemVer(raw: String): ParsedVer? {
    val cleaned = raw.trim().removePrefix("v").removePrefix("V")
    val m = Regex("""^(\d+(?:\.\d+)*)(?:[-._](.+))?$""").find(cleaned) ?: return null
    val nums = m.groupValues[1].split('.').mapNotNull { it.toIntOrNull() }
    if (nums.isEmpty()) return null
    return ParsedVer(nums, m.groupValues[2].lowercase())
  }

  private fun compareSuffix(a: String, b: String): Int {
    if (a == b) return 0
    val numA = Regex("""(\d+)$""").find(a)?.groupValues?.get(1)?.toIntOrNull()
    val numB = Regex("""(\d+)$""").find(b)?.groupValues?.get(1)?.toIntOrNull()
    val prefixA = a.replace(Regex("""\d+$"""), "")
    val prefixB = b.replace(Regex("""\d+$"""), "")
    if (prefixA == prefixB && numA != null && numB != null) {
      return numA.compareTo(numB)
    }
    return a.compareTo(b)
  }
}
