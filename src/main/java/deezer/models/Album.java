package deezer.models;

import java.util.Objects;

/**
 * <h1>Album</h1>
 * Модель альбома Deezer: обложки 5 размеров + tracklist + базовые метаданные.
 * <p>
 * <b>Архитектура:</b> Минималистичная (10 полей), Fluent API, ID-based equals.
 * Компонуется в {@link Track} как связь.
 * </p>
 *
 * <h2>Deezer поля (10):</h2>
 * <table>
 *   <tr><th>Категория</th><th>Поля</th></tr>
 *   <tr><td>ID</td><td>{@code id}</td></tr>
 *   <tr><td>Название</td><td>{@code title}</td></tr>
 *   <tr><td>Обложки (5x)</td><td>{@code cover, cover_small/medium/big/xl}</td></tr>
 *   <tr><td>Хэш</td><td>{@code md5_image}</td></tr>
 *   <tr><td>Ссылки</td><td>{@code tracklist}</td></tr>
 *   <tr><td>Тип</td><td>{@code type="album"}</td></tr>
 * </table>
 *
 * <h2>Ключевые особенности</h2>
 * <ul>
 *   <li><b>ID equals/hashCode:</b> для Set/List distinct</li>
 *   <li><b>Fluent setters:</b> return {@code this}</li>
 *   <li><b>Детальный toString:</b> все поля для дебаг</li>
 *   <li><b>5 размеров обложек:</b> small→xl (аналог Artist)</li>
 * </ul>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * // Из Track
 * Album album = track.getAlbum();
 *
 * // UI
 * albumView.setTitle(album.getTitle());
 * albumView.setImage(album.getCover_medium());
 * albumView.setSubtitle(album.getTracklist());
 *
 * // Distinct альбомы
 * Set<Album> uniqueAlbums = albums.stream().collect(toSet());
 *
 * // Fluent
 * Album custom = new Album()
 *     .setTitle("Custom Album")
 *     .setId(999L)
 *     .setCover_medium("cover.jpg");
 * }</pre>
 *
 * <h2>Обложки приоритет</h2>
 * <pre>
 * cover_xl (960px) → cover_big (500px) → cover_medium (250px)
 * cover_small (56px), cover (legacy)
 * </pre>
 * Рекомендация: {@code getCover_medium()} для большинства UI.
 *
 * <h2>equals/hashCode/toString</h2>
 * <table>
 *   <tr><th>Метод</th><th>Логика</th></tr>
 *   <tr><td>equals</td><td>{@code id == other.id}</td></tr>
 *   <tr><td>hashCode</td><td>{@code Objects.hash(id)}</td></tr>
 *   <tr><td>toString</td><td>ВСЕ поля (дебаг)</td></tr>
 * </table>
 *
 * <h2>Thread-safety</h2>
 * <p><b>Не thread-safe:</b> mutable поля.
 * </p>
 *
 * @see Track#getAlbum() источник Album
 * @see deezer.Api#getAlbumFromJson(String) () маппер
 * @see deezer.Deezer чарты/поиск (Track → Album)
 */
public class Album {
    /**
     * <h3>Уникальный ID альбома</h3>
     * Deezer идентификатор (primary key для equals/hashCode).
     * <p>
     * <b>REST:</b> {@code /album/{id}}, {@code /album/{id}/tracks}
     * </p>
     * <p>
     * <b>Примеры:</b> "A Night at the Opera" = {@code 123456}
     * </p>
     */
    private long id;

    /**
     * <h3>Название альбома</h3>
     * Основное название для UI/каталогизации.
     * <p>
     * <b>Примеры:</b> "A Night at the Opera", "Nevermind", "OK Computer"
     * </p>
     * <p>
     * <b>UI:</b> заголовки, сортировка, фильтры.
     * </p>
     */
    private String title;

    /**
     * <h3>Основная обложка (legacy/default)</h3>
     * Старое поле Deezer, размер не документирован.
     * <p>
     * <b>Роль:</b> обратная совместимость, используется если cover_* отсутствуют.
     * </p>
     */
    private String cover;

    /**
     * <h3>Маленькая обложка (56x56px)</h3>
     * Иконки альбомов, списки, уведомления.
     * <p>
     * <b>UI:</b> Album list thumbnails, notifications.
     * </p>
     */
    private String cover_small;

    /**
     * <h3>Средняя обложка (250x250px)</h3>
     * **Основной размер** для карточек/сеток альбомов.
     * <p>
     * <b>UI:</b> RecyclerView, GridView, альбомные карточки.
     * </p>
     * <p>
     * <b>Рекомендация:</b> {@code getCover_medium()} для большинства случаев.
     * </p>
     */
    private String cover_medium;

