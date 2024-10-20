package net.mangolise.anticheat.checks.movement;

import net.mangolise.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * This will false flag when players first load in and fall through the ground.
 * You should make this an "innocent check" in the config to fix this bug and also
 * stop false flags. This behaviour is implemented by default.
 */
public class PhaseCheck extends ACCheck {
    private static final double THRESHOLD = 0.2;

    public PhaseCheck() {
        super("Phase");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
    }

    private void onMove(@NotNull PlayerMoveEvent event) {
        final Pos to = event.getNewPosition();
        if (to.y() > event.getPlayer().getPosition().y()) {
            return;
        }
        Block blockAt = event.getPlayer().getInstance().getBlock(to.add(0, THRESHOLD, 0));
        if (isFullBlock(blockAt)) {
            debug(event.getPlayer(), blockAt.name() + " is solid and you went into it");
            flag(event.getPlayer(), 0.7f);
            if (isNotPassive()) {
                // Try and get them out, push in the direction they came from until they hit air
//                Vec dir = to.sub(event.getPlayer().getPosition()).asVec().normalize();
//                Pos attempt = to;
//                while (!attempt.sameBlock(to)) {
//                    attempt = attempt.sub(dir.mul(0.5));
//                }
//                event.getPlayer().teleport(attempt);
                event.setCancelled(true);
            }
        }
    }
}
