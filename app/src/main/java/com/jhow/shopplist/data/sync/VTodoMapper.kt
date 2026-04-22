package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.domain.model.ShoppingItem
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class VTodoPayload(
    val uid: String,
    val body: String
)

class VTodoMapper @Inject constructor() {
    fun toVTodo(item: ShoppingItem): VTodoPayload {
        val uid = item.remoteMetadata.remoteUid ?: UUID.randomUUID().toString()
        val completedLines = if (item.isPurchased) {
            val completedAt = formatUtc(item.updatedAt)
            listOf("STATUS:COMPLETED", "COMPLETED:$completedAt")
        } else {
            listOf("STATUS:NEEDS-ACTION")
        }

        val body = buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//JhowShoppList//EN")
            appendLine("BEGIN:VTODO")
            appendLine("UID:$uid")
            appendLine("SUMMARY:${escapeText(item.name)}")
            appendLine("LAST-MODIFIED:${formatUtc(item.updatedAt)}")
            completedLines.forEach(::appendLine)
            appendLine("END:VTODO")
            appendLine("END:VCALENDAR")
        }

        return VTodoPayload(uid = uid, body = body)
    }

    fun toRemoteSnapshot(vtodoBody: String, href: String, eTag: String?): RemoteShoppingItemSnapshot {
        val values = vtodoBody.lineSequence()
            .map { it.trim() }
            .filter { ':' in it }
            .associate { line ->
                val colonIndex = line.indexOf(':')
                val key = line.substring(0, colonIndex).substringBefore(';')
                key to line.substring(colonIndex + 1)
            }

        val rawUid = values["UID"] ?: throw IllegalArgumentException(
            "VTODO is missing required UID property (href=$href)"
        )

        return RemoteShoppingItemSnapshot(
            remoteUid = rawUid,
            summary = unescapeText(values["SUMMARY"].orEmpty()),
            isCompleted = values["STATUS"] == "COMPLETED",
            href = href,
            eTag = eTag,
            lastModifiedAt = values["LAST-MODIFIED"]?.let { parseUtcSafe(it) }
        )
    }

    private fun formatUtc(epochMillis: Long): String =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochMilli(epochMillis))

    private fun parseUtc(value: String): Long =
        Instant.from(
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC)
                .parse(value)
        ).toEpochMilli()

    private fun parseUtcSafe(value: String): Long? =
        try {
            parseUtc(value)
        } catch (_: Exception) {
            null
        }

    private fun escapeText(value: String): String =
        value.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")

    private fun unescapeText(value: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < value.length) {
            if (value[i] == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    'n' -> sb.append('\n')
                    '\\' -> sb.append('\\')
                    ';' -> sb.append(';')
                    ',' -> sb.append(',')
                    else -> sb.append(value[i + 1])
                }
                i += 2
            } else {
                sb.append(value[i])
                i++
            }
        }
        return sb.toString()
    }
}
