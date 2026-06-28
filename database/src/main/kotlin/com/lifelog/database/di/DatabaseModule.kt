package com.lifelog.database.di

import android.content.Context
import androidx.room.Room
import com.lifelog.database.LifeLogDatabase
import com.lifelog.database.dao.AppUsageDao
import com.lifelog.database.dao.BatteryLogDao
import com.lifelog.database.dao.CallLogDao
import com.lifelog.database.dao.LocationLogDao
import com.lifelog.database.dao.NotificationLogDao
import com.lifelog.database.dao.ScreenEventDao
import com.lifelog.database.dao.TimelineEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LifeLogDatabase {
        return Room.databaseBuilder(
            context,
            LifeLogDatabase::class.java,
            LifeLogDatabase.DATABASE_NAME,
        ).build()
    }

    @Provides fun provideTimelineEventDao(db: LifeLogDatabase): TimelineEventDao = db.timelineEventDao()

    @Provides fun provideAppUsageDao(db: LifeLogDatabase): AppUsageDao = db.appUsageDao()

    @Provides fun provideNotificationLogDao(db: LifeLogDatabase): NotificationLogDao = db.notificationLogDao()

    @Provides fun provideCallLogDao(db: LifeLogDatabase): CallLogDao = db.callLogDao()

    @Provides fun provideLocationLogDao(db: LifeLogDatabase): LocationLogDao = db.locationLogDao()

    @Provides fun provideBatteryLogDao(db: LifeLogDatabase): BatteryLogDao = db.batteryLogDao()

    @Provides fun provideScreenEventDao(db: LifeLogDatabase): ScreenEventDao = db.screenEventDao()
}
