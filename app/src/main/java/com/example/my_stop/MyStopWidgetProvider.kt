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
                // Attempt to inflate the layout and update the widget
                val views = RemoteViews(context.packageName, R.layout.widget_layout)

                val intent = Intent(context, MyStopWidgetProvider::class.java).apply {
                    action = REFRESH_ACTION
                }
                val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_refresh_button, pendingIntent)

                // Optionally, you can add another try/catch around this fetch method if needed
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
            val url = "https://my-stop.app/real-time-arrivals/15567/SF"

            val stringRequest = StringRequest(
                Request.Method.GET, url,
                { response ->
                    try {
                        val jsonObject = JSONObject(response)
                        val visit = jsonObject.getJSONObject("ServiceDelivery")
                            .getJSONObject("StopMonitoringDelivery")
                            .getJSONArray("MonitoredStopVisit")
                            .getJSONObject(0)
                            .getJSONObject("MonitoredVehicleJourney")

                        val lineRef = visit.getString("LineRef")
                        val expectedArrivalTime = visit.getJSONObject("MonitoredCall")
                            .getString("ExpectedArrivalTime")

                        val arrivalTime = ZonedDateTime.parse(expectedArrivalTime, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                        val now = ZonedDateTime.now()
                        val minutesAway = ChronoUnit.MINUTES.between(now, arrivalTime)

                        // Update the widget views with the fetched data
                        views.setTextViewText(R.id.widget_line_ref, "Line #: $lineRef")
                        views.setTextViewText(R.id.widget_minutes_away, "$minutesAway min")

                    } catch (e: Exception) {
                        // Handle JSON parsing error
                        Log.e("MyStopWidgetProvider", "Error processing JSON response", e)
                        views.setTextViewText(R.id.widget_line_ref, "Error")
                        views.setTextViewText(R.id.widget_minutes_away, "N/A")
                        // Show a toast for the JSON error
                        Toast.makeText(context, "Error processing data", Toast.LENGTH_SHORT).show()
                    } finally {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                },
                { error ->
                    // Handle the error by displaying a message in the widget
                    Log.e("MyStopWidgetProvider", "Error fetching data: ${error.message}")
                    views.setTextViewText(R.id.widget_line_ref, "Failed to load")
                    views.setTextViewText(R.id.widget_minutes_away, "Check connection")
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
