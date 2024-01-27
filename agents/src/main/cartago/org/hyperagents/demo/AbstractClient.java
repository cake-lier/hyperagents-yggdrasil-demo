package org.hyperagents.demo;

import cartago.Artifact;
import cartago.GUARD;
import cartago.INTERNAL_OPERATION;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public abstract class AbstractClient extends Artifact {
    private WebClient client;
    private String messageBoxUri;
    private String platformHost;
    private int platformPort;
    private String agentName;
    private boolean requestComplete;

    protected void init(final String agentName, final String messageBoxUri, final String platformHost, final int platformPort) {
        this.client = WebClient.create(Vertx.vertx());
        this.agentName = agentName;
        this.messageBoxUri = messageBoxUri;
        this.platformHost = platformHost;
        this.platformPort = platformPort;
    }

    protected final WebClient getClient() {
        return this.client;
    }

    protected final String getMessageBoxUri() {
        return this.messageBoxUri;
    }

    protected final String getPlatformHost() {
        return this.platformHost;
    }

    protected final int getPlatformPort() {
        return this.platformPort;
    }

    protected final String getAgentId() {
        return "http://" + this.platformHost + ":" + this.platformPort + "/agents/" + this.agentName;
    }

    protected final String doRequest(final Future<HttpResponse<Buffer>> request) {
        this.requestComplete = false;
        request.onComplete(r -> this.execInternalOp("signalResponseReceived"));
        this.await("isRequestComplete");
        if (request.succeeded() && request.result().statusCode() <= 299) {
            return request.result().bodyAsString();
        } else if (!request.succeeded()) {
            this.failed(request.cause().getMessage());
        } else {
            this.failed(request.result().statusMessage());
        }
        throw new RuntimeException();
    }

    protected final Model parseFromTurtle(final String graphString) {
        try (var stringReader = new StringReader(graphString)) {
            final var rdfParser = Rio.createParser(RDFFormat.TURTLE);
            final var model = new LinkedHashModel();
            rdfParser.setRDFHandler(new StatementCollector(model));
            rdfParser.parse(stringReader);
            return model;
        } catch (final RDFParseException | RDFHandlerException | IOException e) {
            this.failed(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected final String parseToTurtle(final Model graph) {
        try (var out = new ByteArrayOutputStream()) {
            final var writer = Rio.createWriter(RDFFormat.TURTLE, out);
            writer.getWriterConfig()
                  .set(BasicWriterSettings.PRETTY_PRINT, true)
                  .set(BasicWriterSettings.RDF_LANGSTRING_TO_LANG_LITERAL, true)
                  .set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true)
                  .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            try {
                writer.startRDF();
                writer.handleNamespace("hmas", "https://purl.org/hmas/core/");
                writer.handleNamespace("td", "https://www.w3.org/2019/wot/td#");
                writer.handleNamespace("htv", "http://www.w3.org/2011/http#");
                writer.handleNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#");
                writer.handleNamespace("wotsec", "https://www.w3.org/2019/wot/security#");
                writer.handleNamespace("dct", "http://purl.org/dc/terms/");
                writer.handleNamespace("js", "https://www.w3.org/2019/wot/json-schema#");
                writer.handleNamespace("saref", "https://w3id.org/saref#");
                writer.handleNamespace("schema", "https://schema.org/");
                graph.forEach(writer::handleStatement);
                writer.endRDF();
            } catch (final RDFHandlerException e) {
                this.failed(e.getMessage());
                throw new RuntimeException(e);
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (final IOException e) {
            this.failed(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @INTERNAL_OPERATION
    private void signalResponseReceived() {
        this.requestComplete = true;
    }

    @GUARD
    private boolean isRequestComplete() {
        return this.requestComplete;
    }
}
