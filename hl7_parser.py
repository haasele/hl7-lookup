# hl7_parser.py
from hl7_definitions import FRIENDLY_FIELD_NAMES, FIELD_DESCRIPTIONS

def parse_hl7(hl7_text):
    segments = hl7_text.strip().split('\n')
    parsed_segments = []

    for segment in segments:
        fields = segment.strip().split('|')
        segment_type = fields[0]
        parsed_fields = []

        for index, value in enumerate(fields[1:], start=1):
            friendly_name = FRIENDLY_FIELD_NAMES.get(segment_type, {}).get(index, f'Field {index}')
            description = FIELD_DESCRIPTIONS.get(segment_type, {}).get(index, '')
            parsed_fields.append({
                'segment': segment_type,
                'index': index,
                'friendly_name': friendly_name,
                'description': description,
                'value': value,
            })

        parsed_segments.append({
            'segment_type': segment_type,
            'fields': parsed_fields
        })

    return parsed_segments
