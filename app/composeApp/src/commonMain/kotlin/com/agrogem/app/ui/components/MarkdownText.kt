package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.theme.AgroGemColors

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AgroGemColors.TextPrimary,
) {
    val blocks = parseMarkdownBlocks(text)

    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        color = color,
                        fontSize = when (block.level) {
                            1 -> 18.sp
                            2 -> 16.sp
                            3 -> 14.sp
                            else -> 13.sp
                        },
                        lineHeight = when (block.level) {
                            1 -> 24.sp
                            2 -> 22.sp
                            3 -> 20.sp
                            else -> 18.sp
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        color = color,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }

                is MarkdownBlock.Bullet -> {
                    Text(
                        text = buildAnnotatedString {
                            append("• ")
                            append(parseInlineMarkdown(block.text))
                        },
                        color = color,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }

                is MarkdownBlock.Code -> {
                    Text(
                        text = block.text,
                        color = AgroGemColors.TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AgroGemColors.PillTrackSemi, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            if (index != blocks.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

internal sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Code(val text: String) : MarkdownBlock
}

internal fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return listOf(MarkdownBlock.Paragraph(""))

    val result = mutableListOf<MarkdownBlock>()
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val paragraph = mutableListOf<String>()
    var insideCode = false
    val codeLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            result += MarkdownBlock.Paragraph(paragraph.joinToString(" ").trim())
            paragraph.clear()
        }
    }

    fun flushCode() {
        if (codeLines.isNotEmpty()) {
            result += MarkdownBlock.Code(codeLines.joinToString("\n"))
            codeLines.clear()
        }
    }

    lines.forEach { rawLine ->
        val line = rawLine.trimEnd()
        if (line.trimStart().startsWith("```")) {
            flushParagraph()
            if (insideCode) {
                flushCode()
                insideCode = false
            } else {
                insideCode = true
            }
            return@forEach
        }

        if (insideCode) {
            codeLines += line
            return@forEach
        }

        val trimmed = line.trim()
        when {
            trimmed.isBlank() -> flushParagraph()
            trimmed.startsWith("#") -> {
                val markerLength = trimmed.takeWhile { it == '#' }.length
                val hasSpaceAfterMarker = trimmed.length > markerLength && trimmed[markerLength] == ' '
                if (markerLength in 1..6 && hasSpaceAfterMarker) {
                    flushParagraph()
                    result += MarkdownBlock.Heading(level = markerLength, text = trimmed.drop(markerLength).trim())
                } else {
                    paragraph += trimmed
                }
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                result += MarkdownBlock.Bullet(trimmed.drop(2).trim())
            }
            else -> paragraph += trimmed
        }
    }

    flushParagraph()
    if (insideCode) flushCode()

    return if (result.isEmpty()) listOf(MarkdownBlock.Paragraph(markdown)) else result
}

internal fun parseInlineMarkdown(text: String): AnnotatedString {
    val result = AnnotatedString.Builder()
    var index = 0

    fun markerAt(position: Int, marker: String): Boolean =
        position + marker.length <= text.length && text.substring(position, position + marker.length) == marker

    while (index < text.length) {
        when {
            markerAt(index, "**") -> {
                val end = text.indexOf("**", startIndex = index + 2)
                if (end > index + 2) {
                    result.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    result.append(text.substring(index + 2, end))
                    result.pop()
                    index = end + 2
                } else {
                    result.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    result.append(text.substring(index + 2))
                    result.pop()
                    break
                }
            }

            markerAt(index, "`") -> {
                val end = text.indexOf("`", startIndex = index + 1)
                if (end > index + 1) {
                    result.pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = AgroGemColors.PillTrackSemi,
                        ),
                    )
                    result.append(text.substring(index + 1, end))
                    result.pop()
                    index = end + 1
                } else {
                    result.append(text[index])
                    index += 1
                }
            }

            markerAt(index, "*") -> {
                val end = text.indexOf("*", startIndex = index + 1)
                if (end > index + 1) {
                    result.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    result.append(text.substring(index + 1, end))
                    result.pop()
                    index = end + 1
                } else {
                    result.append(text[index])
                    index += 1
                }
            }

            else -> {
                result.append(text[index])
                index += 1
            }
        }
    }

    return result.toAnnotatedString()
}
