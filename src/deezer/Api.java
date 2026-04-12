package deezer;

import deezer.models.Album;
import deezer.models.Artist;
import deezer.models.Track;
import org.json.simple.parser.ParseException;
import rf.ebanina.utils.formats.json.JsonProcess;
import rf.ebanina.utils.network.Request;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

public class Api {
    private final String url;
    private int max;

    public Api(String url) {
        this.url = url;
    }

    public Track getTrack(String query) throws IOException, ParseException {
        StringBuilder body = new Request(URI.create(buildUrlToGetTrack(query)).toURL()).send().getBody();

        String data = JsonProcess.getJsonItem(body.toString(), "data");
        String[] tracks = JsonProcess.getJsonArray(data);

        String trackJson = tracks[0];

        Track track = getTrackFromJson(trackJson);

        String artistJson = JsonProcess.getJsonItem(trackJson, "artist");
        track.setArtist(getArtistFromJson(artistJson));

        String albumJson = JsonProcess.getJsonItem(trackJson, "album");
        track.setAlbum(getAlbumFromJson(albumJson));

        return track;
    }

    public Track getTrackFromJson(String trackJson) throws ParseException {
        Track track = new Track();

        track.setId(Long.parseLong(JsonProcess.getJsonItem(trackJson, "id")));
        track.setReadable(Boolean.parseBoolean(JsonProcess.getJsonItem(trackJson, "readable")));
        track.setTitle(JsonProcess.getJsonItem(trackJson, "title"));
        track.setTitle_short(JsonProcess.getJsonItem(trackJson, "title_short"));
        track.setTitle_version(JsonProcess.getJsonItem(trackJson, "title_version"));
        track.setIsrc(JsonProcess.getJsonItem(trackJson, "isrc"));
        track.setLink(JsonProcess.getJsonItem(trackJson, "link"));
        track.setDuration(Integer.parseInt(JsonProcess.getJsonItem(trackJson, "duration")));
        track.setRank(Integer.parseInt(JsonProcess.getJsonItem(trackJson, "rank")));
        track.setExplicit_lyrics(Boolean.parseBoolean(JsonProcess.getJsonItem(trackJson, "explicit_lyrics")));
        track.setExplicit_content_lyrics(Integer.parseInt(JsonProcess.getJsonItem(trackJson, "explicit_content_lyrics")));
        track.setExplicit_content_cover(Integer.parseInt(JsonProcess.getJsonItem(trackJson, "explicit_content_cover")));
        track.setPreview(JsonProcess.getJsonItem(trackJson, "preview"));
        track.setMd5_image(JsonProcess.getJsonItem(trackJson, "md5_image"));
        track.setType(JsonProcess.getJsonItem(trackJson, "type"));

        return track;
    }

    public Album getAlbumFromJson(String albumJson) throws ParseException {
        if (albumJson == null || albumJson.trim().isEmpty()) {
            throw new ParseException(0, "Album JSON is null or empty");
        }

        Album album = new Album();
        album.setId(JsonProcess.getJsonItem(albumJson, "id") != null ?
                Long.parseLong(JsonProcess.getJsonItem(albumJson, "id")) : 0L);
        album.setTitle(JsonProcess.getJsonItem(albumJson, "title") != null ?
                JsonProcess.getJsonItem(albumJson, "title") : "");
        album.setCover(JsonProcess.getJsonItem(albumJson, "cover") != null ?
                JsonProcess.getJsonItem(albumJson, "cover") : "");
        album.setCover_small(JsonProcess.getJsonItem(albumJson, "cover_small") != null ?
                JsonProcess.getJsonItem(albumJson, "cover_small") : "");
        album.setCover_medium(JsonProcess.getJsonItem(albumJson, "cover_medium") != null ?
                JsonProcess.getJsonItem(albumJson, "cover_medium") : "");
        album.setCover_big(JsonProcess.getJsonItem(albumJson, "cover_big") != null ?
                JsonProcess.getJsonItem(albumJson, "cover_big") : "");
        album.setCover_xl(JsonProcess.getJsonItem(albumJson, "cover_xl") != null ?
                JsonProcess.getJsonItem(albumJson, "cover_xl") : "");
        album.setMd5_image(JsonProcess.getJsonItem(albumJson, "md5_image") != null ?
                JsonProcess.getJsonItem(albumJson, "md5_image") : "");
        album.setTracklist(JsonProcess.getJsonItem(albumJson, "tracklist") != null ?
                JsonProcess.getJsonItem(albumJson, "tracklist") : "");
        album.setType(JsonProcess.getJsonItem(albumJson, "type") != null ?
                JsonProcess.getJsonItem(albumJson, "type") : "");

        return album;
    }

