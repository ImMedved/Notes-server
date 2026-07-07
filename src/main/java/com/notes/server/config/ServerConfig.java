package com.notes.server.config;

public record ServerConfig(
        int port,
        String dbUrl,
        String dbUser,
        String dbPassword,
        String apiKey
) {
    public static ServerConfig fromEnvironment() {
        return new ServerConfig(
                Integer.parseInt(env("NOTES_PORT", "8080")),
                env("NOTES_DB_URL", "jdbc:postgresql://localhost:5432/notes"),
                env("NOTES_DB_USER", "notes"),
                env("NOTES_DB_PASSWORD", "notes"),
                env("NOTES_API_KEY", "")
        );
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
