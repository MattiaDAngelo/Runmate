package com.example.runmate

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// Class of the fragment of the Statistic section
class StatsFragment:Fragment(R.layout.fragment_stats), TrainingItemClickListener {
    private lateinit var pb_steps: ProgressBar
    private lateinit var pb_distance: ProgressBar
    private lateinit var pb_calories: ProgressBar
    private lateinit var tv_steps_progress: TextView
    private lateinit var tv_distance_progress: TextView
    private lateinit var tv_calories_progress: TextView
    private lateinit var rv_activities: RecyclerView
    private lateinit var analytics: FirebaseAnalytics

    private lateinit var sharedPref: SharedPreferences

    private lateinit var trainingList: MutableList<TrainingObject>

    // default goals
    private var stepsGoal = 10000
    private var distanceGoal = 8000
    private var caloriesGoal = 1000

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        analytics = FirebaseAnalytics.getInstance(requireContext())

        trackFragment(analytics)

        pb_steps = view.findViewById(R.id.pb_steps_foreground)
        pb_distance = view.findViewById(R.id.pb_distance_foreground)
        pb_calories = view.findViewById(R.id.pb_calories_foreground)
        tv_steps_progress = view.findViewById(R.id.tv_steps_stats)
        tv_distance_progress = view.findViewById(R.id.tv_distance_stats)
        tv_calories_progress = view.findViewById(R.id.tv_calories_stats)
        rv_activities = view.findViewById(R.id.rv_activities)

        trainingList = mutableListOf()

        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        sharedPref = requireContext().getSharedPreferences("${uid}UserPrefs", Context.MODE_PRIVATE)

        stepsGoal = sharedPref.getInt("Steps", 10000)
        distanceGoal = sharedPref.getInt("Meters", 8000)
        caloriesGoal = sharedPref.getInt("Calories", 1000)

        // take past trainings (if any)
        val json = sharedPref.getString("trainingList", null)
        if (json != null) {
            val gson = Gson()
            val listType = object : TypeToken<MutableList<TrainingObject>>() {}.type
            trainingList = gson.fromJson(json, listType)
        }

        // pass training list to show trainings in the UI
        rv_activities.layoutManager = LinearLayoutManager(requireContext())
        rv_activities.adapter = TrainingAdapter(trainingList, this)

        // update text views and circular progress bars
        updateStatsUI(sharedPref.getInt("totalSteps", 0), sharedPref.getFloat("totalDistance", 0f), sharedPref.getFloat("totalCalories", 0f))

        return view
    }

    // Shows an alert when the user tries to delete a training and deletes the training if the user confirm
    override fun deleteTrainingItem(position: Int) {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle("Elimina attività")
        alertDialogBuilder.setMessage("Vuoi davvero eliminare questa attività?")
        alertDialogBuilder.setPositiveButton("Elimina") { dialog, _ ->
            sharedPref.edit().apply {
                val temp = trainingList.removeAt(position)
                if (temp.date == LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) {

                    // circular progress bars and local stats must be updated
                    val newSteps = sharedPref.getInt("totalSteps", 0) - temp.steps
                    val newDistance = sharedPref.getFloat("totalDistance", 0f) - temp.distance
                    val newCalories = sharedPref.getFloat("totalCalories", 0f) - temp.calories
                    updateStatsUI(newSteps, newDistance, newCalories)

                    putInt("totalSteps", newSteps)
                    putFloat("totalDistance", newDistance)
                    putFloat("totalCalories", newCalories)
                }
                rv_activities.adapter?.notifyItemRemoved(position)
                rv_activities.adapter?.notifyItemRangeChanged(position, trainingList.size)

                // save training list after deletion
                putString("trainingList", Gson().toJson(trainingList))
                apply()
            }
            dialog.dismiss()
            Toast.makeText(context, "Attività eliminata", Toast.LENGTH_SHORT).show()
        }
        alertDialogBuilder.setNegativeButton("Indietro") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    // Updates text views with locally saved stats and colors circular progress bars
    private fun updateStatsUI(steps: Int, distance: Float, calories: Float) {
        pb_steps.post { pb_steps.progress = (steps.toFloat() / stepsGoal.toFloat() * 100).toInt() }
        pb_distance.post { pb_distance.progress = (distance / distanceGoal.toFloat() * 100).toInt() }
        pb_calories.post { pb_calories.progress = (calories / caloriesGoal.toFloat() * 100).toInt() }
        tv_steps_progress.text = "$steps / $stepsGoal passi"
        tv_distance_progress.text = "${distance.roundToInt()} / $distanceGoal m"
        tv_calories_progress.text = "${calories.roundToInt()} / $caloriesGoal kcal"

        updateTVColor(tv_steps_progress, R.color.steps)
        updateTVColor(tv_distance_progress, R.color.distance)
        updateTVColor(tv_calories_progress, R.color.calories)
    }

    // Changes the color of part of the stats text views
    private fun updateTVColor(tv: TextView, color: Int) {
        var c = color
        val txt = tv.text
        if (txt.substring(0, txt.indexOf("/")) == "0 ") c = R.color.default_stats
        val spannableString = SpannableString(txt)
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), c)), 0, txt.indexOf("/"), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        tv.text = spannableString
    }

    private fun trackFragment(analytics : FirebaseAnalytics) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Statistics Fragment")
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "StatsFragment")
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}