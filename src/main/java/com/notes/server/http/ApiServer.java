package com.notes.server.http;

import com.notes.server.config.ServerConfig;
import com.notes.server.storage.PostgresStorage;
import com.notes.shared.contract.ContractMapper;
import com.notes.shared.json.JsonUtil;
import com.notes.shared.model.Note;
import com.notes.shared.model.ServerSnapshot;
import com.notes.shared.model.TimerEntry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiServer {
    private final ServerConfig config;
    private final PostgresStorage storage;
    private final HttpServer server;

    public ApiServer(ServerConfig config, PostgresStorage storage) throws IOException {
        this.config = config;
        this.storage = storage;
        this.server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        registerContexts();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void registerContexts() {
        server.createContext("/health", this::handleHealth);
        server.createContext("/api/v1/snapshot", this::handleSnapshot);
        server.createContext("/api/v1/notes/", this::handleNotes);
        server.createContext("/api/v1/timers/", this::handleTimers);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!handleCors(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, ContractMapper.error("method_not_allowed", "Use GET."));
            return;
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("service", "notes-server");
        response.put("serverTimeEpochMillis", System.currentTimeMillis());
        writeJson(exchange, 200, response);
    }

    private void handleSnapshot(HttpExchange exchange) throws IOException {
        if (!handleCors(exchange) || !authorize(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, ContractMapper.error("method_not_allowed", "Use GET."));
            return;
        }
        try {
            ServerSnapshot snapshot = storage.getSnapshot();
            writeJson(exchange, 200, ContractMapper.snapshotToMap(snapshot));
        } catch (SQLException exception) {
            writeJson(exchange, 500, ContractMapper.error("storage_error", exception.getMessage()));
        }
    }

    private void handleNotes(HttpExchange exchange) throws IOException {
        if (!handleCors(exchange) || !authorize(exchange)) {
            return;
        }
        String noteId = extractEntityId(exchange, "/api/v1/notes/");
        if (noteId.isBlank()) {
            writeJson(exchange, 400, ContractMapper.error("missing_note_id", "Note id is required in the path."));
            return;
        }
        try {
            if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                Note note = ContractMapper.noteFromMap(JsonUtil.asObject(JsonUtil.parse(readBody(exchange))));
                note.setId(noteId);
                storage.upsertNote(note);
                writeJson(exchange, 200, ContractMapper.noteToMap(storage.getNote(noteId)));
                return;
            }
            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                storage.deleteNote(noteId);
                writeJson(exchange, 200, Map.of("ok", true, "id", noteId));
                return;
            }
            writeJson(exchange, 405, ContractMapper.error("method_not_allowed", "Use PUT or DELETE."));
        } catch (SQLException exception) {
            writeJson(exchange, 500, ContractMapper.error("storage_error", exception.getMessage()));
        }
    }

    private void handleTimers(HttpExchange exchange) throws IOException {
        if (!handleCors(exchange) || !authorize(exchange)) {
            return;
        }
        String timerId = extractEntityId(exchange, "/api/v1/timers/");
        if (timerId.isBlank()) {
            writeJson(exchange, 400, ContractMapper.error("missing_timer_id", "Timer id is required in the path."));
            return;
        }
        try {
            if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                TimerEntry timer = ContractMapper.timerFromMap(JsonUtil.asObject(JsonUtil.parse(readBody(exchange))));
                timer.setId(timerId);
                storage.upsertTimer(timer);
                writeJson(exchange, 200, ContractMapper.timerToMap(storage.getTimer(timerId)));
                return;
            }
            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                storage.deleteTimer(timerId);
                writeJson(exchange, 200, Map.of("ok", true, "id", timerId));
                return;
            }
            writeJson(exchange, 405, ContractMapper.error("method_not_allowed", "Use PUT or DELETE."));
        } catch (SQLException exception) {
            writeJson(exchange, 500, ContractMapper.error("storage_error", exception.getMessage()));
        }
    }

    private boolean authorize(HttpExchange exchange) throws IOException {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            return true;
        }
        String provided = exchange.getRequestHeaders().getFirst("X-Notes-Api-Key");
        if (config.apiKey().equals(provided)) {
            return true;
        }
        writeJson(exchange, 401, ContractMapper.error("unauthorized", "X-Notes-Api-Key is invalid or missing."));
        return false;
    }

    private boolean handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "Content-Type, X-Notes-Api-Key, X-Client-Id, X-Client-Platform, X-Client-Version");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, PUT, DELETE, OPTIONS");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return false;
        }
        return true;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractEntityId(HttpExchange exchange, String prefix) {
        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith(prefix)) {
            return "";
        }
        return path.substring(prefix.length()).trim();
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] content = JsonUtil.stringify(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, content.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(content);
        }
    }
}
