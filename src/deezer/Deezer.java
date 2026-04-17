package deezer;

import deezer.models.Artist;
import deezer.models.Track;
import org.json.simple.parser.ParseException;
import rf.ebanina.utils.formats.json.JsonProcess;
import rf.ebanina.utils.network.Request;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Deezer {
    protected String apiUrl = "https://api.deezer.com/";
    private Api api = new Api(apiUrl);

    public Deezer() {}

    public Deezer(Api api) {
        this.api = api;
    }

    public Deezer(String apiUrl, Api api) {
        this.apiUrl = apiUrl;
        this.api = api;
    }

    public List<Track> getChartTracks() throws IOException, ParseException {
        return getChartTracks("0", 50);
    }

    public List<Track> getChartTracks(
            int limit
    ) throws IOException, ParseException {
        return getChartTracks("0", limit);
    }

    public List<Track> getChartTracks(
            String region,
            int limit
    ) throws IOException, ParseException {
        StringBuilder body = new Request(URI.create(
                new QueryBuilder(apiUrl)
                        .path("chart", region, "tracks")
                        .param("limit", limit)
                        .build()
        ).toURL()).send().getBody();

        String dataObj = JsonProcess.getJsonItem(body.toString(), "data");
        String[] dataArray = JsonProcess.getJsonArray(dataObj);

        List<Track> tracksList = new ArrayList<>();
        for(String jsonTrack : dataArray) {
            Track track = api.getTrackFromJson(jsonTrack);

            String artistJson = JsonProcess.getJsonItem(jsonTrack, "artist");
            track.setArtist(api.getArtistFromJson(artistJson));

            String albumJson = JsonProcess.getJsonItem(jsonTrack, "album");
            track.setAlbum(api.getAlbumFromJson(albumJson));

            tracksList.add(track);
        }

        return tracksList;
    }

    public List<Track> getTopTracksByArtist(
            String artist
    ) throws IOException, ParseException {
        return getTopTracksByArtist(artist, 25);
    }

    public List<Track> getTopTracksByArtist(
            String artist,
            int limit
    ) throws IOException, ParseException {
        Artist artist1 = api.getArtist(artist);

        StringBuilder body = new Request(URI.create(
                new QueryBuilder(apiUrl)
                        .path("artist")
                        .path(artist1.getId())
                        .path("top")
                        .param("limit", limit)
                        .build()
        ).toURL()).send().getBody();

        String dataObj = JsonProcess.getJsonItem(body.toString(), "data");
        String[] dataArray = JsonProcess.getJsonArray(dataObj);

        List<Track> tracksList = new ArrayList<>();

        for(String jsonTrack : dataArray) {
            Track track = api.getTrackFromJson(jsonTrack);

            String artistJson = JsonProcess.getJsonItem(jsonTrack, "artist");
            track.setArtist(api.getArtistFromJson(artistJson));

            String albumJson = JsonProcess.getJsonItem(jsonTrack, "album");
            track.setAlbum(api.getAlbumFromJson(albumJson));

            tracksList.add(track);
        }

        return tracksList;
    }

    public List<Track> searchTracks(
            String query,
            int limit
    )
            throws IOException, ParseException
    {
        StringBuilder body = new Request(URI.create(
                new QueryBuilder(apiUrl)
                        .path("search", "track")
                        .param("q", query)
                        .param("limit", limit)
                        .build()
        ).toURL()).send().getBody();

        System.out.println(body);

        String dataObj = JsonProcess.getJsonItem(body.toString(), "data");
        String[] dataArray = JsonProcess.getJsonArray(dataObj);

        List<Track> tracksList = new ArrayList<>();

        for(String jsonTrack : dataArray) {
            Track track = api.getTrackFromJson(jsonTrack);

            String artistJson = JsonProcess.getJsonItem(jsonTrack, "artist");
            track.setArtist(api.getArtistFromJson(artistJson));

            String albumJson = JsonProcess.getJsonItem(jsonTrack, "album");
            track.setAlbum(api.getAlbumFromJson(albumJson));

            tracksList.add(track);
        }

        return tracksList;
    }

    public List<Track> getRelatedTracks(
            String track
    )
            throws IOException, ParseException
    {
        List<Track> tracks = new ArrayList<>();

        Track track1 = api.getTrack(track);
        tracks.addAll(getTopTracksByArtist(track1.getArtist().getName(), 5));

        List<Artist> relatedArtist = getRelatedArtist(track1.getArtist().getName());
        for(Artist artist : relatedArtist) {
            tracks.addAll(getTopTracksByArtist(artist.getName(), 2));
        }

        tracks.addAll(searchTracks(track, 5));

        return tracks.stream().distinct().collect(Collectors.toList());
    }

    public List<Track> getRelatedTracks(
            String track,
            int topTrackByArtistLimit,
            int anotherTopTrackOfArtistLimit,
            int searchLimit,
            int chartLimit,
            int depth
    ) throws Exception {
        List<Track> tracks = new ArrayList<>();

        Track track1 = api.getTrack(track);

        if(topTrackByArtistLimit > 0) {
            tracks.addAll(getTopTracksByArtist(track1.getArtist().getName(), topTrackByArtistLimit));
        }

        List<Artist> relatedArtist = null;

        if(anotherTopTrackOfArtistLimit > 0) {
            relatedArtist = getRelatedArtist(track1.getArtist().getName(), topTrackByArtistLimit);

            for (Artist artist : relatedArtist) {
                tracks.addAll(getTopTracksByArtist(artist.getName(), anotherTopTrackOfArtistLimit));
            }
        }

        if(searchLimit > 0) {
            tracks.addAll(searchTracks(track1.getArtist().getName(), searchLimit));

            if(relatedArtist != null) {
                for(Artist artist : relatedArtist) {
                    tracks.addAll(searchTracks(artist.getName(), searchLimit));
                }
            }
        }

        if(chartLimit > 0) {
            tracks.addAll(getChartTracks(chartLimit));
        }

        if(depth > 0) {
            tracks.addAll(getArtistChain(track1.getArtist().getName(), depth, topTrackByArtistLimit));
        }

        return tracks.stream().distinct().collect(Collectors.toList());
    }

    public List<Artist> getRelatedArtist(
            String query
    ) throws IOException, ParseException {
        return getRelatedArtist(query, 25);
    }

    public List<Artist> getRelatedArtist(
            String query,
            int limit
    ) throws IOException, ParseException {
        Artist artist = api.getArtist(query);

        StringBuilder body = new Request(URI.create(
                new QueryBuilder(apiUrl)
                        .path("artist")
                        .path(artist.getId())
                        .path("related")
                        .param("limit", limit)
                        .build()
        ).toURL()).send().getBody();

        String dataObj = JsonProcess.getJsonItem(body.toString(), "data");
        String[] dataArray = JsonProcess.getJsonArray(dataObj);

        List<Artist> artistsList = new ArrayList<>();

        for(String jsonArtist : dataArray) {
            Artist relatedArtist = api.getArtistFromJson(jsonArtist);
            artistsList.add(relatedArtist);
        }

        return artistsList;
    }

    public List<Track> getArtistChain(String artistName, int depth, int tracksPerArtist) throws Exception {
        return getArtistChainRecursive(artistName, depth, 1, tracksPerArtist);
    }

    private List<Track> getArtistChainRecursive(
            String artistName,
            int maxDepth,
            int currentDepth,
            int tracksPerArtist
    ) throws Exception {

        List<Track> chainTracks = new ArrayList<>(getTopTracksByArtist(artistName, tracksPerArtist));

        if (currentDepth >= maxDepth) {
            return chainTracks;
        }

        List<Artist> relatedArtists = getRelatedArtist(artistName, 3);

        for (Artist related : relatedArtists) {
            chainTracks.addAll(getArtistChainRecursive(related.getName(), maxDepth, currentDepth + 1, tracksPerArtist));
        }

        return chainTracks;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public Api getApi() {
        return api;
    }
}
