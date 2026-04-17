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
import java.util.stream.Stream;

/**
 * <h1>Deezer</h1>
 * Основной фасад для работы с Deezer API: чарты, поиск, рекомендации, связанные артисты/треки.
 * <p>
 * <b>Архитектура:</b> Facade Pattern над низкоуровневым {@link Api}. Инкапсулирует:
 * <ul>
 *   <li>Сборку URL через {@link QueryBuilder}.</li>
 *   <li>HTTP-запросы через {@link rf.ebanina.utils.network.Request}.</li>
 *   <li>Парсинг JSON через {@link rf.ebanina.utils.formats.json.JsonProcess}.</li>
 *   <li>Маппинг в модели {@link deezer.models.Track}, {@link deezer.models.Artist}.</li>
 * </ul>
 * </p>
 *
 * <h2>Основные возможности</h2>
 * <ul>
 *   <li><b>Чарты:</b> {@link #getChartTracks(String, int)} — топ треки по региону.</li>
 *   <li><b>Топ артиста:</b> {@link #getTopTracksByArtist(String, int)} — хиты конкретного артиста.</li>
 *   <li><b>Поиск:</b> {@link #searchTracks(String, int)} — поиск по названию/артисту.</li>
 *   <li><b>Рекомендации:</b> {@link #getRelatedTracks(String, int, int, int, int, int)} — мультиметодный алгоритм.</li>
 *   <li><b>Связанные артисты:</b> {@link #getRelatedArtist(String, int)} — похожие исполнители.</li>
 *   <li><b>Цепочка артистов:</b> {@link #getArtistChain(String, int, int)} — рекурсивное расширение графа.</li>
 * </ul>
 *
 * <h2>Жизненный цикл объекта</h2>
 * <ol>
 *   <li><b>Создание:</b> {@code new Deezer()} — использует default {@link #apiUrl} + внутренний {@link Api}.</li>
 *   <li><b>Кастомизация:</b> конструкторы с custom {@link #apiUrl}, {@link Api} для тестов/proxy.</li>
 *   <li><b>Запросы:</b> каждый метод делает синхронный HTTP GET → парсит → возвращает List&lt;Model&gt;.</li>
 *   <li><b>Исключения:</b> {@link IOException} (сеть), {@link ParseException} (JSON).</li>
 * </ol>
 *
 * <h2>Общий шаблон методов (Tracks)</h2>
 * <pre>{@code
 * 1. Сборка URL: QueryBuilder → Request(URI.create(url).toURL())
 * 2. HTTP GET: .send().getBody() → StringBuilder JSON
 * 3. Парсинг: JsonProcess.getJsonItem(body, "data") → JsonProcess.getJsonArray()
 * 4. Маппинг: для каждого jsonTrack:
 *    Track track = api.getTrackFromJson(jsonTrack)
 *    track.setArtist(api.getArtistFromJson(...))
 *    track.setAlbum(api.getAlbumFromJson(...))
 * 5. Возврат: ArrayList&lt;Track&gt;
 * }</pre>
 *
 * <h2>Конструкторы и настройка</h2>
 * <pre>{@code
 * // 1. По умолчанию (api.deezer.com + внутренний Api)
 * Deezer deezer = new Deezer();
 *
 * // 2. Custom URL (staging/test)
 * Deezer deezer2 = new Deezer("https://api-staging.deezer.com/");
 *
 * // 3. Custom Api (mock для тестов)
 * Api mockApi = new MockApi();
 * Deezer deezer3 = new Deezer("https://api.deezer.com/", mockApi);
 * }</pre>
 *
 * <h2>Чарты (getChartTracks)</h2>
 * <pre>{@code
 * try {
 *     // Топ-50 мирового чарта
 *     List<Track> top50 = deezer.getChartTracks(); // region="0", limit=50
 *
 *     // Французский чарт, 25 треков
 *     List<Track> french25 = deezer.getChartTracks("301", 25);
 *
 *     // Только лимит (мировой чарт)
 *     List<Track> top10 = deezer.getChartTracks(10);
 * } catch (IOException | ParseException e) {
 *     // Сеть/JSON ошибки
 * }
 * }</pre>
 *
 * <h2>Топ треки артиста</h2>
 * <pre>{@code
 * try {
 *     // Топ-25 Queen
 *     List<Track> queenHits = deezer.getTopTracksByArtist("Queen");
 *
 *     // Топ-10 Radiohead
 *     List<Track> radioheadTop10 = deezer.getTopTracksByArtist("Radiohead", 10);
 * } catch (IOException | ParseException e) {
 *     // 404 если артист не найден
 * }
 * }</pre>
 *
 * <h2>Поиск треков</h2>
 * <pre>{@code
 * try {
 *     // Поиск "Bohemian Rhapsody", максимум 50 результатов
 *     List<Track> bohemian = deezer.searchTracks("Bohemian Rhapsody", 50);
 *
 *     // Поиск по исполнителю
 *     List<Track> metallica = deezer.searchTracks("Metallica", 25);
 * } catch (IOException | ParseException e) {
 *     // Пустой результат если ничего не найдено
 * }
 * }</pre>
 *
 * <h2>Базовые рекомендации</h2>
 * <pre>{@code
 * try {
 *     // Простые рекомендации для трека "Smells Like Teen Spirit"
 *     List<Track> nirvanaRelated = deezer.getRelatedTracks("Smells Like Teen Spirit");
 *     // Алгоритм: 5 топ Nirvana + 2x3 related artists + 5 поиск
 * } catch (IOException | ParseException e) {
 *     // Составной метод, кидает любую подошибку
 * }
 * }</pre>
 *
 * <h2>Расширенные рекомендации</h2>
 * <pre>{@code
 * try {
 *     List<Track> advancedRecs = deezer.getRelatedTracks(
 *         "Hotel California",           // исходный трек
 *         10,                           // топ Eagles (10)
 *         5,                            // топ related artists (5)
 *         3,                            // поиск по имени (3)
 *         20,                           // чарт (20)
 *         2                             // глубина цепочки артистов
 *     );
 *     // Итого: ~10 + 5x5 + 6x3 + 20 + цепочка = 70+ уникальных треков
 * } catch (Exception e) {
 *     // Exception из-за рекурсии getArtistChain
 * }
 * }</pre>
 *
 * <h2>Связанные артисты</h2>
 * <pre>{@code
 * try {
 *     // 25 похожих на The Beatles
 *     List<Artist> beatlesRelated = deezer.getRelatedArtist("The Beatles");
 *
 *     // 10 похожих на Daft Punk
 *     List<Artist> daftRelated = deezer.getRelatedArtist("Daft Punk", 10);
 * } catch (IOException | ParseException e) {
 *     // Каждый Artist содержит полные данные (name, id, picture)
 * }
 * }</pre>
 *
 * <h2>Рекурсивная цепочка артистов</h2>
 * <pre>{@code
 * try {
 *     // Цепочка: Nirvana → related → related (глубина 2, по 3 трека)
 *     List<Track> nirvanaChain = deezer.getArtistChain("Nirvana", 2, 3);
 *     // ~3 (Nirvana) + 3x3 (related1) + 9x3 (related2) = 39 треков
 * } catch (Exception e) {
 *     // Экспоненциальный рост! depth>3 = сотни запросов
 * }
 * }</pre>
 *
 * <h2>Конфигурация</h2>
 * <ul>
 *   <li>{@link #apiUrl}: protected, по умолчанию {@code "https://api.deezer.com/"}. Легко переопределить в наследниках.</li>
 *   <li>{@link #api}: private, инкапсулирует маппинг JSON→модели. Инжектируется через конструктор.</li>
 * </ul>
 *
 * <h2>Рекомендации (getRelatedTracks)</h2>
 * <p><b>Простой:</b> {@link #getRelatedTracks(String)} — фиксированные лимиты, базовый алгоритм.</p>
 * <p><b>Расширенный:</b> {@link #getRelatedTracks(String, int, int, int, int, int)} — настраиваемые лимиты + depth.</p>
 * <p><b>Алгоритм:</b>
 * <ol>
 *   <li>Топ треки исходного артиста.</li>
 *   <li>Топ треки связанных артистов (getRelatedArtist).</li>
 *   <li>Поиск по имени артиста + связанных.</li>
 *   <li>Чарт (опционально).</li>
 *   <li>Рекурсивная цепочка артистов (depth &gt; 0).</li>
 *   <li>{@link Collectors#toList()} с {@link Stream#distinct()} для уникальности.</li>
 * </ol>
 * </p>
 *
 * <h2>Рекурсивная цепочка (ArtistChain)</h2>
 * <p>{@link #getArtistChainRecursive}: BFS-подобное расширение графа артистов.
 * <ul>
 *   <li>Базовый случай: currentDepth &ge; maxDepth → только топ треки текущего.</li>
 *   <li>Рекурсия: для 3 related artists вызывает себя с depth+1.</li>
 *   <li>Конкатенация: все треки собираются в один список.</li>
 * </ul>
 * <b>Осторожно:</b> экспоненциальный рост (depth=3 → ~27 artists → ~135 tracks).
 * </p>
 *
 * <h2>Thread-safety</h2>
 * <p><b>Не thread-safe:</b> {@link #api} может быть mutable, HTTP-запросы синхронные.
 * Каждый поток — свой экземпляр {@link Deezer}.</p>
 *
 * <h2>Зависимости</h2>
 * <ul>
 *   <li>{@link QueryBuilder}: сборка URL.</li>
 *   <li>{@link rf.ebanina.utils.network.Request}: HTTP-клиент.</li>
 *   <li>{@link rf.ebanina.utils.formats.json.JsonProcess}: JSON-парсер (StringBuilder → String[]).</li>
 *   <li>{@link deezer.models}: доменные модели с сеттерами.</li>
 *   <li>{@link Api}: внутренний маппер JSON→модели (getTrackFromJson, getArtistFromJson, getAlbumFromJson).</li>
 * </ul>
 *
 * <h2>Исключения</h2>
 * <ul>
 *   <li>{@link IOException}: сетевая ошибка, таймаут, 4xx/5xx.</li>
 *   <li>{@link ParseException}: некорректный JSON от Deezer API.</li>
 *   <li>{@link Exception}: в расширенных методах (getArtistChain).</li>
 * </ul>
 *
 * <h2>Лимиты по умолчанию</h2>
 * <table>
 *   <tr><th>Метод</th><th>Default limit</th></tr>
 *   <tr><td>getChartTracks()</td><td>50 (region="0")</td></tr>
 *   <tr><td>getTopTracksByArtist(artist)</td><td>25</td></tr>
 *   <tr><td>getRelatedArtist(query)</td><td>25</td></tr>
 * </table>
 *
 * @see Api для низкоуровневого маппинга
 * @see QueryBuilder для URL-билдинга
 * @see rf.ebanina.utils.formats.json.JsonProcess для парсинга
 * @author mizuka-std
 * @version 1.0
 * @since 2026
 */
