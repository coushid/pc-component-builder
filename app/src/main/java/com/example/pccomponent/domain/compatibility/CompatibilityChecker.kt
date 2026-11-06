package com.example.pccomponent.domain.compatibility

import com.example.pccomponent.data.model.Component
import com.example.pccomponent.data.model.ComponentCategory
import com.example.pccomponent.data.model.PcConfiguration
import com.example.pccomponent.domain.calculator.PowerCalculator
import com.example.pccomponent.performance.PerformanceTrace

enum class CompatibilityStatus { COMPATIBLE, WARNINGS, INCOMPATIBLE }

data class CompatibilityResult(
    val status: CompatibilityStatus,
    val messages: List<String>,
    val incompatibilityMessages: List<String> = emptyList()
)

object CompatibilityChecker {
    fun check(configuration: PcConfiguration): CompatibilityResult =
        PerformanceTrace.measure("CompatibilityChecker.check") {
        val byCategory = configuration.components.groupBy { it.category }
        val messages = mutableListOf<String>()
        val incompatibilityMessages = mutableListOf<String>()
        fun addIncompatibility(message: String) {
            messages += message
            incompatibilityMessages += message
        }
        val cpu = byCategory[ComponentCategory.CPU].orEmpty().firstOrNull()
        val motherboard = byCategory[ComponentCategory.MOTHERBOARD].orEmpty().firstOrNull()
        val ram = byCategory[ComponentCategory.RAM].orEmpty().firstOrNull()
        val powerSupply = byCategory[ComponentCategory.POWER_SUPPLY].orEmpty().firstOrNull()
        val pcCase = byCategory[ComponentCategory.PC_CASE].orEmpty().firstOrNull()
        val gpu = byCategory[ComponentCategory.GPU].orEmpty().firstOrNull()
        val cooler = byCategory[ComponentCategory.COOLER].orEmpty().firstOrNull()

        if (cpu != null && motherboard != null &&
            cpu.specifications.socket != motherboard.specifications.socket
        ) addIncompatibility(
            "Процессор «${cpu.name}» и материнская плата «${motherboard.name}»: сокеты не совпадают."
        )
        if (motherboard != null && ram != null &&
            motherboard.specifications.memoryType != ram.specifications.memoryType
        ) addIncompatibility(
            "Материнская плата «${motherboard.name}» и оперативная память «${ram.name}»: тип памяти не поддерживается."
        )
        if (motherboard != null && ram != null &&
            motherboard.specifications.maxMemoryCapacityGb != null &&
            ram.specifications.memoryCapacityGb != null &&
            ram.specifications.memoryCapacityGb > motherboard.specifications.maxMemoryCapacityGb
        ) addIncompatibility(
            "Материнская плата «${motherboard.name}» поддерживает до " +
                "${motherboard.specifications.maxMemoryCapacityGb} ГБ ОЗУ, а выбран комплект на " +
                "${ram.specifications.memoryCapacityGb} ГБ."
        )
        if (powerSupply != null &&
            powerSupply.specifications.powerWatts.orZero() <
            PowerCalculator.recommendedPower(configuration.totalPowerWatts)
        ) addIncompatibility(
            "Блок питания «${powerSupply.name}»: мощности недостаточно для этой сборки. " +
                "Рекомендуется не менее ${PowerCalculator.recommendedPower(configuration.totalPowerWatts)} Вт."
        )
        if (pcCase != null && gpu != null &&
            gpu.specifications.lengthMm.orZero() > pcCase.specifications.maxGpuLengthMm.orZero()
        ) addIncompatibility(
            "Видеокарта «${gpu.name}» и корпус «${pcCase.name}»: видеокарта не помещается."
        )
        if (pcCase != null && motherboard != null &&
            !formFactorFits(motherboard.specifications.formFactor, pcCase.specifications.formFactor)
        ) addIncompatibility(
            "Материнская плата «${motherboard.name}» формата ${motherboard.specifications.formFactor} " +
                "не помещается в корпус «${pcCase.name}» формата ${pcCase.specifications.formFactor}."
        )
        if (cooler != null && cpu != null &&
            cooler.specifications.supportedSockets.isNotEmpty() &&
            cpu.specifications.socket !in cooler.specifications.supportedSockets
        ) addIncompatibility(
            "Охлаждение «${cooler.name}» и процессор «${cpu.name}»: сокет процессора не поддерживается."
        )
        if (pcCase != null && cooler != null &&
            cooler.specifications.heightMm.orZero() > 0 &&
            cooler.specifications.heightMm.orZero() > pcCase.specifications.maxCoolerHeightMm.orZero()
        ) addIncompatibility(
            "Кулер «${cooler.name}» высотой ${cooler.specifications.heightMm} мм выше допустимых " +
                "${pcCase.specifications.maxCoolerHeightMm} мм корпуса «${pcCase.name}»."
        )
        if (powerSupply != null && gpu != null &&
            !hasPowerConnector(
                powerSupply.specifications.gpuPowerConnectors,
                gpu.specifications.requiredGpuPowerConnectors
            )
        ) addIncompatibility(
            "Блок питания «${powerSupply.name}» не имеет подходящего разъёма питания для «${gpu.name}»."
        )
        if (configuration.totalPrice > configuration.budget) {
            messages += "Стоимость сборки ${configuration.totalPrice} руб. превышает бюджет ${configuration.budget} руб."
        }

        return CompatibilityResult(
            status = when {
                incompatibilityMessages.isNotEmpty() -> CompatibilityStatus.INCOMPATIBLE
                messages.isNotEmpty() -> CompatibilityStatus.WARNINGS
                else -> CompatibilityStatus.COMPATIBLE
            },
            messages = messages,
            incompatibilityMessages = incompatibilityMessages
        )
        }

