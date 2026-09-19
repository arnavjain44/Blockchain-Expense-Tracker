# Corda 4.11 Dockerized Private Network

This directory contains the Docker configuration for running the **4-node private permissioned Corda 4.11 DLT network** for ExpenseChain.

---

## 1. Network Topology & Participants

The network consists of **4 logically separate Corda nodes** connected over a private Docker bridge network (`corda-net`):

| Node / Service Name | Role | X.500 Legal Name | P2P Port (Container) | RPC Port (Host-bound) | Admin RPC Port |
|:---|:---|:---|:---:|:---:|:---:|
| **`notary`** | Consensus Provider (Non-validating Notary) | `O=Notary,L=London,C=GB` | `10002` | `127.0.0.1:10003` | `127.0.0.1:10023` |
| **`garvit`** | Participant Node | `O=Garvit,L=New Delhi,C=IN` | `10005` | `127.0.0.1:10006` | `127.0.0.1:10026` |
| **`arnav`** | Participant Node | `O=Arnav,L=Mumbai,C=IN` | `10008` | `127.0.0.1:10009` | `127.0.0.1:10029` |
| **`mridul`** | Participant Node | `O=Mridul,L=Bengaluru,C=IN` | `10011` | `127.0.0.1:10012` | `127.0.0.1:10032` |

> [!NOTE]
> **Logical Separation vs Physical Hosting**:
> These 4 nodes are logically independent Corda participants with distinct X.500 cryptographic identities, private keys, vaults, and ledger states. In this setup, they run containerized on a single physical Docker host. This architecture provides full DLT consensus, transaction notarisation, and peer-to-peer flows while keeping deployment convenient and resource-efficient.

---

## 2. Docker Architecture & Communication

```
                   +------------------------------------------------------+
                   |                 Private Docker Network               |
                   |                      (corda-net)                     |
                   |                                                      |
                   |   +------------+                  +------------+     |
                   |   |   Notary   |<================>|   Garvit   |     |
                   |   | (P2P:10002)|   P2P TLS Flow   | (P2P:10005)|     |
                   |   +------------+                  +------------+     |
                   |         ^                               ^            |
                   |         | P2P TLS Flows                 |            |
                   |         v                               v            |
                   |   +------------+                  +------------+     |
                   |   |   Arnav    |<================>|   Mridul   |     |
                   |   | (P2P:10008)|                  | (P2P:10011)|     |
                   |   +------------+                  +------------+     |
                   +------------------------------------------------------+
                                     ^ RPC connections
                                     | (127.0.0.1 / internal)
                   +-----------------+------------------------------------+
                   |             Spring Boot Backend Server               |
                   +------------------------------------------------------+
```

1. **P2P Communication**:
   - Nodes discover and address each other using **Docker service names**: `notary:10002`, `garvit:10005`, `arnav:10008`, and `mridul:10011`.
   - Node identities and advertised addresses are pre-distributed via `network-parameters` and `additional-node-infos`.
   - Communication uses encrypted TLS connections over the private `corda-net` bridge network.

2. **RPC Security (No Public Exposure)**:
   - RPC ports are **strictly bound to the loopback interface (`127.0.0.1`)** on the host machine.
   - External internet clients cannot connect to node RPC endpoints.
   - When the Spring Boot backend runs on the host, it connects to `localhost:10006`, `localhost:10009`, etc.
   - When the Spring Boot backend runs in a Docker container on the same network, it connects to `garvit:10006`, `arnav:10009`, etc.

3. **RPC Authentication**:
   - Default RPC credentials are set to `user1` / `test` (matching `application.properties` default fallbacks).
   - Production secrets can be passed via environment variables `CORDA_RPC_USERNAME` and `CORDA_RPC_PASSWORD` (or a `.env` file).

---

## 3. Persistence & Data Isolation

Each node maintains **dedicated, isolated storage**; NO persistence data is shared between nodes:

| Volume Name | Mount Point | Purpose |
|:---|:---|:---|
| `expensechain-notary-persistence` | `/opt/corda/persistence` | Notary H2 database & consensus state |
| `expensechain-garvit-persistence` | `/opt/corda/persistence` | Garvit ledger vault (`persistence.mv.db`) |
| `expensechain-arnav-persistence`  | `/opt/corda/persistence` | Arnav ledger vault (`persistence.mv.db`) |
| `expensechain-mridul-persistence` | `/opt/corda/persistence` | Mridul ledger vault (`persistence.mv.db`) |
| `expensechain-*-logs`            | `/opt/corda/logs`        | Node runtime and audit logs |
| `expensechain-*-artemis`         | `/opt/corda/artemis`     | Artemis MQ persistent message broker queues |

Because Docker named volumes are used, **ledger state survives container restarts, updates, and container recreation**.

---

## 4. Operational Commands

### A. Build CorDapps and Start Network (Initial Setup)

**On Windows:**
```cmd
cd expense-chain-corda\docker
build_and_start.bat
```
*(Or from `expense-chain-corda`: `start_docker_nodes.bat`)*

**On Linux / macOS:**
```bash
cd expense-chain-corda/docker
chmod +x build_and_start.sh entrypoint.sh
./build_and_start.sh
```

**Using Docker Compose directly (if nodes are already generated):**
```bash
cd expense-chain-corda/docker
docker compose up -d
```

### B. Stop the Network (Without losing ledger state)
```bash
docker compose down
```
*(Containers are removed, but persistent named volumes remain intact).*

### C. Restart the Network
```bash
docker compose up -d
```

### D. Inspect Running Containers
```bash
docker compose ps
```

### E. View Real-time Node Logs
```bash
# All nodes:
docker compose logs -f

# Specific node (e.g., Garvit):
docker compose logs -f garvit
```

### F. Rebuild Node Images
```bash
docker compose build --no-cache
```

### G. Completely Reset Network (WIPE all persistent data & reset demo)
```bash
docker compose down -v
```
> [!WARNING]
> Running `docker compose down -v` permanently removes the named volumes (`*-persistence`, `*-logs`, `*-artemis`). On the next startup, nodes will start with a fresh genesis ledger state.
