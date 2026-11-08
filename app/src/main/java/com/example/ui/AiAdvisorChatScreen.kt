package com.example.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.TnHolidays2027Catalog
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeElNeutral
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.SapphireAccent
import com.example.ui.theme.SapphirePrimary

@Composable
fun AiAdvisorChatTabScreen(
  leaveState: LeaveUiState.Success,
  chatViewModel: AiChatViewModel
) {
  val chatState by chatViewModel.chatState.collectAsStateWithLifecycle()
  var inputPrompt by rememberSaveable { mutableStateOf("") }
  var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
  val listState = rememberLazyListState()

  val auditYear = leaveState.colGhAuditYear
  val remCl2026 = leaveState.summary2026.clRemaining
  val remRh2026 = leaveState.summary2026.rhRemaining
  val gh2027Count = TnHolidays2027Catalog.tnGovtPublicHolidays2027.size
  val rh2027Count = TnHolidays2027Catalog.tnRestrictedHolidays2027.size

  LaunchedEffect(leaveState.allRecords.size, leaveState.selectedYear) {
    chatViewModel.updateLiveLedgerContext(
      summary2026 = leaveState.summary2026,
      auditSelectedYear = leaveState.colGhAuditYear,
      auditAllYears = leaveState.colGhAuditAllYears
    )
  }

  LaunchedEffect(chatState.messages.size, chatState.isGenerating, selectedSubTab) {
    if (selectedSubTab == 0 && chatState.messages.isNotEmpty()) {
      listState.animateScrollToItem(chatState.messages.size - 1)
    }
  }

  val subTabs = listOf(
    SubTabOption(
      index = 0,
      title = "Live AI Chat",
      badge = "${chatState.messages.size}",
      accentColor = EmeraldOptimal
    ),
    SubTabOption(
      index = 1,
      title = "Prompts & AI Model",
      badge = "3 Tiers",
      accentColor = BadgeColCyan
    ),
    SubTabOption(
      index = 2,
      title = "2027 & Rules Guide",
      badge = "FR 85",
      accentColor = BadgePlGold
    )
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .testTag("ai_advisor_chat_screen")
  ) {
    Spacer(modifier = Modifier.height(6.dp))
    EnterpriseSubTabSwitcher(
      tabs = subTabs,
      selectedIndex = selectedSubTab,
      onSelectTab = { selectedSubTab = it }
    )
    Spacer(modifier = Modifier.height(8.dp))

    when (selectedSubTab) {
      0 -> {
        // Compact Executive Grounding Banner so Chat Thread gets maximum vertical space
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 3.dp,
          shadowElevation = 3.dp,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
        ) {
          Row(
            modifier = Modifier
              .background(
                Brush.linearGradient(
                  listOf(
                    SapphirePrimary.copy(alpha = 0.95f),
                    SapphireAccent.copy(alpha = 0.88f)
                  )
                )
              )
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(EmeraldOptimal.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Psychology,
                  contentDescription = "AI Leave Advisor",
                  tint = Color(0xFF6EE7B7),
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = chatState.selectedTier.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6EE7B7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  )
                  Text(
                    text = "• Grounded (${leaveState.allRecords.size} entries)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                  )
                }
                Text(
                  text = "2026 Left: ${formatDoubleDays(remCl2026)} CL, $remRh2026 RH, ${auditYear.availableUnusedGhCount} GH • 2027 DB: $gh2027Count GH, $rh2027Count RL",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.9f),
                  fontSize = 11.sp,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
              onClick = { chatViewModel.clearConversation() },
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .testTag("clear_chat_button")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Reset Chat",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        // Scrollable Multi-Turn Conversation Thread
        LazyColumn(
          state = listState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag("chat_messages_list"),
          contentPadding = PaddingValues(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(chatState.messages, key = { it.id }) { msg ->
            ChatBubbleRow(message = msg)
          }

          if (chatState.isGenerating) {
            item(key = "generating_indicator") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = EmeraldOptimal
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "${chatState.selectedTier.displayName} is analyzing TN Leave Rules & your 2023–2027 ledger...",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        // Message Composer Input Bar
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 4.dp,
          shadowElevation = 6.dp,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 96.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = inputPrompt,
              onValueChange = { inputPrompt = it },
              placeholder = {
                Text(
                  text = "Ask about TN Leave Rules, COL audit, or 2026–2027 bridges...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("chat_input_field"),
              shape = RoundedCornerShape(16.dp),
              maxLines = 3,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldOptimal,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
              )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
              onClick = {
                val toSend = inputPrompt
                if (toSend.isNotBlank() && !chatState.isGenerating) {
                  inputPrompt = ""
                  chatViewModel.sendMessage(toSend)
                }
              },
              enabled = inputPrompt.isNotBlank() && !chatState.isGenerating,
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                  if (inputPrompt.isNotBlank() && !chatState.isGenerating) {
                    EmeraldOptimal
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant
                  }
                )
                .testTag("send_chat_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Message",
                tint = if (inputPrompt.isNotBlank() && !chatState.isGenerating) {
                  Color.White
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                }
              )
            }
          }
        }
      }

      1 -> {
        val promptGroups = listOf(
          "2026 & 2027 Leave Bridge Planning" to listOf(
            "Best 2026 bridges for my ${formatDoubleDays(remCl2026)} CL & $remRh2026 RH",
            "Plan Dec 2026 Christmas & Jan 2027 New Year cross-year leave spell",
            "Analyze Jan 2027 Pongal 6-day megabridge using 2027 GH & RL calendars"
          ),
          "COL ↔ GH Worked Audit & Expiry" to listOf(
            "Audit my COL & ${auditYear.availableUnusedGhCount} unused GH Worked credits",
            "Which of my GH Worked credits are closest to the 180-day FIFO expiry?",
            "Draft an official Dean note for availing COL against Sunday/GH Hospital Duty"
          ),
          "TN Fundamental Rules & Statutory Caps" to listOf(
            "Explain TN Leave Rules 1933 EL vs CL intervening holidays",
            "Verify the 10-day maximum total absence rule when combining CL + RH + Sundays"
          )
        )

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 104.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          item {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 2.dp,
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldOptimal.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "GEMINI AI MODEL TIER SELECTOR",
                  style = MaterialTheme.typography.labelMedium,
                  color = EmeraldOptimal,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = chatState.selectedTier.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(4.dp),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  GeminiModelTier.entries.forEach { tier ->
                    val selected = chatState.selectedTier == tier
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) EmeraldOptimal else Color.Transparent)
                        .clickable { chatViewModel.selectModelTier(tier) }
                        .padding(vertical = 9.dp, horizontal = 4.dp)
                        .testTag("model_tier_${tier.name.lowercase()}"),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = tier.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }
                }
              }
            }
          }

          promptGroups.forEachIndexed { groupIdx, (groupTitle, prompts) ->
            item(key = "group_$groupIdx") {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                  .fillMaxWidth()
                  .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                    RoundedCornerShape(20.dp)
                  )
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text(
                    text = groupTitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldOptimal,
                    fontWeight = FontWeight.Bold
                  )
                  prompts.forEachIndexed { idx, prompt ->
                    Surface(
                      shape = RoundedCornerShape(14.dp),
                      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !chatState.isGenerating) {
                          selectedSubTab = 0
                          chatViewModel.sendMessage(prompt)
                        }
                        .testTag("quick_prompt_chip_${groupIdx}_$idx")
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          imageVector = Icons.Default.AutoAwesome,
                          contentDescription = null,
                          tint = EmeraldOptimal,
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                          text = prompt,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurface,
                          fontWeight = FontWeight.Medium
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      2 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 104.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          item {
            RuleReferenceCard(
              badge = "2027 ROLLOVER ENGINE",
              title = "What Happens When 2027 Starts?",
              bullets = listOf(
                "Both the 2027 TN Government Public Holiday (GH) calendar ($gh2027Count dates) and the 2027 Restricted/Religious Holiday (RH/RL) calendar ($rh2027Count dates) are pre-seeded in Room DB (`tn_holidays`) and refreshed automatically via `TnHolidayBackgroundSyncService`.",
                "Annual quotas automatically reset on Jan 1, 2027: 12.0 CL and 3 RH/RL fresh credits are provisioned for 2027 while your 2023–2026 historical ledgers remain permanently archived.",
                "Cross-year leave spells (e.g., Dec 2026 Christmas into Jan 2027 New Year/Pongal) and Unused GH Worked credits from late 2026 seamlessly carry across into 2027 within the 180-day FIFO window."
              )
            )
          }
          item {
            RuleReferenceCard(
              badge = "TN LEAVE RULES 1933 • FR 85",
              title = "CL/RH vs EL Intervening Holiday Splicing",
              bullets = listOf(
                "Casual Leave (CL) & Restricted Holidays (RH/RL) can be prefixed or suffixed to Sundays and Government Public Holidays (GH) without those holidays counting as leave.",
                "Total continuous absence from headquarters (CL + RH + COL + prefixed/suffixed/intervening weekends & GH) must not exceed 10 calendar days at a stretch.",
                "Earned Leave (EL) counts intervening Saturdays, Sundays, and GH inside the start-to-end spell as EL consumed (unless only prefixed/suffixed at the outer boundaries)."
              )
            )
          }
          item {
            RuleReferenceCard(
              badge = "DME / IFHRMS AUDIT RULE",
              title = "COL ↔ GH Worked 6-Month (180-Day) FIFO Rule",
              bullets = listOf(
                "Every Compensatory Off Leave (COL) must be paired 1-to-1 with a specific Sunday or Government Holiday (`GH_WORKED`) where 24-hour / clinical hospital duty was performed.",
                "Each `GH_WORKED` credit must be availed as COL within 6 months (180 days) from the date of holiday duty, matched strictly in First-In, First-Out (FIFO) chronological order."
              )
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RuleReferenceCard(
  badge: String,
  title: String,
  bullets: List<String>
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, EmeraldOptimal.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.VerifiedUser,
          contentDescription = null,
          tint = EmeraldOptimal,
          modifier = Modifier.size(15.dp)
        )
        Text(
          text = badge,
          style = MaterialTheme.typography.labelSmall,
          color = EmeraldOptimal,
          fontWeight = FontWeight.Bold
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      bullets.forEach { bullet ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Top
        ) {
          Text(
            text = "• ",
            color = EmeraldOptimal,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = bullet,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
          )
        }
      }
    }
  }
}

@Composable
private fun ChatBubbleRow(message: ChatMessageItem) {
  val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
  val bubbleShape = if (message.isUser) {
    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
  } else {
    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp)
  }

  val containerColor = when {
    message.isUser -> MaterialTheme.colorScheme.primary
    message.isError -> MaterialTheme.colorScheme.errorContainer
    else -> MaterialTheme.colorScheme.surface
  }

  val contentColor = when {
    message.isUser -> MaterialTheme.colorScheme.onPrimary
    message.isError -> MaterialTheme.colorScheme.onErrorContainer
    else -> MaterialTheme.colorScheme.onSurface
  }

  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = alignment
  ) {
    Surface(
      shape = bubbleShape,
      color = containerColor,
      tonalElevation = if (message.isUser) 0.dp else 2.dp,
      shadowElevation = 2.dp,
      modifier = Modifier
        .widthIn(max = 330.dp)
        .border(
          width = 1.dp,
          color = if (message.isUser) {
            Color.Transparent
          } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
          },
          shape = bubbleShape
        )
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(
                when {
                  message.isUser -> BadgeClGreen
                  message.modelLabel.contains("Argon") -> BadgeColCyan
                  message.modelLabel.contains("Lite") -> BadgeElNeutral
                  else -> EmeraldOptimal
                }
              )
          )
          Text(
            text = message.modelLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (message.isUser) {
              Color.White.copy(alpha = 0.8f)
            } else {
              EmeraldOptimal
            },
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyMedium,
          color = contentColor,
          lineHeight = 20.sp
        )
      }
    }
  }
}
