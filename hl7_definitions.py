# hl7_definitions.py

FRIENDLY_FIELD_NAMES = {
    'MSH': {
        9: 'Message Type',
        10: 'Message Control ID',
        11: 'Processing ID',
        12: 'Version ID',
    },
    'PID': {
        3: 'Patient ID',
        5: 'Patient Name',
        7: 'Date of Birth',
        8: 'Gender',
    },
    # Weitere Segmente hier...
}

FIELD_DESCRIPTIONS = {
    'MSH': {
        9: 'Identifies the message type (e.g., ADT^A01)',
        10: 'Unique ID for the message',
        11: 'Processing type (e.g., P for Production)',
        12: 'HL7 version (e.g., 2.5)',
    },
    'PID': {
        3: 'Unique patient identifier',
        5: 'Full patient name (last^first^middle)',
        7: 'Patient’s date of birth',
        8: 'Gender of the patient',
    },
    # Weitere Beschreibungen hier...
}
