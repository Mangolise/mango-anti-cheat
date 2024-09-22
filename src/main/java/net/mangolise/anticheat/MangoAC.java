package net.mangolise.anticheat;

import net.mangolise.anticheat.checks.combat.CpsCheck;
import net.mangolise.anticheat.checks.combat.HitConsistencyCheck;
import net.mangolise.anticheat.checks.combat.ReachCheck;
import net.mangolise.anticheat.checks.exploits.IntOverflowCrashCheck;
import net.mangolise.anticheat.checks.movement.BasicSpeedCheck;
import net.mangolise.anticheat.checks.movement.FlightCheck;
import net.mangolise.anticheat.checks.movement.TeleportCheck;
import net.mangolise.anticheat.checks.movement.TeleportSpamCheck;
import net.mangolise.anticheat.checks.movement.UnaidedLevitationCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

import java.util.*;

public class MangoAC {
    private final Config config;
    private final Map<UUID, List<Tuple<Point, Block>>> fakeBlocks = new HashMap<>();
    private final List<ACCheck> checks = List.of(
        new IntOverflowCrashCheck(),
        new FlightCheck(),
        new UnaidedLevitationCheck(),
        new TeleportCheck(),
        new BasicSpeedCheck(),
        new ReachCheck(),
        new CpsCheck(),
        new HitConsistencyCheck(),
        new TeleportSpamCheck()
    );

    public MangoAC(Config config) {
        this.config = config;
    }

    public Block getBlockAt(Player player, Point pos) {
        if (fakeBlocks.containsKey(player.getUuid())) for (Tuple<Point, Block> fakeBlock : fakeBlocks.get(player.getUuid())) {
            if (pos.sameBlock(fakeBlock.getFirst())) {
                return fakeBlock.getSecond();
            }
        }
        return player.getInstance().getBlock(pos);
    }

    public void start() {
        checks.forEach(acCheck -> {
            if (config.disabledChecks.contains(acCheck.getClass())) return;
            acCheck.enable(this, config);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e -> {
            fakeBlocks.remove(e.getPlayer().getUuid());
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketOutEvent.class, e -> {
            switch (e.getPacket()) {
                case BlockChangePacket packet -> {
                    if (!fakeBlocks.containsKey(e.getPlayer().getUuid())) {
                        fakeBlocks.put(e.getPlayer().getUuid(), new ArrayList<>());
                    }

                    fakeBlocks.get(e.getPlayer().getUuid()).add(new Tuple<>(packet.blockPosition(), Block.STONE));
                }
                default -> { }
            }
        });
    }

    public void tempDisableCheck(Player player, Class<? extends ACCheck> check, int time) {
        checks.stream().filter(acCheck -> acCheck.getClass().equals(check)).findFirst().ifPresent(acCheck -> {
            acCheck.disableFor(player, time);
        });
    }

    /**
     * @param passive Whether the AC should disable lag backs and just observe players.
     */
    public record Config(boolean passive, List<Class<? extends ACCheck>> disabledChecks, List<String> debugChecks) { }
}
