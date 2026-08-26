# ExpenseChain DA2 — Live Demo & Presentation Script

This guide outlines a **5-Minute Live Presentation Walkthrough** for evaluating or demonstrating **ExpenseChain DA2** (Enterprise Decentralized Shared Expense Settlement powered by R3 Corda DLT and Spring Boot).

---

## 🎯 Quick Launch
1. Double-click `DEMO_SHOWCASE.bat` (or open **`http://localhost:8080`**).
2. The environment automatically verifies the 4 Corda nodes (**Notary**, **Garvit**, **Arnav**, **Mridul**) and launches the interface.

---

## 🎬 5-Minute Presentation Flow

### **Phase 1: Architecture & Introduction (1 Minute)**
- **What to say**:
  > *"ExpenseChain solves multi-party shared expense tracking and settlement by replacing centralized databases with an enterprise Distributed Ledger Technology (DLT) — R3 Corda 4.11. Every financial claim, debt obligation, and settlement is an immutable Corda state verified by smart contracts and signed by a non-validating London Notary."*
- **What to show**:
  - Open `http://localhost:8080/api/health` to show all 4 Corda nodes online and responding in real time.
  - Show the landing page with the **Dual-Mode System** (Main Mode for clean production registration; Demo Mode for immediate sandbox presentation).

---

### **Phase 2: Interactive Sandbox & Multi-Party Split (1.5 Minutes)**
- **What to do**:
  1. Click **"Try Demo"** on the landing page.
  2. Notice the pre-seeded dynamic dataset with groups, members, and ongoing expenses.
  3. Use the **User Switcher** in the top navigation bar to seamlessly switch between **Garvit**, **Arnav**, and **Mridul**.
  4. Click on a Group (e.g., *"Apartment Flatmates"* or *"Weekend Trip"*).
  5. Click **"Add Expense"**:
     - Description: `Team Dinner & Groceries`
     - Total Amount: `$150.00`
     - Paid by: `Garvit`
     - Split Type: `Equal` across all participants.
     - Click **Save / Record**.
- **What to say**:
  > *"When an expense is recorded, ExpenseChain initiates an `AddExpenseFlow` on the Corda network. The Corda contract enforces strict mathematical splitting and conservation rules, outputting atomic UTXO state outputs to the vaults of all involved peers."*

---

### **Phase 3: Real-Time Settlement & Flow Execution (1 Minute)**
- **What to do**:
  1. Switch user to **Arnav** or **Mridul** (who owes a balance).
  2. In the balances dashboard, click **"Settle Up"**.
  3. Choose settlement amount (e.g., `$50.00`) and click **"Record Settlement"**.
- **What to say**:
  > *"The settlement triggers a `RecordSettlementFlow` across the peer nodes. The Corda London Notary ensures double-spend protection, consensus, and updates each participant's cryptographic ledger."*

---

### **Phase 4: Corda Blockchain Explorer & Audit Verification (1.5 Minutes)**
- **What to do**:
  1. Click the **"Blockchain / Ledger"** tab in the navigation bar.
  2. Point out:
     - **Cryptographic SHA-256 Transaction IDs** for each expense and settlement.
     - **Consensus & Signatures**: Verified by `O=Notary, L=London, C=GB`.
     - **UTXO Input/Output States** showing state transitions.
  3. Click **"Verify Ledger Integrity"**:
     - The backend scans vault states across the nodes and validates the Merkle tree and notary signatures, returning **"VALID - London Notary Consensus Active"**.
- **What to say**:
  > *"The blockchain ledger viewer provides complete cryptographic transparency and auditability. Every transaction is verifiable with zero single points of failure."*

---

## 🛠️ Management Commands
- **Launch Demo**: `DEMO_SHOWCASE.bat`
- **Start Dev**: `START_DEV.bat` (or `powershell -ExecutionPolicy Bypass -File .\start-dev.ps1`)
- **Stop Dev**: `STOP_DEV.bat` (or `powershell -ExecutionPolicy Bypass -File .\stop-dev.ps1`)
- **Reset Dev**: `RESET_DEV.bat` (or `powershell -ExecutionPolicy Bypass -File .\reset-dev.ps1`)
