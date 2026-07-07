package com.notes.server;

import com.notes.server.config.ServerConfig;
import com.notes.server.http.ApiServer;
import com.notes.server.storage.PostgresStorage;

public class Main {
    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.fromEnvironment();
        PostgresStorage storage = new PostgresStorage(config);
        waitForDatabase(storage);

        ApiServer apiServer = new ApiServer(config, storage);
        apiServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            apiServer.stop();
            storage.close();
        }, "notes-server-shutdown"));

        System.out.println("Notes server started on port " + config.port());
    }

    private static void waitForDatabase(PostgresStorage storage) throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                storage.ensureSchema();
                return;
            } catch (Exception error) {
                lastError = error;
                Thread.sleep(2_000);
            }
        }
        throw lastError;
    }
}
