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
import com.example.ui.neumorphicExtruded
import com.example.ui.neumorphicInset
import com.example.ui.signOutUser
import com.example.ui.theme.AppThemeController
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpMicroShape
import com.example.ui.theme.IosSharpSquircleShape
import com.example.ui.theme.IosSharpTabShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.LocalAppThemeMode
import com.example.ui.theme.LocalNeumorphicColors
import com.example.ui.theme.MyApplicationTheme
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
  val optimizerConstraints by viewModel.optimizerConstraints.collectAsStateWithLifecycle()
  val tentativeHolds by viewModel.tentativeHolds.collectAsStateWithLifecycle()
  val builderPreloadRequest by viewModel.builderPreloadRequest.collectAsStateWithLifecycle()
  val optimizerDeepLinkGoal by viewModel.optimizerDeepLinkGoal.collectAsStateWithLifecycle()
  val leaveWorkflowsByRecordId by viewModel.leaveWorkflowsByRecordId.collectAsStateWithLifecycle()
  val dutyShifts by viewModel.dutyShifts.collectAsStateWithLifecycle()
  val expiryWatchdogSummary by viewModel.expiryWatchdogSummary.collectAsStateWithLifecycle()
  val unclaimedGhColCandidates by viewModel.unclaimedGhColCandidates.collectAsStateWithLifecycle()
  val pendingWorkflowRecordId by viewModel.pendingWorkflowRecordId.collectAsStateWithLifecycle()
  val protectLoggedDutyShifts by viewModel.protectLoggedDutyShifts.collectAsStateWithLifecycle()
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
        val neu = LocalNeumorphicColors.current
        Box(
          modifier = Modifier
            .neumorphicExtruded(
              shape = IosSharpButtonShape,
              elevation = 6.dp,
              blurRadius = 12.dp,
              surfaceColor = neu.primaryAccent,
              showBorder = true,
              borderAlpha = 0.45f,
              neu = neu
            )
            .clickable { showAddDialog = true }
            .testTag("fab_add_leave")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(22.dp)
                .clip(IosSharpMicroShape)
                .background(Color.White.copy(alpha = 0.22f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Leave",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
              )
            }
            Text(
              text = "Log Leave",
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-0.2).sp,
              fontSize = 13.5.sp,
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
              val neu = LocalNeumorphicColors.current
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 8.dp)
                  .neumorphicInset(
                    shape = IosSharpWellShape,
                    depth = 3.dp,
                    blurRadius = 6.dp,
                    accentTint = neu.primaryAccent,
                    accentTintAlpha = 0.08f,
                    borderColor = neu.primaryAccent.copy(alpha = 0.30f),
                    neu = neu
                  )
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = msg,
                    style = MaterialTheme.typography.labelLarge,
                    color = neu.primaryAccent,
                    modifier = Modifier.weight(1f)
                  )
                  IconButton(
                    onClick = { viewModel.clearBanner() },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Dismiss notification",
                      tint = neu.primaryAccent,
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
              onProfileUpdated = {
                profileRefreshTick++
                viewModel.syncOptimizerSettingsFromStore()
              },
              onClose = {
                viewModel.syncOptimizerSettingsFromStore()
                showEnterpriseSettingsDialog = false
              }
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
              },
              expiryWatchdog = expiryWatchdogSummary,
              unclaimedGhCandidates = unclaimedGhColCandidates,
              onOptimizeExpiringCol = { item ->
                viewModel.triggerOptimizerDeepLink(
                  "Consume expiring GH Worked ${item.ghDateIso} FIFO COL before ${item.expiryDateIso}"
                )
                currentTab = 3
              },
              onOpenPaperworkAlert = { alert ->
                viewModel.openWorkflowForRecord(alert.recordId)
                currentTab = 1
              },
              onClaimGhColCandidate = { candidate ->
                viewModel.claimGhColFromDutyShift(candidate)
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
              },
              workflowsByRecordId = leaveWorkflowsByRecordId,
              pendingWorkflowRecordId = pendingWorkflowRecordId,
              onConsumePendingWorkflowRecord = { viewModel.consumePendingWorkflowRecord() },
              onSaveWorkflow = { meta, feedback -> viewModel.saveLeaveWorkflow(meta, feedback) },
              onAttachSlipUri = { recId, uri -> viewModel.attachSanctionSlipPhoto(recId, uri) },
              onRemoveSlip = { recId -> viewModel.removeSanctionSlipPhoto(recId) }
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
              onShowStatusBanner = { msg -> viewModel.showBannerMessage(msg) },
              expiryWatchdog = expiryWatchdogSummary,
              unclaimedGhCandidates = unclaimedGhColCandidates,
              onOptimizeExpiringCol = { item ->
                viewModel.triggerOptimizerDeepLink(
                  "Consume expiring GH Worked ${item.ghDateIso} FIFO COL before ${item.expiryDateIso}"
                )
                currentTab = 3
              },
              onClaimGhColCandidate = { candidate ->
                viewModel.claimGhColFromDutyShift(candidate)
              }
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
              },
              optimizerConstraints = optimizerConstraints,
              onUpdateOptimizerConstraints = { updated -> viewModel.updateOptimizerConstraints(updated) },
              tentativeHolds = tentativeHolds,
              deepLinkGoalHint = optimizerDeepLinkGoal,
              onConsumeDeepLink = { viewModel.consumeOptimizerDeepLink() },
              onApplyPortfolio = { recs -> viewModel.applyPortfolioToLedger(recs) },
              onToggleTentativeHold = { rec -> viewModel.toggleTentativeHoldForBridge(rec) },
              onSimulateInBuilder = { rec -> viewModel.simulateRecInBuilder(rec) },
              pendingBuilderPreload = builderPreloadRequest,
              onBuilderPreloadConsumed = { viewModel.consumeBuilderPreload() },
              dutyShifts = dutyShifts,
              protectLoggedDutyShifts = protectLoggedDutyShifts,
              onToggleProtectLoggedDutyShifts = { enabled -> viewModel.toggleProtectLoggedDutyShifts(enabled) }
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
              onApplyBridge = { viewModel.applySmartBridgeToLedger(it) },
              tentativeHolds = tentativeHolds,
              onPromoteTentativeHold = { hold -> viewModel.promoteTentativeHoldToLedger(hold) },
              onClearTentativeHold = { dateIso -> viewModel.clearTentativeHold(dateIso) },
              pendingBuilderPreload = builderPreloadRequest,
              onBuilderPreloadConsumed = { viewModel.consumeBuilderPreload() },
              dutyShifts = dutyShifts,
              onAddDutyShift = { dateIso, shiftType, note ->
                viewModel.addDutyShift(dateIso, shiftType, note)
              },
              onAddRecurringWeeklyShift = { year, month, dow, shiftType, note ->
                viewModel.addRecurringMonthlyDutyShifts(year, month, dow, shiftType, note)
              },
              onDeleteDutyShift = { shiftId -> viewModel.deleteDutyShift(shiftId) },
              onClaimGhCol = { candidate -> viewModel.claimGhColFromDutyShift(candidate) }
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
              onShowStatusBanner = { msg -> viewModel.showBannerMessage(msg) },
              onOptimizeInAi = { goalHint ->
                showEnterpriseSettingsDialog = false
                currentTab = 3
                viewModel.triggerOptimizerDeepLink(goalHint)
              }
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

  val neu = LocalNeumorphicColors.current

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(neu.baseBg)
      .statusBarsPadding()
  ) {
    // Top Status & 1-Tap Neumorphic Theme Cycle Strip
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 3.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Inset Neumorphic Live Sync Status Capsule
      Box(
        modifier = Modifier
          .height(24.dp)
          .neumorphicInset(
            shape = IosSharpBadgeShape,
            depth = 2.dp,
            blurRadius = 4.dp,
            accentTint = neu.primaryAccent,
            accentTintAlpha = 0.06f,
            neu = neu
          )
          .clickable { onOpenNotifications() }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth(0.54f)
            .padding(horizontal = 9.dp)
            .align(Alignment.CenterStart),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(IosSharpMicroShape)
              .background(neu.primaryAccent)
          )
          Text(
            text = "NEU SOFT-UI • IFHRMS LIVE",
            color = neu.textSecondary,
            fontSize = 8.5.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.35.sp,
            maxLines = 1,
            softWrap = false
          )
        }
      }

      // Right: Extruded 1-Tap Neumorphic Theme Mode Cycle Pill (Mint / Soft Clay / Dark #292E35)
      Box(
        modifier = Modifier
          .height(24.dp)
          .neumorphicExtruded(
            shape = IosSharpBadgeShape,
            elevation = 2.5.dp,
            blurRadius = 5.dp,
            accentTint = neu.primaryAccent,
            accentTintAlpha = 0.08f,
            borderAlpha = 0.32f,
            neu = neu
          )
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
          modifier = Modifier
            .padding(horizontal = 9.dp)
            .align(Alignment.Center),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(IosSharpMicroShape)
              .background(neu.primaryAccent)
          )
          Text(
            text = "${activeThemeMode.shortBadge} ⟳",
            color = neu.primaryAccent,
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

    // Main Title + Inset Trough with Extruded Tactile Action Buttons
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 7.dp),
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
              .background(neu.primaryAccent)
          )
          Text(
            text = officerHeaderSubtitle,
            style = MaterialTheme.typography.labelMedium,
            color = neu.primaryAccent,
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
          fontSize = 17.sp,
          lineHeight = 21.sp,
          letterSpacing = (-0.4).sp,
          fontWeight = FontWeight.ExtraBold,
          color = neu.textPrimary,
          maxLines = 1,
          softWrap = false,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Unified Inset Neumorphic Control Dock containing Extruded Action Buttons
      Box(
        modifier = Modifier
          .neumorphicInset(
            shape = IosSharpWellShape,
            depth = 2.5.dp,
            blurRadius = 5.dp,
            neu = neu
          )
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          Box(
            modifier = Modifier
              .size(31.dp)
              .neumorphicExtruded(
                shape = IosSharpButtonShape,
                elevation = 2.5.dp,
                blurRadius = 5.dp,
                accentTint = neu.primaryAccent,
                accentTintAlpha = 0.05f,
                neu = neu
              )
              .clickable { onOpenBiometricLock() }
              .testTag("top_bar_biometric_lock_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = "Biometric Lock & Security",
              tint = neu.primaryAccent,
              modifier = Modifier.size(15.dp)
            )
          }

          Box(
            modifier = Modifier
              .size(31.dp)
              .neumorphicExtruded(
                shape = IosSharpButtonShape,
                elevation = 2.5.dp,
                blurRadius = 5.dp,
                accentTint = neu.secondaryAccent,
                accentTintAlpha = 0.05f,
                neu = neu
              )
              .clickable { onOpenNotifications() }
              .testTag("top_bar_notifications_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = "Holiday & Leave Push Alerts",
              tint = neu.secondaryAccent,
              modifier = Modifier.size(15.dp)
            )
          }

          Box(
            modifier = Modifier
              .height(31.dp)
              .neumorphicExtruded(
                shape = IosSharpButtonShape,
                elevation = 2.5.dp,
                blurRadius = 5.dp,
                accentTint = neu.primaryAccent,
                accentTintAlpha = 0.08f,
                neu = neu
              )
              .clickable { onOpenPdfExport() }
              .testTag("top_bar_pdf_export_button"),
            contentAlignment = Alignment.Center
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = "Export Official PDF Report",
                tint = neu.primaryAccent,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "PDF",
                color = neu.primaryAccent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                softWrap = false
              )
            }
          }

          Box(
            modifier = Modifier
              .size(31.dp)
              .then(
                if (isSettingsOpen) {
                  Modifier.neumorphicInset(
                    shape = IosSharpButtonShape,
                    depth = 2.dp,
                    blurRadius = 4.dp,
                    surfaceColor = neu.primaryAccent,
                    neu = neu
                  )
                } else {
                  Modifier.neumorphicExtruded(
                    shape = IosSharpButtonShape,
                    elevation = 2.5.dp,
                    blurRadius = 5.dp,
                    accentTint = neu.primaryAccent,
                    accentTintAlpha = 0.05f,
                    neu = neu
                  )
                }
              )
              .clickable { onOpenSettings() }
              .testTag("top_bar_settings_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Enterprise Settings & 2027 Holiday Sync",
              tint = if (isSettingsOpen) Color.White else neu.primaryAccent,
              modifier = Modifier.size(15.dp)
            )
          }

          Box(
            modifier = Modifier
              .size(31.dp)
              .neumorphicExtruded(
                shape = IosSharpButtonShape,
                elevation = 2.5.dp,
                blurRadius = 5.dp,
                neu = neu
              )
              .clickable { onSignOut() }
              .testTag("sign_out_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = neu.textSecondary,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    // Subtle Neumorphic Grooved Divider (dark shadow line + light bevel line)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(neu.darkShadow.copy(alpha = 0.45f))
    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(0.75.dp)
        .background(neu.lightShadow.copy(alpha = 0.65f))
    )
  }
}

