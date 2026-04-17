package deezer.models;

import java.util.Objects;

/**
 * <h1>Artist</h1>
 * Полная модель Deezer исполнителя: базовые данные + 5 размеров фото + статистика.
 * <p>
 * <b>Архитектура:</b> Fluent setters + умный {@link #getPictureUrl()} + ID-based equals.
 * Связан с {@link Track} как композиция.
 * </p>
 *
 * <h2>Deezer поля (15):</h2>
 * <table>
 *   <tr><th>Категория</th><th>Поля</th></tr>
 *   <tr><td>Основное</td><td>{@code id}, {@code name}, {@code link}, {@code type="artist"}</td></tr>
 *   <tr><td>Статистика</td><td>{@code nb_album}, {@code nb_fan}, {@code radio}</td></tr>
 *   <tr><td>Ссылки</td><td>{@code tracklist} (топ/альбомы)</td></tr>
 *   <tr><td>Фото (5x)</td><td>{@code picture_small/medium/big/xl}, {@code picture}</td></tr>
 * </table>
 *
 * <h2>Ключевые возможности</h2>
 * <ul>
 *   <li><b>{@link #getPictureUrl()}:</b> приоритет xl→big→medium</li>
 *   <li><b>ID equals/hashCode:</b> идеально для Set/List distinct</li>
 *   <li><b>Fluent API:</b> все setters return {@code this}</li>
 *   <li><b>Детальный toString:</b> все поля для дебаг</li>
 * </ul>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * // Из API
 * Artist queen = api.getArtist("Queen");
 *
 * // UI
 * artistView.setTitle(queen.getName());
 * artistView.setSubtitle(queen.getNbFan() + " fans");
 * artistView.setImage(queen.getPictureUrl());
 *
 * // Distinct артисты
 * Set<Artist> unique = artists.stream().collect(toSet());
 *
 * // Fluent создание
 * Artist custom = new Artist()
 *     .setName("My Band")
 *     .setId(999L)
 *     .setNbFan(1000);
 * }</pre>
 *
 * <h2>Picture приоритеты</h2>
 * <pre>{@code
 * getPictureUrl() → picture_xl (960x960)
 *              → picture_big (500x500)
 *              → picture_medium (250x250)
 * }</pre>
 *
 * <h2>equals/hashCode/toString</h2>
 * <table>
 *   <tr><th>Метод</th><th>Логика</th></tr>
 *   <tr><td>equals</td><td>{@code id == other.id}</td></tr>
 *   <tr><td>hashCode</td><td>{@code Objects.hash(id)}</td></tr>
 *   <tr><td>toString</td><td>ВСЕ поля (дебаг/лог)</td></tr>
 * </table>
 *
 * <h2>Thread-safety</h2>
 * <p><b>Не thread-safe:</b> mutable поля без синхронизации.
 * </p>
 *
 * @see Track содержит Artist ссылку
 * @see deezer.Api#getArtistFromJson(String) () маппер
 * @see deezer.Deezer#getRelatedArtist(String) () источник данных
 */
public class Artist {
    /**
     * <h3>Уникальный ID исполнителя</h3>
     * Deezer идентификатор артиста (основной ключ для equals/hashCode).
     * <p>
     * <b>Назначение:</b>
     * <ul>
     *   <li>REST: {@code /artist/{id}}</li>
     *   <li>equals/hashCode: {@code id == other.id}</li>
     *   <li>Кэш/БД: primary key</li>
     * </ul>
     * </p>
     * <p>
     * <b>Примеры:</b> Queen={@code 123}, Radiohead={@code 456}
     * </p>
     */
    private long id;

    /**
     * <h3>Название исполнителя</h3>
     * Основное имя для UI/поиска/плейлистов.
     * <p>
     * <b>Примеры:</b> "Queen", "The Beatles", "Nirvana", "Daft Punk"
     * </p>
     * <p>
     * <b>UI:</b> заголовок ArtistView, сортировка по алфавиту.
     * </p>
     */
    private String name;

    /**
     * <h3>Deezer страница артиста</h3>
     * Прямая ссылка на профиль.
     * <p>
     * <b>Формат:</b> {@code https://deezer.com/artist/{id}}
     * </p>
     * <p>
     * <b>Использование:</b> шаринг, открытие в браузере.
     * </p>
     */
    private String link;

