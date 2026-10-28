package net.y4k.core.rank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class RankTest {

  /** One row per role of the real infra/ranks.json (copied to test resources). */
  static Stream<Arguments> rolesFromJson() {
    try (InputStream in = RankTest.class.getClassLoader().getResourceAsStream("ranks.json")) {
      assertThat(in).as("ranks.json on test classpath").isNotNull();
      JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
          .getAsJsonObject();
      assertThat(root.get("schema_version").getAsInt()).isEqualTo(1);
      JsonArray roles = root.getAsJsonArray("roles");
      List<Arguments> rows = new ArrayList<>();
      for (var element : roles) {
        JsonObject role = element.getAsJsonObject();
        rows.add(Arguments.of(
            role.get("name").getAsString(),
            role.get("symbol").getAsString(),
            role.get("color").getAsString(),
            role.get("weight").getAsInt(),
            role.get("kind").getAsString()));
      }
      assertThat(rows).hasSize(Rank.values().length);
      return rows.stream();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @ParameterizedTest(name = "{0}: {1} &{2} weight {3} kind {4}")
  @MethodSource("rolesFromJson")
  void matchesRanksJson(String name, String symbol, String color, int weight, String kind) {
    Rank rank = Rank.of(name);
    assertThat(rank.id()).isEqualTo(name);
    assertThat(rank.symbol()).isEqualTo(symbol);
    assertThat(color).hasSize(1);
    assertThat(rank.colorCode()).isEqualTo(color.charAt(0));
    assertThat(rank.color()).isEqualTo("&" + color);
    assertThat(rank.weight()).isEqualTo(weight);
    assertThat(rank.kind()).isEqualTo(Rank.Kind.fromJson(kind));
    assertThat(rank.isVip()).isEqualTo(kind.equals("vip"));
    assertThat(rank.isStaff()).isEqualTo(kind.equals("staff"));
    assertThat(rank.prefix()).isEqualTo("&f[&" + color + symbol + "&f]");
  }

  @Test
  void colorsFollowColorsMd() {
    assertThat(Rank.ALPHA.color()).isEqualTo("&3");
    assertThat(Rank.THETA.color()).isEqualTo("&7");
    assertThat(Rank.MU.color()).isEqualTo("&e");
    assertThat(Rank.PI.color()).isEqualTo("&b");
    assertThat(Rank.SIGMA.color()).isEqualTo("&6");
    assertThat(Rank.PHI.color()).isEqualTo("&d");
    assertThat(Rank.TAU.color()).isEqualTo("&a");
    assertThat(Rank.DELTA.color()).isEqualTo("&9");
    assertThat(Rank.OMEGA.color()).isEqualTo("&c");
  }

  @Test
  void classifiesVipAndStaff() {
    assertThat(Rank.byWeight()).filteredOn(Rank::isVip)
        .containsExactly(Rank.THETA, Rank.MU, Rank.PI, Rank.SIGMA);
    assertThat(Rank.byWeight()).filteredOn(Rank::isStaff)
        .containsExactly(Rank.TAU, Rank.DELTA, Rank.OMEGA);
    assertThat(Rank.ALPHA.kind()).isEqualTo(Rank.Kind.BASE);
    assertThat(Rank.PHI.kind()).isEqualTo(Rank.Kind.UNCLASSIFIED);
    assertThat(Rank.PHI.isVip()).isFalse();
    assertThat(Rank.PHI.isStaff()).isFalse();
  }

  @Test
  void ordersByWeightAndComparesRanks() {
    assertThat(Rank.byWeight()).containsExactly(Rank.ALPHA, Rank.THETA, Rank.MU, Rank.PI,
        Rank.SIGMA, Rank.PHI, Rank.TAU, Rank.DELTA, Rank.OMEGA);
    assertThat(Rank.OMEGA.outranks(Rank.ALPHA)).isTrue();
    assertThat(Rank.ALPHA.outranks(Rank.OMEGA)).isFalse();
    assertThat(Rank.MU.outranks(Rank.MU)).isFalse();
  }

  @ParameterizedTest
  @EnumSource(Rank.class)
  void prefixComponentKeepsSymbolAndColor(Rank rank) {
    var component = rank.prefixComponent();
    assertThat(PlainTextComponentSerializer.plainText().serialize(component))
        .isEqualTo("[" + rank.symbol() + "]");
    assertThat(component.children()).anySatisfy(child -> {
      assertThat(PlainTextComponentSerializer.plainText().serialize(child)).isEqualTo(rank.symbol());
      assertThat(child.color()).isEqualTo(namedColor(rank.colorCode()));
    });
  }

  @Test
  void findsByNameCaseInsensitivelyAndRejectsUnknown() {
    assertThat(Rank.find("Omega")).contains(Rank.OMEGA);
    assertThat(Rank.find(" theta ")).contains(Rank.THETA);
    assertThat(Rank.find("zeta")).isEmpty();
    assertThat(Rank.find(null)).isEmpty();
    assertThatThrownBy(() -> Rank.of("zeta"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("zeta");
  }

  private static NamedTextColor namedColor(char code) {
    return switch (code) {
      case '3' -> NamedTextColor.DARK_AQUA;
      case '7' -> NamedTextColor.GRAY;
      case 'e' -> NamedTextColor.YELLOW;
      case 'b' -> NamedTextColor.AQUA;
      case '6' -> NamedTextColor.GOLD;
      case 'd' -> NamedTextColor.LIGHT_PURPLE;
      case 'a' -> NamedTextColor.GREEN;
      case '9' -> NamedTextColor.BLUE;
      case 'c' -> NamedTextColor.RED;
      default -> throw new AssertionError("unexpected colour " + code);
    };
  }
}
