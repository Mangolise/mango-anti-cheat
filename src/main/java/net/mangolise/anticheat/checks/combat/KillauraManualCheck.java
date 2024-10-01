package net.mangolise.anticheat.checks.combat;

import net.mangolise.anticheat.ManualCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class KillauraManualCheck extends ManualCheck {
    private static final int HITS_THRESHOLD = 7;
    private static final Tag<Integer> DUMMY_ID = Tag.Integer("anticheat_killaura_dummyid");
    private static final Tag<Integer> DUMMY_HITS = Tag.Integer("anticheat_killaura_dummyhits").defaultValue(0);

    public KillauraManualCheck() {
        super("Killaura");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent.class, this::processPacket);
    }

    private void processPacket(@NotNull PlayerPacketEvent event) {
        if (!(event.getPacket() instanceof ClientInteractEntityPacket interact)) {
            return;
        }

        if (!(interact.type() instanceof ClientInteractEntityPacket.Attack)) {
            return;
        }

        if (!event.getPlayer().hasTag(DUMMY_ID)) {
            return;
        }

        if (event.getPlayer().getTag(DUMMY_ID) != interact.targetId()) {
            return;
        }

        event.getPlayer().updateTag(DUMMY_HITS, v -> v + 1);
    }

    @Override
    public CompletableFuture<Float> check(Player player) {
        final int id = ThreadLocalRandom.current().nextInt(10000, Integer.MAX_VALUE);
        final UUID uuid = UUID.randomUUID();

        double x = ThreadLocalRandom.current().nextDouble(0, 2);
        double z = 2 - x;

        SpawnEntityPacket packet = new SpawnEntityPacket(
                id,
                uuid,
                EntityType.PLAYER.id(),
                player.getPosition().add(x, 0.2, z),
                0.2f,
                0,
                (short) 0, (short) 0, (short) 0);
        player.setTag(DUMMY_ID, id);

        PlayerInfoUpdatePacket addPlayerInfo = new PlayerInfoUpdatePacket(
                PlayerInfoUpdatePacket.Action.ADD_PLAYER,
                new PlayerInfoUpdatePacket.Entry(
                        uuid,
                        "Technoblade",
                        List.of(),
                        true,
                        player.getLatency(),
                        GameMode.SURVIVAL,
                        null,
                        null));

        player.sendPacket(addPlayerInfo);
        player.sendPacket(packet);

        final CompletableFuture<Float> future = new CompletableFuture<>();

        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            player.removeTag(DUMMY_ID);
            Integer hits = player.getAndSetTag(DUMMY_HITS, 0);
            assert hits != null;

            float certainty = 0f;
            if (hits >= HITS_THRESHOLD) {
                certainty = Math.min(0.5f + ((float) (hits - HITS_THRESHOLD) / 10), 1f);
                flag(player, certainty);
            }

            DestroyEntitiesPacket removePacket = new DestroyEntitiesPacket(id);
            player.sendPacket(removePacket);
            future.complete(certainty);
        }, TaskSchedule.seconds(1), TaskSchedule.stop());

        return future;
    }
}
