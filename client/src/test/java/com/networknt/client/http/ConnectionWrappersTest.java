package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import org.junit.jupiter.api.*;
import org.xnio.*;
import java.io.IOException;
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
    private Runnable registeredAction(ClientConnection raw) {
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
        verify(cleaner).register(same(raw), action.capture());
        return action.getValue();
    }
    @Test void viewRecreationPreservesRetirementAndLateNotification() throws Exception {
        ClientConnection raw = transport();
        IoThreadClientConnection original = registry.getOrCreate(raw);
        Runnable action = registeredAction(raw);
        original.close();
        drainOwner();
        weakLink(action, original, raw).clear();
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
    @Test void registeredCleanupRetainsNeitherTransportNorViewAndRemovesItsEntry() throws Exception {
        ClientConnection dead = transport();
        ClientConnection live = transport();
        IoThreadClientConnection deadView = registry.getOrCreate(dead);
        IoThreadClientConnection liveView = registry.getOrCreate(live);
        Runnable cleanup = registeredAction(dead);
        assertNoStrongPath(cleanup, dead, deadView, live, liveView);
        weakLink(cleanup, dead, deadView);
        weakLink(cleanup, deadView, dead);
        cleanup.run();
        cleanup.run();
        assertNull(registry.lookup(dead));
        assertSame(liveView, registry.lookup(live));
        assertTrue(strongGraph(cleanup, dead, deadView, live, liveView).stream()
                .noneMatch(value -> value instanceof WeakReference<?> weak && weak.get() == deadView),
                "the removed entry's view reference must also leave the captured action's map graph");
        Reference.reachabilityFence(dead);
        Reference.reachabilityFence(live);
    }
    @Test void delayedRegisteredActionCannotRemoveAReplacement() {
        ClientConnection raw = transport();
        registry.getOrCreate(raw);
        Runnable oldAction = registeredAction(raw);
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
        Runnable action = registeredAction(raw);
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        original.close();
        IOException reason = strongGraph(action, raw, original, Throwable.class).stream()
                .filter(IOException.class::isInstance).map(IOException.class::cast).findFirst().orElseThrow();
        assertInstanceOf(RejectedExecutionException.class, reason.getCause());
        weakLink(action, original, raw, Throwable.class).clear();
        IoThreadClientConnection recreated = registry.getOrCreate(raw);
        assertFalse(recreated.isOpen());
        assertTrue(strongGraph(action, raw, recreated, Throwable.class).contains(reason));
        registeredAction(raw);
        Reference.reachabilityFence(raw);
    }
    @Test void lookupOfAnUnregisteredTransportHasNoEffects() {
        assertNull(registry.lookup(transport()));
        verifyNoInteractions(cleaner);
    }
    @Test void retentionCheckRejectsMapsWeakReferenceSubclassesAndInheritedStrongFields() {
        Object target = new Object();
        for (Object retaining : List.of(Map.of(target, "key"), Map.of("value", target),
                new RetainingWeakReference(target), new InheritedRetention(target))) {
            assertThrows(org.opentest4j.AssertionFailedError.class, () -> assertNoStrongPath(retaining, target));
        }
    }
    private static class Retention {
        private final Object retained;
        Retention(Object retained) { this.retained = retained; }
    }
    private static final class InheritedRetention extends Retention {
        InheritedRetention(Object retained) { super(retained); }
    }
    private static final class RetainingWeakReference extends WeakReference<Object> {
        private final Object retained;
        RetainingWeakReference(Object retained) { super(retained); this.retained = retained; }
    }
    private static void assertNoStrongPath(Object root, Object... targets) throws IllegalAccessException {
        Set<Object> graph = strongGraph(root, targets);
        for (Object target : targets) assertFalse(graph.contains(target), "captured cleanup action strongly retains a target");
    }
    private static WeakReference<?> weakLink(Object root, Object target, Object... leaves) throws IllegalAccessException {
        List<Object> terminals = new ArrayList<>(Arrays.asList(leaves));
        terminals.add(target);
        terminals.add(Throwable.class);
        for (Object value : strongGraph(root, terminals.toArray())) {
            if (value instanceof WeakReference<?> weak && weak.get() == target) return weak;
        }
        throw new AssertionError("No weak link to target");
    }
    // Read-only, per-action graph inspection: no global registry/field names, final-field writes or GC timing.
    private static Set<Object> strongGraph(Object root, Object... leaves) throws IllegalAccessException {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Object> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            Object value = pending.removeFirst();
            if (!seen.add(value) || isLeaf(value, leaves) || immutable(value)) continue;
            boolean adapted = enqueueContents(value, pending);
            Class<?> base = enqueueCustomFields(value, pending);
            if (!adapted && base != Object.class) throw new IllegalArgumentException("No graph adapter for " + base);
        }
        return seen;
    }
    private static boolean isLeaf(Object value, Object[] leaves) {
        for (Object leaf : leaves) {
            if (value == leaf || leaf instanceof Class<?> type && type.isInstance(value)) return true;
        }
        return false;
    }
    private static boolean immutable(Object value) {
        return Set.of(String.class, Boolean.class, Byte.class, Short.class, Integer.class, Long.class,
                Float.class, Double.class, Character.class).contains(value.getClass());
    }
    private static Class<?> enqueueCustomFields(Object value, Deque<Object> pending) throws IllegalAccessException {
        Class<?> type = value.getClass();
        while (type != Object.class && !type.getPackageName().startsWith("java.")) {
            for (var field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                    assertTrue(field.trySetAccessible(), field.toString());
                    enqueue(field.get(value), pending);
                }
            }
            type = type.getSuperclass();
        }
        return type;
    }
    private static boolean enqueueContents(Object value, Deque<Object> pending) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, entry) -> { enqueue(key, pending); enqueue(entry, pending); });
        } else if (value instanceof Reference<?> reference) {
            if (!(reference instanceof WeakReference<?>) && !(reference instanceof PhantomReference<?>)) enqueue(reference.get(), pending);
        } else if (value instanceof AtomicReference<?> reference) {
            enqueue(reference.get(), pending);
        } else if (value instanceof Iterable<?> iterable) {
            iterable.forEach(entry -> enqueue(entry, pending));
        } else if (value instanceof Object[] array) {
            for (Object entry : array) enqueue(entry, pending);
        } else return value instanceof AtomicBoolean || value instanceof AtomicInteger || value instanceof AtomicLong;
        return true;
    }
    private static void enqueue(Object value, Deque<Object> pending) {
        if (value != null) pending.addLast(value);
    }
}
