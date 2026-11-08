package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.LeaveRepository
import com.example.domain.BiometricLockManager
import androidx.compose.material.icons.filled.Psychology
import com.example.ui.AiAdvisorChatTabScreen
import com.example.ui.AiChatViewModel
import com.example.ui.AddLeaveRecordDialog
import com.example.ui.AuthScreen
import com.example.ui.ColGhAuditTabScreen
import com.example.ui.IosAmbientGlassBackdrop
import com.example.ui.IosCalendarTabScreen
import com.example.ui.LeaveUiState
import com.example.ui.LeaveViewModel
import com.example.ui.LedgerTabScreen
import com.example.ui.OptimizerTabScreen
import com.example.ui.OverviewTabScreen
import com.example.ui.signOutUser
import com.example.ui.theme.AppThemeController
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosDarkDockSurface
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpMicroShape
import com.example.ui.theme.IosSharpSquircleShape
import com.example.ui.theme.IosSharpTabShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.IosSquircleGlassFill
import com.example.ui.theme.LocalAppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAquaAccent
import com.example.ui.theme.NeonCyanBright
import com.example.ui.theme.NeonCyanText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppRootNavigation()
      }
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppRootNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser
  var isBiometricFrontPageAuthenticated by rememberSaveable { mutableStateOf(false) }

  if (user == null && !isBiometricFrontPageAuthenticated) {
    AuthScreen(
      onAuthSuccess = { /* AuthStateListener updates automatically */ },
      onBiometricSignInSuccess = {
        isBiometricFrontPageAuthenticated = true
      }
    )
  } else {
    val resolvedUserId = user?.uid ?: LeaveRepository.BIOMETRIC_HOD_USER_ID
    val resolvedEmail = user?.email ?: "drkasi@gmail.com"
    MainFeatureHost(
      currentUserId = resolvedUserId,
      userEmail = resolvedEmail,
      unlockedViaFrontPageBiometrics = isBiometricFrontPageAuthenticated,
      onBiometricSessionSignOut = {
        isBiometricFrontPageAuthenticated = false
      }
    )
  }
}

