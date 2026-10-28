package net.y4k.queue;

import java.sql.DriverManager;
import java.util.*;
import org.junit.jupiter.api.Test;
import net.y4k.queue.GameStatus.Stage;
import net.y4k.queue.QueueBoard.State;
import static org.assertj.core.api.Assertions.*;

class QueueTest {
    static final long NOW = 1_000_000, STALE = 5_000;
    static final ModeDef SKYWARS = new ModeDef("skywars", "Skywars", "ender_pearl", 0, "skywars");
    static final ModeDef TNT = new ModeDef("tnt_wars", "TNT Wars", "tnt", 1, null);
    static final ModeDef DM = new ModeDef("deathmatch", "Deathmatch", "iron_sword", 1, null);
    static final ModeDef SPLEEF = new ModeDef("spleef_runner", "Spleef Runner", "iron_hoe", 1, null);

    static GameStatus status(String server, String mode, Stage stage, int players, int max) {
        return new GameStatus(server, mode, "map", stage, players, max, NOW - 1000);
    }

    @Test void statusValidatesAndAges() {
        GameStatus s = status("games-1", "deathmatch", Stage.WAITING, 3, 8);
        assertThat(s.joinable()).isTrue();
        assertThat(s.freeSlots()).isEqualTo(5);
        assertThat(status("games-1", "deathmatch", Stage.STARTING, 8, 8).joinable()).as("full").isFalse();
        assertThat(status("games-1", "deathmatch", Stage.PLAYING, 1, 8).joinable()).isFalse();
        assertThat(s.at(NOW + 10_000, STALE).stage()).isEqualTo(Stage.OFFLINE);
        assertThat(s.at(NOW, STALE)).isSameAs(s);
        assertThatThrownBy(() -> status("Games 1", "dm", Stage.WAITING, 0, 1)).hasMessageContaining("inválido");
        assertThatThrownBy(() -> status("../x", "dm", Stage.WAITING, 0, 1)).hasMessageContaining("inválido");
        assertThatThrownBy(() -> status("g", " ", Stage.WAITING, 0, 1)).hasMessageContaining("Modo");
        assertThatThrownBy(() -> status("g", "dm", Stage.WAITING, -1, 1)).hasMessageContaining("negativa");
        assertThatThrownBy(() -> new ModeDef("x", "X", null, 0, "bad server")).hasMessageContaining("fallback");
        assertThat(new ModeDef("x", null, null, 0, null).name()).isEqualTo("x");
    }

    @Test void boardOrdersByPriorityThenMembersAndPicksBestServer() {
        var statuses = List.of(
            status("games-1", "tnt_wars", Stage.PLAYING, 6, 8),
            status("games-2", "deathmatch", Stage.WAITING, 2, 8),
            status("games-3", "deathmatch", Stage.STARTING, 1, 8),
            status("games-4", "deathmatch", Stage.WAITING, 5, 8),
            new GameStatus("games-5", "spleef_runner", "m", Stage.WAITING, 7, 8, NOW - 60_000));
        var rows = QueueBoard.build(List.of(DM, TNT, SPLEEF, SKYWARS), statuses, Map.of("deathmatch", 1, "tnt_wars", 4), NOW, STALE);
        assertThat(rows).extracting(r -> r.mode().id()).as("tier 0 first, then members desc")
            .containsExactly("skywars", "tnt_wars", "deathmatch", "spleef_runner");
        var dm = rows.get(2);
        assertThat(dm.best()).as("countdown beats the fuller waiting lobby").contains("games-3");
        assertThat(dm.state()).isEqualTo(State.COUNTDOWN);
        assertThat(dm.playing()).isEqualTo(8);
        assertThat(dm.members()).isEqualTo(9);
        assertThat(rows.get(1).state()).isEqualTo(State.IN_PROGRESS);
        assertThat(rows.get(1).best()).isEmpty();
        assertThat(rows.get(3).state()).as("stale status is offline").isEqualTo(State.OFFLINE);
        assertThat(rows.get(0).state()).isEqualTo(State.FALLBACK);
        assertThat(rows.get(0).best()).contains("skywars");
        assertThat(QueueBoard.quickPlay(rows).orElseThrow().mode().id()).as("busiest real lobby, not the fallback").isEqualTo("deathmatch");
        var onlyFallback = QueueBoard.build(List.of(SKYWARS, TNT), List.of(), Map.of(), NOW, STALE);
        assertThat(QueueBoard.quickPlay(onlyFallback).orElseThrow().mode().id()).isEqualTo("skywars");
        assertThat(QueueBoard.quickPlay(QueueBoard.build(List.of(TNT), List.of(), Map.of(), NOW, STALE))).isEmpty();
    }

