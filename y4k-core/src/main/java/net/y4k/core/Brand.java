package net.y4k.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Year4000 identity shared by every server, from the original sources ({@code Locales/year4000},
 * {@code Modules/bungee/welcome TabWelcome}, {@code MapNodes Msg.NOTICE}, {@code Locales/linker}),
 * plus the product version read from the root {@code config.ini}.
 *
 * <p>Player-facing text uses '&' colour codes in the original palette: labels {@code &6}, values
 * {@code &e}, the {@code &7:} separator, notices with {@link #NOTICE}, errors {@code &c}, chat blocks
 * framed by {@link #RULE}, item names {@code &a&l<Name> &7(Right Click)} and menu titles
 * {@code &8[&7<Name>&8]}. The network name is always written "Year4000", without a space.
 */
public final class Brand {
  /** The network name shown to players ({@code y4k.name}). */
  public static final String NAME = "Year4000";
  /** Short tag, used only between brackets as in the original server list ({@code &3[&bY4K&3]}). */
  public static final String SHORT_NAME = "Y4K";
  /** Internal product and repository name; never shown to players. */
  public static final String PRODUCT_NAME = "Y4Kv2";

  /** {@code y4k.sloglan} from Locales/year4000, literal in English. */
  public static final String SLOGAN_EN = "Technology, Innovation, and the Future!";
  public static final String SLOGAN_PT = "Tecnologia, Inovação e o Futuro!";
  public static final String SLOGAN_ES = "¡Tecnología, Innovación y el Futuro!";

  /** The logo in its resting colours. */
  public static final String LOGO = "&3[&b" + NAME + "&3]";
  /** The short logo of the original server list. */
  public static final String SHORT_LOGO = "&3[&b" + SHORT_NAME + "&3]";
  /** MapNodes {@code Msg.NOTICE}: prefix of every warning. */
  public static final String NOTICE = " &7[&e!&7] &e";
  /** Width of the MapNodes {@code Common.textLine} frame. */
  public static final int RULE_WIDTH = 40;
  /** Struck-through line that opens and closes chat blocks. */
  public static final String RULE = "&7&m" + "*".repeat(RULE_WIDTH);
  /** Colour cycle of the shimmering logo (TabWelcome), taken two at a time: brackets, then name. */
  public static final List<Character> SHIMMER = List.of('3', 'b', '8', '7', '2', 'a', '4', 'c', '5', 'd', '6', 'e', '1', '9');

  /** Original palette as Adventure colours. */
  public static final TextColor PRIMARY = NamedTextColor.DARK_AQUA;
  public static final TextColor ACCENT = NamedTextColor.AQUA;
  public static final TextColor LABEL = NamedTextColor.GOLD;
  public static final TextColor VALUE = NamedTextColor.YELLOW;
  public static final TextColor MUTED = NamedTextColor.GRAY;
  public static final TextColor FRAME = NamedTextColor.DARK_GRAY;
  public static final TextColor SUCCESS = NamedTextColor.GREEN;
  public static final TextColor ERROR = NamedTextColor.RED;

  /** Earlier artwork colours, kept for binary compatibility; not used in player-facing text. */
  @Deprecated public static final String NAVY_HEX = "#030C20";
  @Deprecated public static final String CYAN_HEX = "#36EAF3";
  @Deprecated public static final String VIOLET_HEX = "#8526FF";
  @Deprecated public static final String WHITE_HEX = "#FFFFFF";
  @Deprecated public static final TextColor NAVY = Objects.requireNonNull(TextColor.fromHexString(NAVY_HEX));
  @Deprecated public static final TextColor CYAN = Objects.requireNonNull(TextColor.fromHexString(CYAN_HEX));
  @Deprecated public static final TextColor VIOLET = Objects.requireNonNull(TextColor.fromHexString(VIOLET_HEX));
  @Deprecated public static final TextColor WHITE = Objects.requireNonNull(TextColor.fromHexString(WHITE_HEX));

  /** Section and key of the product version inside {@code config.ini}. */
  public static final String VERSION_SECTION = "game";
  public static final String VERSION_KEY = "version";

  private Brand() {
  }

  /** {@code &6Label&7: &evalue}. */
  public static String label(String label, Object value) {
    return "&6" + Objects.requireNonNull(label, "label") + "&7: &e" + value;
  }

  /** A warning with the {@link #NOTICE} prefix. */
  public static String notice(String text) {
    return NOTICE + Objects.requireNonNull(text, "text");
  }

  /** An error line in red. */
  public static String error(String text) {
    return "&c" + Objects.requireNonNull(text, "text");
  }

  /** Inventory title {@code &8[&7Name&8]}, as the servermenu {@code &8[&7Year4000&8]}. */
  public static String menuTitle(String name) {
    return "&8[&7" + Objects.requireNonNull(name, "name") + "&8]";
  }

  /** Item name {@code &a&lName &7(action)}, as {@code items.servers_menu}. */
  public static String clickable(String name, String action) {
    return "&a&l" + Objects.requireNonNull(name, "name") + " &7(" + Objects.requireNonNull(action, "action") + ")";
  }

  /** Localised "Right Click" of the original item names. */
  public static String rightClick(Locale locale) {
    return switch (language(locale)) {
      case "pt" -> "Clique Direito";
      case "es" -> "Clic Derecho";
      default -> "Right Click";
    };
  }

  /** Localised slogan. */
  public static String slogan(Locale locale) {
    return switch (language(locale)) {
      case "pt" -> SLOGAN_PT;
      case "es" -> SLOGAN_ES;
      default -> SLOGAN_EN;
    };
  }

  /** Linker {@code server.connect}: {@code &2Connecting to &a<server>&2.} */
  public static String connecting(Locale locale, String server) {
    Objects.requireNonNull(server, "server");
    return switch (language(locale)) {
      case "pt" -> "&2Conectando-se a &a" + server + "&2.";
      case "es" -> "&2Conectando a &a" + server + "&2.";
      default -> "&2Connecting to &a" + server + "&2.";
    };
  }

  /**
   * Frame {@code step} of the shimmering logo: brackets and name take two consecutive colours of
   * {@link #SHIMMER}, as the two calls to the cycling iterator in TabWelcome did.
   */
  public static String shimmer(long step) {
    int index = (int) Math.floorMod(step * 2, (long) SHIMMER.size());
    char bracket = SHIMMER.get(index);
    char name = SHIMMER.get((index + 1) % SHIMMER.size());
    return "&" + bracket + "&l[&" + name + "&l" + NAME + "&" + bracket + "&l]";
  }

  /** The text centred inside the {@link #RULE} frame, as MapNodes {@code Common.textLine}. */
  public static String framed(String text) {
    Objects.requireNonNull(text, "text");
    int visible = text.replaceAll("&[0-9a-fk-or]", "").length() + 2;
    int side = Math.max(1, (RULE_WIDTH - visible) / 2);
    String bar = "&7&m" + "*".repeat(side);
    return bar + "&r " + text + "&r " + bar;
  }

  private static String language(Locale locale) {
    return locale == null ? "en" : locale.getLanguage();
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
