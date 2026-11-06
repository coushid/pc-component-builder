package com.example.pccomponent.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ComponentCategory {
    CPU, MOTHERBOARD, GPU, RAM, STORAGE, POWER_SUPPLY, PC_CASE, COOLER
}

@Serializable
data class Component(
    val id: String,
    val category: ComponentCategory,
    val name: String,
    val brand: String,
    val price: Int,
    val performanceLevel: Int,
    val powerConsumptionW: Int,
    val description: String,
    val tags: List<String> = emptyList(),
    val specifications: Specifications = Specifications()
)

@Serializable
data class Specifications(
    val socket: String? = null,
    val chipset: String? = null,
    val formFactor: String? = null,
    val memoryType: String? = null,
    val memoryCapacityGb: Int? = null,
    val memoryFrequencyMhz: Int? = null,
    val cores: Int? = null,
    val threads: Int? = null,
    val videoMemoryGb: Int? = null,
    val lengthMm: Int? = null,
    val interfaceType: String? = null,
    val capacityGb: Int? = null,
    val powerWatts: Int? = null,
    val certificate: String? = null,
    val supportedSockets: List<String> = emptyList(),
    val maxTdpWatts: Int? = null,
    val maxGpuLengthMm: Int? = null,
    val maxCoolerHeightMm: Int? = null,
    val maxMemoryCapacityGb: Int? = null,
    val m2Slots: Int? = null,
    val pcieGeneration: String? = null,
    val requiredGpuPowerConnectors: String? = null,
    val gpuPowerConnectors: String? = null,
    val heightMm: Int? = null
)

@Serializable
data class ComponentCatalog(
    val version: Int = 1,
    val source: String = "Учебные фиксированные данные",
    val components: List<Component>
)

data class PcConfiguration(
    val components: List<Component>,
    val purpose: String,
    val budget: Int
) {
    val totalPrice: Int get() = components.sumOf { it.price }
    val totalPowerWatts: Int get() = components.sumOf { it.powerConsumptionW }
}


