package org.hyperagents.demo;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;

public class WorkerClient extends AbstractClient {
    @Override
    public void init(final String agentName, final String messageBoxUri, final String platformHost, final int platformPort) {
        super.init(agentName, messageBoxUri, platformHost, platformPort);
    }

    @OPERATION
    public void joinProductionWorkspace(final OpFeedbackParam<Model> agentBody) {
        agentBody.set(this.parseFromTurtle(this.doRequest(
            this.getClient()
                .post(this.getPlatformPort(), this.getPlatformHost(), "/workspaces/production/join")
                .putHeader("X-Agent-WebID", this.getAgentId())
                .putHeader("X-Agent-LocalName", this.getAgentName())
                .send()
        )));
    }

    @OPERATION
    public void searchForRoboticArm(final Model agentBody, final OpFeedbackParam<ThingDescription> robotArm) {
        final var queryResult = this.doRequest(
            this.getClient()
                .post(this.getPlatformPort(), this.getPlatformHost(), "/query")
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/sparql-query")
                .putHeader(HttpHeaders.ACCEPT, "text/turtle")
                .sendBuffer(Buffer.buffer(String.format(
                    """
                    PREFIX td: <https://www.w3.org/2019/wot/td#>
                    PREFIX hmas: <https://purl.org/hmas/>
                    PREFIX schema: <https://schema.org/>
                    PREFIX ex: <https://example.org/>
                    
                    DESCRIBE ?artifact
                    WHERE {
                        ?artifact hmas:isContainedIn [
                            a hmas:Workspace;
                            td:title "production";
                        ];
                        a hmas:Artifact, ex:RobotArm;
                        ^schema:owns <%s>.
                    }
                    """,
                    this.getAgentId()
                )))
        );
        final var bodyIri = agentBody.filter(null, RDF.TYPE, Values.iri("https://purl.org/hmas/jacamo/Body"))
                                     .subjects()
                                     .stream()
                                     .findFirst()
                                     .orElseThrow();
        agentBody.addAll(this.parseFromTurtle(String.format(
            """
            @prefix hmas: <https://purl.org/hmas/>.
            @prefix td: <https://www.w3.org/2019/wot/td#>.
            @prefix htv: <http://www.w3.org/2011/http#>.
            @prefix hctl: <https://www.w3.org/2019/wot/hypermedia#>.
            @prefix js: <https://www.w3.org/2019/wot/json-schema#>.
            
            <%s> td:hasActionAffordance [
                a td:ActionAffordance, <https://example.org/kqml#RequestTell>;
                td:title "tell";
                td:name "tell";
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
                        js:enum "tell";
                    ], [
                        a js:StringSchema, hmas:Agent;
                        js:propertyName "sender";
                    ], [
                        a js:StringSchema, hmas:Agent;
                        js:propertyName "receiver";
                        js:enum "%s";
                    ], [
                        a js:StringSchema, <https://example.org/kqml#PropositionalContent>;
                        js:propertyName "content";
                    ];
                    js:required "performative", "sender", "receiver", "content";
                ];
            ].
            """,
            bodyIri.stringValue(),
            this.getMessageBoxUri() + "/inbox",
            this.getAgentId()
        )));
        if (!this.parseFromTurtle(queryResult).isEmpty()) {
            robotArm.set(TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, queryResult));
            agentBody.addAll(this.parseFromTurtle(String.format(
                """
                @prefix hmas: <https://purl.org/hmas/>.
                @prefix td: <https://www.w3.org/2019/wot/td#>.
                @prefix htv: <http://www.w3.org/2011/http#>.
                @prefix hctl: <https://www.w3.org/2019/wot/hypermedia#>.
                @prefix js: <https://www.w3.org/2019/wot/json-schema#>.
                
                <%s> td:hasActionAffordance [
                    a td:ActionAffordance, <https://example.org/kqml#RequestAchieve>;
                    td:title "achieveMoveCup";
                    td:name "achieveMoveCup";
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
                this.getMessageBoxUri() + "/inbox",
                this.getAgentId()
            )));
        }
        this.doRequest(
            this.getClient()
                .putAbs(bodyIri.stringValue().replaceFirst("/#.*$", ""))
                .putHeader("X-Agent-WebID", this.getAgentId())
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle")
                .sendBuffer(Buffer.buffer(this.parseToTurtle(agentBody)))
        );
    }

    @OPERATION
    public void useRoboticArm(final ThingDescription robotArm, final String to) {
        robotArm.getActionByName(to.equals("warehouse") ? "moveToWarehouse" : "moveToShopFloor")
                .flatMap(ActionAffordance::getFirstForm)
                .ifPresentOrElse(
                    f -> this.doRequest(
                        this.getClient()
                            .requestAbs(HttpMethod.valueOf(f.getMethodName().orElse("POST")), f.getTarget())
                            .putHeader("X-Agent-WebID", this.getAgentId())
                            .putHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                            .send()
                    ),
                    () -> this.failed("Request for using robotic arm failed")
                );
    }
}
