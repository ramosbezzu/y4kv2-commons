package net.y4k.progression.paper;
import java.util.stream.Collectors;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.java.JavaPlugin;
import net.y4k.progression.application.SpawnLedger;
import net.y4k.progression.domain.*;
public class ProgressionPlugin extends JavaPlugin {
 @Override public void onEnable(){
  saveDefaultConfig();String mode=getConfig().getString("mode","survival");RankTrack.in(mode);
  var lp=getServer().getServicesManager().load(LuckPerms.class);
  if(lp==null)throw new IllegalStateException("LuckPerms service unavailable");
  getCommand("ranks").setExecutor(new RankCommands(mode,p->{
   var user=lp.getUserManager().getUser(p.getUniqueId());
   return user==null?java.util.Set.of():user.getInheritedGroups(lp.getContextManager().getQueryOptions(p)).stream().map(g->g.getName()).collect(Collectors.toSet());
  }));
  if(mode.equals("survival")&&getConfig().getBoolean("spawn.enabled",true)){
   var world=getServer().getWorld(getConfig().getString("spawn.world","world"));
   if(world==null)throw new IllegalStateException("Spawn world unavailable");
   int clearance=getConfig().getInt("spawn.clearance",32),attempts=getConfig().getInt("spawn.max-attempts",80);
   if(clearance<32||clearance>64||attempts<1||attempts>1000||getConfig().getInt("spawn.attempts-per-tick",1)!=1)throw new IllegalArgumentException("Unsafe spawn budget");
   var grid=new SpawnGrid(getConfig().getInt("spawn.spacing",50),getConfig().getInt("spawn.radius",250));
   try {
    var ledger=new SpawnLedger(getDataFolder().toPath().resolve("spawn-history.txt"),world.getUID());
    var terrain=new PaperTerrain(world,ledger,new ClaimGuard(getServer().getPluginManager()),clearance);
    var listener=new FirstSpawn(this,world,terrain,ledger,grid,attempts,(p,location)->p.teleportAsync(location));
    getServer().getPluginManager().registerEvents(listener,this);
   }catch(java.io.IOException e){throw new IllegalStateException("Spawn history unavailable",e);}
  }
 }
}
