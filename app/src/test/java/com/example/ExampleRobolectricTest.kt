package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric smoke test that reads a string resource from the Android context.
 * Requires the Robolectric instrumented-SDK jar, which must be downloaded from Maven
 * on first run. CI environments with blocked Maven access should set
 * ROBOLECTRIC_DEPS_DIR to a pre-populated directory, or exclude this test class.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AeroFast Pro", appName)
  }
}
