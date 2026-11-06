package com.example.pccomponent.domain.calculator

import kotlin.math.ceil

object PowerCalculator {
    fun recommendedPower(totalPowerWatts: Int): Int {
        val required = ceil(totalPowerWatts * 1.3).toInt()
        return listOf(450, 500, 550, 600, 650, 750, 850, 1000, 1200)
            .firstOrNull { it >= required } ?: required
    }
}
