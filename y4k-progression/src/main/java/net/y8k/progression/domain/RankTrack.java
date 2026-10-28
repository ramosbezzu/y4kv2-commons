package net.y8k.progression.domain;

import java.util.*;

public final class RankTrack {
    public record Rank(String id, String name, int homes, int shops) {}
    public static final List<Rank> SURVIVAL = List.of(
        new Rank("surv_sobrevivente", "Sobrevivente", 1, 2), new Rank("surv_explorador", "Explorador", 2, 4),
        new Rank("surv_pioneiro", "Pioneiro", 3, 6), new Rank("surv_veterano", "Veterano", 4, 8), new Rank("surv_mestre", "Mestre", 5, 10));
    public static final List<Rank> SKYBLOCK = List.of(
        new Rank("sky_ilheu", "Ilhéu", 1, 2), new Rank("sky_colono", "Colono", 2, 4),
        new Rank("sky_construtor", "Construtor", 3, 6), new Rank("sky_pioneiro", "Pioneiro das Ilhas", 4, 8), new Rank("sky_mestre", "Mestre das Ilhas", 5, 10));
    private RankTrack() {}
    public static List<Rank> in(String mode) {
        return switch(mode) { case "survival" -> SURVIVAL; case "skyblock" -> SKYBLOCK; default -> throw new IllegalArgumentException("Unknown progression mode"); };
    }
    public static Rank highest(String mode, Set<String> groups) {
        var all = in(mode);
        for (int i = all.size() - 1; i >= 0; i--) if (groups.contains(all.get(i).id())) return all.get(i);
        return all.getFirst();
    }
}
