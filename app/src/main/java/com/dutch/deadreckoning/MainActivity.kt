package com.dutch.deadreckoning

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dutch.deadreckoning.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity(), SensorEventListener {
    private val TAG = "_DUTCH"

    private lateinit var activityMainBinding: ActivityMainBinding

    private var currentLatitude = 0.0
    private var currentLongitude = 0.0

    private var lastLatitude = 0.0
    private var lastLongitude = 0.0

    private var distanceMoved = 0.0
    private var currentHeading = 0.0

    private var isDeadReckoningEnabled = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate()")

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        activityMainBinding.btnFetchLocation.setOnClickListener {
            Log.i(TAG, "btnFetchLocation onClick()")
            if (isLocationPermissionGranted()) {
                Log.i(TAG, "permission granted")
                fetchCurrentGPSLocation()
            } else {
                Log.i(TAG, "requesting permissions")
                requestLocationPermission()
            }
        }

        activityMainBinding.btnStopFetchLocation.setOnClickListener {
            lastLatitude = currentLatitude
            lastLongitude = currentLongitude
            Log.i(TAG, "btnFetchLocation laslatitude: $lastLatitude, lastLongitude: $lastLongitude")

        }
        activityMainBinding.btnEvaluateBothData.setOnClickListener {

//            TODO: accuracy evaluation

            Log.i(TAG,"btnEvaluateBothData onClick()")
            fetchCurrentGPSLocation()
            Log.e(TAG, "GPS coordinates: \nLATITUDE: $currentLatitude \nLONGITUDE: $currentLongitude")
            Log.e(TAG, "FUSED coordinates")
        }

        activityMainBinding.btnEnableDeadReckoning.setOnClickListener {
            Log.i(TAG, "btnEnableDeadReckoning onClick()")
            if (lastLatitude != 0.0 && lastLongitude != 0.0) {
                Log.i(TAG, "location 0.0, so enabling dead reckoning")
                isDeadReckoningEnabled = true
                Toast.makeText(this, "Dead reckoning enabled", Toast.LENGTH_SHORT).show()
            } else {
                Log.i(TAG, "location not 0.0")
            }
        }

        activityMainBinding.btnDisableDeadReckoning.setOnClickListener {
            Log.i(TAG, "btnDisableDeadReckoning onClick()")
            isDeadReckoningEnabled = false
            Toast.makeText(this, "disabling dead reckoning", Toast.LENGTH_SHORT).show()
        }

    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000
        )
    }

    private fun fetchCurrentGPSLocation() {

        if (!isDeadReckoningEnabled) {
            Log.i(TAG, "fetchCurrentGPSLocation()")
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    Log.i(TAG, "addOnSuccessListener")
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        Log.i(TAG, "latitude: $currentLatitude  longitude: $currentLongitude")
                        activityMainBinding.tvGpslatitude.text = "$currentLatitude"
                        activityMainBinding.tvGpslongitude.text = "$currentLongitude"
                    } else {
                        Log.i(TAG, "location null, so no value")
                        activityMainBinding.tvGpslatitude.text = "err"
                        activityMainBinding.tvGpslongitude.text = "err"
                    }
                }.addOnFailureListener { exception ->
                    Log.i(TAG, "addOnFailureListener exception:", exception)
                    Toast.makeText(this, "Cannot fetch gps", Toast.LENGTH_SHORT).show()

                }
            } catch (e: SecurityException) {
                Log.e(TAG, "catched: $e")
            }
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.i(TAG, "onRequestPermissionsResult")
                fetchCurrentGPSLocation()
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
//        Log.i(TAG, "onSensorChanged()")
        if (isDeadReckoningEnabled) {
            when (event?.sensor?.type) {
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    Log.i(TAG, "TYPE_LINEAR_ACCELERATION")
                    val acceleration = event.values[0]
                    val timeInterval = 1
                    distanceMoved += acceleration * timeInterval
                }

                Sensor.TYPE_ORIENTATION -> {
                    Log.i(TAG, "TYPE_ORIENTATION")
                    currentHeading = event.values[0].toDouble()
                }
            }

            val deltaLatitude = (distanceMoved * cos(Math.toRadians(currentHeading))) / 111000
            val deltaLongitude =
                (distanceMoved * sin(Math.toRadians(currentHeading))) / (111000 * cos(
                    Math.toRadians(lastLatitude)
                ))


            lastLatitude += deltaLatitude
            lastLongitude += deltaLongitude

            activityMainBinding.tvFusedLatitude.text = "$lastLatitude"
            activityMainBinding.tvFusedLongitude.text = "$lastLongitude"

            Log.d(
                TAG,
                "\"Latitude: $lastLatitude\\nLongitude: $lastLongitude\\nUsing Dead Reckoning\""
            )

        } else {
//            Log.i(TAG, "no dead recoking needed")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onPause() {
        Log.i(TAG, "onPause() unregistering sensorManager")
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        Log.i(TAG, "registering sensorManager")
        super.onResume()
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }
}