    /**
     * <h3>Плейлист топ треков/альбомов</h3>
     * Ссылка на endpoint с треками/альбомами артиста.
     * <p>
     * <b>Формат:</b> {@code https://api.deezer.com/artist/{id}/top}
     * </p>
     * <p>
     * <b>Расширение:</b>
     * <pre>{@code
     * // Получить топ треки через tracklist
     * List<Track> top = deezer.getTracks(artist.getTracklist());
     * }</pre>
     * </p>
     */
    private String tracklist;

    /**
     * <h3>Deezer тип ресурса</h3>
     * Всегда {@code "artist"}.
     * <p>
     * <b>Type safety:</b>
     * <pre>{@code
     * if("artist".equals(artist.getType())) {
     *     artistView.showArtist(artist);
     * }
     * }</pre>
     * </p>
     */
    private String type;

    /**
     * <h3>Количество альбомов</h3>
     * Статистика дискографии.
     * <p>
     * <b>UI:</b> "12 albums", фильтр по активности.
     * </p>
     * <p>
     * <b>Примеры:</b> Queen={@code 45+}, Radiohead={@code 12}
     * </p>
     */
    private int nbAlbum;

    /**
     * <h3>Количество фанатов</h3>
     * Популярность метрика Deezer.
     * <p>
     * <b>UI:</b> сортировка, "5.2M fans", топ артистов.
     * </p>
     * <p>
     * <b>Примеры:</b> Queen={@code 5M+}, Daft Punk={@code 10M}
     * </p>
     */
    private int nbFan;

    /**
     * <h3>Deezer Radio доступен</h3>
     * Автоматический плейлист на основе артиста.
     * <p>
     * <b>true:</b> популярные артисты (Queen, Nirvana)
     * <p>
     * <b>false:</b> нишевые/новые
     * </p>
     * <p>
     * <b>UI:</b> "Play Radio" кнопка.
     * </p>
     */
    private boolean radio;
    /**
     * <h3>Маленькая аватарка (56x56px)</h3>
     * Иконки, списки, таблицы, уведомления.
     * <p>
     * <b>Формат:</b> {@code https://e-cdn-files.dz. Deezer.com/cache/img/{hash}/56x56-000000-80-0-0.jpg}
     * </p>
     * <p>
     * <b>UI:</b> ListView иконки, Android Notification, compact списки.
     * </p>
     * <p>
     * <b>Загрузка:</b> низкий приоритет, thumbnail.
     * </p>
     */
    private String picture_small;

    /**
     * <h3>Средняя аватарка (250x250px)</h3>
     * Основной размер для карточек и альбомов.
     * <p>
     * <b>UI:</b> Artist карточки, альбомные сетки, средние списки.
     * </p>
     * <p>
     * <b>Приоритет:</b> fallback в {@link #getPictureUrl()} если big/xl недоступны.
     * </p>
     * <p>
     * <b>List/GridView:</b> оптимальный размер для RecyclerView.
     * </p>
     */
    private String picture_medium;

    /**
     * <h3>Большая аватарка (500x500px)</h3>
     * Artist страницы, плееры, детальные виды.
     * <p>
     * <b>UI:</b> Artist профиль, now playing, расширенные карточки.
     * </p>
     * <p>
     * <b>Приоритет:</b> второй в {@link #getPictureUrl()} (xl → big → medium).
     * </p>
     */
    private String picture_big;

    /**
     * <h3>HD аватарка (960x960px)</h3>
     * Максимальное разрешение для фонов/Retina.
     * <p>
     * <b>UI:</b> Artist page background, hi-dpi экраны, баннеры.
     * </p>
     * <p>
     * <b>Приоритет #1:</b> первый выбор {@link #getPictureUrl()}.
     * </p>
     * <p>
     * <b>Загрузка:</b> высокий приоритет для современных UI.
     * </p>
     */
    private String picture_xl;

