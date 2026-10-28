package net.y4k.core.rank;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.y4k.core.i18n.Messages;

/**
 * The Greek ranks of Y4K, mirroring {@code infra/ranks.json} and {@code COLORS.MD} in the root
 * repository. Colours are the original Minecraft codes; they never indicate price. Brackets are
 * always white ({@code &f}).
 */
public enum Rank {
  ALPHA("alpha", "α", '3', 1, Kind.BASE),
  THETA("theta", "Θ", '7', 2, Kind.VIP),
  MU("mu", "μ", 'e', 3, Kind.VIP),
  PI("pi", "π", 'b', 4, Kind.VIP),
  SIGMA("sigma", "σ", '6', 5, Kind.VIP),
  PHI("phi", "Φ", 'd', 6, Kind.UNCLASSIFIED),
  TAU("tau", "τ", 'a', 7, Kind.STAFF),
  DELTA("delta", "δ", '9', 8, Kind.STAFF),
  OMEGA("omega", "Ω", 'c', 9, Kind.STAFF);

  /** Classification from {@code ranks.json}: {@code kind}. */
  public enum Kind {
    BASE, VIP, UNCLASSIFIED, STAFF;

    /** Parses the lower-case value used in {@code ranks.json}. */
    public static Kind fromJson(String value) {
      return valueOf(Objects.requireNonNull(value, "value").toUpperCase(Locale.ROOT));
    }
  }

  private static final List<Rank> BY_WEIGHT = Arrays.stream(values())
      .sorted(Comparator.comparingInt(Rank::weight))
      .toList();

  private final String id;
  private final String symbol;
  private final char colorCode;
  private final int weight;
  private final Kind kind;

  Rank(String id, String symbol, char colorCode, int weight, Kind kind) {
    this.id = id;
    this.symbol = symbol;
    this.colorCode = colorCode;
    this.weight = weight;
    this.kind = kind;
  }

  /** Lower-case name used by LuckPerms groups and {@code ranks.json}. */
  public String id() {
    return id;
  }

  /** Greek letter shown in the chat prefix. */
  public String symbol() {
    return symbol;
  }

  /** Minecraft colour character, e.g. {@code '3'} for Alpha. */
  public char colorCode() {
    return colorCode;
  }

  /** Ampersand colour code, e.g. {@code &3}. */
  public String color() {
    return "&" + colorCode;
  }

  /** Ordering weight from {@code ranks.json}; higher outranks lower. */
  public int weight() {
    return weight;
  }

  public Kind kind() {
    return kind;
  }

  /** Theta, Mu, Pi and Sigma. */
  public boolean isVip() {
    return kind == Kind.VIP;
  }

  /** Tau, Delta and Omega. */
  public boolean isStaff() {
    return kind == Kind.STAFF;
  }

  /** Legacy prefix: white brackets around the coloured symbol, e.g. {@code &f[&3α&f]}. */
  public String prefix() {
    return "&f[" + color() + symbol + "&f]";
  }

  /** {@link #prefix()} as an Adventure component. */
  public Component prefixComponent() {
    return Messages.colorize(prefix());
  }

  /** Whether this rank outranks the other by weight. */
  public boolean outranks(Rank other) {
    return weight > Objects.requireNonNull(other, "other").weight;
  }

  /** All ranks ordered by ascending weight. */
  public static List<Rank> byWeight() {
    return BY_WEIGHT;
  }

  /** Finds a rank by its {@link #id()} or enum name, case-insensitive. */
  public static Optional<Rank> find(String name) {
    if (name == null) {
      return Optional.empty();
    }
    String wanted = name.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values()).filter(rank -> rank.id.equals(wanted)).findFirst();
  }

  /** Like {@link #find} but fails loudly on an unknown rank. */
  public static Rank of(String name) {
    return find(name).orElseThrow(() -> new IllegalArgumentException("Unknown rank: " + name));
  }
}
