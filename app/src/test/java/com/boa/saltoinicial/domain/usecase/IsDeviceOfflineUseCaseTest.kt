package com.boa.saltoinicial.domain.usecase

import com.boa.saltoinicial.domain.repository.NetworkMonitor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsDeviceOfflineUseCaseTest {

    private val networkMonitor = mockk<NetworkMonitor>()

    @Test
    fun `IsDeviceOfflineUseCase is true when the monitor reports no connection`() {
        every { networkMonitor.isOnline() } returns false

        assertTrue(IsDeviceOfflineUseCase(networkMonitor)())
    }

    @Test
    fun `IsDeviceOfflineUseCase is false when the monitor reports connection`() {
        every { networkMonitor.isOnline() } returns true

        assertFalse(IsDeviceOfflineUseCase(networkMonitor)())
    }

    @Test
    fun `observe inverts the connectivity stream into an offline stream`() = runTest {
        every { networkMonitor.observeOnline() } returns flowOf(true, false, true)

        val offline = IsDeviceOfflineUseCase(networkMonitor).observe().toList()

        assertEquals(listOf(false, true, false), offline)
    }
}
