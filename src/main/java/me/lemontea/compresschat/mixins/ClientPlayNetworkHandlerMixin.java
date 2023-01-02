package me.lemontea.compresschat.mixins;

import me.lemontea.compresschat.CompressChatMod;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayNetworkHandler.class)
public final class ClientPlayNetworkHandlerMixin {

    @ModifyVariable(method = { "sendChatMessage", "sendChatCommand" }, at = @At("HEAD"), argsOnly = true)
    public String encodeMessage(String msg) {
        return CompressChatMod.CODEC.encodeMessage(msg);
    }

}
