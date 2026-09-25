# UrScan

**UrScan** is offline Android inventory scanning application designed for mobile data collection terminals such as the **Urovo CT48** as well as standard Android devices.

It enables inventory operators to rapidly scan barcodes and inventory numbers, manage quantities, and generate `.csv` or `.txt` files formatted specifically for direct import into **1C:Enterprise** or Excel via USB.

---

## Features

- **Hardware Scanner Integration:** Native support for industrial 2D barcode scanner engines via **Broadcast Intent** and **Hardware Keyboard Emulation** without locking camera hardware.
- **100% Offline Workflows:** Saves all inventory data locally in `Documents/UrScan` for instant USB drag-and-drop transfer to PC.
- **Flexible Export Options:** Support for `.csv` and `.txt` file formats with customizable delimiters (Semicolon `;`, Comma `,`, Tab `\t`).
- **Smart Barcode Grouping:** Option to automatically sum quantities for identical barcodes or append new rows.
- **High-Speed Scanning:** Zero-latency barcode processing with anti-duplication filtering designed for rapid inventory scanning.
- **In-App Auto-Updates:** Built-in update checker that fetches and installs new releases directly from GitHub Releases.
- **Localization:** Full support for English and Russian.

---

## Usage Workflow

1. **Create Inventory:** Tap `+` and name your document (or leave blank to auto-generate a timestamped name, e.g. `Inv_2025-02-17_14-30`).
2. **Scan Items:** Aim and press the physical hardware scan trigger on the terminal (or input manually).
3. **Save Document:** The file is automatically saved locally. Move to another location and create a new inventory sheet.
4. **Import to PC / 1C:** Connect the terminal to a PC via USB, copy the generated `.csv` / `.txt` files from `Documents/UrScan`, and import them into 1C.

---

## Installation

Download the latest `app-release.apk` from the [Releases](https://github.com/mbs0ft/UrScan/releases) page and install it directly on your terminal or Android device (Android 7.0+).

---

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Asynchronous Processing:** Kotlin Coroutines (Dispatchers.IO)
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35 / 37 (Android 12+)
