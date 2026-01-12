import sys
import dark
import socket
from pathlib import Path
from PySide6.QtGui import QIcon, QFontDatabase, QFont
from PySide6.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QDialogButtonBox,
    QSplitter, QTextEdit, QTextBrowser, QTreeWidget,
    QTreeWidgetItem, QTabWidget, QToolBar, QLineEdit,
    QInputDialog, QMessageBox, QLabel, QPushButton,
    QHBoxLayout, QFrame, QTabBar, QPushButton, QDialog, QFormLayout
)
from PySide6.QtGui import QColor, QAction, QTextCursor, QTextCharFormat, QMouseEvent
from PySide6.QtCore import Qt, QEvent, QFile, QTextStream
from hl7apy.parser import parse_message
from hl7apy.parser import parse_segment
from hl7apy.exceptions import HL7apyException
from hl7apy.consts import VALIDATION_LEVEL


SEGMENT_COLORS = {
    "MSH": "#f05d73",
    "EVN": "#e1b3f2",
    "PID": "#88eee2",
    "PV1": "#88a1ee",
    "PV2": "#f1fa8c",
    "GT1": "#feb4df",
    "NK1": "#fee0b4",
    "IN1": "#e3ff9a",
    "IN2": "#9affb2",
    "DG1": "#ff9a9a",
    "AL1": "#82da9b",
    "PR1": "#bca3ca",
}

