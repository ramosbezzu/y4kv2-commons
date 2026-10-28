package net.y4k.queue;

import java.util.*;

/**
 * Hub-side FIFO per mode. A player waits in at most one queue. Each dispatch hands queued players
 * the free slots of joinable servers, reserving slots already promised in the last few seconds so two
 * hubs or two quick dispatches do not overfill a lobby before its next status arrives.
 */
public final class MatchQueue {
    public record Transfer(UUID player, String mode, String server) {}
    private record Reservation(String server, long until) {}

    private final Map<String, LinkedHashSet<UUID>> queues = new LinkedHashMap<>();
    private final Map<UUID, String> modeOf = new HashMap<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private final long reserveMillis;

    public MatchQueue(long reserveMillis) { this.reserveMillis = reserveMillis; }

    /** Joins a mode's queue, leaving any other. Returns the 1-based position. */
    public int join(UUID player, String mode) {
        leave(player);
        LinkedHashSet<UUID> q = queues.computeIfAbsent(mode, m -> new LinkedHashSet<>());
        q.add(player);
        modeOf.put(player, mode);
        return q.size();
    }
    public Optional<String> leave(UUID player) {
        String mode = modeOf.remove(player);
        if (mode != null) queues.get(mode).remove(player);
        return Optional.ofNullable(mode);
    }
    public Optional<String> modeOf(UUID player) { return Optional.ofNullable(modeOf.get(player)); }
    public int position(UUID player) {
        String mode = modeOf.get(player);
        if (mode == null) return 0;
        int i = 1;
        for (UUID p : queues.get(mode)) { if (p.equals(player)) return i; i++; }
        return 0;
    }
    public Map<String, Integer> sizes() {
        Map<String, Integer> out = new HashMap<>();
        queues.forEach((m, q) -> { if (!q.isEmpty()) out.put(m, q.size()); });
        return out;
    }

    /** Promises one slot of {@code server} (a direct click) so the next dispatch accounts for it. */
    public void reserve(String server, long now) { reservations.add(new Reservation(server, now + reserveMillis)); }

    public List<Transfer> dispatch(Collection<GameStatus> statuses, long now, long staleMillis) {
        reservations.removeIf(r -> r.until() <= now);
        List<Transfer> out = new ArrayList<>();
        for (var entry : queues.entrySet()) {
            String mode = entry.getKey();
            Iterator<UUID> waiting = entry.getValue().iterator();
            List<GameStatus> open = statuses.stream().map(s -> s.at(now, staleMillis))
                .filter(s -> s.mode().equals(mode) && s.joinable())
                .sorted(Comparator.comparingInt((GameStatus s) -> s.stage() == GameStatus.Stage.STARTING ? 0 : 1)
                    .thenComparing(Comparator.comparingInt(GameStatus::players).reversed()).thenComparing(GameStatus::server))
                .toList();
            for (GameStatus server : open) {
                long held = reservations.stream().filter(r -> r.server().equals(server.server())).count();
                long free = server.freeSlots() - held;
                while (free-- > 0 && waiting.hasNext()) {
                    UUID player = waiting.next();
                    waiting.remove();
                    modeOf.remove(player);
                    reservations.add(new Reservation(server.server(), now + reserveMillis));
                    out.add(new Transfer(player, mode, server.server()));
                }
            }
        }
        return out;
    }
}
