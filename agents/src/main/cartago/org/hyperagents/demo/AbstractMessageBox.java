package org.hyperagents.demo;

import cartago.Artifact;
import cartago.GUARD;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;
import cartago.Tuple;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public abstract class AbstractMessageBox extends Artifact {
    private Queue<Tuple> signals;

    protected void init(final int port) throws InterruptedException {
        this.signals = new LinkedList<>();
        final var vertx = Vertx.vertx();
        final var router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        this.addRoutes(router);
        final var semaphore = new Semaphore(0);
        vertx.createHttpServer()
             .requestHandler(router)
             .listen(port)
             .onComplete(r -> {
                if (r.failed()) {
                    this.log(r.cause().getMessage());
                }
                semaphore.release();
             });
        semaphore.acquire();
    }

    protected abstract void addRoutes(final Router router);

    @OPERATION
    public void resolveNextSignal() {
        this.await("isSignalAvailable");
        final var nextSignal = this.signals.remove();
        this.signal(nextSignal.getLabel(), nextSignal.getContents());
    }

    @INTERNAL_OPERATION
    protected void addSignal(final Tuple signal) {
        this.signals.add(signal);
    }

    @GUARD
    private boolean isSignalAvailable() {
        return !this.signals.isEmpty();
    }
}
