package net.lerariemann.infinity.compat.kubejs.events;

import dev.latvian.mods.kubejs.level.LevelEventJS;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemInPortalJS extends LevelEventJS {

    private final World level;
    private final BlockPos pos;
    private final ItemEntity entity;

    public ItemInPortalJS(World world, BlockPos pos, ItemEntity entity) {
        super();
        this.level = world;
        this.pos = pos;
        this.entity = entity;
    }


    @Override
    public World getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ItemEntity getEntity() {
        return entity;
    }
}