    @Test void engineServerReplacesFallbackWhenItRunsTheMode() {
        var rows = QueueBoard.build(List.of(SKYWARS), List.of(status("games-1", "skywars", Stage.WAITING, 0, 12)), Map.of(), NOW, STALE);
        assertThat(rows.get(0).best()).contains("games-1");
        var busy = QueueBoard.build(List.of(SKYWARS), List.of(status("games-1", "skywars", Stage.PLAYING, 4, 12)), Map.of(), NOW, STALE);
        assertThat(busy.get(0).best()).as("live but full: wait, do not split players to the fallback").isEmpty();
    }

    @Test void queueKeepsOnePlacePerPlayerAndDispatchesIntoFreeSlots() {
        var q = new MatchQueue(3000);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID();
        assertThat(q.join(a, "deathmatch")).isEqualTo(1);
        assertThat(q.join(b, "deathmatch")).isEqualTo(2);
        assertThat(q.join(c, "tnt_wars")).isEqualTo(1);
        assertThat(q.join(a, "tnt_wars")).as("switching queues").isEqualTo(2);
        assertThat(q.position(b)).isEqualTo(1);
        assertThat(q.sizes()).containsEntry("deathmatch", 1).containsEntry("tnt_wars", 2);
        q.join(d, "deathmatch");
        var statuses = List.of(status("games-2", "deathmatch", Stage.WAITING, 7, 8), status("games-1", "tnt_wars", Stage.PLAYING, 2, 8));
        var sent = q.dispatch(statuses, NOW, STALE);
        assertThat(sent).containsExactly(new MatchQueue.Transfer(b, "deathmatch", "games-2"));
        assertThat(q.modeOf(b)).isEmpty();
        assertThat(q.position(d)).isEqualTo(1);
        assertThat(q.dispatch(statuses, NOW + 500, STALE)).as("slot reserved until the next status").isEmpty();
        assertThat(q.dispatch(List.of(status("games-2", "deathmatch", Stage.WAITING, 7, 8)), NOW + 3500, STALE))
            .as("reservation expired, status still shows a free slot").extracting(MatchQueue.Transfer::player).containsExactly(d);
        assertThat(q.leave(c)).contains("tnt_wars");
        assertThat(q.leave(c)).isEmpty();
        assertThat(q.position(c)).isZero();
    }

    @Test void directClickReservationBlocksQueueOverfill() {
        var q = new MatchQueue(3000);
        UUID a = UUID.randomUUID();
        q.join(a, "deathmatch");
        q.reserve("games-2", NOW);
        assertThat(q.dispatch(List.of(status("games-2", "deathmatch", Stage.WAITING, 7, 8)), NOW, STALE)).isEmpty();
        assertThat(q.dispatch(List.of(status("games-2", "deathmatch", Stage.WAITING, 7, 8)), NOW + 3001, STALE)).hasSize(1);
    }

    @Test void storeUpsertsReadsAndMarksOffline() throws Exception {
        String url = "jdbc:h2:mem:queue" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        var store = new StatusStore(() -> DriverManager.getConnection(url));
        store.createTable();
        store.createTable();
        store.publish(new GameStatus("games-1", "deathmatch", "Arena", Stage.WAITING, 1, 8, NOW));
        store.publish(new GameStatus("games-1", "capture", "Viper".repeat(20), Stage.PLAYING, 6, 12, NOW + 1000));
        store.publish(new GameStatus("games-2", "tnt_wars", "Metro", Stage.STARTING, 3, 8, NOW));
        try (var c = DriverManager.getConnection(url); var s = c.createStatement()) {
            s.executeUpdate("INSERT INTO y4k_game_status VALUES ('BAD NAME', 'x', 'm', 'WAITING', 0, 1, 0)");
            s.executeUpdate("INSERT INTO y4k_game_status VALUES ('games-9', 'x', 'm', 'EXPLODED', 0, 1, 0)");
        }
        var rows = store.all();
        assertThat(rows).extracting(GameStatus::server).containsExactlyInAnyOrder("games-1", "games-2");
        GameStatus g1 = rows.stream().filter(r -> r.server().equals("games-1")).findFirst().orElseThrow();
        assertThat(g1.mode()).isEqualTo("capture");
        assertThat(g1.map()).hasSize(64);
        assertThat(g1.updatedAt()).isEqualTo(NOW + 1000);
        store.offline("games-1", NOW + 2000);
        assertThat(store.all()).filteredOn(r -> r.server().equals("games-1")).extracting(GameStatus::stage).containsExactly(Stage.OFFLINE);
    }
}
