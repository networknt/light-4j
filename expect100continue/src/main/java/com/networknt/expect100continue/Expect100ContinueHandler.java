package com.networknt.expect100continue;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.*;
import io.undertow.server.protocol.http.HttpContinue;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.conduits.AbstractStreamSourceConduit;
import org.xnio.conduits.StreamSourceConduit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;


public class Expect100ContinueHandler implements MiddlewareHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Expect100ContinueHandler.class);
    private static final Expect100ContinueConfig CONFIG = Expect100ContinueConfig.load();
    private volatile HttpHandler next;
    private static final ContinueResponseCommitListener CONTINUE_RESPONSE_COMMIT_LISTENER = new ContinueResponseCommitListener();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        LOG.trace("Expect100ContinueHandler starts.");

        if (HttpContinue.requiresContinueResponse(exchange)) {
            LOG.debug("Expect header detected in request.");
            final var shouldIgnore100Continue = this.shouldIgnore100Continue(exchange);
            final var shouldRespondInPlace = this.shouldRespondInPlace(exchange);

            if (!shouldIgnore100Continue && !shouldRespondInPlace) {
                LOG.debug("Expect header detected in request. Adding request wrapper and response commit listener.");
                exchange.addRequestWrapper(CONTINUE_REQUEST_WRAPPER);
                exchange.addResponseCommitListener(CONTINUE_RESPONSE_COMMIT_LISTENER);

            } else if (shouldRespondInPlace) {

                LOG.debug("Expect header detected in request, and path is configured to respond in place. Sending 100 Continue response.");
                HttpContinue.sendContinueResponse(exchange, new IoCallback() {
                    @Override
                    public void onComplete(final HttpServerExchange exchange, final Sender sender) {
                        LOG.debug("In place 100 Continue response sent successfully. Removing Expect header before continuing.");
                        exchange.getRequestHeaders().remove(Headers.EXPECT);
                    }

                    @Override
                    public void onException(final HttpServerExchange exchange, final Sender sender, final IOException e) {
                        LOG.error("Failed to send 100 Continue response.", e);
                    }
                });

            } else {
                LOG.debug("Expect header detected in request, but path is ignored. Removing Expect header before continuing.");
                exchange.getRequestHeaders().remove(Headers.EXPECT);
            }

        }

        LOG.trace("Expect100ContinueHandler ends.");
        Handler.next(exchange, this.next);
    }

    private boolean shouldRespondInPlace(final HttpServerExchange exchange) {
        return CONFIG.getInPlacePathPrefixes().stream().anyMatch(exchange.getRequestPath()::startsWith);
    }

    private boolean shouldIgnore100Continue(final HttpServerExchange exchange) {
        return CONFIG.getIgnoredPathPrefixes().stream().anyMatch(exchange.getRequestPath()::startsWith);
    }

    @Override
    public void reload() {
        CONFIG.reload();
        this.register();
    }

    @Override
    public HttpHandler getNext() {
        return this.next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return CONFIG.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(
                Expect100ContinueConfig.CONFIG_NAME,
                Expect100ContinueHandler.class.getName(),
                Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(Expect100ContinueConfig.CONFIG_NAME),
                null
        );
    }

    private static final class ContinueResponseCommitListener implements ResponseCommitListener {

        @Override
        public void beforeCommit(final HttpServerExchange exchange) {
            if (HttpContinue.isContinueResponseSent(exchange)) {
                exchange.setPersistent(false);

                // If the request is not complete, terminate the request channel.
                if (!exchange.isRequestComplete())
                    exchange.getConnection().terminateRequestChannel(exchange);

                else Connectors.terminateRequest(exchange);
            }
        }
    }

    private static final ConduitWrapper<StreamSourceConduit> CONTINUE_REQUEST_WRAPPER = (factory, exchange) -> {

        if (exchange.isRequestChannelAvailable() && !exchange.isResponseStarted())
            return new Expect100ContinueConduit(factory.create(), exchange);

        return factory.create();
    };

    private static final class Expect100ContinueConduit extends AbstractStreamSourceConduit<StreamSourceConduit> implements StreamSourceConduit {

        private boolean sent = false;
        private HttpContinue.ContinueResponseSender response = null;
        private final HttpServerExchange exchange;


        private Expect100ContinueConduit(final StreamSourceConduit next, final HttpServerExchange exchange) {
            super(next);
            this.exchange = exchange;
        }

        @Override
        public long transferTo(final long position, final long count, final FileChannel target) throws IOException {

            if (this.exchange.getStatusCode() == StatusCodes.EXPECTATION_FAILED) {
                Connectors.terminateRequest(this.exchange);
                return -1;
            }

            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }

            if (this.response != null) {

                if (!this.response.send())
                    return 0;

                this.response = null;
            }

            return super.transferTo(position, count, target);
        }

        @Override
        public long transferTo(final long count, final ByteBuffer throughBuffer, final StreamSinkChannel target) throws IOException {

            if (this.exchange.getStatusCode() == StatusCodes.EXPECTATION_FAILED) {
                Connectors.terminateRequest(this.exchange);
                return -1;
            }

            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }

            if (this.response != null) {

                if (!this.response.send())
                    return 0;

                this.response = null;
            }

            return super.transferTo(count, throughBuffer, target);
        }

        @Override
        public int read(final ByteBuffer dst) throws IOException {

            if (this.exchange.getStatusCode() == StatusCodes.EXPECTATION_FAILED) {
                Connectors.terminateRequest(this.exchange);
                return -1;
            }

            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }

            if (this.response != null) {

                if (!this.response.send())
                    return 0;

                this.response = null;
            }
            return super.read(dst);
        }

        @Override
        public long read(final ByteBuffer[] dsts, final int offs, final int len) throws IOException {

            if (this.exchange.getStatusCode() == StatusCodes.EXPECTATION_FAILED) {
                Connectors.terminateRequest(this.exchange);
                return -1;
            }

            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }

            if (this.response != null) {

                if (!this.response.send())
                    return 0;

                this.response = null;
            }
            return super.read(dsts, offs, len);
        }

        @Override
        public void awaitReadable(final long time, final TimeUnit timeUnit) throws IOException {

            if (this.exchange.getStatusCode() == StatusCodes.EXPECTATION_FAILED)
                return;

            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }

            long exitTime = System.currentTimeMillis() + timeUnit.toMillis(time);

            if (this.response != null) {

                while (!this.response.send()) {
                    long currentTime = System.currentTimeMillis();

                    if (currentTime > exitTime)
                        return;

                    this.response.awaitWritable(exitTime - currentTime, TimeUnit.MILLISECONDS);
                }
                this.response = null;
            }

            long currentTime = System.currentTimeMillis();
            super.awaitReadable(exitTime - currentTime, TimeUnit.MILLISECONDS);
        }

        @Override
        public void awaitReadable() throws IOException {

            if (this.exchange.getStatusCode() == StatusCodes.EXPECTATION_FAILED)
                return;

            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }

            if (this.response != null) {

                while (!this.response.send())
                    this.response.awaitWritable();

                this.response = null;
            }
            super.awaitReadable();
        }
    }


}