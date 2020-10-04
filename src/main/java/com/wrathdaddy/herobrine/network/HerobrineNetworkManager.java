package com.wrathdaddy.herobrine.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;

public class HerobrineNetworkManager extends NetworkManager {
    private final Channel mChannel;

    public HerobrineNetworkManager(final EnumPacketDirection packetDirection) {
        super(packetDirection);
        mChannel = new EmbeddedChannel(getChannelId(), true);
        mChannel.attr(AttributeKey.valueOf("fml:dispatcher")).set(new NetworkDispatcher(this));
    }

    @Override
    public void channelActive(final ChannelHandlerContext p_channelActive_1_) {
    }

    @Override
    public Channel channel() {
        return mChannel;
    }

    @Override
    public void checkDisconnected() {

    }

    @Override
    public void disableAutoRead() {
        this.mChannel.config().setAutoRead(false);
    }

    private ChannelId getChannelId() {
        return new ChannelId() {
            @Override
            public String asShortText() {
                return "herobrine";
            }

            @Override
            public String asLongText() {
                return "herobrine-long";
            }

            @Override
            public int compareTo(final ChannelId other) {
                return String.CASE_INSENSITIVE_ORDER.compare(asLongText(), other.asLongText());
            }
        };
    }
}
