package net.mangolise.anticheat;

import net.mangolise.anticheat.checks.combat.CpsCheck;
import net.mangolise.anticheat.checks.combat.HitConsistencyCheck;
import net.mangolise.anticheat.checks.combat.KillauraManualCheck;
import net.mangolise.anticheat.checks.combat.ReachCheck;
import net.mangolise.anticheat.checks.exploits.IntOverflowCrashCheck;
import net.mangolise.anticheat.checks.movement.*;
import net.mangolise.anticheat.checks.other.FastBreakCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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
        new TeleportSpamCheck(),
        new FastBreakCheck(),
        new KillauraManualCheck(),
        new PhaseCheck()
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

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e ->
                fakeBlocks.remove(e.getPlayer().getUuid()));

        // This is WIP, just disable flight and levi if you have fake blocks
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
        checks.stream().filter(acCheck -> acCheck.getClass().equals(check)).findFirst().ifPresent(acCheck ->
                acCheck.disableFor(player, time));
    }

    public CompletableFuture<Float> performManualCheck(Class<? extends ManualCheck> check, Player target) {
        ACCheck c = checks.stream().filter(acCheck ->
                acCheck.getClass().equals(check)).findFirst().get();
        return ((ManualCheck) c).check(target);
    }

    /**
     * @param passive
     */
    public record Config(boolean passive, List<Class<? extends ACCheck>> disabledChecks, List<String> debugChecks, List<Class<? extends ACCheck>> innocentChecks) {
        public Config() {
            this(false, List.of(), List.of(), List.of(PhaseCheck.class));
        }

        /**
         * Create a new config with default values.
         * @return The new config.
         */
        public static Config create() {
            return new Config();
        }

        /**
         * Whether the AC should disable lag backs and just observe players.
         * @param passive The value.
         * @return The new config.
         */
        public Config withPassive(boolean passive) {
            return new Config(passive, disabledChecks, debugChecks, innocentChecks);
        }

        /**
         * A list of checks which won't create flags but will create lag backs regardless of `passive`.
         * @param innocentChecks The value.
         * @return The new config.
         */
        public Config withInnocentChecks(List<Class<? extends ACCheck>> innocentChecks) {
            return new Config(passive, disabledChecks, debugChecks, innocentChecks);
        }

        /**
         * A list of checks which will not run or flag.
         * @param disabledChecks The value.
         * @return The new config.
         */
        public Config withDisabledChecks(List<Class<? extends ACCheck>> disabledChecks) {
            return new Config(passive, disabledChecks, debugChecks, innocentChecks);
        }

        /**
         * A list of checks which will print debug info to players, this should not be used in production.
         * @param debugChecks The value.
         * @return The new config.
         */
        public Config withDebugChecks(List<String> debugChecks) {
            return new Config(passive, disabledChecks, debugChecks, innocentChecks);
        }
    }
}
