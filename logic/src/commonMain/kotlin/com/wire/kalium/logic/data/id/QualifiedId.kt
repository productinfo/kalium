package com.wire.kalium.logic.data.id

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class QualifiedID(
    @SerialName("id")
    val value: String,
    @SerialName("domain")
    val domain: String
) {
    companion object {
        const val WIRE_PRODUCTION_DOMAIN = "wire.com"
    }

    override fun toString(): String = if (domain.isEmpty()) value else "$value$VALUE_DOMAIN_SEPARATOR$domain"

    fun toPlainID(): PlainId = PlainId(value)

}

const val VALUE_DOMAIN_SEPARATOR = '@'
val FEDERATION_REGEX = """[^@.]+@[^@.]+\.[^@]+""".toRegex()

typealias ConversationId = QualifiedID

@JvmInline
value class GroupID(val value: String)