FRIENDLY_FIELD_NAMES = {
    # MSH – Message Header (aus HL7‑Soup & Smile CDR)
    "MSH_1": "MSH‑1  –  Field Separator",
    "MSH_2": "MSH‑2  –  Encoding Characters",
    "MSH_2.1": "    MSH‑2.1  –  Component-Separator",
    "MSH_2.2": "    MSH‑2.2  –  Repetition Separator",
    "MSH_2.3": "    MSH‑2.3  –  Escape Character '\\'",
    "MSH_2.4": "    MSH‑2.4  –  Subcomponent Separator '&'",
    "MSH_3": "MSH‑3  –  Sending Application",
    "MSH_3.2": "    MSH‑3.2  –  Sending Application (Universal ID)",
    "MSH_3.3": "    MSH‑3.3  –  Sending Application (Universal ID Type)",
    "MSH_4": "MSH‑4  –  Sending Facility",
    "MSH_4.2": "    MSH‑4.2  –  Sending Facility (Universal ID)",
    "MSH_4.3": "    MSH‑4.3  –  Sending Facility (Universal ID Type)",
    "MSH_5": "MSH‑5  –  Receiving Application",
    "MSH_5.2": "    MSH‑5.2  –  Receiving Application (Universal ID)",
    "MSH_5.3": "    MSH‑5.3  –  Receiving Application (Universal ID Type)",
    "MSH_6": "MSH‑6  –  Receiving Facility",
    "MSH_6.2": "    MSH‑6.2  –  Receiving Facility (Universal ID)",
    "MSH_6.3": "    MSH‑6.3  –  Receiving Facility (Universal ID Type)",
    "MSH_7": "MSH‑7  –  Date/Time Of Message",
    "MSH_8": "MSH‑8  –  Security",
    "MSH_9": "MSH‑9  –  Message Type",
    "MSH_9.1": "    MSH‑9.1  –  Message Type",
    "MSH_9.2": "    MSH‑9.2  –  Trigger Event",
    "MSH_9.3": "    MSH‑9.3  –  Message Structure",
    "MSH_10": "MSH‑10  –  Message Control ID",
    "MSH_11": "MSH‑11  –  Processing ID",
    "MSH_12": "MSH‑12  –  Version ID",
    "MSH_13": "MSH‑13  –  Sequence Number",
    "MSH_14": "MSH‑14  –  Continuation Pointer",
    "MSH_15": "MSH‑15  –  Accept Acknowledgment Type",
    "MSH_16": "MSH‑16  –  Application Acknowledgment Type",
    "MSH_17": "MSH‑17  –  Country Code",
    "MSH_18": "MSH‑18  –  Character Set",
    "MSH_19": "MSH‑19  –  Principal Language",

    # PV1 – Patient Visit (aus Rhapsody: alle Felder bis 52)
    "PV1_1": "PV1‑1 – Set ID – PV1",
    "PV1_2": "PV1‑2 – Patient Class",
    "PV1_3": "PV1‑3 – Assigned Patient Location",
    "PV1_3.1": "    PV1‑3.1 – Point Of Care",
    "PV1_3.2": "    PV1‑3.2 – Room",
    "PV1_3.3": "    PV1‑3.3 – Bed",
    "PV1_3.4": "    PV1‑3.4 – Facility",
    "PV1_4": "PV1‑4 – Admission Type",
    "PV1_5": "PV1‑5 – Preadmit Number",
    "PV1_6": "PV1‑6 – Prior Patient Location",
    "PV1_7": "PV1‑7 – Attending Doctor",
    "PV1_7.1": "    PV1‑7.1 – Attending Physician Code",
    "PV1_7.2": "    PV1‑7.2 – Attending Last Name",
    "PV1_7.3": "    PV1‑7.3 – Attending First Name",
    "PV1_7.4": "    PV1‑7.4 – Attending Doctor",
    "PV1_7.5": "    PV1‑7.5 – Attending Suffix",
    "PV1_7.6": "    PV1‑7.6 – Attending Doctor",
    "PV1_7.7": "    PV1‑7.7 – Attending Credentials",
    "PV1_8": "PV1‑8 – Referring Doctor",
    "PV1_9": "PV1‑9 – Consulting Doctor",
    "PV1_10": "PV1‑10 – Hospital Service",
    "PV1_11": "PV1‑11 – Temporary Location",
    "PV1_12": "PV1‑12 – Preadmit Test Indicator",
    "PV1_13": "PV1‑13 – Re‑admission Indicator",
    "PV1_14": "PV1‑14 – Admit Source",
    "PV1_15": "PV1‑15 – Ambulatory Status",
    "PV1_16": "PV1‑16 – VIP Indicator",
    "PV1_17": "PV1‑17 – Admitting Doctor",
    "PV1_18": "PV1‑18 – Patient Type",
    "PV1_19": "PV1‑19 – Visit Number",
    "PV1_20": "PV1‑20 – Financial Class",
    "PV1_21": "PV1‑21 – Charge Price Indicator",
    "PV1_22": "PV1‑22 – Courtesy Code",
    "PV1_23": "PV1‑23 – Credit Rating",
    "PV1_24": "PV1‑24 – Contract Code",
    "PV1_25": "PV1‑25 – Contract Effective Date",
    "PV1_26": "PV1‑26 – Contract Amount",
    "PV1_27": "PV1‑27 – Contract Period",
    "PV1_28": "PV1‑28 – Interest Code",
    "PV1_29": "PV1‑29 – Transfer to Bad Debt Code",
    "PV1_30": "PV1‑30 – Transfer to Bad Debt Date",
    "PV1_31": "PV1‑31 – Bad Debt Agency Code",
    "PV1_32": "PV1‑32 – Bad Debt Transfer Amount",
    "PV1_33": "PV1‑33 – Bad Debt Recovery Amount",
    "PV1_34": "PV1‑34 – Delete Account Indicator",
    "PV1_35": "PV1‑35 – Delete Account Date",
    "PV1_36": "PV1‑36 – Discharge Disposition",
    "PV1_37": "PV1‑37 – Discharged to Location",
    "PV1_38": "PV1‑38 – Diet Type",
    "PV1_39": "PV1‑39 – Servicing Facility",
    "PV1_40": "PV1‑40 – Bed Status",
    "PV1_41": "PV1‑41 – Account Status",
    "PV1_42": "PV1‑42 – Pending Location",
    "PV1_43": "PV1‑43 – Prior Temporary Location",
    "PV1_44": "PV1‑44 – Admit Date/Time",
    "PV1_45": "PV1‑45 – Discharge Date/Time",
    "PV1_46": "PV1‑46 – Current Patient Balance",
    "PV1_47": "PV1‑47 – Total Charges",
    "PV1_48": "PV1‑48 – Total Adjustments",
    "PV1_49": "PV1‑49 – Total Payments",
    "PV1_50": "PV1‑50 – Alternate Visit ID",
    "PV1_51": "PV1‑51 – Visit Indicator",
    "PV1_52": "PV1‑52 – Other Healthcare Provider",

    # PV2 – Patient Visit - Additional Information
    "PV2_1": "PV2‑1  –  Prior Pending Location",
    "PV2_1.1": "    PV2‑1.1  –  Point of Care",
    "PV2_1.2": "    PV2‑1.2  –  Room",
    "PV2_1.3": "    PV2‑1.3  –  Bed",
    "PV2_1.4": "    PV2‑1.4  –  Facility",
    "PV2_1.5": "    PV2‑1.5  –  Location Status",
    "PV2_1.6": "    PV2‑1.6  –  Person Location Type",
    "PV2_1.7": "    PV2‑1.7  –  Building",
    "PV2_1.8": "    PV2‑1.8  –  Floor",
    "PV2_1.9": "    PV2‑1.9  –  Location Description",
    "PV2_2": "PV2‑2  –  Accommodation Code",
    "PV2_2.1": "    PV2‑2.1  –  Identifier",
    "PV2_2.2": "    PV2‑2.2  –  Text",
    "PV2_2.3": "    PV2‑2.3  –  Name of Coding System",
    "PV2_2.4": "    PV2‑2.4  –  Alternate Identifier",
    "PV2_2.5": "    PV2‑2.5  –  Alternate Text",
    "PV2_2.6": "    PV2‑2.6  –  Name of Alternate Coding System",
    "PV2_3": "PV2‑3  –  Admit Reason",
    "PV2_4": "PV2‑4  –  Transfer Reason",
    "PV2_5": "PV2‑5  –  Patient Valuables",
    "PV2_6": "PV2‑6  –  Patient Valuables Location",
    "PV2_7": "PV2‑7  –  Visit User Code",
    "PV2_8": "PV2‑8  –  Expected Admit Date/Time",
    "PV2_9": "PV2‑9  –  Expected Discharge Date/Time",
    "PV2_10": "PV2‑10  –  Estimated Length of Inpatient Stay",
    "PV2_11": "PV2‑11  –  Actual Length of Inpatient Stay",
    "PV2_12": "PV2‑12  –  Visit Description",
    "PV2_13": "PV2‑13  –  Referral Source Code",
    "PV2_14": "PV2‑14  –  Previous Service Date",
    "PV2_15": "PV2‑15  –  Employment Illness Related Indicator",
    "PV2_16": "PV2‑16  –  Purge Status Code",
    "PV2_17": "PV2‑17  –  Purge Status Date",
    "PV2_18": "PV2‑18  –  Special Program Code",
    "PV2_19": "PV2‑19  –  Retention Indicator",
    "PV2_20": "PV2‑20  –  Expected Number of Insurance Plans",
    "PV2_21": "PV2‑21  –  Visit Publicity Code",
    "PV2_22": "PV2‑22  –  Visit Protection Indicator",
    "PV2_23": "PV2‑23  –  Clinic Organization Name",
    "PV2_24": "PV2‑24  –  Patient Status Code",
    "PV2_25": "PV2‑25  –  Visit Priority Code",
    "PV2_26": "PV2‑26  –  Previous Treatment Date",
    "PV2_27": "PV2‑27  –  Expected Discharge Disposition",
    "PV2_28": "PV2‑28  –  Signature on File Date",
    "PV2_29": "PV2‑29  –  First Similar Illness Date",
    "PV2_30": "PV2‑30  –  Patient Charge Adjustment Code",
    "PV2_31": "PV2‑31  –  Recurring Service Code",
    "PV2_32": "PV2‑32  –  Billing Patient Location",
    "PV2_33": "PV2‑33  –  Visit Care Code",
    "PV2_34": "PV2‑34  –  Visit Code",
    "PV2_35": "PV2‑35  –  Visit Description - Long",
    "PV2_36": "PV2‑36  –  Account Status",
    "PV2_37": "PV2‑37  –  Pending Location",
    "PV2_37.1": "    PV2‑37.1  –  Point of Care",
    "PV2_37.2": "    PV2‑37.2  –  Room",
    "PV2_37.3": "    PV2‑37.3  –  Bed",
    "PV2_37.4": "    PV2‑37.4  –  Facility",
    "PV2_37.5": "    PV2‑37.5  –  Location Status",
    "PV2_37.6": "    PV2‑37.6  –  Person Location Type",
    "PV2_37.7": "    PV2‑37.7  –  Building",
    "PV2_37.8": "    PV2‑37.8  –  Floor",
    "PV2_37.9": "    PV2‑37.9  –  Location Description",
    "PV2_38": "PV2‑38  –  Prior Patient Location",
    "PV2_38.1": "    PV2‑38.1  –  Point of Care",
    "PV2_38.2": "    PV2‑38.2  –  Room",
    "PV2_38.3": "    PV2‑38.3  –  Bed",
    "PV2_38.4": "    PV2‑38.4  –  Facility",
    "PV2_38.5": "    PV2‑38.5  –  Location Status",
    "PV2_38.6": "    PV2‑38.6  –  Person Location Type",
    "PV2_38.7": "    PV2‑38.7  –  Building",
    "PV2_38.8": "    PV2‑38.8  –  Floor",
    "PV2_38.9": "    PV2‑38.9  –  Location Description",
    "PV2_39": "PV2‑39  –  Admit Source Code",
    "PV2_40": "PV2‑40  –  Ambulatory Status",
    "PV2_41": "PV2‑41  –  VIP Indicator",
    "PV2_42": "PV2‑42  –  Admission Type Code",
    "PV2_43": "PV2‑43  –  Visit Indicator",
    "PV2_44": "PV2‑44  –  Other Healthcare Provider",



    #PID - Patient Identification
    "PID_1": "PID 1 - Set ID - PID",
    "PID_2": "PID 2 - Patient ID (External ID)",
    "PID_3": "PID 3 - Patient Identifier List",
    "PID_3.1": "    PID‑3.1 – ID",
    "PID_3.2": "    PID‑3.2 – Check Digit",
    "PID_3.3": "    PID‑3.3 – Check Digit Scheme",
    "PID_3.4": "    PID‑3.4 – Assigning Authority",
    "PID_3.5": "    PID‑3.5 – Identifier Type Code",
    "PID_3.6": "    PID‑3.6 – Assigning Facility",
    "PID_3.7": "    PID‑3.7 – Effective Date",
    "PID_3.8": "    PID‑3.8 – Expiration Date",
    "PID_4": "PID 4 - Alternate Patient ID",
    "PID_5": "PID 5 - Patient Name",
    "PID_5.1": "    PID‑5.1 – Patient Last Name",
    "PID_5.2": "    PID‑5.2 – Patient First Name",
    "PID_5.3": "    PID‑5.3 – Patient Middle Name",
    "PID_5.4": "    PID‑5.4 – Patient Suffix",
    "PID_6": "PID 6 - Mother's Maiden Name",
    "PID_7": "PID 7 - Date/Time of Birth",
    "PID_8": "PID 8 - Administrative Sex",
    "PID_9": "PID 9 - Patient Alias",
    "PID_10": "PID 10 - Race",
    "PID_11": "PID 11 - Patient Address",
    "PID_11.1": "    PID 11.1 - Street 1",
    "PID_11.2": "    PID 11.2 - Street 2",
    "PID_11.3": "    PID 11.3 - City",
    "PID_11.4": "    PID 11.4 - State",
    "PID_11.5": "    PID 11.5 - Postal Code",
    "PID_11.6": "    PID 11.6 - Country",
    "PID_12": "PID 12 - County Code",
    "PID_13": "PID 13 - Phone Number - Home",
    "PID_14": "PID 14 - Phone Number - Business",
    "PID_15": "PID 15 - Primary Language",
    "PID_16": "PID 16 - Marital Status",
    "PID_17": "PID 17 - Religion",
    "PID_18": "PID 18 - Patient Account Number",
    "PID_19": "PID 19 - SSN Number - Patient",
    "PID_20": "PID 20 - Driver's License Number - Patient",
    "PID_21": "PID 21 - Mother's Identifier",
    "PID_22": "PID 22 - Ethnic Group",
    "PID_23": "PID 23 - Birth Place",
    "PID_24": "PID 24 - Multiple Birth Indicator",
    "PID_25": "PID 25 - Birth Order",
    "PID_26": "PID 26 - Citizenship",
    "PID_27": "PID 27 - Veterans Military Status",
    "PID_28": "PID 28 - Nationality",
    "PID_29": "PID 29 - Patient Death Date and Time",
    "PID_30": "PID 30 - Patient Death Indicator",

    # EVN – Event Type (Standardfelder)
    "EVN_1": "EVN‑1 – Event Type Code",
    "EVN_2": "EVN‑2 – Recorded Date/Time",
    "EVN_3": "EVN‑3 – Date/Time Planned Event",
    "EVN_4": "EVN‑4 – Event Reason Code",
    "EVN_5": "EVN‑5 – Operator ID",
    "EVN_6": "EVN‑6 – Event Occurred (Date/Time)?",
    "EVN_7": "EVN‑7 – Event Facility",

    # NK1 – Next of Kin / Associated Party (typische Felder)
    "NK1_1": "NK1‑1 – Set ID – NK1",
    "NK1_2": "NK1‑2 – Name",
    "NK1_3": "NK1‑3 – Relationship",
    "NK1_4": "NK1‑4 – Address",
    "NK1_5": "NK1‑5 – Phone Number",
    "NK1_6": "NK1‑6 – Business Phone Number",
    "NK1_7": "NK1‑7 – Contact Role",
    "NK1_8": "NK1‑8 – Start Date",
    "NK1_9": "NK1‑9 – End Date",
    "NK1_10": "NK1‑10 – Next of Kin / Associated Parties Job Title",

    # AL1 – Allergy Information
    "AL1_1": "AL1‑1 – Set ID – AL1",
    "AL1_2": "AL1‑2 – Allergen Type Code",
    "AL1_3": "AL1‑3 – Allergen Code/Mnemonic/Description",
    "AL1_4": "AL1‑4 – Allergy Severity Code",
    "AL1_5": "AL1‑5 – Allergy Reaction",
    "AL1_6": "AL1‑6 – Identification Date",

    # DG1 – Diagnosis
    "DG1_1": "DG1‑1 – Set ID – DG1",
    "DG1_2": "DG1‑2 – Diagnosis Coding Method",
    "DG1_3": "DG1‑3 – Diagnosis Code",
    "DG1_4": "DG1‑4 – Diagnosis Description",
    "DG1_5": "DG1‑5 – Diagnosis Date/Time",
    "DG1_6": "DG1‑6 – Diagnosis Type",
    "DG1_7": "DG1‑7 – Major Diagnostic Category",
    "DG1_8": "DG1‑8 – Diagnostic Related Group",

    # IN1 – Insurance
    "IN1_1": "IN1‑1 – Set ID – IN1",
    "IN1_2": "IN1‑2 – Insurance Plan ID",
    "IN1_3": "IN1‑3 – Insurance Company ID",
    "IN1_4": "IN1‑4 – Insurance Company Name",
    "IN1_5": "IN1‑5 – Insurance Company Address",
    "IN1_6": "IN1‑6 – Insurance Co Contact Person",
    "IN1_7": "IN1‑7 – Insurance Co Phone Number",
    "IN1_8": "IN1‑8 – Group Number",
    "IN1_9": "IN1‑9 – Group Name",
    "IN1_10": "IN1‑10 – Insured's Group Emp ID",
    "IN1_11": "IN1‑11 – Insured's Group Emp Name",
    "IN1_12": "IN1‑12 – Plan Effective Date",
    "IN1_13": "IN1‑13 – Plan Expiration Date",
    "IN1_14": "IN1‑14 – Authorization Information",
    "IN1_15": "IN1‑15 – Plan Type",
}

