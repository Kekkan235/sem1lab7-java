package lab;

import lab.service.LibraryService;
import lab.service.MusicService;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {

  private static final String DB_URL = "jdbc:h2:mem:lab6db;DB_CLOSE_DELAY=-1";
  private static final String USER = "sa";
  private static final String PASS = "";

  public static void main(String[] args) {
    try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {

      // Задания № 1–3 (работа с таблицей study.music)
      MusicService musicService = new MusicService(conn);
      musicService.initDatabase();
      musicService.printAllMusic();
      musicService.printFilteredMusic();
      musicService.addFavoriteSong(21, "Nothing Else Matters");

      // Задания № 4–8 (работа с JSON, таблицами посетителей и книг)
      LibraryService libraryService = new LibraryService(conn);
      libraryService.loadFromJson();
      libraryService.printSortedBooks();
      libraryService.printBooksBefore2000();
      libraryService.addAndPrintMyProfile();
      libraryService.dropTables();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
