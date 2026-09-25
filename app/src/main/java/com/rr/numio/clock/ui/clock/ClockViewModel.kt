package com.rr.numio.clock.ui.clock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rr.numio.clock.data.WorldCity
import com.rr.numio.clock.data.WorldCityStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClockViewModel(app: Application) : AndroidViewModel(app) {

    val selectedCities: StateFlow<List<WorldCity>> =
        WorldCityStore.getCities(app)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun addCity(city: WorldCity) {
        viewModelScope.launch {
            val current = selectedCities.value
            if (current.none { it.name == city.name }) {
                WorldCityStore.saveCities(getApplication(), current + city)
            }
        }
    }

    fun removeCity(city: WorldCity) {
        viewModelScope.launch {
            WorldCityStore.saveCities(
                getApplication(),
                selectedCities.value.filter { it != city }
            )
        }
    }

    fun reorderCities(cities: List<WorldCity>) {
        viewModelScope.launch {
            WorldCityStore.saveCities(getApplication(), cities)
        }
    }
}