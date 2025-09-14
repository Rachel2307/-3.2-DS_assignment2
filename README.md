# Assignment 2 — Weather Aggregation Server

**Course:** COMP SCI 3012 – Distributed Systems\
**Student Name:** Ngoc Thanh Uyen Ho\
**Student ID**: a1875049
---

## 1. Project Overview

This project implements a **client/server system** that aggregates and distributes weather data in **JSON format** via a **RESTful API**.

### Key Features

* **Aggregation Server**:

    * Accepts `PUT` requests from multiple Content Servers.
    * Serves aggregated weather data to multiple GET Clients.
    * Removes stale data after **30 seconds** (expiry).
    * Crash-safe persistence using atomic file operations.

* **Content Server**:

    * Reads a local `.txt` weather file, converts it into JSON, and uploads it via `PUT`.
    * Supports idempotent updates (same input → same output).

* **GET Client**:

    * Requests and displays aggregated weather data (`GET /weather.json`).
    * Can also request a single record by station ID (`GET /weather/{id}`).

* **Consistency**: All entities implement **Lamport clocks** to maintain a consistent order of events across processes.

---

## 2. Requirements

* **Java:** 17+
* **Maven:** 3.9+
* **JUnit 5** (via Maven Surefire plugin)

### Libraries Used
* **Jackson 2.17.2** (`jackson-databind`, `jackson-core`, `jackson-annotations`) — JSON parsing/serialization
* **JUnit Jupiter 5.10.2** — testing framework
* **Java SE standard libraries** — `HttpServer`, `ExecutorService`, `NIO`, etc.

### Build Plugins
* **maven-compiler-plugin 3.13.0** — Java 17 compilation
* **maven-surefire-plugin 3.2.5** — runs `*Test` and `*IT` tests

---

## 3. Build & Setup

Clone or unzip the project, then run:

```bash
# Clean and compile
mvn -q clean package

# Generate runtime classpath file
mvn -q -DincludeScope=runtime dependency:build-classpath -Dmdep.outputFile=cp.txt

```
This produces:
* `target/classes/` (compiled .class files)
* `cp.txt` (dependency classpath file for running with java)

---

## 4. Running the System

### 4.1 Aggregation Server

Starts on default port **4567** or accepts a custom port as argument.

```bash
# default port
java -cp target/classes:$(cat cp.txt) app.AggregationServer

# custom port
java -cp target/classes:$(cat cp.txt) app.AggregationServer 8080
```

---

### 4.2 Content Server

Uploads weather data from a `.txt` file to the server.

```bash
# Adelaide input text file
java -cp target/classes:$(cat cp.txt) app.ContentServer http://127.0.0.1:4567 resources/sample/adelaide.txt
# Sydney input text file
java -cp target/classes:$(cat cp.txt) app.ContentServer http://127.0.0.1:4567 resources/sample/sydney.txt
```

When running interactively, the Content Server provides a simple menu:
* Option 1 → Reads the current file and sends a PUT request.

* Option 2 → Lets you change the weather data file path (e.g., switch to sydney.txt).

* Option 3 → Exit the Content Server.

```bash

1. Send PUT to Aggregation Server
2. Change weather data file
3. Exit

```

* First successful upload returns **201**.
* Subsequent updates return **200**.
* Empty body returns **204**.
* Malformed JSON returns **500**.

---

### 4.3 GET Client

```bash
# fetch all records
java -cp target/classes:$(cat cp.txt) app.GETClient http://127.0.0.1:4567

# fetch by station ID
java -cp target/classes:$(cat cp.txt) app.GETClient http://127.0.0.1:4567 IDS60901
```

Output is human-readable key\:value lines, not raw JSON.

---

## 5. API Specification

| Method | Path            | Response Codes          | Description                         |
| ------ | --------------- | ----------------------- | ----------------------------------- |
| GET    | `/weather.json` | 200, 204, 500           | Returns aggregated weather data     |
| GET    | `/weather/{id}` | 200, 204, 400           | Returns single record by station id |
| PUT    | `/weather.json` | 201, 200, 204, 400, 500 | Adds or updates weather data        |
| Other  | Any             | 400                     | Simplified per assignment spec      |

**Headers:**

* `Content-Type: application/json` for PUT/GET responses.
* `X-Lamport: <value>` included in every request and response.

---

## 6. Lamport Clock Rules

