package net.y4k.queue;

/**
 * A minigame in the selector. {@code priority} is a tier: lower tiers are listed first, and
 * inside a tier the busiest mode leads. {@code fallbackServer} is a fixed server used when no engine
 * server runs the mode (SkyWars on MythicSkyWars during the beta).
 */
public record ModeDef(String id, String name, String icon, int priority, String fallbackServer) {
    public ModeDef {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Modo sem id");
        if (name == null || name.isBlank()) name = id;
        if (icon == null || icon.isBlank()) icon = "paper";
        if (fallbackServer != null && !GameStatus.validServer(fallbackServer)) throw new IllegalArgumentException("fallback inválido: " + fallbackServer);
    }
}
