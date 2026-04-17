package deezer;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h1>QueryBuilder</h1>
 * Потоковый билдер URL для REST API Deezer с автоматическим URL-кодированием.
 * <p>
 * <b>Архитектура:</b> Классический Builder Pattern с fluent API (каждый метод возвращает {@code this}).
 * Гарантирует иммутабельность {@link #baseUrl} и thread-safe сборку через локальные {@link StringBuilder}.
 * </p>
 * <p>
 * <h2>Ключевые возможности:</h2>
 * <ul>
 *   <li><b>Много overload'ов:</b> {@link #path(String...)}, {@link #path(String)}, {@link #path(long)} для ID.</li>
 *   <li><b>Авто-кодирование:</b> {@link #encode(String)} использует {@link URLEncoder} + {@link StandardCharsets#UTF_8}.</li>
 *   <li><b>Сохранение порядка:</b> {@link LinkedHashMap} для params (важно для API с подписью).</li>
 *   <li><b>Stream API сборка:</b> {@link Collectors#joining} для query string без ручного "&amp;".</li>
 *   <li><b>Пустые параметры:</b> не добавляют "?" если params.isEmpty().</li>
 * </ul>
 * </p>
 *
 * <h2>Жизненный цикл объекта</h2>
 * <ol>
 *   <li><b>Создание:</b> {@code new QueryBuilder("https://api.deezer.com")} — фиксирует baseUrl.</li>
 *   <li><b>Построение:</b> цепочка {@code .path().param().path()} — мутирует внутренние коллекции.</li>
 *   <li><b>Сборка:</b> {@link #build()} — создаёт финальный URL, не меняя состояние билдера.</li>
 *   <li><b>Переиспользование:</b> билдер можно использовать повторно (path/params очищаются только при новом build).</li>
 * </ol>
 *
 * <h2>Примеры использования</h2>
 * <pre>{@code
 * // Треки артиста #123 с лимитом 50, отсортированные по рейтингу
 * String url = new QueryBuilder("https://api.deezer.com")
 *     .path("artist", "123", "top")
 *     .param("limit", 50)
 *     .param("order", "RANK")
 *     .build();
 * // Результат: https://api.deezer.com/artist/123/top?limit=50&amp;order=RANK
 *
 * // Альбом #456, трек #789
 * String url2 = new QueryBuilder("https://api.deezer.com")
 *     .path(456L, "tracks", 789L)
 *     .build();
 * // Результат: https://api.deezer.com/456/tracks/789
 * }</pre>
 *
 * <h2>Внутренняя структура</h2>
 * <ul>
 *   <li>{@link #baseUrl}: неизменяемый базовый URL API (https://api.deezer.com).</li>
 *   <li>{@link #pathSegments}: {@link ArrayList} сегментов пути ({@code artist}, {@code 123}, {@code tracks}).</li>
 *   <li>{@link #params}: {@link LinkedHashMap} для сохранения порядка вставки параметров.</li>
 * </ul>
 *
 * <h3>Потокобезопасность</h3>
 * <p><b>Не thread-safe:</b> мутация {@link #pathSegments}, {@link #params} в методах path/param.
 * Каждый поток должен иметь свой экземпляр билдера.</p>
 *
 * <h3>Исключения</h3>
 * <p>Не выбрасывает checked exceptions. {@link URLEncoder#encode} может кинуть {@link java.io.UnsupportedEncodingException},
 * но {@link StandardCharsets#UTF_8} гарантирует безопасность (Java 7+).</p>
 *
 * @see URLEncoder для URL-кодирования
 * @see LinkedHashMap для сохранения порядка параметров
 * @see Collectors#joining(CharSequence) для сборки query string
 * @since 1.0
 */
public class QueryBuilder {
    /**
     * <h3>Неизменяемый базовый URL API</h3>
     * Фиксируется в конструкторе {@link QueryBuilder#QueryBuilder(String)} и никогда не меняется.
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li>{@code "https://api.deezer.com"} — основной endpoint Deezer.</li>
     *   <li>{@code "https://api.deezer.com/v2"} — версия API.</li>
     *   <li>{@code "https://api.deezer.com/search"} — поисковой endpoint.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Требования:</b> должен быть полным URL с протоколом и хостом.
     * Относительные пути ("/artist") вызовут некорректный результат.
     * </p>
     * <p>
     * <b>Механизм неизменности:</b> {@code final} поле, присвоение только в конструкторе.
     * {@link String} по определению immutable в Java.
     * </p>
     */
    private final String baseUrl;
    /**
     * <h3>Сегменты пути REST API</h3>
     * Коллекция строковых сегментов для построения пути после {@link #baseUrl}.
     * <p>
     * <b>Внутренняя структура:</b> {@link ArrayList} — динамический массив с O(1) добавлением в конец.
     * </p>
     * <b>Пример заполнения:</b>
     * <pre>
     * .path("artist", "123", "top") → ["/artist", "123", "/top"]
     * </pre>
     * <p>
     * <b>Автоматическое кодирование:</b> все добавления проходят через {@link #encode(String)}.
     * Пробелы → {@code %20}, слеши → {@code %2F}, специальные символы экранируются.
     * </p>
     * <p>
     * <b>Очистка:</b> НЕ очищается автоматически. Для повторного использования создавайте новый билдер.
     * </p>
     */
    private final List<String> pathSegments = new ArrayList<>();
    /**
     * <h3>Параметры запроса (query string)</h3>
     * Карта ключ-значение для GET-параметров с сохранением порядка вставки.
     * <p>
     * <b>Почему LinkedHashMap:</b>
     * <ul>
     *   <li>Сохраняет insertion order (в отличие от {@link java.util.HashMap}).</li>
     *   <li>Детерминированный результат {@link #build()} при одинаковых вызовах.</li>
     *   <li>Критично для API с подписью/хэшированием параметров (OAuth, API keys).</li>
     * </ul>
     * </p>
     * <p>
     * <b>Автоконвертация:</b> перегрузки {@link #param(String, int)}, {@link #param(String, boolean)}
     * используют {@link String#valueOf(int)} ()} перед кодированием.
     * </p>
     * <p>
     * <b>Перезапись:</b> повторный вызов {@code param("limit", 50)} перезапишет значение.
     * </p>
     */
    private final Map<String, String> params = new LinkedHashMap<>();
    /**
     * <h3>Основной конструктор билдера</h3>
     * Инициализирует неизменяемый {@link #baseUrl} и пустые коллекции {@link #pathSegments}, {@link #params}.
     * <p>
     * <b>Валидация:</b> НЕ выполняется. Некорректный baseUrl даст некорректный результат в {@link #build()}.
     * </p>
     * <p>
     * <b>Иммутабельность:</b>
     * <ul>
     *   <li>{@link #baseUrl} — final, присвоение только здесь.</li>
     *   <li>{@link #pathSegments} — final ссылка на {@link ArrayList} (содержимое мутируется методами path).</li>
     *   <li>{@link #params} — final ссылка на {@link LinkedHashMap} (содержимое мутируется методами param).</li>
     * </ul>
     * </p>
     * <p>
     * <b>Thread-safety:</b> безопасно вызывать из любого потока, но экземпляр НЕ thread-safe для дальнейшего использования.
     * </p>
     *
     * @param baseUrl базовый URL API (https://api.deezer.com, https://api.deezer.com/v2 и т.д.)
     * @throws NullPointerException если baseUrl = null
     * @throws IllegalArgumentException если baseUrl пустой
     */
    public QueryBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    /**
     * <h3>Добавляет множественные сегменты пути (varargs)</h3>
     * Самый гибкий вариант для REST-путей с переменным числом сегментов.
     * <p>
     * <b>Механизм:</b>
     * <ul>
     *   <li>Varargs {@code String...} разворачивается в массив.</li>
     *   <li>Каждый сегмент проходит через {@link #encode(String)} → добавляется в {@link #pathSegments}.</li>
     *   <li>Возврат {@code this} для fluent API.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <pre>
     * .path("artist", "123", "top")     → /artist/123/top
     * .path("search", "q", "nirvana")   → /search/q/nirvana
     * </pre>
     * </p>
     * <p>
     * <b>Производительность:</b> O(n) где n — количество сегментов (цикл for-each).
     * </p>
     *
     * @param segments сегменты пути ({@code "resource", "id", "subresource"})
     * @return {@code this} для цепочки вызовов
     * @see #path(String) для одного сегмента
     * @see #path(long) для числовых ID
     */
    public QueryBuilder path(String... segments) {
        for (String segment : segments) {
            pathSegments.add(encode(segment));
        }

        return this;
    }
    /**
     * <h3>Добавляет один строковый сегмент пути</h3>
     * Удобно для последовательного построения пути по одному элементу.
     * <p>
     * <b>Отличие от varargs:</b> принимает одиночный {@link String}, не требует массива/разворачивания.
     * </p>
     * <p>
     * <b>Применение:</b> когда сегмент вычисляется динамически или приходит извне.
     * </p>
     * <p>
     * <b>Автокодирование:</b> пробелы, слеши, специальные символы экранируются {@link #encode(String)}.
     * </p>
     *
     * @param segment один сегмент пути
     * @return {@code this}
     * @see #path(String...) для множественных сегментов
     */
    public QueryBuilder path(String segment) {
        pathSegments.add(encode(segment));
        return this;
    }
    /**
     * <h3>Добавляет числовой ID как сегмент пути</h3>
     * Автоконвертация {@code long → String} без кодирования (числа безопасны в URL).
     * <p>
     * <b>Почему long:</b> REST API обычно используют 64-битные ID (Snowflake, UUID-based).
     * </p>
     * <p>
     * <b>Безопасность:</b> {@link String#valueOf(long)} НЕ вызывает {@link #encode(String)}} — цифры не требуют кодирования.
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <pre>
     * .path(123456789L)           → /123456789
     * .path(456L, "tracks", 789L) → /456/tracks/789
     * </pre>
     * </p>
     *
     * @param id числовой идентификатор ресурса
     * @return {@code this}
     */
    public QueryBuilder path(long id) {
        pathSegments.add(String.valueOf(id));
        return this;
    }
    /**
     * <h3>Строковый параметр запроса</h3>
     * Основной метод для GET-параметров с автоматическим URL-кодированием значения.
     * <p>
     * <b>Механизм:</b>
     * <ul>
     *   <li>{@link #encode(String)} для value (ключ НЕ кодируется).</li>
     *   <li>Перезапись при дубликате ключа ({@link Map#put}).</li>
     *   <li>Сохранение порядка через {@link LinkedHashMap}.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <pre>
     * .param("q", "Nirvana")     → q=Nirvana
     * .param("order", "RANK")    → order=RANK
     * .param("limit", "50")      → limit=50
     * </pre>
     * </p>
     *
     * @param key имя параметра (не кодируется)
     * @param value значение (кодируется)
     * @return {@code this}
     * @see #param(String, int)
     * @see #param(String, boolean)
     */
    public QueryBuilder param(String key, String value) {
        params.put(key, encode(value));
        return this;
    }
    /**
     * <h3>Целочисленный параметр</h3>
     * Автоконвертация {@code int → String} без кодирования (цифры безопасны).
     * <p>
     * <b>Преимущества:</b>
     * <ul>
     *   <li>Типобезопасность (компилятор проверит).</li>
     *   <li>{@link String#valueOf(int)} без {@link #encode(String)}}.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Применение:</b> limit, offset, index, page.
     * </p>
     *
     * @param key имя параметра
     * @param value числовое значение
     * @return {@code this}
     */
    public QueryBuilder param(String key, int value) {
        params.put(key, String.valueOf(value));
        return this;
    }
    /**
     * <h3>Логический параметр</h3>
     * Автоконвертация {@code boolean → "true"/"false"} без кодирования.
     * <p>
     * <b>Стандарты:</b> Java {@link String#valueOf(boolean)} даёт lowercase строки.
     * Совместимо с REST API (true/false без кавычек).
     * </p>
     * <p>
     * <b>Применение:</b> debug, strict, safe_mode.
     * </p>
     *
     * @param key имя параметра
     * @param value логическое значение
     * @return {@code this}
     */
    public QueryBuilder param(String key, boolean value) {
        params.put(key, String.valueOf(value));
        return this;
    }
    /**
     * <h3>Финальная сборка URL</h3>
     * Создаёт полную строку URL из {@link #baseUrl} + path + query string.
     * <p>
     * <b>Алгоритм (пошагово):</b>
     * <ol>
     *   <li><b>База:</b> копирует {@link #baseUrl} в {@link StringBuilder}.</li>
     *   <li><b>Путь:</b> для каждого {@link #pathSegments} добавляет {@code "/segment"}.</li>
     *   <li><b>Параметры:</b> если {@link #params} не пустые:
     *     <ol>
     *       <li>Stream API: {@link Map#entrySet()} → пары key=value.</li>
     *       <li>{@link Collectors#joining(CharSequence)} для query string.</li>
     *       <li>Добавляет {@code "?paramsStr"} в конец.</li>
     *     </ol>
     *   </li>
     *   <li><b>Возврат:</b> {@link StringBuilder#toString()}.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Thread-safety:</b> локальный {@link StringBuilder} — каждый вызов независим.
     * НЕ очищает внутренние коллекции (билдер переиспользуем).
     * </p>
     * <p>
     * <b>Краевые случаи:</b>
     * <ul>
     *   <li>Пустой path → только baseUrl.</li>
     *   <li>Пустые params → без "?".</li>
     *   <li>Оба пустые → чистый baseUrl.</li>
     * </ul>
     * </p>
     *
     * @return готовый URL для HTTP-клиента
     * @throws IllegalStateException если baseUrl некорректен
     */
    public String build() {
        StringBuilder url = new StringBuilder(baseUrl);

        for (String segment : pathSegments) {
            url.append("/").append(segment);
        }

        if (!params.isEmpty()) {
            url.append("?");

            String paramsStr = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));

            url.append(paramsStr);
        }

        return url.toString();
    }
    /**
     * <h3>URL-кодирование сегментов и параметров</h3>
     * Стандартное экранирование специальных символов для HTTP URL.
     * <p>
     * <b>Стек Java:</b>
     * <ul>
     *   <li>{@link URLEncoder#encode(String, Charset)}} — RFC 3986 compliant.</li>
     *   <li>{@link StandardCharsets#UTF_8} — фиксированная кодировка (Java 7+).</li>
     * </ul>
     * </p>
     * <p>
     * <b>Экранирует:</b> пробелы({@code %20}), слеши({@code %2F}), {@code &amp; → %26}, {@code = → %3D}.
     * </p>
     * <p>
     * <b>НЕ экранирует:</b> ключи параметров (по REST-конвенциям), цифры, a-zA-Z0-9, -, _, ., ~.
     * </p>
     * <p>
     * <b>Исключения:</b> {@link java.io.UnsupportedEncodingException} невозможна (UTF_8 всегда доступна).
     * </p>
     *
     * @param value строка для кодирования
     * @return URL-safe версия строки
     * @see URLEncoder#encode(String, Charset)
     * @see StandardCharsets#UTF_8
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}