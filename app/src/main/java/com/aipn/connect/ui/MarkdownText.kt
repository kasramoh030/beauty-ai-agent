package com.aipn.connect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.sp

/**
 * A very small markdown renderer: headings, bullets, fenced code blocks, inline
 * code, bold and italic. It never evaluates HTML, so an answer from a model
 * cannot inject markup - everything ends up as plain styled text.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    SelectionContainer {
        Column(modifier = modifier) {
            val blocks = remember(text) { parseBlocks(text) }
            blocks.forEach { block ->
                when (block) {
                    is Block.Code -> CodeBlock(block.content, block.language)
                    is Block.Text -> {
                        for ((style, content) in inlineSpans(block.content)) {
                            Text(
                                text = content,
                                style = style,
                                color = color,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed interface Block {
    data class Text(val content: String) : Block
    data class Code(val content: String, val language: String) : Block
}

private fun parseBlocks(source: String): List<Block> {
    val blocks = ArrayList<Block>()
    val buffer = StringBuilder()
    var inCode = false
    var codeLanguage = ""
    val code = StringBuilder()

    for (line in source.lines()) {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("```")) {
            if (inCode) {
                blocks.add(Block.Code(code.toString(), codeLanguage))
                code.setLength(0)
                codeLanguage = ""
                inCode = false
            } else {
                if (buffer.isNotBlank()) {
                    blocks.add(Block.Text(buffer.toString()))
                    buffer.setLength(0)
                }
                inCode = true
                codeLanguage = trimmed.removePrefix("```").trim()
            }
            continue
        }
        if (inCode) {
            code.append(line).append('\n')
            continue
        }
        buffer.append(line).append('\n')
    }
    if (inCode && code.isNotEmpty()) blocks.add(Block.Code(code.toString(), codeLanguage))
    if (buffer.isNotBlank()) blocks.add(Block.Text(buffer.toString()))
    return blocks
}

/** Splits a paragraph into styled lines: headings get bigger font, bullets get a marker. */
@Composable
private fun inlineSpans(content: String): List<Pair<androidx.compose.ui.text.TextStyle, String>> {
    val base = MaterialTheme.typography.bodyMedium
    val result = ArrayList<Pair<androidx.compose.ui.text.TextStyle, String>>()

    for (rawLine in content.trimEnd().lines()) {
        val line = rawLine.trimEnd()
        when {
            line.startsWith("### ") -> result.add(
                base.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp) to line.removePrefix("### ")
            )
            line.startsWith("## ") -> result.add(
                base.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp) to line.removePrefix("## ")
            )
            line.startsWith("# ") -> result.add(
                base.copy(fontWeight = FontWeight.Bold, fontSize = 19.sp) to line.removePrefix("# ")
            )
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> result.add(
                base to "•  " + line.trimStart().drop(2)
            )
            line.isBlank() -> result.add(base.copy(lineHeight = 8.sp) to " ")
            else -> result.add(base to line)
        }
    }
    return result.ifEmpty { listOf(base to "") }
}

@Composable
private fun CodeBlock(code: String, language: String) {
    val background = MaterialTheme.colorScheme.surfaceVariant
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(background, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
    ) {
        if (language.isNotBlank()) {
            Text(
                text = language,
                style = CodeTextStyle.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp),
            )
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Text(
                text = code.trimEnd(),
                style = CodeTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

/** Applies inline `code`, **bold** and *italic* inside an already-parsed line. */
fun annotateInline(text: String, base: androidx.compose.ui.text.TextStyle): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        val char = text[index]
        when {
            char == '`' -> {
                val end = text.indexOf('`', index + 1)
                if (end > index) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = (base.fontSize - 1).coerceAtLeast(11.sp))) {
                        append(text.substring(index + 1, end))
                    }
                    index = end + 1
                } else {
                    append(char); index++
                }
            }
            char == '*' && index + 1 < text.length && text[index + 1] == '*' -> {
                val end = text.indexOf("**", index + 2)
                if (end > index) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(index + 2, end)) }
                    index = end + 2
                } else {
                    append(char); index++
                }
            }
            char == '*' -> {
                val end = text.indexOf('*', index + 1)
                if (end > index) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(index + 1, end)) }
                    index = end + 1
                } else {
                    append(char); index++
                }
            }
            else -> { append(char); index++ }
        }
    }
}