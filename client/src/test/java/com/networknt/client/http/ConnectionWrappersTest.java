package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Isolated;
import org.xnio.*;
import java.lang.ref.Reference;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Isolated("Exercises the real static connection registry")
class ConnectionWrappersTest {
    private XnioWorker worker;
    private XnioIoThread owner;
    private final List<ConnectionWrappers.Entry> registrations = new ArrayList<>();

    @BeforeEach void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
    }
    @AfterEach void tearDown() throws Exception {
        for (ConnectionWrappers.Entry entry : registrations) {
            entry.key.clear();
            entry.key.enqueue();
            entry.cleanable.clean();
        }
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
        IoThreadClientConnection.wrap(raw);
        registrations.add(ConnectionWrappers.lookup(raw));
        return raw;
    }
    @Test void realRegistryRecreatesAClearedViewWithRetirementIntact() throws Exception {
        ClientConnection raw = transport();
        IoThreadClientConnection original = IoThreadClientConnection.wrap(raw);
        ConnectionWrappers.Entry state = ConnectionWrappers.lookup(raw);
        drainOwner();
        original.close();
        drainOwner();
        state.view.get().clear();
        IoThreadClientConnection recreated = IoThreadClientConnection.wrap(raw);
        assertSame(state, ConnectionWrappers.lookup(raw));
        assertTrue(ConnectionWrappers.isRegistered(state));
        assertNotSame(original, recreated);
        assertFalse(recreated.isOpen());
        assertFalse(original.isOpen());
        CompletableFuture<ClientConnection> lateClose = new CompletableFuture<>();
        recreated.addCloseListener(lateClose::complete);
        assertSame(recreated, lateClose.get(5, TimeUnit.SECONDS));
        Reference.reachabilityFence(raw);
    }
    @Test void registeredCleanerActionPurgesActualWeakKeysAndQueueWithoutAnotherWrap() {
        ClientConnection first = transport();
        ClientConnection second = transport();
        ClientConnection live = transport();
        ConnectionWrappers.Entry firstState = ConnectionWrappers.lookup(first);
        ConnectionWrappers.Entry secondState = ConnectionWrappers.lookup(second);
        ConnectionWrappers.Entry liveState = ConnectionWrappers.lookup(live);
        assertTrue(ConnectionWrappers.isRegistered(firstState));
        assertTrue(ConnectionWrappers.isRegistered(secondState));
        firstState.key.clear();
        secondState.key.clear();
        assertTrue(firstState.key.enqueue());
        assertTrue(secondState.key.enqueue());
        firstState.cleanable.clean(); // Actual registered action also drains the real queue's other key.
        assertFalse(ConnectionWrappers.isRegistered(firstState));
        assertFalse(ConnectionWrappers.isRegistered(secondState));
        assertTrue(ConnectionWrappers.isRegistered(liveState));
        Reference.reachabilityFence(first);
        Reference.reachabilityFence(second);
        Reference.reachabilityFence(live);
    }
    @Test void realCollectionAndIdlePurgeWorkInAControlledJvm() throws Exception {
        String vm = System.getProperty("java.vm.name");
        Assumptions.assumeTrue(vm.contains("OpenJDK") || vm.contains("HotSpot"), "Controlled Serial GC probe requires HotSpot");
        Path output = Files.createTempFile("client-registry-gc-", ".log");
        ProcessBuilder builder = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xint", "-Xms16m", "-Xmx64m", "-XX:+UseSerialGC", "-XX:-DisableExplicitGC",
                "-Duser.home=" + System.getProperty("user.home"),
                "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                ConnectionRegistryGcProbe.class.getName()).redirectErrorStream(true).redirectOutput(output.toFile());
        // Parent collector/agent flags must not change the deliberately controlled child VM.
        for (String variable : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) builder.environment().remove(variable);
        Process probe = builder.start();
        boolean success = false;
        try {
            boolean finished = probe.waitFor(30, TimeUnit.SECONDS);
            if (!finished) probe.destroyForcibly();
            assertTrue(finished, "GC probe did not terminate: " + Files.readString(output));
            String log = Files.readString(output);
            assertEquals(0, probe.exitValue(), log);
            assertTrue(log.contains("REGISTRY_GC_OK"), log);
            success = true;
        } finally {
            if (probe.isAlive()) probe.destroyForcibly();
            if (success) Files.deleteIfExists(output);
        }
    }
    @Test void recreatedViewKeepsTheShutdownRetirementReason() throws Exception {
        ClientConnection raw = transport();
        IoThreadClientConnection original = IoThreadClientConnection.wrap(raw);
        ConnectionWrappers.Entry state = ConnectionWrappers.lookup(raw);
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
        assertSame(state, ConnectionWrappers.lookup(raw));
        assertSame(reason, state.closeReason);
        Reference.reachabilityFence(raw);
    }
}
