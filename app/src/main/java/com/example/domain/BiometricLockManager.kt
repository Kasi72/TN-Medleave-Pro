package com.example.domain

import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

enum class HardwareBiometricStatus(val statusLabel: String) {
  READY("Hardware Biometrics Enrolled & Ready"),
  NONE_ENROLLED("Biometric Sensor Present (No OS Fingerprint Enrolled • PIN/Touch Vault Active)"),
  NO_HARDWARE("Virtual Device / No Hardware Sensor (PIN & Touch Vault Active)")
}

object BiometricLockManager {

  private const val PREFS_NAME = "tn_medleave_biometric_prefs"
  private const val KEY_LOCK_ENABLED = "biometric_lock_enabled"
  private const val KEY_LOCK_ON_BACKGROUND = "auto_lock_on_background"
  private const val KEY_FALLBACK_PIN = "executive_fallback_pin"

  fun isBiometricLockEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // Enabled by default since the user requested biometric lock for the app
    return prefs.getBoolean(KEY_LOCK_ENABLED, true)
  }

  fun setBiometricLockEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_LOCK_ENABLED, enabled)
      .apply()
  }

  fun isAutoLockOnBackgroundEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_LOCK_ON_BACKGROUND, true)
  }

  fun setAutoLockOnBackgroundEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_LOCK_ON_BACKGROUND, enabled)
      .apply()
  }

  fun getFallbackPin(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_FALLBACK_PIN, "2026") ?: "2026"
  }

  fun setFallbackPin(context: Context, newPin: String) {
    if (newPin.length == 4 && newPin.all { it.isDigit() }) {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_FALLBACK_PIN, newPin)
        .apply()
    }
  }

  fun verifyPin(context: Context, enteredPin: String): Boolean {
    return enteredPin == getFallbackPin(context)
  }

  fun checkHardwareBiometricStatus(context: Context): HardwareBiometricStatus {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val bm = context.getSystemService(BiometricManager::class.java)
      if (bm != null) {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
              BiometricManager.Authenticators.BIOMETRIC_WEAK
          )
        } else {
          @Suppress("DEPRECATION")
          bm.canAuthenticate()
        }
        return when (result) {
          BiometricManager.BIOMETRIC_SUCCESS -> HardwareBiometricStatus.READY
          BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> HardwareBiometricStatus.NONE_ENROLLED
          else -> HardwareBiometricStatus.NO_HARDWARE
        }
      }
    }
    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return if (km?.isKeyguardSecure == true) {
      HardwareBiometricStatus.READY
    } else {
      HardwareBiometricStatus.NO_HARDWARE
    }
  }

  /**
   * Launches the native Android OS `BiometricPrompt` when running on API 28+ with enrolled biometrics.
   * If the device/emulator does not have OS biometrics enrolled, invokes `onHardwareUnavailableOrUnenrolled`
   * so the UI can seamlessly complete authentication via the on-screen Biometric Touch Scanner or 4-digit PIN.
   */
  fun authenticateWithSystemBiometrics(
    context: Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onHardwareUnavailableOrUnenrolled: () -> Unit
  ) {
    val hwStatus = checkHardwareBiometricStatus(context)
    if (hwStatus != HardwareBiometricStatus.READY || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      onHardwareUnavailableOrUnenrolled()
      return
    }

    try {
      val executor = ContextCompat.getMainExecutor(context)
      val cancellationSignal = CancellationSignal()

      val builder = BiometricPrompt.Builder(context)
        .setTitle("TN MedLeave Pro • HOD Biometric Lock")
        .setSubtitle("Department of Psychiatry • Govt. Erode Medical College")
        .setDescription("Authenticate with fingerprint or face biometrics to access your official leave ledger & audit records.")

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setAllowedAuthenticators(
          BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
      } else {
        builder.setNegativeButton(
          "Use Executive PIN",
          executor,
          DialogInterface.OnClickListener { _, _ ->
            onError("Switched to Executive 4-Digit PIN entry.")
          }
        )
      }

      val prompt = builder.build()
      prompt.authenticate(
        cancellationSignal,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS ||
              errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT ||
              errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE
            ) {
              onHardwareUnavailableOrUnenrolled()
            } else {
              onError(errString?.toString() ?: "Biometric authentication cancelled.")
            }
          }

          override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            onError("Biometric not recognized. Try again or enter your 4-digit PIN.")
          }
        }
      )
    } catch (e: Exception) {
      onHardwareUnavailableOrUnenrolled()
    }
  }
}
