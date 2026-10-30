package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import org.junit.jupiter.api.*;
import org.xnio.*;
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
    @Test void clearedWeakViewRecreationPreservesRetirement() throws Exception {
        ClientConnection raw = mock(ClientConnection.class);
        when(raw.getIoThread()).thenReturn(owner);
        when(raw.isOpen()).thenReturn(true); // This provider deliberately ignores native close.
        ConnectionWrappers.Entry state = new ConnectionWrappers.Entry();
        IoThreadClientConnection original = IoThreadClientConnection.wrap(state.getOrCreateView(raw));
        drainOwner();
        original.close();
        drainOwner();
        // Reference.clear() models collection's observable effect, independently of GC policy/timing.
        state.view.get().clear();
        IoThreadClientConnection recreated = IoThreadClientConnection.wrap(state.getOrCreateView(raw));
        assertNotSame(original, recreated);
        assertFalse(recreated.isOpen());
        assertFalse(original.isOpen());
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
