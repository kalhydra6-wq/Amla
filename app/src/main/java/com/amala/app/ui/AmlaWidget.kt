package com.amala.app.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.amala.app.MainActivity
import com.amala.app.R
import com.amala.app.audio.AudioController

/** Small home-screen widget: EQ toggle, current preset and a next-preset button. */
class AmlaWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateView(context, manager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> AudioController.toggleEnabled()
            ACTION_NEXT -> AudioController.cyclePreset(1)
        }
        update(context)
    }

    companion object {
        private const val ACTION_TOGGLE = "com.amala.app.WIDGET_TOGGLE"
        private const val ACTION_NEXT = "com.amala.app.WIDGET_NEXT"

        fun update(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, AmlaWidget::class.java))
                ids.forEach { id -> updateView(context, manager, id) }
            }
        }

        private fun updateView(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_amla)
            val toggleIntent = PendingIntent.getBroadcast(
                context, 1, Intent(context, AmlaWidget::class.java).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val nextIntent = PendingIntent.getBroadcast(
                context, 2, Intent(context, AmlaWidget::class.java).setAction(ACTION_NEXT),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val openIntent = PendingIntent.getActivity(
                context, 3, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_toggle, toggleIntent)
            views.setOnClickPendingIntent(R.id.widget_next, nextIntent)
            views.setOnClickPendingIntent(R.id.widget_preset, openIntent)
            views.setTextViewText(R.id.widget_toggle, if (AudioController.enabled) "EQ ON" else "EQ OFF")
            views.setTextViewText(R.id.widget_preset, AudioController.profile.preset.label)
            manager.updateAppWidget(id, views)
        }
    }
}
