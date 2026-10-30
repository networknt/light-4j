package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import org.junit.jupiter.api.*;
import org.xnio.*;
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionWrappersTest {
    private XnioWorker worker;
    private XnioIoThread owner;

    @BeforeEach void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
    }
    @AfterEach void tearDown() throws Exception {
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }
    private void drainOwner() throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        owner.execute(() -> done.complete(null));
        done.get(5, TimeUnit.SECONDS);
    }
    private WeakReference<IoThreadClientConnection> retireAndReleaseView(ClientConnection raw, ReferenceQueue<IoThreadClientConnection> collected) throws Exception {
        IoThreadClientConnection view = IoThreadClientConnection.wrap(raw);
        drainOwner();
        view.close();
        drainOwner();
        // Simulate a provider that drops listeners: Mockito's invocation history is their only owner.
        clearInvocations(raw);
        return new WeakReference<>(view, collected);
    }
    @SuppressWarnings("java:S1215") // This regression must observe collection of an otherwise unreachable connection view.
    @Test void retiredTransportStaysClosedAfterItsViewIsCollected() throws Exception {
        ClientConnection raw = mock(ClientConnection.class);
        when(raw.getIoThread()).thenReturn(owner);
        when(raw.isOpen()).thenReturn(true); // Native close is deliberately ignored by this provider.
        ReferenceQueue<IoThreadClientConnection> collected = new ReferenceQueue<>();
        WeakReference<IoThreadClientConnection> previous = retireAndReleaseView(raw, collected);
        System.gc();
        assertSame(previous, collected.remove(5000));
        assertNull(previous.get(), "the view must be collectible even while the raw transport remains reachable");
        IoThreadClientConnection recreated = IoThreadClientConnection.wrap(raw);
        assertFalse(recreated.isOpen(), "retirement belongs to the transport, not its collected view");
        CompletableFuture<ClientConnection> lateClose = new CompletableFuture<>();
        recreated.addCloseListener(lateClose::complete);
        assertSame(recreated, lateClose.get(5, TimeUnit.SECONDS));
    }
    @Test void recreatedViewKeepsTheShutdownRetirementReason() throws Exception {
        ClientConnection raw = mock(ClientConnection.class);
        when(raw.getIoThread()).thenReturn(owner);
        when(raw.isOpen()).thenReturn(true);
        ConnectionWrappers.Entry state = new ConnectionWrappers.Entry();
        IoThreadClientConnection original = new IoThreadClientConnection(raw, state);
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        original.close();
        var reason = state.closeReason;
        assertNotNull(reason);
        assertInstanceOf(RejectedExecutionException.class, reason.getCause());
        IoThreadClientConnection recreated = new IoThreadClientConnection(raw, state);
        assertFalse(recreated.isOpen());
        assertSame(reason, state.closeReason);
    }
}
