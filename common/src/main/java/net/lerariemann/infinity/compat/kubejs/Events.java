package net.lerariemann.infinity.compat.kubejs;

import net.lerariemann.infinity.compat.kubejs.events.InfinityDimAddedJS;
import net.lerariemann.infinity.compat.kubejs.events.ItemInPortalJS;
import net.lerariemann.infinity.dimensions.RandomDimension;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import static net.lerariemann.infinity.InfinityMod.KUBEJS_LOADED;
import static net.lerariemann.infinity.compat.kubejs.Plugin.DIM_ADDED_EVENT;
import static net.lerariemann.infinity.compat.kubejs.Plugin.ITEM_IN_PORTAL_EVENT;

public class Events {

    public static void postItemInPortal(World world, BlockPos pos, ItemEntity entity) {
        if (KUBEJS_LOADED) {
            ITEM_IN_PORTAL_EVENT.post(new ItemInPortalJS(world, pos, entity));
        }
    }

    public static RandomDimension postInfinityDimAdded(MinecraftServer server, Identifier id) {
        RandomDimension dim = new RandomDimension(id, server);
        if (KUBEJS_LOADED) {
            InfinityDimAddedJS event = new InfinityDimAddedJS(server, id, dim);
            DIM_ADDED_EVENT.post(event);
            return event.getTargetDim().wrapUp();
        }
        return dim.wrapUp();
    }
}
