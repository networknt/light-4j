package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import org.junit.jupiter.api.*;
import org.xnio.*;
import java.lang.ref.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionWrappersTest {
    private XnioWorker worker;
    private XnioIoThread owner;
    private Cleaner cleaner;
    private ConnectionWrappers registry;

    @BeforeEach void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
        cleaner = mock(Cleaner.class);
        registry = new ConnectionWrappers(cleaner);
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
    private ConnectionWrappers.Cleanup registeredAction(ClientConnection raw) {
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
        verify(cleaner).register(same(raw), action.capture());
        return assertInstanceOf(ConnectionWrappers.Cleanup.class, action.getValue());
    }
    private ConnectionWrappers.Entry entry(ConnectionWrappers.Cleanup action) {
        return action.records().get(action.key());
    }
    @Test void viewRecreationPreservesRetirementAndLateNotification() throws Exception {
        ClientConnection raw = transport();
        IoThreadClientConnection original = registry.getOrCreate(raw);
        ConnectionWrappers.Cleanup action = registeredAction(raw);
        original.close();
        drainOwner();
        entry(action).view.get().clear();
        IoThreadClientConnection recreated = registry.getOrCreate(raw);
        assertNotSame(original, recreated);
        assertFalse(recreated.isOpen());
        assertFalse(original.isOpen());
        CompletableFuture<ClientConnection> lateClose = new CompletableFuture<>();
        recreated.addCloseListener(lateClose::complete);
        assertSame(recreated, lateClose.get(5, TimeUnit.SECONDS));
        registeredAction(raw);
        Reference.reachabilityFence(raw);
    }
    @Test void registeredCleanupRemovesOnlyItsEntryAndIsIdempotent() {
        ClientConnection dead = transport();
        ClientConnection live = transport();
        registry.getOrCreate(dead);
        IoThreadClientConnection liveView = registry.getOrCreate(live);
        ConnectionWrappers.Cleanup cleanup = registeredAction(dead);
        assertEquals(2, cleanup.records().size());
        cleanup.run();
        cleanup.run();
        assertEquals(1, cleanup.records().size());
        assertNull(registry.lookup(dead));
        assertSame(liveView, registry.lookup(live));
        Reference.reachabilityFence(dead);
        Reference.reachabilityFence(live);
    }
    @Test void delayedRegisteredActionCannotRemoveAReplacement() {
        ClientConnection raw = transport();
        registry.getOrCreate(raw);
        ConnectionWrappers.Cleanup oldAction = registeredAction(raw);
        oldAction.run();
        assertNull(registry.lookup(raw));
        IoThreadClientConnection current = registry.getOrCreate(raw);
        oldAction.run();
        assertSame(current, registry.lookup(raw));
        assertSame(current, registry.getOrCreate(raw));
        verify(cleaner, times(2)).register(same(raw), any(Runnable.class));
        Reference.reachabilityFence(raw);
    }
    @Test void recreatedViewKeepsTheShutdownRetirementReason() throws Exception {
        ClientConnection raw = transport();
        IoThreadClientConnection original = registry.getOrCreate(raw);
        ConnectionWrappers.Cleanup action = registeredAction(raw);
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        original.close();
        var reason = entry(action).closeReason;
        assertNotNull(reason);
        assertInstanceOf(RejectedExecutionException.class, reason.getCause());
        entry(action).view.get().clear();
        IoThreadClientConnection recreated = registry.getOrCreate(raw);
        assertFalse(recreated.isOpen());
        assertSame(reason, entry(action).closeReason);
        registeredAction(raw);
        Reference.reachabilityFence(raw);
    }
    @Test void lookupOfAnUnregisteredTransportHasNoEffects() {
        assertNull(registry.lookup(transport()));
        verifyNoInteractions(cleaner);
    }
    @Test void cleanupHasOnlyTypedStorageAndWeakIdentityKeys() {
        ClientConnection raw = transport();
        IoThreadClientConnection view = registry.getOrCreate(raw);
        ConnectionWrappers.Cleanup cleanup = registeredAction(raw);
        WeakReference<ClientConnection> key = cleanup.key();
        assertSame(raw, key.get());
        assertTrue(ConnectionWrappers.Cleanup.class.isRecord());
        assertEquals(List.of(ConcurrentMap.class, key.getClass()),
                Arrays.stream(ConnectionWrappers.Cleanup.class.getRecordComponents()).map(component -> component.getType()).toList());
        assertSame(WeakReference.class, key.getClass().getSuperclass());
        assertTrue(Modifier.isFinal(key.getClass().getModifiers()));
        assertEquals(List.of(int.class), Arrays.stream(key.getClass().getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers())).map(field -> field.getType()).toList());
        assertSame(ConcurrentHashMap.class, cleanup.records().getClass());
        for (Object stored : cleanup.records().keySet()) assertSame(key.getClass(), stored.getClass());
        for (ConnectionWrappers.Entry stored : cleanup.records().values()) {
            assertSame(ConnectionWrappers.Entry.class, stored.getClass());
            assertSame(AtomicBoolean.class, stored.closed.getClass());
            assertNull(stored.closeReason);
            assertSame(AtomicReference.class, stored.view.getClass());
            assertSame(WeakReference.class, stored.view.get().getClass());
        }
        assertSame(view, entry(cleanup).view.get().get());
        Reference.reachabilityFence(raw);
    }
}
