package com.resurrect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ResurrectNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ResurrectMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    /** Client -> server: "I pressed Resurrect". Carries no data. */
    public static class RequestPacket {
    }

    public static void register() {
        CHANNEL.messageBuilder(RequestPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((msg, buf) -> { })
                .decoder(buf -> new RequestPacket())
                .consumerMainThread((msg, ctx) -> {
                    ServerPlayer sender = ctx.get().getSender();
                    if (sender != null) {
                        ResurrectManager.handleRequest(sender);
                    }
                })
                .add();
    }
}
