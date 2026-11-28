package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the pure helper functions in FilesScreen.kt (private
 * top-level functions, reached by reflection). Needs Robolectric because the
 * containing file's static initializers touch Android APIs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class FilesHelpersTest {

    private fun callStatic(name: String, vararg args: Any?): Any? {
        val k = Class.forName("com.example.ui.files.FilesScreenKt")
        val matches = k.declaredMethods.filter { it.name == name && it.parameterCount == args.size }
        check(matches.isNotEmpty()) { "method $name/${args.size} not found" }
        val m = matches.first()
        m.isAccessible = true
        return m.invoke(null, *args)
    }

    @Test
    fun extOf_edgeCases() {
        assertEquals("jpg", callStatic("extOf", "a.JPG"))
        assertEquals("jpeg", callStatic("extOf", "photo.Jpeg"))
        assertEquals("", callStatic("extOf", "noext"))
        assertEquals("", callStatic("extOf", "trailingdot."))
        assertEquals("txt", callStatic("extOf", "archive.tar.txt"))
    }

    @Test
    fun formatSize_boundaries() {
        assertEquals("0 B", callStatic("formatSize", 0L))
        assertEquals("1023 B", callStatic("formatSize", 1023L))
        assertEquals("1.0 KB", callStatic("formatSize", 1024L))
        assertEquals("1.0 MB", callStatic("formatSize", 1024L * 1024))
        assertEquals("1.00 GB", callStatic("formatSize", 1024L * 1024 * 1024))
        assertEquals("1.50 GB", callStatic("formatSize", 1536L * 1024 * 1024))
    }

    @Test
    fun formatDate_epochZero_isEmpty() {
        assertEquals("", callStatic("formatDate", 0L, true))
        assertEquals("", callStatic("formatDate", -5L, false))
    }
}
