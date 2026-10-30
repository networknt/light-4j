package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import org.junit.jupiter.api.*;
import org.xnio.*;
import java.lang.ref.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.io.IOException;
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

        Map<String, Type> cleanupFields = instanceFields(ConnectionWrappers.Cleanup.class, Object.class);
        assertEquals(Set.of("records", "key"), cleanupFields.keySet());
        ParameterizedType records = assertInstanceOf(ParameterizedType.class, cleanupFields.get("records"));
        assertEquals(ConcurrentMap.class, records.getRawType());
        Class<?> declaredKey = assertInstanceOf(Class.class, cleanupFields.get("key"));
        assertTrue(WeakReference.class.isAssignableFrom(declaredKey));
        assertTrue(declaredKey.isInstance(key));
        assertArrayEquals(new Type[]{declaredKey, ConnectionWrappers.Entry.class}, records.getActualTypeArguments());
        for (Type field : instanceFields(key.getClass(), WeakReference.class).values()) {
            assertTrue(field instanceof Class<?> type && type.isPrimitive(), "identity keys must not own other objects");
        }

        Map<String, Type> entryFields = instanceFields(ConnectionWrappers.Entry.class, Object.class);
        Set<String> owningFields = new HashSet<>();
        entryFields.forEach((name, type) -> {
            if (!(type instanceof Class<?> concrete && concrete.isPrimitive())) owningFields.add(name);
        });
        assertEquals(Set.of("closed", "closeReason", "view"), owningFields,
                "new owning Entry fields require an explicit retention review");
        assertEquals(AtomicBoolean.class, entryFields.get("closed"));
        assertEquals(IOException.class, entryFields.get("closeReason"));
        ParameterizedType viewHolder = assertInstanceOf(ParameterizedType.class, entryFields.get("view"));
        assertEquals(AtomicReference.class, viewHolder.getRawType());
        ParameterizedType weakView = assertInstanceOf(ParameterizedType.class, viewHolder.getActualTypeArguments()[0]);
        assertEquals(WeakReference.class, weakView.getRawType());
        assertArrayEquals(new Type[]{IoThreadClientConnection.class}, weakView.getActualTypeArguments());
        assertSame(view, entry(cleanup).view.get().get());
        Reference.reachabilityFence(raw);
    }
    @Test void cleanupUsesIdentityEqualityAndDiagnostics() throws ReflectiveOperationException {
        assertEquals(Object.class, ConnectionWrappers.Cleanup.class.getMethod("equals", Object.class).getDeclaringClass());
        assertEquals(Object.class, ConnectionWrappers.Cleanup.class.getMethod("hashCode").getDeclaringClass());
        assertEquals(Object.class, ConnectionWrappers.Cleanup.class.getMethod("toString").getDeclaringClass());
        ClientConnection raw = transport();
        registry.getOrCreate(raw);
        ConnectionWrappers.Cleanup cleanup = registeredAction(raw);
        assertEquals(System.identityHashCode(cleanup), cleanup.hashCode());
        Reference.reachabilityFence(raw);
    }
    // Inspect ownership declarations only: no field values, map implementation assumptions or GC timing.
    private static Map<String, Type> instanceFields(Class<?> type, Class<?> stop) {
        Map<String, Type> fields = new HashMap<>();
        for (Class<?> current = type; current != stop; current = current.getSuperclass()) {
            assertNotNull(current, "unexpected field hierarchy");
            for (var field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    assertNull(fields.put(field.getName(), field.getGenericType()), "shadowed ownership declaration");
                }
            }
        }
        return fields;
    }
}