    /**
     * <h3>Большая обложка (500x500px)</h3>
     * Детальные виды: плееры, альбомные страницы.
     * <p>
     * <b>UI:</b> now playing, album detail screens.
     * </p>
     */
    private String cover_big;

    /**
     * <h3>HD обложка (960x960px)</h3>
     * Фоны, Retina экраны, баннеры.
     * <p>
     * <b>UI:</b> player backgrounds, hi-dpi displays.
     * </p>
     */
    private String cover_xl;

    /**
     * <h3>MD5 хэш обложки</h3>
     * Уникальный идентификатор изображения.
     * <p>
     * <b>CDN:</b> {@code https://e-cdn-files.dz. Deezer.com/images/{md5_image}.jpg}
     * </p>
     * <p>
     * <b>Кэш:</b> идеальный ключ для ImageCache.
     * </p>
     * <p>
     * <b>Пример:</b> {@code "a1b2c3d4e5f67890abcdef1234567890"}
     * </p>
     */
    private String md5_image;

    /**
     * <h3>Плейлист треков альбома</h3>
     * API endpoint со всеми треками альбома.
     * <p>
     * <b>Формат:</b> {@code https://api.deezer.com/album/{id}/tracks}
     * </p>
     * <p>
     * <b>Расширение:</b> полный tracklist альбома.
     * </p>
     */
    private String tracklist;

