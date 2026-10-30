package com.networknt.client.http;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class RequestSubmissionTest {
    @Test void completionPreservesCancellationBeforeAdmission() {
        RequestSubmission submission = new RequestSubmission();
        IOException reason = new IOException("timeout");
        assertFalse(submission.cancel(reason));
        submission.finish();
        assertTrue(submission.isCancelledBeforeSend());
        assertFalse(submission.start());
        AtomicReference<Throwable> observed = new AtomicReference<>();
        submission.onCancellation(observed::set);
        assertSame(reason, observed.get(), "late-created exchanges must observe cancellation");
    }
    @Test void completionPreservesCancellationAfterAdmission() {
        RequestSubmission submission = new RequestSubmission();
        assertTrue(submission.start());
        IOException reason = new IOException("timeout");
        assertTrue(submission.cancel(reason));
        submission.finish();
        assertFalse(submission.cancel());
        AtomicReference<Throwable> observed = new AtomicReference<>();
        submission.onCancellation(observed::set);
        assertSame(reason, observed.get());
    }
    @Test void registrationCancellationAndCompletionCanRace() throws Exception {
        for (int i = 0; i < 100; i++) {
            RequestSubmission submission = new RequestSubmission();
            assertTrue(submission.start());
            AtomicInteger notifications = new AtomicInteger();
            CompletableFuture<Void> gate = new CompletableFuture<>();
            CompletableFuture<Boolean> cancel = gate.thenApplyAsync(ignored -> submission.cancel());
            CompletableFuture<Void> register = gate.thenRunAsync(() -> submission.onCancellation(error -> {
                assertFalse(Thread.holdsLock(submission), "native cleanup must run outside the submission monitor");
                notifications.incrementAndGet();
            }));
            CompletableFuture<Void> finish = gate.thenRunAsync(submission::finish);
            gate.complete(null);
            CompletableFuture.allOf(cancel, register, finish).get(5, TimeUnit.SECONDS);
            assertEquals(cancel.get() ? 1 : 0, notifications.get());
        }
    }
    @Test void finishedExchangeCannotBeCancelled() {
        RequestSubmission submission = new RequestSubmission();
        assertTrue(submission.start());
        submission.finish();
        assertFalse(submission.cancel());
        AtomicReference<Throwable> observed = new AtomicReference<>();
        submission.onCancellation(observed::set);
        assertNull(observed.get());
    }
}
