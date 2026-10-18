package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricHelper(
    private val activity: FragmentActivity
) {

    fun isAvailable(): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    companion object {
        fun isBiometricSupported(context: Context): Boolean {
            val manager = BiometricManager.from(context)
            val result = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            return result == BiometricManager.BIOMETRIC_SUCCESS || result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        }

        fun isFingerprintSupported(context: Context): Boolean {
            val pm = context.packageManager
            val hasFeature = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
            val bioSupported = isBiometricSupported(context)
            // On Android devices, if fingerprint hardware is present or generic biometric exists
            return hasFeature || bioSupported
        }

        fun isFaceSupported(context: Context): Boolean {
            val pm = context.packageManager
            val hasFeature = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FACE)
            return hasFeature
        }
    }

    fun authenticate(
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    onFailure()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailure()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Safe Gate")
            .setSubtitle("Authenticate to open the protected app")
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(promptInfo)
    }

    fun authenticateCustom(
        title: String = "Unlock Safe Gate",
        subtitle: String = "Authenticate to continue",
        negativeButtonText: String = "Use PIN",
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    onFailure()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailure()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()

        prompt.authenticate(promptInfo)
    }
}
