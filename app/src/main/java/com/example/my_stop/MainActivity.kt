package com.example.my_stop

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.my_stop.R
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var responseTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        responseTextView = findViewById(R.id.responseTextView)

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.511.org/transit/StopMonitoring?agency=SF&stopCode=15567&api_key=b149355b-716b-481e-902e-e565f96952fe"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                // Format the JSON response
                val prettyJson = try {
                    val jsonObject = JSONObject(response)
                    jsonObject.toString(4) // Indent with 4 spaces
                } catch (e: Exception) {
                    "Invalid JSON response"
                }

                // Display the formatted JSON in the TextView
                responseTextView.text = prettyJson
            }, { error ->
                // Handle the error using string resource
                responseTextView.text = getString(R.string.error_message, error.message)
            })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }
}
