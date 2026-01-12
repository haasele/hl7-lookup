# hl7-lookup
A simple and extendable HL7 Parser for v2.5

## Prerequisites
- Python -> officially tested on Python 3.13
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
It also works on MacOS, but the icon probably needs to be in a different format.