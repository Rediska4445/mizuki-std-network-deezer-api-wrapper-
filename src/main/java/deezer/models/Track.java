package deezer.models;

import java.util.Objects;

/**
 * <h1>Track</h1>
 * Полная модель Deezer трека: 15 полей API + Artist + Album ссылки.
 * <p>
 * <b>Архитектура:</b> Builder-like fluent setters (return {@code this}).
 * Используется {@link deezer.Api} мапперами + {@link deezer.Deezer} фасадом.
 * </p>
 *
 * <h2>Deezer поля (15):</h2>
 * <table>
 *   <tr><th>Группа</th><th>Поля</th></tr>
 *   <tr><td>ID/Доступ</td><td>{@code id}, {@code readable}</td></tr>
 *   <tr><td>Названия</td><td>{@code title}, {@code title_short}, {@code title_version}</td></tr>
 *   <tr><td>Метаданные</td><td>{@code isrc}, {@code link}, {@code duration}, {@code rank}</td></tr>
 *   <tr><td>Explicit</td><td>{@code explicit_lyrics}, {@code explicit_content_* (3 поля)}</td></tr>
 *   <tr><td>Медиа</td><td>{@code preview} (30сек), {@code md5_image}</td></tr>
 *   <tr><td>Тип</td><td>{@code type}</td></tr>
 * </table>
 *
 * <h2>Ссылки:</h2>
 * <ul>
 *   <li>{@link #artist}: полный Artist (id, name, picture, nb_fan)</li>
 *   <li>{@link #album}: полный Album (cover_*, tracklist)</li>
 * </ul>
 *
 * <h2>Fluent setters</h2>
 * <p>Все setters возвращают {@code this} для цепочки:
 * <pre>{@code
 * Track track = new Track()
 *     .setTitle("Bohemian Rhapsody")
 *     .setArtist(queen)
 *     .setAlbum(nightOpera)
 *     .setPreview("https://e-cdn...mp3");
 * }</pre>
 * </p>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * // Из API
 * Track track = api.getTrack("Bohemian Rhapsody");
 *
 * // Fluent создание
 * Track custom = new Track().setTitle("My Song").setArtist(myArtist);
 *
 * // UI отображение
 * playlist.add(track);
 * ui.showTrack(track.getTitle(), track.getArtist().getName());
 *
 * // Distinct в рекомендациях
 * List<Track> unique = tracks.stream().distinct().toList();
 * }</pre>
 *
 * <h2>equals/hashCode/toString</h2>
 * <table>
 *   <tr><th>Метод</th><th>Логика</th></tr>
 *   <tr><td>{@link #equals}</td><td>{@code title + artist.id} совпадение</td></tr>
 *   <tr><td>{@link #hashCode}</td><td>{@code Objects.hash(title, artist.id)}</td></tr>
 *   <tr><td>{@link #toString}</td><td>{@code "Track{name='...', author='...'}"}</td></tr>
 * </table>
 * <p><b>distinct() friendly:</b> для {@link deezer.Deezer#getRelatedTracks(String, int, int, int, int, int)} ()} дедупликации.</p>
 *
 * <h2>Thread-safety</h2>
 * <p><b>Не thread-safe:</b> mutable поля, нет volatile/synchronized.
 * Используйте defensively в multi-thread.
 * </p>
 *
 * <h2>Null-safety</h2>
 * <ul>
 *   <li>{@link #artist}, {@link #album} могут быть null (до обогащения)</li>
 *   <li>{@link #equals} проверяет {@code artist != null}</li>
 *   <li>toString safe: {@code artist != null ? name : "null"}</li>
 * </ul>
 *
 * @see Artist связанный исполнитель
 * @see Album альбом трека
 * @see deezer.Api#getTrackFromJson(String) () маппер
 * @see deezer.Deezer фасад над моделями
 */
public class Track {
    /**
     * <h3>Уникальный ID трека</h3>
     * Deezer внутренний идентификатор (64-bit long, обычно 9-12 цифр).
     * <p>
     * <b>Назначение:</b>
     * <ul>
     *   <li>REST endpoint: {@code /track/{id}}</li>
     *   <li>equals/hashCode: {@code artist.id + title}</li>
     *   <li>Кэширование/БД: primary key</li>
     * </ul>
     * </p>
     * <p>
     * <b>Примеры:</b> Bohemian Rhapsody = {@code 12345}, Nevermind треки ~{@code 678901234}
     * </p>
     * <p>
     * <b>Маппинг:</b> {@code Long.parseLong(JsonProcess.getJsonItem("id"))}
     * </p>
     */
    private long id;

