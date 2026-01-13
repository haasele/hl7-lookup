# HL7-Lookup
A simple and extendable HL7 Parser for v2.5
---
<img width="1584" height="396" alt="HL7-Lookup_Banner" src="https://github.com/user-attachments/assets/433a78ac-ac77-4186-8772-ed4bd3d785f5" />

---

## Prerequisites
- Python -> officially tested on Python 3.12
- (optional) -> Pyinstall

## How do I run it?

### Widows
Just clone the repo, navigate into it via terminal and Start it with

```
python.exe main.py
```


### Linux
Clone the repo, navigate into it via terminal and Start it with

```
python3 main.py
```


# Can I build an Executable out of it? (Windows/MacOS)

Yes! for this to work, you should use pyinstaller, below is an example syntax that works on Windows - You will still need to cange to your specific python Version, you need to install hl7apy `pip install hl7apy` and pyinstaller `pip install pyinstaller`

```
C:\Users\[Your-User]\AppData\Roaming\Python\Python313\Scripts\pyinstaller.exe --noconsole --onefile --add-data "C:\Users\[Your-User]\AppData\Roaming\Python\Python313\site-packages\hl7apy:hl7apy" --icon=hl7.ico --add-data "stylesheet.qss:." --add-data "hl7.ico:." --clean main.py
```

On Macos, clone the repo with
```
git clone https://github.com/haasele/hl7-lookup
```
And cd into that Directory

Then paste the following string in your terminal (you need pyinstall for this to work, install it with `pip install pyinstaller`)
```
pyinstaller --windowed --name HL7-Lookup --noconsole --onedir --icon=hl7.ico --add-data "/Users/rabbit/Desktop/Archive2/hl7-lookup/stylesheet.qss:." --add-data "hl7.ico:." --clean --collect-all hl7apy --add-data "SourceCodePro-Light.ttf:." main.py
```

## Screenshots
<img width="1910" height="1008" alt="image" src="https://github.com/user-attachments/assets/5607296d-36f8-457b-a365-4d0f464572b8" />
<img width="1916" height="1011" alt="image" src="https://github.com/user-attachments/assets/45f6e8a1-7ad2-4116-b8a7-3e24be05f5ad" />
<img width="1014" height="511" alt="image" src="https://github.com/user-attachments/assets/2066ac49-5f7b-43a1-aa87-4a9448c07384" />

### Have fun!