    public Artist getArtist(String query) throws IOException, ParseException {
        StringBuilder body = new Request(URI.create(buildUrlToGetArtist(query)).toURL()).send().getBody();
        String artistJson = JsonProcess.getJsonArray(JsonProcess.getJsonItem(body.toString(), "data"))[0];

        return getArtistFromJson(artistJson);
    }

    public Artist getArtistFromJson(String artistJson) throws ParseException {
        if (artistJson == null || artistJson.trim().isEmpty()) {
            throw new ParseException(0, "Artist JSON is null or empty");
        }

        Artist artist = new Artist();
        artist.setId(JsonProcess.getJsonItem(artistJson, "id") != null ?
                Long.parseLong(JsonProcess.getJsonItem(artistJson, "id")) : 0L);
        artist.setName(JsonProcess.getJsonItem(artistJson, "name") != null ?
                JsonProcess.getJsonItem(artistJson, "name") : "");
        artist.setLink(JsonProcess.getJsonItem(artistJson, "link") != null ?
                JsonProcess.getJsonItem(artistJson, "link") : "");
        artist.setTracklist(JsonProcess.getJsonItem(artistJson, "tracklist") != null ?
                JsonProcess.getJsonItem(artistJson, "tracklist") : "");
        artist.setType(JsonProcess.getJsonItem(artistJson, "type") != null ?
                JsonProcess.getJsonItem(artistJson, "type") : "");
        artist.setNbAlbum(JsonProcess.getJsonItem(artistJson, "nb_album") != null ?
                Integer.parseInt(JsonProcess.getJsonItem(artistJson, "nb_album")) : 0);
        artist.setNbFan(JsonProcess.getJsonItem(artistJson, "nb_fan") != null ?
                Integer.parseInt(JsonProcess.getJsonItem(artistJson, "nb_fan")) : 0);
        artist.setRadio(JsonProcess.getJsonItem(artistJson, "radio") != null && Boolean.parseBoolean(JsonProcess.getJsonItem(artistJson, "radio")));
        artist.setPicture_small(JsonProcess.getJsonItem(artistJson, "picture_small") != null ?
                JsonProcess.getJsonItem(artistJson, "picture_small") : "");
        artist.setPicture_medium(JsonProcess.getJsonItem(artistJson, "picture_medium") != null ?
                JsonProcess.getJsonItem(artistJson, "picture_medium") : "");
        artist.setPicture_big(JsonProcess.getJsonItem(artistJson, "picture_big") != null ?
                JsonProcess.getJsonItem(artistJson, "picture_big") : "");
        artist.setPicture_xl(JsonProcess.getJsonItem(artistJson, "picture_xl") != null ?
                JsonProcess.getJsonItem(artistJson, "picture_xl") : "");
        artist.setPicture(JsonProcess.getJsonItem(artistJson, "picture") != null ?
                JsonProcess.getJsonItem(artistJson, "picture") : "");

        return artist;
    }

    protected String buildUrlToGetArtist(String query) {
        return new QueryBuilder(url)
                .path("search", "artist")
                .param("q", query)
                .param("limit", 1)
                .param("order", "RANKING")
                .param("strict", false)
                .build();
    }

    protected String buildUrlToGetTrack(String query) {
        return new QueryBuilder(url)
                .path("search", "track")
                .param("q", query)
                .param("limit", 1)
                .param("order", "RANKING")
                .param("strict", false)
                .build();
    }
}
