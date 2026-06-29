package com.lifelog.feature.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelog.domain.model.SmsAccessStatus
import com.lifelog.domain.model.SmsMessage
import com.lifelog.domain.model.SmsSyncStats
import com.lifelog.domain.model.SmsThread
import com.lifelog.domain.repository.SmsRepository
import com.lifelog.domain.sync.SmsSyncTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmsUiState(
    val threads: List<SmsThread> = emptyList(),
    val stats: SmsSyncStats = SmsSyncStats(),
    val accessStatus: SmsAccessStatus = SmsAccessStatus.GRANTED,
    val accessMessage: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class SmsViewModel
    @Inject
    constructor(
        private val smsRepository: SmsRepository,
        private val smsSyncTrigger: SmsSyncTrigger,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SmsUiState())
        val uiState: StateFlow<SmsUiState> = _uiState.asStateFlow()
        private var loadJob: Job? = null

        init {
            viewModelScope.launch { smsSyncTrigger.syncFromProvider() }
            loadThreads()
        }

        fun refresh() {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            viewModelScope.launch {
                smsSyncTrigger.syncFromProvider()
            }
            loadThreads()
        }

        fun onSearchQueryChange(query: String) {
            _uiState.value = _uiState.value.copy(searchQuery = query)
            loadThreads()
        }

        private fun loadThreads() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    val query = _uiState.value.searchQuery.trim()
                    val threadsFlow =
                        if (query.isBlank()) {
                            smsRepository.getAllThreads()
                        } else {
                            smsRepository.searchMessages(query)
                                .map { matches -> buildThreadsFromMessages(matches) }
                                .catch { emit(emptyList()) }
                        }

                    combine(threadsFlow, smsRepository.getSyncStats()) { threads, stats ->
                        SmsUiState(
                            threads = threads,
                            stats = stats,
                            accessStatus = deriveAccessStatus(stats),
                            accessMessage = buildAccessMessage(stats),
                            searchQuery = query,
                            isLoading = false,
                            isRefreshing = false,
                        )
                    }.catch {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                            )
                    }.collect { state ->
                        _uiState.value = state
                    }
                }
        }

        private fun buildThreadsFromMessages(messages: List<SmsMessage>): List<SmsThread> =
            messages
                .groupBy { it.threadId }
                .mapNotNull { (threadId, threadMessages) ->
                    val latest = threadMessages.maxByOrNull { it.date } ?: return@mapNotNull null
                    SmsThread(
                        threadId = threadId,
                        address = latest.address,
                        contactName = threadMessages.firstNotNullOfOrNull { it.contactName },
                        lastMessage = latest.body,
                        lastDate = latest.date,
                        messageCount = threadMessages.size,
                    )
                }.sortedByDescending { it.lastDate }

        private fun deriveAccessStatus(stats: SmsSyncStats): SmsAccessStatus =
            when {
                stats.providerSentCount > 0 && stats.sentCount == 0 -> SmsAccessStatus.PROVIDER_RESTRICTED
                else -> SmsAccessStatus.GRANTED
            }

        private fun buildAccessMessage(stats: SmsSyncStats): String? {
            if (stats.providerSentCount > 0 && stats.sentCount == 0) {
                return "This device has ${stats.providerSentCount} sent SMS in the provider, but none were imported. " +
                    "Grant READ_SMS permission or check Android SMS access restrictions."
            }
            if (stats.providerInboxCount == 0 && stats.providerSentCount == 0 &&
                stats.inboxCount == 0 && stats.sentCount == 0
            ) {
                return "No SMS found. Grant SMS permission in Settings to import inbox and sent messages."
            }
            return null
        }
    }