@Composable
fun MainFeatureHost(
  currentUserId: String,
  userEmail: String,
  unlockedViaFrontPageBiometrics: Boolean = false,
  onBiometricSessionSignOut: () -> Unit = {}
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val viewModel: LeaveViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        LeaveViewModel(LeaveRepository(db), currentUserId, app)
      }
    }
  )

  val aiChatViewModel: AiChatViewModel = viewModel(key = "ai_chat_$currentUserId")

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val aiRecommendationState by viewModel.aiRecommendationState.collectAsStateWithLifecycle()
  val holidaySyncStatus by viewModel.holidaySyncStatus.collectAsStateWithLifecycle()
  val lastDeletedRecord by viewModel.lastDeletedRecord.collectAsStateWithLifecycle()
  var currentTab by rememberSaveable { mutableIntStateOf(0) }
  var calendarInitialSubMode by rememberSaveable { mutableIntStateOf(0) }
  var showAddDialog by rememberSaveable { mutableStateOf(false) }
  var showPdfExportDialog by rememberSaveable { mutableStateOf(false) }
  var showNotificationCenterDialog by rememberSaveable { mutableStateOf(false) }
  var showBiometricSettingsDialog by rememberSaveable { mutableStateOf(false) }
  var showEnterpriseSettingsDialog by rememberSaveable { mutableStateOf(false) }
  var profileRefreshTick by rememberSaveable { mutableIntStateOf(0) }

  var isVaultLocked by rememberSaveable {
    mutableStateOf(
      if (unlockedViaFrontPageBiometrics) {
        false
      } else {
        BiometricLockManager.isBiometricLockEnabled(context)
      }
    )
  }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP) {
        if (BiometricLockManager.isBiometricLockEnabled(context) &&
          BiometricLockManager.isAutoLockOnBackgroundEnabled(context)
        ) {
          isVaultLocked = true
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  if (isVaultLocked) {
    com.example.ui.BiometricVaultLockScreen(
      userEmail = userEmail,
      onUnlocked = { isVaultLocked = false }
    )
    return
  }

  if (showEnterpriseSettingsDialog) {
    BackHandler {
      showEnterpriseSettingsDialog = false
    }
  } else if (currentTab != 0) {
    BackHandler {
      currentTab = 0
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      IosExecutiveTopBar(
        currentTab = currentTab,
        isSettingsOpen = showEnterpriseSettingsDialog,
        profileRefreshTick = profileRefreshTick,
        userEmail = userEmail,
        onOpenBiometricLock = { showBiometricSettingsDialog = true },
        onOpenNotifications = { showNotificationCenterDialog = true },
        onOpenPdfExport = { showPdfExportDialog = true },
        onOpenSettings = { showEnterpriseSettingsDialog = !showEnterpriseSettingsDialog },
        onSignOut = {
          onBiometricSessionSignOut()
          signOutUser(
            context = context,
            credentialManager = credentialManager,
            onSignOutComplete = {},
            scope = scope
          )
        }
      )
    },
    bottomBar = {
      IosSquircleBottomTabBar(
        selectedTab = if (showEnterpriseSettingsDialog) -1 else currentTab,
        onSelectTab = {
          showEnterpriseSettingsDialog = false
          currentTab = it
        }
      )
    },
    floatingActionButton = {
      if (currentTab !in listOf(3, 5) && !showEnterpriseSettingsDialog) {
        FloatingActionButton(
          onClick = { showAddDialog = true },
          shape = IosSharpButtonShape,
          containerColor = EmeraldOptimal,
          contentColor = Color.White,
          modifier = Modifier
            .border(1.2.dp, Color.White.copy(alpha = 0.42f), IosSharpButtonShape)
            .testTag("fab_add_leave")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
          ) {
            Box(
              modifier = Modifier
                .size(20.dp)
                .clip(IosSharpMicroShape)
                .background(Color.White.copy(alpha = 0.22f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Leave",
                modifier = Modifier.size(15.dp)
              )
            }
            Text(
              text = "Log Leave",
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-0.2).sp,
              fontSize = 13.sp,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }
  ) { innerPadding ->
    IosAmbientGlassBackdrop(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        when (val state = uiState) {
        is LeaveUiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator()
          }
        }
        is LeaveUiState.Error -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = state.message,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyLarge
            )
          }
        }
        is LeaveUiState.Success -> {
          AnimatedVisibility(visible = state.statusBannerMessage != null) {
            state.statusBannerMessage?.let { msg ->
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = EmeraldOptimal.copy(alpha = 0.15f),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 6.dp)
                  .border(1.dp, EmeraldOptimal.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = msg,
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldOptimal,
                    modifier = Modifier.weight(1f)
                  )
                  IconButton(
                    onClick = { viewModel.clearBanner() },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Dismiss notification",
                      tint = EmeraldOptimal,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }
          }

          if (showEnterpriseSettingsDialog) {
            com.example.ui.EnterpriseSettingsPageContent(
              state = state,
              userEmail = userEmail,
              holidaySyncStatus = holidaySyncStatus,
              onTriggerHolidaySync = { customUrl -> viewModel.triggerHolidaySync(customUrl) },
              onAddLocalHolidayToRoom = { item -> viewModel.addLocalHolidayToRoom(item) },
              onSelectYear = { yr -> viewModel.selectYear(yr) },
              onOpenPdfExport = { showPdfExportDialog = true },
              onOpenNotificationCenter = { showNotificationCenterDialog = true },
              onLockVaultNow = {
                showEnterpriseSettingsDialog = false
                isVaultLocked = true
              },
              onSignOut = {
                showEnterpriseSettingsDialog = false
                onBiometricSessionSignOut()
                signOutUser(
                  context = context,
                  credentialManager = credentialManager,
                  onSignOutComplete = {},
                  scope = scope
                )
              },
              onShowStatusBanner = { msg -> viewModel.showBannerMessage(msg) },
              onProfileUpdated = { profileRefreshTick++ },
              onClose = { showEnterpriseSettingsDialog = false }
            )
          } else when (currentTab) {
            0 -> OverviewTabScreen(
              state = state,
              onSelectYear = { viewModel.selectYear(it) },
              onNavigateToTab = { currentTab = it },
              onOpenAddDialog = { showAddDialog = true },
              onApplyBridge = { viewModel.applySmartBridgeToLedger(it) },
              onOpenPdfExportModal = { showPdfExportDialog = true },
              onQuickLogLeave = { type, date, notes ->
                viewModel.addLeave(
                  leaveType = type,
                  dateIso = date,
                  pairedDateIso = "",
                  notes = notes
                )
              },
              onOpenHolidayDashboard = {
                calendarInitialSubMode = 2
                currentTab = 4
              },
              onCommitBurnDownSlots = { slots ->
                viewModel.commitBurnDownSuggestions(slots)
              },
              onShowStatusBanner = { msg -> viewModel.showBannerMessage(msg) },
              onLogDualDutyAndCol = { dutyDate, colDate, dutyNotes ->
                viewModel.logDualHolidayDutyAndCol(dutyDate, colDate, dutyNotes)
              }
            )
            1 -> LedgerTabScreen(
              state = state,
              onSelectYear = { viewModel.selectYear(it) },
              onSelectCategory = { viewModel.selectCategory(it) },
              onDeleteRecord = { viewModel.deleteLeave(it) },
              onOpenAddDialog = { showAddDialog = true },
              onImportCsvRows = { rows ->
                viewModel.importCsvRowsToLedger(rows, state.allRecords)
              },
              onShowStatusBanner = { msg -> viewModel.showBannerMessage(msg) },
              lastDeletedRecord = lastDeletedRecord,
              onDeleteWithUndo = { rec -> viewModel.deleteLeaveWithUndo(rec) },
              onUndoDelete = { viewModel.undoLastDeletedLeave() },
              onDismissUndo = { viewModel.dismissUndoDelete() },
              onUpdateRecord = { updated -> viewModel.updateExistingLeaveRecord(updated) },
              onQuickLogLeave = { type, date, notes ->
                viewModel.addLeave(
                  leaveType = type,
                  dateIso = date,
                  pairedDateIso = "",
                  notes = notes
                )
              },
              onScrubDuplicates = {
                viewModel.scrubExistingDuplicatesFromLedger(state.allRecords)
              }
            )
            2 -> ColGhAuditTabScreen(
              state = state,
              onSelectYear = { viewModel.selectYear(it) },
              onPairColWithGh = { colId, ghDate ->
                viewModel.pairColWithGhWorked(colId, ghDate, state.allRecords)
              },
              onAutoReconcileYear = { targetYr ->
                viewModel.autoReconcileAllColWithGhWorked(state.allRecords, targetYr)
              },
              onAvailUnusedGhAsCol = { newColDate, ghRec, notes ->
                viewModel.logNewColAgainstUnusedGh(newColDate, ghRec, notes)
              },
              onOpenPdfExportModal = { showPdfExportDialog = true },
              onLogDualDutyAndCol = { dutyDate, colDate, dutyNotes ->
                viewModel.logDualHolidayDutyAndCol(dutyDate, colDate, dutyNotes)
              },
              onShowStatusBanner = { msg -> viewModel.showBannerMessage(msg) }
            )
            3 -> OptimizerTabScreen(
              state = state,
              aiRecommendationState = aiRecommendationState,
              onGenerateAiRecommendations = { focusGoal ->
                viewModel.generateAiLeaveRecommendations(focusGoal, state)
              },
              onRunCustomRange = { start, end ->
                viewModel.runCustomRangeAnalysis(start, end, state.summary2026)
              },
              onApplyBridge = { viewModel.applySmartBridgeToLedger(it) },
              onOpenNotificationCenter = { showNotificationCenterDialog = true },
              onShowStatusBanner = { msg -> viewModel.showBannerMessage(msg) },
              onCommitDraftLeaves = { draft -> viewModel.commitBuilderDraftToLedger(draft) },
              holidaySyncStatus = holidaySyncStatus,
              onTriggerHolidaySync = { customUrl -> viewModel.triggerHolidaySync(customUrl) },
              onAddLocalHolidayToRoom = { item -> viewModel.addLocalHolidayToRoom(item) },
              onQuickLogLeave = { type, date, notes ->
                viewModel.addLeave(
                  leaveType = type,
                  dateIso = date,
                  pairedDateIso = "",
                  notes = notes
                )
              },
              onCommitBurnDownSlots = { slots ->
                viewModel.commitBurnDownSuggestions(slots)
              },
              onCommitSimulatedWorkingDays = { cat, workingDates, notes ->
                viewModel.addWorkingDaysOnlyLeaveBatch(
                  leaveType = cat.code,
                  workingDateIsos = workingDates,
                  pairedDateIso = "",
                  notes = notes,
                  isHalfDay = false
                )
              }
            )
            4 -> IosCalendarTabScreen(
              state = state,
              onSelectYear = { viewModel.selectYear(it) },
              onQuickLogLeave = { type, date, notes ->
                viewModel.addLeave(
                  leaveType = type,
                  dateIso = date,
                  pairedDateIso = "",
                  notes = notes
                )
              },
              onOpenFullAddDialog = { showAddDialog = true },
              onCommitDraftLeaves = { draft -> viewModel.commitBuilderDraftToLedger(draft) },
              holidaySyncStatus = holidaySyncStatus,
              onTriggerHolidaySync = { customUrl -> viewModel.triggerHolidaySync(customUrl) },
              onAddLocalHolidayToRoom = { item -> viewModel.addLocalHolidayToRoom(item) },
              initialSubMode = calendarInitialSubMode,
              onApplyBridge = { viewModel.applySmartBridgeToLedger(it) }
            )
            5 -> AiAdvisorChatTabScreen(
              leaveState = state,
              chatViewModel = aiChatViewModel
            )
          }

          if (showAddDialog) {
            AddLeaveRecordDialog(
              initialYear = state.selectedYear,
              unusedGhRecords = state.colGhAuditYear.unusedGhRecords,
              existingRecords = state.allRecords,
              onDismiss = { showAddDialog = false },
              onConfirm = { type, startDate, endDate, paired, notes, status, isHalf ->
                viewModel.addLeaveEntrySpell(
                  leaveType = type,
                  startDate = startDate,
                  endDate = endDate,
                  status = status,
                  pairedDutyDate = paired,
                  reason = notes,
                  isHalfDay = isHalf
                )
                showAddDialog = false
              },
              onConfirmWorkingDaysOnly = { type, workingDates, paired, notes, isHalf ->
                viewModel.addWorkingDaysOnlyLeaveBatch(
                  leaveType = type,
                  workingDateIsos = workingDates,
                  pairedDateIso = paired,
                  notes = notes,
                  isHalfDay = isHalf
                )
                showAddDialog = false
              }
            )
          }
          if (showPdfExportDialog) {
            com.example.ui.OfficialPdfExportDialog(
              state = state,
              onDismiss = { showPdfExportDialog = false },
              onExportSuccessMessage = { msg ->
                viewModel.showBannerMessage(msg)
              }
            )
          }
          if (showNotificationCenterDialog) {
            com.example.ui.SmartNotificationCenterDialog(
              state = state,
              aiState = aiRecommendationState,
              onDismiss = { showNotificationCenterDialog = false },
              onApplyBridge = { viewModel.applySmartBridgeToLedger(it) },
              onShowStatusBanner = { msg -> viewModel.showBannerMessage(msg) }
            )
          }
          if (showBiometricSettingsDialog) {
            com.example.ui.BiometricSecuritySettingsDialog(
              onDismiss = { showBiometricSettingsDialog = false },
              onLockAppNow = { isVaultLocked = true },
              onShowStatusBanner = { msg -> viewModel.showBannerMessage(msg) }
            )
          }
        }
      }
      }
    }
  }
}

