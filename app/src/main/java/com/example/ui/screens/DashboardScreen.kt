package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppThemeMode
import com.example.data.model.BookItem
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReaderViewMode
import com.example.data.model.ReadingLogItem
import com.example.data.model.UiState
import com.example.data.model.WorldwideLanguages
import com.example.data.repository.BookshelfRepository
import com.example.notifications.ReadingReminderScheduler
import com.example.ui.viewmodel.BookshelfViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: BookshelfViewModel,
    onShowMessage: (String) -> Unit,
    onOpenLanguagePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val booksUiState by viewModel.booksState.collectAsStateWithLifecycle()
    val logsUiState by viewModel.readingLogsState.collectAsStateWithLifecycle()
    val preferredLanguageTag by viewModel.preferredLanguageTag.collectAsStateWithLifecycle()
    val readerViewMode by viewModel.readerViewMode.collectAsStateWithLifecycle()
    val activeLanguage = remember(preferredLanguageTag) { WorldwideLanguages.findByTag(preferredLanguageTag) }

    val books = (booksUiState as? UiState.Success)?.data ?: emptyList()
    val logs = (logsUiState as? UiState.Success)?.data ?: emptyList()

    val completedCount = books.count { it.completed }
    val todayKey = remember { BookshelfRepository.todayDateKey() }
    val pagesToday = logs.firstOrNull { it.dateKey == todayKey }?.pagesRead?.toInt() ?: 0
    val dailyGoal = (profile?.dailyGoalPages?.toInt() ?: 20).coerceAtLeast(1)
    val streakDays = profile?.streakDays?.toInt() ?: 0
    val totalPagesRead = profile?.totalPagesRead?.toInt() ?: 0

    var reminderEnabled by remember(profile?.reminderEnabled) {
        mutableStateOf(profile?.reminderEnabled ?: false)
    }
    var reminderHour by remember(profile?.reminderHour) {
        mutableIntStateOf(profile?.reminderHour?.toInt() ?: 20)
    }
    var reminderMinute by remember(profile?.reminderMinute) {
        mutableIntStateOf(profile?.reminderMinute?.toInt() ?: 0)
    }
    var goalSliderValue by remember(dailyGoal) { mutableIntStateOf(dailyGoal) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            reminderEnabled = true
            ReadingReminderScheduler.scheduleDailyReminder(context, reminderHour, reminderMinute)
            viewModel.updatePreferences(
                reminderEnabled = true,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            onShowMessage("Daily reading reminder scheduled for ${formatTime(reminderHour, reminderMinute)}")
        } else {
            reminderEnabled = false
            onShowMessage("Notification permission denied. Enable notifications in settings for reminders.")
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reference 2x2 Summary Metrics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Your Reading Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatTile(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            iconTint = MaterialTheme.colorScheme.primary,
                            label = "Total Books",
                            value = "${books.size}",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            icon = Icons.Filled.CheckCircle,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            label = "Finished",
                            value = "$completedCount",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatTile(
                            icon = Icons.Filled.AutoStories,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            label = "Pages Read",
                            value = "$totalPagesRead",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            icon = Icons.Filled.LocalFireDepartment,
                            iconTint = Color(0xFFEAB308),
                            label = "Streak Days",
                            value = "$streakDays d",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Daily Reading Goal Ring Card
            item {
                val goalProgress = (pagesToday.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 11.dp.toPx()
                                    drawArc(
                                        color = trackColor,
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = if (goalProgress >= 1f) primaryColor else secondaryColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * goalProgress,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$pagesToday",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "/ $dailyGoal pp",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Today's Reading Goal",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = if (pagesToday >= dailyGoal) {
                                        "Goal reached! Keep the momentum going."
                                    } else {
                                        "${(dailyGoal - pagesToday).coerceAtLeast(0)} more pages to hit today's target. Total lifetime: $totalPagesRead pages."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Daily target", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "$goalSliderValue pages / day",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = goalSliderValue.toFloat(),
                                onValueChange = { goalSliderValue = it.toInt() },
                                onValueChangeFinished = {
                                    viewModel.updatePreferences(dailyGoalPages = goalSliderValue)
                                },
                                valueRange = 5f..100f
                            )
                        }
                    }
                }
            }

            // 7-Day Activity Bar Chart
            item {
                WeeklyReadingChartCard(logs = logs)
            }

            // Daily Reading Reminder Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Column {
                                    Text(
                                        text = "Daily Reading Reminder",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Scheduled at ${formatTime(reminderHour, reminderMinute)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            reminderEnabled = true
                                            ReadingReminderScheduler.scheduleDailyReminder(context, reminderHour, reminderMinute)
                                            viewModel.updatePreferences(
                                                reminderEnabled = true,
                                                reminderHour = reminderHour,
                                                reminderMinute = reminderMinute
                                            )
                                            onShowMessage("Daily reminder active for ${formatTime(reminderHour, reminderMinute)}")
                                        }
                                    } else {
                                        reminderEnabled = false
                                        ReadingReminderScheduler.cancelDailyReminder(context)
                                        viewModel.updatePreferences(reminderEnabled = false)
                                        onShowMessage("Daily reminder paused")
                                    }
                                },
                                modifier = Modifier.testTag("daily_reminder_switch")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            reminderHour = hour
                                            reminderMinute = minute
                                            if (reminderEnabled) {
                                                ReadingReminderScheduler.scheduleDailyReminder(context, hour, minute)
                                            }
                                            viewModel.updatePreferences(
                                                reminderHour = hour,
                                                reminderMinute = minute
                                            )
                                            onShowMessage("Reminder time set to ${formatTime(hour, minute)}")
                                        },
                                        reminderHour,
                                        reminderMinute,
                                        false
                                    ).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Time")
                            }

                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        val sent = ReadingReminderScheduler.showReminderNotificationNow(context)
                                        onShowMessage(
                                            if (sent) "Test reminder notification sent"
                                            else "Allow notifications to preview reminder"
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Test Reminder")
                            }
                        }
                    }
                }
            }

            // Theme Settings Card
            item {
                val activeAppTheme = profile?.parsedAppTheme ?: AppThemeMode.SYSTEM
                val activeReaderTheme = profile?.parsedReaderTheme ?: ReaderThemeMode.LIGHT
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Appearance & Reading Theme", style = MaterialTheme.typography.titleMedium)

                        Text(
                            text = "App Theme",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = activeAppTheme == mode,
                                    onClick = { viewModel.updatePreferences(appTheme = mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = "Default PDF Reader Theme",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = activeReaderTheme == mode,
                                    onClick = { viewModel.updatePreferences(readerTheme = mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = "PDF Reading Layout",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderViewMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = readerViewMode == mode,
                                    onClick = { viewModel.setReaderViewMode(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Worldwide Language Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Summary & Audio Language",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = activeLanguage.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenLanguagePicker,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Change")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WeeklyReadingChartCard(logs: List<ReadingLogItem>) {
    val last7Days = remember(logs) {
        val logMap = logs.associateBy { it.dateKey }
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        (0..6).map { offset ->
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val key = BookshelfRepository.todayDateKey(dayCal)
            val label = dayFmt.format(dayCal.time).take(2)
            val pages = logMap[key]?.pagesRead?.toInt() ?: 0
            label to pages
        }
    }

    val maxPages = (last7Days.maxOfOrNull { it.second } ?: 10).coerceAtLeast(10)
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "7-Day Reading Activity", style = MaterialTheme.typography.titleLarge)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val count = last7Days.size
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.44f).coerceAtMost(36.dp.toPx())
                val chartHeight = size.height - 8.dp.toPx()

                last7Days.forEachIndexed { index, (_, pages) ->
                    val left = slotWidth * index + (slotWidth - barWidth) / 2f
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    if (pages > 0) {
                        val barH = ((pages.toFloat() / maxPages.toFloat()) * chartHeight).coerceAtLeast(10.dp.toPx())
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, chartHeight - barH),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                last7Days.forEach { (dayLabel, pages) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (pages > 0) "$pages" else "–",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
}
