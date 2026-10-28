package net.y4k.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.format.TextColor;

/**
 * Y4K brand constants (see {@code COLORS.MD} in the root repository) and the product version
 * read from the root {@code config.ini}, the single source of truth for the game version.
 */
public final class Brand {
  public static final String NAME = "Y4K";
  public static final String PRODUCT_NAME = "Y4Kv2";

  public static final String NAVY_HEX = "#030C20";
  public static final String CYAN_HEX = "#36EAF3";
  public static final String VIOLET_HEX = "#8526FF";
  public static final String WHITE_HEX = "#FFFFFF";

  public static final TextColor NAVY = Objects.requireNonNull(TextColor.fromHexString(NAVY_HEX));
  public static final TextColor CYAN = Objects.requireNonNull(TextColor.fromHexString(CYAN_HEX));
  public static final TextColor VIOLET = Objects.requireNonNull(TextColor.fromHexString(VIOLET_HEX));
  public static final TextColor WHITE = Objects.requireNonNull(TextColor.fromHexString(WHITE_HEX));

  /** Section and key of the product version inside {@code config.ini}. */
  public static final String VERSION_SECTION = "game";
  public static final String VERSION_KEY = "version";

  private Brand() {
  }

  /**
   * Reads {@code [game] version} from a {@code config.ini} stream (UTF-8, {@code ;} or {@code #}
   * comments, {@code key = value} lines, section names case-insensitive).
   *
   * @throws IllegalArgumentException when the section or key is absent or blank
   */
  public static String version(InputStream configIni) throws IOException {
    return readValue(configIni, VERSION_SECTION, VERSION_KEY)
        .orElseThrow(() -> new IllegalArgumentException(
            "config.ini has no [" + VERSION_SECTION + "] " + VERSION_KEY));
  }

  /** Reads one {@code [section] key} from an INI stream; empty when absent or blank. */
  public static Optional<String> readValue(InputStream ini, String section, String key)
      throws IOException {
    Objects.requireNonNull(section, "section");
    Objects.requireNonNull(key, "key");
    Map<String, Map<String, String>> parsed = parseIni(ini);
    String value = parsed.getOrDefault(section.toLowerCase(Locale.ROOT), Map.of()).get(key);
    return Optional.ofNullable(value).filter(v -> !v.isBlank());
  }

  /** Parses a minimal INI document into {@code section -> key -> value}; sections are lower-cased. */
  static Map<String, Map<String, String>> parseIni(InputStream ini) throws IOException {
    Objects.requireNonNull(ini, "ini");
    Map<String, Map<String, String>> sections = new LinkedHashMap<>();
    String current = "";
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(ini, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#")) {
          continue;
        }
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
          current = trimmed.substring(1, trimmed.length() - 1).strip().toLowerCase(Locale.ROOT);
          sections.computeIfAbsent(current, k -> new LinkedHashMap<>());
          continue;
        }
        int eq = trimmed.indexOf('=');
        if (eq < 0) {
          continue;
        }
        String name = trimmed.substring(0, eq).strip();
        String value = trimmed.substring(eq + 1).strip();
        sections.computeIfAbsent(current, k -> new LinkedHashMap<>()).put(name, value);
      }
    }
    return sections;
  }
}
