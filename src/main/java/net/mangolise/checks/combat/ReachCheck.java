package net.mangolise.checks.combat;

import net.mangolise.ACCheck;
import net.mangolise.MangoAC;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;

public class ReachCheck extends ACCheck {
    private final static double THRESHOLD = 5;

    public ReachCheck() {
        super("Reach");
    }

    @Override
    public void register(MangoAC.Config config) {
        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, e -> {
            if (!(e.getEntity() instanceof Player player)) {
                return;
            }

            double distance = player.getPosition().distance(e.getTarget().getPosition());
            debug(player, "Distance: " + distance);

            if (distance > THRESHOLD) {
                flag(player, 0.7f);
            }
        });
    }
}
