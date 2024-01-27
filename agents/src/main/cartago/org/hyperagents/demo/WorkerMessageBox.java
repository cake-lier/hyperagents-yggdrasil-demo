package org.hyperagents.demo;

import cartago.Tuple;
import io.vertx.ext.web.Router;

public class WorkerMessageBox extends AbstractMessageBox {
    @Override
    public void init(final int port) throws InterruptedException {
        super.init(port);
    }

    @Override
    protected void addRoutes(final Router router) {
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
            ctx.response().send();
        });
    }
}