    /**
     * <h3>Доступность трека</h3>
     * Можно ли проиграть/скачать (лицензия, регион).
     * <p>
     * <b>true:</b> доступен в вашем регионе/подписке
     * <p>
     * <b>false:</b> геоблокировка, удален правообладателем
     * </p>
     * <p>
     * <b>UI логика:</b>
     * <pre>{@code
     * if(track.isReadable()) {
     *     player.play(track);
     * } else {
     *     ui.show("Not available in your region");
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Маппинг:</b> {@code Boolean.parseBoolean("readable")}
     * </p>
     */
    private boolean readable;

    /**
     * <h3>Полное название трека</h3>
     * Основное название для UI/плейлистов.
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li>{@code "Bohemian Rhapsody"}</li>
     *   <li>{@code "Smells Like Teen Spirit"}</li>
     *   <li>{@code "Hotel California"}</li>
     * </ul>
     * </p>
     * <p>
     * <b>equals:</b> ключевое поле ({@code title + artist.id})
     * </p>
     */
    private String title;

    /**
     * <h3>Короткое название</h3>
     * Укороченная версия для UI (списки, мини-плеер).
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li>{@code title="Bohemian Rhapsody"} → {@code title_short="Bohemian Rhapsody"}</li>
     *   <li>{@code title="Is This The Real Life Instrumental"} → {@code title_short="Real Life"}</li>
     * </li>
     * </ul>
     * </p>
     * <p>
     * <b>UI:</b> компактные списки, уведомления.
     * </p>
     */
    private String title_short;

    /**
     * <h3>Версия трека</h3>
     * Live/Remix/Radio Edit информация.
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li>{@code "Live at Wembley '86"}</li>
     *   <li>{@code "Remix 2020"}</li>
     *   <li>{@code "Radio Edit"}</li>
     *   <li>{@code null/""} — оригинал</li>
     * </ul>
     * </p>
     * <p>
     * <b>Фильтрация:</b>
     * <pre>{@code
     * tracks.stream()
     *     .filter(t -> t.getTitle_version().contains("Live"))
     *     .collect(toList());
     * }</pre>
     * </p>
     */
    private String title_version;

    /**
     * <h3>ISRC код</h3>
     * Международный стандарт (12 символов).
     * <p>
     * <b>Формат:</b> {@code CC-XXX-YY-NNNNN}
     * <ul>
     *   <li>{@code CC} — страна (US, GB, RU)</li>
     *   <li>{@code XXX} — лейбл</li>
     *   <li>{@code YY} — год</li>
     *   <li>{@code NNNNN} — номер трека</li>
     * </ul>
     * </p>
     * <p>
     * <b>Пример:</b> Queen "Bohemian Rhapsody" = {@code GBBKS7500001}
     * </p>
     * <p>
     * <b>Назначение:</b> уникальность через индустриальный стандарт.
     * </p>
     */
    private String isrc;

    /**
     * <h3>Deezer веб-ссылка</h3>
     * Прямая страница трека на deezer.com.
     * <p>
     * <b>Формат:</b> {@code https://deezer.com/track/{id}}
     * </p>
     * <p>
     * <b>Использование:</b>
     * <pre>{@code
     * ui.showTrackPage(track.getLink());
     * shareUrl = track.getLink();
     * }</pre>
     * </p>
     */
    private String link;

    /**
     * <h3>Длительность (секунды)</h3>
     * Точная длина трека.
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li>Bohemian Rhapsody: {@code 355} сек (5:55)</li>
     *   <li>Nevermind: {@code 300} сек (~5 мин)</li>
     *   <li>Radio Edit: {@code 210} сек (3:30)</li>
     * </li>
     * </ul>
     * </p>
     * <p>
     * <b>UI конвертация:</b>
     * <pre>{@code
     * String time = String.format("%d:%02d", duration/60, duration%60);
     * // "5:55", "3:30"
     * }</pre>
     * </p>
     */
    private int duration;

