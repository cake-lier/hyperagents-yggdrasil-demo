package org.hyperagents.demo;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.cartago.artifacts.HypermediaArtifact;

public class RobotArm extends HypermediaArtifact {
    public void init(final String ownerName) {
        final var metadata = new LinkedHashModel();
        metadata.add(
            Values.iri(this.getBaseUri().toString() + "/agents/" + ownerName),
            Values.iri("https://schema.org/owns"),
            Values.iri(this.getArtifactUri())
        );
        this.addMetadata(metadata);
        HypermediaArtifactRegistry.getInstance().register(this);
    }

    @OPERATION
    public void moveToWarehouse() {
        this.log("Moved arm to warehouse");
    }

    @OPERATION
    public void moveToShopFloor() {
        this.log("Moved arm to shop floor");
    }

    @Override
    protected void registerInteractionAffordances() {
        this.registerActionAffordance(
            "https://example.org/Move",
            "moveToWarehouse",
            "/moveToWarehouse"
        );
        this.registerActionAffordance(
            "https://example.org/Move",
            "moveToShopFloor",
            "/moveToShopFloor"
        );
    }
}
