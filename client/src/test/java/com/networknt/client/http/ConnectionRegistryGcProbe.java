package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import org.xnio.*;
import java.lang.ref.*;
import java.lang.reflect.Proxy;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/** Controlled subprocess qualification; no Mockito history or parent GC policy can retain the views. */
public final class ConnectionRegistryGcProbe {
    private ConnectionRegistryGcProbe() { }
    private record Released(WeakReference<ClientConnection> raw, ReferenceQueue<ClientConnection> queue,
                            ConnectionWrappers.Entry entry) { }

    public static void main(String[] args) throws Exception {
        XnioWorker worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        try {
            Released released = exerciseRealRegistry(worker.getIoThread());
            collect(released.raw, released.queue);
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (ConnectionWrappers.isRegistered(released.entry) && System.nanoTime() < end) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
            if (ConnectionWrappers.isRegistered(released.entry)) throw new AssertionError("Idle registry entry was not autonomously purged");
            System.out.println("REGISTRY_GC_OK");
        } finally {
            worker.shutdownNow();
            if (!worker.awaitTermination(5, TimeUnit.SECONDS)) throw new AssertionError("Worker did not stop");
        }
    }
    private static Released exerciseRealRegistry(XnioIoThread owner) throws Exception {
        ClientConnection raw = (ClientConnection) Proxy.newProxyInstance(ClientConnection.class.getClassLoader(),
                new Class<?>[]{ClientConnection.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getIoThread" -> owner;
                    case "isOpen" -> true;
                    case "addCloseListener", "close" -> null; // This provider deliberately drops listeners.
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "registry-gc-probe";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        ReferenceQueue<IoThreadClientConnection> views = new ReferenceQueue<>();
        WeakReference<IoThreadClientConnection> previous = retireView(raw, owner, views);
        collect(previous, views);
        IoThreadClientConnection recreated = IoThreadClientConnection.wrap(raw);
        if (recreated.isOpen()) throw new AssertionError("Collected view lost transport retirement");
        ConnectionWrappers.Entry state = ConnectionWrappers.lookup(raw);
        if (!ConnectionWrappers.isRegistered(state)) throw new AssertionError("Live raw transport lost its entry");
        ReferenceQueue<ClientConnection> raws = new ReferenceQueue<>();
        Released released = new Released(new WeakReference<>(raw, raws), raws, state);
        Reference.reachabilityFence(raw);
        Reference.reachabilityFence(recreated);
        return released;
    }
    private static WeakReference<IoThreadClientConnection> retireView(ClientConnection raw, XnioIoThread owner,
                                                                    ReferenceQueue<IoThreadClientConnection> queue) throws Exception {
        IoThreadClientConnection view = IoThreadClientConnection.wrap(raw);
        drain(owner);
        view.close();
        drain(owner);
        return new WeakReference<>(view, queue);
    }
    private static void drain(XnioIoThread owner) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        owner.execute(() -> done.complete(null));
        done.get(5, TimeUnit.SECONDS);
    }
    @SuppressWarnings("java:S1215") // The isolated VM explicitly enables Serial GC; collection is this probe's contract.
    private static void collect(Reference<?> expected, ReferenceQueue<?> queue) throws InterruptedException {
        for (int attempt = 0; attempt < 8; attempt++) {
            System.gc();
            if (queue.remove(100) == expected) return;
            byte[][] pressure = new byte[12][];
            for (int i = 0; i < pressure.length; i++) pressure[i] = new byte[1024 * 1024];
            Reference.reachabilityFence(pressure);
        }
        throw new AssertionError("Reference retained across controlled collections");
    }
}
