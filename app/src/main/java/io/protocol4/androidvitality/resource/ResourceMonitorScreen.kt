package io.protocol4.androidvitality.resource

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceMonitorScreen(
    viewModel: ResourceMonitorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var activeScreen by remember { mutableStateOf("main") }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = activeScreen == "main",
        drawerContent = {
            SettingsDrawerContent(
                themeMode = uiState.themeMode,
                includeSensors = uiState.includeSensorsInReport,
                activeScreen = activeScreen,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                onToggleSensors = { viewModel.setIncludeSensorsInReport(it) },
                onNavigate = { screen ->
                    scope.launch { drawerState.close() }
                    activeScreen = screen
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                DiagnosticTopBar(
                    activeScreen = activeScreen,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onBackClick = { activeScreen = "main" },
                    onExportClick = {
                        val report = viewModel.exportReport()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, report)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (activeScreen) {
                    "privacy" -> PrivacyPolicyScreen()
                    "sensors" -> SensorListScreen(sensors = uiState.sensors)
                    "license" -> LicenseScreen()
                    else -> MainDiagnosticsScreen(
                        uiState = uiState,
                        onViewSensors = { activeScreen = "sensors" }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticTopBar(
    activeScreen: String,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val title = when (activeScreen) {
        "privacy" -> "Privacy"
        "sensors" -> "Sensors"
        "license" -> "License"
        else -> "Android Vitality"
    }
    
    CenterAlignedTopAppBar(
        title = { 
            Text(
                title, 
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        navigationIcon = {
            if (activeScreen == "main") {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (activeScreen == "main") {
                IconButton(onClick = onExportClick) {
                    Icon(Icons.Default.IosShare, contentDescription = "Export")
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    )
}

@Composable
private fun SettingsDrawerContent(
    themeMode: ThemeMode,
    includeSensors: Boolean,
    activeScreen: String,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleSensors: (Boolean) -> Unit,
    onNavigate: (String) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Android Vitality",
            modifier = Modifier.padding(horizontal = 28.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "System Settings",
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Theme Mode",
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        ThemeOption(
            label = "Light",
            selected = themeMode == ThemeMode.LIGHT,
            icon = Icons.Default.LightMode,
            onClick = { onThemeModeChange(ThemeMode.LIGHT) }
        )
        ThemeOption(
            label = "Dark",
            selected = themeMode == ThemeMode.DARK,
            icon = Icons.Default.DarkMode,
            onClick = { onThemeModeChange(ThemeMode.DARK) }
        )
        ThemeOption(
            label = "System Default",
            selected = themeMode == ThemeMode.SYSTEM,
            icon = Icons.Default.SettingsBrightness,
            onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 28.dp), color = MaterialTheme.colorScheme.outlineVariant)

        NavigationDrawerItem(
            label = { Text("Include Sensors in Report", fontWeight = FontWeight.SemiBold) },
            selected = false,
            onClick = { onToggleSensors(!includeSensors) },
            icon = { Icon(Icons.Default.SettingsInputAntenna, null) },
            badge = { 
                Switch(
                    checked = includeSensors, 
                    onCheckedChange = onToggleSensors,
                    thumbContent = if (includeSensors) {
                        { Icon(Icons.Default.Check, null, Modifier.size(12.dp)) }
                    } else null
                ) 
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 28.dp), color = MaterialTheme.colorScheme.outlineVariant)
        
        NavigationDrawerItem(
            label = { Text("Privacy Policy") },
            selected = activeScreen == "privacy",
            onClick = { onNavigate("privacy") },
            icon = { Icon(Icons.Default.Shield, null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        NavigationDrawerItem(
            label = { Text("Software License") },
            selected = activeScreen == "license",
            onClick = { onNavigate("license") },
            icon = { Icon(Icons.Default.Terminal, null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null) },
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
fun MainDiagnosticsScreen(
    uiState: ResourceMonitorUiState,
    onViewSensors: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "software") { SoftwareCard(uiState.hardwareInfo) }
        item(key = "ram") { RamCard(uiState.memoryInfo) }
        item(key = "motherboard") { MotherboardCard(uiState.hardwareInfo) }
        item(key = "processor") { ProcessorCard(uiState.hardwareInfo) }
        item(key = "display") { DisplayCard(uiState.displayInfo) }
        item(key = "network") { NetworkCard(uiState.networkInfo) }
        item(key = "storage") { StorageCard(uiState.storageInfo) }
        item(key = "build") { BuildCard(uiState.hardwareInfo) }
        item(key = "sensors_btn") {
            Button(
                onClick = onViewSensors,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Icon(Icons.Default.Sensors, null)
                Spacer(Modifier.width(12.dp))
                Text("View All ${uiState.sensors.size} Sensors", fontWeight = FontWeight.Bold)
            }
        }
        item(key = "battery") { BatteryCard(uiState.batteryStatus, uiState.thermalInfo) }
    }
}

@Composable
private fun SoftwareCard(info: HardwareInfo?) {
    SystemCard(title = "Software & OS", icon = Icons.Default.Android, color = Color(0xFF4CAF50)) {
        DataLine("Android Version", "v${info?.androidVersion}")
        info?.vendorOsName?.let { DataLine(it, info.vendorOsVersion ?: "Unknown") }
        DataLine("SDK Level", "API ${info?.sdkInt}")
        DataLine("Security Patch", info?.securityPatch ?: "Unknown")
        DataLine("Kernel", info?.kernelVersion ?: "Unknown")
    }
}

@Composable
private fun RamCard(info: MemoryInfo?) {
    val totalRam = info?.totalRamGb ?: 0.0
    val availRam = (info?.availableMemory ?: 0L) / (1024.0 * 1024.0 * 1024.0)
    val usedRam = totalRam - availRam
    
    val commonRamSizes = listOf(1, 2, 3, 4, 6, 8, 12, 16, 24, 32)
    val marketedRam = commonRamSizes.firstOrNull { it >= totalRam }?.toDouble() ?: kotlin.math.ceil(totalRam)
    val reservedRam = marketedRam - totalRam

    val animatedProgress by animateFloatAsState(
        targetValue = if (totalRam > 0) (usedRam / totalRam).toFloat() else 0f,
        animationSpec = tween(500), label = "RamProgress"
    )

    SystemCard(title = "RAM & Memory", icon = Icons.Default.Memory, color = Color(0xFF9C27B0)) {
        DataLine("Marketed RAM", "%.0f GB".format(marketedRam))
        DataLine("Usable Physical RAM", "%.2f GB".format(totalRam))
        DataLine("Hardware Reserved", "%.2f GB".format(reservedRam))
        DataLine("Available RAM", "%.2f GB".format(availRam))
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(12.dp),
            color = Color(0xFF9C27B0),
            trackColor = Color(0xFF9C27B0).copy(alpha = 0.2f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun MotherboardCard(info: HardwareInfo?) {
    SystemCard(title = "Device & Motherboard", icon = Icons.Default.DeveloperBoard, color = MaterialTheme.colorScheme.secondary) {
        DataLine("Manufacturer", info?.manufacturer ?: "Unknown")
        DataLine("Board", info?.board ?: "Unknown")
        DataLine("Model", info?.model ?: "Unknown")
        DataLine("Brand", info?.brand ?: "Unknown")
    }
}

@Composable
private fun ProcessorCard(info: HardwareInfo?) {
    SystemCard(title = "Processor & SoC", icon = Icons.Default.Speed, color = MaterialTheme.colorScheme.primary) {
        DataLine("SoC Vendor", info?.socManufacturer ?: "N/A")
        DataLine("SoC Model", info?.socModel ?: "N/A")
        DataLine("Architecture", info?.supportedAbis?.firstOrNull() ?: "Unknown")
        DataLine("Cores", Runtime.getRuntime().availableProcessors().toString())
    }
}

@Composable
private fun DisplayCard(info: DisplayInfo?) {
    SystemCard(title = "Display & Graphics", icon = Icons.Default.SettingsOverscan, color = Color(0xFF2196F3)) {
        DataLine("Resolution", info?.resolution ?: "Unknown")
        DataLine("Refresh Rate", "${info?.refreshRate?.toInt()} Hz")
        DataLine("Density", "${info?.density} DPI")
        DataLine("Physical Size", info?.physicalSize ?: "Unknown")
    }
}

@Composable
private fun NetworkCard(info: NetworkInfo?) {
    SystemCard(title = "Network Status", icon = Icons.Default.Public, color = Color(0xFFE91E63)) {
        DataLine("Connection", info?.type ?: "Offline")
        DataLine("Status", if (info?.isConnected == true) "Connected" else "Disconnected")
    }
}

@Composable
private fun StorageCard(info: StorageInfo?) {
    SystemCard(title = "Storage Breakdown", icon = Icons.Default.Storage, color = Color(0xFF795548)) {
        val total = (info?.internalTotal ?: 0L) / (1024.0 * 1024.0 * 1024.0)
        val avail = (info?.internalAvailable ?: 0L) / (1024.0 * 1024.0 * 1024.0)
        val used = total - avail
        
        DataLine("Internal Total", "%.2f GB".format(total))
        DataLine("Available", "%.2f GB".format(avail))
        
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) (used / total).toFloat() else 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF795548),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun BuildCard(info: HardwareInfo?) {
    SystemCard(title = "Build Details", icon = Icons.Default.Terminal, color = Color(0xFF607D8B)) {
        DataLine("Build Tags", info?.buildTags ?: "N/A")
        DataLine("Build Type", info?.buildType ?: "N/A")
        val uptime = info?.uptimeMillis ?: 0L
        val hours = TimeUnit.MILLISECONDS.toHours(uptime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) % 60
        DataLine("Uptime", "${hours}h ${minutes}m")
    }
}

@Composable
private fun BatteryCard(status: BatteryStatus?, thermal: ThermalInfo?) {
    SystemCard(title = "Battery & Thermals", icon = Icons.Default.Bolt, color = Color(0xFFFF9800)) {
        DataLine("Battery Level", "${status?.level}%")
        DataLine("Health", status?.health ?: "Unknown")
        DataLine("Temperature", "${status?.temperature}°C")
        DataLine("Voltage", "${status?.voltage} mV")
        DataLine("Thermal Status", if (thermal?.isThrottling == true) "THROTTLING" else "Normal")
    }
}

@Composable
fun SensorListScreen(sensors: List<SensorDetail>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sensors, key = { it.name + it.vendor }) { sensor ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(sensor.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    DataLine("Vendor", sensor.vendor)
                    DataLine("Power", "${sensor.power} mA")
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Privacy Policy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Android Vitality respects your privacy. \n\n" +
            "• Local Processing: All system diagnostics are calculated in real-time on your device. \n" +
            "• No Tracking: We do not use any analytics, tracking pixels, or remote logging. \n" +
            "• No Servers: Your data never leaves your device unless you manually use the 'Export' feature to share it yourself. \n" +
            "• Minimal Permissions: We only access standard system APIs required for hardware identification.\n\n" +
            "This app is fully open-source and transparent.",
            style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp
        )
    }
}

@Composable
fun LicenseScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            "GNU LESSER GENERAL PUBLIC LICENSE v3",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "GNU LESSER GENERAL PUBLIC LICENSE\n" +
            "Version 3, 29 June 2007\n\n" +

            "Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>\n" +
            "Everyone is permitted to copy and distribute verbatim copies\n" +
            "of this license document, but changing it is not allowed.\n\n" +

            "This version of the GNU Lesser General Public License incorporates\n" +
            "the terms and conditions of version 3 of the GNU General Public\n" +
            "License, supplemented by the additional permissions listed below.\n\n" +

            "0. Additional Definitions.\n\n" +

            "As used herein, “this License” refers to version 3 of the GNU Lesser\n" +
            "General Public License, and the “GNU GPL” refers to version 3 of the GNU\n" +
            "General Public License.\n\n" +

            "“The Library” refers to a covered work governed by this License,\n" +
            "other than an Application or a Combined Work as defined below.\n\n" +

            "An “Application” is any work that makes use of an interface provided\n" +
            "by the Library, but which is not otherwise based on the Library.\n\n" +

            "A “Combined Work” is a work produced by combining or linking an\n" +
            "Application with the Library.\n\n" +

            "The “Minimal Corresponding Source” for a Combined Work means the\n" +
            "Corresponding Source for the Combined Work, excluding any source code\n" +
            "for portions of the Combined Work that, considered in isolation, are\n" +
            "based on the Application, and not on the Library.\n\n" +

            "The “Corresponding Application Code” for a Combined Work means the\n" +
            "object code and/or source code for the Application, including any data\n" +
            "and utility programs needed for reproducing the Combined Work from the\n" +
            "Application, but excluding the System Libraries of the Combined Work.\n\n" +

            "1. Exception to Section 3 of the GNU GPL.\n\n" +

            "You may convey a covered work under sections 3 and 4 of this License\n" +
            "without being bound by section 3 of the GNU GPL.\n\n" +

            "2. Conveying Modified Versions.\n\n" +

            "If you modify a copy of the Library, and, in your modifications, a\n" +
            "facility refers to a function or data to be supplied by an Application\n" +
            "that uses the facility (other than as an argument passed when the\n" +
            "facility is invoked), then you may convey a copy of the modified version:\n\n" +

            "    a) under this License, provided that you make a good faith effort to\n" +
            "    ensure that, in the event an Application does not supply the\n" +
            "    function or data, the facility still operates, and performs whatever\n" +
            "    part of its purpose remains meaningful, or\n\n" +

            "    b) under the GNU GPL, with none of the additional permissions of\n" +
            "    this License applicable to that copy.\n\n" +

            "3. Object Code Incorporating Material from Library Header Files.\n\n" +

            "The object code form of an Application may incorporate material from\n" +
            "a header file that is part of the Library. You may convey such object\n" +
            "code under terms of your choice, provided that you do both of the following:\n\n" +

            "    a) Give prominent notice with each copy that the Library is used in it\n" +
            "    and that the Library and its use are covered by this License.\n\n" +

            "    b) Accompany the object code with a copy of the GNU GPL and this\n" +
            "    license document.\n\n" +

            "4. Combined Works.\n\n" +

            "You may convey a Combined Work under terms of your choice that,\n" +
            "taken together, effectively do not restrict modification of the portions\n" +
            "of the Library contained in the Combined Work and reverse engineering\n" +
            "for debugging such modifications, if you also do each of the following:\n\n" +

            "    a) Give prominent notice that the Library is used in the work.\n\n" +
            "    b) Include a copy of the GNU GPL.\n\n" +
            "    c) For a Combined Work that displays copyright notices during\n" +
            "    execution, include the Library copyright notice.\n\n" +

            "    d) Do one of the following:\n\n" +

            "        0) Convey the Minimal Corresponding Source under this License,\n" +
            "        and the Corresponding Application Code in a form suitable for,\n" +
            "        and under terms that permit, the user to recombine or relink\n" +
            "        the Application with a modified version of the Combined Work.\n\n" +

            "        1) Use a suitable shared library mechanism for linking with the\n" +
            "        Library. A suitable mechanism is one that (a) uses at run time\n" +
            "        a copy of the Library already present on the user's system,\n" +
            "        and (b) will operate properly with a modified version of the\n" +
            "        Library that is interface-compatible.\n\n" +

            "    e) Provide Installation Information.\n\n" +

            "5. Combined Libraries.\n\n" +

            "You may place library facilities that are a work based on the Library\n" +
            "side by side in a single library together with other library facilities\n" +
            "that are not Applications and are not covered by this License, and\n" +
            "convey such a combined library under terms of your choice, if you do\n" +
            "each of the following:\n\n" +

            "    a) Accompany the combined library with a copy of the same work based\n" +
            "    on the Library, uncombined with any other library facilities.\n\n" +

            "    b) Give prominent notice that part of it is a work based on the Library,\n" +
            "    and explaining where to find the uncombined form of the same work.\n\n" +

            "6. Revised Versions of the GNU Lesser General Public License.\n\n" +
            "The Free Software Foundation may publish revised versions of the GNU\n" +
            "Lesser General Public License.\n\n" +
            "Each version is given a distinguishing version number.\n\n" +
            "If the Library specifies 'or later version', you may choose that or any later version.\n",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun SystemCard(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = color.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun DataLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            value, 
            style = MaterialTheme.typography.bodyMedium, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
