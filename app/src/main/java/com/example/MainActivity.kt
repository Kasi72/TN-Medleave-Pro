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
import com.example.ui.IosCalendarTabScreen
import com.example.ui.LeaveUiState
import com.example.ui.LeaveViewModel
import com.example.ui.LedgerTabScreen
import com.example.ui.OptimizerTabScreen
import com.example.ui.OverviewTabScreen
import com.example.ui.signOutUser
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosDarkDockSurface
import com.example.ui.theme.IosSquircleGlassFill
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
      if (currentTab != 5 && !showEnterpriseSettingsDialog) {
        FloatingActionButton(
          onClick = { showAddDialog = true },
          shape = RoundedCornerShape(20.dp),
          containerColor = EmeraldOptimal,
          contentColor = Color.White,
          modifier = Modifier.testTag("fab_add_leave")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Add, contentDescription = "Log Leave")
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Log Leave",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
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
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
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
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(EmeraldOptimal)
            )
            Text(
              text = officerHeaderSubtitle,
              style = MaterialTheme.typography.labelMedium,
              color = EmeraldOptimal,
              fontSize = 10.sp,
              lineHeight = 13.sp,
              maxLines = 1,
              softWrap = false,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 20.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = EmeraldOptimal.copy(alpha = 0.12f),
            modifier = Modifier
              .height(36.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, EmeraldOptimal.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
              .clickable { onOpenBiometricLock() }
              .testTag("top_bar_biometric_lock_button")
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 9.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Biometric Lock & Security",
                tint = EmeraldOptimal,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = EmeraldOptimal.copy(alpha = 0.12f),
            modifier = Modifier
              .height(36.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, EmeraldOptimal.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
              .clickable { onOpenNotifications() }
              .testTag("top_bar_notifications_button")
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 9.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Holiday & Leave Push Alerts",
                tint = EmeraldOptimal,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = EmeraldOptimal.copy(alpha = 0.14f),
            modifier = Modifier
              .height(36.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, EmeraldOptimal.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
              .clickable { onOpenPdfExport() }
              .testTag("top_bar_pdf_export_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = "Export Official PDF Report",
                tint = EmeraldOptimal,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "PDF",
                color = EmeraldOptimal,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                softWrap = false
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = EmeraldOptimal.copy(alpha = 0.14f),
            modifier = Modifier
              .height(36.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, EmeraldOptimal.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
              .clickable { onOpenSettings() }
              .testTag("top_bar_settings_button")
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 9.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Enterprise Settings & 2027 Holiday Sync",
                tint = EmeraldOptimal,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .height(36.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                RoundedCornerShape(12.dp)
              )
              .clickable { onSignOut() }
              .testTag("sign_out_button")
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 9.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Sign Out",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      // Subtle enterprise divider line
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
      )
    }
  }
}

@Composable
private fun IosSquircleBottomTabBar(
  selectedTab: Int,
  onSelectTab: (Int) -> Unit
) {
  val tabs = listOf(
    Triple(0, "Overview", Icons.Default.Dashboard),
    Triple(1, "Ledger", Icons.Default.EventNote),
    Triple(2, "COL & GH", Icons.Default.FactCheck),
    Triple(3, "Optimizer", Icons.Default.AutoAwesome),
    Triple(4, "Calendar", Icons.Default.CalendarMonth),
    Triple(5, "Advisor", Icons.Default.Psychology)
  )

  Surface(
    color = IosDarkDockSurface,
    tonalElevation = 12.dp,
    shadowElevation = 18.dp,
    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = 1.2.dp,
        color = NeonCyanText.copy(alpha = 0.42f),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
      )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 6.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      tabs.forEach { (index, label, icon) ->
        val isSelected = selectedTab == index
        val iconColor = if (isSelected) {
          Color(0xFF041322)
        } else {
          NeonCyanBright
        }
        val labelColor = if (isSelected) {
          NeonAquaAccent
        } else {
          NeonCyanBright
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelectTab(index) }
            .padding(vertical = 4.dp)
            .testTag("bottom_tab_$index"),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box(
            modifier = Modifier
              .height(32.dp)
              .padding(horizontal = 4.dp)
              .fillMaxWidth()
              .clip(RoundedCornerShape(13.dp))
              .then(
                if (isSelected) {
                  Modifier.background(
                    Brush.linearGradient(
                      listOf(NeonAquaAccent, NeonCyanText)
                    )
                  )
                } else {
                  Modifier
                    .background(IosSquircleGlassFill)
                    .border(
                      width = 1.dp,
                      color = NeonCyanText.copy(alpha = 0.28f),
                      shape = RoundedCornerShape(13.dp)
                    )
                }
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = label,
              tint = iconColor,
              modifier = Modifier.size(18.dp)
            )
          }
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.35.sp,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}
