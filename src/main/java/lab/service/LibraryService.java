package lab.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lab.model.Book;
import lab.model.Visitor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class LibraryService {
    private final Connection conn;

    public LibraryService(Connection conn) {
        this.conn = conn;
    }

    // № 4. Создание таблиц и загрузка уникальных записей из books.json
    public void loadFromJson() throws Exception {
        System.out.println("\n=== № 4. Создание таблиц и загрузка из books.json ===");
        createTables();

        Gson gson = new Gson();
        Type visitorListType = new TypeToken<List<Visitor>>() {}.getType();

        List<Visitor> visitors;
        try (InputStream jsonStream = getClass().getClassLoader().getResourceAsStream("book.json")) {
            if (jsonStream == null) {
                throw new RuntimeException("Файл books.json не найден в src/main/resources!");
            }
            try (Reader reader = new InputStreamReader(jsonStream, StandardCharsets.UTF_8)) {
                visitors = gson.fromJson(reader, visitorListType);
            }
        }

        // MERGE гарантирует уникальность по ключу KEY(...)
        String mergeVisitor = "MERGE INTO study.visitors (name, surname, phone, subscribed) KEY (phone) VALUES (?, ?, ?, ?);";
        String mergeBook = "MERGE INTO study.books (isbn, title, author, publishing_year, publisher) KEY (isbn) VALUES (?, ?, ?, ?, ?);";
        String getVisitorId = "SELECT id FROM study.visitors WHERE phone = ?;";
        String mergeLink = "MERGE INTO study.visitor_books (visitor_id, book_isbn) KEY (visitor_id, book_isbn) VALUES (?, ?);";

        try (PreparedStatement psVisitor = conn.prepareStatement(mergeVisitor);
             PreparedStatement psBook = conn.prepareStatement(mergeBook);
             PreparedStatement psGetId = conn.prepareStatement(getVisitorId);
             PreparedStatement psLink = conn.prepareStatement(mergeLink)) {

            for (Visitor v : visitors) {
                psVisitor.setString(1, v.name);
                psVisitor.setString(2, v.surname);
                psVisitor.setString(3, v.phone);
                psVisitor.setBoolean(4, v.subscribed);
                psVisitor.executeUpdate();

                psGetId.setString(1, v.phone);
                int visitorId = -1;
                try (ResultSet rs = psGetId.executeQuery()) {
                    if (rs.next()) visitorId = rs.getInt("id");
                }

                if (v.favoriteBooks != null && visitorId != -1) {
                    for (Book b : v.favoriteBooks) {
                        psBook.setString(1, b.isbn);
                        psBook.setString(2, b.name);
                        psBook.setString(3, b.author);
                        psBook.setInt(4, b.publishingYear);
                        psBook.setString(5, b.publisher);
                        psBook.executeUpdate();

                        psLink.setInt(1, visitorId);
                        psLink.setString(2, b.isbn);
                        psLink.executeUpdate();
                    }
                }
            }
        }
        System.out.println("Уникальные посетители и книги загружены.");
    }

    private void createTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS study.visitors (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    surname VARCHAR(100) NOT NULL,
                    phone VARCHAR(30) UNIQUE NOT NULL,
                    subscribed BOOLEAN
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS study.books (
                    isbn VARCHAR(30) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    author VARCHAR(255) NOT NULL,
                    publishing_year INT,
                    publisher VARCHAR(255)
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS study.visitor_books (
                    visitor_id INT,
                    book_isbn VARCHAR(30),
                    PRIMARY KEY (visitor_id, book_isbn),
                    FOREIGN KEY (visitor_id) REFERENCES study.visitors(id) ON DELETE CASCADE,
                    FOREIGN KEY (book_isbn) REFERENCES study.books(isbn) ON DELETE CASCADE
                );
            """);
        }
    }

    // № 5. Сортировка книг по году издания
    public void printSortedBooks() throws SQLException {
        System.out.println("\n=== № 5. Книги, отсортированные по году издания ===");
        printBooks("SELECT isbn, title, author, publishing_year FROM study.books ORDER BY publishing_year ASC;");
    }

    // № 6. Книги, изданные до 2000 года
    public void printBooksBefore2000() throws SQLException {
        System.out.println("\n=== № 6. Книги, изданные до 2000 года ===");
        printBooks("SELECT isbn, title, author, publishing_year FROM study.books WHERE publishing_year < 2000;");
    }

    // № 7. Добавление информации о себе и любимых книг
    public void addAndPrintMyProfile() throws SQLException {
        System.out.println("\n=== № 7. Добавление информации о себе и любимых книг ===");
        String myPhone = "+7-999-000-11-22";
        String myIsbn = "978-5-17-080115-2";

        try (PreparedStatement psV = conn.prepareStatement(
                "MERGE INTO study.visitors (name, surname, phone, subscribed) KEY (phone) VALUES (?, ?, ?, ?);");
             PreparedStatement psB = conn.prepareStatement(
                     "MERGE INTO study.books (isbn, title, author, publishing_year, publisher) KEY (isbn) VALUES (?, ?, ?, ?, ?);");
             PreparedStatement psId = conn.prepareStatement(
                     "SELECT id FROM study.visitors WHERE phone = ?;");
             PreparedStatement psL = conn.prepareStatement(
                     "MERGE INTO study.visitor_books (visitor_id, book_isbn) KEY (visitor_id, book_isbn) VALUES (?, ?);")) {

            psV.setString(1, "Алексей");
            psV.setString(2, "Смирнов");
            psV.setString(3, myPhone);
            psV.setBoolean(4, true);
            psV.executeUpdate();

            psId.setString(1, myPhone);
            int myId = -1;
            try (ResultSet rs = psId.executeQuery()) {
                if (rs.next()) myId = rs.getInt("id");
            }

            psB.setString(1, myIsbn);
            psB.setString(2, "Мастер и Маргарита");
            psB.setString(3, "Михаил Булгаков");
            psB.setInt(4, 1967);
            psB.setString(5, "АСТ");
            psB.executeUpdate();

            psL.setInt(1, myId);
            psL.setString(2, myIsbn);
            psL.executeUpdate();
        }

        String selectMyData = """
            SELECT v.name, v.surname, v.phone, b.title, b.author, b.publishing_year
            FROM study.visitors v
            JOIN study.visitor_books vb ON v.id = vb.visitor_id
            JOIN study.books b ON vb.book_isbn = b.isbn
            WHERE v.phone = ?;
        """;
        try (PreparedStatement ps = conn.prepareStatement(selectMyData)) {
            ps.setString(1, myPhone);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("Посетитель: %s %s (%s) | Любимая книга: \"%s\" (%s, %d г.)%n",
                            rs.getString("name"),
                            rs.getString("surname"),
                            rs.getString("phone"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getInt("publishing_year"));
                }
            }
        }
    }

    // № 8. Удаление таблиц
    public void dropTables() throws SQLException {
        System.out.println("\n=== № 8. Удаление таблиц ===");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS study.visitor_books;");
            stmt.execute("DROP TABLE IF EXISTS study.books;");
            stmt.execute("DROP TABLE IF EXISTS study.visitors;");
        }
        System.out.println("Таблицы посетителей и книг удалены.");
    }

    private void printBooks(String query) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                System.out.printf("[%d] \"%s\" — %s (ISBN: %s)%n",
                        rs.getInt("publishing_year"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"));
            }
        }
    }
}