* Each process (server, client, content server) maintains a **Lamport clock** (long integer).
* **Increment (tick):** before sending a message, after local events, after processing input.
* **Merge:** on receiving a message with Lamport `L_remote`, set `L_local = max(L_local, L_remote) + 1`.
* **Header:** always include `X-Lamport` in requests and responses.

This ensures:
* Concurrent PUTs are serialized in Lamport order.
* Interleaving PUT/GET/PUT operations respect event causality.

---

## 7. Persistence & Crash Safety

* Data written via **StateStore** using *temp + atomic rename* pattern.
* On crash during write: server recovers last known consistent JSON file on restart.
* Ensures durability and no partial writes.

---

## 8. Expiry Mechanism

* Each content server record is timestamped when last updated.
* An **ExpiryScheduler** task runs every few seconds to remove records older than **30 seconds**.
* Expired entries are removed from aggregate and will not appear in GET responses.

---

## 9. Automated Testing

Run the test suite:

```bash
mvn test
```

Test cover:

* **Startup:** server responds (StartupIT).
* **PUT/GET:** 201 → 200 → GET returns aggregate (PutGetIT).
* **Error Handling:** 400 (invalid), 204 (empty), 500 (malformed JSON) (InvalidInputIT).
* **Concurrency:** concurrent PUTs serialized; GET remains consistent (ConcurrencyIT).
* **Lamport Ordering:** interleaved PUT→GET→PUT returns consistent results (LamportIT).
* **Persistence:** restart restores saved state (PersistenceIT).
* **Expiry:** records expire after 30s (ExpiryIT, skipped in fast runs).

### Notes

* ExpiryIT can be run in a “slow mode” (\~40s wait).
* All other tests complete in \~3 seconds.

---

## 10. File/Folder Structure

```
Assignment2DSFinal/
├─ pom.xml
│    Maven build file — manages dependencies (Jackson, JUnit), 
│    plugins (compiler, surefire), and Java version.
│
├─ resources/
│   └─ sample/
│        ├─ adelaide.txt     # Sample weather station data (text → JSON)
│        └─ sydney.txt       # Another sample station file
│
├─ src/
│   ├─ main/java/
│   │   ├─ api/              # HTTP route handlers
│   │   │   ├─ Routes.java                # Registers all contexts (/weather.json, /weather/{id})
│   │   │   ├─ GetWeatherHandler.java     # Handles GET /weather.json
│   │   │   ├─ GetWeatherByIdHandler.java # Handles GET /weather/{id}
│   │   │   └─ PutWeatherHandler.java     # Handles PUT /weather.json
│   │   │
│   │   ├─ app/              # Executable entry points
│   │   │   ├─ AggregationServer.java     # Main server, starts HTTP service on given port
│   │   │   ├─ ContentServer.java         # CLI client that PUTs data from local files
│   │   │   ├─ GETClient.java             # CLI client that GETs data from server
│   │   │   └─ ConsoleMenu.java           # Menu utility for interactive runs
│   │   │
│   │   ├─ core/             # Domain logic
│   │   │   ├─ AggregationService.java    # Validates JSON, aggregates records, expiry management
│   │   │   ├─ WeatherRecord.java         # Model for a single weather record
│   │   │   ├─ SourceState.java           # Tracks metadata per content server (last update, expiry)
│   │   │   └─ LamportClock.java          # Implementation of Lamport logical clock
│   │   │
│   │   ├─ store/            # Persistence layer
│   │   │   └─ StateStore.java            # Crash-safe persistence (temp + atomic rename)
│   │   │
│   │   └─ util/             # Utilities & configuration
│   │       ├─ Config.java                 # Constants (timeouts, expiry seconds, header names)
│   │       ├─ HttpUtil.java               # Shared HTTP helper (sends GET/PUT with Lamport headers)
│   │       ├─ JsonUtil.java               # JSON (Jackson) parsing/serialization helpers
│   │       └─ ExpiryScheduler.java        # Background task to remove expired records
│   │
│   └─ test/java/
│       ├─ integration/      # Black-box integration tests (JUnit 5)
│       │   ├─ AlternatePortIT.java        # Verifies server responds on non-default ports
│       │   ├─ ConcurrencyIT.java          # Concurrent PUTs are serialized, GET consistent
│       │   ├─ ExpiryIT.java               # Records expire after TTL (skipped in fast runs)
│       │   ├─ InvalidInputIT.java         # Tests 400/204/500 error codes
│       │   ├─ LamportIT.java              # Verifies PUT→GET→PUT preserves Lamport order
│       │   ├─ PersistenceIT.java          # Server restarts restore persisted state
│       │   ├─ PutGetIT.java               # First PUT=201, subsequent=200, GET returns record
│       │   └─ StartupIT.java              # Server responds on startup
│       │
│       └─ testutil/         # Helpers for integration tests
│           ├─ Await.java                   # Utility for waiting with timeout/retry
│           ├─ ClientDrivers.java           # Wraps ContentServer/GETClient for tests
│           ├─ JsonAsserts.java             # Assertions on JSON responses
│           ├─ ServerLauncher.java          # Starts/stops AggregationServer for tests
│           └─ TestData.java                # Provides sample JSON payloads
│
└─ README.md
     Project documentation — overview, requirements, build/run instructions,
     API spec, Lamport clock rules, persistence/expiry explanation, 
     testing strategy, submission checklist.

```