    /**
     * <h3>Рейтинг/позиция</h3>
     * Чартовая позиция или релевантность поиска.
     * <p>
     * <b>Контекст:</b>
     * <ul>
     *   <li>Чарты: {@code 1} = #1 хит</li>
     *   <li>Поиск: выше = релевантнее</li>
     *   <li>Топ артиста: рейтинг популярности</li>
     * </ul>
     * </p>
     * <p>
     * <b>Сортировка:</b>
     * <pre>{@code
     * tracks.sort(Comparator.comparingInt(Track::getRank));
     * }</pre>
     * </p>
     */
    private int rank;
    /**
     * <h3>Текстовая цензура (18+)</h3>
     * Наличие мата/непристойностей в тексте песни.
     * <p>
     * <b>true:</b> содержит explicit lyrics (Rap, Rock, Hip-Hop)
     * <p>
     * <b>false:</b> чистый текст (Pop, Classical)
     * </p>
     * <p>
     * <b>UI фильтр:</b>
     * <pre>{@code
     * // Только clean версии
     * tracks.stream().filter(t -> !t.isExplicit_lyrics()).collect(toList());
     *
     * // Explicit only
     * tracks.stream().filter(Track::isExplicit_lyrics).collect(toList());
     * }</pre>
     * </p>
     * <p>
     * <b>Deezer:</b> {@code Boolean.parseBoolean("explicit_lyrics")}
     * </p>
     */
    private boolean explicit_lyrics;

    /**
     * <h3>Уровень цензуры текста</h3>
     * Градация 18+ контента в лирике (0-4).
     * <p>
     * <b>Значения:</b>
     * <ul>
     *   <li>{@code 0} — чистый</li>
     *   <li>{@code 1} — легкий сленг</li>
     *   <li>{@code 2} — умеренный мат</li>
     *   <li>{@code 3} — сильный</li>
     *   <li>{@code 4} — экстремальный</li>
     * </ul>
     * </p>
     * <p>
     * <b>Фильтрация:</b>
     * <pre>{@code
     * // Семейный режим
     * tracks.stream().filter(t -> t.getExplicit_content_lyrics() <= 1).collect(toList());
     * }</pre>
     * </p>
     */
    private int explicit_content_lyrics;

    /**
     * <h3>Уровень цензуры обложки</h3>
     * 18+ контент на обложке альбома (0-4).
     * <p>
     * <b>Аналогично:</b> {@link #explicit_content_lyrics} но для визуала.
     * <p>
     * <b>UI blur:</b>
     * <pre>{@code
     * if(track.getExplicit_content_cover() >= 3) {
     *     ui.blurCover(track.getAlbum().getCover_medium());
     * }
     * }</pre>
     * </p>
     */
    private int explicit_content_cover;

    /**
     * <h3>30-секундный превью MP3</h3>
     * Прямая ссылка на прослушивание (без подписки).
     * <p>
     * <b>Формат:</b> {@code https://e-cdn-files.dz. Deezer.com/covers/.../PREVIEW.mp3}
     * </p>
     * <p>
     * <b>Использование:</b>
     * <pre>{@code
     * if(track.getPreview() != null) {
     *     player.playPreview(track.getPreview());
     * }
     *
     * // Скачать для UI
     * download(track.getPreview(), "preview.mp3");
     * }</pre>
     * </p>
     * <p>
     * <b>null:</b> недоступно (full track only с подпиской)
     * </p>
     */
    private String preview;

    /**
     * <h3>MD5 хэш обложки</h3>
     * Уникальный хэш изображения трека/альбома.
     * <p>
     * <b>Назначение:</b>
     * <ul>
     *   <li>Кэширование: {@code cover/{md5_image}.jpg}</li>
     *   <li>Дедупликация изображений</li>
     *   <li>Deezer CDN: {@code https://e-cdn.../{md5}.jpg}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Пример:</b> {@code "a1b2c3d4e5f67890abcdef1234567890"}
     * </p>
     */
    private String md5_image;

    /**
     * <h3>Deezer тип ресурса</h3>
     * Всегда {@code "track"} для этой модели.
     * <p>
     * <b>Полезно для:</b>
     * <ul>
     *   <li>Проверки типа в generic коде</li>
     *   <li>Логирование/дебаг</li>
     *   <li>Future-proofing (main/track/light)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Проверка:</b>
     * <pre>{@code
     * if("track".equals(track.getType())) {
     *     playlist.add(track);
     * }
     * }</pre>
     * </p>
     */
    private String type;

