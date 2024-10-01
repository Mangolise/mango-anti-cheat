package net.mangolise.anticheat;

import net.minestom.server.entity.Player;

import java.util.concurrent.CompletableFuture;

public abstract class ManualCheck extends ACCheck {

    public ManualCheck(String name) {
        super(name);
    }

    public abstract CompletableFuture<Float> check(Player player);
}
