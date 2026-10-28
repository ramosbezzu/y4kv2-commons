package net.y8k.progression;
import java.lang.reflect.Proxy;
import net.luckperms.api.LuckPerms;
import net.y8k.progression.paper.ProgressionPlugin;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.*;
import static org.assertj.core.api.Assertions.*;
class ProgressionPluginTest {
 ServerMock server;
 @BeforeEach void setup(){server=MockBukkit.mock();server.addSimpleWorld("world");var provider=MockBukkit.createMockPlugin("LuckPerms");
  var lp=(LuckPerms)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{LuckPerms.class},(p,m,a)->null);
  server.getServicesManager().register(LuckPerms.class,lp,provider,ServicePriority.Normal);
 }
 @AfterEach void cleanup(){MockBukkit.unmock();}
 @Test void pluginBootsWithLuckPermsAndRegistersRanksAndSpawnConfiguration(){
  var plugin=MockBukkit.load(ProgressionPlugin.class);
  assertThat(plugin.isEnabled()).isTrue();assertThat(plugin.getCommand("ranks")).isNotNull();
  assertThat(plugin.getConfig().getInt("spawn.spacing")).isEqualTo(50);
  assertThat(plugin.getConfig().getInt("spawn.radius")).isEqualTo(250);
  assertThat(plugin.getDataFolder().toPath().resolve("spawn-history.txt")).exists();
 }
}
