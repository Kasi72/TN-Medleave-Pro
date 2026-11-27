package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApprovalStage
import com.example.data.ColGhAuditSummary
import com.example.data.DutyShift
import com.example.data.DutyShiftType
import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.data.LeaveRepository
import com.example.data.LeaveWorkflowMeta
import com.example.data.SmartLeaveBridge
import com.example.data.TnHolidayItem
import com.example.data.local.LocalHolidayRepository
import com.example.data.local.TnHolidayDatabase
import com.example.domain.AiLeaveRecommendation
import com.example.domain.AiLeaveRecommendationEngine
import com.example.domain.AiRecommendationUiState
import com.example.domain.BuilderPreloadRequest
import com.example.domain.BurnDownSlotSuggestion
import com.example.domain.CsvImportRow
import com.example.domain.ColExpiryItem
import com.example.domain.CustomRangeAnalysis
import com.example.domain.DutyConflictEngine
import com.example.domain.ExpiryWatchdogEngine
import com.example.domain.ExpiryWatchdogSummary
import com.example.domain.GhColCaptureCandidate
import com.example.domain.HolidaySyncStatus
import com.example.domain.LapsedColArchiveStore
import com.example.domain.LeaveOptimizerEngine
import com.example.domain.OptimizerConstraints
import com.example.domain.SanctionSlipVault
import com.example.domain.TentativeHoldRecord
import com.example.domain.TentativePlanStore
import androidx.work.WorkInfo
import com.example.domain.TnHolidayBackgroundSyncService
import com.example.domain.TnHolidayRemoteConfigService
import com.example.domain.TnHolidaySyncScheduler
import com.example.domain.TnHolidaySyncWorker
import com.example.domain.TnHolidays2027Catalog
import com.example.domain.YearCreditSummary
import com.example.widget.MedLeaveQuotaAppWidget
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LeaveUiState {
  data object Loading : LeaveUiState
  data class Success(
    val allRecords: List<LeaveRecord>,
    val filteredRecords: List<LeaveRecord>,
    val selectedYear: Long,
    val selectedCategory: LeaveCategory?,
    val yearSummary: YearCreditSummary,
    val summary2026: YearCreditSummary,
    val colGhAuditYear: ColGhAuditSummary,
    val colGhAuditAllYears: ColGhAuditSummary,
    val smartBridges: List<SmartLeaveBridge>,
    val customAnalysis: CustomRangeAnalysis?,
    val statusBannerMessage: String?
  ) : LeaveUiState
  data class Error(val message: String) : LeaveUiState
}

