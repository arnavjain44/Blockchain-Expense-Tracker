# ExpenseChain — Enterprise Decentralized Shared Expense Settlement

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

---

## Public Demo (Free, Laptop + Cloudflare Tunnel)

This mode allows you to run a **free, temporary public demo** for approximately 5 real users from your personal Windows laptop without paid cloud hosting or VPS deployment.

### 🌐 Traffic Architecture

```
Demo Users (Internet / Mobile / Laptop)
                 ↓ (HTTPS)
Cloudflare Quick Tunnel (https://xxxxx.trycloudflare.com)
                 ↓
      localhost:8080 (Windows Laptop)
                 ↓
        Spring Boot Backend
                 ↓ (Private Localhost RPC)
Local Corda Nodes (Notary, Garvit, Arnav, Mridul)
```

### 🔒 Security & Privacy Guarantees
- **Port 8080 ONLY**: Cloudflare tunnel forwards incoming web traffic strictly to `http://localhost:8080`.
- **Corda Ports Kept Private**: Corda RPC/P2P ports (`10002–10032`) are bound to local loopback (`127.0.0.1`) and are **never** exposed to the internet.
- **No Firewall Opening**: No router port-forwarding or public inbound Windows firewall rules are created.
- **Free & Accountless**: Uses Cloudflare Quick Tunnels — no credit card, domain name, or paid account required.

### ⏱️ Important Operational Notes
- **Temporary URL**: Quick Tunnel URLs (`https://xxxxx.trycloudflare.com`) are ephemeral. A new URL is generated each time the tunnel is launched.
- **Keep Laptop Active**: The application runs entirely on your Windows laptop. Keep your laptop connected to power, awake, and connected to the internet throughout your demo.
- **Audience Size**: Intended for live demonstrations with approximately 5 concurrent users. This is not a 24/7 production server.

---

### 🚀 Public Demo Scripts

| Script | Purpose |
| :--- | :--- |
| **`INSTALL_CLOUDFLARED.bat`** | Installs Cloudflare CLI via `winget` (or provides official manual fallback). |
| **`CHECK_DEMO.bat`** | Audits Java 8, cloudflared, backend health, and verifies Corda ports are private. |
| **`START_PUBLIC_DEMO.bat`** | Launches Corda nodes, backend, waits for health check, starts tunnel, copies URL to clipboard. |
| **`STOP_PUBLIC_DEMO.bat`** | Safely terminates Cloudflare tunnel, Corda nodes, and Spring Boot backend. |

---

### 🔑 Optional Demo Access Code Protection

You can protect your public demo URL with an optional access code. If configured, visitors must provide the code via standard browser prompt or URL query parameter. `/api/health` always remains open for status monitoring.

To enable before launching:
```powershell
# In PowerShell:
$env:DEMO_ACCESS_CODE = "your-temporary-secret"
.\START_PUBLIC_DEMO.bat
```
```cmd
:: In Command Prompt:
set DEMO_ACCESS_CODE=your-temporary-secret
START_PUBLIC_DEMO.bat
```

Visitors can then access the demo via:
- Direct auto-login URL: `https://xxxxx.trycloudflare.com/?accessCode=your-temporary-secret`
- Standard browser HTTP Basic prompt: enter the code as the password.

If `DEMO_ACCESS_CODE` is unset or empty, the demo operates openly with standard application signup and login.

---

### 💾 Memory Configuration (8 GB RAM Laptops)

Corda nodes run with capsule JVM limits in `start-dev.ps1`:
```powershell
-Dcapsule.jvm.args=-Xmx512m
```
This allocates approximately 512 MB per Corda node (4 nodes = ~2 GB total JVM heap), leaving ample memory for Spring Boot (~384 MB), Cloudflared (~40 MB), and Windows system services on an 8 GB RAM laptop.

If you are running on a 16 GB RAM machine and want increased Corda node heap capacity:
1. Open `start-dev.ps1`.
2. Update `-Xmx512m` to `-Xmx768m`.

---

### 📝 Demo Day Checklist

Follow this checklist prior to demonstrating ExpenseChain:

- [ ] Plug laptop into power charger
- [ ] Disable Windows sleep temporarily (or set sleep timer to "Never while plugged in")
- [ ] Confirm active internet connection
- [ ] Run `RESET_DEV.bat` (cleans previous demo data and stale state)
- [ ] Run `CHECK_DEMO.bat` (verifies Java 8, cloudflared, port isolation)
- [ ] Run `START_PUBLIC_DEMO.bat`
- [ ] Wait until `EXPENSECHAIN PUBLIC DEMO IS LIVE` banner and Cloudflare URL appear
- [ ] Copy public URL (automatically placed on Windows clipboard)
- [ ] Test from mobile phone using **MOBILE DATA** (cellular), NOT the same Wi-Fi
- [ ] Test signup (create test user account)
- [ ] Test login
- [ ] Test creating an expense and group
- [ ] Test second user settling or verifying transaction on notarized ledger
- [ ] Keep laptop awake throughout demo session
- [ ] Run `STOP_PUBLIC_DEMO.bat` after demo completes
