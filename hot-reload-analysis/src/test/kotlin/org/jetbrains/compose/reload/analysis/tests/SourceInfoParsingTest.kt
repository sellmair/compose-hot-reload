package org.jetbrains.compose.reload.analysis.tests

import org.jetbrains.compose.reload.analysis.SourceInfo.Location
import org.jetbrains.compose.reload.analysis.SourceInfo.Parameters
import org.jetbrains.compose.reload.analysis.asSequence
import org.jetbrains.compose.reload.analysis.parseSourceInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceInfoParsingTest {

    @Test
    fun `test - top level marker`() {
        val info = parseSourceInfo("C:Foo.kt")
        assertEquals("", info.functionName)
        assertEquals(true, info.isLambda)
        assertEquals(false, info.isInline)
        assertEquals(null, info.parameters)
        assertEquals(emptyList(), info.locations)
        assertEquals("Foo.kt", info.fileName)
        assertEquals(null, info.hash)
    }

    @Test
    fun `test - remember`() {
        val info = parseSourceInfo("CC(remember):Foo.kt#9igjgp")
        assertEquals("remember", info.functionName)
        assertEquals(false, info.isLambda)
        assertEquals(true, info.isInline)
        assertEquals(null, info.parameters)
        assertEquals(emptyList(), info.locations)
        assertEquals("Foo.kt", info.fileName)
        assertEquals("9igjgp", info.hash)
    }

    @Test
    fun `test - Layout`() {
        val info = parseSourceInfo("CC(Layout)P(!1,2)79@3208L23,82@3359L411:Layout.kt#80mrfh")
        assertEquals("Layout", info.functionName)
        assertEquals(false, info.isLambda)
        assertEquals(true, info.isInline)
        assertEquals(Parameters("!1,2", 79), info.parameters)
        assertEquals(
            listOf(
                Location(3208, 23, 82),
                Location(3359, 411)
            ), info.locations
        )
        assertEquals("Layout.kt", info.fileName)
        assertEquals("80mrfh", info.hash)
    }

    @Test
    fun `test - Column`() {
        val info = parseSourceInfo("C88@4444L9:Column.kt#2w3rfo")
        assertEquals("88", info.functionName)
        assertEquals(true, info.isLambda)
        assertEquals(false, info.isInline)
        assertEquals(null, info.parameters)
        assertEquals(listOf(Location(4444, 9, -1)), info.locations)
        assertEquals("Column.kt", info.fileName)
        assertEquals("2w3rfo", info.hash)
    }

    @Test
    fun `test - lambda with locations`() {
        val info = parseSourceInfo("C15@399L24,16@432L22,18@464L40,20@531L30,20@514L89:Foo.kt")

        assertEquals("15", info.functionName)
        assertEquals(true, info.isLambda)
        assertEquals(false, info.isInline)
        assertEquals(null, info.parameters)
        assertEquals(
            listOf(
                Location(399, 24, 16),
                Location(432, 22, 18),
                Location(464, 40, 20),
                Location(531, 30, 20),
                Location(514, 89),
            ),
            info.locations
        )
        assertEquals("Foo.kt", info.fileName)
        assertEquals(null, info.hash)
    }

    @Test
    fun `test - empty string parsed as empty sequence`() {
        assertEquals(emptyList(), "".asSequence().toList())
    }

    @Test
    fun `test - 1,2,3 parsed as sequence`() {
        assertEquals(listOf(1, 2, 3), "1,2,3".asSequence().toList())
    }

    @Test
    fun `test - !5 parsed as sequence`() {
        assertEquals(listOf(0, 1, 2, 3, 4), "!5".asSequence().toList())
    }

    @Test
    fun `test - mixed sequence parsed as sequence`() {
        assertEquals(listOf(1, 0, 1, 2, 3, 4, 3, 0, 1), "1,!5,3,!2".asSequence().toList())
    }
}