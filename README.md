# Deezer API Parser Module

Deezer API parsing module for the Mizuka music streaming project. Provides track discovery, artist relations, and chart data retrieval.

## Features

- Chart tracks by region
- Artist top tracks
- Track search functionality
- Related tracks with configurable parameters
- Related artists discovery
- Artist chain traversal
- Complete Track/Artist/Album models

## Quick Start

```java
Deezer deezer = new Deezer();

// Get chart tracks
List<Track> charts = deezer.getChartTracks("0", 50);

// Search tracks
List<Track> results = deezer.searchTracks("Imagine Dragons", 25);

// Artist top tracks
List<Track> topTracks = deezer.getTopTracksByArtist("The Weeknd", 10);
```

## API Methods

### Chart Tracks
```java
List<Track> tracks = deezer.getChartTracks();           // Default: region 0, limit 50
List<Track> tracks = deezer.getChartTracks(100);        // Custom limit
List<Track> tracks = deezer.getChartTracks("1", 25);    // Region + limit
```

### Artist Tracks
```java
List<Track> top = deezer.getTopTracksByArtist("Drake");           // Default limit 25
List<Track> top = deezer.getTopTracksByArtist("Drake", 40);       // Custom limit
```

### Search
```java
List<Track> found = deezer.searchTracks("Bohemian Rhapsody", 20);
```

### Related Content
```java
// Simple related tracks
List<Track> related = deezer.getRelatedTracks("Blinding Lights");

// Advanced related with full control
List<Track> advanced = deezer.getRelatedTracks(
    "Shape of You",     // seed track
    5,                  // main artist top tracks
    3,                  // related artists top tracks  
    10,                 // search limit
    20,                 // chart tracks
    2                   // artist chain depth
);
```

### Artists
```java
List<Artist> similar = deezer.getRelatedArtist("Taylor Swift");
List<Artist> similar = deezer.getRelatedArtist("Taylor Swift", 15);
```

### Artist Chains
```java
List<Track> discovery = deezer.getArtistChain("Post Malone", 3, 4);
// Explores 3 levels of related artists, 4 tracks each
```

## Data Models

**Track**: `id`, `title`, `title_short`, `duration`, `preview`, `explicit_lyrics`, `artist`, `album`

**Artist**: `id`, `name`, `nb_fan`, `nb_album`, `picture_*`, `tracklist`, `radio`

**Album**: `id`, `title`, `cover_*` (small/medium/big/xl), `tracklist`

## Demo Application
Launches test window demonstrating all API endpoints with live data.

## Dependencies
* rf.ebanina.utils.network.Request
* rf.ebanina.utils.formats.json.JsonProcess
* deezer.models.Track/Artist/Album

## Mizuka Integration

```java
public class Deezer
        implements ISimilar {
    protected main.java.deezer.Deezer deezer = new main.java.deezer.Deezer();

    @Override
    public void updateSimilar(Track track) {
        // Logic into mizuka
    }

    @Override
    public List<Track> getSimilar(String f) {
        return null;
    }
}

```

## Build Notes

- Maven/Gradle compatible
- Self-contained JAR with test UI
- No external authentication required
- Uses public Deezer API endpoints

---

**Author:** Mizuka (Ebanina) Std.  
**Created:** 21.04.2026.  
**Deezer API via:** [deezer.com](https://developers.deezer.com/api).

---
