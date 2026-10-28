package net.y4k.queue;

import java.util.*;

/**
 * The rotating minigame board shown in the hub: one row per mode with live numbers, ordered by
 * priority tier and then by how many people are in it (playing plus queued), so busy modes rise
 * and fill faster. This is the Y4K "server menu" behaviour, computed from published statuses.
 */
public final class QueueBoard {
    public enum State { OPEN, COUNTDOWN, IN_PROGRESS, FALLBACK, OFFLINE }

    /** One mode row. {@code best} is where a click sends a player right now, if anywhere. */
    public record ModeView(ModeDef mode, State state, int playing, int queued, int servers, Optional<String> best) {
        public int members() { return playing + queued; }
    }

    private QueueBoard() {}

    public static List<ModeView> build(List<ModeDef> modes, Collection<GameStatus> statuses, Map<String, Integer> queued, long now, long staleMillis) {
        List<ModeView> rows = new ArrayList<>();
        for (ModeDef mode : modes) {
            List<GameStatus> live = statuses.stream().map(s -> s.at(now, staleMillis))
                .filter(s -> s.mode().equals(mode.id()) && s.stage() != GameStatus.Stage.OFFLINE).toList();
            int playing = live.stream().mapToInt(GameStatus::players).sum();
            Optional<GameStatus> best = bestServer(live);
            State state;
            if (best.isPresent()) state = best.get().stage() == GameStatus.Stage.STARTING ? State.COUNTDOWN : State.OPEN;
            else if (!live.isEmpty()) state = State.IN_PROGRESS;
            else if (mode.fallbackServer() != null) state = State.FALLBACK;
            else state = State.OFFLINE;
            Optional<String> target = best.map(GameStatus::server).or(() -> live.isEmpty() ? Optional.ofNullable(mode.fallbackServer()) : Optional.empty());
            rows.add(new ModeView(mode, state, playing, queued.getOrDefault(mode.id(), 0), live.size(), target));
        }
        rows.sort(Comparator.comparingInt((ModeView v) -> v.mode().priority())
            .thenComparing(Comparator.comparingInt(ModeView::members).reversed())
            .thenComparing(v -> v.mode().name()));
        return rows;
    }

    /** Joinable server closest to starting: countdown first, then the fullest lobby, then name. */
    static Optional<GameStatus> bestServer(List<GameStatus> live) {
        return live.stream().filter(GameStatus::joinable)
            .min(Comparator.comparingInt((GameStatus s) -> s.stage() == GameStatus.Stage.STARTING ? 0 : 1)
                .thenComparing(Comparator.comparingInt(GameStatus::players).reversed())
                .thenComparing(GameStatus::server));
    }

    /** "Quick play": the busiest mode that has somewhere to go right now. */
    public static Optional<ModeView> quickPlay(List<ModeView> rows) {
        return rows.stream().filter(r -> r.best().isPresent() && r.state() != State.FALLBACK)
            .max(Comparator.comparingInt(ModeView::playing))
            .or(() -> rows.stream().filter(r -> r.best().isPresent()).findFirst());
    }
}
