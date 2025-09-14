package store;

import core.models.SourceState;
import core.models.WeatherRecord;
import util.Config;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

// Handles crash-safe persistence of SourceState data
// Each source is stored as one JSON line in the state file
public final class StateStore {
    private final Path dir, stateFile, tmpFile; // data directory, main file, temp file

    // constructor: create data directory and set file paths
    public StateStore(Path baseDir) throws IOException {
        this.dir = baseDir.resolve(Config.DATA_DIR);
        Files.createDirectories(dir);
        this.stateFile = dir.resolve(Config.STATE_FILE);
        this.tmpFile = dir.resolve(Config.STATE_FILE + ".tmp");
    }

    // save current state to disk safely using a temp file + atomic move
    public synchronized void save(Map<String, SourceState> state) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (var e : state.entrySet()) {
            // serialize each record's fields to JSON, one line per source
            sb.append(JsonUtil.toJson(e.getValue().record().getFields())).append('\n');
        }
        byte[] data = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (FileChannel ch = FileChannel.open(tmpFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ch.write(java.nio.ByteBuffer.wrap(data));
            ch.force(true); // flush to disk
        }
        // replace old state file atomically
        Files.move(tmpFile, stateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // load state from disk, rebuild SourceState/WeatherRecord
    public synchronized Map<String, SourceState> load() throws IOException {
        Path p = Files.exists(stateFile) ? stateFile : (Files.exists(tmpFile) ? tmpFile : null);
        Map<String, SourceState> out = new LinkedHashMap<>();
        if (p == null) return out; // no file exists
        for (String line : Files.readAllLines(p)) {
            String s = line.trim();
            if (s.isEmpty()) continue;
            Map<String, Object> obj = JsonUtil.parseJsonObj(s);
            WeatherRecord rec = new WeatherRecord(obj, 0); // Lamport 0 on load
            String id = rec.id();
            if (id != null && !id.isBlank()) {
                out.put(id, new SourceState(rec)); // lastSeen reset to now
            }
        }
        return out;
    }

    // getter for data directory
    public Path dir() { return dir; }
}
