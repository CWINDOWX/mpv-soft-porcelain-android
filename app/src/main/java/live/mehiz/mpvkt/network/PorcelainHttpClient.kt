package live.mehiz.mpvkt.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.zip.GZIPInputStream

data class HttpResponse(
  val code: Int,
  val body: String,
  val error: String? = null,
) {
  val isSuccessful: Boolean get() = code in 200..299 && error == null
}

object PorcelainHttpClient {
  private const val CONNECT_TIMEOUT_MS = 12_000
  private const val READ_TIMEOUT_MS = 20_000
  private const val MAX_BODY_BYTES = 16 * 1024 * 1024 // 16 MB safety cap

  suspend fun request(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    jsonBody: String? = null,
    retries: Int = if (method.equals("GET", ignoreCase = true)) 1 else 0,
  ): HttpResponse = withContext(Dispatchers.IO) {
    var attempt = 0
    var lastResult: HttpResponse? = null
    while (attempt <= retries) {
      val result = executeOnce(method, url, headers, jsonBody)
      lastResult = result
      val shouldRetry = result.code == 0 || result.code == 502 || result.code == 503 || result.code == 504
      if (!shouldRetry || attempt >= retries) return@withContext result
      attempt++
      delay(650L * attempt)
    }
    lastResult ?: HttpResponse(0, "", "网络请求失败")
  }

  private fun executeOnce(
    method: String,
    urlStr: String,
    headers: Map<String, String>,
    jsonBody: String?,
  ): HttpResponse {
    var conn: HttpURLConnection? = null
    return try {
      val url = URL(urlStr)
      conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = method.uppercase()
        instanceFollowRedirects = true
        connectTimeout = CONNECT_TIMEOUT_MS
        readTimeout = READ_TIMEOUT_MS
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Accept-Encoding", "gzip")
        headers.forEach { (k, v) -> setRequestProperty(k, v) }
        if (jsonBody != null) {
          doOutput = true
          setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
      }

      if (jsonBody != null) {
        val bytes = jsonBody.toByteArray(Charsets.UTF_8)
        conn.setFixedLengthStreamingMode(bytes.size)
        conn.outputStream.use { it.write(bytes) }
      }

      val statusCode = conn.responseCode
      val rawStream = if (statusCode in 200..399) conn.inputStream else (conn.errorStream ?: conn.inputStream)
      val body = if (rawStream != null) {
        val isGzip = conn.contentEncoding.equals("gzip", ignoreCase = true)
        val decoded = if (isGzip) GZIPInputStream(BufferedInputStream(rawStream)) else BufferedInputStream(rawStream)
        decoded.use { input ->
          val out = ByteArrayOutputStream()
          val buf = ByteArray(16 * 1024)
          var total = 0
          while (true) {
            val read = input.read(buf)
            if (read <= 0) break
            total += read
            if (total > MAX_BODY_BYTES) break
            out.write(buf, 0, read)
          }
          out.toString(Charsets.UTF_8.name())
        }
      } else {
        ""
      }

      HttpResponse(code = statusCode, body = body)
    } catch (_: SocketTimeoutException) {
      HttpResponse(code = 0, body = "", error = "网络请求超时，请检查网络或代理设置")
    } catch (_: UnknownHostException) {
      HttpResponse(code = 0, body = "", error = "无法解析服务器地址，请检查网络连接")
    } catch (e: IOException) {
      HttpResponse(code = 0, body = "", error = "网络连接失败：${e.message ?: "IO 错误"}")
    } catch (e: Exception) {
      HttpResponse(code = 0, body = "", error = e.message ?: "请求异常")
    } finally {
      conn?.disconnect()
    }
  }
}
