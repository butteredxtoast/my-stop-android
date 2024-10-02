package com.example.my_stop

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.appcompat.widget.SearchView

class MainActivity : AppCompatActivity() {

    private lateinit var responseTextView: TextView
    private lateinit var refreshButton: Button
    private lateinit var agencySpinner: Spinner
    private lateinit var lineSpinner: Spinner
    private lateinit var directionSpinner: Spinner
    private lateinit var stopSpinner: Spinner

    private var selectedAgency: String = "MUNI"  // Default to MUNI
    private var selectedLine: String = ""
    private var selectedDirection: String = ""
    private var selectedStop: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        responseTextView = findViewById(R.id.responseTextView)
        refreshButton = findViewById(R.id.refreshButton)
        agencySpinner = findViewById(R.id.agencySpinner)
        lineSpinner = findViewById(R.id.lineSpinner)
        directionSpinner = findViewById(R.id.directionSpinner)
        stopSpinner = findViewById(R.id.stopSpinner)

        // Populate the agency spinner with a placeholder
        populateAgencySpinner()

        // Set up the Agency Spinner onItemSelectedListener
        agencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedAgency = parent.getItemAtPosition(position).toString()

                // Check if a valid agency is selected (not the placeholder)
                if (position > 0) {
                    lineSpinner.visibility = View.VISIBLE
                    populateLineSpinner()
                } else {
                    // Hide other spinners if placeholder is selected
                    lineSpinner.visibility = View.GONE
                    directionSpinner.visibility = View.GONE
                    stopSpinner.visibility = View.GONE
                    responseTextView.text = ""  // Clear displayed data
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Line Spinner
        lineSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedLine = parent.getItemAtPosition(position).toString()
                directionSpinner.visibility = View.VISIBLE
                populateDirectionSpinner()  // Populate direction spinner when a line is selected
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Direction Spinner
        directionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedDirection = parent.getItemAtPosition(position).toString()
                stopSpinner.visibility = View.VISIBLE
                populateStopSpinner()  // Populate stop spinner
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Stop Spinner
        stopSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedStop = parent.getItemAtPosition(position).toString()
                fetchDataAndDisplay()  // Fetch and display data
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Refresh button click listener
        refreshButton.setOnClickListener {
            fetchDataAndDisplay()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem: MenuItem? = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        // Search bar
        searchView?.queryHint = "Search for a stop"
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    filterStops(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    filterStops(newText)
                }
                return true
            }
        })
        return true
    }

    private fun filterStops(query: String) {
        // Hardcoded stop list
        val stopList = listOf("24TH St", "Mission St", "Van Ness", "Powell St", "Civic Center")
        val filteredStops = stopList.filter { it.contains(query, ignoreCase = true) }

        stopSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filteredStops)
        stopSpinner.visibility = View.VISIBLE
    }

    private fun fetchDataAndDisplay() {
        val url = when (selectedAgency) {
            "BART" -> "https://my-stop.app/real-time-arrivals/24TH/BA"
            "MUNI" -> "https://my-stop.app/real-time-arrivals/15567/SF"
            else -> "https://my-stop.app/real-time-arrivals/15567/SF"  // Default to MUNI
        }

        val queue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                val formattedData = formatJsonResponse(response)
                responseTextView.text = formattedData
                refreshButton.visibility = View.VISIBLE
            },
            { error ->
                responseTextView.text = "Error fetching data: ${error.message}"
                refreshButton.visibility = View.VISIBLE
            })

        queue.add(stringRequest)
    }

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

    private fun populateAgencySpinner() {
        val agencyList = arrayOf("Select an Agency", "MUNI", "BART")
        val agencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, agencyList)
        agencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        agencySpinner.adapter = agencyAdapter
        agencySpinner.setSelection(0)  // Set default to the placeholder value
    }

    private fun populateLineSpinner() {
        val lineList = arrayOf("Line 1", "Line 2")
        val lineAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lineList)
        lineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        lineSpinner.adapter = lineAdapter
        lineSpinner.setSelection(0)
    }

    private fun populateDirectionSpinner() {
        val directionList = arrayOf("Inbound", "Outbound")
        val directionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, directionList)
        directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        directionSpinner.adapter = directionAdapter
        directionSpinner.setSelection(0)
    }

    private fun populateStopSpinner() {
        val stopList = arrayOf("Stop 1", "Stop 2")
        val stopAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stopList)
        stopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        stopSpinner.adapter = stopAdapter
        stopSpinner.setSelection(0)
    }
}