class LeaveViewModel(
  private val repository: LeaveRepository,
  private val currentUserId: String,
  private val appContext: Context
) : ViewModel() {

  private val localHolidayRepository = LocalHolidayRepository(
    TnHolidayDatabase.getInstance(appContext).holidayDao()
  )

  private val _selectedYear = MutableStateFlow(2026L)
  val selectedYear: StateFlow<Long> = _selectedYear.asStateFlow()

  private val _selectedCategory = MutableStateFlow<LeaveCategory?>(null)
  val selectedCategory: StateFlow<LeaveCategory?> = _selectedCategory.asStateFlow()

  private val _customAnalysis = MutableStateFlow<CustomRangeAnalysis?>(null)
  private val _statusBanner = MutableStateFlow<String?>(null)

  private val _lastDeletedRecord = MutableStateFlow<LeaveRecord?>(null)
  val lastDeletedRecord: StateFlow<LeaveRecord?> = _lastDeletedRecord.asStateFlow()

  private val _aiRecommendationState =
    MutableStateFlow<AiRecommendationUiState>(AiRecommendationUiState.Idle)
  val aiRecommendationState: StateFlow<AiRecommendationUiState> =
    _aiRecommendationState.asStateFlow()

  private val _holidaySyncStatus = MutableStateFlow(
    TnHolidayBackgroundSyncService.readPersistedSyncStatus(
      context = appContext,
      ghCount = InitialLeaveSeedData.tnGovtPublicHolidays2026.size,
      rhCount = InitialLeaveSeedData.tnRestrictedHolidays2026.size
    )
  )
  val holidaySyncStatus: StateFlow<HolidaySyncStatus> = _holidaySyncStatus.asStateFlow()

  private val _holidayDbVersion = MutableStateFlow(0L)

  private val _optimizerConstraints = MutableStateFlow(
    OptimizerConstraints(
      reserveClFloor = HodProfileSettingsStore.getSafetyClReserveDefault(appContext),
      allowHalfDayCl = true,
      prioritizeExpiringCol = HodProfileSettingsStore.isEnforceFifoColEnabled(appContext),
      includeSaturdays = HodProfileSettingsStore.isIncludeSaturdayBridgeEnabled(appContext),
      enforce10DayCap = HodProfileSettingsStore.isEnforce10DayCapEnabled(appContext),
      protectedDaysOfWeek = HodProfileSettingsStore.getProtectedOpdDays(appContext)
    )
  )
  val optimizerConstraints: StateFlow<OptimizerConstraints> = _optimizerConstraints.asStateFlow()

  private val _tentativeHolds = MutableStateFlow(TentativePlanStore.readHolds(appContext))
  val tentativeHolds: StateFlow<Map<String, TentativeHoldRecord>> = _tentativeHolds.asStateFlow()

  private val _builderPreloadRequest = MutableStateFlow<BuilderPreloadRequest?>(null)
  val builderPreloadRequest: StateFlow<BuilderPreloadRequest?> = _builderPreloadRequest.asStateFlow()

  private val _optimizerDeepLinkGoal = MutableStateFlow<String?>(null)
  val optimizerDeepLinkGoal: StateFlow<String?> = _optimizerDeepLinkGoal.asStateFlow()

  private val _pendingWorkflowRecordId = MutableStateFlow<String?>(null)
  val pendingWorkflowRecordId: StateFlow<String?> = _pendingWorkflowRecordId.asStateFlow()

  private val _protectLoggedDutyShifts = MutableStateFlow(true)
  val protectLoggedDutyShifts: StateFlow<Boolean> = _protectLoggedDutyShifts.asStateFlow()

  private val _clearedLapsedColKeys = MutableStateFlow(
    LapsedColArchiveStore.getClearedKeys(appContext, currentUserId)
  )
  val clearedLapsedColKeys: StateFlow<Set<String>> = _clearedLapsedColKeys.asStateFlow()

  private val _isClearingExpiredGh = MutableStateFlow(false)
  val isClearingExpiredGh: StateFlow<Boolean> = _isClearingExpiredGh.asStateFlow()

  private val _expiredGhUndoBackup = MutableStateFlow<ExpiredGhUndoBackup?>(null)
  val expiredGhUndoBackup: StateFlow<ExpiredGhUndoBackup?> = _expiredGhUndoBackup.asStateFlow()

  val leaveWorkflowsByRecordId: StateFlow<Map<String, LeaveWorkflowMeta>> =
    repository.observeLeaveWorkflows(currentUserId)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyMap()
      )

  val dutyShifts: StateFlow<List<DutyShift>> =
    repository.observeDutyShifts(currentUserId)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
      )

  init {
    viewModelScope.launch {
      val seedResult = repository.seedHistoricalDataIfEmpty(currentUserId)
      seedResult.onSuccess { count ->
        if (count > 0) {
          _statusBanner.value = "Imported $count leave & credit entries (2023–2026) from your HOD spreadsheet."
        }
      }
    }

    // 1. Initialize local Room holiday database and observe live holiday changes
    viewModelScope.launch {
      localHolidayRepository.ensureSeededWithDefaultsIfEmpty()
      localHolidayRepository.domainHolidaysFlow.collect { (ghList, rhList) ->
        _holidaySyncStatus.value = _holidaySyncStatus.value.copy(
          totalGhCount = ghList.size,
          totalRhCount = rhList.size
        )
        _holidayDbVersion.value = System.currentTimeMillis()
      }
    }

    // 2. Initialize Firebase Remote Config real-time listener & fetch dynamic Tamil Nadu holiday updates
    TnHolidayRemoteConfigService.ensureInitializedAndObserveRealtimeUpdates(
      context = appContext,
      localRepo = localHolidayRepository,
      coroutineScope = viewModelScope,
      onRemoteHolidaysUpdated = { rcSnapshot ->
        _holidaySyncStatus.value = _holidaySyncStatus.value.copy(
          lastSyncedFormatted = rcSnapshot.lastFetchedFormatted,
          lastSyncSource = rcSnapshot.fetchSourceLabel,
          remoteConfigVersion = rcSnapshot.catalogVersion,
          remoteConfigBanner = rcSnapshot.announcementBanner
        )
        _holidayDbVersion.value = System.currentTimeMillis()
        _statusBanner.value =
          "Live Firebase Remote Config update applied (${rcSnapshot.catalogVersion} • ${rcSnapshot.upsertedHolidayCount} holidays)!"
      }
    )

    // 3. Schedule persistent AndroidX WorkManager periodic sync (every 12h/24h/7d on connected network)
    if (TnHolidayBackgroundSyncService.isAutoYearRolloverSyncEnabled(appContext)) {
      TnHolidaySyncScheduler.schedulePeriodicSync(appContext, replaceExisting = false)
    }

    // 4. Observe WorkManager periodic & immediate background worker state so UI & AI Optimizer stay live
    viewModelScope.launch {
      combine(
        TnHolidaySyncScheduler.observePeriodicWorkState(appContext),
        TnHolidaySyncScheduler.observeImmediateWorkState(appContext)
      ) { periodicInfo, immediateInfo -> periodicInfo to immediateInfo }
        .collect { (periodicInfo, immediateInfo) ->
          val activeImmediate = immediateInfo?.state == WorkInfo.State.RUNNING ||
            immediateInfo?.state == WorkInfo.State.ENQUEUED
          val activePeriodic = periodicInfo?.state == WorkInfo.State.RUNNING
          val runningNow = immediateInfo?.state == WorkInfo.State.RUNNING || activePeriodic

          val stateLabel = when {
            runningNow -> "WORKER SYNCING..."
            immediateInfo?.state == WorkInfo.State.SUCCEEDED -> "API + AI KB SYNCED"
            immediateInfo?.state == WorkInfo.State.FAILED -> "SYNC RETRY QUEUED"
            periodicInfo?.state == WorkInfo.State.ENQUEUED -> "AUTO-SYNC SCHEDULED"
            activeImmediate -> "WORKER QUEUED"
            else -> if (TnHolidayBackgroundSyncService.isAutoYearRolloverSyncEnabled(appContext)) {
              "AUTO-SYNC SCHEDULED"
            } else {
              "WORKER PAUSED"
            }
          }

          val completedInfo = when {
            immediateInfo?.state == WorkInfo.State.SUCCEEDED -> immediateInfo
            periodicInfo?.state == WorkInfo.State.SUCCEEDED -> periodicInfo
            else -> null
          }

          if (completedInfo != null) {
            val out = completedInfo.outputData
            val syncedSource = out.getString(TnHolidaySyncWorker.KEY_OUT_SOURCE)
            val syncedTime = out.getString(TnHolidaySyncWorker.KEY_OUT_SYNC_TIME)
            val syncedSummary = out.getString(TnHolidaySyncWorker.KEY_OUT_SUMMARY)
            val totalGh = out.getInt(TnHolidaySyncWorker.KEY_OUT_TOTAL_GH, _holidaySyncStatus.value.totalGhCount)
            val totalRh = out.getInt(TnHolidaySyncWorker.KEY_OUT_TOTAL_RH, _holidaySyncStatus.value.totalRhCount)
            val apiParsed = out.getInt(TnHolidaySyncWorker.KEY_OUT_API_PARSED, _holidaySyncStatus.value.apiHolidaysParsedCount)
            val kbRev = out.getLong(TnHolidaySyncWorker.KEY_OUT_KB_REVISION, TnHolidays2027Catalog.knowledgeBaseRevision)

            _holidaySyncStatus.value = _holidaySyncStatus.value.copy(
              isSyncing = false,
              hasError = false,
              totalGhCount = totalGh,
              totalRhCount = totalRh,
              lastSyncSource = syncedSource ?: _holidaySyncStatus.value.lastSyncSource,
              lastSyncedFormatted = syncedTime ?: _holidaySyncStatus.value.lastSyncedFormatted,
              statusSummary = syncedSummary ?: _holidaySyncStatus.value.statusSummary,
              workerScheduleLabel = TnHolidaySyncScheduler.formatScheduleLabel(appContext),
              workerStateLabel = stateLabel,
              apiHolidaysParsedCount = apiParsed,
              knowledgeBaseRevision = kbRev
            )
            _holidayDbVersion.value = System.currentTimeMillis()
          } else {
            _holidaySyncStatus.value = _holidaySyncStatus.value.copy(
              isSyncing = runningNow,
              hasError = immediateInfo?.state == WorkInfo.State.FAILED,
              workerScheduleLabel = TnHolidaySyncScheduler.formatScheduleLabel(appContext),
              workerStateLabel = stateLabel
            )
          }
        }
    }

    // 5. Run initial Firebase Remote Config fetch + enqueue WorkManager sync if stale/new year
    viewModelScope.launch {
      localHolidayRepository.ensureSeededWithDefaultsIfEmpty()
      val rcInitial = com.example.domain.TnHolidayRemoteConfigService.fetchAndApplyRemoteHolidays(
        context = appContext,
        localRepo = localHolidayRepository,
        forceZeroCacheInterval = false
      )
      _holidaySyncStatus.value = _holidaySyncStatus.value.copy(
        remoteConfigVersion = rcInitial.catalogVersion,
        remoteConfigBanner = rcInitial.announcementBanner
      )
      _holidayDbVersion.value = System.currentTimeMillis()
      if (TnHolidayBackgroundSyncService.shouldRunAutomaticBackgroundSync(appContext)) {
        TnHolidaySyncScheduler.enqueueImmediateSync(
          context = appContext,
          customSourceUrl = "",
          isManual = false
        )
      }
    }

    // 6. Keep AI Optimizer's protectedDatesIso synced automatically with logged Clinical Duty Shifts
    viewModelScope.launch {
      combine(dutyShifts, _protectLoggedDutyShifts) { shifts, protectEnabled ->
        if (protectEnabled) DutyConflictEngine.extractProtectedDutyDates(shifts) else emptySet()
      }.collect { dutyDates ->
        if (_optimizerConstraints.value.protectedDatesIso != dutyDates) {
          _optimizerConstraints.value = _optimizerConstraints.value.copy(
            protectedDatesIso = dutyDates
          )
        }
      }
    }
  }

  val uiState: StateFlow<LeaveUiState> = combine(
    repository.observeLeaveRecords(currentUserId),
    _selectedYear,
    _selectedCategory,
    _customAnalysis,
    combine(_statusBanner, _holidayDbVersion) { banner, version -> banner to version }
  ) { records, year, category, customAnalysis, (banner, _) ->
    val yearRecords = records.filter { it.year == year }
    val filtered = if (category == null) {
      yearRecords
    } else {
      yearRecords.filter { it.category == category }
    }
    val summarySelected = LeaveOptimizerEngine.computeYearSummary(records, year)
    val summary2026 = LeaveOptimizerEngine.computeYearSummary(records, 2026L)
    val auditYear = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = year)
    val auditAll = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = null)

    val summary2027 = LeaveOptimizerEngine.computeYearSummary(records, 2027L)
    val dynamicBridges = LeaveOptimizerEngine.generateOptimalLeaveBridges(
      summary2026 = summary2026,
      records2026 = records.filter { it.year == 2026L },
      allRecords = records,
      colGhAuditAllYears = auditAll,
      summary2027 = summary2027
    )
    val bridges = (dynamicBridges + TnHolidays2027Catalog.smartBridges2027)
      .distinctBy { "${it.startDateIso}_${it.endDateIso}" }

    val defaultAnalysis = customAnalysis ?: LeaveOptimizerEngine.analyzeCustomRange(
      startIso = "2026-12-24",
      endIso = "2027-01-01",
      summary2026 = summary2026
    )

    // Auto-sync live 2026 quota & next holiday bridge to the Native Android Home Screen Widget
    runCatching {
      val clRemFmt = if (summary2026.clRemaining % 1.0 == 0.0) {
        summary2026.clRemaining.toInt().toString()
      } else {
        String.format("%.1f", summary2026.clRemaining)
      }
      val audit2026 = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = 2026L)
      MedLeaveQuotaAppWidget.syncAndRefreshWidget(
        context = appContext,
        clText = "$clRemFmt / 12",
        rhText = "${summary2026.rhRemaining} / 3",
        colText = "${audit2026.availableUnusedGhCount} READY",
        nextHolidayText = "★ Next Bridge: Dec 24–27 Christmas (4d) • Jan 13–17 Pongal 2027 (5d)",
        updatedLabel = "LIVE ${year}"
      )
    }

    LeaveUiState.Success(
      allRecords = records,
      filteredRecords = filtered,
      selectedYear = year,
      selectedCategory = category,
      yearSummary = summarySelected,
      summary2026 = summary2026,
      colGhAuditYear = auditYear,
      colGhAuditAllYears = auditAll,
      smartBridges = bridges,
      customAnalysis = defaultAnalysis,
      statusBannerMessage = banner
    ) as LeaveUiState
  }
    .catch { e ->
      emit(LeaveUiState.Error(e.localizedMessage ?: "Failed to load leave records"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = LeaveUiState.Loading
    )

  val expiryWatchdogSummary: StateFlow<ExpiryWatchdogSummary> = combine(
    uiState,
    leaveWorkflowsByRecordId,
    _clearedLapsedColKeys
  ) { state, workflows, clearedKeys ->
    val records = (state as? LeaveUiState.Success)?.allRecords.orEmpty()
    ExpiryWatchdogEngine.evaluate(
      records = records,
      workflowsByRecordId = workflows,
      clearedLapsedRecordKeys = clearedKeys
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = ExpiryWatchdogEngine.evaluate(emptyList(), emptyMap())
  )

  val unclaimedGhColCandidates: StateFlow<List<GhColCaptureCandidate>> = combine(
    dutyShifts,
    uiState,
    _holidayDbVersion
  ) { shifts, state, _ ->
    val records = (state as? LeaveUiState.Success)?.allRecords.orEmpty()
    DutyConflictEngine.findUnclaimedGhColCandidates(shifts, records)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = emptyList()
  )

  fun triggerHolidaySync(
    customUrlOrFeedNote: String = "",
    isAutomaticBackground: Boolean = false
  ) {
    if (_holidaySyncStatus.value.isSyncing) return
    _holidaySyncStatus.value = _holidaySyncStatus.value.copy(
      isSyncing = true,
      hasError = false,
      workerStateLabel = "WORKER RUNNING..."
    )
    // Enqueue via AndroidX WorkManager so sync survives app backgrounding & retries on flaky hospital Wi-Fi
    TnHolidaySyncScheduler.enqueueImmediateSync(
      context = appContext,
      customSourceUrl = customUrlOrFeedNote,
      isManual = !isAutomaticBackground
    )
    if (!isAutomaticBackground) {
      _statusBanner.value =
        "Background Holiday Worker started — fetching latest TN Govt public API calendar & refreshing AI Optimizer..."
    }
  }

  fun addLocalHolidayToRoom(item: TnHolidayItem) {
    viewModelScope.launch {
      localHolidayRepository.addCustomOrLocalHoliday(
        item = item,
        sourceOrigin = "Manual G.O. / Collectorate Entry"
      )
      val typeLabel = if (item.isRestrictedHoliday) "Restricted Holiday (RH)" else "Public Holiday (GH)"
      _statusBanner.value =
        "Added ${item.nameEnglish} (${item.dateIso}) as $typeLabel to local Room DB. Optimizer updated!"
    }
  }

  fun selectYear(year: Long) {
    _selectedYear.value = year
  }

  fun selectCategory(category: LeaveCategory?) {
    _selectedCategory.value = category
  }

  fun clearBanner() {
    _statusBanner.value = null
  }

  fun showBannerMessage(message: String) {
    _statusBanner.value = message
  }

  fun runCustomRangeAnalysis(startIso: String, endIso: String, summary2026: YearCreditSummary) {
    _customAnalysis.value = LeaveOptimizerEngine.analyzeCustomRange(startIso, endIso, summary2026)
  }

  fun updateOptimizerConstraints(updated: OptimizerConstraints) {
    _optimizerConstraints.value = updated
  }

  fun syncOptimizerSettingsFromStore() {
    val cur = _optimizerConstraints.value
    _optimizerConstraints.value = cur.copy(
      prioritizeExpiringCol = HodProfileSettingsStore.isEnforceFifoColEnabled(appContext),
      includeSaturdays = HodProfileSettingsStore.isIncludeSaturdayBridgeEnabled(appContext),
      enforce10DayCap = HodProfileSettingsStore.isEnforce10DayCapEnabled(appContext),
      protectedDaysOfWeek = HodProfileSettingsStore.getProtectedOpdDays(appContext),
      reserveClFloor = if (cur.reserveClFloor == 0.0) HodProfileSettingsStore.getSafetyClReserveDefault(appContext) else cur.reserveClFloor
    )
  }

  fun toggleTentativeHoldForBridge(rec: AiLeaveRecommendation) {
    val current = _tentativeHolds.value.toMutableMap()
    val actionSteps = rec.dailyPlan.filter {
      it.creditType.uppercase() in setOf("CL", "HALF_CL", "RH", "COL", "EL") && !it.isAlreadyLogged
    }
    if (actionSteps.isEmpty()) return

    val allHeld = actionSteps.all { current[it.dateIso]?.bridgeId == rec.id }
    if (allHeld) {
      actionSteps.forEach { current.remove(it.dateIso) }
      _tentativeHolds.value = current
      TentativePlanStore.saveHolds(appContext, current)
      _statusBanner.value = "Cleared tentative hold for '${rec.title}'."
    } else {
      actionSteps.forEach { step ->
        current[step.dateIso] = TentativeHoldRecord(
          dateIso = step.dateIso,
          creditType = step.creditType.uppercase(),
          bridgeId = rec.id,
          bridgeTitle = rec.title,
          note = step.description
        )
      }
      _tentativeHolds.value = current
      TentativePlanStore.saveHolds(appContext, current)
      _statusBanner.value = "Marked ${actionSteps.size} tentative date(s) as HOLD on Calendar (balances unchanged)."
    }
  }

  fun clearTentativeHold(dateIso: String) {
    val current = _tentativeHolds.value.toMutableMap()
    if (current.remove(dateIso) != null) {
      _tentativeHolds.value = current
      TentativePlanStore.saveHolds(appContext, current)
      _statusBanner.value = "Removed tentative hold on $dateIso."
    }
  }

  fun promoteTentativeHoldToLedger(hold: TentativeHoldRecord) {
    val normalizedType = if (hold.creditType == "HALF_CL") "CL" else hold.creditType
    val isHalf = hold.creditType == "HALF_CL"
    val curSuccess = uiState.value as? LeaveUiState.Success
    val unusedGh = curSuccess?.colGhAuditAllYears?.unusedGhRecords?.minByOrNull { it.dateIso }
    val pairedIso = if (normalizedType == "COL") unusedGh?.dateIso.orEmpty() else ""

    if (normalizedType == "COL" && unusedGh != null) {
      logNewColAgainstUnusedGh(
        newColDateIso = hold.dateIso,
        ghRecord = unusedGh,
        notes = "Promoted from Tentative Hold (${hold.bridgeTitle})"
      )
    } else {
      addLeave(
        leaveType = normalizedType,
        dateIso = hold.dateIso,
        pairedDateIso = pairedIso,
        notes = "Promoted from Tentative Hold (${hold.bridgeTitle}): ${hold.note}",
        isHalfDay = isHalf
      )
    }
    clearTentativeHold(hold.dateIso)
  }

  fun simulateRecInBuilder(rec: AiLeaveRecommendation) {
    val blocks = linkedMapOf<String, LeaveCategory>()
    rec.dailyPlan.forEach { step ->
      if (step.isAlreadyLogged) return@forEach
      val cat = when (step.creditType.uppercase()) {
        "EL" -> LeaveCategory.EL
        "RH" -> LeaveCategory.RH
        "COL" -> LeaveCategory.COL
        "CL", "HALF_CL" -> LeaveCategory.CL
        else -> null
      }
      if (cat != null) blocks[step.dateIso] = cat
    }
    val startParsed = runCatching { java.time.LocalDate.parse(rec.startDateIso) }.getOrNull()
    val targetYr = startParsed?.year ?: 2026
    val targetMo = startParsed?.monthValue ?: 11
    _builderPreloadRequest.value = BuilderPreloadRequest(
      title = rec.title,
      targetYear = targetYr,
      targetMonth = targetMo,
      draftBlocks = blocks
    )
    _statusBanner.value = "Loaded '${rec.title}' into Interactive Leave Builder for sandbox editing!"
  }

  fun consumeBuilderPreload() {
    _builderPreloadRequest.value = null
  }

  fun triggerOptimizerDeepLink(goalHint: String) {
    _optimizerDeepLinkGoal.value = goalHint
  }

  fun consumeOptimizerDeepLink() {
    _optimizerDeepLinkGoal.value = null
  }

  /**
   * Pairs a specific COL record with a specific GH Worked date, updating both the COL record,
   * the corresponding GH Worked record, and creating/updating the In Lieu Of record in Firestore
   * so all 3 columns (COL, In Lieu of, GH Worked) are 100% accounted and verified.
   */
  fun pairColWithGhWorked(
    colRecordId: String,
    ghWorkedDateIso: String,
    allRecords: List<LeaveRecord>
  ) {
    viewModelScope.launch {
      val colRec = allRecords.firstOrNull { it.id == colRecordId } ?: return@launch
      val ghRec = allRecords.firstOrNull {
        it.category == LeaveCategory.GH_WORKED && it.dateIso == ghWorkedDateIso
      }

      // 1. Update COL record with pairedDateIso = ghWorkedDateIso and VERIFIED_GREEN
      repository.updateLeaveRecord(
        colRec.copy(
          pairedDateIso = ghWorkedDateIso,
          notes = "COL accounted against GH Worked on $ghWorkedDateIso",
          highlightStatus = "VERIFIED_GREEN"
        )
      )

      // 2. If GH Worked record exists, link it back to colRec.dateIso and mark VERIFIED_GREEN;
      // if missing from GH Worked column, create it so the GH Worked column is complete!
      if (ghRec != null) {
        repository.updateLeaveRecord(
          ghRec.copy(
            pairedDateIso = colRec.dateIso,
            notes = "GH Worked paired with COL on ${colRec.dateIso}",
            highlightStatus = "VERIFIED_GREEN"
          )
        )
      } else {
        repository.addLeaveRecord(
          leaveType = "GH_WORKED",
          dateIso = ghWorkedDateIso,
          year = colRec.year,
          pairedDateIso = colRec.dateIso,
          notes = "GH Worked synced from In-Lieu duty (paired with COL ${colRec.dateIso})",
          highlightStatus = "VERIFIED_GREEN"
        )
      }

      // 3. Ensure corresponding IN_LIEU entry is also updated/present
      val existingInLieu = allRecords.firstOrNull {
        it.category == LeaveCategory.IN_LIEU &&
          (it.pairedDateIso == colRec.dateIso || it.dateIso == colRec.pairedDateIso)
      }
      if (existingInLieu != null) {
        repository.updateLeaveRecord(
          existingInLieu.copy(
            dateIso = ghWorkedDateIso,
            pairedDateIso = colRec.dateIso,
            notes = "In Lieu of GH Worked ($ghWorkedDateIso) for COL ${colRec.dateIso}",
            highlightStatus = "VERIFIED_GREEN"
          )
        )
      } else {
        repository.addLeaveRecord(
          leaveType = "IN_LIEU",
          dateIso = ghWorkedDateIso,
          year = colRec.year,
          pairedDateIso = colRec.dateIso,
          notes = "In Lieu of GH Worked ($ghWorkedDateIso) for COL ${colRec.dateIso}",
          highlightStatus = "VERIFIED_GREEN"
        )
      }

      _statusBanner.value = "Paired COL (${colRec.dateIso}) ↔ GH Worked ($ghWorkedDateIso)!"
    }
  }

  /**
   * One-Tap Auto-Reconcile All COL & GH Worked records:
   * Automatically pairs every unaccounted/duplicate COL with an available unused GH Worked credit
   * from the same year (or adds missing In-Lieu dates to GH Worked if no unused GH credit remains)
   * so that 100% of your COL is accounted for against GH Worked.
   */
  fun autoReconcileAllColWithGhWorked(allRecords: List<LeaveRecord>, targetYear: Long? = null) {
    viewModelScope.launch {
      val audit = LeaveOptimizerEngine.auditColAndGhWorked(allRecords, filterYear = targetYear)
      val unusedGhQueue = audit.unusedGhRecords.toMutableList()
      var fixedCount = 0

      audit.items.forEach { item ->
        if (item.colRecordId.isNotEmpty() &&
          item.status != com.example.data.ColAuditStatus.VERIFIED_MATCH
        ) {
          val colRec = allRecords.firstOrNull { it.id == item.colRecordId } ?: return@forEach
          // Prefer an unused GH Worked from the same year first
          val sameYearGhIdx = unusedGhQueue.indexOfFirst { it.year == colRec.year }
          val chosenGh = when {
            sameYearGhIdx >= 0 -> unusedGhQueue.removeAt(sameYearGhIdx)
            unusedGhQueue.isNotEmpty() -> unusedGhQueue.removeAt(0)
            else -> null
          }

          if (chosenGh != null) {
            repository.updateLeaveRecord(
              colRec.copy(
                pairedDateIso = chosenGh.dateIso,
                notes = "Audited & Paired with GH Worked on ${chosenGh.dateIso}",
                highlightStatus = "VERIFIED_GREEN"
              )
            )
            repository.updateLeaveRecord(
              chosenGh.copy(
                pairedDateIso = colRec.dateIso,
                notes = "GH Worked accounted for COL on ${colRec.dateIso}",
                highlightStatus = "VERIFIED_GREEN"
              )
            )
            fixedCount++
          } else if (colRec.pairedDateIso.isNotBlank()) {
            // If COL already had an In-Lieu date that wasn't in the GH_WORKED column (like 2026-04-09),
            // register that duty date into GH_WORKED so the 3-column audit is 100% accounted.
            repository.addLeaveRecord(
              leaveType = "GH_WORKED",
              dateIso = colRec.pairedDateIso,
              year = colRec.year,
              pairedDateIso = colRec.dateIso,
              notes = "Audited GH Worked entry for COL on ${colRec.dateIso}",
              highlightStatus = "VERIFIED_GREEN"
            )
            repository.updateLeaveRecord(
              colRec.copy(
                notes = "Audited & Paired with GH Worked on ${colRec.pairedDateIso}",
                highlightStatus = "VERIFIED_GREEN"
              )
            )
            fixedCount++
          }
        }
      }

      _statusBanner.value = if (fixedCount > 0) {
        "Reconciled & paired $fixedCount COL entries with GH Worked credits! All COLs are now accounted for."
      } else {
        "All COL entries in this view are already 100% accounted for with GH Worked!"
      }
    }
  }

  /**
   * Logs a brand-new COL leave date and immediately pairs it against a selected unused GH Worked credit.
   */
  fun logNewColAgainstUnusedGh(
    newColDateIso: String,
    ghRecord: LeaveRecord,
    notes: String
  ) {
    val year = newColDateIso.take(4).toLongOrNull() ?: ghRecord.year
    viewModelScope.launch {
      repository.addLeaveRecord(
        leaveType = "COL",
        dateIso = newColDateIso,
        year = year,
        pairedDateIso = ghRecord.dateIso,
        notes = notes.ifBlank { "COL availed against GH Worked on ${ghRecord.dateIso}" },
        highlightStatus = "VERIFIED_GREEN"
      )
      repository.addLeaveRecord(
        leaveType = "IN_LIEU",
        dateIso = ghRecord.dateIso,
        year = year,
        pairedDateIso = newColDateIso,
        notes = "In Lieu of GH Worked (${ghRecord.dateIso}) for COL $newColDateIso",
        highlightStatus = "VERIFIED_GREEN"
      )
      repository.updateLeaveRecord(
        ghRecord.copy(
          pairedDateIso = newColDateIso,
          notes = "GH Worked availed as COL on $newColDateIso",
          highlightStatus = "VERIFIED_GREEN"
        )
      )
      _statusBanner.value = "Logged COL on $newColDateIso paired with GH Worked (${ghRecord.dateIso})!"
    }
  }

  fun addLeave(
    leaveType: String,
    dateIso: String,
    pairedDateIso: String,
    notes: String,
    highlightStatus: String = "NORMAL",
    isHalfDay: Boolean = false
  ) {
    val year = dateIso.take(4).toLongOrNull() ?: _selectedYear.value
    viewModelScope.launch {
      val res = repository.addLeaveRecord(
        leaveType = leaveType,
        dateIso = dateIso,
        year = year,
        pairedDateIso = pairedDateIso,
        notes = notes,
        highlightStatus = highlightStatus,
        isHalfDay = isHalfDay
      )
      res.onSuccess {
        _selectedYear.value = year
        if (leaveType == "GH_WORKED" && pairedDateIso.isBlank()) {
          val curSuccess = uiState.value as? LeaveUiState.Success
          val bestColBridge = curSuccess?.let { st ->
            AiLeaveRecommendationEngine.scanHolidayDatabaseForLongWeekends(
              summary2026 = st.summary2026,
              colGhAuditAllYears = st.colGhAuditAllYears,
              records = st.allRecords,
              focusGoal = "Consume Expiring GH Worked (FIFO COL)",
              constraints = _optimizerConstraints.value.copy(prioritizeExpiringCol = true)
            ).firstOrNull()
          }
          _statusBanner.value = if (bestColBridge != null) {
            "Banked GH Worked ($dateIso) • Optimal COL bridge unlocked: ${bestColBridge.title} (${bestColBridge.efficiencyMultiplier}x ROI)"
          } else {
            "Logged $leaveType on $dateIso (6-month COL pool updated)"
          }
        } else {
          _statusBanner.value = "Logged $leaveType on $dateIso"
        }
      }.onFailure { err ->
        _statusBanner.value = "⛔ ${err.localizedMessage ?: "Duplicate leave entry rejected"}"
      }
    }
  }

  fun addLeaveEntrySpell(
    leaveType: String,
    startDate: String,
    endDate: String,
    status: String,
    pairedDutyDate: String,
    reason: String,
    isHalfDay: Boolean = false
  ) {
    val year = startDate.take(4).toLongOrNull() ?: _selectedYear.value
    viewModelScope.launch {
      val entry = com.example.data.LeaveEntry(
        leaveType = leaveType,
        startDate = startDate,
        endDate = endDate,
        status = status,
        year = year,
        pairedDutyDate = pairedDutyDate,
        reason = reason,
        isHalfDay = isHalfDay
      )
      val res = repository.addLeaveEntryWithDailyRecords(entry)
      res.onSuccess { summaryTag ->
        _selectedYear.value = year
        _statusBanner.value = when {
          summaryTag.contains("_SKIPPED_") -> {
            val addedNum = summaryTag.substringAfter("ADDED_").substringBefore("_SKIPPED_")
            val skippedNum = summaryTag.substringAfter("_SKIPPED_")
            "⚡ Logged $addedNum new $leaveType day(s) ($startDate → $endDate) • Auto-rejected $skippedNum duplicate date(s)!"
          }
          startDate == endDate -> "Logged $leaveType on $startDate ($status)"
          else -> "Logged $leaveType spell ($startDate → $endDate, $status)"
        }
      }.onFailure { err ->
        _statusBanner.value = "⛔ ${err.localizedMessage ?: "Duplicate leave entry rejected"}"
      }
    }
  }

  fun generateAiLeaveRecommendations(
    focusGoal: String,
    state: LeaveUiState.Success,
    constraints: OptimizerConstraints = _optimizerConstraints.value
  ) {
    if (_aiRecommendationState.value is AiRecommendationUiState.Loading) return
    _optimizerConstraints.value = constraints
    _aiRecommendationState.value = AiRecommendationUiState.Loading
    viewModelScope.launch {
      val summary2027 = LeaveOptimizerEngine.computeYearSummary(state.allRecords, 2027L)
      val officerContext = "${HodProfileSettingsStore.getOfficerName(appContext)} (${HodProfileSettingsStore.getDesignation(appContext)}, ${HodProfileSettingsStore.getDepartment(appContext)}, ${HodProfileSettingsStore.getCollege(appContext)})"
      val result = AiLeaveRecommendationEngine.generateOptimalRecommendations(
        focusGoal = focusGoal,
        summary2026 = state.summary2026,
        colGhAuditAllYears = state.colGhAuditAllYears,
        records = state.allRecords,
        summary2027 = summary2027,
        constraints = constraints,
        officerContextNote = officerContext
      )
      result.onSuccess { successState ->
        _aiRecommendationState.value = successState
      }.onFailure { err ->
        _aiRecommendationState.value = AiRecommendationUiState.Error(
          focusGoal = focusGoal,
          message = err.localizedMessage ?: "Failed to generate AI leave recommendations."
        )
      }
    }
  }

  fun applyPortfolioToLedger(recs: List<AiLeaveRecommendation>) {
    if (recs.isEmpty()) return
    viewModelScope.launch {
      recs.forEach { rec ->
        applySmartBridgeInternal(rec.toSmartLeaveBridge(), silentBanner = true)
      }
      _statusBanner.value = "Applied ${recs.size} selected portfolio leave plan(s) to your Firestore Ledger!"
    }
  }

  fun applySmartBridgeToLedger(bridge: SmartLeaveBridge) {
    viewModelScope.launch {
      applySmartBridgeInternal(bridge, silentBanner = false)
    }
  }

  private suspend fun applySmartBridgeInternal(
    bridge: SmartLeaveBridge,
    silentBanner: Boolean
  ) {
    val isoRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
    val currentSuccess = uiState.value as? LeaveUiState.Success
    val unusedGhQueue = currentSuccess?.colGhAuditAllYears?.unusedGhRecords
      ?.sortedBy { it.dateIso }
      ?.toMutableList()
      ?: mutableListOf()

    var added = 0
    var skippedDuplicates = 0
    var lastAddedYear: Long? = null
    val appliedDateSet = mutableSetOf<String>()

    bridge.dayByDayBreakdown.forEach { step ->
      if (!isoRegex.matches(step.dateIso.trim())) return@forEach
      val cleanDate = step.dateIso.trim()
      val year = cleanDate.take(4).toLongOrNull() ?: 2026L

      when (step.badgeType) {
        "RH" -> {
          if (!step.description.contains("Already Logged", ignoreCase = true)) {
            val res = repository.addLeaveRecord(
              leaveType = "RH",
              dateIso = cleanDate,
              year = year,
              notes = "Smart Bridge (${bridge.title}): ${step.description}"
            )
            if (res.isSuccess) {
              added++
              lastAddedYear = year
              appliedDateSet.add(cleanDate)
            } else {
              skippedDuplicates++
            }
          } else {
            skippedDuplicates++
          }
        }
        "COL" -> {
          if (!step.description.contains("Already Logged", ignoreCase = true)) {
            val colLocal = runCatching { java.time.LocalDate.parse(cleanDate) }.getOrNull()
            val fifoGhIdx = unusedGhQueue.indexOfFirst { gh ->
              val gd = runCatching { java.time.LocalDate.parse(gh.dateIso) }.getOrNull()
              gd == null || colLocal == null || java.time.temporal.ChronoUnit.DAYS.between(gd, colLocal) in 0..180
            }
            val fifoGh = if (fifoGhIdx >= 0) unusedGhQueue[fifoGhIdx] else unusedGhQueue.firstOrNull()
            val res = repository.addLeaveRecord(
              leaveType = "COL",
              dateIso = cleanDate,
              year = year,
              pairedDateIso = fifoGh?.dateIso.orEmpty(),
              notes = if (fifoGh != null) {
                "AI Smart Bridge (${bridge.title}) paired FIFO w/ GH ${fifoGh.dateIso}"
              } else {
                "AI Smart Bridge (${bridge.title}): ${step.description}"
              },
              highlightStatus = if (fifoGh != null) "VERIFIED_GREEN" else "ALERT_RED"
            )
            if (res.isSuccess) {
              added++
              lastAddedYear = year
              appliedDateSet.add(cleanDate)
              if (fifoGh != null) {
                unusedGhQueue.remove(fifoGh)
                // Cross-year integrity: IN_LIEU keeps the GH Worked year so COL/In-Lieu audit balances cleanly across 2026->2027
                repository.addLeaveRecord(
                  leaveType = "IN_LIEU",
                  dateIso = fifoGh.dateIso,
                  year = fifoGh.year,
                  pairedDateIso = cleanDate,
                  notes = "In Lieu of GH Worked (${fifoGh.dateIso}) for AI Bridge COL $cleanDate",
                  highlightStatus = "VERIFIED_GREEN"
                )
                repository.updateLeaveRecord(
                  fifoGh.copy(
                    pairedDateIso = cleanDate,
                    notes = "GH Worked availed as AI Bridge COL on $cleanDate",
                    highlightStatus = "VERIFIED_GREEN"
                  )
                )
              }
            } else {
              skippedDuplicates++
            }
          } else {
            skippedDuplicates++
          }
        }
        "EL" -> {
          if (!step.description.contains("Already Logged", ignoreCase = true)) {
            val res = repository.addLeaveRecord(
              leaveType = "EL",
              dateIso = cleanDate,
              year = year,
              notes = "Rule-8 Optimal EL (${bridge.title}): ${step.description}"
            )
            if (res.isSuccess) {
              added++
              lastAddedYear = year
              appliedDateSet.add(cleanDate)
            } else {
              skippedDuplicates++
            }
          } else {
            skippedDuplicates++
          }
        }
        "HALF_CL" -> {
          if (!step.description.contains("Already Logged", ignoreCase = true)) {
            val res = repository.addLeaveRecord(
              leaveType = "CL",
              dateIso = cleanDate,
              year = year,
              notes = "Half-Day Bridge (${bridge.title}): ${step.description}",
              isHalfDay = true
            )
            if (res.isSuccess) {
              added++
              lastAddedYear = year
              appliedDateSet.add(cleanDate)
            } else {
              skippedDuplicates++
            }
          } else {
            skippedDuplicates++
          }
        }
        "LEAVE_CREDIT", "CL" -> {
          if (!step.description.contains("Already Logged", ignoreCase = true) &&
            !step.description.contains("COL Logged", ignoreCase = true)
          ) {
            val isHalf = step.description.contains("0.5", ignoreCase = true)
            val res = repository.addLeaveRecord(
              leaveType = "CL",
              dateIso = cleanDate,
              year = year,
              notes = "Smart Bridge (${bridge.title}): ${step.description}",
              isHalfDay = isHalf
            )
            if (res.isSuccess) {
              added++
              lastAddedYear = year
              appliedDateSet.add(cleanDate)
            } else {
              skippedDuplicates++
            }
          } else {
            skippedDuplicates++
          }
        }
      }
    }

    if (appliedDateSet.isNotEmpty()) {
      val updatedHolds = _tentativeHolds.value.toMutableMap()
      var holdsChanged = false
      appliedDateSet.forEach { d ->
        if (updatedHolds.remove(d) != null) holdsChanged = true
      }
      if (holdsChanged) {
        _tentativeHolds.value = updatedHolds
        TentativePlanStore.saveHolds(appContext, updatedHolds)
      }
    }

    if (lastAddedYear != null) {
      _selectedYear.value = lastAddedYear!!
    }
    if (!silentBanner) {
      _statusBanner.value = when {
        added > 0 && skippedDuplicates > 0 ->
          "⚡ Applied $added new leave day(s) for '${bridge.title}' (Auto-skipped $skippedDuplicates duplicate date(s))!"
        added > 0 ->
          "Applied $added optimal leave day(s) for '${bridge.title}' to your Ledger!"
        else ->
          "⛔ Duplicate Guard: All leave dates in '${bridge.title}' are already booked in your ledger."
      }
    }
  }

  fun commitBuilderDraftToLedger(draftBlocks: Map<String, LeaveCategory>) {
    if (draftBlocks.isEmpty()) return
    viewModelScope.launch {
      var savedCount = 0
      draftBlocks.entries.sortedBy { it.key }.forEach { (dateIso, cat) ->
        val year = dateIso.take(4).toLongOrNull() ?: 2026L
        val res = repository.addLeaveRecord(
          leaveType = cat.code,
          dateIso = dateIso,
          year = year,
          notes = "Logged via Interactive Leave Builder (${cat.fullTitle})"
        )
        if (res.isSuccess) savedCount++
      }
      _selectedYear.value = 2026L
      _statusBanner.value =
        "Committed $savedCount leave block(s) from Leave Builder to your 2026 Firestore Ledger!"
    }
  }

  fun importCsvRowsToLedger(rows: List<CsvImportRow>, existingRecords: List<LeaveRecord>) {
    if (rows.isEmpty()) return
    viewModelScope.launch {
      val existingKeys = existingRecords.map { "${it.dateIso}_${it.leaveType}" }.toSet()
      var importedCount = 0
      rows.forEach { row ->
        val key = "${row.dateIso}_${row.leaveType}"
        if (key !in existingKeys) {
          val res = repository.addLeaveRecord(
            leaveType = row.leaveType,
            dateIso = row.dateIso,
            year = row.year,
            pairedDateIso = row.pairedDateIso,
            notes = row.notes,
            highlightStatus = row.highlightStatus,
            isHalfDay = row.isHalfDay
          )
          if (res.isSuccess) importedCount++
        }
      }
      _statusBanner.value = if (importedCount > 0) {
        "Imported $importedCount new leave/duty row(s) from CSV spreadsheet into Firestore!"
      } else {
        "All ${rows.size} row(s) in the CSV are already present in your Firestore Ledger."
      }
    }
  }

  fun commitBurnDownSuggestions(slots: List<BurnDownSlotSuggestion>) {
    if (slots.isEmpty()) return
    viewModelScope.launch {
      var booked = 0
      slots.forEach { slot ->
        val res = repository.addLeaveRecord(
          leaveType = slot.suggestedCategory.code,
          dateIso = slot.dateIso,
          year = 2026L,
          notes = "Zero-Waste Year-End Burn-Down: ${slot.title}"
        )
        if (res.isSuccess) booked++
      }
      _selectedYear.value = 2026L
      _statusBanner.value =
        "Booked $booked Year-End Burn-Down date(s) so zero CL/RH credits lapse on Dec 31!"
    }
  }

  fun updateExistingLeaveRecord(updated: LeaveRecord) {
    viewModelScope.launch {
      val res = repository.updateLeaveRecord(updated)
      res.onSuccess {
        _statusBanner.value = "Updated ${updated.category.shortLabel} entry on ${updated.dateIso}!"
      }.onFailure { err ->
        _statusBanner.value = "Error updating entry: ${err.localizedMessage}"
      }
    }
  }

  fun logDualHolidayDutyAndCol(
    dutyDateIso: String,
    colDateIso: String,
    dutyNotes: String
  ) {
    val dutyYear = dutyDateIso.take(4).toLongOrNull() ?: _selectedYear.value
    viewModelScope.launch {
      if (colDateIso.isNotBlank()) {
        val colYear = colDateIso.take(4).toLongOrNull() ?: dutyYear
        repository.addLeaveRecord(
          leaveType = "GH_WORKED",
          dateIso = dutyDateIso,
          year = dutyYear,
          pairedDateIso = colDateIso,
          notes = "$dutyNotes (Paired with COL $colDateIso)",
          highlightStatus = "VERIFIED_GREEN"
        )
        repository.addLeaveRecord(
          leaveType = "IN_LIEU",
          dateIso = dutyDateIso,
          year = colYear,
          pairedDateIso = colDateIso,
          notes = "In Lieu of GH Duty ($dutyDateIso) for COL $colDateIso",
          highlightStatus = "VERIFIED_GREEN"
        )
        repository.addLeaveRecord(
          leaveType = "COL",
          dateIso = colDateIso,
          year = colYear,
          pairedDateIso = dutyDateIso,
          notes = "COL availed against Holiday Call Duty on $dutyDateIso",
          highlightStatus = "VERIFIED_GREEN"
        )
        _statusBanner.value =
          "Logged & 100% Verified: GH Worked ($dutyDateIso) ↔ In-Lieu ↔ COL ($colDateIso)!"
      } else {
        repository.addLeaveRecord(
          leaveType = "GH_WORKED",
          dateIso = dutyDateIso,
          year = dutyYear,
          pairedDateIso = "",
          notes = dutyNotes,
          highlightStatus = "NORMAL"
        )
        val curSuccess = uiState.value as? LeaveUiState.Success
        val bestColBridge = curSuccess?.let { st ->
          AiLeaveRecommendationEngine.scanHolidayDatabaseForLongWeekends(
            summary2026 = st.summary2026,
            colGhAuditAllYears = st.colGhAuditAllYears,
            records = st.allRecords,
            focusGoal = "Consume Expiring GH Worked (FIFO COL)",
            constraints = _optimizerConstraints.value.copy(prioritizeExpiringCol = true)
          ).firstOrNull()
        }
        _statusBanner.value = if (bestColBridge != null) {
          "Banked GH Worked ($dutyDateIso) • Optimal COL bridge unlocked: ${bestColBridge.title} (${bestColBridge.efficiencyMultiplier}x ROI)"
        } else {
          "Banked GH Worked duty credit on $dutyDateIso (Available in 6-Month COL Pool)!"
        }
      }
    }
  }

  fun addWorkingDaysOnlyLeaveBatch(
    leaveType: String,
    workingDateIsos: List<String>,
    pairedDateIso: String,
    notes: String,
    isHalfDay: Boolean = false
  ) {
    if (workingDateIsos.isEmpty()) {
      _statusBanner.value = "Selected range contains only weekends/public holidays — 0 leave credits debited!"
      return
    }
    viewModelScope.launch {
      var saved = 0
      var skippedDupes = 0
      var firstRejectionMsg = ""
      workingDateIsos.forEach { dIso ->
        val yr = dIso.take(4).toLongOrNull() ?: _selectedYear.value
        val res = repository.addLeaveRecord(
          leaveType = leaveType,
          dateIso = dIso,
          year = yr,
          pairedDateIso = pairedDateIso,
          notes = notes,
          highlightStatus = if (pairedDateIso.isNotBlank()) "VERIFIED_GREEN" else "NORMAL",
          isHalfDay = isHalfDay
        )
        res.onSuccess {
          saved++
        }.onFailure { err ->
          skippedDupes++
          if (firstRejectionMsg.isBlank()) {
            firstRejectionMsg = err.localizedMessage ?: "Duplicate date skipped"
          }
        }
      }
      workingDateIsos.firstOrNull()?.take(4)?.toLongOrNull()?.let { _selectedYear.value = it }
      _statusBanner.value = when {
        saved > 0 && skippedDupes > 0 ->
          "⚡ Logged $saved new working day(s) of $leaveType • Auto-rejected $skippedDupes duplicate date(s)!"
        saved > 0 ->
          "Logged $saved working day(s) of $leaveType (Weekends & Public Holidays auto-skipped)!"
        else ->
          "⛔ $firstRejectionMsg"
      }
    }
  }

  fun scrubExistingDuplicatesFromLedger(allRecords: List<LeaveRecord>) {
    viewModelScope.launch {
      val duplicates = com.example.domain.SmartDuplicateLeaveGuardEngine.scanLedgerForExistingDuplicates(allRecords)
      if (duplicates.isEmpty()) {
        _statusBanner.value = "✓ Smart Duplicate Audit: 0 duplicate or conflicting leave entries found in your ledger!"
        return@launch
      }
      var removed = 0
      duplicates.forEach { dupItem ->
        val delRes = repository.deleteLeaveRecord(dupItem.duplicateRecord.id)
        if (delRes.isSuccess) removed++
      }
      _statusBanner.value =
        "✓ Scrubbed $removed duplicate/conflicting leave record(s) from your ledger while preserving verified master entries!"
    }
  }

  fun deleteLeaveWithUndo(record: LeaveRecord) {
    viewModelScope.launch {
      repository.deleteLeaveRecord(record.id).onSuccess {
        _lastDeletedRecord.value = record
        _statusBanner.value = "Removed ${record.category.shortLabel} on ${record.dateIso}. Tap UNDO in Ledger to restore."
      }
    }
  }

  fun undoLastDeletedLeave() {
    val deleted = _lastDeletedRecord.value ?: return
    viewModelScope.launch {
      repository.addLeaveRecord(
        leaveType = deleted.leaveType,
        dateIso = deleted.dateIso,
        year = deleted.year,
        pairedDateIso = deleted.pairedDateIso,
        notes = deleted.notes,
        highlightStatus = deleted.highlightStatus,
        isHalfDay = deleted.isHalfDay
      ).onSuccess {
        _lastDeletedRecord.value = null
        _statusBanner.value = "Restored ${deleted.category.shortLabel} entry on ${deleted.dateIso}!"
      }
    }
  }

  fun dismissUndoDelete() {
    _lastDeletedRecord.value = null
  }

  fun deleteLeave(recordId: String) {
    viewModelScope.launch {
      repository.deleteLeaveRecord(recordId).onSuccess {
        _statusBanner.value = "Leave entry removed."
      }
    }
  }

  // ===========================================================================
  // Clinical Workflow, Sanction Slip Vault & Duty Roster Actions
  // ===========================================================================

  fun openWorkflowForRecord(recordId: String) {
    _pendingWorkflowRecordId.value = recordId
  }

  fun consumePendingWorkflowRecord() {
    _pendingWorkflowRecordId.value = null
  }

  fun toggleProtectLoggedDutyShifts(enabled: Boolean) {
    _protectLoggedDutyShifts.value = enabled
  }

  fun saveLeaveWorkflow(meta: LeaveWorkflowMeta, feedbackMessage: String? = null) {
    viewModelScope.launch {
      repository.upsertLeaveWorkflow(meta).onSuccess {
        _statusBanner.value = feedbackMessage ?: "Updated leave paperwork (${meta.stageEnum.label})."
      }.onFailure { err ->
        _statusBanner.value = "Unable to save paperwork: ${err.localizedMessage ?: "Retry"}"
      }
    }
  }

  fun quickAdvanceWorkflowStage(record: LeaveRecord) {
    val existing = leaveWorkflowsByRecordId.value[record.id]
    val nextStage = (existing?.stageEnum ?: ApprovalStage.PLANNED).nextStage()
    val updated = (existing ?: LeaveWorkflowMeta(recordId = record.id)).copy(
      recordId = record.id,
      stage = nextStage.code
    )
    saveLeaveWorkflow(
      meta = updated,
      feedbackMessage = "${record.category.shortLabel} (${record.dateIso}) marked ${nextStage.label}."
    )
  }

  fun attachSanctionSlipPhoto(recordId: String, sourceUri: android.net.Uri) {
    viewModelScope.launch {
      val saveRes = SanctionSlipVault.savePickedImageToPrivateVault(
        context = appContext,
        recordId = recordId,
        sourceUri = sourceUri
      )
      saveRes.onSuccess { fileName ->
        val existing = leaveWorkflowsByRecordId.value[recordId]
        val oldFile = existing?.slipLocalFileName.orEmpty()
        if (oldFile.isNotBlank() && oldFile != fileName) {
          SanctionSlipVault.deleteSlipFile(appContext, oldFile)
        }
        val promotedStage = when (existing?.stageEnum) {
          null, ApprovalStage.PLANNED, ApprovalStage.SUBMITTED -> ApprovalStage.SANCTIONED.code
          else -> existing.stage
        }
        val updated = (existing ?: LeaveWorkflowMeta(recordId = recordId)).copy(
          recordId = recordId,
          stage = promotedStage,
          slipLocalFileName = fileName
        )
        saveLeaveWorkflow(
          meta = updated,
          feedbackMessage = "Signed sanction slip saved to private vault & marked Sanctioned."
        )
      }.onFailure { err ->
        _statusBanner.value = "Could not save sanction slip: ${err.localizedMessage ?: "Unsupported image"}"
      }
    }
  }

  fun removeSanctionSlipPhoto(recordId: String) {
    viewModelScope.launch {
      val existing = leaveWorkflowsByRecordId.value[recordId] ?: return@launch
      if (existing.slipLocalFileName.isNotBlank()) {
        SanctionSlipVault.deleteSlipFile(appContext, existing.slipLocalFileName)
      }
      saveLeaveWorkflow(
        meta = existing.copy(slipLocalFileName = ""),
        feedbackMessage = "Removed sanction slip attachment."
      )
    }
  }

  fun addDutyShift(dateIso: String, shiftType: DutyShiftType, departmentNote: String = "") {
    viewModelScope.launch {
      repository.addDutyShift(dateIso, shiftType, departmentNote).onSuccess {
        _statusBanner.value = "Tagged ${shiftType.label} on $dateIso (AI Optimizer & Leave Builder protected)."
      }.onFailure { err ->
        _statusBanner.value = "Unable to save duty shift: ${err.localizedMessage}"
      }
    }
  }

  fun addRecurringMonthlyDutyShifts(
    year: Int,
    month: Int,
    dayOfWeek: DayOfWeek,
    shiftType: DutyShiftType,
    departmentNote: String = ""
  ) {
    viewModelScope.launch {
      val ym = YearMonth.of(year, month)
      val fmt = DateTimeFormatter.ISO_LOCAL_DATE
      var count = 0
      for (day in 1..ym.lengthOfMonth()) {
        val date = ym.atDay(day)
        if (date.dayOfWeek == dayOfWeek) {
          val res = repository.addDutyShift(date.format(fmt), shiftType, departmentNote)
          if (res.isSuccess) count++
        }
      }
      _statusBanner.value = "Tagged $count ${shiftType.shortLabel} shift(s) across ${ym.month.name.lowercase().replaceFirstChar { it.uppercase() }} $year."
    }
  }

  fun deleteDutyShift(shiftId: String) {
    viewModelScope.launch {
      repository.deleteDutyShift(shiftId).onSuccess {
        _statusBanner.value = "Duty shift removed from roster."
      }
    }
  }

  fun claimGhColFromDutyShift(candidate: GhColCaptureCandidate) {
    val shift = candidate.shift
    viewModelScope.launch {
      val year = shift.dateIso.take(4).toLongOrNull() ?: _selectedYear.value
      val note = buildString {
        append("${shift.shiftTypeEnum.label} on ${candidate.holidayLabel}")
        if (shift.departmentNote.isNotBlank()) append(" — ${shift.departmentNote}")
      }.take(450)

      val res = repository.addLeaveRecord(
        leaveType = "GH_WORKED",
        dateIso = shift.dateIso,
        year = year,
        pairedDateIso = "",
        notes = note,
        highlightStatus = "NORMAL",
        isHalfDay = false
      )
      res.onSuccess { newRecordId ->
        repository.markDutyShiftColClaimed(shift.id, newRecordId)
        _statusBanner.value = "+1 COL Credit banked for ${shift.dateIso} (${candidate.holidayLabel}) — valid 6 months!"
      }.onFailure { err ->
        _statusBanner.value = "Could not bank COL credit: ${err.localizedMessage}"
      }
    }
  }

  fun clearLapsedColItem(item: ColExpiryItem) {
    if (_isClearingExpiredGh.value) return
    if (item.urgency != com.example.domain.WatchdogColUrgency.EXPIRED) {
      _statusBanner.value = "Only lapsed (>6-month) GH-Worked credits can be cleared."
      return
    }
    viewModelScope.launch {
      _isClearingExpiredGh.value = true
      try {
        val liveRecords = (uiState.value as? LeaveUiState.Success)?.allRecords.orEmpty()
        val liveWorkflows = leaveWorkflowsByRecordId.value
        val recordBackup = liveRecords.filter { it.id == item.recordId }
        val workflowBackup = liveWorkflows[item.recordId]?.let { listOf(it) }.orEmpty()

        val res = repository.deleteExpiredGhWorkedRecords(
          recordIds = listOf(item.recordId),
          workflowIdsToCleanup = liveWorkflows.keys
        )
        res.onSuccess { summary ->
          if (summary.deletedCount > 0 && recordBackup.isNotEmpty()) {
            val actuallyDeleted = recordBackup.filter { it.id in summary.deletedRecordIds }
            val actuallyDeletedWfs = workflowBackup.filter {
              it.recordId.ifBlank { it.id } in summary.deletedRecordIds
            }
            if (actuallyDeleted.isNotEmpty()) {
              _expiredGhUndoBackup.value = ExpiredGhUndoBackup(
                eventId = System.nanoTime(),
                deletedRecords = actuallyDeleted,
                deletedWorkflows = actuallyDeletedWfs,
                snackbarMessage = "Deleted expired GH Worked (${item.ghDateIso})."
              )
            }
          }
          _statusBanner.value = when {
            summary.deletedCount > 0 && summary.queuedOffline ->
              "Cleared expired GH Worked (${item.ghDateIso}) locally — queued to sync with Firestore."
            summary.deletedCount > 0 ->
              "Deleted expired GH Worked (${item.ghDateIso}) from Firestore."
            else ->
              "Expired GH Worked (${item.ghDateIso}) was already removed."
          }
        }.onFailure { err ->
          _statusBanner.value =
            "Could not delete expired GH Worked (${item.ghDateIso}): ${err.localizedMessage ?: "Please retry."}"
        }
      } finally {
        _isClearingExpiredGh.value = false
      }
    }
  }

  fun clearAllLapsedColItems(items: List<ColExpiryItem>) {
    if (_isClearingExpiredGh.value) return
    viewModelScope.launch {
      _isClearingExpiredGh.value = true
      try {
        // Combine passed items + live watchdog expired items + any previously archived expired records still in Firestore
        val liveRecords = (uiState.value as? LeaveUiState.Success)?.allRecords.orEmpty()
        val liveWorkflows = leaveWorkflowsByRecordId.value
        val fullUnfilteredWatchdog = ExpiryWatchdogEngine.evaluate(
          records = liveRecords,
          workflowsByRecordId = liveWorkflows,
          clearedLapsedRecordKeys = emptySet()
        )
        val allExpiredIds = buildList {
          items.filter { it.urgency == com.example.domain.WatchdogColUrgency.EXPIRED }
            .forEach { if (it.recordId.isNotBlank()) add(it.recordId) }
          fullUnfilteredWatchdog.expiringCols
            .filter { it.urgency == com.example.domain.WatchdogColUrgency.EXPIRED }
            .forEach { if (it.recordId.isNotBlank()) add(it.recordId) }
        }.distinct()

        if (allExpiredIds.isEmpty()) {
          _clearedLapsedColKeys.value = LapsedColArchiveStore.restoreAll(appContext, currentUserId)
          _statusBanner.value = "All clear — no expired GH-Worked credits remaining in Firestore."
          return@launch
        }

        val expiredIdSet = allExpiredIds.toSet()
        val recordsBackupSnapshot = liveRecords.filter { it.id in expiredIdSet }
        val workflowsBackupSnapshot = liveWorkflows
          .filterKeys { it in expiredIdSet }
          .values
          .toList()

        val res = repository.deleteExpiredGhWorkedRecords(
          recordIds = allExpiredIds,
          workflowIdsToCleanup = liveWorkflows.keys
        )
        res.onSuccess { summary ->
          if (summary.failedCount == 0) {
            _clearedLapsedColKeys.value = LapsedColArchiveStore.restoreAll(appContext, currentUserId)
          }
          if (summary.deletedCount > 0 && recordsBackupSnapshot.isNotEmpty()) {
            val actuallyDeleted = recordsBackupSnapshot.filter { it.id in summary.deletedRecordIds }
            val actuallyDeletedWfs = workflowsBackupSnapshot.filter {
              it.recordId.ifBlank { it.id } in summary.deletedRecordIds
            }
            if (actuallyDeleted.isNotEmpty()) {
              val count = actuallyDeleted.size
              _expiredGhUndoBackup.value = ExpiredGhUndoBackup(
                eventId = System.nanoTime(),
                deletedRecords = actuallyDeleted,
                deletedWorkflows = actuallyDeletedWfs,
                snackbarMessage = "Cleared $count expired GH-Worked record${if (count != 1) "s" else ""}."
              )
            }
          }
          _statusBanner.value = when {
            summary.failedCount > 0 ->
              "Deleted ${summary.deletedCount} expired GH-Worked record(s); ${summary.failedCount} could not be removed — tap again to retry."
            summary.deletedCount == 0 && summary.skippedInvalidCount > 0 ->
              "Skipped ${summary.skippedInvalidCount} malformed expired GH-Worked record ID(s)."
            summary.queuedOffline ->
              "Cleared ${summary.deletedCount} expired GH-Worked record(s) — queued to sync with Firestore."
            else ->
              "Deleted ${summary.deletedCount} expired GH-Worked record${if (summary.deletedCount != 1) "s" else ""} from Firestore."
          }
        }.onFailure { err ->
          _statusBanner.value =
            "Could not clear expired GH-Worked records: ${err.localizedMessage ?: "Please check connection and retry."}"
        }
      } finally {
        _isClearingExpiredGh.value = false
      }
    }
  }

  fun undoClearExpiredGhRecords() {
    if (_isClearingExpiredGh.value) return
    val backup = _expiredGhUndoBackup.value ?: return
    _expiredGhUndoBackup.value = null
    viewModelScope.launch {
      _isClearingExpiredGh.value = true
      try {
        val res = repository.restoreDeletedGhWorkedRecords(
          records = backup.deletedRecords,
          workflows = backup.deletedWorkflows
        )
        res.onSuccess { summary ->
          _statusBanner.value = when {
            summary.failedCount > 0 ->
              "Restored ${summary.restoredCount} expired GH-Worked record(s); ${summary.failedCount} could not be restored."
            summary.queuedOffline ->
              "Restored ${summary.restoredCount} expired GH-Worked record(s) — queued to sync with Firestore."
            summary.restoredCount > 0 ->
              "Restored ${summary.restoredCount} expired GH-Worked record${if (summary.restoredCount != 1) "s" else ""} from backup."
            else ->
              "No records needed restoration."
          }
        }.onFailure { err ->
          _statusBanner.value =
            "Could not restore deleted GH-Worked records: ${err.localizedMessage ?: "Please retry."}"
        }
      } finally {
        _isClearingExpiredGh.value = false
      }
    }
  }

  fun dismissExpiredGhUndoBackup(eventId: Long) {
    if (_expiredGhUndoBackup.value?.eventId == eventId) {
      _expiredGhUndoBackup.value = null
    }
  }

  fun restoreArchivedLapsedCols() {
    _clearedLapsedColKeys.value = LapsedColArchiveStore.restoreAll(
      context = appContext,
      userId = currentUserId
    )
    _statusBanner.value = "Restored previously hidden lapsed GH-Worked credits."
  }
}

data class ExpiredGhUndoBackup(
  val eventId: Long,
  val deletedRecords: List<LeaveRecord>,
  val deletedWorkflows: List<LeaveWorkflowMeta>,
  val snackbarMessage: String
)
