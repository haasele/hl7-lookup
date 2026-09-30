<img width="1584" height="396" alt="HL7-Lookup-Banner" src="https://github.com/user-attachments/assets/9acac1c2-cf47-45b5-b03f-807ed70c853a" />

# HL7 Lookup

A workspace for HL7 v2. The same screen runs as a desktop window and in the browser. The window runs HAPI in-process and opens no port. The browser is a WebAssembly client of a separate headless process.

## Run the desktop window

The repository carries the [Kotlin toolchain](https://github.com/JetBrains/kotlin) wrapper (`kotlin` on macOS and Linux, `kotlin.bat` on Windows). The first run downloads the toolchain and a JDK.

```
./kotlin run -m desktop
```

The window does not listen on a port, so several copies can run side by side. Configured senders and receivers still open the ports you set for them.

## Run the browser

Build the web client, then start the engine headless:

```
./kotlin build -m web
./kotlin run -m desktop -- --server
```

Open `http://127.0.0.1:7780/`. `--port` changes that port. Validation, acknowledgements, new messages and MLLP/HTTP stay in that headless process. The page only works while it is running. The desktop window does not serve this page.

## Check the build

```
./kotlin test -m desktop
./kotlin build
```

## What the workspace does

One document is a tab. The screen has the interpretation, the raw message and the field grid, kept on the same field. Under that are the message list, senders, receivers, integrations and acknowledgements, plus field statistics and validation.

- Open, paste, save and download messages, and create one from a message type
- Sample messages for ADT, ORM, ORU, MDM, SIU, REF, VXU and ACK
- Search across every open message
- Delimiters, repetitions, components, local dates and table lists in the raw text
- Field grid with empty fields hidden, suggestions and a calendar
- Filters, highlighting and a field-by-field comparison
- Validation down to the subcomponent, shown in every view
- Anonymisation of PID, NK1, GT1, IN1 and MRG, free text in NTE and OBX, and a stable date shift
- Interface definitions with required fields, expected values, dates, tables and highlights, imported and exported as JSON
- MLLP and HTTP senders and receivers, acknowledgement list, and a connection test

The session holds the messages you have open.
