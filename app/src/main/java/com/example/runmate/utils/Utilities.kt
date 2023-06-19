package com.example.runmate.utils

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth


/*
check if there is a user currently logged in
@param {FirebaseAuth} mAuth authentication object (singleton)
@return true if a user is logged, false otherwise
 */
fun checkCurrentUser(mAuth: FirebaseAuth): Boolean {
    val currentUser = mAuth.currentUser
    return if (currentUser != null) {
        currentUser.reload()
        true
    }
    else false
}

/*
Takes some data stored in Shared preferences and uses them to create the user properties.
This is used to create users' segmentation in Google Analytics.
Called when the user sets the profile in TargetActivity
 */
fun setUserProperties(context: Context, analytics: FirebaseAnalytics){

    val uid = FirebaseAuth.getInstance().currentUser!!.uid
    val tPref = context.getSharedPreferences("${uid}UserPrefs", Context.MODE_PRIVATE)
    val weight = tPref.getInt("Weight", 0)
    val height = tPref.getInt("Height", 0)
    val gender = tPref.getString("Gender", "Male")

    analytics.setUserProperty("Weight", weight.toString())
    analytics.setUserProperty("Height", height.toString())
    analytics.setUserProperty("Gender", gender.toString())
}

/*
Called by the Application class. Tracks device type, OS and language.
 */
fun trackDevice(resources: Resources, analytics : FirebaseAnalytics) {
    val deviceModel = Build.MODEL
    val osVersion = Build.VERSION.RELEASE
    val language = resources.configuration.locales[0].language

    val bundle = Bundle()
    bundle.putString("Device_Model", deviceModel)
    bundle.putString("OS_Version", osVersion)
    bundle.putString("Language", language)

    analytics.logEvent("Device_Properties", bundle)
}