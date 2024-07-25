package io.github.sakurawald.module.initializer.world.structure;

import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.UnmodifiableLevelProperties;

/**
 * The only purpose of this class is to warp the seed.
 **/
@SuppressWarnings("LombokGetterMayBeUsed")
public final class MyWorldProperties extends UnmodifiableLevelProperties {

    private final long seed;

    public MyWorldProperties(SaveProperties saveProperties, long seed) {
        super(saveProperties, saveProperties.getMainWorldProperties());
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }
}