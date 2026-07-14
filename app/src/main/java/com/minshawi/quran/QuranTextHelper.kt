package com.minshawi.quran

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class Ayah(val number: Int, val text: String)

object QuranTextHelper {

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun textDir(context: Context): File {
        val dir = File(context.filesDir, "text")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun localFile(context: Context, surah: Surah): File =
        File(textDir(context), surah.textFileName)

    fun isCached(context: Context, surah: Surah): Boolean =
        localFile(context, surah).exists()

    /** يقرأ الآيات من الملف المحلي (يجب التأكد من isCached أولاً أو استخدام fetchAyahs) */
    fun readCached(context: Context, surah: Surah): List<Ayah> {
        val file = localFile(context, surah)
        if (!file.exists()) return emptyList()
        return parseJson(file.readText())
    }

    private fun parseJson(json: String): List<Ayah> {
        val root = JSONObject(json)
        val data = root.getJSONObject("data")
        val ayahsArray = data.getJSONArray("ayahs")
        val list = mutableListOf<Ayah>()
        for (i in 0 until ayahsArray.length()) {
            val a = ayahsArray.getJSONObject(i)
            list.add(Ayah(a.getInt("numberInSurah"), a.getString("text")))
        }
        return list
    }

    /** يجلب الآيات: من الذاكرة المحلية إن وُجدت، وإلا من الإنترنت (ويخزّنها محليًا لأول مرة) */
    fun fetchAyahs(context: Context, surah: Surah, callback: (List<Ayah>?, String?) -> Unit) {
        if (isCached(context, surah)) {
            executor.execute {
                val ayahs = readCached(context, surah)
                mainHandler.post { callback(ayahs, null) }
            }
            return
        }

        executor.execute {
            var errorMsg: String? = null
            var result: List<Ayah>? = null
            var connection: HttpURLConnection? = null
            try {
                val url = URL(surah.textUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    localFile(context, surah).writeText(json)
                    result = parseJson(json)
                } else {
                    errorMsg = "تعذر جلب النص (${connection.responseCode})"
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: "خطأ في الاتصال"
            } finally {
                connection?.disconnect()
            }
            mainHandler.post { callback(result, errorMsg) }
        }
    }
}
