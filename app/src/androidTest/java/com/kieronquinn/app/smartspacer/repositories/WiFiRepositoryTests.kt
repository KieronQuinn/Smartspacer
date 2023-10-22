@file:Suppress("DEPRECATION")

package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ParceledListSlice
import android.content.pm.ProviderInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TransportInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiSsid
import android.provider.Settings
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository.WiFiNetwork
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.test.BuildConfig
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WiFiRepositoryTests: BaseTest<WiFiRepository>() {

    companion object {
        private const val AUTHORITY_WIFI = "${BuildConfig.APPLICATION_ID}.requirement.wifi"

        private fun createMockNetwork(): Network {
            return mock {

            }
        }

        private fun createMockCapabilities(transportInfo: TransportInfo): NetworkCapabilities {
            return mock {
                every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
                every { this@mock.transportInfo } returns transportInfo
            }
        }

        private fun createMockTransportInfo(ssid: String, mac: String): WifiInfo {
            return mock {
                every { this@mock.ssid } returns ssid
                every { this@mock.bssid } returns mac
            }
        }

        private fun createMockScanResult(ssid: String, mac: String): ScanResult {
            return mock<ScanResult> {
                every { wifiSsid } returns WifiSsid.fromBytes(ssid.toByteArray())
            }.also {
                it.BSSID = mac
            }
        }

        private fun createMockScanResults(): List<ScanResult> {
            return listOf(
                createMockScanResult(randomString(), randomString()),
                createMockScanResult(randomString(), randomString()),
                createMockScanResult(randomString(), randomString())
            )
        }

        private fun createMockWiFiConfiguration(ssid: String, mac: String): WifiConfiguration {
            return WifiConfiguration().apply {
                SSID = ssid
                BSSID = mac
            }
        }

        private fun createMockSavedWiFiNetworks(): List<WifiConfiguration> {
            return listOf(
                createMockWiFiConfiguration(randomString(), randomString()),
                createMockWiFiConfiguration(randomString(), randomString()),
                createMockWiFiConfiguration(randomString(), randomString())
            )
        }
    }

    private val mockTransportInfo = createMockTransportInfo(randomString(), randomString())
    private var currentCapability = createMockCapabilities(mockTransportInfo)
    private val mockScanResults = createMockScanResults()
    private val mockActiveNetwork = createMockNetwork()
    private val mockSavedWiFiNetworks = createMockSavedWiFiNetworks()

    private val shizukuServiceRepositoryMock = mockShizukuRepository {
        every { savedWiFiNetworks } returns ParceledListSlice(mockSavedWiFiNetworks)
    }

    private val connectivityManagerMock = mock<ConnectivityManager> {
        every { getNetworkCapabilities(any()) } returns currentCapability
        every { activeNetwork } returns mockActiveNetwork
    }

    private val wifiManagerMock = mock<WifiManager> {
        every { scanResults } returns mockScanResults
    }

    override val sut by lazy {
        WiFiRepositoryImpl(
            contextMock,
            shizukuServiceRepositoryMock,
            scope
        )
    }

    override fun Context.context() {
        every { getSystemService(Context.WIFI_SERVICE) } returns wifiManagerMock
        every { getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManagerMock
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_WIFI
            }
        }
    }

    @Test
    fun testConnectedNetwork() = runTest {
        sut.connectedNetwork.test {
            assertTrue(awaitItem() == WiFiNetwork(mockTransportInfo.ssid, mockTransportInfo.bssid))
        }
    }

    @Test
    fun testAvailableNetworks() = runTest {
        sut.availableNetworks.test {
            val actual = awaitItem()
            val mock = mockScanResults.map {
                WiFiNetwork(
                    it.wifiSsid!!.toString().removePrefix("\"").removeSuffix("\""),
                    it.BSSID
                )
            }
            assertTrue(actual == mock)
        }
    }

    @Test
    fun testGetSavedWiFiNetworks() = runTest {
        val actual = sut.getSavedWiFiNetworks()
        val mock = mockSavedWiFiNetworks.map {
            WiFiNetwork(it.SSID, it.BSSID)
        }
        assertTrue(actual == mock)
    }

    @Test
    fun testRefresh() = runTest {
        val current = sut.refreshBus.value
        sut.refresh()
        assertFalse(current == sut.refreshBus.value)
    }

    @Test
    fun testScan() = runTest {
        sut.scan()
        verify {
            wifiManagerMock.startScan()
        }
    }

    @Test
    fun testHasWiFiPermissions() = runTest {
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE)
        } returns PackageManager.PERMISSION_DENIED
        assertFalse(sut.hasWiFiPermissions())
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        assertFalse(sut.hasWiFiPermissions())
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        assertFalse(sut.hasWiFiPermissions())
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE)
        } returns PackageManager.PERMISSION_GRANTED
        assertTrue(sut.hasWiFiPermissions())
    }

    @Test
    fun testHasBackgroundLocationPermission() = runTest {
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        assertFalse(sut.hasBackgroundLocationPermission())
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        assertTrue(sut.hasBackgroundLocationPermission())
    }

    @Test
    fun testHasEnabledBackgroundScanning() = runTest {
        settingsGlobalMap[Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON] = "0"
        assertFalse(sut.hasEnabledBackgroundScanning())
        settingsGlobalMap[Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON] = "1"
        assertTrue(sut.hasEnabledBackgroundScanning())
    }

}