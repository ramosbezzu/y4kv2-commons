package net.y4k.progression.paper;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.y4k.progression.application.*;
import net.y4k.progression.domain.SpawnGrid;
import net.y4k.progression.domain.ProgressionText;
/** Serial first-entry allocation. No random teleport on reconnect or death. */
public final class FirstSpawn implements Listener {
 private record Request(Player player, Iterator<SpawnGrid.Point> points, long deadline, int attempts) {}
 private final JavaPlugin plugin;private final World world;private final Terrain terrain;private final SpawnLedger ledger;private final SpawnGrid grid;
 private final NamespacedKey settled,pending;
 private final BiFunction<Player,Location,CompletableFuture<Boolean>> teleport;
 private final Deque<Request> queue=new ArrayDeque<>();private final Set<UUID> waiting=new HashSet<>();
 private final int maxAttempts;private boolean busy,broken;
 public FirstSpawn(JavaPlugin plugin,World world,Terrain terrain,SpawnLedger ledger,SpawnGrid grid,int maxAttempts,
                   BiFunction<Player,Location,CompletableFuture<Boolean>> teleport){
  this.plugin=plugin;this.world=world;this.terrain=terrain;this.ledger=ledger;this.grid=grid;this.maxAttempts=maxAttempts;this.teleport=teleport;
  settled=new NamespacedKey(plugin,"survival-settled");pending=new NamespacedKey(plugin,"survival-pending");
  plugin.getServer().getScheduler().runTaskTimer(plugin,this::tick,1,1);
 }
 @EventHandler public void join(PlayerJoinEvent event){
  var p=event.getPlayer();boolean first=!p.hasPlayedBefore() || p.getPersistentDataContainer().has(pending);
  begin(p,first);
 }
 public void begin(Player p,boolean first){
  if(!p.getWorld().equals(world)||p.getPersistentDataContainer().has(settled)||!first||waiting.contains(p.getUniqueId()))return;
  p.getPersistentDataContainer().set(pending,PersistentDataType.BYTE,(byte)1);
  if(broken){fail(p);return;}
  waiting.add(p.getUniqueId());var origin=world.getSpawnLocation();
  queue.add(new Request(p,grid.candidates(origin.getBlockX(),origin.getBlockZ(),p.getUniqueId()).iterator(),System.nanoTime()+TimeUnit.SECONDS.toNanos(40),0));
  p.sendMessage(RankCommands.LEGACY.deserialize(ProgressionText.searching(p.locale())));
 }
 public boolean waiting(Player p){return waiting.contains(p.getUniqueId());}
 private void main(Runnable action){if(plugin.isEnabled())plugin.getServer().getScheduler().runTask(plugin,action);}
 public void tick(){
  if(busy||queue.isEmpty())return;
  var r=queue.removeFirst();var p=r.player();
  if(!waiting(p)||!p.isOnline())return;
  if(broken||System.nanoTime()>r.deadline()||r.attempts()>=maxAttempts){fail(p);return;}
  SpawnGrid.Point point=null;while(r.points().hasNext()){var next=r.points().next();if(!ledger.used().contains(next.key())){point=next;break;}}
  if(point==null){fail(p);return;}var candidate=point;busy=true;
  CompletableFuture<Void> loading;
  try{loading=terrain.loadExisting(candidate);}catch(RuntimeException e){loading=CompletableFuture.failedFuture(e);}
  loading.orTimeout(2,TimeUnit.SECONDS).whenComplete((ignored,error)->main(()->{
   busy=false;if(!p.isOnline()||!waiting(p))return;
   Optional<SpawnSearch.Destination> found;
   try{found=error==null?new SpawnSearch(List.of(candidate),ledger.used(),terrain::safeY,1).step():Optional.empty();}catch(RuntimeException e){found=Optional.empty();}
   if(found.isEmpty()){queue.addLast(new Request(p,r.points(),r.deadline(),r.attempts()+1));return;}
   try{ledger.reserve(p.getUniqueId(),candidate);}catch(IOException e){broken=true;plugin.getLogger().severe("Cannot persist spawn reservation; allocation stopped");fail(p);return;}
   busy=true;
   var destination=new Location(world,candidate.x()+0.5,found.get().y(),candidate.z()+0.5);
   CompletableFuture<Boolean> transferred;
   try{transferred=teleport.apply(p,destination);}catch(RuntimeException e){transferred=CompletableFuture.failedFuture(e);}
   transferred.orTimeout(5,TimeUnit.SECONDS).whenComplete((success,failure)->main(()->{
    busy=false;if(!p.isOnline()||!waiting(p))return;
    if(failure!=null||!Boolean.TRUE.equals(success)){fail(p);return;}
    p.getPersistentDataContainer().set(settled,PersistentDataType.BYTE,(byte)1);p.getPersistentDataContainer().remove(pending);waiting.remove(p.getUniqueId());
   }));
  }));
 }
 private void fail(Player p){waiting.remove(p.getUniqueId());p.kick(RankCommands.LEGACY.deserialize(ProgressionText.noSafeSpot(p.locale())));}
 @EventHandler public void quit(PlayerQuitEvent event){waiting.remove(event.getPlayer().getUniqueId());}
 @EventHandler public void move(PlayerMoveEvent event){if(waiting(event.getPlayer()))event.setTo(event.getFrom());}
 @EventHandler public void interact(PlayerInteractEvent event){if(waiting(event.getPlayer()))event.setCancelled(true);}
 @EventHandler public void damage(EntityDamageEvent event){if(event.getEntity() instanceof Player p && waiting(p))event.setCancelled(true);}
 private void dirty(org.bukkit.block.Block block){
  if(!block.getWorld().equals(world))return;
  try{ledger.touch(block.getX()>>4,block.getZ()>>4);}catch(IOException e){broken=true;plugin.getLogger().severe("Cannot persist terrain changes; allocation stopped");}
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void place(BlockPlaceEvent event){dirty(event.getBlock());}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void breakBlock(BlockBreakEvent event){dirty(event.getBlock());}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void explosion(EntityExplodeEvent event){event.blockList().forEach(this::dirty);}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void blockExplosion(BlockExplodeEvent event){event.blockList().forEach(this::dirty);}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void piston(BlockPistonExtendEvent event){dirty(event.getBlock());event.getBlocks().forEach(b->{dirty(b);dirty(b.getRelative(event.getDirection()));});}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void retract(BlockPistonRetractEvent event){dirty(event.getBlock());event.getBlocks().forEach(b->{dirty(b);dirty(b.getRelative(event.getDirection()));});}
}
