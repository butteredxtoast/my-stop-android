package com.example.my_stop

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainActivity : AppCompatActivity() {
    private lateinit var responseTextView: TextView
    private lateinit var toggleButton: Button
    private lateinit var refreshButton: Button
    private var isDataVisible = false
    private var formattedData: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        responseTextView = findViewById(R.id.responseTextView)
        toggleButton = findViewById(R.id.toggleButton)
        refreshButton = findViewById(R.id.refreshButton)

        // Initially set the button to show data
        toggleButton.text = "Show Data"

        // Set up the toggle button click listener
        toggleButton.setOnClickListener {
            if (isDataVisible) {
                responseTextView.visibility = View.GONE
                refreshButton.visibility = View.GONE
                toggleButton.text = "Show Data"
                isDataVisible = false
            } else {
                if (formattedData == null) {
                    fetchAndFormatData()
                } else {
                    responseTextView.text = formattedData
                    responseTextView.visibility = View.VISIBLE
                    refreshButton.visibility = View.VISIBLE
                    toggleButton.text = "Hide Data"
                    isDataVisible = true
                }
            }
        }

        // Set up the refresh button click listener
        refreshButton.setOnClickListener {
            fetchAndFormatData()
        }
    }

    // Function to fetch data from the API and format it
    private fun fetchAndFormatData() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://my-stop.app/real-time-arrivals/15567/SF"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                formattedData = formatJsonResponse(response)
                responseTextView.text = formattedData
                responseTextView.visibility = View.VISIBLE
                refreshButton.visibility = View.VISIBLE
                toggleButton.text = "Hide Data"
                isDataVisible = true
            },
            { error ->
                responseTextView.text = "Error fetching data: ${error.message}"
                responseTextView.visibility = View.VISIBLE
                refreshButton.visibility = View.VISIBLE
                toggleButton.text = "Hide Data"
                isDataVisible = true
            })

        queue.add(stringRequest)
    }

    // Function to format JSON response
    private fun formatJsonResponse(response: String): String {
        val formattedResponse = StringBuilder()

        try {
            val jsonObject = JSONObject(response)
            val monitoredStopVisits: JSONArray =
                jsonObject.getJSONObject("ServiceDelivery")
                    .getJSONObject("StopMonitoringDelivery")
                    .getJSONArray("MonitoredStopVisit")

            for (i in 0 until monitoredStopVisits.length()) {
                val visit = monitoredStopVisits.getJSONObject(i)
                val vehicleJourney = visit.getJSONObject("MonitoredVehicleJourney")

                val lineRef = vehicleJourney.getString("LineRef")
                val expectedArrivalTime = vehicleJourney.getJSONObject("MonitoredCall")
                    .getString("ExpectedArrivalTime")

                // Parse the ExpectedArrivalTime
                val arrivalTime = ZonedDateTime.parse(expectedArrivalTime, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                val now = ZonedDateTime.now()

                // Calculate minutes away
                val minutesAway = ChronoUnit.MINUTES.between(now, arrivalTime)

                // Append the data to the formatted response
                formattedResponse.append("Line #: $lineRef\n")
                formattedResponse.append("$minutesAway minutes away\n")
                formattedResponse.append("\n")
            }
        } catch (e: Exception) {
            return "Invalid JSON response"
        }

        return if (formattedResponse.isEmpty()) "No data available" else formattedResponse.toString()
    }
}
