package com.dutch.deadreckoning

import android.app.Application

class DeadReckoningApp: Application() {

    companion object {
        private const val LOG_TAG = "_Dutch"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

}