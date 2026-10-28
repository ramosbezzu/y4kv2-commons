package net.y8k.progression.application;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.y8k.progression.domain.SpawnGrid.Point;
public interface Terrain {
 CompletableFuture<Void> loadExisting(Point point);
 Optional<Integer> safeY(Point point);
}
