package com.example.pccomponent.data.repository

import android.content.Context
import com.example.pccomponent.data.model.Component
import com.example.pccomponent.data.model.ComponentCatalog
import com.example.pccomponent.data.model.ComponentCategory
import com.example.pccomponent.performance.PerformanceTrace
import kotlinx.serialization.json.Json

class ComponentRepository(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private var cachedCatalog: ComponentCatalog? = null
    private var cachedById: Map<String, Component>? = null
    private var cachedByCategory: Map<ComponentCategory, List<Component>>? = null
    private var cachedBySocket: Map<String, List<Component>>? = null
    private var cachedCheapestByCategory: Map<ComponentCategory, Component>? = null
    private var cachedByPrice: Map<ComponentCategory, List<Component>>? = null
    private var cachedByPerformance: Map<ComponentCategory, List<Component>>? = null

    fun getCatalog(): ComponentCatalog = PerformanceTrace.measure("ComponentRepository.getCatalog") {
        cachedCatalog ?: synchronized(this) {
            cachedCatalog ?: context.assets.open(CATALOG_FILE).bufferedReader().use { reader ->
                json.decodeFromString<ComponentCatalog>(reader.readText()).also {
                    val components = it.components
                    cachedCatalog = it
                    cachedById = components.associateBy(Component::id)
                    cachedByCategory = components.groupBy(Component::category)
                    cachedBySocket = components
                        .flatMap { component ->
                            (component.specifications.socket?.let(::listOf).orEmpty() +
                                component.specifications.supportedSockets)
                                .distinct()
                                .map { socket -> socket to component }
                        }
                        .groupBy({ it.first }, { it.second })
                    cachedCheapestByCategory = components
                        .groupBy(Component::category)
                        .mapValues { (_, values) -> values.minBy(Component::price) }
                    cachedByPrice = components
                        .groupBy(Component::category)
                        .mapValues { (_, values) -> values.sortedBy(Component::price) }
                    cachedByPerformance = components
                        .groupBy(Component::category)
                        .mapValues { (_, values) -> values.sortedWith(
                            compareByDescending<Component> { it.performanceLevel }
                                .thenBy(Component::price)
                        ) }
                }
            }
        }
    }

    fun getAll(): List<Component> = getCatalog().components

    fun getByCategory(category: ComponentCategory): List<Component> =
        getCatalog().let { cachedByCategory?.get(category).orEmpty() }

    fun getById(id: String): Component? {
        getCatalog()
        return cachedById?.get(id)
    }

    fun getBySocket(socket: String): List<Component> {
        getCatalog()
        return cachedBySocket?.get(socket).orEmpty()
    }

    fun getCheapest(category: ComponentCategory): Component? {
        getCatalog()
        return cachedCheapestByCategory?.get(category)
    }

    fun getByPrice(category: ComponentCategory): List<Component> {
        getCatalog()
        return cachedByPrice?.get(category).orEmpty()
    }

    fun getByPerformance(category: ComponentCategory): List<Component> {
        getCatalog()
        return cachedByPerformance?.get(category).orEmpty()
    }

    private companion object {
        const val CATALOG_FILE = "catalog/components.json"
    }
}
