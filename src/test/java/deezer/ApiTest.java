package deezer;

import deezer.models.Artist;
import deezer.models.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class ApiTest {
    private Api api;

    @BeforeEach
    void setUp() {
        api = new Deezer().getApi();
    }

    @Test
    void testGetTrack() {
        assertTimeout(Duration.ofSeconds(10), () -> {
            Track res = api.getTrack("Imagine Dragons - Believer");

            assertNotNull(res, "Трек равен null");
            assertEquals(res.getTitle(), "Believer");
        });
    }

    @Test
    void testGetAlbumByTrack() {
        assertTimeout(Duration.ofSeconds(10), () -> {
            Track res = api.getTrack("Imagine Dragons - Believer");

            assertNotNull(res.getAlbum(), "Трек равен null");

            assertNotNull(res.getAlbum().getTitle());
        });
    }

    @Test
    void testGetArtist() {
        assertTimeout(Duration.ofSeconds(10), () -> {
            Artist res = api.getArtist("Imagine Dragons");

            assertNotNull(res, "Артист равен null");
            assertEquals(res.getName(), "Imagine Dragons");
        });
    }
}
