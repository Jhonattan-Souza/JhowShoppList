package com.jhow.shopplist.data.sync

import android.Manifest
import android.content.pm.PackageManager
import android.security.NetworkSecurityPolicy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugNetworkPolicyTest {

    @Test
    fun debugAppCanReachLocalCalDavOverHttp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(Manifest.permission.INTERNET)
        )
        assertTrue(
            "Debug app must allow cleartext HTTP for 10.0.2.2 local DAV testing",
            NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("10.0.2.2")
        )
    }
}
