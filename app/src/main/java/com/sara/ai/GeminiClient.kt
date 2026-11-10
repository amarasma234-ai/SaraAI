package com.sara.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {
    class R(val text: String?, val err: String?)

    fun parseJson(s: String): JSONObject? {
        val a = s.indexOf('{'); val b = s.lastIndexOf('}')
        if (a < 0 || b <= a) return null
        return try { JSONObject(s.substring(a, b + 1)) } catch (e: Exception) { null }
    }

    private fun post(key: String, model: String, body: String): Pair<Int, String> {
        val c = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 15000
        c.readTimeout = 45000
        c.setRequestProperty("Content-Type", "application/json")
        c.setRequestProperty("x-goog-api-key", key)
        c.doOutput = true
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val s = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        return code to s
    }

    fun ask(ctx: Context, system: String, contents: List<Pair<String, String>>): R {
        val key = Prefs.get(ctx, "apiKey")
        if (key.isBlank()) return R(null, "nokey")
        val models = listOf(Prefs.get(ctx, "model", "gemini-3.8-flash"), "gemini-3.6-flash", "gemini-2.5-flash")
            .filter { it.isNotBlank() }.distinct()
        val arr = JSONArray()
        for ((role, t) in contents)
            arr.put(JSONObject().put("role", role).put("parts", JSONArray().put(JSONObject().put("text", t))))
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", arr)
            .put("generationConfig", JSONObject().put("temperature", 0.6).put("responseMimeType", "application/json"))
            .toString()
        var last = "unknown"
        for (m in models) {
            try {
                val (code, resp) = post(key, m, body)
                if (code in 200..299) {
                    val parts = JSONObject(resp).optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")
                    val sb = StringBuilder()
                    if (parts != null) for (i in 0 until parts.length()) {
                        val p = parts.getJSONObject(i)
                        if (!p.optBoolean("thought", false)) sb.append(p.optString("text", ""))
                    }
                    return if (sb.isBlank()) R(null, "empty") else R(sb.toString(), null)
                }
                last = "http$code"
                if (code == 429) return R(null, "rate")
                if (resp.contains("API key", true) || code == 401 || code == 403) return R(null, "badkey")
            } catch (e: IOException) { return R(null, "network") }
            catch (e: Exception) { last = "parse" }
        }
        return R(null, last)
    }
}
