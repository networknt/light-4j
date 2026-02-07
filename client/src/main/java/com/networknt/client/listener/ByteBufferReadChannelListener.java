package com.networknt.client.listener;

import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;

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
    /**
     * Constructs a ByteBufferReadChannelListener with the given buffer pool.
     * @param bufferPool the pool to allocate buffers from
     */
    public ByteBufferReadChannelListener(ByteBufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    /**
     * Sets up the channel for reading.
     * @param channel the channel to read from
     */
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

    /**
     * Called when the buffer reading is done.
     * @param out the list of bytes read
     */
    protected abstract void bufferDone(List<Byte> out);

    /**
     * Called when an error occurs during reading.
     * @param var1 the exception that occurred
     */
    protected abstract void error(IOException var1);
}
