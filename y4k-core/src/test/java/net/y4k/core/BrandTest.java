package net.y4k.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

class BrandTest {

  @Test
  void nameAndPaletteFollowTheOriginalYear4000() {
    assertThat(Brand.NAME).isEqualTo("Year4000").doesNotContain(" ");
    assertThat(Brand.SHORT_NAME).isEqualTo("Y4K");
    assertThat(Brand.PRODUCT_NAME).isEqualTo("Y4Kv2");
    assertThat(Brand.LOGO).isEqualTo("&3[&bYear4000&3]");
    assertThat(Brand.SHORT_LOGO).isEqualTo("&3[&bY4K&3]");
    assertThat(Brand.NOTICE).isEqualTo(" &7[&e!&7] &e");
    assertThat(Brand.RULE).isEqualTo("&7&m" + "*".repeat(40));
    assertThat(Brand.SLOGAN_EN).isEqualTo("Technology, Innovation, and the Future!");
    assertThat(Brand.PRIMARY).isEqualTo(NamedTextColor.DARK_AQUA);
    assertThat(Brand.ACCENT).isEqualTo(NamedTextColor.AQUA);
    assertThat(Brand.LABEL).isEqualTo(NamedTextColor.GOLD);
    assertThat(Brand.VALUE).isEqualTo(NamedTextColor.YELLOW);
    assertThat(Brand.ERROR).isEqualTo(NamedTextColor.RED);
  }

  @Test
  @SuppressWarnings("deprecation")
  void earlierArtworkColoursStayForCompatibility() {
    assertThat(Brand.NAVY_HEX).isEqualTo("#030C20");
    assertThat(Brand.CYAN_HEX).isEqualTo("#36EAF3");
    assertThat(Brand.VIOLET_HEX).isEqualTo("#8526FF");
    assertThat(Brand.WHITE_HEX).isEqualTo("#FFFFFF");
    assertThat(Brand.NAVY).isEqualTo(TextColor.color(0x03, 0x0C, 0x20));
    assertThat(Brand.CYAN.asHexString()).isEqualToIgnoringCase(Brand.CYAN_HEX);
    assertThat(Brand.VIOLET).isEqualTo(TextColor.color(0x85, 0x26, 0xFF));
    assertThat(Brand.WHITE).isEqualTo(TextColor.color(0xFF, 0xFF, 0xFF));
  }

  @Test
  void helpersBuildTheOriginalFormats() {
    assertThat(Brand.label("Saldo", "10.00")).isEqualTo("&6Saldo&7: &e10.00");
    assertThat(Brand.notice("Aguarde")).isEqualTo(" &7[&e!&7] &eAguarde");
    assertThat(Brand.error("Sem saldo")).isEqualTo("&cSem saldo");
    assertThat(Brand.menuTitle("Títulos")).isEqualTo("&8[&7Títulos&8]");
    assertThat(Brand.clickable("Servidores de Jogos", Brand.rightClick(Locale.of("pt"))))
        .isEqualTo("&a&lServidores de Jogos &7(Clique Direito)");
    assertThat(Brand.rightClick(Locale.of("es"))).isEqualTo("Clic Derecho");
    assertThat(Brand.rightClick(null)).isEqualTo("Right Click");
    assertThat(Brand.slogan(Locale.of("pt"))).isEqualTo("Tecnologia, Inovação e o Futuro!");
    assertThat(Brand.connecting(Locale.of("pt"), "Hub")).isEqualTo("&2Conectando-se a &aHub&2.");
    assertThat(Brand.connecting(Locale.ENGLISH, "Hub")).isEqualTo("&2Connecting to &aHub&2.");
    assertThat(Brand.connecting(Locale.of("es"), "Hub")).isEqualTo("&2Conectando a &aHub&2.");
    assertThatThrownBy(() -> Brand.label(null, 1)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shimmerCyclesBracketAndNameColoursLikeTabWelcome() {
    assertThat(Brand.shimmer(0)).isEqualTo("&3&l[&b&lYear4000&3&l]");
    assertThat(Brand.shimmer(1)).isEqualTo("&8&l[&7&lYear4000&8&l]");
    assertThat(Brand.shimmer(6)).isEqualTo("&1&l[&9&lYear4000&1&l]");
    assertThat(Brand.shimmer(7)).isEqualTo(Brand.shimmer(0));
    assertThat(Brand.shimmer(-1)).isEqualTo(Brand.shimmer(6));
  }

  @Test
  void framedCentresTextInsideTheRule() {
    String line = Brand.framed("&6Survival");
    assertThat(line).startsWith("&7&m*").endsWith("*").contains("&r &6Survival&r ");
    int stars = (int) line.chars().filter(c -> c == '*').count();
    assertThat(stars + "Survival".length() + 2).isBetween(Brand.RULE_WIDTH - 1, Brand.RULE_WIDTH);
    assertThat(Brand.framed("x".repeat(60))).contains("&7&m*&r ");
  }

  @Test
  void readsVersionFromRealConfigIni() throws IOException {
    try (InputStream in = resource("config.ini")) {
      assertThat(Brand.version(in)).isEqualTo("2.0.0-alpha.1");
    }
    try (InputStream in = resource("config.ini")) {
      assertThat(Brand.readValue(in, "game", "name")).contains("Y4Kv2");
    }
    try (InputStream in = resource("config.ini")) {
      assertThat(Brand.readValue(in, "platform", "java")).contains("25");
    }
    try (InputStream in = resource("config.ini")) {
      assertThat(Brand.readValue(in, "platform", "version")).isEmpty();
    }
  }

  @Test
  void parsesCommentsSpacingAndCaseInsensitiveSections() throws IOException {
    String ini = """
        ; comment
        # another comment
        [Game]
        name=Y4Kv2
          version =  9.9.9
        [other]
        version = 1.0.0
        junk line without equals
        """;
    assertThat(Brand.version(stream(ini))).isEqualTo("9.9.9");
    assertThat(Brand.readValue(stream(ini), "other", "version")).contains("1.0.0");
    assertThat(Brand.readValue(stream(ini), "OTHER", "junk")).isEmpty();
  }

  @Test
  void failsWhenVersionIsMissingOrBlank() {
    assertThatThrownBy(() -> Brand.version(stream("[game]\nname = x\n")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("[game] version");
    assertThatThrownBy(() -> Brand.version(stream("[game]\nversion =\n")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Brand.version(stream("version = 1.0.0\n")))
        .as("key outside [game] does not count")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Brand.version(null)).isInstanceOf(NullPointerException.class);
  }

  private static InputStream resource(String name) {
    InputStream in = BrandTest.class.getClassLoader().getResourceAsStream(name);
    assertThat(in).as(name + " on test classpath").isNotNull();
    return in;
  }

  private static InputStream stream(String text) {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }
}
