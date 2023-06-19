package com.example.runmate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import kotlin.math.roundToInt

// Class of the fragment of the Training section
class TrainingFragment:Fragment(R.layout.fragment_training), TrainingFragmentCallback {
    private lateinit var cService: CaloriesService
    private var isServiceBounded: Boolean = false

    private lateinit var tv_totalSteps: TextView
    private lateinit var tv_totalDistance: TextView
    private lateinit var tv_totalCalories: TextView
    private lateinit var chronometer: Chronometer

    // variables to handle the training stages
    private var isPlayed = false
    private var isPaused = false
    private var isServiceStarted = false
    private var pauseOffset: Long = 0
    private lateinit var trainingTime: String
    private lateinit var trainingType: String
    private var isTraining = false

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {

            // We've bound to CaloriesService, cast the IBinder and get CaloriesService instance.
            val binder = service as CaloriesService.LocalBinder
            cService = binder.getService()
            isServiceBounded = true

            cService.setTrainingFragmentCallback(this@TrainingFragment)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isServiceBounded = false
        }
    }

    fun getIsTraining(): Boolean{
        return isTraining
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            trainingType = it.getString("trainingType").toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_training, container, false)

        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        view.findViewById<TextView>(R.id.tv_training_type).text = trainingType
        tv_totalSteps = view.findViewById(R.id.tv_steps_train)
        tv_totalDistance = view.findViewById(R.id.tv_distance_train)
        tv_totalCalories = view.findViewById(R.id.tv_calories_train)
        chronometer = view.findViewById(R.id.chronometer_train)

        // the training chronometer shows time formatted to hours, minutes, seconds
        chronometer.setOnChronometerTickListener {
            val elapsedTimeMillis = SystemClock.elapsedRealtime() - chronometer.base
            val elapsedTimeSeconds = elapsedTimeMillis / 1000
            val h = elapsedTimeSeconds / 3600
            val m = (elapsedTimeSeconds % 3600) / 60
            val s = elapsedTimeSeconds % 60
            trainingTime = "$h h $m min"
            chronometer.text = String.format("%02d:%02d:%02d", h, m, s)
        }

        val intentService = Intent(context, CaloriesService::class.java)

        // code to handle training buttons click events
        val btn_play_pause = view.findViewById<ImageButton>(R.id.btn_play_pause_train)
        btn_play_pause.setOnClickListener {
            if (!isServiceStarted){ // the service is not started yet, so start it
                isServiceStarted = true

                logTrainingStartStopEvent(firebaseAnalytics, "training_started")

                // bind to CaloriesService
                intentService.putExtra("trainingType", trainingType)
                context?.bindService(intentService, connection, Context.BIND_AUTO_CREATE)
            }

            if (!isPlayed) { // the play button is pressed
                btn_play_pause.setImageResource(R.drawable.pause_circle)

                logTrainingFlowEvent(firebaseAnalytics, "play_button_pressed")

                if(!isPaused) // the pause button was not previously pressed
                    pauseOffset = 0
                else{ // the pause button was previously pressed
                    isPaused = false
                    cService.setIsTrainingPaused(false)
                }
                chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
                chronometer.start()
            }
            else { // the pause button is pressed
                btn_play_pause.setImageResource(R.drawable.play_circle)

                logTrainingFlowEvent(firebaseAnalytics, "pause_button_pressed")

                cService.setIsTrainingPaused(true)
                isPaused = true
                chronometer.stop()
                pauseOffset = SystemClock.elapsedRealtime() - chronometer.base
            }
            isTraining = true
            isPlayed = !isPlayed
        }

        val btn_stop = view.findViewById<ImageButton>(R.id.btn_stop_train)
        btn_stop.setOnClickListener {
            if (isServiceStarted){  // stop the service if it is running
                isServiceStarted = false

                // register the current training
                cService.registerTraining()

                unbindCS()

                logTrainingStartStopEvent(firebaseAnalytics, "training_stopped")

                isPlayed = false
                isPaused = false
                pauseOffset = 0
                btn_play_pause.setImageResource(R.drawable.play_circle)
                chronometer.stop()

                // reset UI
                updateUI(0, 0f, 0f)
                chronometer.text = "00:00:00"
            }
            isTraining = false
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()

        unbindCS()
    }

    // Updates the training UI with the training stats
    override fun updateUI(steps: Int, distance: Float, calories: Float) {
        activity?.runOnUiThread {
            tv_totalSteps.text = steps.toString()
            tv_totalDistance.text = distance.roundToInt().toString()
            tv_totalCalories.text = calories.roundToInt().toString()
        }
    }

    override fun getTrainingTime(): String{
        return trainingTime
    }

    // Unbinds this CaloriesService if not bounded
    private fun unbindCS(){
        if (isServiceBounded) {
            context?.unbindService(connection)
            isServiceBounded = false
        }
    }

    private fun logTrainingFlowEvent(firebaseAnalytics: FirebaseAnalytics, playPause: String) {
        val params = Bundle()
        params.putString("ButtonPlayPause", playPause)
        firebaseAnalytics.logEvent("ButtonPlayPause", params)
    }

    private fun logTrainingStartStopEvent(firebaseAnalytics: FirebaseAnalytics, startStop: String) {
        val params = Bundle()
        params.putString("ButtonStartStop", startStop)
        firebaseAnalytics.logEvent("ButtonStartStop", params)
    }
}