package net.y4k.progression.paper;
import java.util.*;
import java.util.function.Function;
import net.y4k.progression.domain.RankTrack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
public final class RankCommands implements CommandExecutor {
 private final String mode;private final Function<Player,Set<String>> groups;
 public RankCommands(String mode,Function<Player,Set<String>> groups){RankTrack.in(mode);this.mode=mode;this.groups=groups;}
 public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
  if(!(sender instanceof Player p)){sender.sendMessage("Use this command in game.");return true;}
  var current=RankTrack.highest(mode,groups.apply(p));
  p.sendMessage(Component.text("Rank: "+current.name()+" • "+mode));
  for(var rank:RankTrack.in(mode))p.sendMessage(Component.text((rank.equals(current)?"✓ ":"• ")+rank.name()));
  p.sendMessage(Component.text("Progressão separada do VIP / Progression is separate from VIP. Rankup automático e benefícios de homes/lojas aguardam economia / Automatic rankup and home/shop limits await economy."));
  return true;
 }
}
