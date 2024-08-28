package net.mangolise.checks.movement;

import net.mangolise.ACCheck;
import net.mangolise.MangoAC;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;

public class VClipCheck extends ACCheck {
    private final static int THRESHOLD = 3;

    private MangoAC.Config config;

    public VClipCheck() {
        super("VClip");
    }

    @Override
    public void register(MangoAC.Config config) {
        this.config = config;
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!canCheck(p)) return;

        double diff = Math.abs(e.getNewPosition().y() - e.getPlayer().getPosition().y());
        if (diff > THRESHOLD) {
            flag(p, 0.9f);
            if (!config.passive()) {
                e.setCancelled(true);
            }
        }
    }
}
