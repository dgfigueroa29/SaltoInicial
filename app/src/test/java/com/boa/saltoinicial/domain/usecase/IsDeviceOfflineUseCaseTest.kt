package com.boa.saltoinicial.domain.usecase

import com.boa.saltoinicial.domain.repository.NetworkMonitor
import io.mockk.every
import io.mockk.mockk
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
}
