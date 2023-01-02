package me.lemontea.compresschat;

import me.lemontea.compresschat.codec.MessageCodec;
import net.fabricmc.api.ClientModInitializer;

public final class CompressChatMod implements ClientModInitializer {

    public static final MessageCodec CODEC = MessageCodec.createCodec();

    @Override
    public void onInitializeClient() {}

}
