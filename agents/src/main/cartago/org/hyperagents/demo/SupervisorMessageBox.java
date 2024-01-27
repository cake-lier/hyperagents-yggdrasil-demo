package org.hyperagents.demo;

import cartago.Tuple;
import io.vertx.ext.web.Router;

public class SupervisorMessageBox extends AbstractMessageBox {
    @Override
    public void init(final int port) throws InterruptedException {
        super.init(port);
    }

    @Override
    protected void addRoutes(final Router router) {
        router.post("/actions/:agentName").handler(ctx -> {
            final var body = ctx.body().asJsonObject();
            this.execInternalOp(
                "addSignal",
                new Tuple(
                    body.getString("actionName"),
                    body.getString("eventType"),
                    ctx.pathParam("agentName")
                )
            );
            ctx.response().send();
        });
    }
}
