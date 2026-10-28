package net.y4k.queue;

import java.sql.*;
import java.util.*;

/**
 * Status table shared by minigame servers (writers) and hubs (readers), on the network MariaDB.
 * One row per server, upserted every second. Blocking JDBC: call it off the server thread.
 */
public final class StatusStore {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }

    static final String TABLE = "y4k_game_status";
    private final Connections connections;

    public StatusStore(Connections connections) { this.connections = connections; }

    public void createTable() throws SQLException {
        try (Connection c = connections.open(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " (server VARCHAR(32) PRIMARY KEY, mode VARCHAR(32) NOT NULL,"
                + " map VARCHAR(64) NOT NULL, stage VARCHAR(16) NOT NULL, players INT NOT NULL, max_players INT NOT NULL, updated_at BIGINT NOT NULL)");
        }
    }

    public void publish(GameStatus status) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement p = c.prepareStatement(
            "INSERT INTO " + TABLE + " (server, mode, map, stage, players, max_players, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE mode = VALUES(mode), map = VALUES(map), stage = VALUES(stage), players = VALUES(players),"
                + " max_players = VALUES(max_players), updated_at = VALUES(updated_at)")) {
            p.setString(1, status.server()); p.setString(2, status.mode()); p.setString(3, truncate(status.map(), 64));
            p.setString(4, status.stage().name()); p.setInt(5, status.players()); p.setInt(6, status.maxPlayers()); p.setLong(7, status.updatedAt());
            p.executeUpdate();
        }
    }

    /** Every row; rows with unknown stages or bad names are skipped rather than breaking the board. */
    public List<GameStatus> all() throws SQLException {
        List<GameStatus> out = new ArrayList<>();
        try (Connection c = connections.open(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT server, mode, map, stage, players, max_players, updated_at FROM " + TABLE)) {
            while (r.next()) {
                try {
                    out.add(new GameStatus(r.getString(1), r.getString(2), r.getString(3), GameStatus.Stage.valueOf(r.getString(4)),
                        r.getInt(5), r.getInt(6), r.getLong(7)));
                } catch (IllegalArgumentException ignored) { /* foreign or corrupt row */ }
            }
        }
        return out;
    }

    /** Marks a server offline on clean shutdown so hubs stop sending players at once. */
    public void offline(String server, long now) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement p = c.prepareStatement(
            "UPDATE " + TABLE + " SET stage = 'OFFLINE', players = 0, updated_at = ? WHERE server = ?")) {
            p.setLong(1, now); p.setString(2, server); p.executeUpdate();
        }
    }

    private static String truncate(String s, int max) { return s.length() <= max ? s : s.substring(0, max); }
}