EXAMPLE_HL7 = """MSH|^~\\&|SendingApp|SendingFac|ReceivingApp|ReceivingFac|202208101200||ADT^A01|MSG00001|P|2.3
EVN|A01|202208101200
PID|1||123456^^^Hospital^MR||Müller^Hans^A||19800101|M|||Musterstraße 1^^Musterstadt^DE^12345||0123456789|||M||123456789
PV1|1|I|Ward^123^Bed^1||||1234^Arzt^Max^^Dr.|||MED|||||||1234567|||||||||||||||||||||||||202208101200
"""

# Stylesheet laden
def load_stylesheet(filename):
    base_path = getattr(sys, '_MEIPASS', Path(__file__).parent)
    style_path = Path(base_path) / filename
    with open(style_path, "r", encoding="utf-8") as f:
        return f.read()


# Dialog-Box mit Host und IP + Message Panel
class HL7SendDialog(QDialog):
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("Send Test Message")
        self.setMinimumSize(1000, 500)
        self.layout = QVBoxLayout(self)

        # Eingabefeld für HL7-Nachricht
        self.message_edit = QTextEdit(self)
        self.message_edit.setPlaceholderText("Insert HL7-Message here...")
        self.layout.addWidget(QLabel("HL7-Message:"))
        self.layout.addWidget(self.message_edit)

        # Eingabefelder für Host und Port
        form_layout = QFormLayout()
        self.host_input = QLineEdit(self)
        self.host_input.setPlaceholderText("127.0.0.1")
        form_layout.addRow("Host/IP:", self.host_input)

        self.port_input = QLineEdit(self)
        self.port_input.setPlaceholderText("7777")
        form_layout.addRow("Port:", self.port_input)

        self.layout.addLayout(form_layout)

        # OK / Abbrechen-Buttons
        self.buttons = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel, self)
        self.buttons.accepted.connect(self.accept)
        self.buttons.rejected.connect(self.reject)
        self.layout.addWidget(self.buttons)

    def get_values(self):
        try:
            message = self.message_edit.toPlainText().strip()
            host = self.host_input.text().strip()
            port = int(self.port_input.text().strip())
            return message, host, port
        except Exception as e:
            QMessageBox.critical(self, "Error", f"Unkown Syntax: {e}")
            return None, None, None


