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

    /**
     * Constructs a ByteBufferWriteChannelListener with the given body buffer.
     * @param body the buffer containing the data to write
     */
    public ByteBufferWriteChannelListener(ByteBuffer body) {
        this.buffer =body;
    }

    /**
     * Sets up the channel for writing.
     * @param channel the channel to write to
     */
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

    /**
     * Handles an error that occurs during writing.
     * @param channel the channel where the error occurred
     * @param e the exception that occurred
     */
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

    /**
     * Checks if the buffer has remaining data to write.
     * @return true if there is data remaining, false otherwise
     */
    public boolean hasRemaining() {
        return this.buffer.hasRemaining();
    }

    /**
     * Called when the writing is finished.
     * @param channel the channel that was written to
     */
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
