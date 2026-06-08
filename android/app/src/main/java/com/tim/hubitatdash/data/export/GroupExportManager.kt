package com.tim.hubitatdash.data.export

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.tim.hubitatdash.data.model.CustomGroupData
import com.tim.hubitatdash.data.model.MultiTileConfig
import com.tim.hubitatdash.data.model.TileType
import com.tim.hubitatdash.data.repository.GroupRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Canonical JSON format for group config export/import.
 * Keys and TileType values match the web app's Zustand persisted store so
 * JSON files are exchangeable between web and Android without any conversion tool.
 */
data class GroupExportData(
    val version: Int = 2,
    @SerializedName("customGroups")    val customGroups: List<ExportCustomGroup> = emptyList(),
    @SerializedName("groupAdditions")  val groupAdditions: Map<String, List<String>> = emptyMap(),
    @SerializedName("groupExclusions") val groupExclusions: Map<String, List<String>> = emptyMap(),
    @SerializedName("groupOrder")      val groupOrder: List<String> = emptyList(),
    @SerializedName("childGroupOrder") val childGroupOrder: Map<String, List<String>> = emptyMap(),
    // v2: groupId → (deviceId → typeString)
    @SerializedName("tileTypeOverrides") val tileTypeOverrides: Map<String, Map<String, String>> = emptyMap(),
    @SerializedName("tileOrder")       val tileOrder: Map<String, List<String>> = emptyMap(),
    // Preserved opaque so multi-device tile configs survive web→Android→web round-trips.
    // Android does not render these tiles but stores and re-exports the config unchanged.
    @SerializedName("multiTileConfigs") val multiTileConfigs: Map<String, MultiTileConfig> = emptyMap(),
    /** Per-group tile display title overrides: groupId → deviceId/syntheticId → title string. */
    @SerializedName("tileTitleOverrides") val tileTitleOverrides: Map<String, Map<String, String>> = emptyMap()
)

/** Used only when parsing v1 export files where overrides were deviceId → typeString */
private data class GroupExportDataV1(
    val version: Int = 1,
    @SerializedName("customGroups")    val customGroups: List<ExportCustomGroup> = emptyList(),
    @SerializedName("groupAdditions")  val groupAdditions: Map<String, List<String>> = emptyMap(),
    @SerializedName("groupExclusions") val groupExclusions: Map<String, List<String>> = emptyMap(),
    @SerializedName("groupOrder")      val groupOrder: List<String> = emptyList(),
    @SerializedName("childGroupOrder") val childGroupOrder: Map<String, List<String>> = emptyMap(),
    @SerializedName("tileTypeOverrides") val tileTypeOverrides: Map<String, String> = emptyMap(),
    @SerializedName("tileOrder")       val tileOrder: Map<String, List<String>> = emptyMap()
)

data class ExportCustomGroup(
    @SerializedName("id")          val id: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("iconName")    val iconName: String,
    @SerializedName("parentId")    val parentId: String? = null
)

