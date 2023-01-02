package me.lemontea.compresschat.mixins;

import me.lemontea.compresschat.CompressChatMod;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MessageHandler.class)
public final class MessageHandlerMixin {

    @ModifyVariable(method = { "onGameMessage", "onProfilelessMessage" }, at = @At("HEAD"), argsOnly = true)
    public Text decodeUnsignedMessage(Text msg) {
        return Text.literal(CompressChatMod.CODEC.decodeMessage(msg.getString()));
    }

    @ModifyVariable(method = "onChatMessage", at = @At("HEAD"), argsOnly = true)
    public SignedMessage decodeSignedMessage(SignedMessage msg) {
        String origMsg = msg.getContent().getString();
        String decoded = CompressChatMod.CODEC.decodeMessage(origMsg);

        return origMsg.equals(decoded) ? msg : SignedMessage.ofUnsigned(msg.getSender(), decoded);
    }

}
