package net.y4k.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

class BrandTest {

  @Test
  void brandConstantsMatchColorsMd() {
    assertThat(Brand.NAME).isEqualTo("Y4K");
    assertThat(Brand.PRODUCT_NAME).isEqualTo("Y4Kv2");
    assertThat(Brand.NAVY_HEX).isEqualTo("#030C20");
    assertThat(Brand.CYAN_HEX).isEqualTo("#36EAF3");
    assertThat(Brand.VIOLET_HEX).isEqualTo("#8526FF");
    assertThat(Brand.WHITE_HEX).isEqualTo("#FFFFFF");
    assertThat(Brand.NAVY).isEqualTo(TextColor.color(0x03, 0x0C, 0x20));
    assertThat(Brand.CYAN).isEqualTo(TextColor.color(0x36, 0xEA, 0xF3));
    assertThat(Brand.VIOLET).isEqualTo(TextColor.color(0x85, 0x26, 0xFF));
    assertThat(Brand.WHITE).isEqualTo(TextColor.color(0xFF, 0xFF, 0xFF));
    assertThat(Brand.CYAN.asHexString()).isEqualToIgnoringCase(Brand.CYAN_HEX);
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
