package com.networknt.handler.conduit;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.AbstractServerConnection;
import org.xnio.Buffers;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.conduits.AbstractStreamSourceConduit;
import org.xnio.conduits.ConduitReadableByteChannel;
import org.xnio.conduits.StreamSourceConduit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

public class ContentStreamSourceConduit extends AbstractStreamSourceConduit<StreamSourceConduit> {

    private final AbstractServerConnection connection;

    protected ContentStreamSourceConduit(StreamSourceConduit next, final AbstractServerConnection connection) {
        super(next);
        this.connection = connection;
    }

    public long transferTo(final long position, final long count, final FileChannel target) throws IOException {
        return target.transferFrom(new ConduitReadableByteChannel(this), position, count);
    }

    public long transferTo(final long count, final ByteBuffer throughBuffer, final StreamSinkChannel target) throws IOException {
        return IoUtils.transfer(new ConduitReadableByteChannel(this), count, throughBuffer, target);
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        PooledByteBuffer eb = connection.getExtraBytes();
        if (eb != null) {
            final ByteBuffer buffer = eb.getBuffer();
            int result = Buffers.copy(dst, buffer);
            if (!buffer.hasRemaining()) {
                eb.close();
                connection.setExtraBytes(null);
            }
            return result;
        } else {
            return super.read(dst);
        }
    }

    @Override
    public long read(final ByteBuffer[] dsts, final int offs, final int len) throws IOException {
        PooledByteBuffer eb = connection.getExtraBytes();
        if (eb != null) {
            final ByteBuffer buffer = eb.getBuffer();
            int result = Buffers.copy(dsts, offs, len, buffer);
            if (!buffer.hasRemaining()) {
                eb.close();
                connection.setExtraBytes(null);
            }
            return result;
        } else {
            return super.read(dsts, offs, len);
        }
    }

    @Override
    public void resumeReads() {
        if (connection.getExtraBytes() != null) {
            wakeupReads();
        } else {
            super.resumeReads();
        }
    }

    @Override
    public void awaitReadable() throws IOException {
        if (connection.getExtraBytes() != null) {
            return;
        }
        super.awaitReadable();
    }

    @Override
    public void awaitReadable(final long time, final TimeUnit timeUnit) throws IOException {
        if (connection.getExtraBytes() != null) {
            return;
        }
        super.awaitReadable(time, timeUnit);
    }
}
