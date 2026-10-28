package net.y4k.progression;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import net.y4k.progression.domain.ProgressionText;
import net.y4k.progression.domain.RankTrack;
import org.junit.jupiter.api.Test;

class ProgressionTextTest {
    private static final Locale PT = Locale.of("pt", "BR"), ES = Locale.of("es"), EN = Locale.ENGLISH;

    @Test void ranksUseTheYear4000LabelValueLayoutInEveryLanguage() {
        var track = RankTrack.in("skyblock");
        var lines = ProgressionText.ranks(PT, "skyblock", track.get(1), track);
        assertThat(lines.getFirst()).isEqualTo("&6Rank&7: &eColono &7(&eSkyBlock&7)");
        assertThat(lines).contains(" &a&lColono &7(atual)", " &7Ilhéu");
        assertThat(lines.getLast()).startsWith(" &7[&e!&7] &e").contains("separada do VIP");
        assertThat(ProgressionText.ranks(ES, "survival", RankTrack.SURVIVAL.getFirst(), RankTrack.SURVIVAL).getFirst()).startsWith("&6Rango&7: ");
        assertThat(ProgressionText.ranks(EN, "survival", RankTrack.SURVIVAL.getFirst(), RankTrack.SURVIVAL).getLast()).contains("separate from VIP");
        assertThat(lines).hasSize(track.size() + 2).noneMatch(line -> line.contains("•") || line.contains("—") || line.contains(" / "));
    }

    @Test void spawnSearchIsANoticeAndFailureIsRed() {
        assertThat(ProgressionText.searching(PT)).isEqualTo(" &7[&e!&7] &ePreparando um ponto inicial seguro&7...");
        assertThat(ProgressionText.searching(null)).contains("Finding a safe starting point");
        assertThat(ProgressionText.searching(ES)).contains("Buscando");
        assertThat(ProgressionText.noSafeSpot(PT)).startsWith("&c").contains("avise a staff");
        assertThat(ProgressionText.noSafeSpot(Locale.FRENCH)).startsWith("&cNo safe starting point");
        assertThat(ProgressionText.PLAYER_ONLY).isEqualTo("&cMust be ran as a player&7!");
        assertThat(ProgressionText.mode("creative")).isEqualTo("creative");
    }
}
