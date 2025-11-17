package net.lerariemann.infinity.mixin.mavity;

import dev.architectury.platform.Platform;
import net.lerariemann.infinity.access.MavityInterface;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThrownEntity.class)
public abstract class ThrownEntityMixin extends ProjectileEntity implements MavityInterface {
    @Shadow protected abstract float getGravity();

    public ThrownEntityMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/thrown/ThrownEntity;getGravity()F"))
    protected float getGravity(ThrownEntity instance) {
        // 只在Fabric平台且gravity_changer_q未加载时应用
        if (Platform.isFabric() && !Platform.isModLoaded("gravity_changer_q")) {
            return (float)getMavity() * getGravity();
        }
        return getGravity();
    }
}
