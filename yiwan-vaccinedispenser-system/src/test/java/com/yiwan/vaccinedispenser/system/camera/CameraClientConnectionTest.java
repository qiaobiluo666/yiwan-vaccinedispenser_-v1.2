package com.yiwan.vaccinedispenser.system.camera;

import io.netty.channel.socket.SocketChannel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CameraClientConnectionTest {

    @Test
    void sendCommandUsesOriginalConnectedChannelDirectly() {
        CameraClient client = new CameraClient("127.0.0.1", 2001, "上方扫码相机", message -> { });
        SocketChannel channel = mock(SocketChannel.class);
        when(channel.isActive()).thenReturn(true);
        ReflectionTestUtils.setField(client, "socketChannel", channel);

        boolean sent = client.sendCommand();

        assertThat(sent).isTrue();
        verify(channel).writeAndFlush("start");
    }

    @Test
    void disconnectedSendUsesOriginalImmediateReconnectTrigger() {
        CameraClient client = spy(new CameraClient(
                "127.0.0.1", 2001, "上方扫码相机", message -> { }));
        SocketChannel channel = mock(SocketChannel.class);
        when(channel.isActive()).thenReturn(false);
        ReflectionTestUtils.setField(client, "socketChannel", channel);
        doNothing().when(client).connect();

        boolean sent = client.sendCommand();

        assertThat(sent).isFalse();
        assertThat(client.isConnecting()).isTrue();
        verify(client).connect();
    }

    @Test
    void nullSocketDoesNotThrowOrStartDuplicateConnect() {
        CameraClient client = spy(new CameraClient(
                "127.0.0.1", 2001, "上方扫码相机", message -> { }));
        doNothing().when(client).connect();

        boolean sent = client.sendCommand();

        assertThat(sent).isFalse();
        assertThat(client.isConnecting()).isFalse();
        verify(client, org.mockito.Mockito.never()).connect();
    }
}
