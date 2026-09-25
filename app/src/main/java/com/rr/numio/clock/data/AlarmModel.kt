package com.rr.numio.clock.data

import kotlinx.serialization.Serializable

@Serializable
data class AlarmModel(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val days: List<Boolean>,
    val isEnabled: Boolean
)