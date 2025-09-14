package store;

import core.models.SourceState;
import core.models.WeatherRecord;
import util.Config;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Crash-safe persistence without external libs.
 * File format: one JSON object *line per source*, each line is exactly the
 * JSON produced by JsonUtil.toJson(record.fields). On load, we parse each line
 * back to a Map and rebuild SourceState/WeatherRecord.
 */
public final class StateStore {
    private final Path dir, stateFile, tmpFile;

    public StateStore(Path baseDir) throws IOException {
        this.dir = baseDir.resolve(Config.DATA_DIR);
        Files.createDirectories(dir);
        this.stateFile = dir.resolve(Config.STATE_FILE);
        this.tmpFile = dir.resolve(Config.STATE_FILE + ".tmp");
    }

    public synchronized void save(Map<String, SourceState> state) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (var e : state.entrySet()) {
            // Each line is JSON of the record fields
            sb.append(JsonUtil.toJson(e.getValue().record().getFields())).append('\n');
        }
        byte[] data = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (FileChannel ch = FileChannel.open(tmpFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ch.write(java.nio.ByteBuffer.wrap(data));
            ch.force(true);
        }
        Files.move(tmpFile, stateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public synchronized Map<String, SourceState> load() throws IOException {
        Path p = Files.exists(stateFile) ? stateFile : (Files.exists(tmpFile) ? tmpFile : null);
        Map<String, SourceState> out = new LinkedHashMap<>();
        if (p == null) return out;
        for (String line : Files.readAllLines(p)) {
            String s = line.trim();
            if (s.isEmpty()) continue;
            Map<String, Object> obj = JsonUtil.parseJsonObj(s);
            WeatherRecord rec = new WeatherRecord(obj, 0);
            String id = rec.id();
            if (id != null && !id.isBlank()) {
                out.put(id, new SourceState(rec)); // lastSeen reset to now on restart
            }
        }
        return out;
    }

    public Path dir() { return dir; }
}
