package deezer.models;

import java.util.Objects;

public class Album {
    private long id;
    private String title;
    private String cover;
    private String cover_small;
    private String cover_medium;
    private String cover_big;
    private String cover_xl;
    private String md5_image;
    private String tracklist;
    private String type;

    public long getId() {
        return id;
    }

    public Album setId(long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Album setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getCover() {
        return cover;
    }

    public Album setCover(String cover) {
        this.cover = cover;
        return this;
    }

    public String getCover_small() {
        return cover_small;
    }

    public Album setCover_small(String cover_small) {
        this.cover_small = cover_small;
        return this;
    }

    public String getCover_medium() {
        return cover_medium;
    }

    public Album setCover_medium(String cover_medium) {
        this.cover_medium = cover_medium;
        return this;
    }

    public String getCover_big() {
        return cover_big;
    }

    public Album setCover_big(String cover_big) {
        this.cover_big = cover_big;
        return this;
    }

    public String getCover_xl() {
        return cover_xl;
    }

    public Album setCover_xl(String cover_xl) {
        this.cover_xl = cover_xl;
        return this;
    }

    public String getMd5_image() {
        return md5_image;
    }

    public Album setMd5_image(String md5_image) {
        this.md5_image = md5_image;
        return this;
    }

    public String getTracklist() {
        return tracklist;
    }

    public Album setTracklist(String tracklist) {
        this.tracklist = tracklist;
        return this;
    }

    public String getType() {
        return type;
    }

    public Album setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Album album = (Album) o;
        return id == album.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Album{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", cover='" + cover + '\'' +
                ", cover_small='" + cover_small + '\'' +
                ", cover_medium='" + cover_medium + '\'' +
                ", cover_big='" + cover_big + '\'' +
                ", cover_xl='" + cover_xl + '\'' +
                ", md5_image='" + md5_image + '\'' +
                ", tracklist='" + tracklist + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}