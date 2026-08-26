# ExpenseChain DA2 — Enterprise Decentralized Shared Expense Settlement

> **A Distributed Ledger Technology (DLT) platform powered by R3 Corda 4.11 and Spring Boot for multi-party group expense splitting, real-time debt simplification, and notarized consensus.**

---

## 🚀 Quickstart for Team Members (Zero-Config Setup)

When cloning this repository on a new Windows computer, you do **not** need to install Java 8 or Gradle manually. The setup script will download portable versions automatically.

### **Step 1: Clone Repository**
```powershell
git clone <repository-url>
cd <repository-folder>
```

### **Step 2: Setup Environment (One-Time)**
Double-click **`SETUP_ENVIRONMENT.bat`** (or run `powershell -ExecutionPolicy Bypass -File .\setup_environment.ps1`).

This will automatically:
- Download portable **OpenJDK 8 (Temurin 8u412)** into `tools/jdk8`.
- Download portable **Gradle 6.9.3** into `tools/gradle-6.9.3`.
- Deploy the 4 Corda nodes (**Notary**, **Garvit**, **Arnav**, **Mridul**).
- Compile CorDapps and build the Spring Boot backend.

### **Step 3: Run the Application**
Double-click **`DEMO_SHOWCASE.bat`** (or **`START_DEV.bat`**).
- Web Dashboard: **[http://localhost:8080](http://localhost:8080)**
- Health Check: **[http://localhost:8080/api/health](http://localhost:8080/api/health)**

---

## 🛠️ Project Control Scripts

| Script | Purpose |
| :--- | :--- |
| **`SETUP_ENVIRONMENT.bat`** | One-time setup: downloads portable JDK 8, Gradle, builds nodes & backend |
| **`DEMO_SHOWCASE.bat`** | Starts environment, opens browser, and opens the demo presentation guide |
| **`START_DEV.bat`** | Starts 4 Corda nodes and the Spring Boot backend server |
| **`STOP_DEV.bat`** | Cleanly terminates all node/backend processes and releases ports |
| **`RESET_DEV.bat`** | Resets test database and node ledger states |

---

## 🏗️ Architecture & Network Topology

```
                  ┌─────────────────────────────────────┐
                  │    London Notary (Corda 4.11)       │
                  │  P2P: 10002 | RPC: 10003            │
                  └──────────────────┬──────────────────┘
                                     │ (Consensus & Notarization)
          ┌──────────────────────────┼──────────────────────────┐
          │                          │                          │
┌─────────▼─────────┐      ┌─────────▼─────────┐      ┌─────────▼─────────┐
│    Garvit Node    │      │    Arnav Node     │      │    Mridul Node    │
│  P2P: 10005       │◄────►│  P2P: 10008       │◄────►│  P2P: 10011       │
│  RPC: 10006       │      │  RPC: 10009       │      │  RPC: 10012       │
└─────────┬─────────┘      └─────────┬─────────┘      └─────────┬─────────┘
          │                          │                          │
          └──────────────────────────┼──────────────────────────┘
                                     │ (RPC Connection Pool)
                  ┌──────────────────▼──────────────────┐
                  │ Spring Boot Backend (Port: 8080)    │
                  │ - Health: /api/health               │
                  │ - Blockchain: /api/blockchain/*     │
                  │ - Web UI: http://localhost:8080     │
                  └─────────────────────────────────────┘
```

---

## 📋 System Requirements
See [REQUIREMENTS.md](REQUIREMENTS.md) and [requirements.txt](requirements.txt) for full specifications.
- **OS**: Windows 10 / 11 (64-bit)
- **Memory**: 8 GB RAM minimum (16 GB recommended)
- **Ports**: `8080`, `10002-10032` must be available.
