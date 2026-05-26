package com.example.scamshield.util

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class AppUpdateHelper(private val activity: ComponentActivity) {

    private val manager: AppUpdateManager? = runCatching {
        AppUpdateManagerFactory.create(activity)
    }.getOrNull()

    val isAvailable: Boolean get() = manager != null

    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var pendingFlexibleInfo: AppUpdateInfo? = null

    // Must be called in Activity.onCreate before onStart.
    fun init() {
        if (manager == null) return
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { }
    }

    fun checkOnStart(onFlexibleAvailable: () -> Unit) {
        val mgr = manager ?: return
        mgr.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                    startUpdate(mgr, info, AppUpdateType.IMMEDIATE)

                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    pendingFlexibleInfo = info
                    onFlexibleAvailable()
                }
            }
        }
    }

    fun checkManually(onNoUpdate: () -> Unit, onFlexibleAvailable: () -> Unit) {
        val mgr = manager ?: return
        mgr.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                        startUpdate(mgr, info, AppUpdateType.IMMEDIATE)

                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        pendingFlexibleInfo = info
                        onFlexibleAvailable()
                    }

                    else -> onNoUpdate()
                }
            }
            .addOnFailureListener { onNoUpdate() }
    }

    fun startFlexibleInstall() {
        val mgr = manager ?: return
        val info = pendingFlexibleInfo
        if (info != null) {
            startUpdate(mgr, info, AppUpdateType.FLEXIBLE)
            return
        }
        mgr.appUpdateInfo.addOnSuccessListener { fresh ->
            if (fresh.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                fresh.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                startUpdate(mgr, fresh, AppUpdateType.FLEXIBLE)
            }
        }
    }

    fun onResume() {
        val mgr = manager ?: return
        mgr.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                mgr.completeUpdate()
            }
            // Resume a stalled IMMEDIATE update (e.g. after process restart).
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdate(mgr, info, AppUpdateType.IMMEDIATE)
            }
        }
    }

    private fun startUpdate(mgr: AppUpdateManager, info: AppUpdateInfo, type: Int) {
        runCatching {
            mgr.startUpdateFlowForResult(
                info,
                launcher ?: return@runCatching,
                AppUpdateOptions.newBuilder(type).build(),
            )
        }
    }
}
