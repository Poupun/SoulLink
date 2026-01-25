package com.jellycreative.soullink.mixin;

import com.jellycreative.soullink.inventory.SharedInventoryEventHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to detect when players open containers.
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "onScreenHandlerOpened", at = @At("HEAD"))
    private void onContainerOpen(ScreenHandler handler, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        SharedInventoryEventHandler.markContainerOpen(player);
    }

    @Inject(method = "closeHandledScreen", at = @At("RETURN"))
    private void onContainerClose(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        SharedInventoryEventHandler.markContainerClosed(player);
    }
}
