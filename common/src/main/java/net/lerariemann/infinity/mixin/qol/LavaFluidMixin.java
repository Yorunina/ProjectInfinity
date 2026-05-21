package net.lerariemann.infinity.mixin.qol;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.lerariemann.infinity.util.InfinityMethods;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LavaFluid.class)
public class LavaFluidMixin {
    /* Disabling fire tick in infdims */
    @ModifyExpressionValue(method = "onRandomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"))
    boolean inj(boolean original, @Local(argsOnly = true) World world) {
        if (InfinityMethods.isInfinity(world)) return false;
        return original;
    }
}
