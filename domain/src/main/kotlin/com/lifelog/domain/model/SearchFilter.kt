package com.lifelog.domain.model

enum class LogCategory {
    ALL,
    TIMELINE,
    APPS,
    NOTIFICATIONS,
    CALLS,
    LOCATION,
    BATTERY,
    SCREEN,
}

data class SearchFilter(
    val keyword: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val packageName: String? = null,
    val category: LogCategory = LogCategory.ALL,
)