---

## 11. Known Constraints

* Expiry TTL is fixed at **30 seconds** (can be made configurable if needed).
* JSON parsing uses **Gson** 
* Only **GET** and **PUT** are supported; other methods return 400.

---
perfect — we can make this into a **concise “📋 Manual Test Scenarios” section** at the end of your `README.md`.
since you already included detailed build/run commands earlier in the README, we’ll just reference them here and focus on **steps + expected output**.

---

## 12. Manual Test Scenarios

> Pre-requisite: Build (`mvn -q -DskipTests package`) and generate classpath (`mvn -q -DincludeScope=runtime dependency:build-classpath -Dmdep.outputFile=cp.txt`).
> Use the run commands from earlier sections (`AggregationServer`, `ContentServer`, `GETClient`).

---

### 1) Start Aggregation Server (empty store)

**T1**
Run server → expected log:

```
HTTP server listening on 4567
AggregationServer started on port 4567 (Lamport=0, expiry=30s)
```

**T2**
Run GET client → expected:

```
Status: 204 No Content  X-Lamport=<n>
--- JSON ---
<empty>
```

---

### 2) Content Server PUT (valid data)

**T2** send `adelaide.txt` → expected menu:

```
------ Content Server ------
1. Send PUT to Aggregation Server
2. Change weather data file
3. Exit
>
```

Choose `1` → expected:

```
PUT status=201, Lamport=<n>
```

**T1 log**:

```
[PUT] id=IDS60901 L=<n> q=0
```

---

### 3) Client GET (valid data)

**T3** run GET client → expected:

```
Status: 200 OK  X-Lamport=<n>
--- JSON ---
[{"id":"IDS60901", ... "state":"SA", ...}]
```

---

### 4) Data expiry (TTL 30s)

Wait >30s → run GET again → expected:

* **T1 log**: `[expiry] removed=1 ids=[IDS60901] L=<n>`
* **T3 output**: `Status: 204 No Content`

---

### 5) PUT with missing id (invalid)

Send JSON without `"id"` → expected:

* Content server shows non-2xx result.
* Server log: no `[PUT]` applied.
* GET remains unchanged (204 if empty).

---

### 6) Multiple Content Servers (concurrency)

Run Adelaide + Sydney content servers → expected:

* **T1 log**:

  ```
  [PUT] id=IDS60901 L=<n>
  [PUT] id=IDS60902 L=<n+1>
  ```
* **T3 GET**: shows JSON with both records.

---

### 7) Multiple Clients (concurrent GET)

Run two GETClients at same time → expected:

* Both print `200 OK`.
* Lamport headers strictly increase.

---

### 8) Persistence (restart recovery)

Stop server, restart, then GET → expected:

* If within 30s, records restored (200 OK).
* If >30s, expiry applies (204).
* No corruption.

---

### 9) Lamport Clock Verification

PUT → GET → PUT → GET → expected:

* Server logs: Lamport monotonic increase.
* GET responses: `X-Lamport` header strictly increases across sequence.

---

### 10) Invalid Client Request (wrong path)

`curl http://localhost:4567/wrongpath` → expected:

```
HTTP/1.1 400 Bad Request
```

(or `404 Not Found`, acceptable).

---

### 11) Custom Port

Start server on `9000` → run Content/GET clients against port 9000 → expected same behaviour.

---

### 12) Empty PUT

Send an empty file → expected:

```
Status: 204 No Content
```

GET remains unchanged.

---

