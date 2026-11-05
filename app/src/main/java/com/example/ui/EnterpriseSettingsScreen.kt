package com.example.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.InitialLeaveSeedData
import com.example.data.TnHolidayItem
import com.example.domain.BiometricLockManager
import com.example.domain.CalendarIcsSyncService
import com.example.domain.ElMasterPassbookEngine
import com.example.domain.ExecutiveDigestShareService
import com.example.domain.HolidaySyncStatus
import com.example.domain.LeaveNotificationHelper
import com.example.domain.LedgerEnrichmentStore
import com.example.domain.TnHolidayBackgroundSyncService
import com.example.domain.TnHolidays2027Catalog
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeOnDutyBlue
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.StatusAlertRed
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object HodProfileSettingsStore {
  private const val PREFS_NAME = "hod_profile_settings_prefs"
  private const val KEY_OFFICER_NAME = "officer_name"
  private const val KEY_DESIGNATION = "designation"
  private const val KEY_DEPARTMENT = "department"
  private const val KEY_COLLEGE = "college"
  private const val KEY_IFHRMS_ID = "ifhrms_id"
  private const val KEY_DEFAULT_START_YEAR = "default_start_year"
  private const val KEY_EXTERNAL_CALENDAR_FEED_URL = "external_calendar_feed_url"
  private const val KEY_AUTO_SYNC_ON_LAUNCH = "auto_sync_on_launch"
  private const val KEY_CLOUD_FIRESTORE_SYNC = "cloud_firestore_sync"
  private const val KEY_ENFORCE_10_DAY_CAP = "enforce_10_day_cap"
  private const val KEY_ENFORCE_FIFO_COL = "enforce_fifo_col"
  private const val KEY_INCLUDE_SATURDAY_BRIDGE = "include_saturday_bridge"

  fun getOfficerName(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_OFFICER_NAME, "Dr. Kasi, MD (Psychiatry)") ?: "Dr. Kasi, MD (Psychiatry)"

  fun getDesignation(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_DESIGNATION, "Professor & HOD") ?: "Professor & HOD"

  fun getDepartment(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_DEPARTMENT, "Department of Psychiatry") ?: "Department of Psychiatry"

  fun getCollege(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_COLLEGE, "Govt. Erode Medical College & Hospital, Perundurai")
      ?: "Govt. Erode Medical College & Hospital, Perundurai"

  fun getIfhrmsId(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_IFHRMS_ID, "TN-MES-PSYCH-HOD-01") ?: "TN-MES-PSYCH-HOD-01"

  fun getDefaultYearOnLaunch(context: Context): Long =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getLong(KEY_DEFAULT_START_YEAR, 2026L)

  fun getExternalCalendarFeedUrl(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(
        KEY_EXTERNAL_CALENDAR_FEED_URL,
        "https://date.nager.at/api/v3/PublicHolidays/2027/IN"
      ) ?: "https://date.nager.at/api/v3/PublicHolidays/2027/IN"

  fun setExternalCalendarFeedUrl(context: Context, url: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_EXTERNAL_CALENDAR_FEED_URL, url.trim())
      .apply()
  }

  fun isAutoSyncOnLaunchEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_AUTO_SYNC_ON_LAUNCH, true)

  fun setAutoSyncOnLaunchEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_AUTO_SYNC_ON_LAUNCH, enabled)
      .apply()
  }

  fun isCloudFirestoreSyncEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_CLOUD_FIRESTORE_SYNC, true)

  fun setCloudFirestoreSyncEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_CLOUD_FIRESTORE_SYNC, enabled)
      .apply()
  }

  fun isEnforce10DayCapEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_ENFORCE_10_DAY_CAP, true)

  fun setEnforce10DayCapEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_ENFORCE_10_DAY_CAP, enabled)
      .apply()
  }

  fun isEnforceFifoColEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_ENFORCE_FIFO_COL, true)

  fun setEnforceFifoColEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_ENFORCE_FIFO_COL, enabled)
      .apply()
  }

  fun isIncludeSaturdayBridgeEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_INCLUDE_SATURDAY_BRIDGE, true)

  fun setIncludeSaturdayBridgeEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_INCLUDE_SATURDAY_BRIDGE, enabled)
      .apply()
  }

  fun saveProfile(
    context: Context,
    officerName: String,
    designation: String,
    department: String,
    college: String,
    ifhrmsId: String,
    defaultYear: Long
  ) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_OFFICER_NAME, officerName.trim())
      .putString(KEY_DESIGNATION, designation.trim())
      .putString(KEY_DEPARTMENT, department.trim())
      .putString(KEY_COLLEGE, college.trim())
      .putString(KEY_IFHRMS_ID, ifhrmsId.trim())
      .putLong(KEY_DEFAULT_START_YEAR, defaultYear)
      .apply()
  }
}

