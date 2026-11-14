package com.parkomatic.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Opens the database. Reads optional settings from "db.properties" in the working folder:
 *   db.url=jdbc:sqlite:parkomatic.db        (default)
 *   db.user=...   db.password=...           (needed for MySQL, not for SQLite)
 * The matching JDBC driver jar must be on the classpath.
 */
public final class Database {
    public static final String DEFAULT_URL = "jdbc:sqlite:parkomatic.db";

    private Database() { }

    public static Repository open() {
        Properties props = new Properties();
        File file = new File("db.properties");
        if (file.isFile()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                throw new DataAccessException("Could not read db.properties: " + e.getMessage(), e);
            }
        }
        return new JdbcRepository(
                props.getProperty("db.url", DEFAULT_URL),
                props.getProperty("db.user"),
                props.getProperty("db.password"));
    }
}
