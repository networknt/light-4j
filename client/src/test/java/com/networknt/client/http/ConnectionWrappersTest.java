package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import org.junit.jupiter.api.*;
import org.xnio.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.ref.Cleaner;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionWrappersTest {
    private XnioWorker worker;
    private XnioIoThread owner;
    private Cleaner cleaner;
    private ConcurrentMap<WeakReference<ClientConnection>, ConnectionWrappers.Entry> storage;
    private ConnectionWrappers registry;

    @BeforeEach void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
        cleaner = mock(Cleaner.class);
        storage = new ConcurrentHashMap<>();
        registry = new ConnectionWrappers(cleaner, storage);
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
    private ClientConnection transport() {
        ClientConnection raw = mock(ClientConnection.class);
        when(raw.getIoThread()).thenReturn(owner);
        when(raw.isOpen()).thenReturn(true);
        return raw;
    }
    private Runnable registeredAction(ClientConnection raw) {
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
        verify(cleaner).register(same(raw), action.capture());
        return action.getValue();
    }
    @Test void viewRecreationPreservesRetirementAndLateNotification() throws Exception {
        ClientConnection raw = transport();
        IoThreadClientConnection original = registry.getOrCreate(raw);
        ConnectionWrappers.Entry state = storage.values().iterator().next();
        original.close();
        drainOwner();
        state.view.get().clear();
        IoThreadClientConnection recreated = registry.getOrCreate(raw);
        assertSame(state, storage.values().iterator().next());
        assertNotSame(original, recreated);
        assertFalse(recreated.isOpen());
        assertFalse(original.isOpen());
        CompletableFuture<ClientConnection> lateClose = new CompletableFuture<>();
        recreated.addCloseListener(lateClose::complete);
        assertSame(recreated, lateClose.get(5, TimeUnit.SECONDS));
        registeredAction(raw); // Recreation reuses one Cleaner registration.
        Reference.reachabilityFence(raw);
    }
    @Test void registeredWeakCleanupRemovesOnlyItsEntryWithoutAnotherWrap() {
        ClientConnection dead = transport();
        ClientConnection live = transport();
        registry.getOrCreate(dead);
        IoThreadClientConnection liveView = registry.getOrCreate(live);
        Runnable cleanup = registeredAction(dead);
        Reference<?> weak = assertInstanceOf(WeakReference.class, cleanup);
        assertSame(dead, weak.get());
        assertTrue(storage.containsKey(weak), "Cleaner receives the actual stored identity key");
        // Retention contract: the action may own weak references/maps/identity values, never a transport/view/closure.
        for (var field : cleanup.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertTrue(field.getType().isPrimitive() || Map.class.isAssignableFrom(field.getType())
                        || Reference.class.isAssignableFrom(field.getType()), field.toString());
            }
        }
        weak.clear();
        assertNull(weak.get());
        assertEquals(2, storage.size(), "clearing a key alone must not hide a missing Cleaner action");
        cleanup.run();
        cleanup.run();
        assertEquals(1, storage.size());
        assertSame(liveView, registry.lookup(live));
        Reference.reachabilityFence(dead);
        Reference.reachabilityFence(live);
    }
    @Test void delayedRegisteredActionCannotRemoveAReplacement() {
        ClientConnection raw = transport();
        registry.getOrCreate(raw);
        ArgumentCaptor<Runnable> actions = ArgumentCaptor.forClass(Runnable.class);
        verify(cleaner).register(same(raw), actions.capture());
        Runnable oldAction = actions.getValue();
        assertInstanceOf(Reference.class, oldAction).clear();
        IoThreadClientConnection current = registry.getOrCreate(raw);
        assertEquals(2, storage.size());
        oldAction.run();
        assertEquals(1, storage.size());
        assertSame(current, registry.lookup(raw));
        assertSame(current, registry.getOrCreate(raw));
        verify(cleaner, times(2)).register(same(raw), any(Runnable.class));
        Reference.reachabilityFence(raw);
    }
    @Test void recreatedViewKeepsTheShutdownRetirementReason() throws Exception {
        ClientConnection raw = transport();
        IoThreadClientConnection original = registry.getOrCreate(raw);
        ConnectionWrappers.Entry state = storage.values().iterator().next();
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        original.close();
        var reason = state.closeReason;
        assertNotNull(reason);
        assertInstanceOf(RejectedExecutionException.class, reason.getCause());
        state.view.get().clear();
        IoThreadClientConnection recreated = registry.getOrCreate(raw);
        assertFalse(recreated.isOpen());
        assertSame(reason, state.closeReason);
        registeredAction(raw);
        Reference.reachabilityFence(raw);
    }
    @Test void lookupOfAnUnregisteredTransportHasNoEffects() {
        ClientConnection raw = transport();
        assertNull(registry.lookup(raw));
        assertTrue(storage.isEmpty());
        verifyNoInteractions(cleaner);
    }
}
