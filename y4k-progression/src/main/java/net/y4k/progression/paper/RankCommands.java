package net.y4k.progression.paper;
import java.util.*;
import java.util.function.Function;
import net.y4k.progression.domain.RankTrack;
import net.y4k.progression.domain.ProgressionText;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
public final class RankCommands implements CommandExecutor {
 static final LegacyComponentSerializer LEGACY=LegacyComponentSerializer.legacyAmpersand();
 private final String mode;private final Function<Player,Set<String>> groups;
 public RankCommands(String mode,Function<Player,Set<String>> groups){RankTrack.in(mode);this.mode=mode;this.groups=groups;}
 public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
  if(!(sender instanceof Player p)){sender.sendMessage(LEGACY.deserialize(ProgressionText.PLAYER_ONLY));return true;}
  var current=RankTrack.highest(mode,groups.apply(p));
  for(String line:ProgressionText.ranks(p.locale(),mode,current,RankTrack.in(mode)))p.sendMessage(LEGACY.deserialize(line));
  return true;
 }
}
