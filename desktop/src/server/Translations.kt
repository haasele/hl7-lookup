// Startup lines printed by the window. server/Index reads them.
package hl7lookup.desktop.server

import hl7lookup.i18n.Text

// Holds console messages for server start, missing bundle and headless mode. Servers.texts exposes them.
object ServerTexts {
    val started = Text("HL7 Lookup server on {0}", "HL7-Lookup-Server auf {0}")
    val noBundle = Text("Web client bundle not found. Build it with: ./kotlin build -m web", "Web-Client nicht gefunden. Bauen mit: ./kotlin build -m web")
    val failed = Text("Server could not start on port {0}: {1}", "Server konnte auf Port {0} nicht starten: {1}")
    val headless = Text("Running without window. Press Ctrl+C to stop.", "Läuft ohne Fenster. Mit Strg+C beenden.")
}
