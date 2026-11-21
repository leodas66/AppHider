package com.vishwa.apphider.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSignatureVerifierTest {

    @Test
    fun `matching signatures with different byte arrays are accepted`() {
        val installed = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
        val archive = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))

        assertTrue(signaturesMatch(installed, archive))
    }

    @Test
    fun `different signatures are rejected`() {
        val installed = listOf(byteArrayOf(1, 2, 3))
        val archive = listOf(byteArrayOf(1, 2, 4))

        assertFalse(signaturesMatch(installed, archive))
    }

    @Test
    fun `empty or null signatures are rejected`() {
        assertFalse(signaturesMatch(null, listOf(byteArrayOf(1))))
        assertFalse(signaturesMatch(emptyList(), listOf(byteArrayOf(1))))
        assertFalse(signaturesMatch(listOf(byteArrayOf(1)), null))
        assertFalse(signaturesMatch(listOf(byteArrayOf(1)), emptyList()))
    }
}
