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
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var responseTextView: TextView
    private lateinit var toggleMuniButton: Button
    private lateinit var toggleBartButton: Button
    private lateinit var refreshButton: Button
    private var isDataVisible = false
    private var formattedData: String? = null
    private val muniUrl = "https://my-stop.app/real-time-arrivals/15567/SF"
    private val bartUrl = "https://my-stop.app/real-time-arrivals/24TH/BA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        responseTextView = findViewById(R.id.responseTextView)
        toggleMuniButton = findViewById(R.id.toggleButton)
        toggleBartButton = findViewById(R.id.toggleBartButton)
        refreshButton = findViewById(R.id.refreshButton)

        // Initially set the MUNI button to show data
        toggleMuniButton.text = "Show MUNI Data"

        toggleMuniButton.setOnClickListener {
            if (isDataVisible) {
                responseTextView.visibility = View.GONE
                refreshButton.visibility = View.GONE
                toggleMuniButton.text = "Show MUNI Data"
                isDataVisible = false
            } else {
                fetchAndFormatData(muniUrl)
                responseTextView.visibility = View.VISIBLE
                refreshButton.visibility = View.VISIBLE
                toggleMuniButton.text = "Hide Data"
                isDataVisible = true
            }
        }

        toggleBartButton.setOnClickListener {
            if (isDataVisible) {
                responseTextView.visibility = View.GONE
                refreshButton.visibility = View.GONE
                toggleBartButton.text = "Show BART Data"
                isDataVisible = false
            } else {
                fetchAndFormatData(bartUrl)
                responseTextView.visibility = View.VISIBLE
                refreshButton.visibility = View.VISIBLE
                toggleBartButton.text = "Hide Data"
                isDataVisible = true
            }
        }

        // Set up the refresh button click listener
        refreshButton.setOnClickListener {
            // Refresh the currently displayed data
            if (toggleMuniButton.text == "Hide Data") {
                fetchAndFormatData(muniUrl)
            } else if (toggleBartButton.text == "Hide Data") {
                fetchAndFormatData(bartUrl)
            }
        }
    }

    // Function to fetch data from the provided URL and format it
    private fun fetchAndFormatData(url: String) {
        val queue = Volley.newRequestQueue(this)

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                formattedData = formatJsonResponse(response)
                responseTextView.text = formattedData
            },
            { error ->
                responseTextView.text = "Error fetching data: ${error.message}"
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