    /**
     * <h3>Исполнитель трека</h3>
     * Полносвязный Artist объект (обогащается {@link deezer.Api}).
     * <p>
     * <b>Содержит:</b> id, name, nb_fan, picture_*, radio, tracklist.
     * </p>
     * <p>
     * <b>Состояния:</b>
     * <ul>
     *   <li>{@code null} — до {@link deezer.Api#getTrack} обогащения</li>
     *   <li>Полный Artist — после парсинга/сеттера</li>
     * </ul>
     * </p>
     * <p>
     * <b>equals/hashCode:</b> {@code artist.getId()} критично для distinct().
     * </p>
     * <p>
     * <b>UI:</b>
     * <pre>{@code
     * String display = track.getArtist().getName() + " - " + track.getTitle();
     * imageView.setImageURI(track.getArtist().getPicture_medium());
     * }</pre>
     * </p>
     * <p>
     * <b>Null-safe toString:</b> {@code artist != null ? name : "null"}
     * </p>
     */
    private Artist artist;

    /**
     * <h3>Альбом трека</h3>
     * Полносвязный Album с обложками всех размеров.
     * <p>
     * <b>Содержит:</b> id, title, cover_small/medium/big/xl, tracklist.
     * </p>
     * <p>
     * <b>Состояния:</b> аналогично {@link #artist} (null до обогащения).
     * </p>
     * <p>
     * <b>UI приоритет:</b>
     * <ul>
     *   <li>{@code cover_medium} — списки</li>
     *   <li>{@code cover_big} — плеер</li>
     *   <li>{@code cover_xl} — фон</li>
     * </ul>
     * </p>
     * <p>
     * <b>Навигация:</b> {@code album.getTracklist()} → полный плейлист альбома.
     * </p>
     */
    private Album album;
    /**
     * <h3>ID трека (readonly)</h3>
     * Уникальный Deezer идентификатор.
     * <p>
     * <b>Геттер:</b> простое чтение {@link #id} поля.
     * </p>
     * <p>
     * <b>Сеттер (Fluent):</b> устанавливает ID, возвращает {@code this}.
     * <pre>{@code
     * track.setId(12345L).setTitle("Bohemian Rhapsody");
     * }</pre>
     * </p>
     * <p>
     * <b>Использование:</b> REST вызовы {@code /track/{getId()}}.
     * </p>
     *
     * @return текущий ID
     */
    public long getId() {
        return id;
    }
    /**
     * <h3>ID setter (Builder pattern)</h3>
     *
     * @param id новый ID трека
     * @return {@code this}
     */
    public Track setId(long id) {
        this.id = id;
        return this;
    }
    /**
     * <h3>Доступность трека</h3>
     * Можно ли воспроизвести (регион/лицензия).
     * <p>
     * <b>Getter:</b> boolean {@link #readable} (is- префикс по Java конвенции).
     * <p>
     * <b>UI логика:</b>
     * <pre>{@code
     * if(track.isReadable()) {
     *     playButton.setEnabled(true);
     * } else {
     *     playButton.setText("Region locked");
     * }
     * }</pre>
     * </p>
     *
     * @return доступен ли трек
     */
    public boolean isReadable() {
        return readable;
    }
    /**
     * <h3>Доступность setter</h3>
     *
     * @param readable новое состояние
     * @return {@code this}
     */
    public Track setReadable(boolean readable) {
        this.readable = readable;
        return this;
    }
    /**
     * <h3>Основное название</h3>
     * Полное название для заголовков/плейлистов.
     * <p>
     * <b>equals/hashCode:</b> ключевое поле + {@link #artist#id}.
     * </p>
     * <p>
     * <b>Примеры:</b> "Bohemian Rhapsody", "Smells Like Teen Spirit".
     * </p>
     *
     * @return полное название
     */
    public String getTitle() {
        return title;
    }
    /**
     * <h3>Название setter</h3>
     *
     * @param title новое название
     * @return {@code this}
     */
    public Track setTitle(String title) {
        this.title = title;
        return this;
    }
    /**
     * <h3>Укороченное название</h3>
     * Компактная версия для списков/miniplayer.
     * <p>
     * <b>getTitle_short():</b> часто = {@link #getTitle()} или урезано.
     * </p>
     *
     * @return короткое название
     */
    public String getTitle_short() {
        return title_short;
    }
    /**
     * <h3>Короткое название setter</h3>
     *
     * @param title_short новое короткое название
     * @return {@code this}
     */
    public Track setTitle_short(String title_short) {
        this.title_short = title_short;
        return this;
    }
    /**
     * <h3>Версия трека</h3>
     * Live/Remix/Radio Edit информация.
     * <p>
     * <b>get:</b> возвращает {@link #title_version} или {@code null} (оригинал).
     * </p>
     * <p>
     * <b>Примеры:</b> "Live at Wembley '86", "Remix 2020", "Radio Edit".
     * </p>
     * <p>
     * <b>set:</b> fluent, возвращает {@code this}.
     * </p>
     *
     * @return версия или null
     */
    public String getTitle_version() {
        return title_version;
    }
    /**
     * <h3>Версия setter</h3>
     *
     * @param title_version live/remix информация
     * @return {@code this}
     */
    public Track setTitle_version(String title_version) {
        this.title_version = title_version;
        return this;
    }
    /**
     * <h3>ISRC код</h3>
     * Международный идентификатор трека (12 символов).
     * <p>
     * <b>Формат:</b> {@code CC-XXX-YY-NNNNN} (GBBKS7500001).
     * </p>
     * <p>
     * <b>get/set:</b> стандартный доступ к {@link #isrc}.
     * </p>
     *
     * @return ISRC или null
     */
    public String getIsrc() {
        return isrc;
    }
    /**
     * <h3>ISRC setter</h3>
     *
     * @param isrc международный код
     * @return {@code this}
     */
    public Track setIsrc(String isrc) {
        this.isrc = isrc;
        return this;
    }
    /**
     * <h3>Deezer веб-ссылка</h3>
     * Прямая страница трека на deezer.com.
     * <p>
     * <b>get:</b> {@code https://deezer.com/track/12345}
     * </p>
     * <p>
     * <b>UI:</b> открытие в браузере, шаринг.
     * </p>
     *
     * @return ссылка на трек
     */
    public String getLink() {
        return link;
    }
    /**
     * <h3>Ссылка setter</h3>
     *
     * @param link Deezer URL
     * @return {@code this}
     */
    public Track setLink(String link) {
        this.link = link;
        return this;
    }
    /**
     * <h3>Длительность (секунды)</h3>
     * Точное время трека.
     * <p>
     * <b>get:</b> {@code 355} = 5:55 для Bohemian Rhapsody.
     * </p>
     * <p>
     * <b>UI:</b> {@code String.format("%d:%02d", duration/60, duration%60)}
     * </p>
     *
     * @return секунды
     */
    public int getDuration() {
        return duration;
    }
    /**
     * <h3>Длительность setter</h3>
     *
     * @param duration секунды
     * @return {@code this}
     */
    public Track setDuration(int duration) {
        this.duration = duration;
        return this;
    }
    /**
     * <h3>Рейтинг/позиция</h3>
     * Чартовая позиция или релевантность.
     * <p>
     * <b>get:</b> {@code 1} = #1 хит, выше = лучше.
     * </p>
     * <p>
     * <b>Сортировка:</b> {@code Comparator.comparingInt(Track::getRank)}
     * </p>
     *
     * @return позиция в чарте/поиске
     */
    public int getRank() {
        return rank;
    }
    /**
     * <h3>Рейтинг setter</h3>
     *
     * @param rank новая позиция
     * @return {@code this}
     */
    public Track setRank(int rank) {
        this.rank = rank;
        return this;
    }
    /**
     * <h3>Explicit текст (18+)</h3>
     * Наличие мата в лирике.
     * <p>
     * <b>isExplicit_lyrics():</b> Java boolean конвенция.
     * </p>
     * <p>
     * <b>Фильтрация:</b>
     * <pre>{@code
     * tracks.stream().filter(t -> !t.isExplicit_lyrics()).collect(toList());
     * }</pre>
     * </p>
     *
     * @return содержит ли мат
     */
    public boolean isExplicit_lyrics() {
        return explicit_lyrics;
    }
    /**
     * <h3>Explicit setter</h3>
     *
     * @param explicit_lyrics 18+ текст
     * @return {@code this}
     */
    public Track setExplicit_lyrics(boolean explicit_lyrics) {
        this.explicit_lyrics = explicit_lyrics;
        return this;
    }
    /**
     * <h3>Уровень цензуры текста (0-4)</h3>
     * Градация мата в лирике (0=чистый, 4=экстремальный).
     * <p>
     * <b>get:</b> числовой рейтинг Deezer.
     * </p>
     * <p>
     * <b>Фильтрация:</b>
     * <pre>{@code
     * // Семейный режим (0-1)
     * tracks.stream()
     *     .filter(t -> t.getExplicit_content_lyrics() <= 1)
     *     .collect(toList());
     * }</pre>
     * </p>
     *
     * @return уровень 0-4
     */
    public int getExplicit_content_lyrics() {
        return explicit_content_lyrics;
    }
    /**
     * <h3>Цензура текста setter</h3>
     *
     * @param explicit_content_lyrics 0-4 рейтинг
     * @return {@code this}
     */
    public Track setExplicit_content_lyrics(int explicit_content_lyrics) {
        this.explicit_content_lyrics = explicit_content_lyrics;
        return this;
    }
    /**
     * <h3>Уровень цензуры обложки (0-4)</h3>
     * 18+ контент на визуале альбома.
     * <p>
     * <b>UI blur:</b>
     * <pre>{@code
     * if(track.getExplicit_content_cover() >= 3) {
     *     ui.blurImage(track.getAlbum().getCover_medium());
     * }
     * }</pre>
     * </p>
     *
     * @return визуальный рейтинг 0-4
     */
    public int getExplicit_content_cover() {
        return explicit_content_cover;
    }
    /**
     * <h3>Цензура обложки setter</h3>
     *
     * @param explicit_content_cover 0-4 рейтинг
     * @return {@code this}
     */
    public Track setExplicit_content_cover(int explicit_content_cover) {
        this.explicit_content_cover = explicit_content_cover;
        return this;
    }
    /**
     * <h3>30-секундный превью MP3</h3>
     * Ссылка на демо без подписки.
     * <p>
     * <b>null:</b> full track only (премиум).
     * </p>
     * <p>
     * <b>Player:</b>
     * <pre>{@code
     * if(track.getPreview() != null) {
     *     audioPlayer.playPreview(track.getPreview());
     * }
     * }</pre>
     * </p>
     *
     * @return MP3 URL или null
     */
    public String getPreview() {
        return preview;
    }
    /**
     * <h3>Превью setter</h3>
     *
     * @param preview 30сек MP3 ссылка
     * @return {@code this}
     */
    public Track setPreview(String preview) {
        this.preview = preview;
        return this;
    }
    /**
     * <h3>MD5 хэш обложки</h3>
     * Уникальный идентификатор изображения.
     * <p>
     * <b>CDN:</b> {@code https://e-cdn-files.dz. Deezer.com/images/{md5_image}.jpg}
     * </p>
     * <p>
     * <b>Кэш ключ:</b> идеально для ImageCache.
     * </p>
     *
     * @return 32 символа MD5
     */
    public String getMd5_image() {
        return md5_image;
    }
    /**
     * <h3>MD5 хэш setter</h3>
     *
     * @param md5_image хэш изображения
     * @return {@code this}
     */
    public Track setMd5_image(String md5_image) {
        this.md5_image = md5_image;
        return this;
    }
    /**
     * <h3>Deezer тип ресурса</h3>
     * Всегда {@code "track"}.
     * <p>
     * <b>Type safety:</b>
     * <pre>{@code
     * if("track".equals(track.getType())) {
     *     playlist.add(track);
     * }
     * }</pre>
     * </p>
     *
     * @return "track"
     */
    public String getType() {
        return type;
    }
    /**
     * <h3>Тип setter</h3>
     *
     * @param type тип ресурса
     * @return {@code this}
     */
    public Track setType(String type) {
        this.type = type;
        return this;
    }
    /**
     * <h3>Связанный исполнитель</h3>
     * Полный Artist (name, picture, nb_fan).
     * <p>
     * <b>UI display:</b>
     * <pre>{@code
     * String line1 = track.getArtist().getName();
     * String line2 = track.getTitle();
     * }</pre>
     * </p>
     * <p>
     * <b>equals:</b> {@code artist.getId()} критично для distinct().
     * </p>
     *
     * @return Artist или null (до обогащения)
     */
    public Artist getArtist() {
        return artist;
    }
    /**
     * <h3>Исполнитель setter (обогащение)</h3>
     * Используется {@link deezer.Api#getTrack()} после базового парсинга.
     * <p>
     * <b>Цепочка:</b>
     * <pre>{@code
     * track.setArtist(queenArtist)
     *      .setAlbum(nightOpera);
     * }</pre>
     * </p>
     *
     * @param artist полный Artist объект
     * @return {@code this}
     */
    public Track setArtist(Artist artist) {
        this.artist = artist;
        return this;
    }

    public Album getAlbum() {
        return album;
    }

    public Track setAlbum(Album album) {
        this.album = album;
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
