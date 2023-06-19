package com.example.runmate

// Interface implemented by TrainingFragment and accessed by CaloriesService
interface TrainingFragmentCallback {
    fun updateUI(steps: Int, distance: Float, calories: Float)
    fun getTrainingTime(): String
}