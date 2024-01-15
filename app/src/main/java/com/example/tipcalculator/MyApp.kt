package com.example.tipcalculator

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        val sharedPrefernceManger = SharedPrefernceManger(this)
        AppCompatDelegate.setDefaultNightMode(sharedPrefernceManger.themeFlag[sharedPrefernceManger.theme])
    }
}