@Composable
private fun IosExecutiveTopBar(
  currentTab: Int,
  isSettingsOpen: Boolean = false,
  profileRefreshTick: Int = 0,
  userEmail: String,
  onOpenBiometricLock: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenPdfExport: () -> Unit,
  onOpenSettings: () -> Unit,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  val activeThemeMode = LocalAppThemeMode.current
  val officerHeaderSubtitle = remember(profileRefreshTick) {
    val collegeShort = com.example.ui.HodProfileSettingsStore.getCollege(context)
      .substringBefore(",")
      .uppercase()
    val desigShort = com.example.ui.HodProfileSettingsStore.getDesignation(context)
      .uppercase()
    "$collegeShort • $desigShort"
  }
  val title = if (isSettingsOpen) {
    "Enterprise Settings & Sync"
  } else when (currentTab) {
    0 -> "Executive Leave Desk"
    1 -> "2023–2027 Leave Ledger"
    2 -> "COL ↔ GH Worked Ledger"
    3 -> "Smart Bridge Optimizer"
    4 -> "iOS Leave & Holiday Calendar"
    else -> "Gemini Leave Advisor"
  }

  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    tonalElevation = 4.dp,
    shadowElevation = 4.dp,
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(
              Color.White.copy(alpha = if (activeThemeMode == AppThemeMode.DARK_OBSIDIAN) 0.04f else 0.45f),
              EmeraldOptimal.copy(alpha = 0.04f),
              Color.Transparent
            )
          )
        )
    ) {
      // iOS 18 Dynamic Island Live Activity & Theme Switcher Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left: iOS Dynamic Island Live Activity Capsule (Sharp Squircle)
        Surface(
          shape = IosSharpButtonShape,
          color = Color(0xFF06111F),
          modifier = Modifier
            .height(22.dp)
            .border(0.8.dp, Color(0xFF34D399).copy(alpha = 0.45f), IosSharpButtonShape)
            .clickable { onOpenNotifications() }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Box(
              modifier = Modifier
                .size(5.dp)
                .clip(IosSharpMicroShape)
                .background(Color(0xFF34D399))
            )
            Text(
              text = "iOS 18 PRO • IFHRMS LIVE SYNC",
              color = Color(0xFFA7F3D0),
              fontSize = 8.5.sp,
              lineHeight = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.35.sp,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        // Right: Instant 1-Tap iOS Theme Mode Cycle Pill (Green / White / Dark)
        Surface(
          shape = IosSharpButtonShape,
          color = EmeraldOptimal.copy(alpha = 0.13f),
          modifier = Modifier
            .height(22.dp)
            .clip(IosSharpButtonShape)
            .border(0.8.dp, EmeraldOptimal.copy(alpha = 0.42f), IosSharpButtonShape)
            .clickable {
              val nextMode = when (activeThemeMode) {
                AppThemeMode.GREEN_EMERALD -> AppThemeMode.WHITE_CLINICAL
                AppThemeMode.WHITE_CLINICAL -> AppThemeMode.DARK_OBSIDIAN
                AppThemeMode.DARK_OBSIDIAN -> AppThemeMode.GREEN_EMERALD
              }
              AppThemeController.setThemeMode(context, nextMode)
            }
            .testTag("top_bar_quick_theme_pill")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(IosSharpMicroShape)
                .background(EmeraldOptimal)
            )
            Text(
              text = "${activeThemeMode.shortBadge} ⟳",
              color = EmeraldOptimal,
              fontSize = 8.5.sp,
              lineHeight = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.3.sp,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }

      // Main iOS Large Title + Sharp Squircle Segmented Control Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Box(
              modifier = Modifier
                .size(5.dp)
                .clip(IosSharpMicroShape)
                .background(EmeraldOptimal)
            )
            Text(
              text = officerHeaderSubtitle,
              style = MaterialTheme.typography.labelMedium,
              color = EmeraldOptimal,
              fontSize = 9.sp,
              lineHeight = 11.sp,
              letterSpacing = 0.25.sp,
              maxLines = 1,
              softWrap = false,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.height(1.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 17.5.sp,
            lineHeight = 21.sp,
            letterSpacing = (-0.4).sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Unified iOS Sharp-Edged Squircle Control Cluster
        Surface(
          shape = IosSharpWellShape,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            shape = IosSharpWellShape
          )
        ) {
          Row(
            modifier = Modifier.padding(2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Surface(
              shape = IosSharpButtonShape,
              color = EmeraldOptimal.copy(alpha = 0.14f),
              modifier = Modifier
                .size(31.dp)
                .clip(IosSharpButtonShape)
                .border(0.9.dp, EmeraldOptimal.copy(alpha = 0.38f), IosSharpButtonShape)
                .clickable { onOpenBiometricLock() }
                .testTag("top_bar_biometric_lock_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Fingerprint,
                  contentDescription = "Biometric Lock & Security",
                  tint = EmeraldOptimal,
                  modifier = Modifier.size(15.dp)
                )
              }
            }

            Surface(
              shape = IosSharpButtonShape,
              color = EmeraldOptimal.copy(alpha = 0.14f),
              modifier = Modifier
                .size(31.dp)
                .clip(IosSharpButtonShape)
                .border(0.9.dp, EmeraldOptimal.copy(alpha = 0.38f), IosSharpButtonShape)
                .clickable { onOpenNotifications() }
                .testTag("top_bar_notifications_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.NotificationsActive,
                  contentDescription = "Holiday & Leave Push Alerts",
                  tint = EmeraldOptimal,
                  modifier = Modifier.size(15.dp)
                )
              }
            }

            Surface(
              shape = IosSharpButtonShape,
              color = EmeraldOptimal.copy(alpha = 0.18f),
              modifier = Modifier
                .height(31.dp)
                .clip(IosSharpButtonShape)
                .border(0.9.dp, EmeraldOptimal.copy(alpha = 0.48f), IosSharpButtonShape)
                .clickable { onOpenPdfExport() }
                .testTag("top_bar_pdf_export_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.PictureAsPdf,
                  contentDescription = "Export Official PDF Report",
                  tint = EmeraldOptimal,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "PDF",
                  color = EmeraldOptimal,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 10.sp,
                  lineHeight = 12.sp,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }

            Surface(
              shape = IosSharpButtonShape,
              color = if (isSettingsOpen) EmeraldOptimal else EmeraldOptimal.copy(alpha = 0.14f),
              modifier = Modifier
                .size(31.dp)
                .clip(IosSharpButtonShape)
                .border(
                  0.9.dp,
                  if (isSettingsOpen) EmeraldOptimal else EmeraldOptimal.copy(alpha = 0.42f),
                  IosSharpButtonShape
                )
                .clickable { onOpenSettings() }
                .testTag("top_bar_settings_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = "Enterprise Settings & 2027 Holiday Sync",
                  tint = if (isSettingsOpen) Color.White else EmeraldOptimal,
                  modifier = Modifier.size(15.dp)
                )
              }
            }

            Surface(
              shape = IosSharpButtonShape,
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier
                .size(31.dp)
                .clip(IosSharpButtonShape)
                .border(
                  0.9.dp,
                  MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
                  IosSharpButtonShape
                )
                .clickable { onSignOut() }
                .testTag("sign_out_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Logout,
                  contentDescription = "Sign Out",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }
        }
      }

      // Crisp iOS Hairline Separator (0.75.dp)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(0.75.dp)
          .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
      )
    }
  }
}

