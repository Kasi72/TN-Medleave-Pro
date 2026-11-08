package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ColGhAuditSummary
import com.example.domain.TamilNaduLeaveRulesService
import com.example.domain.YearCreditSummary
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class GeminiModelTier(
  val modelName: String,
  val displayName: String,
  val subtitle: String
) {
  FAST(
    modelName = "gemini-3.5-flash-lite",
    displayName = "Fast 3.5 Lite",
    subtitle = "Instant holiday & rule lookup"
  ),
  GENERAL(
    modelName = "gemini-3.8-flash",
    displayName = "General 3.8 Flash",
    subtitle = "Balanced TN Leave Advisor (Default)"
  ),
  DEEP_REASONING(
    modelName = "gemini-4-argon",
    displayName = "Deep 4 Argon",
    subtitle = "Multi-year COL audit & FR legal analysis"
  )
}

data class ChatMessageItem(
  val id: String = UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String,
  val modelLabel: String,
  val isError: Boolean = false
)

data class AiChatUiState(
  val selectedTier: GeminiModelTier = GeminiModelTier.GENERAL,
  val messages: List<ChatMessageItem> = listOf(
    ChatMessageItem(
      isUser = false,
      text = "Vanakkam Doctor! I am your **Tamil Nadu Medical Service Leave & Holiday Optimization Advisor** for the Department of Psychiatry, Government Erode Medical College & Hospital.\n\n" +
        "I have live access to your **2023–2027 Leave Ledger**, your **COL ↔ GH Worked 1-to-1 Audit**, and the **2026–2027 TN Restricted (RL) & Public Holiday (GH) Lists**.\n\n" +
        "Ask me to:\n" +
        "• Recommend optimal 2026–2027 leave bridges using your remaining CL, RH, or unused GH Worked credits\n" +
        "• Audit any unlinked COL or unclaimed GH Worked duty dates\n" +
        "• Clarify Tamil Nadu Leave Rules, 1933 & Fundamental Rules (FR Annexure VII) for HODs",
      modelLabel = GeminiModelTier.GENERAL.displayName
    )
  ),
  val isGenerating: Boolean = false,
  val errorMessage: String? = null
)

class AiChatViewModel : ViewModel() {

  private val _chatState = MutableStateFlow(AiChatUiState())
  val chatState: StateFlow<AiChatUiState> = _chatState.asStateFlow()

  private var latestLedgerContext: String = ""

  fun updateLiveLedgerContext(
    summary2026: YearCreditSummary,
    auditSelectedYear: ColGhAuditSummary,
    auditAllYears: ColGhAuditSummary
  ) {
    val unusedGhDates = auditSelectedYear.unusedGhRecords.joinToString(", ") {
      "${ClinicalDateFormatter.formatFriendlyDate(it.dateIso)} [${it.dateIso}] (${it.notes.ifBlank { "GH Worked" }})"
    }

    latestLedgerContext = """
      LIVE FIRESTORE LEDGER CONTEXT FOR HOD, DEPT. OF PSYCHIATRY, GOVT. ERODE MEDICAL COLLEGE:
      • 2026 Leave Usage & Remaining Balances:
        - Casual Leave (CL): Used ${summary2026.clTaken} / ${summary2026.clQuota}, Remaining = ${summary2026.clRemaining}
        - Restricted Holiday (RH): Used ${summary2026.rhTaken} / ${summary2026.rhQuota}, Remaining = ${summary2026.rhRemaining}
        - Compensatory Off (COL): Taken ${summary2026.colTaken}, GH Worked = ${summary2026.ghWorked}, In Lieu = ${summary2026.inLieuWorked}
        - Earned Leave (EL) Taken in 2026: ${summary2026.elTaken}, Medical Leave (ML) Taken in 2026: ${summary2026.mlTaken}, Public Holiday (PL): ${summary2026.plTaken}
      • Selected Year COL ↔ GH Worked Audit:
        - Total COL Taken: ${auditSelectedYear.totalColTaken}, Total GH Worked: ${auditSelectedYear.totalGhWorked}
        - Fully Accounted 1:1 Paired COL: ${auditSelectedYear.fullyAccountedColCount}
        - Unaccounted COL Needing Pair: ${auditSelectedYear.unaccountedColCount}
        - Available Unused GH Worked Credits (${auditSelectedYear.availableUnusedGhCount}): $unusedGhDates
      • Multi-Year (2023–2026) COL ↔ GH Worked Audit Totals:
        - Total COL Taken (2023–2026): ${auditAllYears.totalColTaken}, Total GH Worked: ${auditAllYears.totalGhWorked}
        - Fully Accounted 1:1 Paired COL: ${auditAllYears.fullyAccountedColCount}, Unaccounted COL: ${auditAllYears.unaccountedColCount}, Available Unused GH Credits: ${auditAllYears.availableUnusedGhCount}
    """.trimIndent()
  }

  fun selectModelTier(tier: GeminiModelTier) {
    _chatState.update { it.copy(selectedTier = tier, errorMessage = null) }
  }

  fun clearConversation() {
    val tier = _chatState.value.selectedTier
    _chatState.value = AiChatUiState(selectedTier = tier)
  }

