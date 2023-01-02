package me.lemontea.compresschat.mixins;

import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatScreen.class)
public final class ChatScreenMixin {

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setMaxLength(I)V"))
    public int modifyChatWidth(int oldWidth) {
        return Integer.MAX_VALUE;
    }

    @Redirect(method = "normalize", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringHelper;truncateChat(Ljava/lang/String;)Ljava/lang/String;"))
    public String truncateChatProxy(String msg) {
        return msg;
    }

}
