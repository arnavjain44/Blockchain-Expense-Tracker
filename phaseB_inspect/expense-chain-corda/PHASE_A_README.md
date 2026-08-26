# Phase A — Corda Local Environment (Windows)

Goal for this phase, and ONLY this phase: get 4 Corda nodes (Notary,
Garvit, Arnav, Mridul) to start locally with no CorDapp logic yet.
Nothing else. Once this works, we move to Phase B (the first real
ExpenseState).

---

## 1. Install JDK 8 (Zulu build)

Corda 4.x's Gradle tooling targets JDK 8 specifically — a newer JDK
will cause the build/deployNodes step to fail with obscure errors, so
don't substitute JDK 11/17 here even though you may have them already.

1. Download **Zulu JDK 8** (Windows x64 .msi installer):
   https://www.azul.com/downloads/?version=java-8-lts&os=windows&package=jdk
2. Run the installer. Note the install path, typically:
   `C:\Program Files\Zulu\zulu-8\`
3. Set `JAVA_HOME` and update `PATH`:
   - Search "Environment Variables" in the Start menu → **Edit the system environment variables** → **Environment Variables**
   - Under **System variables**, click **New**:
     - Variable name: `JAVA_HOME`
     - Variable value: `C:\Program Files\Zulu\zulu-8` (adjust to your actual path)
   - Edit the `Path` variable → **New** → add `%JAVA_HOME%\bin`
4. Open a **new** Command Prompt and verify:
   ```
   java -version
   ```
   Expected output should mention `1.8.0` and `Zulu`.

---

## 2. Install standalone Gradle 6.9.3 (one-time, just to generate the wrapper)

1. Download the binary-only zip:
   https://services.gradle.org/distributions/gradle-6.9.3-bin.zip
2. Extract it to, e.g., `C:\Gradle\gradle-6.9.3`
3. Add `C:\Gradle\gradle-6.9.3\bin` to your `Path` (same Environment Variables screen as above)
4. Open a **new** Command Prompt and verify:
   ```
   gradle -v
   ```
   Expected output should show `Gradle 6.9.3` and `JVM: 1.8...`.

---

## 3. Unzip the project and generate the Gradle wrapper

1. Unzip the `expense-chain-corda` project I gave you to, e.g., `C:\Dev\expense-chain-corda`
2. Open Command Prompt in that folder:
   ```
   cd C:\Dev\expense-chain-corda
   gradle wrapper --gradle-version 6.9.3
   ```
   This generates `gradlew.bat`, `gradlew`, and `gradle\wrapper\gradle-wrapper.jar` —
   from now on always use `gradlew.bat`, not the standalone `gradle`, so the
   project always builds with the exact pinned Gradle version regardless of
   what's on your PATH later.

---

## 4. Build the node topology

```
gradlew.bat deployNodes
```

This will:
- Download Corda 4.11 platform jars (first run takes a while — several hundred MB from Maven Central + R3's Artifactory)
- Generate 4 node folders under `build\nodes\`: `Notary`, `Garvit`, `Arnav`, `Mridul`
- Generate a `runnodes.bat` script in `build\nodes\`

**Expected output:** ends with `BUILD SUCCESSFUL`.

If it fails, paste me the full error output — first-run dependency
resolution issues (wrong repo URL, version mismatch) are the most
likely failure point and I'll adjust `build.gradle` accordingly.

---

## 5. Start the nodes

```
cd build\nodes
runnodes.bat
```

This opens **4 separate terminal windows**, one per node (Notary,
Garvit, Arnav, Mridul). Each will print startup logs and, once ready,
show a Corda interactive shell prompt.

**Expected output per node**, ending with something like:
```
Node for "O=Garvit,L=New Delhi,C=IN" started up and registered in 8.2 sec
```

and a shell prompt:
```
Welcome to the Corda interactive shell.
Useful commands include 'help' to see what is available.

Fri Aug 21 12:03:11 UTC 2026>>>
```

---

## 6. Confirm success

In the **Garvit** node's shell, run:
```
run networkMapSnapshot
```
This should list all 4 parties (Notary, Garvit, Arnav, Mridul) with
their X.500 names — confirming the nodes can see each other on the
local network map.

---

## What to send back to me

1. The full console output of `gradlew.bat deployNodes` (or the error, if it fails)
2. Confirmation that all 4 `runnodes.bat` terminal windows reached "started up ... sec"
3. The output of `run networkMapSnapshot` from Garvit's node

Once you confirm this, we move to **Phase B: one real ExpenseState**.