# 2 Panele links, eine Legende Rechts
class HL7Tab(QWidget):
    def __init__(self):
        super().__init__()
        self.layout = QVBoxLayout(self)
        self.splitter = QSplitter(Qt.Horizontal)

        # Input Feld für HL7 Nachrichten
        self.text_edit = QTextEdit()
        self.text_edit.setPlaceholderText("Input Message here")
        self.text_edit.setPlainText(EXAMPLE_HL7)
        self.text_edit.setLineWrapMode(QTextEdit.NoWrap)
        self.text_edit.textChanged.connect(self.update_view)
        self.text_edit.setObjectName("hl7Input")

        # Browser für formattierte Anzeige
        self.hl7_view = QTextBrowser()
        self.hl7_view.setReadOnly(True)
        self.hl7_view.setLineWrapMode(QTextEdit.NoWrap)
        self.hl7_view.setObjectName("hl7Output")

        self.left_panel = QSplitter(Qt.Vertical)
        self.left_panel.addWidget(self.text_edit)
        self.left_panel.addWidget(self.hl7_view)

        # Legende für Segmente
        self.legend = QTreeWidget()
        self.legend.setHeaderLabels(["Seg.", "Feld", "Wert"])
        self.legend.setColumnWidth(0, 75)
        self.legend.setColumnWidth(1, 300)
        self.legend.setColumnWidth(2, 120)

        self.splitter.addWidget(self.left_panel)
        self.splitter.addWidget(self.legend)
        self.splitter.setStretchFactor(0, 3)
        self.splitter.setStretchFactor(1, 1)
        self.splitter.setSizes([700, 450])

        # Nachricht senden
        self.layout.addWidget(self.splitter)
        self.send_button = QPushButton("Send Test Message")
        self.send_button.clicked.connect(self.send_test_message)
        self.layout.addWidget(self.send_button)

        self.update_view()

    def send_test_message(self):
        dialog = HL7SendDialog(self)
        if dialog.exec_() == QDialog.Rejected:
            return

        hl7_message, host, port = dialog.get_values()
        if not hl7_message or not host or not port:
            QMessageBox.warning(self, "Error", "Wrong Syntax.")
            return

        try:
            ack = self.send_hl7_message_tcp(hl7_message, host, port)
            QMessageBox.information(self, "Sucess", f"Message was sent to {host}:{port} .\n\nAnswer:\n{ack}")
        except Exception as e:
            QMessageBox.critical(self, "Send-Error", f"Message couldn't be sent:\n{e}")

    def send_hl7_message_tcp(self, hl7_message: str, host: str, port: int) -> str:
        hl7_message = hl7_message.replace("\n", "\r")
        mllp_message = f"\x0b{hl7_message}\x1c\x0d".encode("utf-8")

        with socket.create_connection((host, port), timeout=10) as sock:
            sock.sendall(mllp_message)
            ack = sock.recv(4096).decode("utf-8", errors="ignore")
            print("Message from Server:")
            print(ack)
            return ack

    def update_view(self):
        raw = self.text_edit.toPlainText()
        self.hl7_view.clear()
        self.legend.clear()

        if not raw.strip():
            return

        raw = raw.replace('\n', '\r')

        try:
            message = parse_message(raw, validation_level=VALIDATION_LEVEL.QUIET)
        except HL7apyException as e:
            import traceback
            self.hl7_view.setPlainText(f"Parsing-Fehler:\n{repr(e)}\n\n{traceback.format_exc()}")
            return
        except Exception as e:
            self.hl7_view.setPlainText(f"Unerwarteter Fehler: {repr(e)}")
            return

        html_lines = []

        for segment in message.children:
            seg_name = segment.name
            raw_segment = segment.to_er7()
            color = SEGMENT_COLORS.get(seg_name, "#f8f8f2")
            fields_html = []

            seg_item = QTreeWidgetItem([seg_name])
            self.legend.addTopLevelItem(seg_item)

            if seg_name == "MSH":
                field_separator = raw_segment[3]
                encoding_chars_end = raw_segment.find(field_separator, 4)
                encoding_chars = raw_segment[4:encoding_chars_end]
                remaining_fields = raw_segment[encoding_chars_end + 1:].split(field_separator)
                raw_fields = [field_separator, encoding_chars] + remaining_fields
            else:
                raw_fields = raw_segment.split('|')[1:]

            for i, raw_val in enumerate(raw_fields, 1):
                field_key = f"{seg_name}_{i}"
                desc = FRIENDLY_FIELD_NAMES.get(field_key, field_key)
                val_html = raw_val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                fields_html.append(f'<a href="#" style="color:{color};" title="{desc}">{val_html}</a>')

                if seg_name == "MSH" and i == 2 and len(raw_val) == 4:
                    parent_item = QTreeWidgetItem(["", desc, ""])
                    seg_item.addChild(parent_item)
                    encoding_labels = ["Component Separator '^'", "Repetition Separator '~'", "Escape Character '\\'", "Subcomponent Separator '&'"]
                    for j, char in enumerate(raw_val, 1):
                        sub_desc = f"MSH‑2.{j} – {encoding_labels[j - 1]}"
                        child_item = QTreeWidgetItem(["", sub_desc, char])
                        child_item.setForeground(2, QColor("#888888"))
                        parent_item.addChild(child_item)
                else:
                    subfields = raw_val.split('^')
                    if len(subfields) > 1:
                        parent_item = QTreeWidgetItem(["", desc, ""])
                        seg_item.addChild(parent_item)
                        for j, subval in enumerate(subfields, 1):
                            sub_key = f"{field_key}.{j}"
                            sub_desc = FRIENDLY_FIELD_NAMES.get(sub_key, sub_key)
                            child_item = QTreeWidgetItem(["", sub_desc, subval])
                            child_item.setForeground(2, QColor("#888888"))
                            parent_item.addChild(child_item)
                    else:
                        item = QTreeWidgetItem(["", desc, raw_val])
                        item.setForeground(2, QColor("#888888"))
                        seg_item.addChild(item)

            full_line_html = f'<span style="color:{color};">{seg_name}|{"|".join(fields_html)}</span>'
            html_lines.append(full_line_html)

        self.hl7_view.setHtml("<br>".join(html_lines))
        self.legend.expandAll()


