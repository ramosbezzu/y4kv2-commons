package net.y4k.core.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessagesTest {
  private Messages messages;

  @BeforeEach
  void load() {
    messages = Messages.fromClassLoader(getClass().getClassLoader(), "i18n/messages");
  }

  @Test
  void loadsEachSupportedLocaleFromClassLoader() {
    assertThat(messages.has(Messages.PT, "greeting.welcome")).isTrue();
    assertThat(messages.has(Messages.EN, "greeting.welcome")).isTrue();
    assertThat(messages.has(Messages.ES, "greeting.welcome")).isTrue();
    assertThat(messages.has(Messages.ES, "only.pt")).isFalse();
  }

  @Test
  void sequentialPlaceholdersConsumeArgumentsInOrder() {
    assertThat(messages.text(Messages.EN, "greeting.welcome", "Y4K", "Steve"))
        .isEqualTo("&aWelcome to Y4K, Steve!");
  }

  @Test
  void indexedPlaceholdersPickArgumentsByPosition() {
    assertThat(messages.text(Messages.PT, "greeting.indexed", "Y4K", "Steve"))
        .isEqualTo("Olá Steve, você está no Y4K.");
  }

  @Test
  void formatMixesStylesAndKeepsUnmatchedPlaceholders() {
    assertThat(Messages.format("{} and {1} then {}", "a", "b")).isEqualTo("a and b then b");
    assertThat(Messages.format("missing {2} and {}", "x")).isEqualTo("missing {2} and x");
    assertThat(Messages.format("literal {abc} {", "x")).isEqualTo("literal {abc} {");
    assertThat(Messages.format("no args {}")).isEqualTo("no args {}");
    assertThat(Messages.format("nulls {}", (Object[]) null)).isEqualTo("nulls {}");
    assertThat(Messages.format("value {}", 42)).isEqualTo("value 42");
  }

  @Test
  void readsUtf8AccentsWithoutEscapes() {
    assertThat(messages.text(Messages.PT, "greeting.accent"))
        .isEqualTo("Parabéns, você venceu a partida de ação!");
    assertThat(messages.text(Messages.ES, "greeting.accent"))
        .isEqualTo("¡Felicitaciones, ganaste la partida de acción!");
  }

  @Test
  void fallsBackFromSpanishToPortugueseThenEnglishThenBracketedKey() {
    assertThat(messages.text(Messages.ES, "only.pt")).isEqualTo("Só em português");
    assertThat(messages.text(Messages.ES, "only.en")).isEqualTo("English only");
    assertThat(messages.text(Messages.PT, "only.en")).isEqualTo("English only");
    assertThat(messages.text(Messages.EN, "only.pt")).isEqualTo("Só em português");
    assertThat(messages.text(Messages.ES, "nope.missing")).isEqualTo("[nope.missing]");
    assertThat(messages.raw(Messages.ES, "nope.missing")).isEmpty();
  }

  @Test
  void regionalLocalesAndNullResolveByLanguage() {
    assertThat(messages.text(Locale.of("pt", "BR"), "only.pt")).isEqualTo("Só em português");
    assertThat(messages.text(Locale.of("en", "US"), "only.en")).isEqualTo("English only");
    assertThat(messages.text(null, "only.pt")).isEqualTo("Só em português");
    assertThat(messages.text(Locale.FRENCH, "only.pt.en")).isEqualTo("Só em inglês e português");
  }

  @Test
  void convertsLegacyColourCodesIntoComponents() {
    Component component = messages.component(Messages.PT, "greeting.welcome", "Y4K", "Steve");
    String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
        .serialize(component);
    assertThat(plain).isEqualTo("Bem-vindo ao Y4K, Steve!");
    assertThat(coloredPart(component).color()).isEqualTo(NamedTextColor.GREEN);

    Component styled = Messages.colorize("&c&lOmega &#36EAF3cyan &x&8&5&2&6&F&Fviolet");
    TextComponent red = coloredPart(styled);
    assertThat(red.color()).isEqualTo(NamedTextColor.RED);
    assertThat(red.decoration(TextDecoration.BOLD)).isEqualTo(TextDecoration.State.TRUE);
    assertThat(styled.children()).extracting(Component::color)
        .contains(TextColor.fromHexString("#36EAF3"), TextColor.fromHexString("#8526FF"));
    assertThat(Messages.legacy(Messages.colorize("&f[&3α&f]"))).isEqualTo("&f[&3α&f]");
  }

  @Test
  void loadsBundlesFromDirectoryAndToleratesMissingLanguages(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("msg_en.properties"), "k=Hello {}\n", StandardCharsets.UTF_8);
    Files.writeString(dir.resolve("msg_pt.properties"), "k=Olá {}\n", StandardCharsets.UTF_8);
    Messages fromDir = Messages.fromPath(dir, "msg");
    assertThat(fromDir.text(Messages.ES, "k", "você")).isEqualTo("Olá você");
    assertThat(fromDir.text(Messages.EN, "k", "you")).isEqualTo("Hello you");
    assertThat(fromDir.has(Messages.ES, "k")).isFalse();
    assertThat(Messages.fileName("i18n/messages", Messages.ES)).isEqualTo("i18n/messages_es.properties");
  }

  @Test
  void inMemoryBundlesFollowTheSameFallback() {
    Messages memory = Messages.of(Map.of(
        Messages.EN, Map.of("a", "A"),
        Messages.PT, Map.of("b", "B")));
    assertThat(memory.text(Messages.ES, "a")).isEqualTo("A");
    assertThat(memory.text(Messages.EN, "b")).isEqualTo("B");
    assertThat(memory.text(Messages.EN, "c")).isEqualTo("[c]");
  }

  @Test
  void rejectsNullKeyAndTemplate() {
    assertThatThrownBy(() -> messages.text(Messages.PT, null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> Messages.format(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> Messages.colorize(null)).isInstanceOf(NullPointerException.class);
  }

  /** First text component carrying a colour, whether root or child. */
  private static TextComponent coloredPart(Component component) {
    if (component instanceof TextComponent text && text.color() != null && !text.content().isEmpty()) {
      return text;
    }
    for (Component child : component.children()) {
      if (child instanceof TextComponent text && text.color() != null && !text.content().isEmpty()) {
        return text;
      }
    }
    throw new AssertionError("no coloured text in " + component);
  }
}
