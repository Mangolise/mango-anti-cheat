package net.mangolise.anticheat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.anticheat.checks.movement.UnaidedLevitationCheck;
import net.mangolise.anticheat.events.PlayerFlagEvent;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.Instance;

import java.util.List;

/**
 * Test server with a world that we can use to test the AC.
 */
public class Test {
    // Checks that debug messages should be sent for
    private static final List<String> DEBUG_CHECKS = List.of();

    public static void main(String[] args) {
        System.out.println("Starting test server...");

        // It's hard to test when minestom kicks cheaters
        System.setProperty("minestom.packet-queue-size", "10000");
        System.setProperty("minestom.packet-per-tick", "10000");

        MinecraftServer server = MinecraftServer.init();

        IChunkLoader chunkLoader = GameSdkUtils.getPolarLoaderFromResource("test-world.polar");
        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer(chunkLoader);
        instance.enableAutoChunkLoad(true);

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> e.setSpawningInstance(instance));
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e -> e.getPlayer().teleport(GameSdkUtils.getSpawnPosition(instance)));

        MangoAC ac = new MangoAC(new MangoAC.Config(false, List.of(), DEBUG_CHECKS));

        MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent.class, e -> {
            if (e.getMessage().equals("t")) {
                e.getPlayer().teleport(e.getPlayer().getPosition().add(10, 10, 0));
            }

            if (e.getMessage().equals("ping")) {
                e.getPlayer().sendMessage("Latency: " + e.getPlayer().getLatency());
            }

            if (e.getMessage().equals("dislev")) {
                ac.tempDisableCheck(e.getPlayer(), UnaidedLevitationCheck.class, 100);
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, e -> {
            if (!(e.getTarget() instanceof LivingEntity target)) {
                return;
            }

            target.damage(Damage.fromEntity(e.getEntity(), 0f));
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerFlagEvent.class, e ->
                e.player().sendMessage(Component
                    .text("You have been flagged for " + e.checkName() + " with a certainty of " + e.certainty())
                    .color(NamedTextColor.RED)));

        ac.start();

        server.start("0.0.0.0", GameSdkUtils.getConfiguredPort());
    }
}
