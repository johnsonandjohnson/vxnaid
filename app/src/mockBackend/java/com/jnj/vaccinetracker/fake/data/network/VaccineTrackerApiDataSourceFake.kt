package com.jnj.vaccinetracker.fake.data.network

import com.jnj.vaccinetracker.common.data.network.VaccineTrackerApiDataSource
import com.jnj.vaccinetracker.common.data.network.VaccineTrackerApiDataSourceDefault
import javax.inject.Inject

class VaccineTrackerApiDataSourceFake @Inject constructor(
    private val fakeBackendApi: FakeBackendApi,
    private val vaccineTrackerApiDataSourceDefault: VaccineTrackerApiDataSourceDefault,
) : VaccineTrackerApiDataSource by vaccineTrackerApiDataSourceDefault