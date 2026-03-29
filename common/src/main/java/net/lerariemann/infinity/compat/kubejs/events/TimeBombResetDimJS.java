package net.lerariemann.infinity.compat.kubejs.events;

import dev.latvian.mods.kubejs.level.LevelEventJS;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;

public class TimeBombResetDimJS extends LevelEventJS {

    private final World level;
    private final BlockState state;

    public TimeBombResetDimJS(World world, BlockState state) {
        super();
        this.level = world;
        this.state = state;
    }


    @Override
    public World getLevel() {
        return level;
    }

    public BlockState getState() {
        return state;
    }

}