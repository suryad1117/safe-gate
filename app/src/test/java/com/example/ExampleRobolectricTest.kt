package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AccentColor
import com.example.data.AppEntity
import com.example.data.AppTheme
import com.example.data.LockScreenStyle
import com.example.data.SafeGateDatabase
import com.example.data.SettingsRepository
import com.example.lock.LockStateManager
import com.example.security.PinManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: SafeGateDatabase
    private lateinit var pinManager: PinManager
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = SafeGateDatabase.getDatabase(context)
        pinManager = PinManager(context)
        settingsRepository = SettingsRepository(context)
    }

    @Test
    fun `read app name string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("SafeGate", appName)
    }

    @Test
    fun `settings repository saves and restores appearance and setup status`() = runBlocking {
        settingsRepository.setSetupCompleted(false)
        var settings = settingsRepository.settingsFlow.first()
        assertFalse(settings.setupCompleted)

        settingsRepository.saveAppearance(
            theme = AppTheme.DARK,
            accent = AccentColor.GREEN,
            lockScreen = LockScreenStyle.SECURE,
            iconStyle = com.example.data.AppIconStyle.SHIELD
        )

        settings = settingsRepository.settingsFlow.first()
        assertEquals(AppTheme.DARK, settings.theme)
        assertEquals(AccentColor.GREEN, settings.accentColor)
        assertEquals(LockScreenStyle.SECURE, settings.lockScreenStyle)
        assertEquals(com.example.data.AppIconStyle.SHIELD, settings.appIconStyle)

        settingsRepository.setSetupCompleted(true)
        settings = settingsRepository.settingsFlow.first()
        assertTrue(settings.setupCompleted)
    }

    @Test
    fun `pin manager saves and verifies custom pin`() {
        // Save PIN
        assertTrue(pinManager.savePin("6543"))
        assertTrue(pinManager.hasPin())
        assertTrue(pinManager.verifyPin("6543"))
        assertFalse(pinManager.verifyPin("1234"))
    }

    @Test
    fun `pin manager enforces 3 attempts per hour rate limiting`() {
        val testPin = "1122"
        val wrongPin = "9999"
        assertTrue(pinManager.savePin(testPin))
        assertEquals(0, pinManager.getAttemptsUsed())
        assertFalse(pinManager.isPinBlocked())

        // Attempt 1: Wrong
        val t0 = 1_000_000_000L
        pinManager.recordFailedAttempt(t0)
        assertEquals(1, pinManager.getAttemptsUsed(t0))
        assertFalse(pinManager.isPinBlocked(t0))
        assertEquals(2, pinManager.getRemainingAttempts(t0))

        // Attempt 2: Wrong
        val t1 = t0 + 60_000L // 1 min later
        pinManager.recordFailedAttempt(t1)
        assertEquals(2, pinManager.getAttemptsUsed(t1))
        assertFalse(pinManager.isPinBlocked(t1))
        assertEquals(1, pinManager.getRemainingAttempts(t1))

        // Attempt 3: Wrong -> Blocked!
        val t2 = t1 + 60_000L // 2 min later
        pinManager.recordFailedAttempt(t2)
        assertEquals(3, pinManager.getAttemptsUsed(t2))
        assertTrue(pinManager.isPinBlocked(t2))
        assertEquals(0, pinManager.getRemainingAttempts(t2))
        assertTrue(pinManager.getRemainingBlockTimeMillis(t2) > 0)

        // Cannot verify even with correct PIN while blocked
        assertFalse(pinManager.verifyPin(testPin, currentTime = t2))

        // After 1 hour passes (e.g. t0 + 3600_001 ms)
        val tAfter1Hour = t0 + PinManager.WINDOW_DURATION_MS + 1000L
        assertFalse(pinManager.isPinBlocked(tAfter1Hour))
        assertEquals(0, pinManager.getAttemptsUsed(tAfter1Hour))
        assertEquals(3, pinManager.getRemainingAttempts(tAfter1Hour))

        // Successful PIN resets counter
        pinManager.resetPinAttempts()
        pinManager.recordFailedAttempt(tAfter1Hour)
        assertEquals(1, pinManager.getAttemptsUsed(tAfter1Hour))
        assertTrue(pinManager.verifyPin(testPin, currentTime = tAfter1Hour))
        assertEquals(0, pinManager.getAttemptsUsed(tAfter1Hour))
    }

    @Test
    fun `temporary unlock token grants and clears correctly`() {
        val testPkg = "com.test.targetapp"
        assertFalse(LockStateManager.isTemporarilyUnlocked(testPkg))

        LockStateManager.grantTemporaryUnlock(testPkg, 5000L)
        assertTrue(LockStateManager.isTemporarilyUnlocked(testPkg))

        LockStateManager.clearTemporaryUnlock(testPkg)
        assertFalse(LockStateManager.isTemporarilyUnlocked(testPkg))
    }

    @Test
    fun `lock state manager respects self package and unregistered apps`() = runBlocking {
        val lockStateManager = LockStateManager.getInstance(context)

        // SafeGate should never be locked
        assertFalse(lockStateManager.shouldLock(context.packageName))

        // Unregistered apps should not be locked
        assertFalse(lockStateManager.shouldLock("com.unknown.randomapp"))
    }

    @Test
    fun `lock state manager locks protected app`() = runBlocking {
        val lockStateManager = LockStateManager.getInstance(context)
        val testPkg = "com.social.media"

        database.appDao().insertApp(
            AppEntity(
                packageName = testPkg,
                appName = "Social App",
                isProtected = true
            )
        )

        assertTrue(lockStateManager.shouldLock(testPkg))

        // Temporarily unlock
        LockStateManager.grantTemporaryUnlock(testPkg, 10_000L)
        assertFalse(lockStateManager.shouldLock(testPkg))

        LockStateManager.clearTemporaryUnlock(testPkg)
        assertTrue(lockStateManager.shouldLock(testPkg))
    }

    @Test
    fun `prevent uninstall preference toggles correctly`() {
        assertFalse(pinManager.isPreventUninstallEnabled())
        pinManager.setPreventUninstallEnabled(true)
        assertTrue(pinManager.isPreventUninstallEnabled())
        pinManager.setPreventUninstallEnabled(false)
        assertFalse(pinManager.isPreventUninstallEnabled())
    }

    @Test
    fun `device admin helper provides valid component and intents`() {
        val component = com.example.security.DeviceAdminHelper.getDeviceAdminComponentName(context)
        assertEquals(context.packageName, component.packageName)
        assertTrue(component.className.contains("SafeGateDeviceAdminReceiver"))

        val enableIntent = com.example.security.DeviceAdminHelper.getEnableDeviceAdminIntent(context)
        assertEquals(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN, enableIntent.action)

        val uninstallIntent = com.example.security.DeviceAdminHelper.getUninstallIntent(context)
        assertEquals(android.content.Intent.ACTION_DELETE, uninstallIntent.action)
        assertEquals("package:${context.packageName}", uninstallIntent.dataString)
    }

    @Test
    fun `strict inactivity lock strictly enforces lock without bypass and unlocks at end time`() = runBlocking {
        val lockStateManager = LockStateManager.getInstance(context)
        val testPkg = "com.instagram.android"
        val start = 1_000_000L
        val end = 2_000_000L

        val schedule = com.example.data.ScheduleEntity(
            id = 101L,
            packageName = testPkg,
            appName = "Instagram",
            startDateTime = start,
            endDateTime = end,
            repeatType = "Once",
            customDays = "",
            enabled = true,
            inactivityEnabled = true,
            inactivityMinutes = 0
        )
        database.scheduleDao().insertSchedule(schedule)
        database.scheduleDao().insertScheduleApps(
            listOf(
                com.example.data.ScheduleAppEntity(
                    scheduleId = 101L,
                    packageName = testPkg,
                    appName = "Instagram"
                )
            )
        )

        // 1. During active window:
        val during = 1_500_000L
        assertTrue(com.example.scheduling.ScheduleCalculator.isScheduleActiveNow(schedule, during))
        assertTrue(com.example.scheduling.ScheduleCalculator.isStrictInactivityActive(schedule, during))

        // shouldLock is true
        assertTrue(lockStateManager.shouldLock(testPkg, during))

        // Session authorization or temporary unlock MUST NOT bypass the lock
        LockStateManager.grantTemporaryUnlock(testPkg, 60_000L)
        com.example.lock.UnlockSessionManager(context).authorize(testPkg)
        assertTrue(lockStateManager.shouldLock(testPkg, during))

        // 2. After scheduled end time:
        val after = 2_000_001L
        assertFalse(com.example.scheduling.ScheduleCalculator.isScheduleActiveNow(schedule, after))
        assertFalse(com.example.scheduling.ScheduleCalculator.isStrictInactivityActive(schedule, after))
        // Automatically unlocked
        assertFalse(lockStateManager.shouldLock(testPkg, after))
    }

    @Test
    fun `upi and unselected apps are never locked and have no barrier`() = runBlocking {
        val lockStateManager = LockStateManager.getInstance(context)
        val gpay = "com.google.android.apps.nbu.paisa.user"
        val phonePe = "com.phonepe.app"
        val paytm = "net.one97.paytm"

        // Unselected UPI apps must never be locked
        assertFalse(lockStateManager.shouldLock(gpay))
        assertFalse(lockStateManager.shouldLock(phonePe))
        assertFalse(lockStateManager.shouldLock(paytm))
    }
}
