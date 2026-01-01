package com.example.conversationgenerator.util

data class ConversationLine(
    val speaker: String,
    val originalText: String,
    val translationText: String? = null
)

data class ParsedConversation(
    val title: String,
    val lines: List<ConversationLine>
)

object ConversationParser {
    fun parse(rawText: String): ParsedConversation {
        val lines = rawText.lines()
        var title = ""
        val conversationLines = mutableListOf<ConversationLine>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            // Extract title (lines starting with ** or #)
            if (line.startsWith("**") && line.endsWith("**")) {
                title = line.removeSurrounding("**").trim()
                i++
                continue
            }
            if (line.startsWith("#")) {
                title = line.removePrefix("#").trim()
                i++
                continue
            }

            // Skip empty lines
            if (line.isEmpty()) {
                i++
                continue
            }

            // Parse speaker lines (format: "Speaker A: text" or "A: text")
            if (line.contains(":")) {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val speaker = parts[0].trim()
                    val originalText = parts[1].trim()

                    // Check if next line is a translation
                    var translationText: String? = null
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1].trim()
                        if (nextLine.startsWith("[TRANSLATION]:") || nextLine.startsWith("(") && nextLine.contains("translation")) {
                            translationText = nextLine
                                .removePrefix("[TRANSLATION]:")
                                .removePrefix("(")
                                .removeSuffix(")")
                                .trim()
                            // Remove "translation:" prefix if exists
                            if (translationText.lowercase().startsWith("translation:")) {
                                translationText = translationText.substringAfter(":").trim()
                            }
                            i++ // Skip the translation line
                        }
                    }

                    conversationLines.add(
                        ConversationLine(
                            speaker = speaker,
                            originalText = originalText,
                            translationText = translationText
                        )
                    )
                }
            }

            i++
        }

        return ParsedConversation(
            title = title.ifEmpty { "Conversation" },
            lines = conversationLines
        )
    }

    fun formatForCopy(parsed: ParsedConversation): String {
        val builder = StringBuilder()
        builder.append("**${parsed.title}**\n\n")

        for (line in parsed.lines) {
            builder.append("${line.speaker}: ${line.originalText}\n")
            if (line.translationText != null) {
                builder.append("(${line.translationText})\n")
            }
            builder.append("\n")
        }

        return builder.toString().trim()
    }
}
