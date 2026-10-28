package net.y4k.progression;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.*;
import net.y4k.progression.application.SpawnLedger;
import net.y4k.progression.domain.SpawnGrid.Point;
import net.y4k.progression.paper.*;
import static org.assertj.core.api.Assertions.*;
class PaperTerrainTest {
 @TempDir Path directory;ServerMock server;World backing,world;SpawnLedger ledger;boolean generated=true,inside=true;int loads;
 @BeforeEach void setup() throws Exception {
  server=MockBukkit.mock();backing=server.addSimpleWorld("world");ledger=new SpawnLedger(directory.resolve("history"),backing.getUID());
  for(int x=42;x<=58;x++)for(int z=-8;z<=8;z++)backing.getBlockAt(x,64,z).setType(Material.GRASS_BLOCK);
  var border=(WorldBorder)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{WorldBorder.class},(p,m,a)->m.getName().equals("isInside")?inside:m.invoke(backing.getWorldBorder(),a));
  world=(World)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{World.class},(p,m,a)->switch(m.getName()){
   case "isChunkGenerated","isChunkLoaded" -> generated;
   case "getChunkAtAsync" -> {assertThat(a[2]).isEqualTo(false);loads++;yield CompletableFuture.completedFuture(backing.getChunkAt((int)a[0],(int)a[1]));}
   case "getHighestBlockYAt" -> 64;
   case "getSeaLevel" -> 63;
   case "getWorldBorder" -> border;
   default -> m.invoke(backing,a);
  });
 }
 @AfterEach void cleanup(){MockBukkit.unmock();}
 PaperTerrain terrain(){return new PaperTerrain(world,ledger,new ClaimGuard(server.getPluginManager()),32);}
 @Test void existingChunksLoadWithoutGenerationAndNaturalGroundIsSafe(){
  terrain().loadExisting(new Point(50,0)).join();assertThat(loads).isLessThanOrEqualTo(4);assertThat(terrain().safeY(new Point(50,0))).contains(65);
 }
 @Test void ungeneratedTerrainIsRejectedWithoutAnyChunkLoad(){
  generated=false;assertThatThrownBy(()->terrain().loadExisting(new Point(50,0)).join()).isInstanceOf(java.util.concurrent.CompletionException.class);assertThat(loads).isZero();assertThat(terrain().safeY(new Point(50,0))).isEmpty();
 }
 @Test void alteredHazardousAndOutsideBorderGroundIsRejected() throws Exception {
  inside=false;assertThat(terrain().safeY(new Point(50,0))).isEmpty();inside=true;
  backing.getBlockAt(50,64,0).setType(Material.LAVA);assertThat(terrain().safeY(new Point(50,0))).isEmpty();
  backing.getBlockAt(50,64,0).setType(Material.GRASS_BLOCK);backing.getBlockAt(50,65,0).setType(Material.OAK_PLANKS);assertThat(terrain().safeY(new Point(50,0))).isEmpty();
  backing.getBlockAt(50,65,0).setType(Material.AIR);ledger.touch(4,0);assertThat(terrain().safeY(new Point(50,0))).isEmpty();
 }
 @Test void missingClaimProviderAllowsWildernessButUnknownInstalledProviderFailsClosed(){
  var guard=new ClaimGuard(server.getPluginManager());var location=new Location(backing,50,65,0);assertThat(guard.free(location,32)).isTrue();
  MockBukkit.createMockPlugin("WorldGuard");assertThat(guard.free(location,32)).isFalse();
 }
 @Test void nearbyTownyClaimRejectsLandingEvenIfCenterIsFree(){
  var towny=MockBukkit.createMockPlugin("Towny");var location=new Location(backing,50,65,0);
  assertThat(new ClaimGuard(server.getPluginManager(),p->false).free(location,32)).isTrue();
  assertThat(new ClaimGuard(server.getPluginManager(),p->p.getBlockX()==66).free(location,32)).isFalse();
  server.getPluginManager().disablePlugin(towny);
  assertThat(new ClaimGuard(server.getPluginManager(),p->false).free(location,32)).isFalse();
 }
 @Test void brokenTownyAdapterFailsClosed(){
  MockBukkit.createMockPlugin("Towny");assertThat(new ClaimGuard(server.getPluginManager()).free(new Location(backing,50,65,0),32)).isFalse();
 }
}
