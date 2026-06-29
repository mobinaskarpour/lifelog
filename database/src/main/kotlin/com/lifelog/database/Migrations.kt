package com.lifelog.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notification_logs ADD COLUMN text TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE notification_logs ADD COLUMN subtext TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE notification_logs ADD COLUMN bigText TEXT")
            db.execSQL("ALTER TABLE notification_logs ADD COLUMN notificationId INTEGER NOT NULL DEFAULT -1")
            db.execSQL("ALTER TABLE notification_logs ADD COLUMN conversationName TEXT")
            db.execSQL("ALTER TABLE notification_logs ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE notification_logs SET notificationId = id WHERE notificationId = -1")
            db.execSQL("UPDATE notification_logs SET updatedAt = timestamp WHERE updatedAt = 0")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_notification_logs_package_notification " +
                    "ON notification_logs(packageName, notificationId)",
            )
            db.execSQL("ALTER TABLE app_usage ADD COLUMN lastOpen INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE app_usage SET lastOpen = firstOpen WHERE lastOpen = 0")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_app_usage_package_date " +
                    "ON app_usage(packageName, date)",
            )
        }
    }

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sms_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    providerId INTEGER NOT NULL,
                    threadId INTEGER NOT NULL,
                    address TEXT NOT NULL,
                    contactName TEXT,
                    body TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    dateSent INTEGER NOT NULL,
                    type INTEGER NOT NULL,
                    read INTEGER NOT NULL,
                    seen INTEGER NOT NULL,
                    status INTEGER NOT NULL,
                    subscriptionId INTEGER NOT NULL,
                    serviceCenter TEXT,
                    person INTEGER
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_sms_logs_providerId ON sms_logs(providerId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_sms_logs_threadId ON sms_logs(threadId)",
            )
        }
    }