public class Deezer
{
    /**
     * <h3>Базовый URL Deezer API</h3>
     * Конфигурируемый endpoint для всех запросов (задается в конструкторах).
     * <p>
     * <b>Значение по умолчанию:</b> {@code "https://api.deezer.com/"} — production API.
     * </p>
     * <p>
     * <b>Protected доступ:</b> для переопределения в наследниках:
     * <pre>{@code
     * public class DeezerPro extends Deezer {
     *     protected String apiUrl = "https://api.deezer.com/v2/"; // новая версия
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Использование:</b> передается в {@link QueryBuilder#QueryBuilder(String)} для каждого запроса.
     * <ul>
     *   <li>Production: {@code "https://api.deezer.com/"}</li>
     *   <li>Staging: {@code "https://api-staging.deezer.com/"}</li>
     *   <li>Mock: {@code "http://localhost:8080/mock/"}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Иммутабельность:</b> {@link String} — неизменяемый, но поле само по себе mutable.
     * Рекомендуется устанавливать только в конструкторе.
     * </p>
     * <p>
     * <b>Геттер:</b> {@link #getApiUrl()} для чтения текущего значения.
     * </p>
     */
    protected String apiUrl = "https://api.deezer.com/";
    /**
     * <h3>Низкоуровневый API-адаптер</h3>
     * Инкапсулирует маппинг JSON ↔ модели и поиск по ID/имени.
     * <p>
     * <b>Инициализация:</b>
     * <ul>
     *   <li>По умолчанию: {@code new Api(apiUrl)} в поле.</li>
     *   <li>Инжектируется через конструкторы для тестирования/mocking.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Используемые методы:</b>
     * <table>
     *   <tr><th>Метод Api</th><th>Используется в Deezer</th></tr>
     *   <tr><td>{@code getTrackFromJson(String)}</td><td>Все track-методы</td></tr>
     *   <tr><td>{@code getArtistFromJson(String)}</td><td>Обогащение Track</td></tr>
     *   <tr><td>{@code getAlbumFromJson(String)}</td><td>Обогащение Track</td></tr>
     *   <tr><td>{@code getArtist(String)}</td><td>getTopTracksByArtist, getRelatedArtist</td></tr>
     *   <tr><td>{@code getTrack(String)}</td><td>getRelatedTracks</td></tr>
     * </table>
     * </p>
     * <p>
     * <b>Private доступ:</b> полная инкапсуляция. Внешний код работает только через публичные методы Deezer.
     * </p>
     * <p>
     * <b>Thread-safety:</b> зависит от реализации Api. По умолчанию — не thread-safe.
     * </p>
     * <p>
     * <b>Геттер:</b> {@link #getApi()} для доступа (readonly).
     * </p>
     */
    private Api api = new Api(apiUrl);
    /**
     * <h3>Конструктор по умолчанию</h3>
     * Создает полностью готовый к использованию экземпляр с production настройками.
     * <p>
     * <b>Инициализация (ленивая):</b>
     * <ul>
     *   <li>{@link #apiUrl} = {@code "https://api.deezer.com/"} (production).</li>
     *   <li>{@link #api} = {@code new Api(apiUrl)} — внутренний адаптер.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Когда использовать:</b>
     * <pre>{@code
     * Deezer deezer = new Deezer(); // Готово к работе
     * List<Track> charts = deezer.getChartTracks();
     * }</pre>
     * </p>
     * <p>
     * <b>Плюсы:</b> Zero-configuration, типичный use-case.
     * </p>
     * <p>
     * <b>Минусы:</b> нельзя поменять URL без рефлексии.
     * </p>
     * <p>
     * <b>Thread-safety:</b> безопасно вызывать из любого потока.
     * </p>
     */
    public Deezer() {}
    /**
     * <h3>Конструктор с инжекцией Api</h3>
     * Позволяет подставить custom/mock Api, сохраняя production URL.
     * <p>
     * <b>Инициализация:</b>
     * <ul>
     *   <li>{@link #apiUrl} = {@code "https://api.deezer.com/"} (по умолчанию, неизменно).</li>
     *   <li>{@link #api} = переданный параметр (перезаписывает default).</li>
     * </ul>
     * </p>
     * <p>
     * <b>Когда использовать:</b>
     * <pre>{@code
     * // Unit-тесты с mock
     * Api mockApi = Mockito.mock(Api.class);
     * when(mockApi.getTrackFromJson(any())).thenReturn(mockTrack);
     * Deezer testable = new Deezer(mockApi);
     *
     * // Proxy Api для кэширования/ретри
     * Api cachingApi = new CachingApi();
     * Deezer cachedDeezer = new Deezer(cachingApi);
     * }</pre>
     * </p>
     * <p>
     * <b>Валидация:</b> НЕ выполняется. Null Api приведет к NPE в методах.
     * </p>
     * <p>
     * <b>Dependency Injection:</b> Следует принципам DI (constructor injection).
     * </p>
     * <p>
     * <b>Ограничение:</b> apiUrl остается production. Для custom URL — третий конструктор.
     * </p>
     *
     * @param api кастомный Api-адаптер (mock, proxy, caching и т.д.)
     * @throws NullPointerException если api = null (лениво, при первом использовании)
     */
    public Deezer(Api api) {
        this.api = api;
    }
    /**
     * <h3>Полностью кастомизируемый конструктор</h3>
     * Максимальная гибкость: произвольный URL + инжектированный Api-адаптер.
     * <p>
     * <b>Инициализация:</b>
     * <ul>
     *   <li>{@link #apiUrl} = переданный параметр (полное переопределение).</li>
     *   <li>{@link #api} = переданный параметр (полное переопределение).</li>
     * </ul>
     * </p>
     * <p>
     * <b>Когда использовать:</b>
     * <pre>{@code
     * // Staging + caching proxy
     * Api cachingApi = new CachingApi("https://api.deezer.com/");
     * Deezer staging = new Deezer("https://api-staging.deezer.com/", cachingApi);
     *
     * // Локальный mock-сервер
     * Deezer mock = new Deezer("http://localhost:3000/deezer", mockApi);
     *
     * // Альтернативный регион (JP)
     * Deezer jpDeezer = new Deezer("https://jp.api.deezer.com/", defaultApi);
     * }</pre>
     * </p>
     * <p>
     * <b>Валидация:</b> НЕ выполняется. Null значения → NPE при первом использовании.
     * </p>
     * <p>
     * <b>Комбинация:</b> объединяет возможности двух предыдущих конструкторов.
     * Идеален для продвинутых сценариев (тесты E2E, региональные API, proxy).
     * </p>
     *
     * @param apiUrl произвольный базовый URL API
     * @param api произвольный Api-адаптер
     * @throws NullPointerException если любой параметр null (лениво)
     */
    public Deezer(String apiUrl, Api api) {
        this.apiUrl = apiUrl;
        this.api = api;
    }
    /**
     * <h3>Чарты Deezer (упрощенный доступ)</h3>
     * Топ-50 треков мирового чарта (region="0"). Делегирует полной перегрузке.
     * <p>
     * <b>Фиксированные параметры:</b>
     * <ul>
     *   <li>{@code region} = {@code "0"} — глобальный чарт (не локализованный).</li>
     *   <li>{@code limit} = {@code 50} — стандартный размер топа.</li>
     * </ul>
     * </p>
     * <p>
     * <b>URL запроса:</b> {@code GET /chart/0/tracks?limit=50}
     * </p>
     * <p>
     * <b>Когда использовать:</b>
     * <pre>{@code
     * try {
     *     List<Track> worldTop50 = deezer.getChartTracks();
     *     Track number1 = worldTop50.get(0); // #1 в мире прямо сейчас
     * } catch (IOException | ParseException e) {
     *     // Нет интернета или Deezer вернул битый JSON
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Результат:</b> List&lt;Track&gt; с полностью обогащенными объектами:
     * <ul>
     *   <li>Каждый Track содержит {@code artist}, {@code album} (полные объекты).</li>
     *   <li>Готово к немедленному использованию в UI/плейлистах.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Делегирование:</b> {@code return getChartTracks("0", 50)} — DRY принцип.
     * </p>
     *
     * @return топ-50 мировых хитов с Artist/Album
     * @throws IOException сетевая ошибка (DNS, таймаут, 5xx)
     * @throws ParseException Deezer вернул некорректный JSON
     * @see #getChartTracks(int) для кастомного лимита
     * @see #getChartTracks(String, int) полная перегрузка
     */
    public List<Track> getChartTracks() throws IOException, ParseException {
        return getChartTracks("0", 50);
    }
    /**
     * <h3>Чарты с кастомным лимитом (мировой)</h3>
     * Топ треков мирового чарта с произвольным количеством результатов.
     * <p>
     * <b>Фиксированный параметр:</b> {@code region} = {@code "0"} (глобальный чарт).
     * </p>
     * <p>
     * <b>URL запроса:</b> {@code GET /chart/0/tracks?limit={limit}}
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <pre>{@code
     * List<Track> top10World = deezer.getChartTracks(10);    // Топ-10 мира
     * List<Track> top100World = deezer.getChartTracks(100);  // Топ-100 мира
     * List<Track> singleHit = deezer.getChartTracks(1);      // Только #1
     * }</pre>
     * </p>
     * <p>
     * <b>Делегирование:</b> {@code return getChartTracks("0", limit)} — передает в полную реализацию.
     * </p>
     * <p>
     * <b>Ограничения Deezer API:</b> limit обычно 1-50, большие значения могут быть усечены.
     * </p>
     *
     * @param limit количество треков (1-500, зависит от API лимитов)
     * @return List&lt;Track&gt; с Artist/Album для каждого
     * @throws IOException сетевая ошибка
     * @throws ParseException JSON-парсинг
     * @see #getChartTracks() для default (50)
     * @see #getChartTracks(String, int) для region
     */
    public List<Track> getChartTracks(
            int limit
    ) throws IOException, ParseException {
        return getChartTracks("0", limit);
    }
    /**
     * <h3>Чарты по региону (полная реализация)</h3>
     * Топ треков конкретного региона с заданным лимитом. **Основная реализация** для всех перегрузок.
     * <p>
     * <b>URL шаблон:</b> {@code GET /chart/{region}/tracks?limit={limit}}
     * </p>
     * <p>
     * <b>Регионы Deezer (примеры):</b>
     * <table>
     *   <tr><th>Код</th><th>Регион</th></tr>
     *   <tr><td>"0"</td><td>Мир</td></tr>
     *   <tr><td>"301"</td><td>Франция</td></tr>
     *   <tr><td>"304"</td><td>Германия</td></tr>
     *   <tr><td>"316"</td><td>США</td></tr>
     *   <tr><td>"320"</td><td>Великобритания</td></tr>
     * </table>
     * Полный список: <a href="https://developers.deezer.com/api/chart">Deezer Docs</a>
     * </p>
     *
     * <h3>Алгоритм (пошагово):</h3>
     * <ol>
     *   <li><b>URL:</b> {@link QueryBuilder} → {@code /chart/301/tracks?limit=25}</li>
     *   <li><b>HTTP:</b> {@link rf.ebanina.utils.network.Request} → {@link StringBuilder} JSON</li>
     *   <li><b>Парсинг:</b> {@code JsonProcess.getJsonItem(body, "data")} → объект "data"</li>
     *   <li><b>Массив:</b> {@code JsonProcess.getJsonArray(dataObj)} → {@code String[] jsonTracks}</li>
     *   <li><b>Маппинг (цикл):</b> для каждого JSON-трека:
     *     <ol>
     *       <li>{@code api.getTrackFromJson()} → базовый Track</li>
     *       <li>{@code JsonProcess.getJsonItem(trackJson, "artist")} → artist JSON</li>
     *       <li>{@code api.getArtistFromJson()} → Artist → {@code track.setArtist()}</li>
     *       <li>Аналогично для "album" → {@code track.setAlbum()}</li>
     *       <li>{@code tracksList.add(track)}</li>
     *     </ol>
     *   </li>
     * </ol>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * // Мировой топ-25
     * List<Track> world25 = deezer.getChartTracks("0", 25);
     *
     * // Французский топ-10
     * List<Track> frenchTop10 = deezer.getChartTracks("301", 10);
     *
     * // Британский чарт, 50 треков
     * List<Track> uk50 = deezer.getChartTracks("320", 50);
     * }</pre>
     *
     * <h3>Структура ответа Track:</h3>
     * <pre>{@code
     * Track {
     *   id, title, duration, preview, // базовые поля
     *   artist: Artist { id, name, picture }, // полный объект!
     *   album: Album { id, title, cover }     // полный объект!
     * }
     * }</pre>
     *
     * <p>
     * <b>Исключения:</b>
     * <ul>
     *   <li>{@link IOException}: DNS, таймаут, 429 Rate Limit, 5xx</li>
     *   <li>{@link ParseException}: битый JSON от Deezer</li>
     * </ul>
     * </p>
     * <p>
     * <b>Производительность:</b> N+2 HTTP запроса (1 чарт + N artist/album маппинг).
     * </p>
     *
     * @param region код региона ("0"=мир, "301"=FR, "316"=US и т.д.)
     * @param limit количество треков (1-50 типично)
     * @return List&lt;Track&gt; полностью обогащенных объектов
     * @throws IOException HTTP/сеть
     * @throws ParseException JSON data/array
     */
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
    /**
     * <h3>Топ треки артиста (стандартный лимит)</h3>
     * Возвращает топ-25 хитов артиста по поиску имени. Делегирует полной версии.
     * <p>
     * <b>Фиксированный параметр:</b> {@code limit} = {@code 25}.
     * </p>
     * <p>
     * <b>Когда использовать:</b>
     * <pre>{@code
     * try {
     *     List<Track> queenHits = deezer.getTopTracksByArtist("Queen");
     *     // Bohemian Rhapsody, We Will Rock You, ...
     *
     *     List<Track> metallicaTop = deezer.getTopTracksByArtist("Metallica");
     * } catch (IOException | ParseException e) {
     *     // Артист не найден или API недоступен
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Делегирование:</b> {@code return getTopTracksByArtist(artist, 25)}.
     * </p>
     *
     * @param artist имя артиста ("Queen", "The Beatles", "Nirvana")
     * @return топ-25 хитов с Artist/Album
     * @throws IOException сетевая ошибка или артист не найден (404)
     * @throws ParseException JSON-парсинг
     * @see #getTopTracksByArtist(String, int) для кастомного лимита
     */
    public List<Track> getTopTracksByArtist(
            String artist
    ) throws IOException, ParseException {
        return getTopTracksByArtist(artist, 25);
    }
    /**
     * <h3>Топ треки артиста (полная реализация)</h3>
     * Хиты конкретного исполнителя по ID с заданным лимитом.
     * <p>
     * <b>Двухэтапный алгоритм:</b>
     * <ol>
     *   <li><b>Поиск артиста:</b> {@code api.getArtist(artist)} → {@link Artist} с ID.</li>
     *   <li><b>Топ по ID:</b> {@code /artist/{ID}/top?limit={limit}} → треки.</li>
     * </ol>
     * </p>
     *
     * <h3>URL шаблон:</h3>
     * <pre>{@code
     * 1. Поиск: api.getArtist("Queen") → Artist{id=123}
     * 2. Топ: GET /artist/123/top?limit=25
     * }</pre>
     *
     * <h3>Полная последовательность (пошагово):</h3>
     * <ol>
     *   <li><b>ID артиста:</b> {@code api.getArtist(artist)} → {@code artist1.getId()}</li>
     *   <li><b>URL:</b> QueryBuilder → {@code /artist/123/top?limit=25}</li>
     *   <li><b>HTTP:</b> Request → StringBuilder JSON ответа</li>
     *   <li><b>Парсинг:</b> {@code "data" → String[] jsonTracks}</li>
     *   <li><b>Цикл маппинга (для каждого трека):</b>
     *     <ol>
     *       <li>{@code api.getTrackFromJson()} → базовый Track</li>
     *       <li>{@code getJsonItem("artist")} → вложенный JSON</li>
     *       <li>{@code api.getArtistFromJson()} → Artist → {@code setArtist()}</li>
     *       <li>То же для "album" → {@code setAlbum()}</li>
     *       <li>{@code tracksList.add(track)}</li>
     *     </ol>
     *   </li>
     * </ol>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * // Топ-10 Queen
     * List<Track> queenTop10 = deezer.getTopTracksByArtist("Queen", 10);
     *
     * // Все хиты Radiohead (API limit ~50)
     * List<Track> radioheadAll = deezer.getTopTracksByArtist("Radiohead", 50);
     *
     * // 5 хитов Daft Punk
     * List<Track> daft5 = deezer.getTopTracksByArtist("Daft Punk", 5);
     * }</pre>
     *
     * <h3>Особенности:</h3>
     * <ul>
     *   <li><b>Два HTTP-запроса:</b> 1 поиск артиста + 1 топ-лист.</li>
     *   <li><b>Кэширование:</b> зависит от {@link Api#getArtist(String)} реализации.</li>
     *   <li><b>Обогащение:</b> каждый Track содержит полный {@code artist}, {@code album}.</li>
     *   <li><b>Пустой результат:</b> если артист не найден → пустой список.</li>
     * </ul>
     *
     * <p>
     * <b>Исключения:</b>
     * <ul>
     *   <li>{@link IOException}: поиск артиста ИЛИ топ-лист</li>
     *   <li>{@link ParseException}: любой JSON (artist/top/tracks)</li>
     * </ul>
     * </p>
     *
     * @param artist имя для поиска (точное/частично)
     * @param limit количество хитов (1-50)
     * @return List&lt;Track&gt; топа артиста с вложенными объектами
     * @throws IOException два HTTP-запроса
     * @throws ParseException любой JSON
     */
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
    /**
     * <h3>Поиск треков по запросу</h3>
     * Находит треки по названию, исполнителю или комбинации. **Единственная перегрузка**.
     * <p>
     * <b>URL шаблон:</b> {@code GET /search/track?q={query}&amp;limit={limit}}
     * </p>
     * <p>
     * <b>Поддерживаемые запросы:</b>
     * <ul>
     *   <li>Название: {@code "Bohemian Rhapsody"}</li>
     *   <li>Исполнитель: {@code "Queen"}</li>
     *   <li>Комбинация: {@code "Queen live"}</li>
     *   <li>Жанр/альбом: {@code "Dark Side Moon"}</li>
     * </ul>
     * Deezer fuzzy search — находит близкие варианты.
     * </p>
     *
     * <h3>Алгоритм (один HTTP-запрос):</h3>
     * <ol>
     *   <li><b>URL:</b> QueryBuilder → {@code /search/track?q=Bohemian+Rhapsody&amp;limit=25}</li>
     *   <li><b>HTTP GET:</b> Request → StringBuilder сырой JSON</li>
     *   <li><b>Парсинг:</b> {@code JsonProcess.getJsonItem(body, "data")} → объект</li>
     *   <li><b>Массив:</b> {@code JsonProcess.getJsonArray()} → {@code String[] jsonTracks}</li>
     *   <li><b>Цикл обогащения:</b> для каждого результата поиска:
     *     <ol>
     *       <li>{@code api.getTrackFromJson()} → базовый Track</li>
     *       <li>{@code getJsonItem("artist")} → artist JSON → {@code api.getArtistFromJson()} → {@code setArtist()}</li>
     *       <li>То же для "album" → {@code setAlbum()}</li>
     *       <li>{@code tracksList.add()}</li>
     *     </ol>
     *   </li>
     * </ol>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * try {
     *     // Поиск хита
     *     List<Track> bohemian = deezer.searchTracks("Bohemian Rhapsody", 10);
     *
     *     // Артист + альбом
     *     List<Track> darkSide = deezer.searchTracks("Pink Floyd Dark Side", 25);
     *
     *     // Жанр/стиль
     *     List<Track> grunge = deezer.searchTracks("Nirvana", 50);
     *
     *     // Rare треки
     *     List<Track> bootlegs = deezer.searchTracks("Queen live 1975", 5);
     * } catch (IOException | ParseException e) {
     *     // Rate limit или поисковый индекс недоступен
     * }
     * }</pre>
     *
     * <h3>Качество поиска Deezer:</h3>
     * <ul>
     *   <li>Fuzzy matching — "boh rhaps" найдет Bohemian Rhapsody</li>
     *   <li>Релевантность: популярные треки выше</li>
     *   <li>Локализация: учитывает язык (но query на английском лучше)</li>
     *   <li>Limit 1-50 оптимально, большие = дубли/ремейки</li>
     * </ul>
     *
     * <h3>Структура Track (обогащенный):</h3>
     * <pre>{@code
     * Track {
     *   id, title, duration, explicit, preview,
     *   rank (позиция в поиске),
     *   artist: Artist {name, id, picture_medium},
     *   album: Album {title, cover_medium}
     * }
     * }</pre>
     *
     * <p>
     * <b>Производительность:</b> 1 HTTP + N маппинг (artist/album). Быстрее getTopTracksByArtist (без поиска ID).
     * </p>
     *
     * <p>
     * <b>Исключения:</b>
     * <ul>
     *   <li>{@link IOException}: HTTP 429 (rate limit), 5xx</li>
     *   <li>{@link ParseException}: битый JSON в "data" поле</li>
     * </ul>
     * </p>
     * <p>
     * <b>Пустой результат:</b> query слишком специфичный → пустой список.
     * </p>
     *
     * @param query поисковый запрос (название/исполнитель)
     * @param limit максимум результатов (1-50 оптимально)
     * @return List&lt;Track&gt; отсортированных по релевантности
     * @throws IOException сетевая/HTTP ошибка
     * @throws ParseException JSON data/array парсинг
     */
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
    /**
     * <h3>Базовые рекомендации по треку (фиксированный алгоритм)</h3>
     * Генерирует ~25-30 связанных треков по простому правилу. **Упрощенная версия**.
     * <p>
     * <b>Жестко заданный алгоритм:</b>
     * <ol>
     *   <li><b>Исходный трек:</b> {@code api.getTrack(track)} → Artist</li>
     *   <li><b>Топ артиста:</b> {@code getTopTracksByArtist(artistName, 5)} — 5 хитов</li>
     *   <li><b>Related artists:</b> {@code getRelatedArtist(artistName)} → все → по 2 топа каждого</li>
     *   <li><b>Поиск:</b> {@code searchTracks(track, 5)} — 5 похожих</li>
     *   <li><b>Уникализация:</b> {@code stream().distinct().collect(toList())}</li>
     * </ol>
     * </p>
     *
     * <h3>Пример работы:</h3>
     * <pre>{@code
     * // "Bohemian Rhapsody" → рекомендации
     * List<Track> recs = deezer.getRelatedTracks("Bohemian Rhapsody");
     * // 5 Queen + 2xN Queen-related + 5 поиск = ~25 уникальных
     * }</pre>
     *
     * <p>
     * <b>Количество:</b> 12-30 треков (зависит от related artists).
     * </p>
     * <p>
     * <b>Делегирование:</b> использует существующие методы API.
     * </p>
     *
     * @param track название/ID для поиска трека
     * @return 20-30 связанных треков без дублей
     * @throws IOException любой подзапрос (artist/track/search)
     * @throws ParseException JSON в любом методе
     */
    public List<Track> getRelatedTracks(
            String track
    )
            throws IOException, ParseException
    {

        Track track1 = api.getTrack(track);
        List<Track> tracks = new ArrayList<>(getTopTracksByArtist(track1.getArtist().getName(), 5));

        List<Artist> relatedArtist = getRelatedArtist(track1.getArtist().getName());
        for(Artist artist : relatedArtist) {
            tracks.addAll(getTopTracksByArtist(artist.getName(), 2));
        }

        tracks.addAll(searchTracks(track, 5));

        return tracks.stream().distinct().collect(Collectors.toList());
    }
    /**
     * <h3>Расширенные рекомендации (полностью настраиваемые)</h3>
     * Мощный алгоритм рекомендаций с контролем каждого источника данных.
     * <p>
     * <b>Параметры (нулевые значения = пропуск):</b>
     * <table>
     *   <tr><th>Параметр</th><th>Источник</th><th>Пример</th></tr>
     *   <tr><td>{@code topTrackByArtistLimit}</td><td>Топ исходного артиста</td><td>10</td></tr>
     *   <tr><td>{@code anotherTopTrackOfArtistLimit}</td><td>Related artists топ</td><td>5</td></tr>
     *   <tr><td>{@code searchLimit}</td><td>Поиск по имени</td><td>3</td></tr>
     *   <tr><td>{@code chartLimit}</td><td>Глобальный чарт</td><td>20</td></tr>
     *   <tr><td>{@code depth}</td><td>Рекурсивная цепочка</td><td>2</td></tr>
     * </table>
     * </p>
     *
     * <h3>Полный алгоритм (условный):</h3>
     * <ol>
     *   <li><b>Базовый трек:</b> {@code api.getTrack(track)} → artistName</li>
     *   <li><b>if(topTrackByArtistLimit &gt; 0):</b> {@code getTopTracksByArtist(artistName, limit)}</li>
     *   <li><b>if(anotherTopTrackOfArtistLimit &gt; 0):</b>
     *     <ol>
     *       <li>{@code getRelatedArtist(artistName, topTrackByArtistLimit)} → relatedArtist</li>
     *       <li>Для каждого: {@code getTopTracksByArtist(related.getName(), anotherLimit)}</li>
     *     </ol>
     *   </li>
     *   <li><b>if(searchLimit &gt; 0):</b>
     *     <ol>
     *       <li>{@code searchTracks(artistName, searchLimit)}</li>
     *       <li>Если есть related: для каждого {@code searchTracks(relatedName, searchLimit)}</li>
     *     </ol>
     *   </li>
     *   <li><b>if(chartLimit &gt; 0):</b> {@code getChartTracks(chartLimit)}</li>
     *   <li><b>if(depth &gt; 0):</b> {@code getArtistChain(artistName, depth, topTrackByArtistLimit)}</li>
     *   <li><b>Уникализация:</b> {@code stream().distinct()}</li>
     * </ol>
     *
     * <h3>Примеры настройки:</h3>
     * <pre>{@code
     * // Минимальные рекомендации (только топ + related)
     * List<Track> simple = deezer.getRelatedTracks("Hotel California", 10, 3, 0, 0, 0);
     *
     * // Полный набор без рекурсии
     * List<Track> full = deezer.getRelatedTracks("Smells Like Teen Spirit", 5, 3, 5, 20, 0);
     *
     * // Рекурсивная цепочка (опасно!)
     * List<Track> chain = deezer.getRelatedTracks("Nirvana", 5, 2, 0, 0, 2); // ~50+ треков
     *
     * // Только чарт + поиск
     * List<Track> broad = deezer.getRelatedTracks("Anything", 0, 0, 5, 25, 0);
     * }</pre>
     *
     * <h3>Предупреждения:</h3>
     * <ul>
     *   <li><b>HTTP-запросы:</b> 1 + N*2 + M (экспоненциально при depth&gt;1)</li>
     *   <li><b>Exception:</b> шире IOException (включает getArtistChain рекурсию)</li>
     *   <li><b>distinct():</b> использует {@code Track.equals/hashCode()} — проверьте реализацию!</li>
     * </ul>
     *
     * <p>
     * <b>Производительность:</b> от 2 запросов (простые) до 50+ (глубокие).
     * </p>
     *
     * @param track исходный трек (название/ID)
     * @param topTrackByArtistLimit топ исходного артиста (0=пропуск)
     * @param anotherTopTrackOfArtistLimit топ related artists (0=пропуск)
     * @param searchLimit поиск по имени (0=пропуск)
     * @param chartLimit добавить чарт (0=пропуск)
     * @param depth глубина рекурсивной цепочки (0=пропуск)
     * @return уникальный список рекомендаций (50-200+ треков)
     * @throws Exception любой подметод (IOException + рекурсия)
     */
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
    /**
     * <h3>Похожие артисты (стандартный лимит)</h3>
     * Находит исполнителей, похожих на заданный по алгоритму Deezer. Делегирует полной версии.
     * <p>
     * <b>Фиксированный параметр:</b> {@code limit} = {@code 25}.
     * </p>
     * <p>
     * <b>Пример:</b>
     * <pre>{@code
     * try {
     *     List<Artist> queenLike = deezer.getRelatedArtist("Queen");
     *     // The Beatles, Led Zeppelin, Pink Floyd, ...
     * } catch (IOException | ParseException e) {
     *     // Queen не найден
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Делегирование:</b> {@code return getRelatedArtist(query, 25)}.
     * </p>
     *
     * @param query имя артиста для поиска похожих
     * @return 25 похожих исполнителей
     * @throws IOException поиск ID или related endpoint
     * @throws ParseException JSON artist/related
     */
    public List<Artist> getRelatedArtist(
            String query
    ) throws IOException, ParseException {
        return getRelatedArtist(query, 25);
    }
    /**
     * <h3>Похожие артисты (полная реализация)</h3>
     * Алгоритм Deezer "related artists" по ID с кастомным лимитом.
     * <p>
     * <b>Двухэтапный процесс:</b>
     * <ol>
     *   <li><b>Поиск:</b> {@code api.getArtist(query)} → Artist с ID</li>
     *   <li><b>Related:</b> {@code /artist/{ID}/related?limit={limit}}</li>
     * </ol>
     * </p>
     *
     * <h3>Последовательность:</h3>
     * <ol>
     *   <li><b>ID:</b> {@code api.getArtist("Queen")} → {@code artist.getId()=123}</li>
     *   <li><b>URL:</b> {@code /artist/123/related?limit=25}</li>
     *   <li><b>HTTP:</b> Request → JSON StringBuilder</li>
     *   <li><b>Парсинг:</b> {@code "data" → String[] jsonArtists}</li>
     *   <li><b>Простой маппинг:</b> для каждого:
     *     <ol>
     *       <li>{@code api.getArtistFromJson(jsonArtist)} → готовый Artist</li>
     *       <li>{@code artistsList.add()}</li>
     *     </ol>
     *   </li>
     * </ol>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * try {
     *     // 10 похожих на Nirvana
     *     List<Artist> grunge = deezer.getRelatedArtist("Nirvana", 10);
     *     // Pearl Jam, Soundgarden, Alice In Chains
     *
     *     // Максимум похожих на Daft Punk
     *     List<Artist> electronic = deezer.getRelatedArtist("Daft Punk", 50);
     *
     *     // Минимальный список
     *     List<Artist> top3Beatles = deezer.getRelatedArtist("The Beatles", 3);
     * } catch (IOException | ParseException e) {
     *     // 2 HTTP + JSON парсинг
     * }
     * }</pre>
     *
     * <h3>Особенности Deezer "related":</h3>
     * <ul>
     *   <li>Жанровая близость + популярность</li>
     *   <li>Коллаборации учитываются</li>
     *   <li>Каждый Artist полный: id, name, picture_small/medium/big</li>
     *   <li>**НЕ обогащается** (в отличие от Track — нет рекурсивного artist/album)</li>
     * </ul>
     *
     * <p>
     * <b>Производительность:</b> 2 HTTP (поиск + related). Быстрее рекомендаций треков.
     * </p>
     *
     * <p>
     * <b>Исключения:</b>
     * <ul>
     *   <li>{@link IOException}: поиск артиста ИЛИ related endpoint</li>
     *   <li>{@link ParseException}: artist JSON ИЛИ "data" array</li>
     * </ul>
     * </p>
     * <p>
     * <b>Пустой результат:</b> неизвестный артист → пустой список.
     * </p>
     *
     * @param query имя для поиска основного артиста
     * @param limit количество похожих (1-50)
     * @return List&lt;Artist&gt; отсортированных по схожести
     * @throws IOException два последовательных запроса
     * @throws ParseException любой JSON этап
     */
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
    /**
     * <h3>Цепочка артистов (публичный entry point)</h3>
     * Рекурсивно расширяет граф похожих исполнителей, собирая топ треки на каждом уровне.
     * <p>
     * <b>Логика:</b> Делегирует {@link #getArtistChainRecursive} с {@code currentDepth=1}.
     * </p>
     * <p>
     * <b>Структура графа:</b>
     * <pre>
     * Level 1: Nirvana (N треков)
     *          ↓ 3 related
     * Level 2: Pearl Jam, Soundgarden, Alice (N треков каждый)
     *          ↓ 3x3=9 related
     * Level 3: Foo Fighters, Audioslave... (N треков каждый)
     * </pre>
     * </p>
     * <p>
     * <b>Формула треков:</b> {@code N + 3N + 9N + ... + 3^(depth-1)N = N * (3^depth - 1)/2}
     * </p>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * try {
     *     // Nirvana → related → related (по 5 треков, глубина 2)
     *     List<Track> grungeChain = deezer.getArtistChain("Nirvana", 2, 5);
     *     // 5 + 3*5 = 20 треков
     *
     *     // Широкая сеть (глубина 3, по 3 трека)
     *     List<Track> rockNetwork = deezer.getArtistChain("Led Zeppelin", 3, 3);
     *     // 3 + 9 + 27 = 39 треков
     *
     *     // Глубокий дайв (ОСТОРОЖНО!)
     *     List<Track> insane = deezer.getArtistChain("Queen", 4, 2);
     *     // 2 + 6 + 18 + 54 = 80 треков, ~40 HTTP!
     * } catch (Exception e) {
     *     // Рекурсия + таймауты
     * }
     * }</pre>
     *
     * <p>
     * <b>Предупреждения:</b>
     * <ul>
     *   <li><b>Экспоненциальный рост:</b> depth=4 → 40+ запросов → 5+ сек</li>
     *   <li><b>Rate limiting:</b> Deezer заблокирует при depth&gt;3</li>
     *   <li><b>Память:</b> сотни Track объектов</li>
     * </ul>
     * </p>
     *
     * @param artistName стартовая точка графа
     * @param depth уровни рекурсии (1=только сам, 2=1+related, 3=1+3+9...)
     * @param tracksPerArtist треков с каждого узла
     * @return плоский список всех треков графа
     * @throws Exception любой подметод (рекурсивно!)
     */
    public List<Track> getArtistChain(String artistName, int depth, int tracksPerArtist)
            throws Exception
    {
        return getArtistChainRecursive(artistName, depth, 1, tracksPerArtist);
    }
    /**
     * <h3>Рекурсивный генератор цепочки (приватная реализация)</h3>
     * **BFS-подобное** tree traversal графа related artists.
     *
     * <h3>Алгоритм рекурсии:</h3>
     * <pre>{@code
     * function getArtistChainRecursive(artist, maxDepth, currentDepth, tracksPerArtist):
     *   1. БАЗОВЫЙ: chainTracks = getTopTracksByArtist(artist, tracksPerArtist)
     *   2. if currentDepth >= maxDepth: return chainTracks
     *   3. related = getRelatedArtist(artist, 3)  // ФИКСИРОВАННЫЕ 3!
     *   4. for each related_artist:
     *        chainTracks += getArtistChainRecursive(related, maxDepth, current+1, tracksPerArtist)
     *   5. return chainTracks
     * }</pre>
     *
     * <h3>Дерево вызовов (depth=3):</h3>
     * <pre>
     * Nirvana (5 треков)
     * ├─ Pearl Jam (5) → 3 related → 9 вызовов
     * ├─ Soundgarden (5) → 3 related → 9 вызовов
     * └─ Alice (5) → 3 related → 9 вызовов
     * Итого: 1 + 3 + 9 = 13 вызовов getTopTracksByArtist()
     * </pre>
     *
     * <h3>Ключевые особенности:</h3>
     * <ul>
     *   <li><b>Фиксированные 3 related:</b> {@code getRelatedArtist(artistName, 3)}</li>
     *   <li><b>Базовый случай:</b> {@code currentDepth >= maxDepth} → только топ треки</li>
     *   <li><b>Конкатенация:</b> {@code addAll()} всех поддеревьев</li>
     *   <li><b>Нет уникализации:</b> дубли возможны (обрабатывается в вызывающем)</li>
     * </ul>
     *
     * <p>
     * <b>Stack overflow риск:</b> depth&gt;20 (Java default stack ~1MB).
     * </p>
     * <p>
     * <b>HTTP взрыв:</b> depth=5 → 1+3+9+27+81=121 запрос!
     * </p>
     *
     * @param artistName текущий узел графа
     * @param maxDepth максимальная глубина рекурсии
     * @param currentDepth текущий уровень (1=root)
     * @param tracksPerArtist треков с узла
     * @return треки поддерева (включая себя)
     * @throws Exception рекурсивно от getTopTracksByArtist/getRelatedArtist
     */
    private List<Track> getArtistChainRecursive(
            String artistName,
            int maxDepth,
            int currentDepth,
            int tracksPerArtist
    )
            throws Exception
    {
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
    /**
     * <h3>Чтение текущего API URL</h3>
     * Возвращает базовый endpoint, используемый всеми запросами.
     * <p>
     * <b>Назначение:</b>
     * <ul>
     *   <li>Логирование/дебаг: какой сервер используется</li>
     *   <li>Dynamic proxy: перенаправление в runtime</li>
     *   <li>UI: отображение региона/версии API</li>
     * </ul>
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <pre>{@code
     * Deezer deezer = new Deezer("https://api-staging.deezer.com/");
     * logger.info("Using API: " + deezer.getApiUrl());
     * // "Using API: https://api-staging.deezer.com/"
     *
     * // Проверка production
     * if(deezer.getApiUrl().contains("api.deezer.com")) {
     *     enableRateLimiting();
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Характеристики:</b>
     * <ul>
     *   <li><b>Readonly:</b> только возврат значения, без side-effects</li>
     *   <li><b>Thread-safe:</b> {@link String} immutable, чтение protected поля</li>
     *   <li><b>Никогда null:</b> всегда значение из конструктора/по умолчанию</li>
     * </ul>
     * </p>
     * <p>
     * <b>Использование в тестах:</b>
     * <pre>{@code
     * @Test
     * void usesCustomUrl() {
     *     Deezer deezer = new Deezer("http://mock/");
     *     assertEquals("http://mock/", deezer.getApiUrl());
     * }
     * }</pre>
     * </p>
     *
     * @return текущий {@link #apiUrl} (production/staging/custom)
     */
    public String getApiUrl() {
        return apiUrl;
    }
    /**
     * <h3>Доступ к внутреннему API-адаптеру</h3>
     * Readonly ссылка на инжектированный/дефолтный {@link Api}.
     * <p>
     * <b>Назначение:</b>
     * <ul>
     *   <li><b>Расширение:</b> вызов недокументированных методов Api</li>
     *   <li><b>Тестирование:</b> проверка mock версий</li>
     *   <li><b>Кэш/статистика:</b> доступ к внутреннему состоянию Api</li>
     * </ul>
     * </p>
     * <p>
     * <b>Осторожно:</b> получатель может вызвать мутирующие методы Api!
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <pre>{@code
     * // Расширение функционала
     * Api api = deezer.getApi();
     * Genre genre = api.getGenre("rock"); // не из Deezer фасада
     *
     * // Тесты
     * @Test
     * void usesInjectedApi() {
     *     MockApi mock = new MockApi();
     *     Deezer deezer = new Deezer(mock);
     *     assertSame(mock, deezer.getApi());
     * }
     *
     * // Мониторинг
     * Api api = deezer.getApi();
     * if(api instanceof CachingApi) {
     *     ((CachingApi)api).printCacheStats();
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Характеристики:</b>
     * <ul>
     *   <li><b>Read-only ссылка:</b> нельзя переприсвоить {@link #api}</li>
     *   <li><b>Thread-safe:</b> чтение private поля (если Api thread-safe)</li>
     *   <li><b>Никогда null:</b> всегда значение из конструктора/по умолчанию</li>
     * </ul>
     * </p>
     * <p>
     * <b>Безопасность:</b> нарушает инкапсуляцию, но необходимо для DI/расширения.
     * </p>
     *
     * @return инжектированный {@link #api} (default/custom/mock)
     */
    public Api getApi() {
        return api;
    }
}