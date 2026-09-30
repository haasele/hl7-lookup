// Locks window launch flags so the native app does not bind the API port. Calls window/Logic parseArgs.
package hl7lookup.desktop.window

import hl7lookup.engine.Engines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Checks that a plain launch stays off the loopback API and --server is the browser engine. Calls parseArgs.
class LaunchTest {
    // Asserts the packaged window starts with no listener. Calls parseArgs with an empty argv.
    @Test
    fun windowLaunchDoesNotBindTheApi() {
        val options = parseArgs(emptyArray())
        assertFalse(options.headless)
        assertFalse(options.server)
    }

    // Asserts --server is the headless browser engine on the default port. Calls parseArgs.
    @Test
    fun serverFlagStartsTheHeadlessEngine() {
        val options = parseArgs(arrayOf("--server", "--port", "7790"))
        assertTrue(options.headless)
        assertTrue(options.server)
        assertEquals(7790, options.port)
    }

    // Asserts an omitted port stays on the engine default. Calls parseArgs and Engines.defaultPort.
    @Test
    fun headlessEngineUsesTheDefaultPort() {
        val options = parseArgs(arrayOf("--headless"))
        assertTrue(options.headless)
        assertTrue(options.server)
        assertEquals(Engines.defaultPort(), options.port)
    }
}
