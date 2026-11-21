package com.vishwa.apphider.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockManagerTest {

    @Test
    fun `starts locked`() {
        val lock = LockManager()
        assertTrue(lock.locked.value)
    }

    @Test
    fun `unlock and lock update state`() {
        val lock = LockManager()
        lock.unlock()
        assertFalse(lock.locked.value)
        lock.lock()
        assertTrue(lock.locked.value)
    }
}