@Composable
private fun IosSquircleBottomTabBar(
  selectedTab: Int,
  onSelectTab: (Int) -> Unit
) {
  val themeMode = LocalAppThemeMode.current
  val isWhiteTheme = themeMode == AppThemeMode.WHITE_CLINICAL
  val isGreenTheme = themeMode == AppThemeMode.GREEN_EMERALD

  val dockBackground = when (themeMode) {
    AppThemeMode.WHITE_CLINICAL -> Color(0xFFFFFFFF).copy(alpha = 0.90f)
    AppThemeMode.GREEN_EMERALD -> Color(0xFF042F2E).copy(alpha = 0.92f)
    AppThemeMode.DARK_OBSIDIAN -> IosDarkDockSurface.copy(alpha = 0.91f)
  }
  val dockBorderColor = when (themeMode) {
    AppThemeMode.WHITE_CLINICAL -> Color(0xFFCBD5E1)
    AppThemeMode.GREEN_EMERALD -> Color(0xFF34D399).copy(alpha = 0.55f)
    AppThemeMode.DARK_OBSIDIAN -> NeonCyanText.copy(alpha = 0.45f)
  }
  val dockTopShape = IosSharpSquircleShape(
    topStart = 12.dp,
    topEnd = 12.dp,
    bottomEnd = 0.dp,
    bottomStart = 0.dp,
    sharpnessBias = 0.85f
  )

  val tabs = listOf(
    Triple(0, "Overview", Icons.Default.Dashboard),
    Triple(1, "Ledger", Icons.Default.EventNote),
    Triple(2, "COL & GH", Icons.Default.FactCheck),
    Triple(3, "Optimizer", Icons.Default.AutoAwesome),
    Triple(4, "Calendar", Icons.Default.CalendarMonth),
    Triple(5, "Advisor", Icons.Default.Psychology)
  )

  Surface(
    color = dockBackground,
    tonalElevation = 14.dp,
    shadowElevation = 20.dp,
    shape = dockTopShape,
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = 1.1.dp,
        color = dockBorderColor,
        shape = dockTopShape
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(
              Color.White.copy(alpha = if (isWhiteTheme) 0.65f else 0.06f),
              Color.Transparent,
              EmeraldOptimal.copy(alpha = 0.05f)
            )
          )
        )
        .navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top Specular Chamfer Line along the dock edge
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(
            Brush.horizontalGradient(
              colors = listOf(
                Color.Transparent,
                if (isWhiteTheme) EmeraldOptimal.copy(alpha = 0.45f) else NeonAquaAccent.copy(alpha = 0.65f),
                Color.Transparent
              )
            )
          )
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        tabs.forEachIndexed { idx, (index, label, icon) ->
          val isSelected = selectedTab == index
          val iconColor = when {
            isSelected && isWhiteTheme -> Color.White
            isSelected -> Color(0xFF041322)
            isWhiteTheme -> Color(0xFF475569)
            isGreenTheme -> Color(0xFFA7F3D0).copy(alpha = 0.88f)
            else -> NeonCyanBright.copy(alpha = 0.80f)
          }
          val labelColor = when {
            isSelected && isWhiteTheme -> EmeraldOptimal
            isSelected -> NeonAquaAccent
            isWhiteTheme -> Color(0xFF334155)
            isGreenTheme -> Color(0xFFD1FAE5).copy(alpha = 0.85f)
            else -> NeonCyanBright.copy(alpha = 0.76f)
          }
          val activePillBrush = if (isWhiteTheme) {
            Brush.linearGradient(listOf(EmeraldOptimal, Color(0xFF047857)))
          } else {
            Brush.linearGradient(listOf(NeonAquaAccent, NeonCyanText))
          }

          Column(
            modifier = Modifier
              .weight(1f)
              .clip(IosSharpTabShape)
              .background(
                if (isSelected) {
                  if (isWhiteTheme) EmeraldOptimal.copy(alpha = 0.08f)
                  else Color.White.copy(alpha = 0.06f)
                } else {
                  Color.Transparent
                }
              )
              .then(
                if (isSelected) {
                  Modifier.border(
                    width = 1.dp,
                    color = if (isWhiteTheme) EmeraldOptimal.copy(alpha = 0.45f) else NeonAquaAccent.copy(alpha = 0.55f),
                    shape = IosSharpTabShape
                  )
                } else {
                  Modifier
                }
              )
              .clickable { onSelectTab(index) }
              .padding(vertical = 4.dp, horizontal = 2.dp)
              .testTag("bottom_tab_$index"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.5.dp)
          ) {
            // Top Sharp Laser Accent Bar
            Box(
              modifier = Modifier
                .size(width = if (isSelected) 20.dp else 0.dp, height = 2.dp)
                .clip(IosSharpMicroShape)
                .background(
                  if (isSelected) (if (isWhiteTheme) EmeraldOptimal else NeonAquaAccent) else Color.Transparent
                )
            )

            // Sharp-Edged iOS Squircle Icon Button Well (6.dp sharp squircle)
            Box(
              modifier = Modifier
                .height(27.dp)
                .padding(horizontal = 5.dp)
                .fillMaxWidth()
                .clip(IosSharpButtonShape)
                .then(
                  if (isSelected) {
                    Modifier
                      .background(activePillBrush)
                      .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.55f),
                        shape = IosSharpButtonShape
                      )
                  } else {
                    Modifier.background(Color.Transparent)
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(if (isSelected) 17.dp else 16.dp)
              )
            }

            Text(
              text = label,
              style = MaterialTheme.typography.labelLarge,
              color = labelColor,
              fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
              fontSize = 10.sp,
              lineHeight = 11.5.sp,
              letterSpacing = (-0.15).sp,
              maxLines = 1,
              softWrap = false,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
          }

          // Subtle iOS Vertical Hairline Divider between tabs
          if (idx < tabs.lastIndex) {
            Box(
              modifier = Modifier
                .width(0.6.dp)
                .height(26.dp)
                .background(
                  if (isWhiteTheme) Color(0xFFE2E8F0)
                  else Color.White.copy(alpha = 0.08f)
                )
            )
          }
        }
      }

      // Authentic iOS Home Indicator Bar at the bottom edge
      Box(
        modifier = Modifier
          .padding(bottom = 3.dp)
          .width(116.dp)
          .height(3.5.dp)
          .clip(IosSharpMicroShape)
          .background(
            if (isWhiteTheme) Color(0xFFCBD5E1)
            else Color.White.copy(alpha = 0.24f)
          )
      )
    }
  }
}