@Composable
fun EnterpriseSettingsDialog(
  state: LeaveUiState.Success,
  userEmail: String,
  holidaySyncStatus: HolidaySyncStatus,
  onTriggerHolidaySync: (String) -> Unit,
  onAddLocalHolidayToRoom: (TnHolidayItem) -> Unit,
  onSelectYear: (Long) -> Unit,
  onOpenPdfExport: () -> Unit,
  onOpenNotificationCenter: () -> Unit = {},
  onLockVaultNow: () -> Unit,
  onSignOut: () -> Unit,
  onShowStatusBanner: (String) -> Unit,
  onProfileUpdated: () -> Unit = {},
  onDismiss: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 18.dp)
        .testTag("enterprise_settings_dialog"),
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.background,
      tonalElevation = 6.dp
    ) {
      EnterpriseSettingsPageContent(
        state = state,
        userEmail = userEmail,
        holidaySyncStatus = holidaySyncStatus,
        onTriggerHolidaySync = onTriggerHolidaySync,
        onAddLocalHolidayToRoom = onAddLocalHolidayToRoom,
        onSelectYear = onSelectYear,
        onOpenPdfExport = onOpenPdfExport,
        onOpenNotificationCenter = onOpenNotificationCenter,
        onLockVaultNow = onLockVaultNow,
        onSignOut = onSignOut,
        onShowStatusBanner = onShowStatusBanner,
        onProfileUpdated = onProfileUpdated,
        onClose = onDismiss
      )
    }
  }
}

