package com.tim.hubitatdash.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Custom Gson deserializer for DeviceState.
 *
 * Handles both Hubitat API response formats for the `attributes` field:
 *  - Flat JSON object: {"switch":"on","level":75}
 *  - Array of attribute objects: [{"name":"switch","value":"on","currentValue":"on"}]
 *
 * Also coerces numeric `id` values to strings.
 */
class DeviceStateDeserializer : JsonDeserializer<DeviceState> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        ctx: JsonDeserializationContext
    ): DeviceState {
        val obj = json.asJsonObject

        val id = obj.get("id")?.asString ?: ""
        val label = obj.get("label")?.asString ?: ""
        val type = obj.get("type")?.asString ?: ""
        val commands = obj.get("commands")?.asJsonArray
            ?.mapNotNull { if (it.isJsonPrimitive) it.asString else null }

        val attributes: Map<String, String> = when {
            obj.has("attributes") -> {
                val attrsElem = obj.get("attributes")
                when {
                    attrsElem.isJsonObject -> {
                        // Flat object: {"switch": "on", "level": 75}
                        // Some values may be arrays (e.g. "supportedButtonValues": ["pushed","held"])
                        // or objects — coerce them to strings safely.
                        attrsElem.asJsonObject.entrySet().associate { (k, v) ->
                            k to when {
                                v.isJsonNull -> ""
                                v.isJsonPrimitive -> v.asString
                                v.isJsonArray -> v.asJsonArray.joinToString(",") { elem ->
                                    if (elem.isJsonNull) "" else if (elem.isJsonPrimitive) elem.asString else elem.toString()
                                }
                                else -> v.toString()
                            }
                        }
                    }
                    attrsElem.isJsonArray -> {
                        // Array: [{"name":"switch","value":"on","currentValue":"on"}]
                        attrsElem.asJsonArray.filter { it.isJsonObject }.mapNotNull { elem ->
                            val o = elem.asJsonObject
                            val name = o.get("name")?.asString?.takeIf { it.isNotEmpty() }
                                ?: return@mapNotNull null
                            val rawValue = o.get("currentValue") ?: o.get("value")
                            val value = if (rawValue == null || rawValue.isJsonNull) "" else rawValue.asString
                            name to value
                        }.toMap()
                    }
                    else -> emptyMap()
                }
            }
            else -> emptyMap()
        }

        return DeviceState(
            id = id,
            label = label,
            type = type,
            attributes = attributes,
            commands = commands
        )
    }
}

