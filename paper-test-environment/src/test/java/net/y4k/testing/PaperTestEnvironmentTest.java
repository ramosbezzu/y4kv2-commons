package net.y4k.testing;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the test environment before Y8K develops its own Paper adapters. */
class PaperTestEnvironmentTest {
    private ServerMock server;

    @BeforeEach
    void createServer() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void releaseServer() {
        MockBukkit.unmock();
    }

    @Test
    void playerInventoryCanBeExercisedWithoutLaunchingPaper() {
        var player = server.addPlayer("Y8KTestPlayer");
        player.getInventory().setItem(0, new ItemStack(Material.STONE, 8));

        assertThat(Bukkit.getServer()).isSameAs(server);
        assertThat(server.getOnlinePlayers()).hasSize(1);
        assertThat(server.getPlayer("Y8KTestPlayer")).isSameAs(player);
        assertThat(player.getInventory().getItem(0).getType()).isEqualTo(Material.STONE);
        assertThat(player.getInventory().getItem(0).getAmount()).isEqualTo(8);
    }

    @Test
    void eachTestStartsWithoutPlayersFromPreviousTests() {
        assertThat(server.getOnlinePlayers()).isEmpty();
    }
}