    /**
     * Checks whether a candidate introduces a new hard incompatibility.
     *
     * Existing problems in other component categories are intentionally ignored:
     * the user is replacing only one category and should still be able to see
     * candidates that do not make that existing problem worse.
     */
    fun isCandidateCompatible(candidate: Component, selected: List<Component>): Boolean {
        val others = selected.filterNot { it.category == candidate.category }
        val baseResult = check(PcConfiguration(others, "Проверка совместимости", Int.MAX_VALUE))
        val candidateResult = check(
            PcConfiguration(others + candidate, "Проверка совместимости", Int.MAX_VALUE)
        )
        val existingProblems = baseResult.incompatibilityMessages.toSet()
        return candidateResult.incompatibilityMessages.all { it in existingProblems }
    }

    private fun Int?.orZero(): Int = this ?: 0

    private fun formFactorFits(board: String?, caseFormat: String?): Boolean {
        val sizes = mapOf("ITX" to 1, "MINI-ITX" to 1, "MATX" to 2, "M-ATX" to 2, "ATX" to 3, "E-ATX" to 4)
        val boardSize = board?.uppercase()?.let(sizes::get) ?: return false
        val caseSize = caseFormat?.uppercase()?.let(sizes::get) ?: return false
        return boardSize <= caseSize
    }

    /**
     * Connector names are deliberately checked strictly.  A 12VHPWR/12V-2x6
     * cable is not silently treated as an 8-pin PCIe cable (and vice versa):
     * the recommendation engine must replace the PSU or the GPU instead of
     * returning a build that requires an unlisted adapter.
     */
    fun hasPowerConnector(available: String?, required: String?): Boolean {
        if (available.isNullOrBlank() || required.isNullOrBlank()) return true
        val availableNormalized = available.lowercase().replace(" ", "")
        val requiredNormalized = required.lowercase().replace(" ", "")
        if (requiredNormalized.contains("12v-2x6")) {
            return availableNormalized.contains("12v-2x6") ||
                availableNormalized.contains("12vhpwr")
        }
        if (requiredNormalized.contains("12vhpwr")) {
            return availableNormalized.contains("12vhpwr") ||
                availableNormalized.contains("12v-2x6")
        }
        if (requiredNormalized.contains("8-pin")) {
            return availableNormalized.contains("8-pin")
        }
        return availableNormalized.contains(requiredNormalized)
    }
}
