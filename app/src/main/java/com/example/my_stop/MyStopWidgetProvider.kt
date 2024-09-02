package com.example.my_stop

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MyStopWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val REFRESH_ACTION = "com.example.my_stop.REFRESH_ACTION"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)

                val intent = Intent(context, MyStopWidgetProvider::class.java).apply {
                    action = REFRESH_ACTION
                }
                val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_refresh_button, pendingIntent)

                // Fetch and update for MUNI
                fetchDataAndUpdateWidget(
                    context,
                    views,
                    appWidgetManager,
                    appWidgetId,
                    "https://my-stop.app/real-time-arrivals/15567/SF",
                    R.id.widget_muni_line_ref,
                    R.id.widget_muni_minutes_away,
                    R.id.widget_muni_line_ref_2,
                    R.id.widget_muni_minutes_away_2
                )

                // Fetch and update for BART
                fetchDataAndUpdateWidget(
                    context,
                    views,
                    appWidgetManager,
                    appWidgetId,
                    "https://my-stop.app/real-time-arrivals/24TH/BA",
                    R.id.widget_bart_line_ref,
                    R.id.widget_bart_minutes_away,
                    R.id.widget_bart_line_ref_2,
                    R.id.widget_bart_minutes_away_2
                )

            } catch (e: Exception) {
                Log.e("MyStopWidgetProvider", "Error in widget update", e)
            }
        }

        private fun fetchDataAndUpdateWidget(
            context: Context,
            views: RemoteViews,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            url: String,
            lineRefViewId1: Int,
            minutesAwayViewId1: Int,
            lineRefViewId2: Int,
            minutesAwayViewId2: Int
        ) {
            val queue = Volley.newRequestQueue(context)

            val stringRequest = StringRequest(
                Request.Method.GET, url,
                { response ->
                    try {
                        val jsonObject = JSONObject(response)
                        val monitoredStopVisits = jsonObject.getJSONObject("ServiceDelivery")
                            .getJSONObject("StopMonitoringDelivery")
                            .getJSONArray("MonitoredStopVisit")

                        // First upcoming stop
                        val firstVisit = monitoredStopVisits.getJSONObject(0)
                            .getJSONObject("MonitoredVehicleJourney")

                        val lineRef1 = firstVisit.getString("LineRef")
                        val expectedArrivalTime1 = firstVisit.getJSONObject("MonitoredCall")
                            .getString("ExpectedArrivalTime")

                        val arrivalTime1 = ZonedDateTime.parse(expectedArrivalTime1, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                        val now1 = ZonedDateTime.now()
                        val minutesAway1 = ChronoUnit.MINUTES.between(now1, arrivalTime1)

                        views.setTextViewText(lineRefViewId1, "Line #: $lineRef1")
                        views.setTextViewText(minutesAwayViewId1, "$minutesAway1 min")

                        // Second upcoming stop
                        if (monitoredStopVisits.length() > 1) {
                            val secondVisit = monitoredStopVisits.getJSONObject(1)
                                .getJSONObject("MonitoredVehicleJourney")

                            val lineRef2 = secondVisit.getString("LineRef")
                            val expectedArrivalTime2 = secondVisit.getJSONObject("MonitoredCall")
                                .getString("ExpectedArrivalTime")

                            val arrivalTime2 = ZonedDateTime.parse(expectedArrivalTime2, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                            val minutesAway2 = ChronoUnit.MINUTES.between(now1, arrivalTime2)

                            views.setTextViewText(lineRefViewId2, "Line #: $lineRef2")
                            views.setTextViewText(minutesAwayViewId2, "$minutesAway2 min")
                        } else {
                            views.setTextViewText(lineRefViewId2, "Line #: N/A")
                            views.setTextViewText(minutesAwayViewId2, "N/A")
                        }

                    } catch (e: Exception) {
                        Log.e("MyStopWidgetProvider", "Error processing JSON response", e)
                        views.setTextViewText(lineRefViewId1, "Error")
                        views.setTextViewText(minutesAwayViewId1, "N/A")
                        views.setTextViewText(lineRefViewId2, "Error")
                        views.setTextViewText(minutesAwayViewId2, "N/A")
                        Toast.makeText(context, "Error processing data", Toast.LENGTH_SHORT).show()
                    } finally {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                },
                { error ->
                    Log.e("MyStopWidgetProvider", "Error fetching data: ${error.message}")
                    views.setTextViewText(lineRefViewId1, "Failed to load")
                    views.setTextViewText(minutesAwayViewId1, "Check connection")
                    views.setTextViewText(lineRefViewId2, "Failed to load")
                    views.setTextViewText(minutesAwayViewId2, "Check connection")
                    appWidgetManager.updateAppWidget(appWidgetId, views)

                    // Show a toast with the error message
                    Toast.makeText(context, "Failed to fetch data. Check your connection.", Toast.LENGTH_SHORT).show()
                })

            queue.add(stringRequest)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == REFRESH_ACTION && context != null) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, MyStopWidgetProvider::class.java.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}