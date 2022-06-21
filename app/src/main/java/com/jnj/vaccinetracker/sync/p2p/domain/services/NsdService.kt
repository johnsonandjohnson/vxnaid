package com.jnj.vaccinetracker.sync.p2p.domain.services

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.p2p.common.models.CompatibleNsdDevice.Companion.toNsdDevice
import com.jnj.vaccinetracker.sync.p2p.common.models.CompatibleService
import com.jnj.vaccinetracker.sync.p2p.common.models.NsdDeviceEvent
import com.jnj.vaccinetracker.sync.p2p.common.models.ReceiverInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class NsdService @Inject constructor(
    context: Context,
    private val userRepository: UserRepository,
) {
    companion object {
        const val SERVICE_TYPE = "_vmp._tcp."
    }

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val thisService get() = userRepository.getDeviceName()?.let { CompatibleService.fromDeviceName(it) }

    val nsdDeviceEvents = MutableSharedFlow<NsdDeviceEvent>(extraBufferCapacity = 10)

    private val resolvingService = MutableStateFlow<NsdServiceInfo?>(null)

    private val registrationListener = MutableStateFlow<NsdManager.RegistrationListener?>(null)
    private val discoveryListener = MutableStateFlow<NsdManager.DiscoveryListener?>(null)

    private fun createDiscoveryListener() = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            logError("onStartDiscoveryFailed: $serviceType $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            logError("onStopDiscoveryFailed: $serviceType $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String?) {
            logInfo("onDiscoveryStarted: $serviceType")
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            logInfo("onDiscoveryStopped: $serviceType")
        }

        override fun onServiceFound(service: NsdServiceInfo?) {
            logInfo("onServiceFound: $service")
            if (service != null) {
                val compatibleService = CompatibleService.tryParse(service.serviceName)
                when {
                    service.serviceType != SERVICE_TYPE -> // Service type is the string containing the protocol and
                        // transport layer for this service.
                        logInfo("Unknown Service Type: ${service.serviceType}")
                    service.serviceName == thisService?.serviceName ->
                        // name matches exactly thus it's our device
                        logInfo("Same machine: ${service.serviceName}")
                    compatibleService != null -> {
                        // different compatible device, try to resolve it
                        if (resolvingService.value == null) {
                            resolvingService.value = service
                            nsdManager.resolveService(service, resolveListener)
                        }
                    }
                }
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            logInfo("onServiceLost: $serviceInfo")
            val nsdDevice = serviceInfo?.toNsdDevice()
            if (nsdDevice != null) {
                nsdDeviceEvents.tryEmit(NsdDeviceEvent.Lost(nsdDevice))
            }
        }
    }.also {
        this.discoveryListener.value = it
    }

    private fun createRegistrationListener() = object : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            logError("onRegistrationFailed: $serviceInfo $errorCode")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            logError("onUnregistrationFailed: $serviceInfo $errorCode")
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
            logInfo("onServiceRegistered: $serviceInfo")
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
            logInfo("onServiceUnregistered: $serviceInfo")
        }
    }.also { this.registrationListener.value = it }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            logError("onResolveFailed: $serviceInfo $errorCode")
            resolvingService.value = null
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            logInfo("onServiceResolved: $serviceInfo")
            val nsdDevice = serviceInfo.toNsdDevice()
            if (nsdDevice != null)
                nsdDeviceEvents.tryEmit(NsdDeviceEvent.Resolved(nsdDevice))

            resolvingService.value = null
        }

    }

    fun registerService(receiverInfo: ReceiverInfo) {
        val thisService = requireNotNull(thisService) { "deviceName must not be null" }
        logInfo("registerService: $receiverInfo")
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo().apply {
            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceName = thisService.serviceName
            serviceType = SERVICE_TYPE
            port = receiverInfo.port
        }
        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            createRegistrationListener()
        )
    }

    fun rediscoverServices() {
        stopServiceDiscover()
        discoverServices()
    }

    fun discoverServices() {
        nsdManager.discoverServices(
            SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            createDiscoveryListener()
        )
    }

    private fun stopServiceDiscover() {
        discoveryListener.value?.let {
            nsdManager.stopServiceDiscovery(it)
            discoveryListener.value = null
        }
    }

    fun disconnect() {
        registrationListener.value?.let {
            nsdManager.unregisterService(it)
            registrationListener.value = null
        }
        stopServiceDiscover()

    }

}