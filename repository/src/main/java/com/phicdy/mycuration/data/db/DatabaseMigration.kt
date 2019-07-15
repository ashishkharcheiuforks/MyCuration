package com.phicdy.mycuration.data.db

import android.database.sqlite.SQLiteDatabase

import java.util.ArrayList

class DatabaseMigration(private val oldVersion: Int, newVersion: Int) {

    private val tasks = ArrayList<DatabaseMigrationTask>()

    init {
        if (oldVersion < newVersion) {
            if (oldVersion < DATABASE_VERSION_ADD_FILTER_FEED_REGISTRATION) {
                tasks.add(AddFilterFeedRegistrationTask())
            }
            if (oldVersion < DATABASE_VERSION_FETCH_ICON) {
                tasks.add(ResetIconPathTask())
            }
        }
    }

    fun migrate(db: SQLiteDatabase) {
        for (task in tasks) {
            task.execute(db, oldVersion)
        }
    }

    companion object {
        const val FIRST_VERSION = 1
        const val DATABASE_VERSION_ADD_ENABLED_TO_FILTER = 2
        const val DATABASE_VERSION_ADD_FILTER_FEED_REGISTRATION = 3
        const val DATABASE_VERSION_FETCH_ICON = 4
    }
}
