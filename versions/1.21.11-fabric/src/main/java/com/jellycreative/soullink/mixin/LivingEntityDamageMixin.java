package com.jellycreative.soullink.mixin;

import com.jellycreative.soullink.event.SoulLinkEventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept damage events for Soul-Link synchronization.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamageAfter(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            LivingEntity entity = (LivingEntity) (Object) this;
            SoulLinkEventHandler.onDamageAfter(entity, source, amount);
        }
    }
}
