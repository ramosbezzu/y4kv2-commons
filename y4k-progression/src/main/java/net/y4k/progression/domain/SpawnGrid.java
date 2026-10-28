package net.y4k.progression.domain;

import java.util.*;

/** Bounded lattice: randomness changes ordering, never the size of the explored area. */
public final class SpawnGrid {
    public record Point(int x, int z) { public String key() { return x + ":" + z; } }
    private final int spacing, radius;
    public SpawnGrid(int spacing, int radius) {
        if (spacing < 50 || radius < spacing || radius > 1000 || radius % spacing != 0)
            throw new IllegalArgumentException("Spawn spacing/radius invalid");
        this.spacing = spacing; this.radius = radius;
    }
    public List<Point> candidates(int originX, int originZ, UUID player) {
        var result = new ArrayList<Point>();
        for (int x = -radius; x <= radius; x += spacing) for (int z = -radius; z <= radius; z += spacing) {
            if ((long)x*x + (long)z*z <= (long)radius*radius && (x != 0 || z != 0))
                result.add(new Point(Math.addExact(originX, x), Math.addExact(originZ, z)));
        }
        Collections.shuffle(result, new Random(player.getMostSignificantBits() ^ player.getLeastSignificantBits()));
        return List.copyOf(result);
    }
    public int radius() { return radius; }
}
