package net.y4k.core.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Locale-aware message catalogue shared by every Y4K server.
 *
 * <p>Bundles are UTF-8 {@code .properties} files named {@code <base>_<language>.properties}
 * (for example {@code i18n/messages_pt.properties}). Lookups resolve the requested language,
 * then Portuguese, then English; a key missing everywhere renders as {@code [key]} so the
 * gap is visible in game instead of throwing.
 *
 * <p>Templates accept two placeholder styles that can be mixed: sequential {@code {}} (the
 * historic Locales/mapnodes format, each one consumes the next argument) and indexed
 * {@code {0}}, {@code {1}}. Legacy colour codes ({@code &a}, {@code &l}, {@code &#rrggbb},
 * {@code &x&r&r&g&g&b&b}) are kept in {@link #text} and turned into Adventure components by
 * {@link #component} and {@link #colorize}.
 */
public final class Messages {
  public static final Locale PT = Locale.of("pt");
  public static final Locale EN = Locale.of("en");
  public static final Locale ES = Locale.of("es");
  /** Languages shipped by the network, in fallback order after the requested one. */
  public static final List<Locale> SUPPORTED = List.of(PT, EN, ES);
  private static final List<Locale> FALLBACKS = List.of(PT, EN);
  private static final String EXTENSION = ".properties";
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
      .character(LegacyComponentSerializer.AMPERSAND_CHAR)
      .hexColors()
      .useUnusualXRepeatedCharacterHexFormat()
      .build();

  private final Map<String, Map<String, String>> bundles;

  private Messages(Map<String, Map<String, String>> bundles) {
    this.bundles = bundles;
  }

  /** Builds a catalogue from in-memory bundles keyed by locale; handy for tests and defaults. */
  public static Messages of(Map<Locale, Map<String, String>> bundles) {
    Map<String, Map<String, String>> copy = new LinkedHashMap<>();
    bundles.forEach((locale, entries) ->
        copy.put(language(locale), Collections.unmodifiableMap(new LinkedHashMap<>(entries))));
    return new Messages(Collections.unmodifiableMap(copy));
  }

  /**
   * Loads {@code <base>_<lang>.properties} for every {@link #SUPPORTED} locale from a class loader.
   * A missing resource yields an empty bundle for that language.
   *
   * @param loader class loader that sees the resources (usually the plugin's)
   * @param base resource path without language or extension, e.g. {@code i18n/messages}
   */
  public static Messages fromClassLoader(ClassLoader loader, String base) {
    Objects.requireNonNull(loader, "loader");
    Map<String, Map<String, String>> loaded = new LinkedHashMap<>();
    for (Locale locale : SUPPORTED) {
      String name = fileName(base, locale);
      try (InputStream in = loader.getResourceAsStream(name)) {
        loaded.put(language(locale), in == null ? Map.of() : read(in));
      } catch (IOException e) {
        throw new UncheckedIOException("Cannot read message bundle " + name, e);
      }
    }
    return new Messages(Collections.unmodifiableMap(loaded));
  }

  /**
   * Loads {@code <base>_<lang>.properties} for every {@link #SUPPORTED} locale from a directory,
   * so operators can override messages next to the plugin configuration.
   */
  public static Messages fromPath(Path directory, String base) {
    Objects.requireNonNull(directory, "directory");
    Map<String, Map<String, String>> loaded = new LinkedHashMap<>();
    for (Locale locale : SUPPORTED) {
      Path file = directory.resolve(fileName(base, locale));
      if (!Files.isRegularFile(file)) {
        loaded.put(language(locale), Map.of());
        continue;
      }
      try (InputStream in = Files.newInputStream(file)) {
        loaded.put(language(locale), read(in));
      } catch (IOException e) {
        throw new UncheckedIOException("Cannot read message bundle " + file, e);
      }
    }
    return new Messages(Collections.unmodifiableMap(loaded));
  }

  /** Resource or file name for a bundle, e.g. {@code i18n/messages_pt.properties}. */
  public static String fileName(String base, Locale locale) {
    Objects.requireNonNull(base, "base");
    return base + "_" + language(locale) + EXTENSION;
  }

  /** Whether the key is defined for the exact language (no fallback). */
  public boolean has(Locale locale, String key) {
    return bundles.getOrDefault(language(locale), Map.of()).containsKey(key);
  }

  /**
   * Raw template after fallback, before placeholders are filled.
   * Empty when the key is unknown in the requested language, Portuguese and English.
   */
  public Optional<String> raw(Locale locale, String key) {
    Objects.requireNonNull(key, "key");
    String lang = language(locale);
    String value = bundles.getOrDefault(lang, Map.of()).get(key);
    if (value != null) {
      return Optional.of(value);
    }
    for (Locale fallback : FALLBACKS) {
      String fallbackLang = language(fallback);
      if (fallbackLang.equals(lang)) {
        continue;
      }
      value = bundles.getOrDefault(fallbackLang, Map.of()).get(key);
      if (value != null) {
        return Optional.of(value);
      }
    }
    return Optional.empty();
  }

  /** Translated text with placeholders filled and legacy colour codes untouched. */
  public String text(Locale locale, String key, Object... args) {
    return raw(locale, key).map(template -> format(template, args)).orElse("[" + key + "]");
  }

  /** Translated text as an Adventure component with legacy colour codes applied. */
  public Component component(Locale locale, String key, Object... args) {
    return colorize(text(locale, key, args));
  }

  /** Converts {@code &a}, {@code &l}, {@code &#rrggbb} and {@code &x&r&r&g&g&b&b} codes into a component. */
  public static Component colorize(String legacyText) {
    return LEGACY.deserialize(Objects.requireNonNull(legacyText, "legacyText"));
  }

  /** Serialises a component back to ampersand codes, the inverse of {@link #colorize}. */
  public static String legacy(Component component) {
    return LEGACY.serialize(Objects.requireNonNull(component, "component"));
  }

  /**
   * Fills {@code {}} (sequential) and {@code {n}} (indexed) placeholders. Placeholders without a
   * matching argument and unrelated braces are left as written.
   */
  public static String format(String template, Object... args) {
    Objects.requireNonNull(template, "template");
    Object[] values = args == null ? new Object[0] : args;
    StringBuilder out = new StringBuilder(template.length() + 16);
    int next = 0;
    int i = 0;
    while (i < template.length()) {
      char c = template.charAt(i);
      if (c != '{') {
        out.append(c);
        i++;
        continue;
      }
      int close = template.indexOf('}', i + 1);
      if (close < 0) {
        out.append(template, i, template.length());
        break;
      }
      String inside = template.substring(i + 1, close);
      int index;
      if (inside.isEmpty()) {
        index = next++;
      } else if (isDigits(inside)) {
        index = Integer.parseInt(inside);
      } else {
        out.append('{');
        i++;
        continue;
      }
      if (index < values.length) {
        out.append(values[index]);
      } else {
        out.append(template, i, close + 1);
      }
      i = close + 1;
    }
    return out.toString();
  }

  private static boolean isDigits(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) < '0' || s.charAt(i) > '9') {
        return false;
      }
    }
    return true;
  }

  /** Language tag used as bundle key; a null locale means Portuguese, the network default. */
  static String language(Locale locale) {
    Locale effective = locale == null ? PT : locale;
    String lang = effective.getLanguage();
    return lang.isEmpty() ? PT.getLanguage() : lang;
  }

  private static Map<String, String> read(InputStream in) throws IOException {
    Properties props = new Properties();
    try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      props.load(reader);
    }
    Map<String, String> entries = new LinkedHashMap<>();
    for (String name : props.stringPropertyNames()) {
      entries.put(name, props.getProperty(name));
    }
    return Collections.unmodifiableMap(entries);
  }
}
