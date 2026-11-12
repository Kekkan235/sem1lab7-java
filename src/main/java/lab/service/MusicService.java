package lab.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MusicService {
    private final Connection conn;

    public MusicService(Connection conn) {
        this.conn = conn;
    }

    // Подготовка схемы и выполнение music-create.sql
    public void initDatabase() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS study;");
            try (InputStream sqlStream = getClass().getClassLoader().getResourceAsStream("music-create.sql")) {
                if (sqlStream == null) {
                    throw new RuntimeException("Файл music-create.sql не найден в src/main/resources!");
                }
                String sqlScript = new String(sqlStream.readAllBytes(), StandardCharsets.UTF_8);
                stmt.execute(sqlScript);
            }
        }
    }

    // № 1. Список музыкальных композиций
    public void printAllMusic() throws SQLException {
        System.out.println("=== № 1. Список музыкальных композиций ===");
        printQuery("SELECT id, name FROM study.music;");
    }

    // № 2. Фильтрация композиций (отсутствуют буквы 'm' и 't' без учёта регистра)
    public void printFilteredMusic() throws SQLException {
        System.out.println("\n=== № 2. Композиции без букв 'm' и 't' ===");
        String query = "SELECT id, name FROM study.music " +
                "WHERE LOWER(name) NOT LIKE '%m%' AND LOWER(name) NOT LIKE '%t%';";
        printQuery(query);
    }

    // № 3. Добавление композиции
    public void addFavoriteSong(int id, String songName) throws SQLException {
        System.out.println("\n=== № 3. Добавление композиции ===");
        String insertQuery = "INSERT INTO study.music (id, name) VALUES (?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, songName);
            pstmt.executeUpdate();
        }
        printQuery("SELECT id, name FROM study.music WHERE id = " + id + ";");
    }

    private void printQuery(String query) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                System.out.printf("%d. %s%n", rs.getInt("id"), rs.getString("name"));
            }
        }
    }
}
