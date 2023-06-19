package com.example.runmate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Debug
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock

// Service class to manage the step counter and the stats calculation
class CaloriesService : Service(), SensorEventListener {
    private var isFirstStep = true
    private var isTrainingPaused = false
    fun setIsTrainingPaused(paused: Boolean){
        isTrainingPaused = paused
    }

    private var sensorManager: SensorManager? = null
    private val statsLock = ReentrantLock()
    private var job: Job? = null

    // timestamps to handle calories computation
    private val computingTime = 10000L

    // variables for calories computation
    private var horizontalComponent = 0.1
    private var verticalComponent = 1.8
    private var h = 180 // [cm]
    private var m = 80 // [kg]
    private val G = 0.01f
    private var k = 0.42 // walk = 0.42, run = 0.6
    private var stepSize = (k * h / 100).toFloat() // [m]

    // variables to save steps, distance and calories
    private var currentSteps = 0 // steps in this session before calling computeStats()
    private var totalSteps = 0 // steps in this session
    private var totalDistance = 0f // distance in this session
    private var totalCalories = 0f // calories in this session

    // when the training started
    private lateinit var startTime: LocalTime

    private lateinit var trainingType: String // "Corsa" or "Camminata"

    // saves instance of TrainingFragmentCallback
    private lateinit var tfCallback: TrainingFragmentCallback

    // sets instance of TrainingFragmentCallback so the service can call TrainingFragment methods
    fun setTrainingFragmentCallback(callback: TrainingFragmentCallback) {
        tfCallback = callback
    }

    // binder given to clients (TrainingFragment)
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {

        // returns this instance of CaloriesService so clients can call public methods
        fun getService(): CaloriesService = this@CaloriesService
    }

    private lateinit var serviceTrace : Trace

    override fun onCreate() {
        super.onCreate()

        serviceTrace = FirebasePerformance.getInstance().newTrace("CaloriesServiceTrace")
        serviceTrace.start()

        // Measure CPU time used by the service
        val cpuTime = Debug.threadCpuTimeNanos()

        // Get memory information of the service
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)

        // Calculate the total RAM consumption of the service
        val totalPss = memoryInfo.totalPss

        // Record CPU and RAM consumption metrics in the service trace
        serviceTrace?.putMetric("cpu_time", cpuTime)
        serviceTrace?.putMetric("total_pss", totalPss.toLong())

        // show a notification on the screen and allow the service to work in the background
        startForeground(1, createNotification())

        // initialize some variables
        initialize()
    }

    override fun onBind(intent: Intent): IBinder {
        trainingType = intent.getStringExtra("trainingType").toString()
        if (trainingType == "Corsa"){
            k = 0.6
            horizontalComponent = 0.2
            verticalComponent = 0.9
        }
        stepSize = (k * h / 100).toFloat()
        return binder
    }

    private fun initialize(){
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // step sensor registration
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepSensor != null) {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
        else{
            Toast.makeText(baseContext, "Sensore \"step detector\" non rilevato", Toast.LENGTH_SHORT).show()
        }

        // get user data
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val sharedPref = getSharedPreferences("${uid}UserPrefs", Context.MODE_PRIVATE)
        h = sharedPref.getInt("Height", 180)
        m = sharedPref.getInt("Weight", 80)

        startTime = LocalTime.now()
    }

    override fun onDestroy() {
        super.onDestroy()

        sensorManager?.unregisterListener(this)
        job?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)

        serviceTrace.stop()
    }

    private fun createNotification(): Notification {
        val channelId = "RunmateCaloriesService"
        val channelName = "Runmate Activity Tracker"

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Runmate sta registrando la tua attività")
            .setSmallIcon(R.drawable.runmate_notification_icon)
            .build()
    }

    // Detects user steps
    override fun onSensorChanged(event: SensorEvent?) {
        // we don't count steps if the training is paused
        if (!isTrainingPaused) {
            currentSteps++

            if (isFirstStep) {
                isFirstStep = false
                startCoroutineCalories()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    /* The coroutine checks for changes every "computingTime" milliseconds.
       If no steps are detected, it is stopped to save resources.
       A lock is used to avoid race conditions with the code inside registerTraining().
     */
    private fun startCoroutineCalories() {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(computingTime)

                try {
                    statsLock.lock()
                    if (currentSteps != 0) {
                        computeStats()
                    }
                    else {
                        job?.cancel()
                        isFirstStep = true
                    }
                } finally {
                    statsLock.unlock()
                }
            }
        }
    }

    // Calculates distance and calories and makes a call to update the UI.
    private fun computeStats(){
        val (distance, calories) = computeCalories()

        totalSteps += currentSteps
        totalDistance += distance
        totalCalories += calories

        currentSteps = 0

        // update the UI
        tfCallback.updateUI(totalSteps, totalDistance, totalCalories)
    }

    // Computes calories based on the time interval. Returns distance and calories.
    private fun computeCalories(): Pair<Float, Float> {
        val interval_in_seconds = computingTime / 1000f // [s]
        val d = currentSteps * stepSize // [m]
        val S = (d / interval_in_seconds) * 60 // [m / min]
        val VO2 = 3.5 + (horizontalComponent * S) + (verticalComponent * S * G) // [mL / (kg * min)]
        var calories = ((VO2 * m) / 1000) * 5 // [kcal / min]
        calories = calories * interval_in_seconds / 60 // [kcal]

        return Pair(d, calories.toFloat())
    }

    // Locally saves stats
    private fun saveStats(){
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val sharedPref = getSharedPreferences("${uid}UserPrefs", Context.MODE_PRIVATE)
        sharedPref?.edit()?.apply {
            val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

            // this object saves the current session that will be present in the activities list
            val trainingObj = TrainingObject(trainingType, currentDate, startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                totalSteps, totalDistance, totalCalories, tfCallback.getTrainingTime())

            // take older trainings (if any) and add this training
            var json = sharedPref.getString("trainingList", null)
            val gson = Gson()
            if (json != null) {
                val listType = object : TypeToken<MutableList<TrainingObject>>() {}.type
                val pastTraining = gson.fromJson<MutableList<TrainingObject>>(json, listType)
                pastTraining.add(0, trainingObj)
                json = gson.toJson(pastTraining)
            }
            else{ // this training is the first training
                val trainingList = mutableListOf(trainingObj)
                json = gson.toJson(trainingList)
            }
            putString("trainingList", json)
            putString("currentDate", currentDate)

            // old values are added to new values and the stats are locally saved
            putInt("totalSteps", totalSteps + sharedPref.getInt("totalSteps", 0))
            putFloat("totalDistance", totalDistance + sharedPref.getFloat("totalDistance", 0f))
            putFloat("totalCalories", totalCalories + sharedPref.getFloat("totalCalories", 0f))
            apply()
        }
    }

    /* The user pressed the stop button => if necessary, new stats are computed and saved.
       A lock avoid race conditions because also the coroutine can access "currentSteps" and call computeStats().
     */
    fun registerTraining() {
        try {
            statsLock.lock()
            if (currentSteps != 0){
                computeStats()
                job?.cancel()
            }
            if(totalSteps != 0) {
                saveStats()
                Toast.makeText(baseContext, "Attività registrata!", Toast.LENGTH_SHORT).show()
            }
        } finally {
            statsLock.unlock()
        }

        sensorManager?.unregisterListener(this)
    }
}