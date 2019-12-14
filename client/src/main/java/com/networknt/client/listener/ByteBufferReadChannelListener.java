package com.networknt.client.listener;

import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.websockets.core.UTF8Output;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a new Channel Listener to handler Byte Array result from response.
 *
 * @author Gavin Chen
 */

public abstract class ByteBufferReadChannelListener implements ChannelListener<StreamSourceChannel> {
    private final ByteBufferPool bufferPool;
    private List<Byte> result = new ArrayList<>();
    public ByteBufferReadChannelListener(ByteBufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    public void setup(StreamSourceChannel channel) {
        PooledByteBuffer resource = this.bufferPool.allocate();
        ByteBuffer buffer = resource.getBuffer();

        try {
            int r;
            do {
                r = channel.read(buffer);
                if (r == 0) {
                    channel.getReadSetter().set(this);
                    channel.resumeReads();
                } else if (r == -1) {
                    this.bufferDone(this.result);
                    IoUtils.safeClose(channel);
                } else {
                    buffer.flip();
                    ByteBuffer[] buffs = new ByteBuffer[]{buffer};
                    for(int i = 0; i < buffs.length; ++i) {
                        ByteBuffer buf = buffs[i];
                        while(buf.hasRemaining()) {
                            result.add(buf.get());
                        }
                    }
                }
            } while(r > 0);
        } catch (IOException var8) {
            this.error(var8);
        } finally {
            resource.close();
        }

    }

    public void handleEvent(StreamSourceChannel channel) {
        PooledByteBuffer resource = this.bufferPool.allocate();
        ByteBuffer buffer = resource.getBuffer();

        try {
            int r;
            do {
                r = channel.read(buffer);
                if (r == 0) {
                    return;
                }
                if (r == -1) {
                    this.bufferDone(this.result);
                    IoUtils.safeClose(channel);
                } else {
                    buffer.flip();
                    ByteBuffer[] buffs = new ByteBuffer[]{buffer};;
                    for(int i = 0; i < buffs.length; ++i) {
                        ByteBuffer buf = buffs[i];
                        while(buf.hasRemaining()) {
                            result.add(buf.get());
                        }
                    }
                }
            } while(r > 0);
        } catch (IOException var8) {
            this.error(var8);
        } finally {
            resource.close();
        }

    }

    protected abstract void bufferDone(List<Byte> out);

    protected abstract void error(IOException var1);
}
