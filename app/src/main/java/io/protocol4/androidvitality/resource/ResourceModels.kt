package io.protocol4.androidvitality.resource

import androidx.compose.runtime.Immutable

@Immutable
data class CpuUsage(
    val totalUsage: Float,
    val perAppUsage: List<AppResourceUsage>
)

@Immutable
data class MemoryInfo(
    val totalMemory: Long,
    val availableMemory: Long,
    val threshold: Long,
    val lowMemory: Boolean,
    val totalRamGb: Double
)

@Immutable
data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
    val drainRate: Float,
    val estimatedRemainingTime: Long,
    val health: String,
    val temperature: Float,
    val voltage: Int,
    val technology: String
)

@Immutable
data class StorageInfo(
    val internalTotal: Long,
    val internalAvailable: Long,
    val externalTotal: Long?,
    val externalAvailable: Long?
)

@Immutable
data class NetworkInfo(
    val type: String,
    val isConnected: Boolean,
    val downloadSpeed: Int,
    val uploadSpeed: Int
)

@Immutable
data class DisplayInfo(
    val resolution: String,
    val density: Int,
    val refreshRate: Float,
    val physicalSize: String
)

@Immutable
data class ThermalInfo(
    val status: Int,
    val isThrottling: Boolean
)

@Immutable
data class AppResourceUsage(
    val packageName: String,
    val appName: String,
    val cpuUsage: Float,
    val memoryUsage: Long
)

@Immutable
data class ResourceSnapshot(
    val timestamp: Long,
    val cpuUsage: Float,
    val memoryUsed: Long,
    val batteryLevel: Int,
    val thermalStatus: Int
)

@Immutable
data class HardwareInfo(
    val model: String,
    val manufacturer: String,
    val brand: String,
    val board: String,
    val hardware: String,
    val socManufacturer: String,
    val socModel: String,
    val bootloader: String,
    val device: String,
    val product: String,
    val fingerprint: String,
    val supportedAbis: List<String>,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatch: String,
    val baseband: String,
    val vendorOsName: String?,
    val vendorOsVersion: String?,
    val buildTags: String,
    val buildType: String,
    val buildUser: String,
    val buildHost: String,
    val uptimeMillis: Long,
    val kernelVersion: String
)

@Immutable
data class FeatureInfo(
    val name: String,
    val isAvailable: Boolean
)

@Immutable
data class SensorDetail(
    val name: String,
    val vendor: String,
    val version: Int,
    val power: Float
)

@Immutable
data class RuntimeInfo(
    val vmName: String,
    val vmVersion: String,
    val vmVendor: String,
    val javaVersion: String
)
