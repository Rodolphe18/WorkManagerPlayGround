package com.francotte.workmanagerplayground

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OverwritingInputMerger
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.setInputMerger
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val workManager: WorkManager) : ViewModel() {

    data class UiState(
        val status: UiStatus = UiStatus.Idle,
        val imagePath: String? = null,
        val showRedOverlay: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var downloadObsJob: Job? = null
    private var filterObsJob: Job? = null

    private val autoResetAfterMs = 5000L

    fun startChainFromUrl(url: String) = enqueue(workDataOf(Keys.KEY_IMAGE_URL to url))
    fun startChainFromUri(uri: Uri) = enqueue(workDataOf(Keys.KEY_IMAGE_URL to uri.toString()))

    private fun enqueue(downloadInput: Data) {
        val download = OneTimeWorkRequestBuilder<DownloadImageWorker>()
            .setInputData(downloadInput)
            .build()

        val filter = OneTimeWorkRequestBuilder<RedFilterWorker>()
            .setInputMerger(OverwritingInputMerger::class)
            .build()

        // Unique work pour éviter les doublons si l’utilisateur partage plusieurs fois
        workManager.beginUniqueWork(
            "download_filter_chain",
            ExistingWorkPolicy.REPLACE,
            download
        ).then(filter).enqueue()


        downloadObsJob?.cancel()
        filterObsJob?.cancel()

        observe(download.id, filter.id)

        _uiState.update {
            it.copy(
                status = UiStatus.Downloading,
                imagePath = null,
                showRedOverlay = false
            )
        }
    }

    private fun observe(downloadId: UUID, filterId: UUID) {
        // Download observer
       downloadObsJob = viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(downloadId)
                .asFlow()
                .collect { info ->
                    when (info?.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val path = info.outputData.getString(Keys.KEY_DOWNLOADED_PATH)
                            if (path != null) {
                                _uiState.update {
                                    it.copy(status = UiStatus.Downloaded, imagePath = path)
                                }
                            }
                        }
                        WorkInfo.State.FAILED -> _uiState.update { it.copy(status = UiStatus.Error) }
                        WorkInfo.State.RUNNING -> _uiState.update { it.copy(status = UiStatus.Downloading) }
                        else -> Unit
                    }
                }
        }

        // Filter observer
      filterObsJob = viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(filterId)
                .asFlow()
                .collect { info ->
                    when (info?.state) {
                        WorkInfo.State.RUNNING ->
                            _uiState.update { it.copy(status = UiStatus.Filtering, showRedOverlay = true) }

                        WorkInfo.State.SUCCEEDED -> {
                            val path = info.outputData.getString(Keys.KEY_FILTERED_PATH)
                                _uiState.update {
                                    it.copy(
                                        status = UiStatus.Filtered,
                                        imagePath = path ?: it.imagePath,
                                        showRedOverlay = false
                                    )
                            }
                            viewModelScope.launch {
                                delay(autoResetAfterMs)
                                _uiState.update { st ->
                                    st.copy(status = UiStatus.Idle)
                                }
                            }

                        }

                        WorkInfo.State.FAILED ->
                            _uiState.update { it.copy(status = UiStatus.Error, showRedOverlay = false) }

                        else -> Unit
                    }
                }
        }
    }
}
