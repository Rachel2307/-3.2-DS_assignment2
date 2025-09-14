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
```

This compiles all sources and packages classes into target/classes.

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
mvn -q test
```

### Coverage

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
├─ resources/
│  └─ sample/
│     ├─ adelaide.txt
│     └─ sydney.txt
├─ src/
│  ├─ main/java/ (api, app, core, util, store)
│  └─ test/java/
│     ├─ integration/ (all IT tests)
│     └─ testutil/ (helpers)
└─ README.md
```

---

## 11. How to Extend / Notes for Markers

* **Scalability:** Multiple servers can be added; Lamport ensures ordering.
* **Fault tolerance:** Content servers tolerate server restarts and reconnect safely.
* **Interoperability:** While Lamport header breaks strict HTTP standard, it follows assignment’s spec.
* **Design sketch** (`DesignSketch.pdf`) provided separately with diagrams.

---

## 12. Known Constraints

* Expiry TTL is fixed at **30 seconds** (can be made configurable if needed).
* JSON parsing uses **Gson** (no bonus marks for custom parser).
* Only **GET** and **PUT** are supported; other methods return 400.

---

# ✅ Submission Checklist

* [x] Code (src + test)
* [x] Resources (sample input files)
* [x] README.md
* [x] DesignSketch.pdf
* [ ] Changes.pdf (optional)

---

👉 this README now ticks: project purpose, setup steps, how to run, dependencies, rules for Lamport tick, persistence/expiry, test coverage, and submission checklist.

---

Do you want me to also **write the text for DesignSketch.pdf** (with component + sequence diagrams described in words) so you can quickly drop it into diagrams.net or Word to export?
