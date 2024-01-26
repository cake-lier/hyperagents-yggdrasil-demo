package org.hyperagents.demo;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import org.apache.http.HttpHeaders;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;

public class WorkerClient extends AbstractClient {
    @Override
    public void init(final String agentName, final String mailboxUri, final String platformUri) {
        super.init(agentName, mailboxUri, platformUri);
    }

    @OPERATION
    public void joinProductionWorkspace(final OpFeedbackParam<Model> agentBody) {
        agentBody.set(this.parseFromTurtle(this.doRequest(
            this.getClient()
                .post(this.getPlatformUri() + "/workspaces/production/join")
                .putHeader("X-Agent-WebID", this.getAgentId())
                .send()
        )));
    }

    @OPERATION
    public void searchForRoboticArm(final Model agentBody, final OpFeedbackParam<ThingDescription> robotArm) {
        final var queryResult = this.doRequest(
            this.getClient()
                .post(this.getPlatformUri() + "/query")
                .putHeader(HttpHeaders.ACCEPT, "text/turtle")
                .sendBuffer(Buffer.buffer(String.format(
                    """
                    PREFIX td: <https://www.w3.org/2019/wot/td#>
                    PREFIX hmas: <https://purl.org/hmas/>
                    PREFIX schema: <https://schema.org/>
                    PREFIX ex: <https://example.org/>
                    
                    DESCRIBE ?artifact
                    WHERE {
                        ?artifact hmas:containedIn [
                            a hmas:Workspace;
                            td:title "production";
                        ];
                        a hmas:Artifact, ex:RobotArm;
                        ^schema:owns %s.
                    }
                    """,
                    this.getAgentId()
                )))
        );
        final var bodyIri = agentBody.filter(null, RDF.TYPE, Values.iri("https://example.org/Body"))
                                     .subjects()
                                     .stream()
                                     .findFirst()
                                     .orElseThrow();
        agentBody.addAll(this.parseFromTurtle(String.format(
            """
            @prefix hmas: <https://purl.org/hmas/core/> .
            @prefix td: <https://www.w3.org/2019/wot/td#> .
            @prefix htv: <http://www.w3.org/2011/http#> .
            @prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
            @prefix js: <https://www.w3.org/2019/wot/json-schema#> .
                  
            <%s> td:hasActionAffordance [
                a td:ActionAffordance, <http://example.org/kqml#RequestTell>;
                td:hasForm [
                    htv:methodName "POST";
                    hctl:hasTarget <%s>;
                    hctl:forContentType "application/json";
                    hctl:hasOperationType td:invokeAction
                ];
                td:hasInputSchema [
                    a js:ObjectSchema;
                    js:properties [
                        a js:StringSchema, <http://example.org/kqml#Performative>;
                        js:propertyName "performative";
                        js:enum "tell";
                    ], [
                        a js:StringSchema, hmas:Agent;
                        js:propertyName "sender";
                    ], [
                        a js:StringSchema, hmas:Agent;
                        js:propertyName "receiver";
                        js:enum "%s";
                    ], [
                        a js:StringSchema, <http://example.org/kqml#PropositionalContent>;
                        js:propertyName "content";
                    ];
                    js:required "performative", "sender", "receiver", "content";
                ];
            ].
            """,
            bodyIri.stringValue(),
            this.getMailboxUri() + "/inbox",
            this.getAgentId()
        )));
        if (!this.parseFromTurtle(queryResult).isEmpty()) {
            robotArm.set(TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, queryResult));
            agentBody.addAll(this.parseFromTurtle(String.format(
                """
                @prefix hmas: <https://purl.org/hmas/core/> .
                @prefix td: <https://www.w3.org/2019/wot/td#> .
                @prefix htv: <http://www.w3.org/2011/http#> .
                @prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .
                @prefix js: <https://www.w3.org/2019/wot/json-schema#> .
                      
                <%s> td:hasActionAffordance [
                    a td:ActionAffordance, <https://example.org/kqml#RequestAchieve>;
                    td:hasForm [
                        htv:methodName "POST";
                        hctl:hasTarget <%s>;
                        hctl:forContentType "application/json";
                        hctl:hasOperationType td:invokeAction
                    ];
                    td:hasInputSchema [
                        a js:ObjectSchema;
                        js:properties [
                            a js:StringSchema, <https://example.org/kqml#Performative>;
                            js:propertyName "performative";
                            js:enum "achieve";
                        ], [
                            a js:StringSchema, hmas:Agent;
                            js:propertyName "sender";
                        ], [
                            a js:StringSchema, hmas:Agent;
                            js:propertyName "receiver";
                            js:enum "%s";
                        ], [
                            a js:ObjectSchema, <https://example.org/kqml#PropositionalContent>;
                            js:propertyName "content";
                            js:properties [
                                a js:StringSchema;
                                js:propertyName "goal";
                                js:enum "move_cup";
                            ], [
                                a js:StringSchema;
                                js:propertyName "from";
                            ], [
                                a js:StringSchema;
                                js:propertyName "to";
                            ];
                            js:required "goal", "from", "to";
                        ];
                        js:required "performative", "sender", "receiver", "content";
                    ];
                ].
                """,
                bodyIri.stringValue(),
                this.getMailboxUri() + "/inbox",
                this.getAgentId()
            )));
        }
        this.doRequest(
            this.getClient()
                .put(bodyIri.stringValue())
                .putHeader("X-Agent-WebID", this.getAgentId())
                .sendBuffer(Buffer.buffer(this.parseToTurtle(agentBody)))
        );
    }

    @OPERATION
    public void useRoboticArm(final ThingDescription robotArm, final String from, final String to) {
        robotArm.getFirstActionBySemanticType("https://example.org/Move")
                .flatMap(ActionAffordance::getFirstForm)
                .ifPresentOrElse(
                    f -> this.doRequest(
                        this.getClient()
                            .request(HttpMethod.valueOf(f.getMethodName().orElse("POST")), f.getTarget())
                            .putHeader("X-Agent-WebID", this.getAgentId())
                            .sendJson(JsonArray.of(from, to))
                    ),
                    () -> this.failed("Request for using robotic arm failed")
                );
    }
}
