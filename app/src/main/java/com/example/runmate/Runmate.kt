package com.example.runmate

import com.example.runmate.utils.*
import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics

/*
Application class instantiated when the App starts.
It instantiates an analytics object that tracks the device type
 */
class Runmate : Application() {

    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate() {
        super.onCreate()

        analytics = FirebaseAnalytics.getInstance(this)
        trackDevice(resources, analytics)
    }
}