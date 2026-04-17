package main.java.deezer.models;

import java.util.Objects;

public class Track {
    private long id;
    private boolean readable;
    private String title;
    private String title_short;
    private String title_version;
    private String isrc;
    private String link;
    private int duration;
    private int rank;
    private boolean explicit_lyrics;
    private int explicit_content_lyrics;
    private int explicit_content_cover;
    private String preview;
    private String md5_image;
    private String type;

    private Artist artist;

    private Album album;

    public Album getAlbum() {
        return album;
    }

    public Track setAlbum(Album album) {
        this.album = album;
        return this;
    }

    public long getId() {
        return id;
    }

    public Track setId(long id) {
        this.id = id;
        return this;
    }

    public boolean isReadable() {
        return readable;
    }

    public Track setReadable(boolean readable) {
        this.readable = readable;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Track setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getTitle_short() {
        return title_short;
    }

    public Track setTitle_short(String title_short) {
        this.title_short = title_short;
        return this;
    }

    public String getTitle_version() {
        return title_version;
    }

    public Track setTitle_version(String title_version) {
        this.title_version = title_version;
        return this;
    }

    public String getIsrc() {
        return isrc;
    }

    public Track setIsrc(String isrc) {
        this.isrc = isrc;
        return this;
    }

    public String getLink() {
        return link;
    }

    public Track setLink(String link) {
        this.link = link;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public Track setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public int getRank() {
        return rank;
    }

    public Track setRank(int rank) {
        this.rank = rank;
        return this;
    }

    public boolean isExplicit_lyrics() {
        return explicit_lyrics;
    }

    public Track setExplicit_lyrics(boolean explicit_lyrics) {
        this.explicit_lyrics = explicit_lyrics;
        return this;
    }

    public int getExplicit_content_lyrics() {
        return explicit_content_lyrics;
    }

    public Track setExplicit_content_lyrics(int explicit_content_lyrics) {
        this.explicit_content_lyrics = explicit_content_lyrics;
        return this;
    }

    public int getExplicit_content_cover() {
        return explicit_content_cover;
    }

    public Track setExplicit_content_cover(int explicit_content_cover) {
        this.explicit_content_cover = explicit_content_cover;
        return this;
    }

    public String getPreview() {
        return preview;
    }

    public Track setPreview(String preview) {
        this.preview = preview;
        return this;
    }

    public String getMd5_image() {
        return md5_image;
    }

    public Track setMd5_image(String md5_image) {
        this.md5_image = md5_image;
        return this;
    }

    public String getType() {
        return type;
    }

    public Track setType(String type) {
        this.type = type;
        return this;
    }

    public Artist getArtist() {
        return artist;
    }

    public Track setArtist(Artist artist) {
        this.artist = artist;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Track track = (Track) o;

        return Objects.equals(title, track.title) && Objects.equals(artist.getId(), track.artist.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist.getId());
    }

    @Override
    public String toString() {
        return "Track{" +
                "name='" + title + '\'' +
                ", author='" + (artist != null ? artist.getName() : "null") + '\'' +
                '}';
    }
}
