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
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.LocalNeumorphicColors

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
        val neu = LocalNeumorphicColors.current
        // Compact Neumorphic Executive Grounding Banner
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .neumorphicExtruded(
              shape = IosSharpCardShape,
              elevation = 4.dp,
              blurRadius = 8.dp,
              accentTint = neu.primaryAccent,
              borderAlpha = 0.34f,
              neu = neu
            )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
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
                  .size(34.dp)
                  .neumorphicInset(
                    shape = IosSharpButtonShape,
                    depth = 2.dp,
                    blurRadius = 4.dp,
                    accentTint = neu.primaryAccent,
                    accentTintAlpha = 0.12f,
                    neu = neu
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Psychology,
                  contentDescription = "AI Leave Advisor",
                  tint = neu.primaryAccent,
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
                    color = neu.primaryAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  )
                  Text(
                    text = "• Grounded (${leaveState.allRecords.size} entries)",
                    style = MaterialTheme.typography.labelSmall,
                    color = neu.textSecondary,
                    fontSize = 10.sp
                  )
                }
                Text(
                  text = "2026 Left: ${formatDoubleDays(remCl2026)} CL, $remRh2026 RH, ${auditYear.availableUnusedGhCount} GH • 2027 DB: $gh2027Count GH, $rh2027Count RL",
                  style = MaterialTheme.typography.bodySmall,
                  color = neu.textPrimary,
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
                .neumorphicExtruded(
                  shape = IosSharpButtonShape,
                  elevation = 2.dp,
                  blurRadius = 4.dp,
                  borderAlpha = 0.25f,
                  neu = neu
                )
                .testTag("clear_chat_button")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Reset Chat",
                tint = neu.textPrimary,
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

        // Neumorphic Message Composer Input Bar
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 96.dp)
            .neumorphicExtruded(
              shape = IosSharpCardShape,
              elevation = 4.5.dp,
              blurRadius = 9.dp,
              accentTint = neu.primaryAccent,
              borderAlpha = 0.28f,
              neu = neu
            )
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
                  color = neu.textSecondary
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("chat_input_field"),
              shape = IosSharpWellShape,
              maxLines = 3,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = neu.textPrimary,
                unfocusedTextColor = neu.textPrimary,
                focusedContainerColor = neu.surfaceInset,
                unfocusedContainerColor = neu.surfaceInset,
                focusedBorderColor = neu.primaryAccent,
                unfocusedBorderColor = neu.hairlineDivider
              )
            )

            Spacer(modifier = Modifier.width(8.dp))

            val canSend = inputPrompt.isNotBlank() && !chatState.isGenerating
            IconButton(
              onClick = {
                val toSend = inputPrompt
                if (canSend) {
                  inputPrompt = ""
                  chatViewModel.sendMessage(toSend)
                }
              },
              enabled = canSend,
              modifier = Modifier
                .size(48.dp)
                .neumorphicExtruded(
                  shape = CircleShape,
                  elevation = if (canSend) 3.5.dp else 1.5.dp,
                  blurRadius = 7.dp,
                  surfaceColor = if (canSend) MaterialTheme.colorScheme.primary else neu.surfaceRaised,
                  borderAlpha = 0.40f,
                  neu = neu
                )
                .testTag("send_chat_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Message",
                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else neu.textSecondary
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

        val neu = LocalNeumorphicColors.current
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .neumorphicExtruded(
                  shape = IosSharpCardShape,
                  elevation = 4.5.dp,
                  blurRadius = 9.dp,
                  accentTint = neu.primaryAccent,
                  borderAlpha = 0.30f,
                  neu = neu
                )
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "GEMINI AI MODEL TIER SELECTOR",
                  style = MaterialTheme.typography.labelMedium,
                  color = neu.primaryAccent,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = chatState.selectedTier.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = neu.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .neumorphicInset(
                      shape = IosSharpWellShape,
                      depth = 2.5.dp,
                      blurRadius = 5.dp,
                      neu = neu
                    )
                    .padding(5.dp),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  GeminiModelTier.entries.forEach { tier ->
                    val selected = chatState.selectedTier == tier
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                          if (selected) {
                            Modifier.neumorphicExtruded(
                              shape = IosSharpButtonShape,
                              elevation = 3.dp,
                              blurRadius = 6.dp,
                              surfaceColor = MaterialTheme.colorScheme.primary,
                              borderAlpha = 0.45f,
                              neu = neu
                            )
                          } else {
                            Modifier.clip(IosSharpButtonShape)
                          }
                        )
                        .clickable { chatViewModel.selectModelTier(tier) }
                        .padding(vertical = 9.dp, horizontal = 4.dp)
                        .testTag("model_tier_${tier.name.lowercase()}"),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = tier.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else neu.textPrimary,
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
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .neumorphicExtruded(
                    shape = IosSharpCardShape,
                    elevation = 4.5.dp,
                    blurRadius = 9.dp,
                    borderAlpha = 0.25f,
                    neu = neu
                  )
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text(
                    text = groupTitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = neu.primaryAccent,
                    fontWeight = FontWeight.Bold
                  )
                  prompts.forEachIndexed { idx, prompt ->
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicInset(
                          shape = IosSharpWellShape,
                          depth = 2.dp,
                          blurRadius = 4.dp,
                          neu = neu
                        )
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
                          tint = neu.primaryAccent,
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                          text = prompt,
                          style = MaterialTheme.typography.bodySmall,
                          color = neu.textPrimary,
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
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = 4.5.dp,
        blurRadius = 9.dp,
        accentTint = neu.primaryAccent,
        borderAlpha = 0.28f,
        neu = neu
      )
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
          tint = neu.primaryAccent,
          modifier = Modifier.size(15.dp)
        )
        Text(
          text = badge,
          style = MaterialTheme.typography.labelSmall,
          color = neu.primaryAccent,
          fontWeight = FontWeight.Bold
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = neu.textPrimary
      )
      bullets.forEach { bullet ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Top
        ) {
          Text(
            text = "• ",
            color = neu.primaryAccent,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = bullet,
            style = MaterialTheme.typography.bodySmall,
            color = neu.textSecondary,
            lineHeight = 18.sp
          )
        }
      }
    }
  }
}

@Composable
private fun ChatBubbleRow(message: ChatMessageItem) {
  val neu = LocalNeumorphicColors.current
  val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
  val bubbleShape = if (message.isUser) {
    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
  } else {
    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp)
  }

  val containerColor = when {
    message.isUser -> MaterialTheme.colorScheme.primary
    message.isError -> MaterialTheme.colorScheme.errorContainer
    else -> neu.surfaceRaised
  }

  val contentColor = when {
    message.isUser -> MaterialTheme.colorScheme.onPrimary
    message.isError -> MaterialTheme.colorScheme.onErrorContainer
    else -> neu.textPrimary
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp),
    contentAlignment = alignment
  ) {
    Box(
      modifier = Modifier
        .widthIn(max = 330.dp)
        .neumorphicExtruded(
          shape = bubbleShape,
          elevation = 3.5.dp,
          blurRadius = 7.dp,
          surfaceColor = containerColor,
          accentTint = if (message.isUser) null else neu.primaryAccent,
          borderAlpha = if (message.isUser) 0.45f else 0.25f,
          neu = neu
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
                  message.isUser -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                  message.modelLabel.contains("Argon") -> BadgeColCyan
                  message.modelLabel.contains("Lite") -> BadgeElNeutral
                  else -> neu.primaryAccent
                }
              )
          )
          Text(
            text = message.modelLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (message.isUser) {
              MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            } else {
              neu.primaryAccent
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
