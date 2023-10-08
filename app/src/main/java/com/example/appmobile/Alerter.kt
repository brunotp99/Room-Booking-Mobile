package com.example.appmobile

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import com.tapadoo.alerter.Alerter

public fun alertaDanger(activity: Activity, title: String, message: String){
    Alerter.create(activity)
        .setTitle(title)
        .setText(message)
        .setIcon(R.drawable.ic_error)
        .setIconColorFilter(0)
        .setBackgroundColorRes(R.color.danger_bg)
        .setDuration(3000)
        .show()
}

public fun alertaSuccess(activity: Activity, title: String, message: String){
    Alerter.create(activity)
        .setTitle(title)
        .setText(message)
        .setIcon(R.drawable.ic_check)
        .setIconColorFilter(0)
        .setBackgroundColorRes(R.color.default_bg)
        .setDuration(3000)
        .show()
}

public fun alertaDangerFragment(activity: FragmentActivity, title: String, message: String){
    Alerter.create(activity)
        .setTitle(title)
        .setText(message)
        .setIcon(R.drawable.ic_error)
        .setIconColorFilter(0)
        .setBackgroundColorRes(R.color.danger_bg)
        .setDuration(3000)
        .show()
}

public fun alertaSuccessFragment(activity: FragmentActivity, title: String, message: String){
    Alerter.create(activity)
        .setTitle(title)
        .setText(message)
        .setIcon(R.drawable.ic_check)
        .setIconColorFilter(0)
        .setBackgroundColorRes(R.color.default_bg)
        .setDuration(3000)
        .show()
}