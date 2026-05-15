package top.xihale.xdocs.dao;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import top.xihale.xdocs.util.ConnectionPool;
import top.xihale.xdocs.util.Db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for DAO integration tests using H2 in-memory database.
 * Ensures the test schema is created once, and cleans tables between tests.
 */
public class BaseDaoTest {

    private static final AtomicBoolean SCHEMA_CREATED = new AtomicBoolean(false);

    @BeforeAll
    static void setUpSchema() {
        if (!SCHEMA_CREATED.compareAndSet(false, true)) {
            return;
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection()) {
            String sql = loadResource("schema-h2.sql");
            List<String> statements = splitStatements(sql);
            try (Statement stmt = conn.createStatement()) {
                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                    try {
                        stmt.execute(trimmed);
                    } catch (Exception e) {
                        System.err.println("Failed to execute: " + trimmed.substring(0, Math.min(80, trimmed.length())));
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test database schema", e);
        }
    }

    @BeforeEach
    void cleanTables() {
        String[] tables = {
            "notification", "recent_visit", "favorite", "follow_user",
            "comment", "article_like", "article_chat_message", "article",
            "knowledge_base_member", "knowledge_base", "team_member", "team",
            "upload_file", "sys_user"
        };
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                try {
                    stmt.execute("DELETE FROM " + table);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clean test tables", e);
        }
    }

    @AfterAll
    static void tearDownSchema() {
        // H2 in-memory DB lives as long as the JVM (DB_CLOSE_DELAY=-1).
        // We leave it alive for the duration of the test run.
    }

    private static String loadResource(String name) {
        try (InputStream in = BaseDaoTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) throw new RuntimeException("Resource not found: " + name);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + name, e);
        }
    }

    private static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'' && !inString) {
                inString = true;
                current.append(c);
            } else if (c == '\'' && inString) {
                inString = false;
                current.append(c);
            } else if (c == ';' && !inString) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty() && !stmt.startsWith("--")) {
                    statements.add(stmt);
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        String remainder = current.toString().trim();
        if (!remainder.isEmpty()) {
            statements.add(remainder);
        }
        return statements;
    }
}
