package com.jnj.vaccinetracker.config

enum class MockFlavor {
    OFFLINE_PERF_TEST, ONLINE_PERF_TEST, P2P_PERF_TEST
}

data class MockAppSettings(
    val flavor: MockFlavor,
    val useFakeApiDataSource: Boolean,
    val useFakeSyncApiDataSource: Boolean,
    val useFakeNetworkConnectivity: Boolean,
    val showGenerateTemplatesView: Boolean,
    val showGenerateFatParticipantsView: Boolean,
    val showP2pTransferTime: Boolean,
) {

    companion object {
        val OFFLINE_PERFORMANCE_TEST = MockAppSettings(
            MockFlavor.OFFLINE_PERF_TEST,
            useFakeApiDataSource = true,
            useFakeSyncApiDataSource = true,
            showGenerateFatParticipantsView = false,
            useFakeNetworkConnectivity = true,
            showGenerateTemplatesView = false,
            showP2pTransferTime = false
        )
        val ONLINE_PERFORMANCE_TEST = MockAppSettings(
            MockFlavor.ONLINE_PERF_TEST,
            useFakeApiDataSource = false,
            useFakeSyncApiDataSource = false,
            showGenerateFatParticipantsView = false,
            useFakeNetworkConnectivity = false,
            showGenerateTemplatesView = false,
            showP2pTransferTime = false
        )
        val P2P_PERFORMANCE_TEST = MockAppSettings(
            MockFlavor.P2P_PERF_TEST,
            useFakeApiDataSource = false,
            useFakeSyncApiDataSource = false,
            showGenerateFatParticipantsView = true,
            useFakeNetworkConnectivity = false,
            showGenerateTemplatesView = true,
            showP2pTransferTime = true
        )
    }

    val showParticipantGenerationCountView = useFakeSyncApiDataSource
}

val mockAppSettings: MockAppSettings = MockAppSettings.P2P_PERFORMANCE_TEST