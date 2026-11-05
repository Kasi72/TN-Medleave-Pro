package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ColGhAuditSummary
import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.data.LeaveRepository
import com.example.data.SmartLeaveBridge
import com.example.data.TnHolidayItem
import com.example.data.local.LocalHolidayRepository
import com.example.data.local.TnHolidayDatabase
import com.example.domain.AiLeaveRecommendationEngine
import com.example.domain.AiRecommendationUiState
import com.example.domain.BurnDownSlotSuggestion
import com.example.domain.CsvImportRow
import com.example.domain.CustomRangeAnalysis
import com.example.domain.HolidaySyncStatus
import com.example.domain.LeaveOptimizerEngine
import com.example.domain.TnHolidayBackgroundSyncService
import com.example.domain.TnHolidays2027Catalog
import com.example.domain.YearCreditSummary
import com.example.widget.MedLeaveQuotaAppWidget
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

    // 2. Run automatic background web/API sync if due
    viewModelScope.launch {
      localHolidayRepository.ensureSeededWithDefaultsIfEmpty()
      if (TnHolidayBackgroundSyncService.shouldRunAutomaticBackgroundSync(appContext)) {
        triggerHolidaySync(customUrlOrFeedNote = "", isAutomaticBackground = true)
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

    val bridges2026 = LeaveOptimizerEngine.generateOptimalLeaveBridges(
      summary2026 = summary2026,
      records2026 = records.filter { it.year == 2026L }
    )
    val bridges = if (year == 2027L) {
      TnHolidays2027Catalog.smartBridges2027 + bridges2026
    } else {
      bridges2026 + TnHolidays2027Catalog.smartBridges2027
    }

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
        nextHolidayText = "★ Next Bridge: Nov 08 Deepavali • Dec 24–27 Christmas (4d Stretch)",
        updatedLabel = "LIVE 2026"
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

  fun triggerHolidaySync(
    customUrlOrFeedNote: String = "",
    isAutomaticBackground: Boolean = false
  ) {
    if (_holidaySyncStatus.value.isSyncing) return
    _holidaySyncStatus.value = _holidaySyncStatus.value.copy(
      isSyncing = true,
      hasError = false
    )
    viewModelScope.launch {
      val syncResult = TnHolidayBackgroundSyncService.syncLatestHolidaysToLocalDb(
        context = appContext,
        localRepo = localHolidayRepository,
        customSourceUrlOrNote = customUrlOrFeedNote
      )
      syncResult.onSuccess { updatedStatus ->
        _holidaySyncStatus.value = updatedStatus
        _holidayDbVersion.value = System.currentTimeMillis()
        if (!isAutomaticBackground) {
          _statusBanner.value =
            "Synced TN Holiday Room DB (${updatedStatus.totalGhCount} GH • ${updatedStatus.totalRhCount} RH). Optimizer updated!"
        }
      }.onFailure { err ->
        _holidaySyncStatus.value = _holidaySyncStatus.value.copy(
          isSyncing = false,
          hasError = true,
          statusSummary = err.localizedMessage ?: "Holiday sync encountered an error."
        )
      }
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
        _statusBanner.value = "Logged $leaveType on $dateIso"
      }.onFailure { err ->
        _statusBanner.value = "Error saving leave: ${err.localizedMessage}"
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
      res.onSuccess {
        _selectedYear.value = year
        _statusBanner.value = if (startDate == endDate) {
          "Logged $leaveType on $startDate ($status)"
        } else {
          "Logged $leaveType spell ($startDate → $endDate, $status)"
        }
      }.onFailure { err ->
        _statusBanner.value = "Error saving LeaveEntry: ${err.localizedMessage}"
      }
    }
  }

  fun generateAiLeaveRecommendations(
    focusGoal: String,
    state: LeaveUiState.Success
  ) {
    if (_aiRecommendationState.value is AiRecommendationUiState.Loading) return
    _aiRecommendationState.value = AiRecommendationUiState.Loading
    viewModelScope.launch {
      val result = AiLeaveRecommendationEngine.generateOptimalRecommendations(
        focusGoal = focusGoal,
        summary2026 = state.summary2026,
        colGhAuditAllYears = state.colGhAuditAllYears,
        records2026 = state.allRecords.filter { it.year == 2026L }
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

  fun applySmartBridgeToLedger(bridge: SmartLeaveBridge) {
    viewModelScope.launch {
      var added = 0
      bridge.dayByDayBreakdown.forEach { step ->
        when (step.badgeType) {
          "RH" -> {
            val year = step.dateIso.take(4).toLongOrNull() ?: 2026L
            repository.addLeaveRecord(
              leaveType = "RH",
              dateIso = step.dateIso,
              year = year,
              notes = "Smart Bridge (${bridge.title}): ${step.description}"
            )
            added++
          }
          "COL" -> {
            val year = step.dateIso.take(4).toLongOrNull() ?: 2026L
            repository.addLeaveRecord(
              leaveType = "COL",
              dateIso = step.dateIso,
              year = year,
              notes = "AI Smart Bridge (${bridge.title}): ${step.description}"
            )
            added++
          }
          "LEAVE_CREDIT", "CL" -> {
            if (!step.description.contains("Already Logged", ignoreCase = true) &&
              !step.description.contains("COL Logged", ignoreCase = true)
            ) {
              val year = step.dateIso.take(4).toLongOrNull() ?: 2026L
              repository.addLeaveRecord(
                leaveType = "CL",
                dateIso = step.dateIso,
                year = year,
                notes = "Smart Bridge (${bridge.title}): ${step.description}"
              )
              added++
            }
          }
        }
      }
      _statusBanner.value = if (added > 0) {
        "Applied $added optimal leave days for '${bridge.title}' to your 2026 Ledger!"
      } else {
        "All leave days in '${bridge.title}' are already logged in your ledger."
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
        _statusBanner.value =
          "Banked GH Worked duty credit on $dutyDateIso (Available in 6-Month COL Pool)!"
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
        if (res.isSuccess) saved++
      }
      workingDateIsos.firstOrNull()?.take(4)?.toLongOrNull()?.let { _selectedYear.value = it }
      _statusBanner.value =
        "Logged $saved working day(s) of $leaveType (Weekends & Public Holidays auto-skipped)!"
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
}
