package com.jnj.vaccinetracker.visit.screens

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import javax.inject.Inject

class ContradictionsViewModel @Inject constructor(
   override val dispatchers: AppCoroutineDispatchers
) : ViewModelBase() {
   val errorMessage = mutableLiveData<String>()
}