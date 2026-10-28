package net.y4k.queue;

import java.util.regex.Pattern;

/**
 * What one minigame server publishes about itself every second: which mode and map it runs now,
 * its match stage and how full it is. Server names double as Velocity server names.
 */
public record GameStatus(String server, String mode, String map, Stage stage, int players, int maxPlayers, long updatedAt) {
    public enum Stage { WAITING, STARTING, PLAYING, ENDING, OFFLINE }
    private static final Pattern NAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

    public GameStatus {
        if (server == null || !NAME.matcher(server).matches()) throw new IllegalArgumentException("Nome de servidor inválido: " + server);
        if (mode == null || mode.isBlank()) throw new IllegalArgumentException("Modo ausente");
        if (players < 0 || maxPlayers < 0) throw new IllegalArgumentException("Contagem negativa");
        map = map == null ? "" : map;
    }

    public static boolean validServer(String name) { return name != null && NAME.matcher(name).matches(); }

    /** Accepts new players into the current match. */
    public boolean joinable() { return (stage == Stage.WAITING || stage == Stage.STARTING) && players < maxPlayers; }
    public int freeSlots() { return Math.max(0, maxPlayers - players); }
    /** Statuses not refreshed within {@code staleMillis} count as offline. */
    public GameStatus at(long now, long staleMillis) {
        return now - updatedAt > staleMillis && stage != Stage.OFFLINE
            ? new GameStatus(server, mode, map, Stage.OFFLINE, 0, maxPlayers, updatedAt) : this;
    }
}
