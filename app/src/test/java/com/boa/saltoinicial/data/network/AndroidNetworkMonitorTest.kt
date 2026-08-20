package com.boa.saltoinicial.data.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidNetworkMonitorTest {

    private val network = mockk<Network>()

    private fun managerWith(
        activeNetwork: Network?,
        capabilities: NetworkCapabilities? = null
    ): ConnectivityManager = mockk<ConnectivityManager>().also { manager ->
        every { manager.activeNetwork } returns activeNetwork
        every { manager.getNetworkCapabilities(any()) } returns capabilities
    }

    private fun capabilitiesWithInternet(hasInternet: Boolean) =
        mockk<NetworkCapabilities>().also { capabilities ->
            every {
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } returns hasInternet
        }

    @Test
    fun `isOnline is true when the active network can reach the internet`() {
        val monitor = AndroidNetworkMonitor(
            managerWith(network, capabilitiesWithInternet(hasInternet = true))
        )

        assertTrue(monitor.isOnline())
    }

    @Test
    fun `isOnline is false in airplane mode when there is no active network`() {
        val monitor = AndroidNetworkMonitor(managerWith(activeNetwork = null))

        assertFalse(monitor.isOnline())
    }

    @Test
    fun `isOnline is false when the active network has no internet capability`() {
        val monitor = AndroidNetworkMonitor(
            managerWith(network, capabilitiesWithInternet(hasInternet = false))
        )

        assertFalse(monitor.isOnline())
    }

    @Test
    fun `isOnline is true when the capabilities of the active network are unknown`() {
        val monitor = AndroidNetworkMonitor(managerWith(network, capabilities = null))

        assertTrue(monitor.isOnline())
    }

    @Test
    fun `isOnline is true when ConnectivityManager is not available`() {
        val monitor = AndroidNetworkMonitor(connectivityManager = null)

        assertTrue(monitor.isOnline())
    }

    @Test
    fun `isOnline is true when querying connectivity throws`() {
        val manager = mockk<ConnectivityManager>()
        every { manager.activeNetwork } throws SecurityException("missing permission")

        assertTrue(AndroidNetworkMonitor(manager).isOnline())
    }
}