    /**
     * <h3>Основная аватарка (default размер)</h3>
     * Фоллбэк картинка, размер не документирован Deezer.
     * <p>
     * <b>Роль:</b> legacy поле, используется если другие null.
     * </p>
     * <p>
     * <b>getPictureUrl():</b> **НЕ** включается в приоритет (только xl→big→medium).
     * </p>
     * <p>
     * <b>Совместимость:</b> старые клиенты Deezer.
     * </p>
     */
    private String picture;
    /**
     * <h3>Умный выбор аватарки (приоритетный)</h3>
     * Возвращает лучшее доступное изображение по приоритету качества.
     * <p>
     * <b>Логика (тернарный оператор):</b>
     * <pre>{@code
     * picture_xl (960px) → picture_big (500px) → picture_medium (250px)
     * }</pre>
     * </p>
     * <p>
     * <b>Исключает:</b> {@code picture_small} (мелкая), {@code picture} (legacy).
     * </p>
     * <p>
     * <b>UI идеал:</b>
     * <pre>{@code
     * // Один вызов для всех экранов
     * imageView.load(artist.getPictureUrl());
     *
     * // Glide/Picasso/Coil автоматически кэширует
     * Glide.with(context).load(artist.getPictureUrl()).into(imageView);
     * }</pre>
     * </p>
     * <p>
     * <b>Производительность:</b> O(1), 3 null-чекa.
     * </p>
     *
     * @return xl→big→medium URL или null (если все null)
     */
    public String getPictureUrl() {
        return picture_xl != null ? picture_xl :
                picture_big != null ? picture_big : picture_medium;
    }
    /**
     * <h3>Уникальный ID (primary key)</h3>
     * Deezer идентификатор артиста.
     * <p>
     * <b>get:</b> простое чтение {@link #id}.
     * </p>
     * <p>
     * <b>Использование:</b> REST {@code /artist/{id}}, equals/hashCode.
     * </p>
     *
     * @return артист ID
     */
    public long getId() {
        return id;
    }
    /**
     * <h3>ID setter (Fluent)</h3>
     * Устанавливает ID, возвращает {@code this}.
     * <p>
     * <pre>{@code
     * artist.setId(123L).setName("Queen");
     * }</pre>
     * </p>
     *
     * @param id новый ID
     * @return {@code this}
     */
    public Artist setId(long id) {
        this.id = id;
        return this;
    }
    /**
     * <h3>Название исполнителя</h3>
     * Основное имя для UI/поиска.
     * <p>
     * <b>get:</b> возвращает {@link #name} (может быть null до парсинга).
     * </p>
     * <p>
     * <b>UI:</b> заголовки, списки, поиск.
     * <pre>{@code
     * textView.setText(artist.getName());  // "Queen"
     * }</pre>
     * </p>
     *
     * @return имя артиста
     */
    public String getName() {
        return name;
    }
    /**
     * <h3>Название setter (Fluent)</h3>
     * Основное поле для JSON маппинга.
     * <p>
     * <b>Используется:</b> {@link deezer.Api#getAlbumFromJson(String)} ()}.
     * </p>
     *
     * @param name имя исполнителя
     * @return {@code this}
     */
    public Artist setName(String name) {
        this.name = name;
        return this;
    }
    /**
     * <h3>Deezer профиль артиста</h3>
     * Прямая веб-страница исполнителя на deezer.com.
     * <p>
     * <b>Формат:</b> {@code https://deezer.com/artist/{id}}
     * </p>
     * <p>
     * <b>get:</b> возвращает {@link #link} для шаринга/браузера.
     * </p>
     * <p>
     * <b>UI/Sharing:</b>
     * <pre>{@code
     * // Открыть профиль
     * Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(artist.getLink()));
     * startActivity(browser);
     *
     * // Соцсети
     * shareIntent.putExtra(Intent.EXTRA_TEXT, "Check " + artist.getName() + ": " + artist.getLink());
     * }</pre>
     * </p>
     *
     * @return URL профиля или null
     */
    public String getLink() {
        return link;
    }
    /**
     * <h3>Ссылка setter</h3>
     * Устанавливает Deezer профиль.
     *
     * @param link профиль URL
     * @return {@code this}
     */
    public Artist setLink(String link) {
        this.link = link;
        return this;
    }
    /**
     * <h3>API плейлист артиста</h3>
     * Ссылка на endpoint с топ треками/альбомами.
     * <p>
     * <b>Формат:</b> {@code https://api.deezer.com/artist/{id}/top}
     * </p>
     * <p>
     * <b>Расширение:</b> позволяет получить полную дискографию.
     * </p>
     * <p>
     * <b>Использование:</b>
     * <pre>{@code
     * // Топ треки через tracklist
     * String topTracksUrl = artist.getTracklist();
     * List<Track> top = deezer.getTracks(topTracksUrl); // custom метод
     *
     * // Альбомы
     * String albumsUrl = topTracksUrl.replace("/top", "/albums");
     * }</pre>
     * </p>
     * <p>
     * <b>Расширяемость:</b> базовый URL для всех artist endpoints.
     * </p>
     *
     * @return API плейлист URL
     */
    public String getTracklist() {
        return tracklist;
    }
    /**
     * <h3>Tracklist setter</h3>
     * Устанавливает API ссылку на контент артиста.
     *
     * @param tracklist топ/альбомы endpoint
     * @return {@code this}
     */
    public Artist setTracklist(String tracklist) {
        this.tracklist = tracklist;
        return this;
    }
    /**
     * <h3>Deezer тип ресурса</h3>
     * Всегда {@code "artist"} для type safety.
     * <p>
     * <b>get:</b> проверка корректности модели.
     * </p>
     * <p>
     * <b>Generic код:</b>
     * <pre>{@code
     * if("artist".equals(artist.getType())) {
     *     artistList.add(artist);
     * }
     * }</pre>
     * </p>
     *
     * @return "artist"
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
    public Artist setType(String type) {
        this.type = type;
        return this;
    }
    /**
     * <h3>Количество альбомов</h3>
     * Размер дискографии исполнителя.
     * <p>
     * <b>UI:</b> "45 albums", фильтр по активности.
     * </p>
     * <p>
     * <b>Примеры:</b> Queen={@code 45+}, Radiohead={@code 12}, новички={@code 1}.
     * </p>
     *
     * @return число альбомов
     */
    public int getNbAlbum() {
        return nbAlbum;
    }
    /**
     * <h3>Альбомы setter</h3>
     *
     * @param nbAlbum дискография
     * @return {@code this}
     */
    public Artist setNbAlbum(int nbAlbum) {
        this.nbAlbum = nbAlbum;
        return this;
    }
    /**
     * <h3>Количество фанатов</h3>
     * Основная метрика популярности.
     * <p>
     * <b>UI форматирование:</b>
     * <pre>{@code
     * String fans = nbFan >= 1_000_000 ? nbFan/1_000_000 + "M" :
     *               nbFan >= 1_000 ? nbFan/1_000 + "K" : nbFan + "";
     * // "5.2M", "150K", "999"
     * }</pre>
     * </p>
     * <p>
     * <b>Сортировка:</b> {@code Comparator.comparingInt(Artist::getNbFan)}
     * </p>
     *
     * @return число подписчиков
     */
    public int getNbFan() {
        return nbFan;
    }
    /**
     * <h3>Фанаты setter</h3>
     *
     * @param nbFan популярность
     * @return {@code this}
     */
    public Artist setNbFan(int nbFan) {
        this.nbFan = nbFan;
        return this;
    }
    /**
     * <h3>Deezer Radio доступен</h3>
     * Автогенерированный плейлист на основе артиста.
     * <p>
     * <b>true:</b> Queen, Daft Punk, Nirvana (популярные).
     * <p>
     * <b>false:</b> нишевые/новые исполнители.
     * </p>
     * <p>
     * <b>UI:</b> "Play Radio" кнопка.
     * </p>
     *
     * @return radio плейлист доступен
     */
    public boolean isRadio() {
        return radio;
    }
    /**
     * <h3>Radio setter</h3>
     *
     * @param radio наличие радио
     * @return {@code this}
     */
    public Artist setRadio(boolean radio) {
        this.radio = radio;
        return this;
    }
    /**
     * <h3>Маленькая аватарка (56x56px)</h3>
     * Иконки, списки, уведомления.
     * <p>
     * <b>UI:</b> RecyclerView thumbnails, Notification icons.
     * </p>
     *
     * @return small picture URL
     */
    public String getPicture_small() {
        return picture_small;
    }
    /**
     * <h3>Маленькая аватарка setter</h3>
     *
     * @param picture_small 56x56 URL
     * @return {@code this}
     */
    public Artist setPicture_small(String picture_small) {
        this.picture_small = picture_small;
        return this;
    }
    /**
     * <h3>Средняя аватарка (250x250px)</h3>
     * Основной размер для карточек и альбомных сеток.
     * <p>
     * <b>UI контекст:</b> RecyclerView карточки, Artist списки, альбомные сетки.
     * </p>
     * <p>
     * <b>get:</b> прямой доступ к {@link #picture_medium}.
     * </p>
     * <p>
     * <b>Приоритет:</b> #3 в {@link #getPictureUrl()} (xl→big→**medium**).
     * </p>
     * <p>
     * <b>Оптимально:</b> баланс качества/трафика для мобильных.
     * </p>
     *
     * @return 250x250 URL
     */
    public String getPicture_medium() {
        return picture_medium;
    }
    /**
     * <h3>Средняя аватарка setter</h3>
     *
     * @param picture_medium 250x250 URL
     * @return {@code this}
     */
    public Artist setPicture_medium(String picture_medium) {
        this.picture_medium = picture_medium;
        return this;
    }
    /**
     * <h3>Большая аватарка (500x500px)</h3>
     * Детальные виды: Artist страницы, плееры, расширенные карточки.
     * <p>
     * <b>UI контекст:</b> Artist профиль, now playing экраны, pop-up карточки.
     * </p>
     * <p>
     * <b>Приоритет:</b> #2 в {@link #getPictureUrl()} (xl→**big**→medium).
     * </p>
     * <p>
     * <b>Tablet/Desktop:</b> основной размер для средних экранов.
     * </p>
     *
     * @return 500x500 URL
     */
    public String getPicture_big() {
        return picture_big;
    }
    /**
     * <h3>Большая аватарка setter</h3>
     *
     * @param picture_big 500x500 URL
     * @return {@code this}
     */
    public Artist setPicture_big(String picture_big) {
        this.picture_big = picture_big;
        return this;
    }
    /**
     * <h3>HD аватарка (960x960px)</h3>
     * Максимальное качество для фонов и Retina экранов.
     * <p>
     * <b>UI контекст:</b> Artist page backgrounds, hi-dpi дисплеи, баннеры.
     * </p>
     * <p>
     * <b>Приоритет #1:</b> первый выбор {@link #getPictureUrl()}.
     * </p>
     * <p>
     * <b>Современные UI:</b> 4K/Retina, плеер фоны.
     * </p>
     *
     * @return 960x960 URL (или null)
     */
    public String getPicture_xl() {
        return picture_xl;
    }
    /**
     * <h3>HD аватарка setter</h3>
     *
     * @param picture_xl 960x960 URL
     * @return {@code this}
     */
    public Artist setPicture_xl(String picture_xl) {
        this.picture_xl = picture_xl;
        return this;
    }
    /**
     * <h3>Основная аватарка (legacy/default размер)</h3>
     * Старое поле Deezer API, размер не документирован.
     * <p>
     * <b>Роль:</b> использовалось в ранних версиях API как единственная картинка.
     * </p>
     * <p>
     * <b>Совместимость:</b> для обратной совместимости со старыми клиентами.
     * </p>
     * <p>
     * <b>Исключено из:</b> {@link #getPictureUrl()} — не участвует в приоритете.
     * </p>
     * <p>
     * <b>get:</b> прямой доступ к {@link #picture} полю.
     * </p>
     * <p>
     * <b>Когда использовать:</b>
     * <pre>{@code
     * // Legacy клиенты/библиотеки
     * if(artist.getPicture() != null) {
     *     legacyImageView.setImageURI(artist.getPicture());
     * }
     *
     * // Emergency fallback (последний резерв)
     * String url = artist.getPictureUrl() != null ? artist.getPictureUrl() :
     *              artist.getPicture();
     * }</pre>
     * </p>
     *
     * @return legacy picture URL или null
     */
    public String getPicture() {
        return picture;
    }
    /**
     * <h3>Legacy картинка setter</h3>
     * Устанавливает основную аватарку для совместимости.
     * <p>
     * <b>JSON маппинг:</b> используется {@link deezer.Api#getArtistFromJson(String)} ()}.
     * </p>
     * <p>
     * <b>Fluent:</b> возвращает {@code this} как все сеттеры Artist.
     * </p>
     *
     * @param picture legacy/default размер URL
     * @return {@code this}
     */
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