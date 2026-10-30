package com.networknt.client.http;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
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
