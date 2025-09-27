package net.lerariemann.infinity.compat.kubejs.events;

import dev.latvian.mods.kubejs.server.ServerEventJS;
import net.lerariemann.infinity.dimensions.RandomDimension;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

public class InfinityDimAddedJS extends ServerEventJS {
    public final Identifier id;
    public final RandomDimension targetDim;

    public InfinityDimAddedJS(MinecraftServer server, Identifier id, RandomDimension dim) {
        super(server);
        this.id = id;
        this.targetDim = dim;
    }

    public Identifier getId() {
        return this.id;
    }

    public RandomDimension getTargetDim() {
        return this.targetDim;
    }
}
