package net.mangolise.anticheat.checks.other;

import net.mangolise.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientPlayerDiggingPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class FastBreakCheck extends ACCheck {
    private static final Tag<Long> LAST_START_DIG_TAG = Tag.Long("anticheat_fastbreak_last_start_dig").defaultValue(0L);
    private static final int MIN_TIME = 1000 / 20;  // 1 tick, so they'd have to break 2 blocks without instamine in a tick

    public FastBreakCheck() {
        super("FastBreak");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent.class, this::playerPacket);
    }

    private void playerPacket(@NotNull PlayerPacketEvent event) {
        if (event.getPacket() instanceof ClientPlayerDiggingPacket dig) {
            switch (dig.status()) {
                case STARTED_DIGGING -> event.getPlayer().setTag(LAST_START_DIG_TAG, System.currentTimeMillis());
                case FINISHED_DIGGING -> {
                    long timeTaken = System.currentTimeMillis() - event.getPlayer().getTag(LAST_START_DIG_TAG);
                    if (timeTaken <= MIN_TIME) {
                        flag(event.getPlayer(), 1f);

                        if (!config.passive()) {  // This doesn't achieve much, it may make the break slightly longer than instant
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
}
