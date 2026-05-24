package com.example.scamshield.service

import android.content.Context
import android.util.Log
import com.example.scamshield.util.logD
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.DetectedThreat
import com.example.scamshield.data.MonitoringEvent
import com.example.scamshield.data.MonitoringStore
import com.example.scamshield.data.MonitorSource
import com.example.scamshield.data.RiskLevel
import com.example.scamshield.data.ScanStatus
import com.example.scamshield.data.ThreatStore
import com.example.scamshield.engine.ExplainabilityEngine
import com.example.scamshield.overlay.OverlayManager
import com.example.scamshield.overlay.toOverlayData
import com.example.scamshield.repository.BackendUnavailableException
import com.example.scamshield.repository.ScamAnalysisRepository
import com.example.scamshield.repository.ThreatRepository
import com.example.scamshield.util.AnalysisCooldown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

abstract class BaseMessageAnalysisService {

    protected abstract val monitorSource: MonitorSource
    protected abstract val appContext: Context
    protected abstract val tag: String

    // SupervisorJob isolates failures so one slow analysis job does not cancel siblings.
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val repository = ScamAnalysisRepository()
    private val threatRepo: ThreatRepository by lazy { ScamShieldApp.container().threatRepository }

    fun destroy() {
        serviceScope.cancel()
    }

    fun analyzeAndReport(text: String, packageName: String, appName: String) {
        if (text.isBlank()) return
        if (!AnalysisCooldown.shouldAllow(packageName, text)) return

        val scanId     = System.nanoTime()
        val preview    = text.take(80).replace('\n', ' ')
        val quickLabel = packageName.substringAfterLast('.')

        MonitoringStore.pushLiveEvent(
            MonitoringEvent(
                id          = scanId,
                packageName = packageName,
                appLabel    = quickLabel,
                textPreview = preview,
                status      = ScanStatus.SCANNING,
                source      = monitorSource,
            ),
        )
        logD(tag, "Monitor: SCANNING | pkg=$packageName | \"${preview.take(40)}\"")

        serviceScope.launch {
            MonitoringStore.pushLiveEvent(
                MonitoringEvent(
                    id          = scanId,
                    packageName = packageName,
                    appLabel    = quickLabel,
                    textPreview = preview,
                    status      = ScanStatus.ANALYZING,
                    source      = monitorSource,
                ),
            )
            logD(tag, "Monitor: ANALYZING | pkg=$packageName")

            repository.analyzeText(
                message     = text,
                packageName = packageName,
            ).fold(
                onSuccess = { response ->
                    ThreatStore.setAiConnected(true)
                    ThreatStore.incrementScanned()

                    logD(
                        tag,
                        "Result | pkg=$packageName | " +
                            "prob=${"%.3f".format(response.scamProbability)} | " +
                            "label=${response.label} | " +
                            "keywords=${response.suspiciousKeywords}",
                    )

                    val overlayData = response.toOverlayData(
                        capturedText  = text,
                        packageName   = packageName,
                        sourceAppName = appName,
                    )

                    val resultStatus = if (overlayData != null) ScanStatus.THREAT_DETECTED else ScanStatus.SAFE_MESSAGE
                    MonitoringStore.commitScan(
                        MonitoringEvent(
                            id          = scanId,
                            packageName = packageName,
                            appLabel    = appName,
                            textPreview = preview,
                            status      = resultStatus,
                            probability = response.scamProbability,
                            source      = monitorSource,
                        ),
                    )
                    logD(tag, "Monitor: $resultStatus | pkg=$packageName | prob=${"%.3f".format(response.scamProbability)}")

                    if (overlayData == null) return@fold

                    val analysis = ExplainabilityEngine.analyze(
                        message            = text,
                        senderRaw          = "",
                        backendKeywords    = response.suspiciousKeywords,
                        backendProbability = overlayData.probability,
                    )
                    val threat = DetectedThreat(
                        sourcePackage  = packageName,
                        messagePreview = overlayData.messagePreview,
                        probability    = analysis.finalProbability,
                        keywords       = overlayData.keywords,
                        explanation    = analysis.explanation,
                        appLabel       = appName,
                        category       = analysis.category,
                        backendLabel   = response.label,
                    )
                    ThreatStore.addThreat(threat)
                    runCatching {
                        threatRepo.insert(threat, appName, analysis.category, response.label)
                    }.onFailure { Log.w(tag, "Persist failed: ${it.message}") }

                    logD(
                        tag,
                        "Threat recorded | pkg=$packageName | app=\"$appName\" | " +
                            "level=${overlayData.threatLevel} | category=${analysis.category} | " +
                            "prob=${"%.3f".format(analysis.finalProbability)}",
                    )

                    OverlayManager.showWarning(appContext, overlayData)
                },
                onFailure = { e ->
                    ThreatStore.setAiConnected(false)
                    if (e is BackendUnavailableException) {
                        val analysis = ExplainabilityEngine.analyze(
                            message            = text,
                            senderRaw          = "",
                            backendKeywords    = emptyList(),
                            backendProbability = 0.0f,
                            backendOffline     = true,
                        )
                        val status = if (analysis.riskLevel != RiskLevel.SAFE)
                            ScanStatus.THREAT_DETECTED else ScanStatus.SAFE_MESSAGE
                        MonitoringStore.commitScan(
                            MonitoringEvent(
                                id          = scanId,
                                packageName = packageName,
                                appLabel    = quickLabel,
                                textPreview = preview,
                                status      = status,
                                probability = analysis.finalProbability,
                                source      = monitorSource,
                            ),
                        )
                        logD(tag, "Monitor: OFFLINE-LOCAL | pkg=$packageName | risk=${analysis.riskLevel} | prob=${"%.3f".format(analysis.finalProbability)}")
                    } else {
                        MonitoringStore.commitScan(
                            MonitoringEvent(
                                id          = scanId,
                                packageName = packageName,
                                appLabel    = quickLabel,
                                textPreview = preview,
                                status      = ScanStatus.ERROR,
                                source      = monitorSource,
                            ),
                        )
                        logD(tag, "Monitor: ERROR | pkg=$packageName")
                        Log.w(tag, "Analysis failed for $packageName — AI marked offline")
                    }
                },
            )
        }
    }
}
