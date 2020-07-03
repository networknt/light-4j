package com.networknt.client.listener;

import io.undertow.UndertowLogger;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This is a new Channel Listener to handler Byte Array write from request.
 *
 * @author Gavin Chen
 */

public class ByteBufferWriteChannelListener implements ChannelListener<StreamSinkChannel>{

    private final ByteBuffer buffer;

    public ByteBufferWriteChannelListener(ByteBuffer body) {
        this.buffer =body;
    }

    public void setup(StreamSinkChannel channel) {
        while(true) {
            try {
                int c = channel.write(this.buffer);
                if (this.buffer.hasRemaining() && c > 0) {
                    continue;
                }

                if (this.buffer.hasRemaining()) {
                    channel.getWriteSetter().set(this);
                    channel.resumeWrites();
                } else {
                    this.writeDone(channel);
                }
            } catch (IOException var3) {
                this.handleError(channel, var3);
            }

            return;
        }
    }

    protected void handleError(StreamSinkChannel channel, IOException e) {
        UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
        IoUtils.safeClose(channel);
    }

    public void handleEvent(StreamSinkChannel channel) {
        while(true) {
            try {
                int c = channel.write(this.buffer);
                if (this.buffer.hasRemaining() && c > 0) {
                    continue;
                }

                if (this.buffer.hasRemaining()) {
                    channel.resumeWrites();
                    return;
                }

                this.writeDone(channel);
            } catch (IOException var3) {
                this.handleError(channel, var3);
            }

            return;
        }
    }

    public boolean hasRemaining() {
        return this.buffer.hasRemaining();
    }

    protected void writeDone(final StreamSinkChannel channel) {
        try {
            channel.shutdownWrites();
            if (!channel.flush()) {
                channel.getWriteSetter().set(ChannelListeners.flushingChannelListener(new ChannelListener<StreamSinkChannel>() {
                    public void handleEvent(StreamSinkChannel o) {
                        IoUtils.safeClose(channel);
                    }
                }, ChannelListeners.closingChannelExceptionHandler()));
                channel.resumeWrites();
            }
        } catch (IOException var3) {
            this.handleError(channel, var3);
        }

    }

}


