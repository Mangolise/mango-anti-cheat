package net.mangolise;

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

    public static void main(String[] args) {
        System.out.println("Starting test server...");

        MinecraftServer server = MinecraftServer.init();

        IChunkLoader chunkLoader = GameSdkUtils.getPolarLoaderFromResource("test-world.polar");
        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer(chunkLoader);
        instance.enableAutoChunkLoad(true);

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> {
            e.setSpawningInstance(instance);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e -> {
            e.getPlayer().teleport(GameSdkUtils.getSpawnPosition(instance));
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent.class, e -> {
            if (e.getMessage().equals("t")) {
                e.getPlayer().teleport(e.getPlayer().getPosition().add(10, 0, 0));
            }

            if (e.getMessage().equals("ping")) {
                e.getPlayer().sendMessage("Latency: " + e.getPlayer().getLatency());
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, e -> {
            if (!(e.getTarget() instanceof LivingEntity target)) {
                return;
            }

            target.damage(Damage.fromEntity(e.getEntity(), 0f));
        });

        new MangoAC(new MangoAC.Config(false, List.of())).start();

        server.start("0.0.0.0", GameSdkUtils.getConfiguredPort());
    }
}
