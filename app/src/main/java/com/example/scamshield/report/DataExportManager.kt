package com.example.scamshield.report

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.scamshield.data.db.ThreatEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExportManager {

    private val stampFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val isoFmt   = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    suspend fun exportToCsv(context: Context, threats: List<ThreatEntity>): Uri =
        withContext(Dispatchers.IO) {
            val file = outFile(context, "csv")
            file.bufferedWriter().use { w ->
                w.write("timestamp,sourceApp,riskLevel,category,probability,messagePreview,keywords\n")
                threats.forEach { t ->
                    w.write(
                        listOf(
                            isoFmt.format(Date(t.timestamp)),
                            t.appLabel.ifBlank { t.sourcePackage },
                            t.riskLevel,
                            t.category,
                            String.format(Locale.US, "%.4f", t.probability),
                            t.messagePreview,
                            t.keywords,
                        ).joinToString(",") { csvCell(it) } + "\n"
                    )
                }
            }
            uriFor(context, file)
        }

    suspend fun exportToJson(context: Context, threats: List<ThreatEntity>): Uri =
        withContext(Dispatchers.IO) {
            val file = outFile(context, "json")
            val array = JSONArray()
            threats.forEach { t ->
                array.put(JSONObject().apply {
                    put("id",               t.id)
                    put("timestamp",        isoFmt.format(Date(t.timestamp)))
                    put("sourcePackage",    t.sourcePackage)
                    put("appLabel",         t.appLabel)
                    put("messagePreview",   t.messagePreview)
                    put("probability",      t.probability.toDouble())
                    put("riskLevel",        t.riskLevel)
                    put("category",         t.category)
                    put("backendLabel",     t.backendLabel)
                    put("keywords",         t.keywords)
                    put("phishingLinks",    t.phishingLinks)
                    put("urgencyPatterns",  t.urgencyPatterns)
                    put("socialIndicators", t.socialIndicators)
                    put("explanation",      t.explanation)
                    put("analysisSource",   t.analysisSource)
                })
            }
            file.writeText(array.toString(2))
            uriFor(context, file)
        }

    private fun outFile(context: Context, ext: String): File {
        val dir = File(context.getExternalFilesDir(null), "Reports").also { it.mkdirs() }
        return File(dir, "ScamShield_Export_${stampFmt.format(Date())}.$ext")
    }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun csvCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(',') || escaped.contains('"') || escaped.contains('\n'))
            "\"$escaped\"" else escaped
    }
}
