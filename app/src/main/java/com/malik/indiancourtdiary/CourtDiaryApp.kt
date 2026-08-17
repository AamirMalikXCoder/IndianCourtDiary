package com.malik.indiancourtdiary

import android.app.Application
import com.malik.indiancourtdiary.data.CourtDatabase

class CourtDiaryApp : Application() {
    val database by lazy { CourtDatabase.create(this) }

    override fun onCreate() {
        super.onCreate()
        if (AppPreferences.autoSync(this)) DailyCaseSyncScheduler.schedule(this)
    }
}
