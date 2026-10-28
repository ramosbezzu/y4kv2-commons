package net.y8k.progression.paper;
import org.bukkit.Location;
import org.bukkit.plugin.PluginManager;
/** Queries Towny without generating blocks. Unknown installed protectors fail closed. */
public final class ClaimGuard {
 private final PluginManager plugins;
 private final java.util.function.Predicate<Location> query;
 public ClaimGuard(PluginManager plugins){this(plugins,null);}
 public ClaimGuard(PluginManager plugins,java.util.function.Predicate<Location> query){this.plugins=plugins;this.query=query;}
 public boolean free(Location location,int radius){
  for(String name:new String[]{"GriefPrevention","WorldGuard","BentoBox"})if(plugins.getPlugin(name)!=null)return false;
  var towny=plugins.getPlugin("Towny");if(towny==null)return true;
  if(!towny.isEnabled())return false;
  try {
   if(query!=null){
    for(int x=location.getBlockX()-radius;x<=location.getBlockX()+radius;x+=8)for(int z=location.getBlockZ()-radius;z<=location.getBlockZ()+radius;z+=8)
     if(query.test(new Location(location.getWorld(),x,location.getY(),z)))return false;
    return true;
   }
   var type=Class.forName("com.palmergames.bukkit.towny.TownyAPI",true,towny.getClass().getClassLoader());
   var api=type.getMethod("getInstance").invoke(null);var query=type.getMethod("getTownBlock",Location.class);
   for(int x=location.getBlockX()-radius;x<=location.getBlockX()+radius;x+=8)
    for(int z=location.getBlockZ()-radius;z<=location.getBlockZ()+radius;z+=8)
     if(query.invoke(api,new Location(location.getWorld(),x,location.getY(),z))!=null)return false;
   return true;
  }catch(ReflectiveOperationException|LinkageError|RuntimeException e){return false;}
 }
}
