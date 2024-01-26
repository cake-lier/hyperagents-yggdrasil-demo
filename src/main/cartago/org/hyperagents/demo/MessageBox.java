package org.hyperagents.demo;

import cartago.Artifact;
import cartago.GUARD;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;
import cartago.Tuple;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;

public class MessageBox extends Artifact {
    private Queue<Tuple> signals;

    private void init(final int port) {
        this.signals = new LinkedList<>();
        final var vertx = Vertx.vertx();
        final var webServer = vertx.createHttpServer();
        webServer.listen(port);
        final var router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/inbox").handler(ctx -> {
            final var body = ctx.body().asJsonObject();
            if (body.getString("performative").equals("achieve")) {
                final var content = body.getJsonObject("content");
                this.execInternalOp(
                    "addSignal",
                    new Tuple(
                        content.getString("goal"),
                        content.getString("from"),
                        content.getString("to")
                    )
                );
            } else if (body.getString("performative").equals("tell")) {
                this.execInternalOp("addSignal", new Tuple(body.getString("content")));
            }
        });
        router.post("/actions/:agentName").handler(ctx -> this.execInternalOp(
            "addSignal",
            new Tuple(ctx.body().asJsonObject().getString("actionName"), ctx.pathParam("agentName"))
        ));
        webServer.requestHandler(router);
    }

    @OPERATION
    public void resolveNextSignal() {
        this.await("isSignalAvailable");
        final var nextSignal = this.signals.remove();
        this.signal(nextSignal.getLabel(), nextSignal.getContents());
    }

    @INTERNAL_OPERATION
    private void addSignal(final Tuple signal) {
        this.signals.add(signal);
    }

    @GUARD
    private boolean isSignalAvailable() {
        return !this.signals.isEmpty();
    }
}
