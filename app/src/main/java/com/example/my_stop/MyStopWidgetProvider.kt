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
import java.lang.reflect.InvocationTargetException
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

                fetchDataAndUpdateWidget(context, views, appWidgetManager, appWidgetId)

            } catch (e: InvocationTargetException) {
                val cause = e.cause
                Log.e("MyStopWidgetProvider", "Error inflating layout", cause)
                // Handle the specific cause of the exception here if needed
            } catch (e: Exception) {
                // Catch any other exceptions
                Log.e("MyStopWidgetProvider", "General error in widget update", e)
            }
        }

        private fun fetchDataAndUpdateWidget(context: Context, views: RemoteViews, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val queue = Volley.newRequestQueue(context)
            val muniUrl = "https://my-stop.app/real-time-arrivals/15567/SF"
            val bartUrl = "https://my-stop.app/real-time-arrivals/24TH/BA"

            val stringRequest = StringRequest(
                Request.Method.GET, muniUrl,
                { response ->
                    try {
                        val jsonObject = JSONObject(response)
                        val monitoredStopVisits = jsonObject.getJSONObject("ServiceDelivery")
                            .getJSONObject("StopMonitoringDelivery")
                            .getJSONArray("MonitoredStopVisit")

                        // Process first and second visits
                        val firstVisit = monitoredStopVisits.getJSONObject(0).getJSONObject("MonitoredVehicleJourney")
                        val secondVisit = monitoredStopVisits.getJSONObject(1).getJSONObject("MonitoredVehicleJourney")

                        // Extract first line data
                        val firstLineRef = firstVisit.getString("LineRef")
                        val firstExpectedArrivalTime = firstVisit.getJSONObject("MonitoredCall")
                            .getString("ExpectedArrivalTime")
                        val firstArrivalTime = ZonedDateTime.parse(firstExpectedArrivalTime, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                        val firstMinutesAway = ChronoUnit.MINUTES.between(ZonedDateTime.now(), firstArrivalTime)

                        // Extract second line data
                        val secondLineRef = secondVisit.getString("LineRef")
                        val secondExpectedArrivalTime = secondVisit.getJSONObject("MonitoredCall")
                            .getString("ExpectedArrivalTime")
                        val secondArrivalTime = ZonedDateTime.parse(secondExpectedArrivalTime, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                        val secondMinutesAway = ChronoUnit.MINUTES.between(ZonedDateTime.now(), secondArrivalTime)

                        // Update the widget views with the fetched data
                        views.setTextViewText(R.id.widget_line_ref, "Line #: $firstLineRef")
                        views.setTextViewText(R.id.widget_minutes_away, "$firstMinutesAway min")
                        views.setTextViewText(R.id.widget_line_ref_2, "Line #: $secondLineRef")
                        views.setTextViewText(R.id.widget_minutes_away_2, "$secondMinutesAway min")

                    } catch (e: Exception) {
                        Log.e("MyStopWidgetProvider", "Error processing JSON response", e)
                        views.setTextViewText(R.id.widget_line_ref, "Error")
                        views.setTextViewText(R.id.widget_minutes_away, "N/A")
                        views.setTextViewText(R.id.widget_line_ref_2, "Error")
                        views.setTextViewText(R.id.widget_minutes_away_2, "N/A")
                        Toast.makeText(context, "Error processing data", Toast.LENGTH_SHORT).show()
                    } finally {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                },
                { error ->
                    Log.e("MyStopWidgetProvider", "Error fetching data: ${error.message}")
                    views.setTextViewText(R.id.widget_line_ref, "Failed to load")
                    views.setTextViewText(R.id.widget_minutes_away, "Check connection")
                    views.setTextViewText(R.id.widget_line_ref_2, "Failed to load")
                    views.setTextViewText(R.id.widget_minutes_away_2, "Check connection")
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