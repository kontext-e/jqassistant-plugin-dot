package de.kontext_e.jqassistant.plugin.dot.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import de.kontext_e.jqassistant.plugin.dot.store.descriptor.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

@ScannerPlugin.Requires(FileDescriptor.class)
public class DotScannerPlugin extends AbstractScannerPlugin<FileResource, DotDescriptor>  {
    private static final Logger LOGGER = LoggerFactory.getLogger(DotScannerPlugin.class);

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) {
        final boolean accepted = path != null &&
                (
                        path.toLowerCase().endsWith(".dot")
                                || path.toLowerCase().endsWith(".gv")
                )
                ;
        if (accepted) {
            LOGGER.debug("Dot Scanner Plugin accepted path " + path);
        }

        return accepted;
    }

    @Override
    public DotDescriptor scan(final FileResource file, String path, Scope scope, Scanner scanner) throws IOException {
        LOGGER.info("Dot Scanner Plugin scans file "+path);
        FileDescriptor fileDescriptor = scanner.getContext().getCurrentDescriptor();
        final Store store = scanner.getContext().getStore();
        final DotFileDescriptor checkstyleReportDescriptor = store.addDescriptorType(fileDescriptor, DotFileDescriptor.class);
        readStream(store, checkstyleReportDescriptor, file.createStream());
        return checkstyleReportDescriptor;
    }

    void readStream(Store store, DotFileDescriptor dotFileDescriptor, InputStream inputStream) throws IOException {
        CharStream cs = CharStreams.fromStream(inputStream);
        DOTLexer markupLexer = new DOTLexer(cs);
        CommonTokenStream commonTokenStream = new CommonTokenStream(markupLexer);
        DOTParser markupParser = new DOTParser(commonTokenStream);
        DOTGraphVisitor visitor = new DOTGraphVisitor(store);
        visitor.visit(markupParser.graph());
    }

/*
    void readStream(Store store, DotFileDescriptor dotFileDescriptor, InputStream inputStream) throws IOException {
        Graph graph = DOTMarshaller.importGraph(inputStream);
        importGraph(store, dotFileDescriptor, graph);
    }

    void importGraph(Store store, HasGraph graphParent, Graph graph) {
        final DotGraphDescriptor dotGraphDescriptor = store.create(DotGraphDescriptor.class);
        dotGraphDescriptor.setStrict(graph.isStrict());
        dotGraphDescriptor.setType(graph.getGraphType().name());
        dotGraphDescriptor.setDotId(graph.getId());
        importAttributes(graph.getAnonymousAttributes(), dotGraphDescriptor);
        importAttributes(graph.getNodeAttributes(), dotGraphDescriptor);
        importAttributes(graph.getEdgeAttributes(), dotGraphDescriptor);
        importAttributes(graph.getGraphAttributes(), dotGraphDescriptor);
        graphParent.getGraphs().add(dotGraphDescriptor);

        final Map<String, DotNodeDescriptor> nodeDescriptors = new HashMap<>();

        importNodes(store, graph, dotGraphDescriptor, nodeDescriptors);
        importEdges(store, graph, nodeDescriptors);
        importSubGraphs(store, graph, dotGraphDescriptor);
    }

    private static void importNodes(Store store, Graph graph, DotGraphDescriptor dotGraphDescriptor, Map<String, DotNodeDescriptor> nodeDescriptors) {
        final Map<String, Node> nodes = graph.getNodes();
        for (Node node : nodes.values()) {
            final DotNodeDescriptor dotNodeDescriptor = store.create(DotNodeDescriptor.class);
            dotNodeDescriptor.setDotId(node.getId());
            dotGraphDescriptor.getNodes().add(dotNodeDescriptor);
            nodeDescriptors.put(node.getId(), dotNodeDescriptor);
            importAttributes(node.getAttributes(), dotNodeDescriptor);
        }
    }

    private static void importEdges(Store store, Graph graph, Map<String, DotNodeDescriptor> nodeDescriptors) {
        final List<Edge> edges = graph.getEdges();
        for (Edge edge : edges) {
            final EdgeConnectionPoint from = edge.getFrom();
            final EdgeConnectionPoint to = edge.getTo();
            final NodeId fromNodeId = from.getNodeId();
            if (fromNodeId != null) {
                DotNodeDescriptor fromDescriptor = nodeDescriptors.get(fromNodeId.getId());
                if (fromDescriptor != null) {
                    final NodeId toNodeId = to.getNodeId();
                    if (toNodeId != null) {
                        DotNodeDescriptor toDescriptor = nodeDescriptors.get(toNodeId.getId());
                        if (toDescriptor != null) {
                            final DotRelationDescriptor dotRelationDescriptor = store.create(fromDescriptor, DotRelationDescriptor.class, toDescriptor);
                            importAttributes(edge.getAttributes(), dotRelationDescriptor);
                            final Attributes attributes = edge.getAttributes();
                            for (Attribute attribute : attributes.getAttributes()) {
                                dotRelationDescriptor.setAttribute(attribute.getLhs(), attribute.getRhs(), attributes.getAttributeType().name());
                            }
                        }
                    }
                }
            }
        }
    }

    private void importSubGraphs(Store store, Graph graph, DotGraphDescriptor dotGraphDescriptor) {
        final List<Graph> subGraphs = graph.getSubGraphs();
        for (Graph subGraph : subGraphs) {
            importGraph(store, dotGraphDescriptor, subGraph);
        }
    }

    private static void importAttributes(Attributes attributes, AttributesContainer attributeDescriptors) {
        if(attributes == null) throw new IllegalArgumentException("attributes is null");
        if(attributeDescriptors == null) throw new IllegalArgumentException("attributeDescriptors is null");
        final List<Attribute> attributeList = attributes.getAttributes();
        for (Attribute attribute : attributeList) {
            if(attribute == null) throw new IllegalStateException("attribute should not be null");
            attributeDescriptors.setAttribute(attribute.getLhs(), attribute.getRhs(), attributes.getAttributeType().name());
        }
    }
*/
}
