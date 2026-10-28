package net.y4k.progression.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Player-facing progression text in PT/EN/ES with the Year4000 '&' palette: labels &6, values &e,
 * separator &7:, notices ' &7[&e!&7] &e' (MapNodes Msg.NOTICE) and errors &c. Pure, no Paper API.
 */
public final class ProgressionText {
    public static final String NOTICE = " &7[&e!&7] &e";
    /** Locales/utilities cmd.error.player, literal. */
    public static final String PLAYER_ONLY = "&cMust be ran as a player&7!";

    private ProgressionText() {}

    static String language(Locale locale) {
        String language = locale == null ? "" : locale.getLanguage();
        return language.equals("pt") || language.equals("es") ? language : "en";
    }

    private static String pick(Locale locale, String pt, String en, String es) {
        return switch (language(locale)) { case "pt" -> pt; case "es" -> es; default -> en; };
    }

    /** Shown while the first safe spot is searched. */
    public static String searching(Locale locale) {
        return NOTICE + pick(locale, "Preparando um ponto inicial seguro&7...", "Finding a safe starting point&7...",
            "Buscando un punto de inicio seguro&7...");
    }

    /** Kick reason when no safe spot was found. */
    public static String noSafeSpot(Locale locale) {
        return "&c" + pick(locale, "Sem ponto seguro disponível na área inicial. Tente novamente ou avise a staff.",
            "No safe starting point is available. Try again or contact staff.",
            "No hay un punto de inicio seguro disponible. Inténtalo de nuevo o avisa al staff.");
    }

    /** Display name of a progression mode. */
    public static String mode(String mode) {
        return switch (mode) { case "survival" -> "Survival"; case "skyblock" -> "SkyBlock"; default -> mode; };
    }

    /** /ranks: current rank, the track with the current one highlighted, and the VIP note. */
    public static List<String> ranks(Locale locale, String mode, RankTrack.Rank current, List<RankTrack.Rank> track) {
        var lines = new ArrayList<String>();
        lines.add("&6" + pick(locale, "Rank", "Rank", "Rango") + "&7: &e" + current.name() + " &7(&e" + mode(mode) + "&7)");
        for (var rank : track) {
            lines.add(rank.equals(current) ? " &a&l" + rank.name() + " &7(" + pick(locale, "atual", "current", "actual") + ")" : " &7" + rank.name());
        }
        lines.add(NOTICE + pick(locale, "A progressão é conquistada jogando e é separada do VIP.",
            "Progression is earned by playing and is separate from VIP.",
            "La progresión se gana jugando y está separada del VIP."));
        return lines;
    }
}
