package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import org.junit.jupiter.api.*;
import org.xnio.*;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionWrappersTest {
    private XnioWorker worker;
    private XnioIoThread owner;
    private final List<Reference<ClientConnection>> registrations = new ArrayList<>();

    @BeforeEach void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
    }
    @AfterEach void tearDown() throws Exception {
        for (Reference<ClientConnection> key : registrations) {
            key.clear();
            ((Runnable) key).run();
        }
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }
    private void drainOwner() throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        owner.execute(() -> done.complete(null));
        done.get(5, TimeUnit.SECONDS);
    }
    @SuppressWarnings("unchecked")
    private Map<Reference<ClientConnection>, ConnectionWrappers.Entry> registry() throws Exception {
        Field field = ConnectionWrappers.class.getDeclaredField("entries");
        field.setAccessible(true);
        return (Map<Reference<ClientConnection>, ConnectionWrappers.Entry>) field.get(null);
    }
    private Reference<ClientConnection> keyFor(ClientConnection raw) throws Exception {
        return registry().keySet().stream().filter(key -> key.get() == raw).findFirst().orElseThrow();
    }
    private ClientConnection transport() throws Exception {
        ClientConnection raw = mock(ClientConnection.class);
        when(raw.getIoThread()).thenReturn(owner);
        when(raw.isOpen()).thenReturn(true);
        IoThreadClientConnection.wrap(raw);
        registrations.add(keyFor(raw));
        return raw;
    }
    @Test void realRegistryRecreatesAClearedViewWithRetirementIntact() throws Exception {
        ClientConnection raw = transport();
        IoThreadClientConnection original = IoThreadClientConnection.wrap(raw);
        ConnectionWrappers.Entry state = registry().get(keyFor(raw));
        drainOwner();
        original.close();
        drainOwner();
        state.view.get().clear();
        IoThreadClientConnection recreated = IoThreadClientConnection.wrap(raw);
        assertSame(state, registry().get(keyFor(raw)));
        assertNotSame(original, recreated);
        assertFalse(recreated.isOpen());
        assertFalse(original.isOpen());
        CompletableFuture<ClientConnection> lateClose = new CompletableFuture<>();
        recreated.addCloseListener(lateClose::complete);
        assertSame(recreated, lateClose.get(5, TimeUnit.SECONDS));
        Reference.reachabilityFence(raw);
    }
    @Test void cleanerActionRemovesOnlyItsActualRegistryKeyWithoutAnotherWrap() throws Exception {
        ClientConnection dead = transport();
        ClientConnection live = transport();
        Reference<ClientConnection> deadKey = keyFor(dead);
        Reference<ClientConnection> liveKey = keyFor(live);
        ConnectionWrappers.Entry liveState = registry().get(liveKey);
        deadKey.clear(); // Simulate reference processing, without GC timing or a child JVM.
        Runnable cleanup = (Runnable) deadKey; // This exact key is the registered Cleaner action.
        cleanup.run();
        cleanup.run(); // Cleanup is idempotent.
        assertFalse(registry().containsKey(deadKey));
        assertSame(liveState, registry().get(liveKey));
        Reference.reachabilityFence(dead);
        Reference.reachabilityFence(live);
    }
    @Test void staleCleanupCannotRemoveAReplacementForTheSameTransport() throws Exception {
        ClientConnection raw = transport();
        Reference<ClientConnection> oldKey = keyFor(raw);
        oldKey.clear();
        IoThreadClientConnection current = IoThreadClientConnection.wrap(raw);
        Reference<ClientConnection> newKey = keyFor(raw);
        registrations.add(newKey);
        ((Runnable) oldKey).run();
        assertFalse(registry().containsKey(oldKey));
        assertSame(current, ConnectionWrappers.find(raw));
        assertSame(current, IoThreadClientConnection.wrap(raw));
        assertNotNull(registry().get(newKey));
        Reference.reachabilityFence(raw);
    }
    @Test void recreatedViewKeepsTheShutdownRetirementReason() throws Exception {
        ClientConnection raw = transport();
        IoThreadClientConnection original = IoThreadClientConnection.wrap(raw);
        ConnectionWrappers.Entry state = registry().get(keyFor(raw));
        drainOwner();
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        original.close();
        var reason = state.closeReason;
        assertNotNull(reason);
        assertInstanceOf(RejectedExecutionException.class, reason.getCause());
        state.view.get().clear();
        IoThreadClientConnection recreated = IoThreadClientConnection.wrap(raw);
        assertFalse(recreated.isOpen());
        assertSame(state, registry().get(keyFor(raw)));
        assertSame(reason, state.closeReason);
        Reference.reachabilityFence(raw);
    }
}