  fun sendMessage(userPrompt: String) {
    val trimmed = userPrompt.trim()
    if (trimmed.isEmpty() || _chatState.value.isGenerating) return

    val activeTier = _chatState.value.selectedTier
    val priorMessages = _chatState.value.messages

    val userMessage = ChatMessageItem(
      isUser = true,
      text = trimmed,
      modelLabel = "HOD Psychiatry"
    )

    _chatState.update {
      it.copy(
        messages = it.messages + userMessage,
        isGenerating = true,
        errorMessage = null
      )
    }

    viewModelScope.launch {
      try {
        val systemPromptText = buildSystemInstruction()
        val model = Firebase.ai.generativeModel(
          modelName = activeTier.modelName,
          systemInstruction = content { text(systemPromptText) }
        )

        val historyContent: List<Content> = priorMessages
          .filter { !it.isError }
          .map { msg ->
            content(role = if (msg.isUser) "user" else "model") {
              text(msg.text)
            }
          }

        val chat = model.startChat(history = historyContent)
        val response = withTimeout(60_000L) {
          chat.sendMessage(trimmed)
        }

        val replyText = response.text?.takeIf { it.isNotBlank() }
          ?: "I processed your request under Tamil Nadu Leave Rules, 1933, but received an empty response. Please try rephrasing."

        val botMessage = ChatMessageItem(
          isUser = false,
          text = replyText,
          modelLabel = activeTier.displayName
        )

        _chatState.update {
          it.copy(
            messages = it.messages + botMessage,
            isGenerating = false
          )
        }
      } catch (e: TimeoutCancellationException) {
        Log.e("AiChatViewModel", "Gemini request timed out", e)
        val timeoutMsg = "The AI Advisor request timed out after 60 seconds. Please try switching to Fast 3.5 Lite or sending a shorter query."
        _chatState.update {
          it.copy(
            messages = it.messages + ChatMessageItem(
              isUser = false,
              text = timeoutMsg,
              modelLabel = activeTier.displayName,
              isError = true
            ),
            isGenerating = false,
            errorMessage = timeoutMsg
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e("AiChatViewModel", "Gemini chat error", e)
        val errText = "Unable to reach Firebase AI Logic (${activeTier.modelName}): ${e.localizedMessage ?: "Unknown error"}"
        _chatState.update {
          it.copy(
            messages = it.messages + ChatMessageItem(
              isUser = false,
              text = errText,
              modelLabel = activeTier.displayName,
              isError = true
            ),
            isGenerating = false,
            errorMessage = errText
          )
        }
      }
    }
  }

  private fun buildSystemInstruction(): String {
    val rulesText = TamilNaduLeaveRulesService.getAllRuleSpecs().joinToString("\n") { spec ->
      "• ${spec.category.fullTitle} (${spec.category.code}) [${spec.statutoryCitation}]: Entitlement=${spec.annualEntitlementDesc}, MaxStretch=${spec.maxContinuousStretchDays}d, CombineWithCL=${spec.canCombineWithCasualLeave}, InterveningHolidaysCountAsLeave=${spec.interveningHolidaysCountAsLeave}. Clause: ${spec.medicalOfficerSpecialClause}"
    }

    return """
      You are the authoritative Tamil Nadu Medical Service Leave Rules & Holiday Optimizer AI Advisor built exclusively for the Head of Department (HOD), Department of Psychiatry, Government Erode Medical College & Hospital (Tamil Nadu Medical Service).

      STATUTORY RULES ENGINE (Tamil Nadu Leave Rules, 1933 & Fundamental Rules Annexure VII):
      $rulesText

      CRITICAL OPTIMIZATION & AUDIT PRINCIPLES FOR HOD PSYCHIATRY:
      1. 10-Day Total Absence Cap (FR Annexure VII): Casual Leave (CL) + Restricted Holidays (RH) + Compensatory Off Leave (COL) combined with prefixed/suffixed/intervening weekends and Government Holidays (GH) MUST NEVER exceed 10 consecutive days of total absence at a single stretch.
      2. Intervening Holidays Rule:
         - Under CL / RH / COL: Intervening Saturdays, Sundays, and Government Holidays (GH) are NOT counted as leave days (0 leave debit) so long as total absence <= 10 days.
         - Under Earned Leave (EL) or Medical Leave (ML): Intervening holidays ARE swallowed and debited as EL/ML. Only prefixing and suffixing holidays are exempt. Never use EL to bridge short holiday gaps!
      3. COL ↔ GH Worked 1-to-1 Audit: Every COL must be paired with a specific Sunday/GH hospital duty date (OPD, Emergency Psychiatric Board, DME inspection) and availed within 6 months.
      4. Key 2026 TN Restricted Holidays (RL) & Bridge Opportunities:
         - Aug 21 (Fri RL Varalakshmi Viratham) -> Aug 28 (Fri RL Gayathri Japam) with Aug 25 Milad-un-Nabi GH & Aug 26 Onam RL
         - Sep 4 (Fri GH Krishna Jayanthi) -> Sep 14 (Mon GH Vinayakar Chathurthi) with Sep 12 (Sat RL Sama Upakarma)
         - Oct 19 (Mon Ayudha Pooja GH) & Oct 20 (Tue Vijaya Dasami GH)
         - Nov 8 (Sun Deepavali GH) & Nov 9 (Mon RL/CL bridge)
         - Dec 24 (Thu RL Christmas Eve / Arudhra Darisanam) + Dec 25 (Fri Christmas GH) -> Dec 31 (Thu RL New Year's Eve) -> Jan 1, 2027 (Fri New Year GH).

      $latestLedgerContext

      Keep responses concise, structured with clear bullet points, specific ISO dates, leave credit costs, and exact multiplier yields (e.g., "2 Leave Credits -> 5 Days Off [2.5x]").
    """.trimIndent()
  }
}
