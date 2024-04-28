package com.shashankmunda.stickershub

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import java.util.ArrayList

object Utils {
    inline fun<reified T:Parcelable> Intent.getParcelableExtraCompat(key: String, type: Class<T>): T?{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            getParcelableExtra(key,type)
        else getParcelableExtra(key)
    }

    inline fun<reified T:Parcelable> Intent.getParcelableArrayListExtraCompat(key: String, type: Class<T>): ArrayList<T>?{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            getParcelableArrayListExtra(key,type)
        else getParcelableArrayListExtra(key)
    }
}