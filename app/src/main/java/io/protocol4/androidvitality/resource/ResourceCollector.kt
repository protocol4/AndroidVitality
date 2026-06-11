package io.protocol4.androidvitality.resource

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.getSystemService
import java.io.BufferedReader
import java.io.InputStreamReader

class ResourceCollector(private val context: Context) {

    private val activityManager = context.getSystemService<ActivityManager>()
    private val powerManager = context.getSystemService<PowerManager>()
    private val connectivityManager = context.getSystemService<ConnectivityManager>()
    private val windowManager = context.getSystemService<WindowManager>()
    private val sensorManager = context.getSystemService<SensorManager>()
    private val packageManager = context.packageManager

    fun getCpuUsage(): CpuUsage {
        return CpuUsage(
            totalUsage = 0f,
            perAppUsage = emptyList()
        )
    }

    fun getMemoryInfo(): MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        return MemoryInfo(
            totalMemory = memoryInfo.totalMem,
            availableMemory = memoryInfo.availMem,
            threshold = memoryInfo.threshold,
            lowMemory = memoryInfo.lowMemory,
            totalRamGb = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        )
    }

    fun getBatteryStatus(): BatteryStatus {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else -1
        
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        
        val healthInt = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val volt = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        return BatteryStatus(
            level = pct,
            isCharging = isCharging,
            drainRate = 0f,
            estimatedRemainingTime = -1,
            health = health,
            temperature = temp,
            voltage = volt,
            technology = tech
        )
    }

    fun getThermalInfo(): ThermalInfo {
        val status = powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
        return ThermalInfo(
            status = status,
            isThrottling = status >= PowerManager.THERMAL_STATUS_MODERATE
        )
    }

    fun getStorageInfo(): StorageInfo {
        val internalPath = Environment.getDataDirectory()
        val internalStat = StatFs(internalPath.path)
        val internalTotal = internalStat.blockCountLong * internalStat.blockSizeLong
        val internalAvail = internalStat.availableBlocksLong * internalStat.blockSizeLong

        return StorageInfo(
            internalTotal = internalTotal,
            internalAvailable = internalAvail,
            externalTotal = null,
            externalAvailable = null
        )
    }

    fun getNetworkInfo(): NetworkInfo {
        val network = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(network)
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        val type = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Offline"
        }

        return NetworkInfo(
            type = type,
            isConnected = isConnected,
            downloadSpeed = caps?.linkDownstreamBandwidthKbps ?: 0,
            uploadSpeed = caps?.linkUpstreamBandwidthKbps ?: 0
        )
    }

    fun getDisplayInfo(): DisplayInfo {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getMetrics(metrics)
        val refresh = @Suppress("DEPRECATION") windowManager?.defaultDisplay?.refreshRate ?: 60f
        
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        val diagonalInches = Math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
        
        return DisplayInfo(
            resolution = "${metrics.widthPixels}x${metrics.heightPixels}",
            density = metrics.densityDpi,
            refreshRate = refresh,
            physicalSize = "%.1f\"".format(diagonalInches)
        )
    }

    fun getHardwareInfo(): HardwareInfo {
        val vendorOs = getVendorOsInfo()
        return HardwareInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else "N/A",
            socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else "N/A",
            bootloader = Build.BOOTLOADER,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            fingerprint = Build.FINGERPRINT,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown",
            baseband = Build.getRadioVersion() ?: "Unknown",
            vendorOsName = vendorOs.first,
            vendorOsVersion = vendorOs.second,
            buildTags = Build.TAGS,
            buildType = Build.TYPE,
            buildUser = Build.USER,
            buildHost = Build.HOST,
            uptimeMillis = SystemClock.elapsedRealtime(),
            kernelVersion = System.getProperty("os.version") ?: "Unknown"
        )
    }

    private fun getVendorOsInfo(): Pair<String?, String?> {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") -> {
                val sepVersion = getSystemProperty("ro.build.version.sep")
                if (sepVersion != null) {
                    val version = sepVersion.toIntOrNull() ?: 0
                    if (version > 0) {
                        val major = (version - 90000) / 10000
                        val minor = (version % 10000) / 100
                        "One UI" to "$major.$minor"
                    } else "One UI" to null
                } else "One UI" to null
            }
            manufacturer.contains("xiaomi") -> "MIUI" to getSystemProperty("ro.miui.ui.version.name")
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> "EMUI" to getSystemProperty("ro.build.version.emui")
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> "ColorOS" to getSystemProperty("ro.build.version.opporom")
            manufacturer.contains("oneplus") -> "OxygenOS" to getSystemProperty("ro.oxygen.version")
            manufacturer.contains("vivo") -> "FuntouchOS" to getSystemProperty("ro.vivo.os.version")
            else -> null to null
        }
    }

    private fun getSystemProperty(propName: String): String? {
        return try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            val input = BufferedReader(InputStreamReader(p.inputStream))
            val line = input.readLine()
            input.close()
            if (line.isNullOrBlank()) null else line
        } catch (e: Exception) {
            null
        }
    }

    fun getFeatures(): List<FeatureInfo> {
        val features = mutableListOf<FeatureInfo>()
        val featureMap = mapOf(
            PackageManager.FEATURE_BLUETOOTH to "Bluetooth",
            PackageManager.FEATURE_CAMERA to "Camera",
            PackageManager.FEATURE_LOCATION_GPS to "GPS",
            PackageManager.FEATURE_NFC to "NFC",
            PackageManager.FEATURE_WIFI to "WiFi",
            PackageManager.FEATURE_FINGERPRINT to "Fingerprint Sensor",
            PackageManager.FEATURE_FACE to "Face Unlock",
            PackageManager.FEATURE_USB_HOST to "USB Host",
            PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL to "Vulkan Support"
        )
        for ((feature, name) in featureMap) {
            features.add(FeatureInfo(name, packageManager.hasSystemFeature(feature)))
        }
        return features
    }

    fun getSensors(): List<SensorDetail> {
        return sensorManager?.getSensorList(Sensor.TYPE_ALL)?.map {
            SensorDetail(it.name, it.vendor, it.version, it.power)
        } ?: emptyList()
    }

    fun getRuntimeInfo(): RuntimeInfo {
        return RuntimeInfo(
            vmName = System.getProperty("java.vm.name") ?: "Unknown",
            vmVersion = System.getProperty("java.vm.version") ?: "Unknown",
            vmVendor = System.getProperty("java.vm.vendor") ?: "Unknown",
            javaVersion = System.getProperty("java.version") ?: "Unknown"
        )
    }
}
