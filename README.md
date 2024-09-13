# Mangolise Anti Cheat
This is an open source anti cheat that can be used by any Minestom server as a library.

## Checks
- [Flight](#flight)
- [BasicSpeed](#basicspeed)
- [Teleport](#teleport)
- [TeleportSpam](#teleportspam)
- [UnaidedLevitation](#unaidedlevitation)
- [IntOverflowCrash](#intoverflowcrash)
- [CpsCheck](#cpscheck)
- [HitConsistency](#hitconsistency)
- [Reach](#reach)

## Usage

### Dependency

build.gradle.kts
```kotlin
repositories {
    mavenCentral()
    maven("https://maven.serble.net/snapshots/")
}

dependencies {
    implementation("net.mangolise:mango-anti-cheat:latest")
}
```

build.gradle
```groovy
repositories {
    maven { url 'https://maven.serble.net/snapshots/' }
}

dependencies {
    implementation 'net.mangolise:mango-anti-cheat:latest'
}
```

pom.xml
```xml
<repositories>
    <repository>
        <id>Serble</id>
        <url>https://maven.serble.net/snapshots/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>net.mangolise</groupId>
        <artifactId>mango-anti-cheat</artifactId>
        <version>latest</version>
    </dependency>
</dependencies>
```

### Example
```java
package net.mangolise.anticheat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.anticheat.events.PlayerFlagEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;

import java.util.List;

/**
 * Minimum example of how to use MangoAC.
 */
public class Test {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        // Spawn players into an empty instance
        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.enableAutoChunkLoad(true);

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> e.setSpawningInstance(instance));

        // This is triggered when the player is flagged for anything
        MinecraftServer.getGlobalEventHandler().addListener(PlayerFlagEvent.class, e ->
                e.player().sendMessage(Component
                        .text("You have been flagged for " + e.checkName() + " with a certainty of " + e.certainty())
                        .color(NamedTextColor.RED)));

        // Enable the anti cheat
        // Config.   (passive, disabled checks, debug checks)
        MangoAC.Config config = new MangoAC.Config(false, List.of(), List.of());
        MangoAC ac = new MangoAC(config);
        ac.start();  // Start the anti cheat

        server.start("0.0.0.0", 25565);
    }
}
```
You can also use the `Test` class as a reference for how to use the library.

## Check Descriptions

### Flight
Detects flight by checking if player moves more than a certain amount of blocks without 
going up or down.

### BasicSpeed
Detects speed by checking if the player is moving too quickly, using distance over time.

### Teleport
Detects teleportation by checking if the player moves more than a certain amount of blocks
in one packet.

### TeleportSpam
Detects TP-Aura by checking if the player moves more than a certain amount of blocks at a 
time without a certain time frame a certain number of times.

### UnaidedLevitation
Detects flight by checking if the player moves too many blocks up vertically without
touching the ground or being otherwise aided by things such as a ladder, vines, or a liquid.

### IntOverflowCrash
Stops a crash exploit in Minestom by checking if the player moves past the integer limit
on any axis. If this happens they will be stopped and flagged with 100% certainty.

### CpsCheck
Detects auto clickers and killaura by checking if the player's CPS is above a certain
value.

### HitConsistency
Detects auto clickers and killaura by checking how consistently timed the player's hits are.

### Reach
Detects reach by checking how far away players that the player hits are.
