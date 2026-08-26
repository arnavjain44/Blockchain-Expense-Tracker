# Phase B — First Real ExpenseState Transaction

Assumes Phase A already worked (4 nodes started, `networkMapSnapshot`
showed all 4 parties). This phase adds:

- `contracts-java` — `ExpenseState`, `ExpenseContract`
- `workflows-java` — `AddExpenseFlow` (+ responder)

and wires them into every node.

---

## 1. Replace your project folder

Unzip this new archive **over** your existing `expense-chain-corda`
folder (or just extract fresh to the same path) — it's the full
project, Phase A + B combined. Your generated `gradlew.bat` and
`gradle\wrapper\` from Phase A are untouched; don't regenerate them.

---

## 2. Rebuild and redeploy the nodes

From the project root:
```
gradlew.bat deployNodes
```
This now also compiles `contracts-java` and `workflows-java` and
copies both cordapp jars into each node's `cordapps\` folder. If any
node terminals from Phase A are still open, close them first.

**Expected:** `BUILD SUCCESSFUL`, and inside
`build\nodes\Garvit\cordapps\` (same for Arnav, Mridul, and Notary)
you should see two new jars: `contracts-java-1.0-SNAPSHOT.jar` and
`workflows-java-1.0-SNAPSHOT.jar`.

---

## 3. Start the nodes

```
cd build\nodes
runnodes.bat
```

Same as Phase A — 4 terminal windows, each ending in the Corda shell
prompt. This time each node's startup log will also mention loading
the `ExpenseChain Contracts` and `ExpenseChain Flows` cordapps.

---

## 4. Run the flow from Garvit's node

In **Garvit's** shell, run (all on one line):

```
flow start AddExpenseFlow expenseId: "42", groupId: "1", amountMinorUnits: 150000, currency: "INR", participants: [O=Garvit,L=New Delhi,C=IN, O=Arnav,L=Mumbai,C=IN, O=Mridul,L=Bengaluru,C=IN], splitType: "EQUAL", splitDetails: {"O=Garvit,L=New Delhi,C=IN": 50000, "O=Arnav,L=Mumbai,C=IN": 50000, "O=Mridul,L=Bengaluru,C=IN": 50000}
```

This represents: Garvit paid ₹1,500.00 (150000 paise), split equally
3 ways (₹500.00 / 50000 paise each) between Garvit, Arnav and Mridul —
the exact example from DA2 prompt section 10.

**Expected output:** ends with something like
```
✅ AddExpenseFlow finished
Result: SignedTransaction(id=<some tx hash>, ...)
```
That `id` is the real Corda transaction ID — this is what will show
on the upgraded Blockchain page later (Phase E), replacing the fake
SHA-256 hash from DA1.

---

## 5. Confirm it's genuinely multi-party (this is the important check)

Query the Vault from **all three** app nodes — not just Garvit's:

**In Garvit's shell:**
```
run vaultQuery contractStateType: com.expensechain.states.ExpenseState
```

**In Arnav's shell:**
```
run vaultQuery contractStateType: com.expensechain.states.ExpenseState
```

**In Mridul's shell:**
```
run vaultQuery contractStateType: com.expensechain.states.ExpenseState
```

**Expected:** all three return the same `ExpenseState` (expenseId
"42", payer Garvit, amount 150000 INR-minor-units) even though only
Garvit signed the transaction. That's the proof this isn't "store it
in a database and call it blockchain" — Arnav and Mridul received and
independently stored the ledger fact because `FinalityFlow` named them
as participants (see DA2 prompt section 10).

---

## 6. Try an invalid expense (contract rejection check)

Still in Garvit's shell, try an amount of 0:
```
flow start AddExpenseFlow expenseId: "43", groupId: "1", amountMinorUnits: 0, currency: "INR", participants: [O=Garvit,L=New Delhi,C=IN, O=Arnav,L=Mumbai,C=IN], splitType: "EQUAL", splitDetails: {"O=Garvit,L=New Delhi,C=IN": 0, "O=Arnav,L=Mumbai,C=IN": 0}
```

**Expected:** the flow fails with a `TransactionVerificationException`
citing `"Expense amount must be positive."` — proving `ExpenseContract`
is actually being enforced, not bypassed.

---

## What to send back to me

1. Confirmation `deployNodes` succeeded with both new jars present
2. The `AddExpenseFlow` success output (the transaction ID) from Garvit
3. The `vaultQuery` output from **all three** of Garvit/Arnav/Mridul showing the same state
4. The rejection output from step 6

Once confirmed, we move to **Phase C: SettlementState + SettlementContract + RecordSettlementFlow** — same pattern, applied to the "record settlement" side of the app.
