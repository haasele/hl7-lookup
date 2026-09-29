package hl7lookup.license

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hl7lookup.controls.Label
import hl7lookup.controls.Modal
import hl7lookup.i18n.tr
import hl7lookup.platform.LocalPlatform
import hl7lookup.platform.PlatformKind
import hl7lookup.theme.LocalPalette

object Licenses {
    fun components(desktop: Boolean): List<Component> = componentsFor(thirdParty, desktop)
    fun texts() = LicenseTexts
}

@Composable
fun LicenseDialog(appVersion: String, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val desktop = LocalPlatform.current.kind == PlatformKind.DESKTOP
    Modal(tr(LicenseTexts.title), onDismiss, width = 620.dp) {
        Label(tr(LicenseTexts.app), weight = FontWeight.Bold, size = 16.sp)
        Label(tr(LicenseTexts.version, appVersion), color = palette.textDim, size = 12.sp)
        Label(tr(LicenseTexts.appNotice), size = 13.sp)
        Label(tr(LicenseTexts.hl7Notice), color = palette.textDim, size = 12.sp)
        Label(tr(LicenseTexts.components), weight = FontWeight.SemiBold)
        for (component in Licenses.components(desktop)) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Label(listOf(component.name, component.version).filter { it.isNotBlank() }.joinToString(" "), weight = FontWeight.SemiBold, size = 13.sp)
                Label(component.license, size = 12.sp)
                Label(component.url, color = palette.link, size = 11.sp, mono = true)
                component.note?.let { Label(tr(it), color = palette.textDim, size = 11.sp) }
            }
        }
    }
}
