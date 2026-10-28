package net.y8k.progression.paper;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import net.y8k.progression.application.SpawnLedger;
import net.y8k.progression.domain.SpawnGrid.Point;
public final class PaperTerrain implements net.y8k.progression.application.Terrain {
 private static final Set<Material> GROUND=Set.of(Material.GRASS_BLOCK,Material.DIRT,Material.PODZOL,Material.COARSE_DIRT,Material.ROOTED_DIRT,Material.SAND,Material.RED_SAND,Material.GRAVEL,Material.MOSS_BLOCK,Material.MYCELIUM);
 private final World world;private final SpawnLedger ledger;private final ClaimGuard claims;private final int clearance;
 public PaperTerrain(World world,SpawnLedger ledger,ClaimGuard claims,int clearance){this.world=world;this.ledger=ledger;this.claims=claims;this.clearance=clearance;}
 /** Load only existing chunks for a 17x17 footprint, with generate=false. */
 public CompletableFuture<Void> loadExisting(Point point){
  var futures=new ArrayList<CompletableFuture<Chunk>>();
  for(int x=(point.x()-8)>>4;x<=(point.x()+8)>>4;x++)for(int z=(point.z()-8)>>4;z<=(point.z()+8)>>4;z++)
   if(!world.isChunkGenerated(x,z))return CompletableFuture.failedFuture(new IllegalStateException("Ungenerated terrain"));
  for(int x=(point.x()-8)>>4;x<=(point.x()+8)>>4;x++)for(int z=(point.z()-8)>>4;z<=(point.z()+8)>>4;z++)futures.add(world.getChunkAtAsync(x,z,false));
  return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
 }
 public Optional<Integer> safeY(Point point){
  if(ledger.touched(point.x(),point.z(),clearance))return Optional.empty();
  for(int x=(point.x()-8)>>4;x<=(point.x()+8)>>4;x++)for(int z=(point.z()-8)>>4;z<=(point.z()+8)>>4;z++)if(!world.isChunkLoaded(x,z))return Optional.empty();
  int center=world.getHighestBlockYAt(point.x(),point.z(),HeightMap.MOTION_BLOCKING_NO_LEAVES);
  var destination=new Location(world,point.x()+0.5,center+1,point.z()+0.5);
  if(!world.getWorldBorder().isInside(destination)||!claims.free(destination,clearance))return Optional.empty();
  for(int x=-8;x<=8;x+=4)for(int z=-8;z<=8;z+=4){
   int y=world.getHighestBlockYAt(point.x()+x,point.z()+z,HeightMap.MOTION_BLOCKING_NO_LEAVES);
   if(Math.abs(y-center)>3||y<world.getSeaLevel()-2||!GROUND.contains(world.getBlockAt(point.x()+x,y,point.z()+z).getType()))return Optional.empty();
  }
  if(!world.getBlockAt(point.x(),center+1,point.z()).isEmpty()||!world.getBlockAt(point.x(),center+2,point.z()).isEmpty())return Optional.empty();
  return Optional.of(center+1);
 }
}