    /**
     * <h3>Deezer тип ресурса</h3>
     * Всегда {@code "album"}.
     * <p>
     * <b>Type safety:</b> generic код проверки.
     * </p>
     */
    private String type;
    /**
     * <h3>ID геттер</h3>
     * Уникальный идентификатор альбома (primary key).
     * <p>
     * <b>equals/hashCode:</b> основано на {@link #id}.
     * </p>
     * <p>
     * <b>REST:</b> {@code /album/{getId()}}, {@code /album/{getId()}/tracks}.
     * </p>
     *
     * @return альбом ID
     */
    public long getId() {
        return id;
    }
    /**
     * <h3>ID setter (Fluent)</h3>
     * Устанавливает primary key, возвращает {@code this}.
     *
     * @param id уникальный ID
     * @return {@code this}
     */
    public Album setId(long id) {
        this.id = id;
        return this;
    }
    /**
     * <h3>Название альбома</h3>
     * Основное название для UI/каталогов.
     * <p>
     * <b>Примеры:</b> "A Night at the Opera", "Nevermind", "Abbey Road".
     * </p>
     * <p>
     * <b>UI:</b> заголовки, сортировка, поиск.
     * </p>
     *
     * @return название альбома
     */
    public String getTitle() {
        return title;
    }
    /**
     * <h3>Название setter</h3>
     *
     * @param title название альбома
     * @return {@code this}
     */
    public Album setTitle(String title) {
        this.title = title;
        return this;
    }
    /**
     * <h3>Основная обложка (legacy)</h3>
     * Старое поле Deezer API, размер неизвестен.
     * <p>
     * <b>Роль:</b> fallback для старых клиентов.
     * </p>
     * <p>
     * <b>Совместимость:</b> используется если cover_* null.
     * </p>
     *
     * @return legacy cover URL
     */
    public String getCover() {
        return cover;
    }
    /**
     * <h3>Основная обложка setter</h3>
     *
     * @param cover legacy URL
     * @return {@code this}
     */
    public Album setCover(String cover) {
        this.cover = cover;
        return this;
    }
    /**
     * <h3>Маленькая обложка (56x56px)</h3>
     * Иконки альбомов, списки, уведомления.
     * <p>
     * <b>UI:</b> Album list thumbnails, notifications, compact views.
     * </p>
     *
     * @return 56x56 URL
     */
    public String getCover_small() {
        return cover_small;
    }
    /**
     * <h3>Маленькая обложка setter</h3>
     *
     * @param cover_small иконка URL
     * @return {@code this}
     */
    public Album setCover_small(String cover_small) {
        this.cover_small = cover_small;
        return this;
    }
    /**
     * <h3>Средняя обложка (250x250px)</h3>
     * **Основной размер** для альбомных карточек и сеток.
     * <p>
     * <b>UI контекст:</b> RecyclerView, GridView, альбомные списки.
     * </p>
     * <p>
     * <b>Рекомендация:</b> **используйте для большинства случаев**.
     * </p>
     * <p>
     * <b>Баланс:</b> качество/трафик оптимально для мобильных.
     * </p>
     *
     * @return 250x250 URL
     */
    public String getCover_medium() {
        return cover_medium;
    }
    /**
     * <h3>Средняя обложка setter</h3>
     *
     * @param cover_medium карточки URL
     * @return {@code this}
     */
    public Album setCover_medium(String cover_medium) {
        this.cover_medium = cover_medium;
        return this;
    }
    /**
     * <h3>Большая обложка (500x500px)</h3>
     * Детальные виды: плееры, альбомные страницы.
     * <p>
     * <b>UI:</b> now playing экраны, album detail, pop-up views.
     * </p>
     *
     * @return 500x500 URL
     */
    public String getCover_big() {
        return cover_big;
    }
    /**
     * <h3>Большая обложка setter</h3>
     *
     * @param cover_big плеер URL
     * @return {@code this}
     */
    public Album setCover_big(String cover_big) {
        this.cover_big = cover_big;
        return this;
    }
    /**
     * <h3>HD обложка (960x960px)</h3>
     * Фоны, Retina экраны, баннеры альбомов.
     * <p>
     * <b>UI:</b> player backgrounds, hi-dpi displays, promotional.
     * </p>
     *
     * @return 960x960 URL
     */
    public String getCover_xl() {
        return cover_xl;
    }
    /**
     * <h3>HD обложка setter</h3>
     *
     * @param cover_xl фон URL
     * @return {@code this}
     */
    public Album setCover_xl(String cover_xl) {
        this.cover_xl = cover_xl;
        return this;
    }
    /**
     * <h3>MD5 хэш обложки</h3>
     * Уникальный идентификатор изображения альбома.
     * <p>
     * <b>CDN путь:</b> {@code https://e-cdn-files.dz.deezer.com/images/{md5_image}.jpg}
     * </p>
     * <p>
     * <b>Кэширование:</b> идеальный ключ для ImageCache/LruCache.
     * </p>
     * <p>
     * <b>Дедупликация:</b> одинаковые обложки = один md5_image.
     * </p>
     * <p>
     * <b>Пример:</b> {@code "a1b2c3d4e5f67890abcdef1234567890"}
     * </p>
     *
     * @return 32 символа MD5 хэш
     */
    public String getMd5_image() {
        return md5_image;
    }
    /**
     * <h3>MD5 хэш setter</h3>
     *
     * @param md5_image уникальный хэш изображения
     * @return {@code this}
     */
    public Album setMd5_image(String md5_image) {
        this.md5_image = md5_image;
        return this;
    }
    /**
     * <h3>Плейлист треков альбома</h3>
     * API endpoint со всеми треками альбома (включая бонусы).
     * <p>
     * <b>Формат:</b> {@code https://api.deezer.com/album/{id}/tracks}
     * </p>
     * <p>
     * <b>get:</b> возвращает URL для загрузки полного tracklist.
     * </p>
     * <p>
     * <b>Расширение:</b>
     * <pre>{@code
     * // Загрузить все треки альбома
     * String tracksUrl = album.getTracklist();
     * List<Track> albumTracks = deezer.getTracks(tracksUrl); // custom метод
     *
     * // Плейлист = альбом целиком
     * player.queue(album.getTracklist());
     * }</pre>
     * </p>
     * <p>
     * <b>Порядок:</b> соответствует оригинальному релизу.
     * </p>
     *
     * @return API tracks URL альбома
     */
    public String getTracklist() {
        return tracklist;
    }
    /**
     * <h3>Tracklist setter</h3>
     * Устанавливает ссылку на треки альбома.
     * <p>
     * <b>JSON маппинг:</b> {@link deezer.Api#getAlbumFromJson(String)} )}.
     * </p>
     *
     * @param tracklist URL треков альбома
     * @return {@code this}
     */
    public Album setTracklist(String tracklist) {
        this.tracklist = tracklist;
        return this;
    }
    /**
     * <h3>Deezer тип ресурса</h3>
     * Всегда {@code "album"} для type safety.
     * <p>
     * <b>get:</b> проверка корректности в generic коде.
     * </p>
     * <p>
     * <b>Пример:</b>
     * <pre>{@code
     * if("album".equals(album.getType())) {
     *     albumList.add(album);
     * }
     *
     * // Полиморфизм Track/Album/Artist
     * switch(resource.getType()) {
     *     case "album" -> showAlbum((Album) resource);
     *     case "artist" -> showArtist((Artist) resource);
     * }
     * }</pre>
     * </p>
     *
     * @return "album"
     */
    public String getType() {
        return type;
    }
    /**
     * <h3>Тип setter</h3>
     * Устанавливает тип ресурса (всегда "album").
     * <p>
     * <b>JSON маппинг:</b> из Deezer API.
     * </p>
     *
     * @param type тип ресурса
     * @return {@code this}
     */
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