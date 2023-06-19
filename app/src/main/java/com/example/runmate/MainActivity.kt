package com.example.runmate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private val bundle = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_nav)

        val permissions = mutableListOf<String>()

        // check permissions to use the step sensor
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if(permissions.isNotEmpty())
            requestPermissions(permissions.toTypedArray(), 1)

        // get current user id and remove daily stats if this is a new day
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val sharedPref = getSharedPreferences("${uid}UserPrefs", Context.MODE_PRIVATE)
        if (sharedPref != null) {
            val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            if (sharedPref.getString("currentDate", "") != currentDate){ // the stats are reset each day (apart from training list)
                val editor = sharedPref.edit()
                editor.remove("totalSteps")
                editor.remove("totalDistance")
                editor.remove("totalCalories")
                editor.apply()
            }
        }

        val statsFragment = StatsFragment()
        val trainingChoiceFragment = TrainingChoiceFragment()
        val userFragment = UserFragment()

        val username = sharedPref.getString("username", "")
        bundle.putString("USERNAME", username)
        userFragment.arguments = bundle

        setCurrentFragment(statsFragment)

        // set navigation of app sections
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->

            // get instance of TrainingFragment (if any) and handle user navigation between fragments
            val tfInstance = supportFragmentManager.findFragmentByTag("TrainingFragment") as? TrainingFragment
            when (item.itemId) {
                R.id.stats -> {
                    if (tfInstance?.getIsTraining() == true) { // the user is training => don't change current fragment
                        bottomNavigationView.post {
                            bottomNavigationView.selectedItemId = R.id.training
                        }
                    }
                    else setCurrentFragment(statsFragment)
                }
                R.id.training -> {
                    if (tfInstance?.getIsTraining() == true) { // the user is training => don't change current fragment
                        showAlert()
                    }
                    else setCurrentFragment(trainingChoiceFragment)
                }
                R.id.user -> {
                    if (tfInstance?.getIsTraining() == true) { // the user is training => don't change current fragment
                        bottomNavigationView.post {
                            bottomNavigationView.selectedItemId = R.id.training
                        }
                    }
                    else setCurrentFragment(userFragment)
                }
            }
            true
        }

        // overrides default back button behavior when the user is in TrainingFragment
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.findFragmentById(R.id.flFragment) is TrainingFragment) { // the user is training => don't change current fragment
                    val tfInstance = supportFragmentManager.findFragmentByTag("TrainingFragment") as TrainingFragment
                    if (tfInstance.getIsTraining())
                        showAlert()
                    else setCurrentFragment(trainingChoiceFragment)
                }
                else finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            commit()
        }

    // Shows an alert to inform the user
    private fun showAlert() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Attenzione!")
        alertDialogBuilder.setMessage("Stai registrando un'attività:\npremi il pulsante di stop per fermarla e permetterne il salvataggio.")
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}