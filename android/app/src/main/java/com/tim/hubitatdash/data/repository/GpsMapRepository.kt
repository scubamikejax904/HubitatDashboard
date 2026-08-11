package com.tim.hubitatdash.data.repository

import com.tim.hubitatdash.data.model.GpsDataPoint
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpsMapRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun fetchPoints(
        startDate: LocalDate?,
        endDate: LocalDate?,
        csvUrlOverride: String? = null
    ): List<GpsDataPoint> {
        val csvUrl = csvUrlOverride?.takeIf { it.isNotBlank() } ?: DEFAULT_CSV_URL
        val request = Request.Builder().url(csvUrl).get().build()
        val csvText = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("CSV fetch failed: HTTP ${response.code}")
            }
            response.body?.string() ?: ""
        }

        val rawPoints = parseCsv(csvText)
        val filtered = filterByDate(rawPoints, startDate, endDate)
        return filtered.sortedBy { parseTimestampMillis(it.timestamp) ?: Long.MAX_VALUE }
    }

    private fun filterByDate(
        points: List<GpsDataPoint>,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<GpsDataPoint> {
        if (startDate == null && endDate == null) return points

        val zone = ZoneId.systemDefault()
        val startMs = startDate?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: Long.MIN_VALUE
        val endMs = endDate?.plusDays(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()?.minus(1)
            ?: Long.MAX_VALUE

        return points.filter { point ->
            val ts = parseTimestampMillis(point.timestamp) ?: return@filter false
            ts in startMs..endMs
        }
    }

    private fun parseCsv(csv: String): List<GpsDataPoint> {
        val lines = csv.split(Regex("\\r?\\n")).filter { it.trim().isNotEmpty() }
        if (lines.size < 2) return emptyList()

        val headers = parseCsvRow(lines.first()).map { it.lowercase().trim() }
        val tsIdx = headers.indexOfFirst { it.contains("timestamp") || it == "time" || it == "date" || it == "datetime" }
        val latIdx = headers.indexOfFirst { it == "lat" || it == "latitude" }
        val lngIdx = headers.indexOfFirst { it == "long" || it == "lng" || it == "longitude" || it == "lon" }
        val devIdx = headers.indexOfFirst { it == "device" || it == "phone" || it == "name" }

        if (tsIdx == -1 || latIdx == -1 || lngIdx == -1) {
            throw IllegalStateException(
                "Could not find required columns (timestamp, lat, long). Headers: ${headers.joinToString(", ")}"
            )
        }

        return lines.drop(1).mapNotNull { line ->
            val cells = parseCsvRow(line)
            val neededIndex = maxOf(tsIdx, latIdx, lngIdx)
            if (cells.size <= neededIndex) return@mapNotNull null

            val lat = cells[latIdx].toDoubleOrNull() ?: return@mapNotNull null
            val lng = cells[lngIdx].toDoubleOrNull() ?: return@mapNotNull null
            val rawTimestamp = cells[tsIdx].trim()
            if (rawTimestamp.isBlank()) return@mapNotNull null

            val isoTimestamp = normalizeTimestamp(rawTimestamp) ?: return@mapNotNull null
            val device = if (devIdx >= 0 && devIdx < cells.size) cells[devIdx].trim().ifBlank { null } else null

            GpsDataPoint(
                timestamp = isoTimestamp,
                lat = lat,
                long = lng,
                device = device
            )
        }
    }

    private fun normalizeTimestamp(raw: String): String? {
        val numeric = raw.toDoubleOrNull()
        if (numeric != null && raw.matches(NUMERIC_REGEX)) {
            val epochMillis = ((numeric - GOOGLE_SHEETS_EPOCH_OFFSET_DAYS) * MILLIS_PER_DAY).toLong()
            return Instant.ofEpochMilli(epochMillis).toString()
        }

        return runCatching { Instant.parse(raw).toString() }.getOrElse {
            parseTimestampMillis(raw)?.let { ts -> Instant.ofEpochMilli(ts).toString() }
        }
    }

    private fun parseTimestampMillis(value: String): Long? {
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrElse {
            runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrElse {
                runCatching { ZonedDateTime.parse(value).toInstant().toEpochMilli() }.getOrElse {
                    val zone = ZoneId.systemDefault()
                    TIMESTAMP_FORMATTERS.firstNotNullOfOrNull { formatter ->
                        runCatching {
                            LocalDateTime.parse(value, formatter).atZone(zone).toInstant().toEpochMilli()
                        }.getOrNull()
                    }
                }
            }
        }
    }

    private fun parseCsvRow(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    current.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        cells += current.toString()
                        current.clear()
                    }
                    else -> current.append(ch)
                }
            }
            i++
        }
        cells += current.toString()
        return cells
    }

    companion object {
        private const val DEFAULT_CSV_URL =
            "https://docs.google.com/spreadsheets/d/1EGNmf9XvinmTE5EGZUOBjaoeU1HIvBLHlz33TN0KfcU/export?format=csv&gid=0"
        private const val MILLIS_PER_DAY = 86_400_000.0
        private const val GOOGLE_SHEETS_EPOCH_OFFSET_DAYS = 25569.0
        private val NUMERIC_REGEX = Regex("^\\d+(\\.\\d+)?$")
        private val TIMESTAMP_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )
    }
}
