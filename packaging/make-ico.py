# Builds a Windows icon from the HL7 mark. packaging/bundle.sh calls it on Windows.
from PIL import Image

img = Image.open("desktop/resources/icons/hl7.png").convert("RGBA")
sizes = (16, 32, 48, 64, 128, 256)
icons = [img.resize((size, size), Image.Resampling.LANCZOS) for size in sizes]
icons[-1].save(
    "build/hl7.ico",
    format="ICO",
    append_images=icons[:-1],
    sizes=[(icon.width, icon.height) for icon in icons],
)
