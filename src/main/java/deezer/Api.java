package deezer;

import deezer.models.Album;
import deezer.models.Artist;
import deezer.models.Track;
import org.json.simple.parser.ParseException;
import rf.ebanina.utils.formats.json.JsonProcess;
import rf.ebanina.utils.network.Request;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;

/**
 * <h1>Api</h1>
 * Низкоуровневый адаптер Deezer API: поиск по ID/имени + JSON→модель маппинг.
 * <p>
 * <b>Архитектура:</b> Детальный парсер Deezer JSON ответов в доменные модели.
 * Используется {@link Deezer} фасадом как dependency.
 * </p>
 *
 * <h2>Основные возможности</h2>
 * <table>
 *   <tr><th>Метод</th><th>Назначение</th></tr>
 *   <tr><td>{@link #getTrack(String)}</td><td>Поиск трека по имени → обогащенный Track</td></tr>
 *   <tr><td>{@link #getArtist(String)}</td><td>Поиск артиста по имени → базовый Artist</td></tr>
 *   <tr><td>{@link #getTrackFromJson(String)}</td><td>Raw JSON → Track (используется фасадом)</td></tr>
 *   <tr><td>{@link #getArtistFromJson(String)}</td><td>Raw JSON → Artist</td></tr>
 *   <tr><td>{@link #getAlbumFromJson(String)}</td><td>Raw JSON → Album</td></tr>
 * </table>
 *
 * <h2>Proxy поддержка</h2>
 * <p>Fluent setter {@link #setProxy(Proxy)} + {@link #buildRequest(URL)} для HTTP-прокси.
 * <pre>{@code
 * Api api = new Api("https://api.deezer.com/")
 *     .setProxy(new Proxy(Proxy.Type.HTTP, proxyAddr));
 * }</pre>
 * </p>
 *
 * <h2>Поисковая логика (getTrack/getArtist)</h2>
 * <p><b>Общий шаблон:</b> search endpoint → первый результат (limit=1, order=RANKING, strict=false)
 * <table>
 *   <tr><th>Метод</th><th>URL</th></tr>
 *   <tr><td>{@link #getTrack}</td><td>{@code /search/track?q=Bohemian+Rhapsody&amp;limit=1&amp;order=RANKING&amp;strict=false}</td></tr>
 *   <tr><td>{@link #getArtist}</td><td>{@code /search/artist?q=Queen&amp;limit=1&amp;order=RANKING&amp;strict=false}</td></tr>
 * </table></p>
 *
 * <h2>JSON маппинг (FromJson методы)</h2>
 * <p><b>Детальный парсинг:</b> каждое поле модели заполняется с null-check + type conversion.
 * <pre>{@code
 * Track: 15 полей (id→Long, readable→Boolean, preview→String...)
 * Artist: 15 полей (nb_fan→int, radio→Boolean, picture_*→String)
 * Album: 10 полей (cover_*→String, tracklist→String)
 * }</pre>
 * </p>
 * <p><b>Null-safety:</b> defensive null-checks → default значения (0L, "", false).
 * </p>
 *
 * <h2>Жизненный цикл</h2>
 * <pre>{@code
 * Api api = new Api(deezerUrl);           // Базовый
 * api = api.setProxy(proxy);              // Proxy (опционально)
 * Track track = api.getTrack("Bohemian"); // Поиск
 * Artist artist = api.getArtistFromJson(json); // Маппинг
 * }</pre>
 *
 * <h2>Использование в Deezer фасаде</h2>
 * <table>
 *   <tr><th>Fassade метод</th><th>Api вызовы</th></tr>
 *   <tr><td>{@code getChartTracks}</td><td>{@code getTrackFromJson + getArtistFromJson + getAlbumFromJson}</td></tr>
 *   <tr><td>{@code getTopTracksByArtist}</td><td>{@code getArtist → getTrackFromJson*}</td></tr>
 *   <tr><td>{@code getRelatedTracks}</td><td>{@code getTrack → getArtist → getRelatedArtist}</td></tr>
 * </table>
 *
 * <h2>URL Builder'ы (protected)</h2>
 * <p><b>Автогенерация search URL:</b>
 * <pre>{@code
 * /search/{type}?q={query}&amp;limit=1&amp;order=RANKING&amp;strict=false
 * type=track/artist, strict=false = fuzzy search
 * }</pre>
 * Расширяемо через наследование.
 * </p>
 *
 * <h2>Thread-safety</h2>
 * <p><b>Не thread-safe:</b> mutable {@link #proxy}, синхронные HTTP.
 * Каждый поток — свой Api экземпляр.
 * </p>
 *
 * <h2>Исключения</h2>
 * <table>
 *   <tr><th>Метод</th><th>Исключения</th></tr>
 *   <tr><td>Поиск</td><td>IOException + ParseException</td></tr>
 *   <tr><td>FromJson</td><td>ParseException (null JSON)</td></tr>
 * </table>
 *
 * @see Deezer фасад над этим классом
 * @see JsonProcess парсер JSON→String[]
 * @see Request HTTP клиент с proxy
 * @author mizuka-std
 */
