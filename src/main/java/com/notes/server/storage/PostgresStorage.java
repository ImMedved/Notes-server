package com.notes.server.storage;

import com.notes.server.config.ServerConfig;
import com.notes.shared.model.Note;
import com.notes.shared.model.ServerSnapshot;
import com.notes.shared.model.TimerEntry;
import com.notes.shared.model.TimerMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostgresStorage implements AutoCloseable {
    private final ServerConfig config;

    public PostgresStorage(ServerConfig config) {
        this.config = config;
    }

    public void ensureSchema() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS app_meta (
                        meta_key TEXT PRIMARY KEY,
                        meta_value BIGINT NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO app_meta(meta_key, meta_value)
                    VALUES ('revision', 0)
                    ON CONFLICT (meta_key) DO NOTHING
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS notes (
                        id TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        pinned BOOLEAN NOT NULL,
                        archived BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                    """);
            statement.execute("""
                    ALTER TABLE notes
                    ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS timers (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        mode TEXT NOT NULL,
                        duration_millis BIGINT NOT NULL,
                        started_at BIGINT NOT NULL,
                        accumulated_millis BIGINT NOT NULL,
                        running BOOLEAN NOT NULL,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                    """);
        }
    }

    public ServerSnapshot getSnapshot() throws SQLException {
        try (Connection connection = openConnection()) {
            ServerSnapshot snapshot = new ServerSnapshot();
            snapshot.setRevision(readRevision(connection));
            snapshot.setServerTimeEpochMillis(System.currentTimeMillis());
            snapshot.setNotes(listNotes(connection));
            snapshot.setTimers(listTimers(connection));
            return snapshot;
        }
    }

    public Note getNote(String id) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, title, content, pinned, archived, created_at, updated_at
                     FROM notes
                     WHERE id = ?
                     """)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapNote(resultSet);
                }
            }
        }
        return null;
    }

    public TimerEntry getTimer(String id) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, mode, duration_millis, started_at, accumulated_millis, running, created_at, updated_at
                     FROM timers
                     WHERE id = ?
                     """)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTimer(resultSet);
                }
            }
        }
        return null;
    }

    public void upsertNote(Note note) throws SQLException {
        long now = System.currentTimeMillis();
        if (note.getId() == null || note.getId().isBlank()) {
            throw new SQLException("Note id is required.");
        }
        if (note.getCreatedAt() == 0) {
            note.setCreatedAt(now);
        }
        if (note.getUpdatedAt() == 0) {
            note.setUpdatedAt(now);
        }
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO notes (id, title, content, pinned, archived, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                      title = EXCLUDED.title,
                      content = EXCLUDED.content,
                      pinned = EXCLUDED.pinned,
                      archived = EXCLUDED.archived,
                      updated_at = EXCLUDED.updated_at
                    """)) {
                statement.setString(1, note.getId());
                statement.setString(2, blankToDefault(note.getTitle(), "Без названия"));
                statement.setString(3, blankToDefault(note.getContent(), ""));
                statement.setBoolean(4, note.isPinned());
                statement.setBoolean(5, note.isArchived());
                statement.setLong(6, note.getCreatedAt());
                statement.setLong(7, note.getUpdatedAt());
                statement.executeUpdate();
            }
            incrementRevision(connection);
            connection.commit();
        }
    }

    public void deleteNote(String id) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM notes WHERE id = ?")) {
                statement.setString(1, id);
                int affected = statement.executeUpdate();
                if (affected > 0) {
                    incrementRevision(connection);
                }
            }
            connection.commit();
        }
    }

    public void upsertTimer(TimerEntry timer) throws SQLException {
        long now = System.currentTimeMillis();
        if (timer.getId() == null || timer.getId().isBlank()) {
            throw new SQLException("Timer id is required.");
        }
        if (timer.getCreatedAt() == 0) {
            timer.setCreatedAt(now);
        }
        if (timer.getUpdatedAt() == 0) {
            timer.setUpdatedAt(now);
        }
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO timers (id, name, mode, duration_millis, started_at, accumulated_millis, running, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                      name = EXCLUDED.name,
                      mode = EXCLUDED.mode,
                      duration_millis = EXCLUDED.duration_millis,
                      started_at = EXCLUDED.started_at,
                      accumulated_millis = EXCLUDED.accumulated_millis,
                      running = EXCLUDED.running,
                      updated_at = EXCLUDED.updated_at
                    """)) {
                statement.setString(1, timer.getId());
                statement.setString(2, blankToDefault(timer.getName(), defaultTimerName(timer.getMode())));
                statement.setString(3, timer.getMode().name());
                statement.setLong(4, timer.getDurationMillis());
                statement.setLong(5, timer.getStartedAt());
                statement.setLong(6, timer.getAccumulatedMillis());
                statement.setBoolean(7, timer.isRunning());
                statement.setLong(8, timer.getCreatedAt());
                statement.setLong(9, timer.getUpdatedAt());
                statement.executeUpdate();
            }
            incrementRevision(connection);
            connection.commit();
        }
    }

    public void deleteTimer(String id) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM timers WHERE id = ?")) {
                statement.setString(1, id);
                int affected = statement.executeUpdate();
                if (affected > 0) {
                    incrementRevision(connection);
                }
            }
            connection.commit();
        }
    }

    private List<Note> listNotes(Connection connection) throws SQLException {
        List<Note> notes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, title, content, pinned, archived, created_at, updated_at
                FROM notes
                ORDER BY archived ASC, pinned DESC, updated_at DESC
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                notes.add(mapNote(resultSet));
            }
        }
        notes.sort(Comparator.comparing(Note::isArchived)
                .thenComparing(Note::isPinned, Comparator.reverseOrder())
                .thenComparing(Note::getUpdatedAt, Comparator.reverseOrder()));
        return notes;
    }

    private List<TimerEntry> listTimers(Connection connection) throws SQLException {
        List<TimerEntry> timers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, name, mode, duration_millis, started_at, accumulated_millis, running, created_at, updated_at
                FROM timers
                ORDER BY updated_at DESC
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                timers.add(mapTimer(resultSet));
            }
        }
        timers.sort(Comparator.comparing(TimerEntry::getUpdatedAt).reversed());
        return timers;
    }

    private long readRevision(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT meta_value
                FROM app_meta
                WHERE meta_key = 'revision'
                """);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
        }
        return 0L;
    }

    private void incrementRevision(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE app_meta
                SET meta_value = meta_value + 1
                WHERE meta_key = 'revision'
                """)) {
            statement.executeUpdate();
        }
    }

    private Note mapNote(ResultSet resultSet) throws SQLException {
        Note note = new Note();
        note.setId(resultSet.getString("id"));
        note.setTitle(resultSet.getString("title"));
        note.setContent(resultSet.getString("content"));
        note.setPinned(resultSet.getBoolean("pinned"));
        note.setArchived(resultSet.getBoolean("archived"));
        note.setCreatedAt(resultSet.getLong("created_at"));
        note.setUpdatedAt(resultSet.getLong("updated_at"));
        return note;
    }

    private TimerEntry mapTimer(ResultSet resultSet) throws SQLException {
        TimerEntry timer = new TimerEntry();
        timer.setId(resultSet.getString("id"));
        timer.setName(resultSet.getString("name"));
        timer.setMode(TimerMode.valueOf(resultSet.getString("mode")));
        timer.setDurationMillis(resultSet.getLong("duration_millis"));
        timer.setStartedAt(resultSet.getLong("started_at"));
        timer.setAccumulatedMillis(resultSet.getLong("accumulated_millis"));
        timer.setRunning(resultSet.getBoolean("running"));
        timer.setCreatedAt(resultSet.getLong("created_at"));
        timer.setUpdatedAt(resultSet.getLong("updated_at"));
        return timer;
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String defaultTimerName(TimerMode mode) {
        return mode == TimerMode.STOPWATCH ? "Секундомер" : "Таймер";
    }

    @Override
    public void close() {
        // The implementation opens short-lived connections, so nothing is held here.
    }
}