@Composable
private fun IosSquircleBottomTabBar(
  selectedTab: Int,
  onSelectTab: (Int) -> Unit
) {
  val neu = LocalNeumorphicColors.current
  val dockTopShape = IosSharpSquircleShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomEnd = 0.dp,
    bottomStart = 0.dp,
    sharpnessBias = 0.74f
  )

  val tabs = listOf(
    Triple(0, "Overview", Icons.Default.Dashboard),
    Triple(1, "Ledger", Icons.Default.EventNote),
    Triple(2, "COL & GH", Icons.Default.FactCheck),
    Triple(3, "Optimizer", Icons.Default.AutoAwesome),
    Triple(4, "Calendar", Icons.Default.CalendarMonth),
    Triple(5, "Advisor", Icons.Default.Psychology)
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = dockTopShape,
        elevation = 6.dp,
        blurRadius = 12.dp,
        showBorder = true,
        borderAlpha = 0.30f,
        neu = neu
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top Soft Highlight Edge
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(neu.lightShadow.copy(alpha = if (neu.isDark) 0.45f else 0.90f))
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        tabs.forEach { (index, label, icon) ->
          val isSelected = selectedTab == index
          val iconColor = if (isSelected) Color.White else neu.textSecondary
          val labelColor = if (isSelected) neu.primaryAccent else neu.textSecondary

          Column(
            modifier = Modifier
              .weight(1f)
              .then(
                if (isSelected) {
                  Modifier.neumorphicInset(
                    shape = IosSharpTabShape,
                    depth = 2.5.dp,
                    blurRadius = 5.dp,
                    accentTint = neu.primaryAccent,
                    accentTintAlpha = 0.08f,
                    borderColor = neu.primaryAccent.copy(alpha = 0.32f),
                    neu = neu
                  )
                } else {
                  Modifier.clip(IosSharpTabShape)
                }
              )
              .clickable { onSelectTab(index) }
              .padding(vertical = 5.dp, horizontal = 3.dp)
              .testTag("bottom_tab_$index"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Box(
              modifier = Modifier
                .height(27.dp)
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
                .then(
                  if (isSelected) {
                    Modifier.neumorphicExtruded(
                      shape = IosSharpButtonShape,
                      elevation = 2.5.dp,
                      blurRadius = 5.dp,
                      surfaceColor = neu.primaryAccent,
                      showBorder = true,
                      borderAlpha = 0.45f,
                      neu = neu
                    )
                  } else {
                    Modifier
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(if (isSelected) 16.dp else 16.dp)
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
        }
      }

      // Recessed Home Indicator Groove
      Box(
        modifier = Modifier
          .padding(bottom = 4.dp)
          .width(108.dp)
          .height(4.dp)
          .neumorphicInset(
            shape = IosSharpMicroShape,
            depth = 1.5.dp,
            blurRadius = 3.dp,
            neu = neu
          )
      )
    }
  }
}
