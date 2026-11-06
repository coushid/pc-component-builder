package com.example.pccomponent.domain.chatbot

enum class ChatBotStep {
    PURPOSE, BUDGET, PERFORMANCE, UPGRADE, RESULT
}

data class ChatBotState(
    val step: ChatBotStep = ChatBotStep.PURPOSE,
    val messages: List<ChatMessage> = emptyList(),
    val purpose: String? = null,
    val budget: Int? = null,
    val performanceLevel: Int? = null,
    val upgradeRequired: Boolean? = null
)

data class ChatMessage(
    val text: String,
    val isBot: Boolean
)
