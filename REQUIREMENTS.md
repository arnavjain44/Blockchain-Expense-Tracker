# ExpenseChain DA2 — Requirements & System Specifications

This document defines the system prerequisites, runtime components, and automated setup instructions for team members setting up **ExpenseChain DA2** on a new machine.

---

## 💻 1. System Prerequisites

| Component | Minimum Specification | Recommended Specification |
| :--- | :--- | :--- |
| **Operating System** | Windows 10 (64-bit) | Windows 11 (64-bit) |
| **RAM (Memory)** | 8 GB RAM | 16 GB RAM |
| **CPU** | 4 Cores (x86_64) | 6+ Cores |
| **Free Disk Space** | 3 GB free disk space | 5 GB free disk space |
| **PowerShell** | PowerShell 5.1+ (Built-in to Windows 10/11) | PowerShell 5.1+ |

---

## ☕ 2. Java & Build Tool Versions

> [!IMPORTANT]
> **R3 Corda 4.11 strictly requires Java 8 (Hotspot JVM).** Newer versions (Java 11, 17, 21) are **incompatible** with Corda 4.11 and will fail with bytecode verification errors.
> You **do NOT** need to manually install Java on your system. Running `SETUP_ENVIRONMENT.bat` downloads a self-contained portable OpenJDK 8 directly into the `tools/` folder.

- **Java JDK**: Eclipse Temurin OpenJDK `8u412-b08` x64 (Portable in `tools/jdk8`)
- **Gradle**: Gradle `6.9.3` (Portable in `tools/gradle-6.9.3`)
- **R3 Corda**: Community Edition `4.11`
- **Quasar (Flow Fiber Instrumenter)**: `0.7.12_r3`
- **Spring Boot**: `2.7.18` (with embedded Tomcat 9)

---

## 🌐 3. Network Ports Allocation

The following local ports are utilized by the application and Corda nodes. They must be available:

| Port | Service | Description |
| :---: | :--- | :--- |
| **`8080`** | **Web Backend & UI** | Spring Boot REST API & Interactive Web Dashboard |
| **`10002` / `10003`** | **Notary Node** | Non-validating London Notary P2P & RPC |
| **`10005` / `10006`** | **Garvit Node** | Peer Node P2P & RPC (`O=Garvit, L=New Delhi, C=IN`) |
| **`10008` / `10009`** | **Arnav Node** | Peer Node P2P & RPC (`O=Arnav, L=Mumbai, C=IN`) |
| **`10011` / `10012`** | **Mridul Node** | Peer Node P2P & RPC (`O=Mridul, L=Bengaluru, C=IN`) |

---

## ⚡ 4. 1-Click Setup for Team Members

When a team partner clones this repository to their computer, they only need to perform two steps:

1. **Double-click `SETUP_ENVIRONMENT.bat`** (or run `powershell -ExecutionPolicy Bypass -File .\setup_environment.ps1`):
   - Automatically downloads portable JDK 8 and Gradle 6.9.3 into `tools/`.
   - Compiles CorDapp contracts and workflows.
   - Deploys the 4 Corda nodes (`Notary`, `Garvit`, `Arnav`, `Mridul`).
   - Builds the Spring Boot backend JAR.
2. **Double-click `START_DEV.bat`** (or `DEMO_SHOWCASE.bat`):
   - Starts all 4 Corda nodes in the background.
   - Starts the Spring Boot backend.
   - Automatically opens `http://localhost:8080` in the browser!
