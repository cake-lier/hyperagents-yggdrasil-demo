package org.hyperagents.demo;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SupervisorClient extends AbstractClient {
    @Override
    public void init(final String agentName, final String messageBoxUri, final String platformHost, final int platformPort) {
        super.init(agentName, messageBoxUri, platformHost, platformPort);
    }

    @OPERATION
    public void searchCupMoverWorkers(final OpFeedbackParam<Map<String, ThingDescription>> workerBodies) {
        workerBodies.set(
            JsonObject
                .mapFrom(Json.decodeValue(this.doRequest(
                    this.getClient()
                        .post(this.getPlatformPort(), this.getPlatformHost(), "/query")
                        .putHeader(HttpHeaders.CONTENT_TYPE, "application/sparql-query")
                        .putHeader(HttpHeaders.ACCEPT, "application/sparql-results+json")
                        .sendBuffer(Buffer.buffer(
                            """
                            PREFIX td: <https://www.w3.org/2019/wot/td#>
                            PREFIX hmas: <https://purl.org/hmas/>
                            PREFIX js: <https://www.w3.org/2019/wot/json-schema#>
                            PREFIX ex: <https://example.org/>
                            PREFIX kqml: <https://example.org/kqml#>
                            
                            SELECT DISTINCT ?worker ?name
                            WHERE {
                                ?worker a hmas:Artifact, ex:Body;
                                td:title ?name;
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
                )))
                .getJsonObject("results")
                .getJsonArray("bindings")
                .stream()
                .map(JsonObject::mapFrom)
                .map(o -> Map.entry(
                    o.getJsonObject("name").getString("value"),
                    TDGraphReader.readFromString(
                        ThingDescription.TDFormat.RDF_TURTLE,
                        this.doRequest(
                            this.getClient()
                                .getAbs(o.getJsonObject("worker").getString("value"))
                                .putHeader("X-Agent-WebID", this.getAgentId())
                                .send()
                        )
                    )
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    @OPERATION
    public void focusWorkerBody(final String workerName, final ThingDescription workerBody) {
        this.doRequest(
            this.getClient()
                .post(this.getPlatformPort(), this.getPlatformHost(), "/hub/")
                .putHeader("X-Agent-WebID", this.getAgentId())
                .putHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .sendJsonObject(JsonObject.of(
                    "hub.mode",
                    "subscribe",
                    "hub.topic",
                    workerBody.getThingURI().orElseThrow(),
                    "hub.callback",
                    this.getMessageBoxUri() + "/actions/" + workerName
                ))
        );
    }

    @OPERATION
    public void assignMoveCupTask(final ThingDescription workerBody, final String from, final String to) {
        final var moveCupAction =
            workerBody.getFirstActionBySemanticType("https://example.org/kqml#RequestAchieve")
                      .orElseThrow();
        final var moveCupActionForm = moveCupAction.getFirstForm().orElseThrow();
        this.doRequest(
            this.getClient()
                .requestAbs(
                    HttpMethod.valueOf(moveCupActionForm.getMethodName().orElse("POST")),
                    moveCupActionForm.getTarget()
                )
                .putHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
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
        final var tellActionForm = tellAction.getFirstForm().orElseThrow();
        this.doRequest(
            this.getClient()
                .requestAbs(
                    HttpMethod.valueOf(tellActionForm.getMethodName().orElse("POST")),
                    tellActionForm.getTarget()
                )
                .putHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .sendJsonObject(JsonObject.of(
                    "performative",
                    "tell",
                    "sender",
                    this.getAgentId(),
                    "receiver",
                    this.getReceiver(tellAction),
                    "content",
                    "scold"
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