@Singleton
class GroupExportManager @Inject constructor(
    private val groupRepository: GroupRepository
) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    // ------------------------------------------------------------------
    // TileType mapping: Android UPPER_SNAKE ↔ web lowercase-kebab
    // ------------------------------------------------------------------

    private val toWebFormat = mapOf(
        TileType.SWITCH         to "switch",
        TileType.DIMMER         to "dimmer",
        TileType.RGBW           to "rgbw",
        TileType.CONTACT        to "contact",
        TileType.MOTION         to "motion",
        TileType.TEMPERATURE    to "temperature",
        TileType.POWER_METER    to "power-meter",
        TileType.BUTTON         to "button",
        TileType.LOCK           to "lock",
        TileType.CONNECTOR      to "connector",
        TileType.HUB_VARIABLE   to "hub-variable",
        TileType.HSM            to "hsm",
        TileType.MODE           to "mode",
        TileType.RING_DETECTION to "ring-detection",
        TileType.PRESENCE       to "presence",
        TileType.BATTERY        to "battery",
        TileType.SUN_TIMES      to "sun-times",
        TileType.MULTI_DEVICE   to "multi-device"
    )

    private val fromWebFormat: Map<String, TileType> = toWebFormat.entries
        .associate { (k, v) -> v to k }

    /** TileType → "power-meter" */
    fun toExportString(type: TileType): String = toWebFormat[type] ?: type.name.lowercase()

    /** "power-meter" → POWER_METER; falls back to uppercase matching for unknowns */
    fun fromExportString(value: String): TileType? =
        fromWebFormat[value] ?: runCatching { TileType.valueOf(value.uppercase()) }.getOrNull()

    // ------------------------------------------------------------------
    // Tile-order system IDs: "MODE","HSM" ↔ "mode","hsm"
    // ------------------------------------------------------------------

    private fun tileOrderIdToExport(id: String): String =
        TileType.entries.find { it.name == id }?.let { toExportString(it) } ?: id

    private fun tileOrderIdFromExport(id: String): String =
        fromWebFormat[id]?.name ?: id

    // ------------------------------------------------------------------
    // Export
    // ------------------------------------------------------------------

    fun buildExportJson(): String {
        val customGroups = groupRepository.customGroupsRaw.map {
            ExportCustomGroup(id = it.id, displayName = it.displayName,
                iconName = it.iconName, parentId = it.parentId)
        }
        // v2 format: groupId → deviceId → typeString
        val tileTypeOverrides: Map<String, Map<String, String>> = groupRepository.tileTypeOverridesRaw
            .mapValues { (_, deviceMap) -> deviceMap.mapValues { (_, v) -> toExportString(v) } }
        val tileOrder = groupRepository.tileOrderRaw
            .mapValues { (_, ids) -> ids.map { tileOrderIdToExport(it) } }

        val data = GroupExportData(
            customGroups      = customGroups,
            groupAdditions    = groupRepository.groupAdditionsRaw,
            groupExclusions   = groupRepository.groupExclusionsRaw,
            groupOrder        = groupRepository.groupOrderRaw,
            childGroupOrder   = groupRepository.childGroupOrderRaw,
            tileTypeOverrides = tileTypeOverrides,
            tileOrder         = tileOrder,
            multiTileConfigs  = groupRepository.multiTileConfigsRaw,
            tileTitleOverrides = groupRepository.tileTitleOverridesRaw
        )
        return gson.toJson(data)
    }

    // ------------------------------------------------------------------
    // Import
    // ------------------------------------------------------------------

    fun parseImportJson(json: String): Result<GroupExportData> = runCatching {
        // Peek at version to decide how to parse tileTypeOverrides
        val versionProbe = runCatching {
            gson.fromJson(json, GroupExportData::class.java)?.version ?: 1
        }.getOrDefault(1)

        if (versionProbe >= 2) {
            // v2: tileTypeOverrides is groupId → deviceId → typeString
            val raw = gson.fromJson(json, GroupExportData::class.java)
                ?: error("Empty or null JSON")
            val safeOverrides = raw.tileTypeOverrides.mapValues { (_, deviceMap) ->
                deviceMap.filter { (_, v) -> fromExportString(v) != null }
            }
            val safeTileOrder = raw.tileOrder.mapValues { (_, ids) ->
                ids.map { tileOrderIdFromExport(tileOrderIdToExport(it)) }
            }
            raw.copy(tileTypeOverrides = safeOverrides, tileOrder = safeTileOrder)
        } else {
            // v1: tileTypeOverrides was deviceId → typeString (global)
            // Expand to per-group by applying to every group that contains each device
            val rawV1 = gson.fromJson(json, GroupExportDataV1::class.java)
                ?: error("Empty or null JSON")
            val oldOverrides = rawV1.tileTypeOverrides.filter { (_, v) -> fromExportString(v) != null }

            // Build per-group overrides from the v1 global map
            val perGroup = mutableMapOf<String, MutableMap<String, String>>()
            fun applyToGroup(groupId: String, deviceIds: Iterable<String>) {
                for (deviceId in deviceIds) {
                    val typeStr = oldOverrides[deviceId] ?: continue
                    perGroup.getOrPut(groupId) { mutableMapOf() }[deviceId] = typeStr
                }
            }
            for (staticGroup in com.tim.hubitatdash.data.model.groups) {
                val staticDeviceIds = staticGroup.tiles.mapNotNull { it.deviceId }
                applyToGroup(staticGroup.id, staticDeviceIds + (rawV1.groupAdditions[staticGroup.id] ?: emptyList()))
            }
            for (customGroup in rawV1.customGroups) {
                applyToGroup(customGroup.id, rawV1.groupAdditions[customGroup.id] ?: emptyList())
            }

            val safeTileOrder = rawV1.tileOrder.mapValues { (_, ids) ->
                ids.map { tileOrderIdFromExport(tileOrderIdToExport(it)) }
            }
            GroupExportData(
                version         = 2,
                customGroups    = rawV1.customGroups,
                groupAdditions  = rawV1.groupAdditions,
                groupExclusions = rawV1.groupExclusions,
                groupOrder      = rawV1.groupOrder,
                childGroupOrder = rawV1.childGroupOrder,
                tileTypeOverrides = perGroup,
                tileOrder       = safeTileOrder
            )
        }
    }

    fun importConfig(data: GroupExportData) {
        val androidCustomGroups = data.customGroups.map {
            CustomGroupData(id = it.id, displayName = it.displayName,
                iconName = it.iconName, parentId = it.parentId)
        }
        // v2 format: groupId → deviceId → TileType
        val androidTileTypeOverrides: Map<String, Map<String, TileType>> =
            data.tileTypeOverrides.mapValues { (_, deviceMap) ->
                deviceMap.mapNotNull { (deviceId, v) ->
                    fromExportString(v)?.let { deviceId to it }
                }.toMap()
            }
        val androidTileOrder = data.tileOrder.mapValues { (_, ids) ->
            ids.map { tileOrderIdFromExport(it) }
        }
        groupRepository.replaceAll(
            customGroups      = androidCustomGroups,
            groupAdditions    = data.groupAdditions,
            groupExclusions   = data.groupExclusions,
            groupOrder        = data.groupOrder,
            childGroupOrder   = data.childGroupOrder,
            tileTypeOverrides = androidTileTypeOverrides,
            tileOrder         = androidTileOrder,
            multiTileConfigs  = data.multiTileConfigs,
            tileTitleOverrides = data.tileTitleOverrides
        )
    }
}

