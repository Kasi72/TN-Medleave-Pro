package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.domain.BiometricLockManager
import com.example.domain.HardwareBiometricStatus
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {}
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context as Activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

fun signOutUser(
  context: Context,
  credentialManager: CredentialManager,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope
) {
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e("Auth", "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  onBiometricSignInSuccess: () -> Unit = onAuthSuccess
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var isGoogleLoading by remember { mutableStateOf(false) }
  var isBiometricScanning by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }
  var statusBanner by remember { mutableStateOf<String?>(null) }
  var showPinInput by remember { mutableStateOf(false) }
  var pinInputText by remember { mutableStateOf("") }

  val hardwareStatus = remember {
    BiometricLockManager.checkHardwareBiometricStatus(context)
  }
  val configuredPin = remember {
    BiometricLockManager.getFallbackPin(context)
  }

  val pulseTransition = rememberInfiniteTransition(label = "front_biometric_pulse")
  val pulseScale by pulseTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.07f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "front_pulse_scale"
  )

  fun triggerFrontPageBiometricSignIn() {
    errorMsg = null
    isBiometricScanning = true
    statusBanner = "Verifying HOD Biometric Identity..."

    BiometricLockManager.authenticateWithSystemBiometrics(
      context = context,
      onSuccess = {
        // Attempt silent Google credential attach if available, otherwise proceed with Biometric HOD session
        attemptAutoSignIn(
          context = context,
          credentialManager = credentialManager,
          onAuthSuccess = {
            isBiometricScanning = false
            onBiometricSignInSuccess()
          },
          onUnauthenticated = {
            isBiometricScanning = false
            onBiometricSignInSuccess()
          },
          scope = scope
        )
      },
      onError = { err ->
        isBiometricScanning = false
        errorMsg = err
        statusBanner = null
      },
      onHardwareUnavailableOrUnenrolled = {
        scope.launch {
          delay(400L)
          attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isBiometricScanning = false
              onBiometricSignInSuccess()
            },
            onUnauthenticated = {
              isBiometricScanning = false
              onBiometricSignInSuccess()
            },
            scope = scope
          )
        }
      }
    )
  }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF040D1A),
            Color(0xFF081C33),
            Color(0xFF062C30)
          )
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 18.dp),
    contentAlignment = Alignment.Center
  ) {
    EnterpriseMeshHeroContainer(
      cornerRadius = 32.dp,
      primaryGlowColor = Color(0xFF10B981),
      secondaryGlowColor = Color(0xFF38BDF8),
      contentPadding = PaddingValues(24.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Institutional Security Pill
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF10B981).copy(alpha = 0.16f),
          border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.45f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color(0xFF34D399))
            )
            Text(
              text = "TN MES • EXECUTIVE BIOMETRIC PORTAL",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              lineHeight = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF6EE7B7),
              maxLines = 1,
              softWrap = false
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Interactive Pulsing Biometric Crest Orb (Tappable directly on the Front Page)
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(104.dp)
            .scale(if (isBiometricScanning) 1.12f else pulseScale)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                colors = listOf(
                  Color(0xFF10B981).copy(alpha = 0.34f),
                  Color(0xFF0EA5E9).copy(alpha = 0.10f),
                  Color.Transparent
                )
              )
            )
            .border(
              width = 1.8.dp,
              brush = Brush.linearGradient(
                colors = listOf(Color(0xFF34D399), Color(0xFF38BDF8))
              ),
              shape = CircleShape
            )
            .clickable(enabled = !isBiometricScanning && !isGoogleLoading) {
              triggerFrontPageBiometricSignIn()
            }
            .testTag("front_page_biometric_scanner_orb")
        ) {
          if (isBiometricScanning) {
            CircularProgressIndicator(
              modifier = Modifier.size(44.dp),
              color = Color(0xFF34D399),
              strokeWidth = 3.dp
            )
          } else {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = "Tap to Sign in with Biometrics",
              tint = Color(0xFF34D399),
              modifier = Modifier.size(54.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "TN MedLeave Pro",
          style = MaterialTheme.typography.headlineLarge,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "HOD, Dept. of Psychiatry • drkasi@gmail.com\nGovt. Erode Medical College & Hospital",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFBAE6FD),
          textAlign = TextAlign.Center,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1. PRIMARY CTA: SIGN IN WITH BIOMETRICS (Front Page)
        Button(
          onClick = { triggerFrontPageBiometricSignIn() },
          enabled = !isBiometricScanning && !isGoogleLoading,
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF10B981),
            contentColor = Color(0xFF042F2E)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("biometric_sign_in_button")
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(horizontalAlignment = Alignment.Start) {
            Text(
              text = "Sign in with Biometrics",
              fontSize = 16.sp,
              lineHeight = 20.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
            Text(
              text = if (hardwareStatus == HardwareBiometricStatus.READY) {
                "Fingerprint / Face ID Hardware Ready"
              } else {
                "Instant Touch Sensor & HOD Vault Access"
              },
              fontSize = 11.sp,
              lineHeight = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF042F2E).copy(alpha = 0.80f),
              maxLines = 1,
              softWrap = false
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick 4-Digit Executive PIN Sign-In Toggle on the Front Page
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = { showPinInput = !showPinInput },
            modifier = Modifier.testTag("toggle_front_page_pin_button")
          ) {
            Icon(
              imageVector = Icons.Default.Dialpad,
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (showPinInput) {
                "Hide 4-Digit Executive PIN"
              } else {
                "Or Sign in with 4-Digit Executive PIN ($configuredPin)"
              },
              color = Color(0xFF38BDF8),
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        AnimatedVisibility(visible = showPinInput) {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.07f),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = pinInputText,
                onValueChange = { raw ->
                  val digits = raw.filter { it.isDigit() }.take(4)
                  pinInputText = digits
                  if (digits.length == 4) {
                    if (BiometricLockManager.verifyPin(context, digits)) {
                      errorMsg = null
                      onBiometricSignInSuccess()
                    } else {
                      errorMsg = "Incorrect 4-digit Executive PIN (Default: $configuredPin)"
                    }
                  }
                },
                label = { Text("4-Digit PIN (e.g. $configuredPin)", color = Color(0xFFBAE6FD)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White,
                  focusedBorderColor = Color(0xFF34D399),
                  unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("front_page_pin_input")
              )

              Button(
                onClick = {
                  if (BiometricLockManager.verifyPin(context, pinInputText)) {
                    errorMsg = null
                    onBiometricSignInSuccess()
                  } else {
                    errorMsg = "Incorrect 4-digit Executive PIN (Default: $configuredPin)"
                  }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF38BDF8),
                  contentColor = Color(0xFF042F2E)
                ),
                modifier = Modifier.height(52.dp)
              ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Unlock", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        if (statusBanner != null) {
          Surface(
            color = Color(0xFF064E3B),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
          ) {
            Text(
              text = statusBanner ?: "",
              color = Color(0xFFA7F3D0),
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(12.dp),
              textAlign = TextAlign.Center
            )
          }
        }

        if (errorMsg != null) {
          Surface(
            color = Color(0xFF7F1D1D),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
          ) {
            Text(
              text = errorMsg ?: "",
              color = Color(0xFFFECACA),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp),
              textAlign = TextAlign.Center
            )
          }
        }

        // Divider between Biometric Sign-In and Google Sign-In
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.14f)
          )
          Text(
            text = "  CLOUD ACCOUNT SYNC  ",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = Color(0xFF94A3B8)
          )
          HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.14f)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. SECONDARY CTA: SIGN IN WITH GOOGLE
        OutlinedButton(
          onClick = {
            isGoogleLoading = true
            errorMsg = null
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isGoogleLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isGoogleLoading = false
                errorMsg = msg
              },
              scope = scope,
              onAuthCancelled = {
                isGoogleLoading = false
              }
            )
          },
          enabled = !isGoogleLoading && !isBiometricScanning,
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.55f)),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button")
        ) {
          if (isGoogleLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = Color(0xFF38BDF8),
              strokeWidth = 2.5.dp
            )
          } else {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Sign in with Google",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Enterprise Feature Highlights
        FeatureBulletRow(
          icon = Icons.Default.CalendarMonth,
          title = "2023–2026 Leave Ledger Pre-Loaded",
          subtitle = "198 historical CL, COL, In-Lieu, PL, GH Worked, ML, RH, EL & OD records"
        )
        Spacer(modifier = Modifier.height(8.dp))
        FeatureBulletRow(
          icon = Icons.Default.AutoAwesome,
          title = "AI Smart Bridge & Room Holiday Sync",
          subtitle = "Auto-syncs 2026 TN Govt Holidays (23 GH) + 31 Restricted Holidays (RL)"
        )
        Spacer(modifier = Modifier.height(8.dp))
        FeatureBulletRow(
          icon = Icons.Default.AccountBalance,
          title = "TN Leave Rules 1933 & Official A4 PDF Audit",
          subtitle = "12 CL quota, 3 RH quota, 10-day stretch validator & COL ↔ GH pairing"
        )
      }
    }
  }
}

@Composable
private fun FeatureBulletRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White.copy(alpha = 0.06f))
      .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color(0xFF38BDF8).copy(alpha = 0.18f),
      modifier = Modifier.size(40.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = Color(0xFF38BDF8),
          modifier = Modifier.size(20.dp)
        )
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF94A3B8),
        fontSize = 11.sp,
        lineHeight = 15.sp
      )
    }
  }
}
