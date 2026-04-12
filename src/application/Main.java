package application;

import deezer.Deezer;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        Deezer deezer = new Deezer();
        deezer.getRelatedTracks("dvrst - dream space",
                        3, 5, 5, 0, 3
                ).forEach(e -> System.out.println(e.getArtist().getName() + " - " + e.getTitle()));
    }
}
