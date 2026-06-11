package io.protocol4.androidvitality.resource

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ResourceMonitorUiState(
    val hardwareInfo: HardwareInfo? = null,
    val cpuUsage: CpuUsage? = null,
    val memoryInfo: MemoryInfo? = null,
    val batteryStatus: BatteryStatus? = null,
    val thermalInfo: ThermalInfo? = null,
    val storageInfo: StorageInfo? = null,
    val networkInfo: NetworkInfo? = null,
    val displayInfo: DisplayInfo? = null,
    val features: List<FeatureInfo> = emptyList(),
    val sensors: List<SensorDetail> = emptyList(),
    val runtimeInfo: RuntimeInfo? = null,
    val includeSensorsInReport: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val history: List<ResourceSnapshot> = emptyList()
)

class ResourceMonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val collector = ResourceCollector(application)
    private val settingsRepository = SettingsRepository(application)
    private val _uiState = MutableStateFlow(ResourceMonitorUiState(
        themeMode = settingsRepository.getThemeMode(),
        includeSensorsInReport = settingsRepository.shouldIncludeSensors()
    ))
    val uiState: StateFlow<ResourceMonitorUiState> = _uiState.asStateFlow()

    init {
        // Load static info once on IO thread
        viewModelScope.launch(Dispatchers.IO) {
            val hw = collector.getHardwareInfo()
            val feat = collector.getFeatures()
            val sens = collector.getSensors()
            val run = collector.getRuntimeInfo()
            val disp = collector.getDisplayInfo()
            
            _uiState.update { it.copy(
                hardwareInfo = hw,
                features = feat,
                sensors = sens,
                runtimeInfo = run,
                displayInfo = disp
            ) }
            
            // Start the monitoring loop
            startMonitoring()
        }
    }

    private suspend fun startMonitoring() {
        withContext(Dispatchers.IO) {
            while (true) {
                val cpu = collector.getCpuUsage()
                val mem = collector.getMemoryInfo()
                val batt = collector.getBatteryStatus()
                val therm = collector.getThermalInfo()
                val storage = collector.getStorageInfo()
                val network = collector.getNetworkInfo()

                val snapshot = ResourceSnapshot(
                    timestamp = System.currentTimeMillis(),
                    cpuUsage = cpu.totalUsage,
                    memoryUsed = mem.totalMemory - mem.availableMemory,
                    batteryLevel = batt.level,
                    thermalStatus = therm.status
                )

                _uiState.update { state ->
                    state.copy(
                        cpuUsage = cpu,
                        memoryInfo = mem,
                        batteryStatus = batt,
                        thermalInfo = therm,
                        storageInfo = storage,
                        networkInfo = network,
                        history = (state.history + snapshot).takeLast(60)
                    )
                }
                delay(1000)
            }
        }
    }

    fun setIncludeSensorsInReport(include: Boolean) {
        settingsRepository.setIncludeSensors(include)
        _uiState.update { it.copy(includeSensorsInReport = include) }
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun exportReport(): String {
        val state = _uiState.value
        val report = StringBuilder()
        report.append("--- ANDROID VITALITY SYSTEM REPORT ---\n")
        report.append("Generated on: ${java.util.Date()}\n\n")

        report.append("1. SOFTWARE & OS\n")
        report.append("- Android Version: ${state.hardwareInfo?.androidVersion}\n")
        if (state.hardwareInfo?.vendorOsName != null) {
            report.append("- ${state.hardwareInfo.vendorOsName}: ${state.hardwareInfo.vendorOsVersion ?: "Unknown"}\n")
        }
        report.append("- SDK Level: ${state.hardwareInfo?.sdkInt}\n")
        report.append("- Security Patch: ${state.hardwareInfo?.securityPatch}\n")
        report.append("- Build Fingerprint: ${state.hardwareInfo?.fingerprint}\n\n")

        report.append("2. RAM & MEMORY\n")
        report.append("- Total RAM: %.2f GB\n".format(state.memoryInfo?.totalRamGb ?: 0.0))
        report.append("- Available RAM: %.2f GB\n".format((state.memoryInfo?.availableMemory ?: 0L) / (1024.0 * 1024.0 * 1024.0)))
        report.append("- Low Memory State: ${state.memoryInfo?.lowMemory}\n\n")

        report.append("3. MOTHERBOARD & BUILD\n")
        report.append("- Manufacturer: ${state.hardwareInfo?.manufacturer}\n")
        report.append("- Brand: ${state.hardwareInfo?.brand}\n")
        report.append("- Model: ${state.hardwareInfo?.model}\n")
        report.append("- Board: ${state.hardwareInfo?.board}\n")
        report.append("- Hardware: ${state.hardwareInfo?.hardware}\n")
        report.append("- Bootloader: ${state.hardwareInfo?.bootloader}\n")
        report.append("- Radio: ${state.hardwareInfo?.baseband}\n\n")

        report.append("4. PROCESSOR & SOC\n")
        report.append("- SoC Manufacturer: ${state.hardwareInfo?.socManufacturer}\n")
        report.append("- SoC Model: ${state.hardwareInfo?.socModel}\n")
        report.append("- Architecture: ${state.hardwareInfo?.supportedAbis?.joinToString(", ")}\n")
        report.append("- CPU Cores: ${Runtime.getRuntime().availableProcessors()}\n\n")

        report.append("5. DISPLAY\n")
        report.append("- Resolution: ${state.displayInfo?.resolution}\n")
        report.append("- Refresh Rate: ${state.displayInfo?.refreshRate}Hz\n")
        report.append("- Density: ${state.displayInfo?.density} DPI\n")
        report.append("- Physical Size: ${state.displayInfo?.physicalSize}\n\n")

        report.append("6. STORAGE\n")
        report.append("- Internal Total: %.2f GB\n".format((state.storageInfo?.internalTotal ?: 0L) / (1024.0 * 1024.0 * 1024.0)))
        report.append("- Internal Available: %.2f GB\n".format((state.storageInfo?.internalAvailable ?: 0L) / (1024.0 * 1024.0 * 1024.0)))
        report.append("\n")

        report.append("7. BATTERY & THERMALS\n")
        report.append("- Level: ${state.batteryStatus?.level}%\n")
        report.append("- Health: ${state.batteryStatus?.health}\n")
        report.append("- Temperature: ${state.batteryStatus?.temperature}°C\n")
        report.append("- Voltage: ${state.batteryStatus?.voltage}mV\n")
        report.append("- Thermal Status: ${state.thermalInfo?.status}\n\n")

        report.append("8. HARDWARE FEATURES\n")
        state.features.forEach {
            report.append("- ${it.name}: ${if (it.isAvailable) "YES" else "NO"}\n")
        }
        report.append("\n")

        if (state.includeSensorsInReport) {
            report.append("9. SENSORS (${state.sensors.size})\n")
            state.sensors.forEach {
                report.append("- ${it.name} (${it.vendor}) [Power: ${it.power}mA]\n")
            }
        } else {
            report.append("9. SENSORS\n- Sensor list excluded (Toggle enabled in app to include)\n")
        }

        return report.toString()
    }
}
