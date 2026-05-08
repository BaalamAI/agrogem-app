package com.agrogem.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownTextTest {

    @Test
    fun parseMarkdownBlocks_supportsParagraphBulletsAndCodeBlock() {
        val markdown = """
            Intro paragraph line 1
            continues line 2

            - first item
            * second item

            ```
            val a = 1
            println(a)
            ```
        """.trimIndent()

        val blocks = parseMarkdownBlocks(markdown)

        assertEquals(4, blocks.size)
        assertEquals(MarkdownBlock.Paragraph("Intro paragraph line 1 continues line 2"), blocks[0])
        assertEquals(MarkdownBlock.Bullet("first item"), blocks[1])
        assertEquals(MarkdownBlock.Bullet("second item"), blocks[2])
        assertEquals(MarkdownBlock.Code("val a = 1\nprintln(a)"), blocks[3])
    }

    @Test
    fun parseInlineMarkdown_removesMarkersFromRenderedText() {
        val rendered = parseInlineMarkdown("**bold** *italic* `code`")

        assertEquals("bold italic code", rendered.text)
        assertTrue(rendered.spanStyles.isNotEmpty())
    }

    @Test
    fun parseInlineMarkdown_handlesBoldWithTrailingSpaceBeforeClosingMarker() {
        val rendered = parseInlineMarkdown("**negrita **")

        assertEquals("negrita ", rendered.text)
        assertTrue(rendered.spanStyles.isNotEmpty())
    }

    @Test
    fun parseMarkdownBlocks_supportsHeadings() {
        val blocks = parseMarkdownBlocks("# Header\n## Subheader\nTexto")

        assertEquals(3, blocks.size)
        assertEquals(MarkdownBlock.Heading(level = 1, text = "Header"), blocks[0])
        assertEquals(MarkdownBlock.Heading(level = 2, text = "Subheader"), blocks[1])
        assertEquals(MarkdownBlock.Paragraph("Texto"), blocks[2])
    }
}
