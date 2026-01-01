package com.example.conversationgenerator.util

data class ConversationLine(
    val speaker: String,
    val speakerTranslation: String? = null,
    val originalText: String,
    val translationText: String? = null
)

data class ParsedConversation(
    val title: String,
    val titleTranslation: String? = null,
    val lines: List<ConversationLine>
)

object ConversationParser {
    fun parse(rawText: String): ParsedConversation {
        val lines = rawText.lines()
        var title = ""
        var titleTranslation: String? = null
        val conversationLines = mutableListOf<ConversationLine>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            // Extract title (lines starting with ** or #)
            if (line.startsWith("**") && line.endsWith("**")) {
                title = line.removeSurrounding("**").trim()

                // Check if next line is title translation
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    if (nextLine.startsWith("[TITLE_TRANSLATION]:")) {
                        titleTranslation = nextLine.removePrefix("[TITLE_TRANSLATION]:").trim()
                        i++ // Skip the translation line
                    }
                }
                i++
                continue
            }
            if (line.startsWith("#")) {
                title = line.removePrefix("#").trim()

                // Check if next line is title translation
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    if (nextLine.startsWith("[TITLE_TRANSLATION]:")) {
                        titleTranslation = nextLine.removePrefix("[TITLE_TRANSLATION]:").trim()
                        i++ // Skip the translation line
                    }
                }
                i++
                continue
            }

            // Skip empty lines
            if (line.isEmpty()) {
                i++
                continue
            }

            // Parse speaker lines (format: "Speaker A: text" or "A: text")
            if (line.contains(":") && !line.startsWith("[")) {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val speaker = parts[0].trim()
                    val originalText = parts[1].trim()

                    // Check for speaker translation and text translation
                    var speakerTranslation: String? = null
                    var translationText: String? = null
                    var linesToSkip = 0

                    // Check next lines for translations
                    var checkIndex = i + 1
                    while (checkIndex < lines.size && checkIndex <= i + 3) {
                        val nextLine = lines[checkIndex].trim()

                        if (nextLine.isEmpty()) {
                            checkIndex++
                            continue
                        }

                        if (nextLine.startsWith("[SPEAKER_TRANSLATION]:")) {
                            speakerTranslation = nextLine.removePrefix("[SPEAKER_TRANSLATION]:").trim()
                            linesToSkip++
                            checkIndex++
                        } else if (nextLine.startsWith("[TRANSLATION]:")) {
                            translationText = nextLine.removePrefix("[TRANSLATION]:").trim()
                            linesToSkip++
                            checkIndex++
                        } else if (nextLine.startsWith("(") && nextLine.contains("translation")) {
                            translationText = nextLine
                                .removePrefix("(")
                                .removeSuffix(")")
                                .trim()
                            // Remove "translation:" prefix if exists
                            if (translationText.lowercase().startsWith("translation:")) {
                                translationText = translationText.substringAfter(":").trim()
                            }
                            linesToSkip++
                            checkIndex++
                        } else {
                            // Not a translation line, stop checking
                            break
                        }
                    }

                    conversationLines.add(
                        ConversationLine(
                            speaker = speaker,
                            speakerTranslation = speakerTranslation,
                            originalText = originalText,
                            translationText = translationText
                        )
                    )

                    // Skip the translation lines we've processed
                    i += linesToSkip
                }
            }

            i++
        }

        return ParsedConversation(
            title = title.ifEmpty { "Conversation" },
            titleTranslation = titleTranslation,
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