@Composable
fun EnterpriseSettingsPageContent(
  state: LeaveUiState.Success,
  userEmail: String,
  holidaySyncStatus: HolidaySyncStatus,
  onTriggerHolidaySync: (String) -> Unit,
  onAddLocalHolidayToRoom: (TnHolidayItem) -> Unit,
  onSelectYear: (Long) -> Unit,
  onOpenPdfExport: () -> Unit,
  onOpenNotificationCenter: () -> Unit = {},
  onLockVaultNow: () -> Unit,
  onSignOut: () -> Unit,
  onShowStatusBanner: (String) -> Unit,
  onProfileUpdated: () -> Unit = {},
  onClose: (() -> Unit)? = null
) {
  var activeSubTab by rememberSaveable { mutableIntStateOf(0) }

  val gh2026 = InitialLeaveSeedData.tnGovtPublicHolidays2026.size
  val rh2026 = InitialLeaveSeedData.tnRestrictedHolidays2026.size
  val gh2027 = TnHolidays2027Catalog.tnGovtPublicHolidays2027.size
  val rh2027 = TnHolidays2027Catalog.tnRestrictedHolidays2027.size

  val settingsSubTabs = remember(gh2026, rh2026, gh2027, rh2027) {
    listOf(
      SubTabOption(
        index = 0,
        title = "Profile & Prefs",
        badge = "HOD",
        accentColor = BadgeColCyan
      ),
      SubTabOption(
        index = 1,
        title = "Notifications",
        badge = "ALERTS",
        accentColor = BadgePlGold
      ),
      SubTabOption(
        index = 2,
        title = "Data & Calendar Sync",
        badge = "${gh2026 + gh2027} GH",
        accentColor = EmeraldOptimal
      ),
      SubTabOption(
        index = 3,
        title = "App Config & Security",
        badge = "RULES",
        accentColor = BadgeOnDutyBlue
      )
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .testTag("dedicated_settings_screen")
  ) {
    // Dedicated Settings Banner Header
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 3.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          if (onClose != null) {
            IconButton(
              onClick = onClose,
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("back_from_settings_btn")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Dashboard",
                modifier = Modifier.size(18.dp)
              )
            }
          } else {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  Brush.linearGradient(
                    listOf(EmeraldOptimal, BadgeColCyan)
                  )
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Column {
            Text(
              text = "DEDICATED SETTINGS & GOVERNANCE",
              style = MaterialTheme.typography.labelSmall,
              color = EmeraldOptimal,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = "Profile, Alerts, Calendar Sync & Config",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        if (onClose != null) {
          IconButton(
            onClick = onClose,
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .testTag("close_settings_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Settings",
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("settings_lazy_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Sub-Tab Switcher
      item {
        EnterpriseSubTabSwitcher(
          tabs = settingsSubTabs,
          selectedIndex = activeSubTab,
          onSelectTab = { activeSubTab = it }
        )
      }

      when (activeSubTab) {
        0 -> {
          // SUB-TAB 0: USER PROFILE PREFERENCES
          item {
            OfficerAndCollegeProfileSubTab(
              userEmail = userEmail,
              onSelectYear = onSelectYear,
              onProfileUpdated = onProfileUpdated,
              onShowStatusBanner = onShowStatusBanner
            )
          }
        }

        1 -> {
          // SUB-TAB 1: NOTIFICATION TOGGLES & ALERT CADENCE
          item {
            NotificationTogglesSubTab(
              onOpenNotificationCenter = onOpenNotificationCenter,
              onShowStatusBanner = onShowStatusBanner
            )
          }
        }

        2 -> {
          // SUB-TAB 2: DATA SYNCHRONIZATION & EXTERNAL CALENDAR FETCHING
          item {
            DataAndCalendarSyncSubTab(
              state = state,
              holidaySyncStatus = holidaySyncStatus,
              gh2026 = gh2026,
              rh2026 = rh2026,
              gh2027 = gh2027,
              rh2027 = rh2027,
              selectedYear = state.selectedYear,
              onSelectYear = onSelectYear,
              onTriggerHolidaySync = onTriggerHolidaySync,
              onAddLocalHolidayToRoom = onAddLocalHolidayToRoom,
              onShowStatusBanner = onShowStatusBanner
            )
          }
        }

        3 -> {
          // SUB-TAB 3: APPLICATION CONFIGURATIONS, SECURITY & EXPORT
          item {
            AppConfigSecurityAndExportSubTab(
              state = state,
              userEmail = userEmail,
              onLockVaultNow = onLockVaultNow,
              onOpenPdfExport = onOpenPdfExport,
              onSignOut = onSignOut,
              onShowStatusBanner = onShowStatusBanner
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(48.dp))
      }
    }
  }
}

@Composable
private fun OfficerAndCollegeProfileSubTab(
  userEmail: String,
  onSelectYear: (Long) -> Unit,
  onProfileUpdated: () -> Unit,
  onShowStatusBanner: (String) -> Unit
) {
  val context = LocalContext.current
  var officerName by remember { mutableStateOf(HodProfileSettingsStore.getOfficerName(context)) }
  var designation by remember { mutableStateOf(HodProfileSettingsStore.getDesignation(context)) }
  var department by remember { mutableStateOf(HodProfileSettingsStore.getDepartment(context)) }
  var college by remember { mutableStateOf(HodProfileSettingsStore.getCollege(context)) }
  var ifhrmsId by remember { mutableStateOf(HodProfileSettingsStore.getIfhrmsId(context)) }
  var defaultYear by remember {
    mutableLongStateOf(HodProfileSettingsStore.getDefaultYearOnLaunch(context))
  }
  var openingEl2023 by remember {
    mutableStateOf(LedgerEnrichmentStore.getOpeningElBalance2023(context).toString())
  }
  var surrenderedEl by remember {
    mutableStateOf(LedgerEnrichmentStore.getElSurrenderedDays(context).toString())
  }

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, BadgeColCyan.copy(alpha = 0.35f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("settings_profile_section")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = BadgeColCyan,
          modifier = Modifier.size(20.dp)
        )
        Column {
          Text(
            text = "USER PROFILE PREFERENCES & HOD CREDENTIALS",
            style = MaterialTheme.typography.labelMedium,
            color = BadgeColCyan,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = "Printed on Official A4 PDF Leave Audit Reports & Dean's Digests ($userEmail)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      OutlinedTextField(
        value = officerName,
        onValueChange = { officerName = it },
        label = { Text("Officer Name & Qualification") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_officer_name_input")
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = designation,
          onValueChange = { designation = it },
          label = { Text("Designation") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
          value = ifhrmsId,
          onValueChange = { ifhrmsId = it },
          label = { Text("IFHRMS / GPF ID") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f)
        )
      }

      OutlinedTextField(
        value = department,
        onValueChange = { department = it },
        label = { Text("Department") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      )

      OutlinedTextField(
        value = college,
        onValueChange = { college = it },
        label = { Text("Medical College & Hospital") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      // Default Service Year Preference
      Text(
        text = "Default Active Service Year Preference",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min)
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(2026L to "2026 Active Year", 2027L to "2027 Rollover Year").forEach { (yr, label) ->
          val isSelected = defaultYear == yr
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(11.dp))
              .background(if (isSelected) EmeraldOptimal else Color.Transparent)
              .clickable {
                defaultYear = yr
                onSelectYear(yr)
              }
              .padding(vertical = 10.dp, horizontal = 8.dp)
              .testTag("profile_default_year_$yr"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelMedium,
              color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      Text(
        text = "Earned Leave (EL) Career Passbook Calibration",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = openingEl2023,
          onValueChange = { openingEl2023 = it.filter { ch -> ch.isDigit() } },
          label = { Text("Opening EL (Jan 2023)") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
          value = surrenderedEl,
          onValueChange = { surrenderedEl = it.filter { ch -> ch.isDigit() } },
          label = { Text("Surrendered EL Days") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f)
        )
      }

      Button(
        onClick = {
          HodProfileSettingsStore.saveProfile(
            context = context,
            officerName = officerName,
            designation = designation,
            department = department,
            college = college,
            ifhrmsId = ifhrmsId,
            defaultYear = defaultYear
          )
          val opEl = openingEl2023.toIntOrNull()?.coerceIn(0, 240) ?: 142
          val surEl = surrenderedEl.toIntOrNull()?.coerceIn(0, 240) ?: 15
          LedgerEnrichmentStore.setOpeningElBalance2023(context, opEl)
          LedgerEnrichmentStore.setElSurrenderedDays(context, surEl)
          onProfileUpdated()
          onShowStatusBanner("Saved User Profile ($officerName) & EL Passbook preferences!")
          Toast.makeText(
            context,
            "Saved User Profile & EL Passbook preferences",
            Toast.LENGTH_SHORT
          ).show()
        },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("save_hod_profile_btn")
      ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save User Profile & Preferences", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun NotificationTogglesSubTab(
  onOpenNotificationCenter: () -> Unit,
  onShowStatusBanner: (String) -> Unit
) {
  val context = LocalContext.current
  var hasOsPermission by remember {
    mutableStateOf(LeaveNotificationHelper.hasNotificationPermission(context))
  }
  var masterEnabled by remember {
    mutableStateOf(LeaveNotificationHelper.isMasterNotificationsEnabled(context))
  }
  var aiBridgeAlerts by remember {
    mutableStateOf(LeaveNotificationHelper.isAutoNotifyAiEnabled(context))
  }
  var colExpiryAlerts by remember {
    mutableStateOf(LeaveNotificationHelper.isColExpiryAlertEnabled(context))
  }
  var upcomingHolidayAlerts by remember {
    mutableStateOf(LeaveNotificationHelper.isUpcomingHolidayAlertEnabled(context))
  }
  var yearEndBurnAlerts by remember {
    mutableStateOf(LeaveNotificationHelper.isYearEndBurnDownAlertEnabled(context))
  }
  var calendarSyncAlerts by remember {
    mutableStateOf(LeaveNotificationHelper.isCalendarSyncAlertEnabled(context))
  }
  var leadDays by remember {
    mutableIntStateOf(LeaveNotificationHelper.getLeadDaysPreference(context))
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasOsPermission = granted
    onShowStatusBanner(
      if (granted) "System Push Notification permission granted!"
      else "Notification permission denied by device settings"
    )
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.testTag("settings_notifications_section")
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      border = BorderStroke(1.dp, BadgePlGold.copy(alpha = 0.40f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = BadgePlGold,
              modifier = Modifier.size(22.dp)
            )
            Column {
              Text(
                text = "NOTIFICATION TOGGLES & ALERT CADENCE",
                style = MaterialTheme.typography.labelMedium,
                color = BadgePlGold,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = if (hasOsPermission) "Android Push Channels Active" else "Runtime Permission Required",
                style = MaterialTheme.typography.labelSmall,
                color = if (hasOsPermission) EmeraldOptimal else StatusAlertRed
              )
            }
          }

          if (!hasOsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OutlinedButton(
              onClick = {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              },
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Grant Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Master Switch
        SettingToggleRow(
          title = "Master Smart Push Notifications",
          subtitle = "Enable all TN MedLeave Pro push alerts and scheduled reminders",
          checked = masterEnabled,
          testTag = "toggle_master_notifications",
          onCheckedChange = {
            masterEnabled = it
            LeaveNotificationHelper.setMasterNotificationsEnabled(context, it)
            onShowStatusBanner(
              if (it) "Enabled Master Smart Push Notifications"
              else "Paused Master Smart Push Notifications"
            )
          }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Granular Category Toggles
        SettingToggleRow(
          title = "AI Leave Bridge & Long-Weekend Alerts",
          subtitle = "High-priority alerts for 3x–4.5x ROI CL & RH/RL bridge windows",
          checked = aiBridgeAlerts && masterEnabled,
          testTag = "toggle_ai_bridge_alerts",
          onCheckedChange = {
            aiBridgeAlerts = it
            LeaveNotificationHelper.setAutoNotifyAiEnabled(context, it)
          }
        )

        SettingToggleRow(
          title = "6-Month (180-Day) COL ↔ GH Worked Expiry Alerts",
          subtitle = "Warnings before unpaired Sunday / GH hospital duty credits expire",
          checked = colExpiryAlerts && masterEnabled,
          testTag = "toggle_col_expiry_alerts",
          onCheckedChange = {
            colExpiryAlerts = it
            LeaveNotificationHelper.setColExpiryAlertEnabled(context, it)
          }
        )

        SettingToggleRow(
          title = "Upcoming TN Government (GH) & Restricted (RH/RL) Alerts",
          subtitle = "Advance notice for gazetted public and religious festival holidays",
          checked = upcomingHolidayAlerts && masterEnabled,
          testTag = "toggle_upcoming_holiday_alerts",
          onCheckedChange = {
            upcomingHolidayAlerts = it
            LeaveNotificationHelper.setUpcomingHolidayAlertEnabled(context, it)
          }
        )

        SettingToggleRow(
          title = "Year-End December CL & RH/RL Burn-Down Reminders",
          subtitle = "Alerts in Nov–Dec if any of your 12 CL or 3 RH annual credits are unavailed",
          checked = yearEndBurnAlerts && masterEnabled,
          testTag = "toggle_year_end_burn_alerts",
          onCheckedChange = {
            yearEndBurnAlerts = it
            LeaveNotificationHelper.setYearEndBurnDownAlertEnabled(context, it)
          }
        )

        SettingToggleRow(
          title = "External Calendar & 2027 G.O. Sync Notifications",
          subtitle = "Notify when new 2026/2027 TN Govt holidays are synced into Room DB",
          checked = calendarSyncAlerts && masterEnabled,
          testTag = "toggle_calendar_sync_alerts",
          onCheckedChange = {
            calendarSyncAlerts = it
            LeaveNotificationHelper.setCalendarSyncAlertEnabled(context, it)
          }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Advance Lead Time Selector
        Text(
          text = "Advance Alert Lead Window (Days Before Holiday / Bridge)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(1, 3, 7, 14).forEach { days ->
            val selected = leadDays == days
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(11.dp))
                .background(if (selected) EmeraldOptimal else Color.Transparent)
                .clickable {
                  leadDays = days
                  LeaveNotificationHelper.setLeadDaysPreference(context, days)
                  onShowStatusBanner("Updated notification lead time to $days days before holiday")
                }
                .padding(vertical = 9.dp, horizontal = 4.dp)
                .testTag("lead_days_option_$days"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "$days Days",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }

        // Action Row: Send Test Notification & Open Alert Center
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              val sent = LeaveNotificationHelper.sendTestVerificationNotification(context)
              if (sent) {
                onShowStatusBanner("Dispatched live test notification to Android status bar!")
              } else {
                onShowStatusBanner("Enable Master Notifications & OS Permission to dispatch push alerts")
              }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
            modifier = Modifier
              .weight(1f)
              .testTag("send_test_notification_btn")
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Send Test Alert",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }

          OutlinedButton(
            onClick = onOpenNotificationCenter,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("open_alert_center_from_settings_btn")
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Open Alert Center",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DataAndCalendarSyncSubTab(
  state: LeaveUiState.Success,
  holidaySyncStatus: HolidaySyncStatus,
  gh2026: Int,
  rh2026: Int,
  gh2027: Int,
  rh2027: Int,
  selectedYear: Long,
  onSelectYear: (Long) -> Unit,
  onTriggerHolidaySync: (String) -> Unit,
  onAddLocalHolidayToRoom: (TnHolidayItem) -> Unit,
  onShowStatusBanner: (String) -> Unit
) {
  val context = LocalContext.current
  var autoRolloverEnabled by remember {
    mutableStateOf(TnHolidayBackgroundSyncService.isAutoYearRolloverSyncEnabled(context))
  }
  var autoSyncOnLaunch by remember {
    mutableStateOf(HodProfileSettingsStore.isAutoSyncOnLaunchEnabled(context))
  }
  var cloudFirestoreSync by remember {
    mutableStateOf(HodProfileSettingsStore.isCloudFirestoreSyncEnabled(context))
  }
  var externalFeedUrl by remember {
    mutableStateOf(HodProfileSettingsStore.getExternalCalendarFeedUrl(context))
  }
  var newHolDateIso by remember { mutableStateOf("2027-03-29") }
  var newHolNameEn by remember { mutableStateOf("") }
  var newHolNameTa by remember { mutableStateOf("") }
  var newHolIsRh by remember { mutableStateOf(true) }

  val feedPresets = listOf(
    "Nager.Date 2027 API" to "https://date.nager.at/api/v3/PublicHolidays/2027/IN",
    "TN Govt Portal (G.O.)" to "https://www.tn.gov.in/holiday",
    "2026–2027 AI G.O. Sync" to "Fetch and verify 2026 and 2027 Tamil Nadu Government Public Holidays (GH) and Restricted Holidays (RH/RL)"
  )

  Column(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.testTag("settings_data_sync_section")
  ) {
    // 1. External Calendar Fetching & Synchronization Options Card
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.40f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CloudSync,
              contentDescription = null,
              tint = EmeraldOptimal,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "EXTERNAL CALENDAR FETCHING & DATA SYNC",
              style = MaterialTheme.typography.labelMedium,
              color = EmeraldOptimal,
              fontWeight = FontWeight.ExtraBold
            )
          }
          Text(
            text = holidaySyncStatus.lastSyncedFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // 4 Uniform Pills showing 2026 GH, 2026 RL, 2027 GH, 2027 RL
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          SettingsMetricPill(
            label = "2026 GH",
            value = "$gh2026 dates",
            accent = BadgePlGold,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          )
          SettingsMetricPill(
            label = "2026 RH/RL",
            value = "$rh2026 dates",
            accent = BadgeRhSlate,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          )
          SettingsMetricPill(
            label = "2027 GH",
            value = "$gh2027 dates",
            accent = EmeraldOptimal,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          )
          SettingsMetricPill(
            label = "2027 RH/RL",
            value = "$rh2027 dates",
            accent = BadgeColCyan,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          )
        }

        Text(
          text = holidaySyncStatus.statusSummary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Feed Source Preset Selector
        Text(
          text = "External Calendar Source Presets",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          feedPresets.forEachIndexed { idx, (label, presetUrl) ->
            val isSelected = externalFeedUrl == presetUrl
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) {
                EmeraldOptimal.copy(alpha = 0.18f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
              },
              border = BorderStroke(
                1.dp,
                if (isSelected) EmeraldOptimal else Color.Transparent
              ),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  externalFeedUrl = presetUrl
                  HodProfileSettingsStore.setExternalCalendarFeedUrl(context, presetUrl)
                }
                .testTag("calendar_feed_preset_$idx")
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSelected) EmeraldOptimal else MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = externalFeedUrl,
          onValueChange = {
            externalFeedUrl = it
            HodProfileSettingsStore.setExternalCalendarFeedUrl(context, it)
          },
          label = { Text("External Calendar Endpoint URL or TN G.O. Query") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("external_calendar_url_input")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              HodProfileSettingsStore.setExternalCalendarFeedUrl(context, externalFeedUrl)
              onTriggerHolidaySync(
                externalFeedUrl.ifBlank {
                  "Fetch and verify both 2026 and 2027 Tamil Nadu Government Public Holidays (GH) and Restricted Holidays (RH/RL)"
                }
              )
            },
            enabled = !holidaySyncStatus.isSyncing,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
            modifier = Modifier
              .weight(1f)
              .testTag("settings_sync_2026_2027_btn")
          ) {
            if (holidaySyncStatus.isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White
              )
            } else {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (holidaySyncStatus.isSyncing) "Syncing..." else "Fetch & Sync Now",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }

          OutlinedButton(
            onClick = {
              val nextYr = if (selectedYear == 2027L) 2026L else 2027L
              onSelectYear(nextYr)
              onShowStatusBanner("Switched active service year to $nextYr")
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("settings_toggle_2027_year_btn")
          ) {
            Icon(
              Icons.Default.CalendarMonth,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (selectedYear == 2027L) "Active: 2027 (→26)" else "Preview 2027 Year",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Synchronization Toggles
        SettingToggleRow(
          title = "Auto-Fetch GH & RH/RL on Year Rollover (2027+)",
          subtitle = "Automatically fetches new calendar year's TN Govt G.O. holidays into Room DB",
          checked = autoRolloverEnabled,
          testTag = "toggle_auto_rollover_sync",
          onCheckedChange = { enabled ->
            autoRolloverEnabled = enabled
            TnHolidayBackgroundSyncService.setAutoYearRolloverSyncEnabled(context, enabled)
            onShowStatusBanner(
              if (enabled) "Enabled automatic 2027+ year-rollover holiday sync"
              else "Paused automatic year-rollover holiday sync"
            )
          }
        )

        SettingToggleRow(
          title = "Background External Calendar Sync on Launch",
          subtitle = "Checks external holiday feed on app startup if cache is older than 7 days",
          checked = autoSyncOnLaunch,
          testTag = "toggle_auto_sync_launch",
          onCheckedChange = { enabled ->
            autoSyncOnLaunch = enabled
            HodProfileSettingsStore.setAutoSyncOnLaunchEnabled(context, enabled)
          }
        )

        SettingToggleRow(
          title = "Real-Time Cloud Firestore Ledger Sync",
          subtitle = "Continuously synchronizes your 2023–2027 leave entries across authenticated devices",
          checked = cloudFirestoreSync,
          testTag = "toggle_cloud_firestore_sync",
          onCheckedChange = { enabled ->
            cloudFirestoreSync = enabled
            HodProfileSettingsStore.setCloudFirestoreSyncEnabled(context, enabled)
          }
        )
      }
    }

    // 2. Add Custom 2026 or 2027 Collectorate / DME Local Holiday into Room DB
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Add Local Collectorate / DME Holiday to Room DB",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (newHolIsRh) BadgeRhSlate.copy(alpha = 0.16f) else EmeraldOptimal.copy(alpha = 0.16f),
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable { newHolIsRh = !newHolIsRh }
          ) {
            Text(
              text = if (newHolIsRh) "Type: RH / RL (Tap for GH)" else "Type: Public GH (Tap for RL)",
              style = MaterialTheme.typography.labelSmall,
              color = if (newHolIsRh) BadgeRhSlate else EmeraldOptimal,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newHolDateIso,
            onValueChange = { newHolDateIso = it.trim() },
            label = { Text("Date (YYYY-MM-DD)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = newHolNameEn,
            onValueChange = { newHolNameEn = it },
            label = { Text("Holiday Name (English)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1.3f)
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = newHolNameTa,
            onValueChange = { newHolNameTa = it },
            label = { Text("Tamil Name (Optional)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              val parsed = runCatching { LocalDate.parse(newHolDateIso) }.getOrNull()
              if (parsed != null && newHolNameEn.isNotBlank()) {
                val item = TnHolidayItem(
                  dateIso = newHolDateIso,
                  dayOfWeek = parsed.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                  nameEnglish = newHolNameEn.trim(),
                  nameTamil = newHolNameTa.ifBlank { newHolNameEn.trim() },
                  isRestrictedHoliday = newHolIsRh,
                  monthNameTamil = parsed.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                )
                onAddLocalHolidayToRoom(item)
                newHolNameEn = ""
                newHolNameTa = ""
              } else {
                Toast.makeText(
                  context,
                  "Enter a valid YYYY-MM-DD date and holiday name",
                  Toast.LENGTH_SHORT
                ).show()
              }
            },
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(if (newHolIsRh) "+ Add RL/RH" else "+ Add GH", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 3. Export External Device Calendar (.ICS)
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "External Google / Apple Calendar (.ICS) Synchronization",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        OutlinedButton(
          onClick = {
            CalendarIcsSyncService.exportAndShareIcsFile(context, state.allRecords)
            onShowStatusBanner("Exported 2026–2027 Leave & TN Holiday .ICS file")
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            "Export 2026–2027 .ICS to Google / iOS Calendar",
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun AppConfigSecurityAndExportSubTab(
  state: LeaveUiState.Success,
  userEmail: String,
  onLockVaultNow: () -> Unit,
  onOpenPdfExport: () -> Unit,
  onSignOut: () -> Unit,
  onShowStatusBanner: (String) -> Unit
) {
  val context = LocalContext.current
  var biometricEnabled by remember {
    mutableStateOf(BiometricLockManager.isBiometricLockEnabled(context))
  }
  var autoLockOnBg by remember {
    mutableStateOf(BiometricLockManager.isAutoLockOnBackgroundEnabled(context))
  }
  var enforce10DayCap by remember {
    mutableStateOf(HodProfileSettingsStore.isEnforce10DayCapEnabled(context))
  }
  var enforceFifoCol by remember {
    mutableStateOf(HodProfileSettingsStore.isEnforceFifoColEnabled(context))
  }
  var includeSaturdayBridge by remember {
    mutableStateOf(HodProfileSettingsStore.isIncludeSaturdayBridgeEnabled(context))
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.testTag("settings_app_config_section")
  ) {
    // 1. Application Calculation & Rule Configurations Card
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      border = BorderStroke(1.dp, BadgeOnDutyBlue.copy(alpha = 0.35f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = BadgeOnDutyBlue,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "APPLICATION CONFIGURATIONS & RULE ENGINE",
            style = MaterialTheme.typography.labelMedium,
            color = BadgeOnDutyBlue,
            fontWeight = FontWeight.ExtraBold
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SettingsMetricPill(
            "Annual CL",
            "12 Days",
            BadgeClGreen,
            Modifier
              .weight(1f)
              .fillMaxHeight()
          )
          SettingsMetricPill(
            "Annual RH/RL",
            "3 Days",
            BadgeRhSlate,
            Modifier
              .weight(1f)
              .fillMaxHeight()
          )
          SettingsMetricPill(
            "Max Stretch",
            "10 Days",
            EmeraldOptimal,
            Modifier
              .weight(1f)
              .fillMaxHeight()
          )
          SettingsMetricPill(
            "COL Expiry",
            "6 Months",
            BadgeColCyan,
            Modifier
              .weight(1f)
              .fillMaxHeight()
          )
        }

        SettingToggleRow(
          title = "Enforce TN 10-Day Continuous Absence Guardrail",
          subtitle = "Warns when combined CL + RH + intervening weekends/GH exceed 10 days at a stretch",
          checked = enforce10DayCap,
          testTag = "toggle_enforce_10_day_cap",
          onCheckedChange = {
            enforce10DayCap = it
            HodProfileSettingsStore.setEnforce10DayCapEnabled(context, it)
          }
        )

        SettingToggleRow(
          title = "Strict 180-Day FIFO COL ↔ GH Worked Reconciliation",
          subtitle = "Pairs earliest unavailed Sunday / GH hospital duty credit first within 6 months",
          checked = enforceFifoCol,
          testTag = "toggle_enforce_fifo_col",
          onCheckedChange = {
            enforceFifoCol = it
            HodProfileSettingsStore.setEnforceFifoColEnabled(context, it)
          }
        )

        SettingToggleRow(
          title = "Include Saturdays in Long-Weekend Bridge Splicing",
          subtitle = "Treats Saturdays + Sundays as non-debit weekend anchors in the Optimizer",
          checked = includeSaturdayBridge,
          testTag = "toggle_include_saturday_bridge",
          onCheckedChange = {
            includeSaturdayBridge = it
            HodProfileSettingsStore.setIncludeSaturdayBridgeEnabled(context, it)
          }
        )
      }
    }

    // 2. Biometric Security Card
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.35f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            tint = EmeraldOptimal,
            modifier = Modifier.size(22.dp)
          )
          Text(
            text = "BIOMETRIC VAULT & SESSION SECURITY",
            style = MaterialTheme.typography.labelMedium,
            color = EmeraldOptimal,
            fontWeight = FontWeight.ExtraBold
          )
        }

        SettingToggleRow(
          title = "Require Biometric Unlock on Launch",
          subtitle = "Protects HOD Leave Ledger & SR Vault with Fingerprint / Face Unlock",
          checked = biometricEnabled,
          testTag = "toggle_biometric_unlock",
          onCheckedChange = {
            biometricEnabled = it
            BiometricLockManager.setBiometricLockEnabled(context, it)
            onShowStatusBanner(
              if (it) "Biometric Vault Lock enabled" else "Biometric Vault Lock disabled"
            )
          }
        )

        SettingToggleRow(
          title = "Auto-Lock When App Minimized",
          subtitle = "Immediately locks the ledger when switching apps",
          checked = autoLockOnBg,
          testTag = "toggle_biometric_autolock",
          onCheckedChange = {
            autoLockOnBg = it
            BiometricLockManager.setAutoLockOnBackgroundEnabled(context, it)
          }
        )

        OutlinedButton(
          onClick = onLockVaultNow,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Lock Biometric Vault Now", fontWeight = FontWeight.Bold)
        }
      }
    }

    // 3. Official PDF, Digest & Account Card
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "OFFICIAL EXPORT & ACCOUNT ($userEmail)",
          style = MaterialTheme.typography.labelMedium,
          color = EmeraldOptimal,
          fontWeight = FontWeight.ExtraBold
        )

        Button(
          onClick = onOpenPdfExport,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            Icons.Default.PictureAsPdf,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Generate Official A4 PDF Leave & COL Audit Report", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = {
            val elPassbook = ElMasterPassbookEngine.computePassbook(
              allRecords = state.allRecords,
              openingBalanceJan2023 = LedgerEnrichmentStore.getOpeningElBalance2023(context),
              elSurrenderedDays = LedgerEnrichmentStore.getElSurrenderedDays(context)
            )
            val digest = ExecutiveDigestShareService.buildExecutiveQuotaDigest(
              summary = state.yearSummary,
              audit = state.colGhAuditYear,
              elNetBalance = elPassbook.currentNetElBalance
            )
            ExecutiveDigestShareService.copyDigestToClipboard(context, "HOD Leave Digest", digest)
            onShowStatusBanner("Copied HOD Executive Leave Digest to Clipboard!")
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Copy HOD Executive Digest for Dean's Office", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onSignOut,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, StatusAlertRed.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = StatusAlertRed,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out of Session", color = StatusAlertRed, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  testTag: String,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = EmeraldOptimal
      ),
      modifier = Modifier.testTag(testTag)
    )
  }
}

@Composable
private fun SettingsMetricPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = accent.copy(alpha = 0.10f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium.copy(
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          lineHeight = 14.sp
        ),
        fontWeight = FontWeight.ExtraBold,
        color = accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