# Hauptfenster mit Tab-Verwaltung
class HL7Viewer(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("HL7 Parser Lookup")
        self.setMinimumSize(1500, 650)

        self.tabs = QTabWidget()
        self.tabs.setTabsClosable(True)
        self.tabs.tabCloseRequested.connect(self.close_tab)

        self.plus_tab = QWidget()
        self.tabs.addTab(self.plus_tab, "+")
        self.tabs.tabBar().setTabButton(self.tabs.count() - 1, QTabBar.RightSide, None)

        self.tabs.currentChanged.connect(self.on_tab_changed)
        self.tabs.tabBarDoubleClicked.connect(self.rename_tab)

        self.setCentralWidget(self.tabs)
        self.add_tab("New Message")

    def add_tab(self, name="New Message"):
        new_tab = HL7Tab()
        index = self.tabs.count() - 1
        self.tabs.insertTab(index, new_tab, name)
        self.tabs.setCurrentIndex(index)

    def close_tab(self, index):
        if self.tabs.count() <= 2:
            QMessageBox.warning(self, "Attention!", "At least One Tab must be kept open!")
            return
        self.tabs.removeTab(index)

    def on_tab_changed(self, index):
        if index == self.tabs.count() - 1:
            self.add_tab()

    def rename_tab(self, index):
        if index == self.tabs.count() - 1:
            return
        current_name = self.tabs.tabText(index)
        new_name, ok = QInputDialog.getText(self, "Rename Tab", "New Name:", text=current_name)
        if ok and new_name:
            self.tabs.setTabText(index, new_name)


def main():
    app = QApplication(sys.argv)
    
    # Font laden
    font_id = QFontDatabase.addApplicationFont("./SourceCodePro-Light.ttf")
    families = QFontDatabase.applicationFontFamilies(font_id)

    if families:
        SourceCodePro = families[0]
        print("Loaded Fontfamily:", SourceCodePro)
        SourceCodePro = QFont(SourceCodePro, 10)
        SourceCodePro.setLetterSpacing(QFont.AbsoluteSpacing, 1)  # Pacing zwischen Buchstaben
        app.setFont(SourceCodePro)

    else:
        print("Fehler: Font konnte nicht geladen werden")
        app.setFont(QFont("JetBrains Mono", 10))  # Fallback-Font
    
    #Stylesheet laden
    stylesheet_file = QFile("./stylesheet.qss")

    if not stylesheet_file.exists():
        print("Stylesheet not found!")
    else:
        if stylesheet_file.open(QFile.ReadOnly | QFile.Text):
            stream = QTextStream(stylesheet_file)
            qss = stream.readAll()
            app.setStyleSheet(qss)
            print("Stylesheet loaded (Length:", len(qss), "Character)")
            stylesheet_file.close()
        else:
            print("Stylesheet couldn't be opened.")


    # Breeze-Style aktivieren
    app.setStyle("breeze")

    
    # Icon setzen (kompatibel mit PyInstaller)
    base_path = getattr(sys, '_MEIPASS', Path(__file__).parent)
    icon_path = Path(base_path) / "hl7.ico"  # "icon.ico" für Windows
    app.setWindowIcon(QIcon(str(icon_path)))

    viewer = HL7Viewer()
    viewer.setWindowIcon(QIcon(str(icon_path)))
    viewer.show()

    sys.exit(app.exec())

if __name__ == "__main__":
    main()