public class Api {
    /**
     * <h3>Неизменяемый базовый URL</h3>
     * Фиксированный endpoint Deezer API, задается только в конструкторе.
     * <p>
     * <b>Назначение:</b> используется всеми {@link #buildUrlToGetArtist}, {@link #buildUrlToGetTrack},
     * передается в {@link Deezer} фасад.
     * </p>
     * <p>
     * <b>Иммутабельность:</b>
     * <ul>
     *   <li>{@code private final} — присвоение только в конструкторе</li>
     *   <li>{@link String} — неизменяемый тип</li>
     *   <li>Нет setter'а — защищено от случайной мутации</li>
     * </ul>
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li>{@code "https://api.deezer.com/"} — production</li>
     *   <li>{@code "https://api-staging.deezer.com/"} — staging</li>
     *   <li>{@code "http://localhost:3000/mock/"} — mock server</li>
     * </ul>
     * </p>
     * <p>
     * <b>Thread-safe:</b> immutable поле, безопасно для чтения из любого потока.
     * </p>
     */
    private final String url;
    /**
     * <h3>HTTP Proxy конфигурация</h3>
     * Опциональный прокси-сервер для обхода блокировок/кэширования.
     * <p>
     * <b>Инициализация:</b> {@code null} по умолчанию (direct connection).
     * </p>
     * <p>
     * <b>Fluent setter:</b> {@link #setProxy(Proxy)} возвращает {@code this}.
     * </p>
     * <p>
     * <b>Применение:</b> автоматически используется в {@link #buildRequest(URL)}.
     * <pre>{@code
     * Api api = new Api("https://api.deezer.com/")
     *     .setProxy(new Proxy(Proxy.Type.HTTP,
     *         new InetSocketAddress("proxy.corp", 8080)));
     * }</pre>
     * </p>
     * <p>
     * <b>Поддерживаемые типы:</b> {@link Proxy.Type#HTTP}, {@link Proxy.Type#SOCKS}.
     * </p>
     * <p>
     * <b>Thread-safety:</b> mutable поле! setter НЕ thread-safe.
     * </p>
     * <p>
     * <b>Readonly геттер:</b> {@link #getProxy()} для чтения текущей конфигурации.
     * </p>
     */
    private Proxy proxy;
    /**
     * <h3>Создание API адаптера</h3>
     * Инициализирует неизменяемый {@link #url}, proxy = {@code null}.
     * <p>
     * <b>Валидация:</b> НЕ выполняется. {@code null} url → NPE позже.
     * </p>
     * <p>
     * <b>Типичные вызовы:</b>
     * <pre>{@code
     * // Deezer фасад (default)
     * new Api("https://api.deezer.com/");
     *
     * // Custom/тесты
     * new Api("http://localhost:3000/");
     *
     * // Proxy-ready
     * new Api(deezerUrl).setProxy(corpProxy);
     * }</pre>
     * </p>
     * <p>
     * <b>Иммутабельность:</b> только {@link #url} final. Proxy mutable через fluent setter.
     * </p>
     *
     * @param url базовый URL (<a href="https://api.deezer.com/">...</a>)
     */
    public Api(String url) {
        this.url = url;
    }
    /**
     * <h3>Текущая proxy конфигурация</h3>
     * Readonly доступ к установленному прокси.
     * <p>
     * <b>Возвращает:</b> {@code null} (direct) или {@link Proxy} объект.
     * </p>
     * <p>
     * <b>Применение:</b>
     * <pre>{@code
     * Proxy current = api.getProxy();
     * if(current != null) {
     *     logger.info("Using proxy: " + current.type());
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Thread-safe:</b> чтение mutable поля (используйте synchronized если multi-thread).
     * </p>
     *
     * @return текущий proxy или null
     */
    public Proxy getProxy() {
        return proxy;
    }
    /**
     * <h3>Установка HTTP/SOCKS прокси (Fluent API)</h3>
     * Настраивает прокси для всех последующих {@link #buildRequest(URL)} вызовов.
     * <p>
     * <b>Автоматическая активация:</b> proxy применяется в {@link Request#setProxy(Proxy)}.
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <pre>{@code
     * // Корпоративный прокси
     * api.setProxy(new Proxy(Proxy.Type.HTTP,
     *     new InetSocketAddress("proxy.corp", 3128)));
     *
     * // SOCKS5
     * api.setProxy(new Proxy(Proxy.Type.SOCKS,
     *     new InetSocketAddress("socks5.tor", 9050)));
     *
     * // Отключение
     * api.setProxy(null); // direct connection
     * }</pre>
     * </p>
     * <p>
     * <b>Thread-safety:</b> НЕ thread-safe! mutable {@link #proxy}.
     * </p>
     * <p>
     * <b>Fluent:</b> возвращает {@code this} для цепочки.
     * </p>
     *
     * @param proxy HTTP/SOCKS прокси или null (direct)
     * @return {@code this} для fluent API
     */
    public Api setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * <h3>Фабрика Request с автоматической proxy настройкой</h3>
     * Создает настроенный под текущий {@link #proxy} HTTP-запрос.
     * <p>
     * <b>Логика (пошагово):</b>
     * <ol>
     *   <li><b>Базовый Request:</b> {@code new Request(url)} — без proxy</li>
     *   <li><b>Proxy check:</b> {@code if(proxy != null)}</li>
     *   <li><b>Применение:</b> {@code requestToBody.setProxy(proxy)}</li>
     *   <li><b>Возврат:</b> готовый Request для {@code .send()}</li>
     * </ol>
     * </p>
     *
     * <h3>Применение в коде:</h3>
     * <pre>{@code
     * // В getTrack/getArtist
     * Request request = buildRequest(url);
     * StringBuilder body = request.send().getBody();
     *
     * // Proxy автоматически применяется
     * }</pre>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * Api api = new Api("https://api.deezer.com/").setProxy(corpProxy);
     *
     * // Request 1: с прокси
     * Request req1 = api.buildRequest(trackUrl);
     * // req1.proxy == corpProxy
     *
     * // Request 2: без прокси
     * api.setProxy(null);
     * Request req2 = api.buildRequest(artistUrl);
     * // req2.proxy == null
     * }</pre>
     *
     * <h3>Преимущества архитектуры:</h3>
     * <ul>
     *   <li><b>DRY:</b> proxy логика в одном месте</li>
     *   <li><b>Fluent:</b> api.setProxy() → все buildRequest() работают</li>
     *   <li><b>Immutable URL:</b> Request(url) не мутирует входной URL</li>
     * </ul>
     *
     * <h3>Thread-safety:</h3>
     * <p><b>НЕ thread-safe:</b> читает mutable {@link #proxy}.
     * Каждый поток — свой Api.
     * </p>
     *
     * <h3>Ошибки:</h3>
     * <ul>
     *   <li>{@code url == null} → NPE в {@link Request#Request(URL)}</li>
     *   <li>{@code proxy.setProxy()} → RuntimeException от Request impl</li>
     * </ul>
     *
     * <p>
     * <b>Используется:</b> всеми поисковыми методами ({@link #getTrack}, {@link #getArtist}).
     * </p>
     *
     * @param url целевой URL (из {@link #buildUrlToGetTrack} / {@link #buildUrlToGetArtist})
     * @return Request с proxy (если установлен)
     * @throws NullPointerException если url=null
     */
    public Request buildRequest(URL url) {
        Request requestToBody = new Request(url);

        if(proxy != null)
            requestToBody.setProxy(proxy);

        return requestToBody;
    }

    /**
     * <h3>Поиск трека по имени → обогащенный объект</h3>
     * Находит первый (самый релевантный) трек + Artist + Album. **Основной поиск треков**.
     * <p>
     * <b>URL:</b> {@link #buildUrlToGetTrack(String)} → {@code /search/track?q={query}&limit=1&order=RANKING&strict=false}
     * </p>
     *
     * <h3>Полная последовательность:</h3>
     * <ol>
     *   <li><b>URL:</b> {@code buildUrlToGetTrack(query)} → URI → URL</li>
     *   <li><b>Request:</b> {@link #buildRequest(URL)} (с proxy если установлен)</li>
     *   <li><b>HTTP:</b> {@code request.send().getBody()} → StringBuilder JSON</li>
     *   <li><b>Парсинг:</b>
     *     <ol>
     *       <li>{@code JsonProcess.getJsonItem(body, "data")} → data объект</li>
     *       <li>{@code JsonProcess.getJsonArray(data)} → {@code String[] tracks}</li>
     *       <li>{@code trackJson = tracks[0]} — **первый результат** (RANKING)</li>
     *     </ol>
     *   </li>
     *   <li><b>Маппинг:</b>
     *     <ol>
     *       <li>{@link #getTrackFromJson(String)}} → базовый Track</li>
     *       <li>{@code getJsonItem("artist")} → artist JSON → {@link #getArtistFromJson(String)}} → {@code setArtist()}</li>
     *       <li>То же для "album" → {@link #getAlbumFromJson(String)}} → {@code setAlbum()}</li>
     *     </ol>
     *   </li>
     * </ol>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * try {
     *     Track bohemian = api.getTrack("Bohemian Rhapsody");
     *     // Track{title="Bohemian Rhapsody", artist=Queen{id=123}, album=A Night at the Opera}
     *
     *     Track nirvana = api.getTrack("Smells Like Teen Spirit");
     *     // Nevermind альбом, полные данные Kurt Cobain как Artist
     *
     *     Track rare = api.getTrack("Queen live 1975");
     *     // Live Killers или концертный вариант
     * } catch (IOException | ParseException e) {
     *     // Ничего не найдено или API down
     * }
     * }</pre>
     *
     * <h3>Структура результата:</h3>
     * <pre>{@code
     * Track {
     *   id, title, title_short, duration, preview, rank,
     *   explicit_lyrics, explicit_content_*,
     *   artist: Artist {id, name, picture_medium, nb_fan},
     *   album: Album {id, title, cover_medium, tracklist}
     * }
     * }</pre>
     *
     * <h3>Поисковые особенности:</h3>
     * <ul>
     *   <li><b>Fuzzy:</b> strict=false — "boh rhaps" найдет Bohemian</li>
     *   <li><b>RANKING:</b> первый = самый популярный/релевантный</li>
     *   <li><b>limit=1:</b> только лидер поиска</li>
     *   <li><b>Обогащение:</b> 3 HTTP внутри (search + artist + album маппинг)</li>
     * </ul>
     *
     * <p>
     * <b>Ошибки:</b>
     * <ul>
     *   <li>{@link ArrayIndexOutOfBoundsException} если tracks пустой (не кидается!)</li>
     *   <li>{@link IOException} HTTP/search</li>
     *   <li>{@link ParseException} "data" или вложенные JSON</li>
     * </ul>
     * </p>
     * <p>
     * <b>Используется:</b> {@link deezer.Deezer#getRelatedTracks(String)}} как стартовая точка.
     * </p>
     *
     * @param query название трека/куплет/исполнитель
     * @return первый (лучший) трек с Artist/Album
     * @throws IOException HTTP/search
     * @throws ParseException JSON data/tracks[0]/artist/album
     */
    public Track getTrack(String query)
            throws IOException, ParseException
    {
        Request requestToBody = buildRequest(URI.create(buildUrlToGetTrack(query)).toURL());

        StringBuilder body = requestToBody
                .send()
                .getBody();

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

    /**
     * <h3>Поиск артиста по имени → базовый объект</h3>
     * Находит первого (самого релевантного) исполнителя. **Проще getTrack() — без обогащения**.
     * <p>
     * <b>URL:</b> {@link #buildUrlToGetArtist(String)} → {@code /search/artist?q={query}&limit=1&order=RANKING&strict=false}
     * </p>
     *
     * <h3>Компактный алгоритм (1 HTTP):</h3>
     * <ol>
     *   ><b>URL:</b> {@code buildUrlToGetArtist(query)} → URI → URL</li>
     *   ><b>Request:</b> {@link #buildRequest(URL)}.send().getBody() → JSON</li>
     *   ><b>Парсинг (однострочник):</b> 
     *     {@code JsonProcess.getJsonArray(JsonProcess.getJsonItem(body, "data"))[0]}</li>
     *   ><b>Маппинг:</b> {@link #getArtistFromJson(String)}} → готовый Artist</li>
     * </ol>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * try {
     *     Artist queen = api.getArtist("Queen");
     *     // Artist{id=123, name="Queen", nb_fan=5M, picture_medium="..."}
     *
     *     Artist radiohead = api.getArtist("Radiohead");
     *     // nb_album=12, radio=true, tracklist="/artist/456/top"
     *
     *     Artist rare = api.getArtist("Steve Vai"); 
     *     // Меньше фанов, но полные данные
     * } catch (IOException | ParseException e) {
     *     // Артист не найден
     * }
     * }</pre>
     *
     * <h3>Ключевые отличия от getTrack():</h3>
     * <table>
     *   <tr><th>Характеристика</th><th>getArtist()</th><th>getTrack()</th></tr>
     *   <tr><td>HTTP запросов</td><td>1</td><td>3 (search+artist+album)</td></tr>
     *   <tr><td>Парсинг</td><td>Однострочник</td><td>Цикл + вложенности</td></tr>
     *   <tr><td>Обогащение</td><td>Нет</td><td>Artist + Album</td></tr>
     *   <tr><td>Результат</td><td>Artist</td><td>Track+Artist+Album</td></tr>
     * </table>
     *
     * <h3>Структура Artist:</h3>
     * <pre>{@code
     * Artist {
     *   id, name, link, tracklist, type,
     *   nb_album, nb_fan, radio (boolean),
     *   picture_small/medium/big/xl/picture
     * }
     * }</pre>
     *
     * <h3>Поисковые параметры (из buildUrlToGetArtist):</h3>
     * <ul>
     *   >{@code limit=1} — только лидер</li>
     *   >{@code order=RANKING} — популярность/релевантность</li>
     *   >{@code strict=false} — fuzzy search</li>
     * </ul>
     *
     * <p>
     * <b>Используется:</b> 
     * {@link deezer.Deezer#getTopTracksByArtist(String)} ()}, {@link deezer.Deezer#getRelatedArtist(String, int)} ()}.
     * </p>
     *
     * <p>
     * <b>Ошибки:</b> {@code tracks[0]} при пустом результате → ArrayIndexOutOfBounds!
     * </p>
     *
     * @param query имя исполнителя ("Queen", "Radiohead", "Steve Vai")
     * @return первый (лучший) Artist по релевантности
     * @throws IOException HTTP/search
     * @throws ParseException JSON data/array[0]
     * @throws ArrayIndexOutOfBoundsException ничего не найдено
     */
    public Artist getArtist(String query)
            throws IOException, ParseException
    {
        StringBuilder body = buildRequest(URI.create(buildUrlToGetArtist(query)).toURL()).send().getBody();
        String artistJson = JsonProcess.getJsonArray(JsonProcess.getJsonItem(body.toString(), "data"))[0];

        return getArtistFromJson(artistJson);
    }
    /**
     * <h3>Deezer Track JSON → объект (полный маппинг)</h3>
     * Парсит все 15 полей трека без null-check (предполагает валидный JSON).
     * <p>
     * <b>Поля Track (Deezer API):</b>
     * <table>
     *   <tr><th>Поле</th><th>Тип</th><th>Назначение</th></tr>
     *   <tr><td>{@code id}</td><td>Long</td><td>Уникальный ID</td></tr>
     *   <tr><td>{@code readable}</td><td>Boolean</td><td>Доступен ли трек</td></tr>
     *   <tr><td>{@code title}, {@code title_short}</td><td>String</td><td>Названия</td></tr>
     *   <tr><td>{@code title_version}</td><td>String</td><td>Live/Remix версия</td></tr>
     *   <tr><td>{@code isrc}</td><td>String</td><td>ISRC код</td></tr>
     *   <tr><td>{@code link}</td><td>String</td><td>Deezer страница</td></tr>
     *   <tr><td>{@code duration}</td><td>int</td><td>Секунды</td></tr>
     *   <tr><td>{@code rank}</td><td>int</td><td>Позиция в чарте</td></tr>
     *   <tr><td>{@code explicit_*}</td><td>Boolean/int</td><td>18+ контент</td></tr>
     *   <tr><td>{@code preview}</td><td>String</td><td>30сек MP3</td></tr>
     *   <tr><td>{@code md5_image}, {@code type}</td><td>String</td><td>Метаданные</td></tr>
     * </table>
     * </p>
     *
     * <h3>Использование:</h3>
     * <pre>{@code
     * String json = "{\"id\":\"123\", \"title\":\"Bohemian Rhapsody\", ...}";
     * Track track = api.getTrackFromJson(json);
     * // Готовый объект для UI/плейлиста
     * }</pre>
     *
     * <p>
     * <b>Предполагает:</b> валидный JSON от Deezer, все поля присутствуют.
     * </p>
     * <p>
     * <b>Используется:</b> {@link deezer.Deezer} фасадом для списков.
     * </p>
     *
     * @param trackJson Deezer track объект JSON
     * @return заполненный Track (15 полей)
     * @throws ParseException getJsonItem() или type conversion
     * @throws NumberFormatException Long.parseLong()/int parse
     */
    public Track getTrackFromJson(String trackJson)
            throws ParseException
    {
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
    /**
     * <h3>Deezer Album JSON → объект (defensive null-check)</h3>
     * Парсит 10 полей альбома с полной защитой от null/empty.
     * <p>
     * <b>Особенности:</b>
     * <ul>
     *   <li><b>Strict валидация:</b> {@code null/empty → ParseException}</li>
     *   <li><b>Null coalescing:</b> {@code field != null ? value : default}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Поля Album:</b>
     * <table>
     *   <tr><th>Поле</th><th>Default</th></tr>
     *   <tr><td>{@code id}</td><td>0L</td></tr>
     *   <tr><td>{@code title}, {@code cover_*}</td><td>""</td></tr>
     *   <tr><td>{@code tracklist}, {@code type}</td><td>""</td></tr>
     * </table>
     * 5 размеров обложки: small/medium/big/xl + cover.
     * </p>
     *
     * @param albumJson Deezer album объект JSON
     * @return безопасный Album
     * @throws ParseException null/empty JSON или parse error
     */
    public Album getAlbumFromJson(String albumJson)
            throws ParseException
    {
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
    /**
     * <h3>Deezer Artist JSON → объект (defensive + сложная логика)</h3>
     * Самый полный маппер: 15 полей, picture 5 размеров, nb_* статистика.
     * <p>
     * <b>Особенности:</b>
     * <ul>
     *   <li><b>Strict:</b> {@code null/empty → ParseException}</li>
     *   <li><b>Boolean radio:</b> {@code "radio"!=null && Boolean.parseBoolean()}</li>
     *   <li><b>Статистика:</b> nb_album (альбомов), nb_fan (фанов)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Picture variants (5):</b> small/medium/big/xl + picture (default).
     * </p>
     * <p>
     * <b>Используется:</b> всеми методами (getArtist, фасад обогащение).
     * </p>
     *
     * @param artistJson Deezer artist объект JSON
     * @return заполненный Artist
     * @throws ParseException null/empty или conversion
     */
    public Artist getArtistFromJson(String artistJson)
            throws ParseException
    {
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
    /**
     * <h3>URL для поиска артиста (первый результат)</h3>
     * Стандартизированный search/artist endpoint с оптимальными параметрами.
     * <p>
     * <b>Шаблон:</b> {@code /search/artist?q={query}&limit=1&order=RANKING&strict=false}
     * </p>
     * <p>
     * <b>Параметры поиска:</b>
     * <table>
     *   <tr><th>Параметр</th><th>Значение</th><th>Эффект</th></tr>
     *   <tr><td>{@code q}</td><td>{query}</td><td>Fuzzy поиск имени</td></tr>
     *   <tr><td>{@code limit}</td><td>1</td><td>Только лидер</td></tr>
     *   <tr><td>{@code order}</td><td>RANKING</td><td>Популярность/релевантность</td></tr>
     *   <tr><td>{@code strict}</td><td>false</td><td>Fuzzy matching</td></tr>
     * </table>
     * </p>
     * <p>
     * <b>Пример:</b> {@code buildUrlToGetArtist("Queen")} → {@code /search/artist?q=Queen&limit=1&...}
     * </p>
     * <p>
     * <b>Protected:</b> для переопределения в наследниках.
     * <pre>{@code
     * protected String buildUrlToGetArtist(String query) {
     *     return super.buildUrlToGetArtist(query) + "&country=US";
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Используется:</b> {@link #getArtist(String)}.
     * </p>
     *
     * @param query имя исполнителя
     * @return готовый search URL
     */
    protected String buildUrlToGetArtist(String query) {
        return new QueryBuilder(url)
                .path("search", "artist")
                .param("q", query)
                .param("limit", 1)
                .param("order", "RANKING")
                .param("strict", false)
                .build();
    }
    /**
     * <h3>URL для поиска трека (первый результат)</h3>
     * Идентичный artist, но для track endpoint.
     * <p>
     * <b>Шаблон:</b> {@code /search/track?q={query}&limit=1&order=RANKING&strict=false}
     * </p>
     * <p>
     * <b>Поисковые возможности:</b>
     * <ul>
     *   <li>Название: "Bohemian Rhapsody"</li>
     *   <li>Исполнитель: "Queen"</li>
     *   <li>Куплет: "Is this the real life"</li>
     *   <li>Комбо: "Queen live"</li>
     * </ul>
     * </p>
     * <p>
     * <b>RANKING порядок:</b> популярность &gt; релевантность &gt; новизна.
     * </p>
     * <p>
     * <b>Protected override:</b> добавление фильтров (genre, duration...).
     * </p>
     * <p>
     * <b>Используется:</b> {@link #getTrack(String)}.
     * </p>
     *
     * @param query название/исполнитель/куплет
     * @return готовый track search URL
     */
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
