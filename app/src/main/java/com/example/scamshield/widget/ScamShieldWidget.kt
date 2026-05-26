package com.example.scamshield.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.scamshield.MainActivity
import com.example.scamshield.R
import com.example.scamshield.data.db.ScamShieldDatabase
import com.example.scamshield.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class ScamShieldWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /** Call this from anywhere to refresh all placed widget instances. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ScamShieldWidget::class.java),
            )
            if (ids.isNotEmpty()) {
                ids.forEach { id -> updateWidget(context, manager, id) }
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
        ) {
            scope.launch {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val todayCount = runCatching {
                    ScamShieldDatabase.get(context)
                        .threatDao()
                        .observeCountSince(startOfDay)
                        .first()
                }.getOrDefault(0)

                val isProtected = runCatching {
                    SettingsRepository.get(context).settings.first().activeProtection
                }.getOrDefault(true)

                manager.updateAppWidget(widgetId, buildViews(context, isProtected, todayCount))
            }
        }

        private fun buildViews(
            context: Context,
            isProtected: Boolean,
            todayCount: Int,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_scamshield)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            if (isProtected) {
                views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_status_protected))
                views.setTextColor(R.id.widget_status, Color.parseColor("#4CAF50"))
                views.setInt(R.id.widget_shield_icon, "setColorFilter", Color.parseColor("#00E5FF"))
            } else {
                views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_status_off))
                views.setTextColor(R.id.widget_status, Color.parseColor("#8D9BAD"))
                views.setInt(R.id.widget_shield_icon, "setColorFilter", Color.parseColor("#8D9BAD"))
            }

            val countText = if (todayCount == 0) {
                context.getString(R.string.widget_no_threats_today)
            } else {
                context.resources.getQuantityString(R.plurals.widget_threats_today, todayCount, todayCount)
            }
            views.setTextViewText(R.id.widget_threat_count, countText)

            return views
        }
    }
}
