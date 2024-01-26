package org.hyperagents.demo;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.http.HttpHeaders;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SupervisorClient extends AbstractClient {
    @Override
    public void init(final String agentName, final String mailboxUri, final String platformUri) {
        super.init(agentName, mailboxUri, platformUri);
    }

    @OPERATION
    public void searchCupMoverWorkers(final OpFeedbackParam<Map<String, ThingDescription>> workerBodies) {
        final var queryResult = JsonObject.mapFrom(Json.decodeValue(this.doRequest(
            this.getClient()
                .post(this.getPlatformUri() + "/query")
                .putHeader(HttpHeaders.ACCEPT, "application/sparql-results+json")
                .sendBuffer(Buffer.buffer(
                    """
                    PREFIX td: <https://www.w3.org/2019/wot/td#>
                    PREFIX hmas: <https://purl.org/hmas/>
                    PREFIX ex: <https://example.org/>
                    PREFIX kqml: <https://example.org/kqml#>
                    
                    SELECT DISTINCT ?worker
                    WHERE {
                        ?worker a hmas:Artifact, ex:Body;
                        td:hasActionAffordance [
                            a kqml:RequestAchieve;
                            td:hasInputSchema [
                                js:properties [
                                    a kqml:PropositionalContent;
                                    js:properties [
                                        js:propertyName "goal";
                                        js:enum "move_cup";
                                    ];
                                ];
                            ];
                        ].
                    }
                    """
                ))
        )));
        workerBodies.set(
            queryResult.getJsonObject("results")
                       .getJsonArray("bindings")
                       .stream()
                       .map(JsonObject::mapFrom)
                       .map(o -> o.getJsonObject("worker").getString("value"))
                       .map(w -> Map.entry(
                           w,
                           TDGraphReader.readFromString(
                               ThingDescription.TDFormat.RDF_TURTLE,
                               this.doRequest(
                                   this.getClient()
                                       .get(w)
                                       .putHeader("X-Agent-WebID", this.getAgentId())
                                       .send()
                               )
                           )
                       ))
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    @OPERATION
    public void focusWorkerBody(final ThingDescription workerBody) {
        this.doRequest(
            this.getClient()
                .post(this.getPlatformUri() + "/hub/")
                .putHeader("X-Agent-WebID", this.getAgentId())
                .sendJsonObject(JsonObject.of(
                    "hub.mode",
                    "subscribe",
                    "hub.topic",
                    workerBody.getThingURI().orElseThrow(),
                    "hub.callback",
                    this.getMailboxUri() + "/actions/" + workerBody.getTitle()
                ))
        );
    }

    @OPERATION
    public void assignMoveCupTask(final ThingDescription workerBody, final String from, final String to) {
        final var moveCupAction =
            workerBody.getFirstActionBySemanticType("https://example.org/kqml#RequestAchieve")
                      .orElseThrow();
        this.doRequest(
            this.getClient()
                .post(moveCupAction.getFirstForm().orElseThrow().getTarget())
                .sendJsonObject(JsonObject.of(
                    "performative",
                    "achieve",
                    "sender",
                    this.getAgentId(),
                    "receiver",
                    this.getReceiver(moveCupAction),
                    "content",
                    JsonObject.of(
                        "goal",
                        "move_cup",
                        "from",
                        from,
                        "to",
                        to
                    )
                ))
        );
    }

    @OPERATION
    public void punish(final ThingDescription workerBody) {
        final var tellAction =
            workerBody.getFirstActionBySemanticType("https://example.org/kqml#RequestTell")
                      .orElseThrow();
        this.doRequest(
            this.getClient()
                .post(tellAction.getFirstForm().orElseThrow().getTarget())
                .sendJsonObject(JsonObject.of(
                    "performative",
                    "tell",
                    "sender",
                    this.getAgentId(),
                    "receiver",
                    this.getReceiver(tellAction),
                    "content",
                    "punish"
                ))
        );
    }

    private String getReceiver(final ActionAffordance action) {
        return action.getInputSchema()
                     .stream()
                     .flatMap(d ->
                         (d instanceof ObjectSchema o ? Optional.of(o) : Optional.<ObjectSchema>empty()).stream()
                     )
                     .flatMap(o -> o.getProperty("receiver").stream())
                     .flatMap(d ->
                         (d instanceof StringSchema o ? Optional.of(o) : Optional.<StringSchema>empty()).stream()
                     )
                     .flatMap(s -> s.getEnumeration().stream())
                     .findFirst()
                     .orElseThrow();
    }
}
