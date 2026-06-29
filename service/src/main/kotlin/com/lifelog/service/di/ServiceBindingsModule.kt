package com.lifelog.service.di

import com.lifelog.domain.sync.SmsSyncTrigger
import com.lifelog.service.sms.SmsSyncTriggerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceBindingsModule {
    @Binds
    @Singleton
    abstract fun bindSmsSyncTrigger(impl: SmsSyncTriggerImpl): SmsSyncTrigger
}
