package deezer;

import deezer.models.Artist;
import deezer.models.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DeezerTest {
    private Deezer deezerService;

    @BeforeEach
    void setUp() {
        this.deezerService = new Deezer();
    }

    @Test
    void testGetChartTracks() {
        assertTimeout(Duration.ofSeconds(10), () -> {
            List<Track> result = deezerService.getChartTracks();

            assertNotNull(result, "API вернуло null вместо списка");
            assertFalse(result.isEmpty(), "Список треков пуст, чарты не загрузились");

            System.out.println("Успешно загружено треков: " + result.size());
        });
    }

    @Test
    void testGetTopTracksByArtist() {
        assertTimeout(Duration.ofSeconds(15), () -> {
            String artistName = "Eminem";

            List<Track> result = deezerService.getTopTracksByArtist(artistName);

            assertNotNull(result, "Список треков артиста null");
            assertFalse(result.isEmpty(), "Не удалось найти треки для артиста: " + artistName);

            String actualArtistName = result.get(0).getArtist().getName();
            assertNotNull(actualArtistName, "Имя артиста в треке не заполнено");

            System.out.println("Топ трек " + artistName + ": " + result.get(0).getTitle());
        });
    }

    @Test
    void testSearchTracks() {
        assertTimeout(Duration.ofSeconds(15), () -> {
            String query = "Linkin Park";
            int limit = 5;

            List<Track> result = deezerService.searchTracks(query, limit);

            assertNotNull(result, "Поиск вернул null");
            assertFalse(result.isEmpty(), "Ничего не найдено по запросу: " + query);
            assertTrue(result.size() <= limit, "Результатов больше, чем запрашивали");

            String firstTrackTitle = result.get(0).getTitle().toLowerCase();
            String firstArtistName = result.get(0).getArtist().getName().toLowerCase();

            boolean foundMatch = firstTrackTitle.contains("linkin") || firstArtistName.contains("linkin");
            assertTrue(foundMatch, "Первый результат поиска не похож на запрос. Найдено: " + firstArtistName + " - " + firstTrackTitle);

            assertNotNull(result.get(0).getAlbum().getTitle(), "Название альбома не распарсилось");

            System.out.println("Поиск по '" + query + "': Найдено " + result.get(0).getArtist().getName());
        });
    }

    @Test
    void testGetRelatedTracksByQuery() {
        assertTimeout(Duration.ofSeconds(30), () -> {
            String searchQuery = "Blinding Lights";

            List<Track> result = deezerService.getRelatedTracks(searchQuery);

            assertNotNull(result, "Результат не должен быть null");
            assertFalse(result.isEmpty(), "Метод ничего не нашел по запросу: " + searchQuery);

            long artistCount = result.stream()
                    .map(t -> t.getArtist().getName())
                    .distinct()
                    .count();

            assertTrue(artistCount > 1, "Список должен содержать разных артистов, а не только одного");

            System.out.println("По запросу '" + searchQuery + "' найдено " + result.size() + " похожих треков");
            if (!result.isEmpty()) {
                System.out.println("Пример похожего трека: " + result.get(0).getArtist().getName() + " - " + result.get(0).getTitle());
            }
        });
    }

    @Test
    void testGetRelatedArtist() {
        assertTimeout(Duration.ofSeconds(15), () -> {
            String artistName = "Radiohead";
            int expectedLimit = 25;

            List<Artist> result = deezerService.getRelatedArtist(artistName);

            assertNotNull(result, "Список похожих артистов null");
            assertFalse(result.isEmpty(), "Не удалось найти похожих артистов для: " + artistName);

            assertTrue(result.size() <= expectedLimit, "Список превысил лимит в " + expectedLimit);

            assertNotNull(result.get(0).getName(), "Имя первого похожего артиста не распарсилось");

            System.out.println("Похожие на " + artistName + ": " + result.size() + " артистов.");
            System.out.println("Первый в списке: " + result.get(0).getName());
        });
    }
}
