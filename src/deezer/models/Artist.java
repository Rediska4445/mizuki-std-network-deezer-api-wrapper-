package deezer.models;

import java.util.Objects;

public class Artist {
    private long id;
    private String name;
    private String link;
    private String tracklist;
    private String type;
    private int nbAlbum;
    private int nbFan;
    private boolean radio;

    private String picture_small;
    private String picture_medium;
    private String picture_big;
    private String picture_xl;
    private String picture;

    public String getPictureUrl() {
        return picture_xl != null ? picture_xl :
                picture_big != null ? picture_big : picture_medium;
    }

    public long getId() {
        return id;
    }

    public Artist setId(long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Artist setName(String name) {
        this.name = name;
        return this;
    }

    public String getLink() {
        return link;
    }

    public Artist setLink(String link) {
        this.link = link;
        return this;
    }

    public String getTracklist() {
        return tracklist;
    }

    public Artist setTracklist(String tracklist) {
        this.tracklist = tracklist;
        return this;
    }

    public String getType() {
        return type;
    }

    public Artist setType(String type) {
        this.type = type;
        return this;
    }

    public int getNbAlbum() {
        return nbAlbum;
    }

    public Artist setNbAlbum(int nbAlbum) {
        this.nbAlbum = nbAlbum;
        return this;
    }

    public int getNbFan() {
        return nbFan;
    }

    public Artist setNbFan(int nbFan) {
        this.nbFan = nbFan;
        return this;
    }

    public boolean isRadio() {
        return radio;
    }

    public Artist setRadio(boolean radio) {
        this.radio = radio;
        return this;
    }

    public String getPicture_small() {
        return picture_small;
    }

    public Artist setPicture_small(String picture_small) {
        this.picture_small = picture_small;
        return this;
    }

    public String getPicture_medium() {
        return picture_medium;
    }

    public Artist setPicture_medium(String picture_medium) {
        this.picture_medium = picture_medium;
        return this;
    }

    public String getPicture_big() {
        return picture_big;
    }

    public Artist setPicture_big(String picture_big) {
        this.picture_big = picture_big;
        return this;
    }

    public String getPicture_xl() {
        return picture_xl;
    }

    public Artist setPicture_xl(String picture_xl) {
        this.picture_xl = picture_xl;
        return this;
    }

    public String getPicture() {
        return picture;
    }

    public Artist setPicture(String picture) {
        this.picture = picture;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artist artist = (Artist) o;
        return id == artist.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Artist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", link='" + link + '\'' +
                ", tracklist='" + tracklist + '\'' +
                ", type='" + type + '\'' +
                ", nbAlbum=" + nbAlbum +
                ", nbFan=" + nbFan +
                ", radio=" + radio +
                ", picture_small='" + picture_small + '\'' +
                ", picture_medium='" + picture_medium + '\'' +
                ", picture_big='" + picture_big + '\'' +
                ", picture_xl='" + picture_xl + '\'' +
                ", picture='" + picture + '\'' +
                '}';
    }
}