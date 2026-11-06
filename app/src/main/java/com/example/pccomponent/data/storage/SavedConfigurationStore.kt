package com.example.pccomponent.data.storage

import android.content.Context
import com.example.pccomponent.data.model.Component
import com.example.pccomponent.data.model.PcConfiguration

/** In-memory cache and persistence boundary for saved PC configurations. */
class SavedConfigurationStore(
    context: Context,
    catalog: List<Component>
) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val catalogById = catalog.associateBy(Component::id)
    private var cachedConfigurations: List<PcConfiguration> = readInitial()

    fun getAll(): List<PcConfiguration> = cachedConfigurations

    /** Replaces the cached list and persists only when its stable content changed. */
    fun saveAll(configurations: List<PcConfiguration>): Boolean {
        val normalized = configurations
            .filter { it.components.isNotEmpty() }
            .distinctBy(::signature)
        if (normalized == cachedConfigurations) return false
        cachedConfigurations = normalized
        val serialized = normalized.map(::serialize)
        persist(normalized)
        return true
    }

    private fun readInitial(): List<PcConfiguration> {
        val ordered = preferences.getString(SAVED_BUILDS_ORDERED, null)
            ?.split(RECORD_SEPARATOR)
            ?.mapNotNull(::deserialize)
            .orEmpty()
        if (ordered.isNotEmpty()) return ordered

        val stored = preferences.getStringSet(SAVED_BUILDS, emptySet()).orEmpty()
        if (stored.isNotEmpty()) {
            return stored.asSequence()
                .mapNotNull(::deserialize)
                .sortedWith(compareBy<PcConfiguration>({ signature(it) }))
                .toList()
        }

        // Migration from the original three-key format.
        val ids = preferences.getStringSet(SAVED_BUILD_IDS, null)
        if (ids.isNullOrEmpty()) return emptyList()
        val migrated = PcConfiguration(
            components = ids.mapNotNull(catalogById::get),
            purpose = preferences.getString(SAVED_BUILD_PURPOSE, DEFAULT_PURPOSE) ?: DEFAULT_PURPOSE,
            budget = preferences.getInt(SAVED_BUILD_BUDGET, 0)
        )
        if (migrated.components.isEmpty()) return emptyList()
        // Persist the migrated value in the new stable format once.
        saveAll(listOf(migrated))
        return listOf(migrated)
    }

    private fun persist(configurations: List<PcConfiguration>) {
        val serialized = configurations.map(::serialize)
        preferences.edit()
            // Keep the historical set key for compatibility with existing installs.
            .putStringSet(SAVED_BUILDS, serialized.toSet())
            // A string preserves the order that SharedPreferences StringSet cannot preserve.
            .putString(SAVED_BUILDS_ORDERED, serialized.joinToString(RECORD_SEPARATOR))
            .apply()
    }

    private fun serialize(configuration: PcConfiguration): String =
        "${configuration.purpose.replace("|", "/")}|${configuration.budget}|${configuration.components.joinToString(",") { it.id }}"

    private fun deserialize(value: String): PcConfiguration? {
        val parts = value.split("|", limit = 3)
        if (parts.size != 3) return null
        val components = parts[2].split(",").mapNotNull(catalogById::get)
        if (components.isEmpty()) return null
        return PcConfiguration(components, parts[0], parts[1].toIntOrNull() ?: components.sumOf { it.price })
    }

    private fun signature(configuration: PcConfiguration): String =
        "${configuration.purpose}|${configuration.budget}|${configuration.components.map { it.id }.sorted().joinToString(",")}"

    private companion object {
        const val PREFS_NAME = "pccomponent_saved_build"
        const val SAVED_BUILDS = "builds"
        const val SAVED_BUILDS_ORDERED = "builds_ordered"
        const val SAVED_BUILD_IDS = "component_ids"
        const val SAVED_BUILD_PURPOSE = "purpose"
        const val SAVED_BUILD_BUDGET = "budget"
        const val DEFAULT_PURPOSE = "Сборка"
        const val RECORD_SEPARATOR = "\n"
    }
}

