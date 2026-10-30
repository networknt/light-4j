import com.networknt.handler.HandlerContinuation;
import com.networknt.httpstring.AttachmentConstants;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.MDC;
import java.lang.management.ManagementFactory;
import java.util.*;

/** Warmed allocation-only microprobe. Excludes registration, transport, logging and gateway throughput. */
public class MdcAllocationProbe {
    private static volatile int sink;
    private static final int WARMUP = 100000;
    private static final int ITERATIONS = 100000;
    private static final com.sun.management.ThreadMXBean BEAN =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final long THREAD = Thread.currentThread().threadId();
    private static final HttpHandler CALLBACK = exchange -> sink++;

    private static void measure(String name, Runnable operation) {
        for (int i = 0; i < WARMUP; i++) operation.run();
        for (int sample = 0; sample < 3; sample++) {
            long before = BEAN.getThreadAllocatedBytes(THREAD);
            for (int i = 0; i < ITERATIONS; i++) operation.run();
            long bytes = BEAN.getThreadAllocatedBytes(THREAD) - before;
            System.out.printf(Locale.ROOT,
                    "ALLOC:{\"case\":\"%s\",\"sample\":%d,\"bytes_per_frame\":%.4f}%n",
                    name, sample, (double) bytes / ITERATIONS);
        }
    }

    private static void frame(String name, Map<String, String> request, Map<String, String> caller) {
        HttpServerExchange exchange = new HttpServerExchange(null);
        exchange.putAttachment(AttachmentConstants.MDC_CONTEXT, request);
        Runnable task = HandlerContinuation.task(exchange, HandlerContinuation.capture(exchange, CALLBACK));
        MDC.clear();
        if (!caller.isEmpty()) MDC.setContextMap(caller);
        measure(name, task);
        Map<String, String> restored = MDC.getCopyOfContextMap();
        if (!Objects.equals(caller, restored == null ? Map.of() : restored))
            throw new AssertionError("Caller MDC was not restored");
    }

    public static void main(String[] args) {
        if (!BEAN.isThreadAllocatedMemorySupported()) throw new IllegalStateException("Allocation counter unavailable");
        BEAN.setThreadAllocatedMemoryEnabled(true);
        measure("callback_only", () -> sink++);
        frame("empty_request_empty_caller", Map.of(), Map.of());
        Map<String, String> three = Map.of("correlation", "a", "trace", "b", "client", "c");
        frame("three_fields_matching_caller", three, three);
        frame("three_fields_different_caller", three, Map.of("worker", "outside"));
        frame("three_fields_different_caller_same_size", three, Map.of("worker", "outside", "other", "prior", "tenant", "outside"));
        Map<String, String> twelve = new HashMap<>();
        for (int i = 0; i < 12; i++) twelve.put("field" + i, "value" + i);
        frame("twelve_fields_different_caller", twelve, Map.of("worker", "outside"));
        MDC.clear();
    }
}
