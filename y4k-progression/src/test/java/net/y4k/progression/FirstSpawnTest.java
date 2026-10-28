package net.y4k.progression;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.*;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import net.y4k.progression.application.*;
import net.y4k.progression.domain.*;
import net.y4k.progression.paper.*;
import static org.assertj.core.api.Assertions.*;
class FirstSpawnTest {
 @TempDir Path directory; ServerMock server; JavaPlugin plugin; World world; PlayerMock player; SpawnLedger ledger;
 @BeforeEach void setup() throws Exception {server=MockBukkit.mock();world=server.addSimpleWorld("world");plugin=MockBukkit.createMockPlugin();player=server.addPlayer();ledger=new SpawnLedger(directory.resolve("history"),world.getUID());}
 @AfterEach void cleanup(){MockBukkit.unmock();}
 Terrain terrain(boolean safe){return new Terrain(){public CompletableFuture<Void> loadExisting(SpawnGrid.Point p){return CompletableFuture.completedFuture(null);}public Optional<Integer> safeY(SpawnGrid.Point p){return safe?Optional.of(70):Optional.empty();}};}
 FirstSpawn listener(boolean safe,int attempts){return new FirstSpawn(plugin,world,terrain(safe),ledger,new SpawnGrid(50,250),attempts,(p,to)->CompletableFuture.completedFuture(p.teleport(to)));}
 @Test void firstAllocationPersistsAndReconnectDoesNotMovePlayer() throws Exception {
  var listener=listener(true,80);listener.begin(player,true);
  assertThat(player.nextMessage()).contains("[","!","]").doesNotContain(" / ");
  server.getScheduler().performTicks(5);
  assertThat(listener.waiting(player)).isFalse();assertThat(ledger.used()).hasSize(1);assertThat(player.getLocation().getY()).isEqualTo(70);
  var original=player.getLocation();listener.begin(player,true);server.getScheduler().performTicks(5);assertThat(player.getLocation()).isEqualTo(original);
  assertThat(new SpawnLedger(directory.resolve("history"),world.getUID()).used()).hasSize(1);
 }
 @Test void concurrentPlayersNeverShareAReservedCell(){
  var listener=listener(true,80);var other=server.addPlayer();listener.begin(player,true);listener.begin(other,true);server.getScheduler().performTicks(10);
  assertThat(ledger.used()).hasSize(2);assertThat(player.getLocation().distance(other.getLocation())).isGreaterThanOrEqualTo(50);
 }
 @Test void unsafeTerrainFailsWithoutTeleportOrUnboundedExpansion(){
  var listener=listener(false,2);var original=player.getLocation();listener.begin(player,true);server.getScheduler().performTicks(10);
  assertThat(ledger.used()).isEmpty();assertThat(listener.waiting(player)).isFalse();assertThat(player.getLocation()).isEqualTo(original);assertThat(player.isOnline()).isFalse();
 }
 @Test void waitingPlayersCannotMoveInteractOrTakeDamage(){
  var future=new CompletableFuture<Void>();var slow=new Terrain(){public CompletableFuture<Void> loadExisting(SpawnGrid.Point p){return future;}public Optional<Integer> safeY(SpawnGrid.Point p){return Optional.of(70);}};
  var listener=new FirstSpawn(plugin,world,slow,ledger,new SpawnGrid(50,250),2,(p,to)->CompletableFuture.completedFuture(true));listener.begin(player,true);
  var from=player.getLocation();var movement=new PlayerMoveEvent(player,from,from.clone().add(10,0,0));listener.move(movement);assertThat(movement.getTo()).isEqualTo(from);
  var event=new EntityDamageEvent(player,EntityDamageEvent.DamageCause.FALL,10);listener.damage(event);assertThat(event.isCancelled()).isTrue();
  listener.quit(new PlayerQuitEvent(player,net.kyori.adventure.text.Component.empty()));future.complete(null);server.getScheduler().performTicks(5);assertThat(ledger.used()).isEmpty();
 }
 @Test void returningPlayersAndOtherWorldsArePreserved(){
  var listener=listener(true,80);listener.begin(player,false);server.getScheduler().performTicks(5);assertThat(ledger.used()).isEmpty();
  player.teleport(new Location(server.addSimpleWorld("other"),0,70,0));listener.begin(player,true);server.getScheduler().performTicks(5);assertThat(ledger.used()).isEmpty();
 }
 @Test void terrainChangesPersistAndCancelledBreaksAreNotRecorded() throws Exception {
  var listener=listener(true,80);server.getPluginManager().registerEvents(listener,plugin);
  var block=world.getBlockAt(60,64,0);block.setType(Material.GRASS_BLOCK);
  var cancelled=new BlockBreakEvent(block,player);cancelled.setCancelled(true);server.getPluginManager().callEvent(cancelled);assertThat(ledger.touched(60,0,0)).isFalse();
  server.getPluginManager().callEvent(new BlockBreakEvent(block,player));assertThat(new SpawnLedger(directory.resolve("history"),world.getUID()).touched(60,0,32)).isTrue();
 }
 @Test void failedTeleportDoesNotMarkPlayerSettled(){
  var listener=new FirstSpawn(plugin,world,terrain(true),ledger,new SpawnGrid(50,250),2,(p,to)->CompletableFuture.completedFuture(false));
  listener.begin(player,true);server.getScheduler().performTicks(6);assertThat(listener.waiting(player)).isFalse();assertThat(player.isOnline()).isFalse();
  assertThat(player.getPersistentDataContainer().has(new NamespacedKey(plugin,"survival-settled"))).isFalse();
 }
 @Test void synchronousChunkAndTeleportErrorsCannotLeavePlayersWaitingForever(){
  var bad=new Terrain(){public CompletableFuture<Void> loadExisting(SpawnGrid.Point p){throw new IllegalStateException("io");}public Optional<Integer> safeY(SpawnGrid.Point p){return Optional.of(70);}};
  var listener=new FirstSpawn(plugin,world,bad,ledger,new SpawnGrid(50,250),2,(p,to)->CompletableFuture.completedFuture(true));
  listener.begin(player,true);server.getScheduler().performTicks(8);assertThat(listener.waiting(player)).isFalse();assertThat(ledger.used()).isEmpty();
  var other=server.addPlayer();var failed=new FirstSpawn(plugin,world,terrain(true),ledger,new SpawnGrid(50,250),2,(p,to)->{throw new IllegalStateException("teleport");});
  failed.begin(other,true);server.getScheduler().performTicks(8);assertThat(failed.waiting(other)).isFalse();assertThat(other.isOnline()).isFalse();
 }
 @Test void rankCommandUsesEarnedMembershipAndHandlesConsole(){
  var command=new RankCommands("survival",p->Set.of("omega","sigma","surv_explorador"));
  assertThat(command.onCommand(player,null,"ranks",new String[0])).isTrue();
  assertThat(player.nextMessage()).contains("Rank","Explorador").doesNotContain("•");
  assertThat(player.nextMessage()).contains("Sobrevivente");
  assertThat(command.onCommand(server.getConsoleSender(),null,"ranks",new String[0])).isTrue();
  assertThat(server.getConsoleSender().nextMessage()).contains("Must be ran as a player");
 }
}
