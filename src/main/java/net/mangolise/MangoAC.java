package net.mangolise;

import net.mangolise.checks.combat.CpsCheck;
import net.mangolise.checks.combat.HitConsistencyCheck;
import net.mangolise.checks.combat.ReachCheck;
import net.mangolise.checks.exploits.VClipCrashCheck;
import net.mangolise.checks.movement.BasicSpeedCheck;
import net.mangolise.checks.movement.FlightCheck;
import net.mangolise.checks.movement.UnaidedLevitationCheck;
import net.mangolise.checks.movement.VClipCheck;

import java.util.List;

public class MangoAC {
    private final Config config;
    private final List<ACCheck> checks = List.of(
        new VClipCrashCheck(),
        new FlightCheck(),
        new UnaidedLevitationCheck(),
        new VClipCheck(),
        new BasicSpeedCheck(),
        new ReachCheck(),
        new CpsCheck(),
        new HitConsistencyCheck()
    );

    public MangoAC(Config config) {
        this.config = config;
    }

    public void start() {
        checks.forEach(acCheck -> {
            if (config.disabledChecks.contains(acCheck.getClass())) return;
            acCheck.register(config);
        });
    }

    /**
     * @param passive Whether the AC should disable lag backs and just observe players.
     */
    public record Config(boolean passive, List<Class<? extends ACCheck>> disabledChecks) { }
}
