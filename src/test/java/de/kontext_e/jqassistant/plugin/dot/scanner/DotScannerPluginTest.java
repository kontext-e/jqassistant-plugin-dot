package de.kontext_e.jqassistant.plugin.dot.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.dot.store.descriptor.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class DotScannerPluginTest {
    private final Store mockStore = mock(Store.class);
    private final DotFileDescriptor mockDotFileDescriptor = mock(DotFileDescriptor.class);
    private final DotRelationDescriptor mockDotRelationshipDescriptor = mock(DotRelationDescriptor.class);
    private final DotGraphDescriptor mockDotGraphDescriptor = mock(DotGraphDescriptor.class);
    private final Set<DotGraphDescriptor> fileGraphs = new HashSet<>();
    private DotScannerPlugin plugin;
    private final Set<DotNodeDescriptor> nodes = new HashSet<>();

    @BeforeEach
    public void setUp() {
        when(mockStore.create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class))).thenReturn(mockDotRelationshipDescriptor);
        when(mockStore.create(DotGraphDescriptor.class)).thenReturn(mockDotGraphDescriptor);
        when(mockDotFileDescriptor.getGraphs()).thenReturn(fileGraphs);
        plugin = new DotScannerPlugin();
        fileGraphs.clear();
        nodes.clear();
        TransformVisitor.clearNodes();
    }

    class TransformVisitor extends DOTBaseVisitor<String> {
        private static final Map<String, DotNodeDescriptor> nameToNodeMapping = new HashMap<>();
        private final Store store;
        private final HasGraph graphParent;
        private AttributesContainer attributesContainer;
        private DotGraphDescriptor dotGraphDescriptor;
        private String type;

        public static void clearNodes() {
            nameToNodeMapping.clear();
        }

        @Override
        public String visitGraph(DOTParser.GraphContext ctx) {
            String graphType = "illegal graph type";
            if(ctx.GRAPH() != null)
            {
                graphType = "graph";
            }
            if(ctx.DIGRAPH() != null)
            {
                graphType = "digraph";
            }

            dotGraphDescriptor = store.create(DotGraphDescriptor.class);
            attributesContainer = dotGraphDescriptor;
            graphParent.getGraphs().add(dotGraphDescriptor);

            dotGraphDescriptor.setStrict(ctx.STRICT() != null);
            dotGraphDescriptor.setType(graphType);
            dotGraphDescriptor.setDotId(visitId_(ctx.id_()));

            return visitChildren(ctx);
        }

        @Override
        public String visitStmt(DOTParser.StmtContext ctx) {
            if (null != ctx.attr_stmt()) {
                System.out.println("attr stmt: "+ctx.attr_stmt().getText());
            }
            if (null != ctx.id_()) {
                // graph attributes were given as statement ids
                String attributeName = "";
                String attributeValue = "";

                if(ctx.id_(0) != null) {
                    System.out.println("id stmt: "+visitId_(ctx.id_(0)));
                    attributeName = visitId_(ctx.id_(0));
                }
                if(ctx.id_(1) != null) {
                    System.out.println("id stmt: "+ctx.id_(1).getText());
                    attributeValue = visitId_(ctx.id_(1));
                }
                if (attributeName.isEmpty() == false && attributeValue.isEmpty() == false) {
                    attributesContainer.setAttribute(attributeName, attributeValue, "graph");
                }
            }
            return visitChildren(ctx);
        }

        @Override
        public String visitEdge_stmt(DOTParser.Edge_stmtContext ctx) {

            final String fromNodeName = visitNode_id(ctx.node_id());
            System.out.println("edge from: " + fromNodeName);
            DotNodeDescriptor fromNode = nameToNodeMapping.get(fromNodeName);
            if (fromNode == null) {
                System.out.println("  not there yet.");
                createNodeDescriptor(fromNodeName);
                fromNode = nameToNodeMapping.get(fromNodeName);
                if (fromNode == null) {
                    System.out.println("ERROR while creating");
                    return null;
                }
            }

            DOTParser.EdgeRHSContext edgeRHSContext = ctx.edgeRHS();
            final String edgeOp = findEdgeop(edgeRHSContext);
            String toNodeName = visitEdgeRHS(edgeRHSContext);
            System.out.println("edge to  : " + toNodeName);
            DotNodeDescriptor toNode = nameToNodeMapping.get(toNodeName);
            if(toNode == null) {
                System.out.println("  not there yet.");
                createNodeDescriptor(toNodeName);
                toNode = nameToNodeMapping.get(toNodeName);
                if (toNode == null) {
                    System.out.println("ERROR while creating");
                    return null;
                }
            }

            var dotRelationDescriptor = store.create(fromNode, DotRelationDescriptor.class, toNode);
            attributesContainer = dotRelationDescriptor;
            type = "edge";
            if(ctx.attr_list() != null) {
                visitAttr_list(ctx.attr_list());
            }

            if ("--".equals(edgeOp)) {
                dotRelationDescriptor = store.create(toNode, DotRelationDescriptor.class, fromNode);
                attributesContainer = dotRelationDescriptor;
                type = "edge";
                when(mockDotGraphDescriptor.getNodes()).thenReturn(nodes);
                if(ctx.attr_list() != null) {
                    visitAttr_list(ctx.attr_list());
                }
            }

            return null;
        }

        private String findEdgeop(DOTParser.EdgeRHSContext edgeRHSContext) {
            if (edgeRHSContext.edgeop() != null) {
                for (var eo : edgeRHSContext.edgeop()) {
                    String edgeop = visitEdgeop(eo);
                    if(edgeop != null && edgeop.isEmpty() == false) {
                        return edgeop;
                    }
                }
            }
            return "";
        }

        @Override
        public String visitNode_stmt(DOTParser.Node_stmtContext ctx) {
            String nodeId = visitNode_id(ctx.node_id());
            System.out.println("node name: " + nodeId);
            createNodeDescriptor(nodeId);

            if (ctx.node_id().port() != null) {
                // TODO update DotNodeDescriptor to hold ports
            }

            return visitChildren(ctx);
        }

        private void createNodeDescriptor(String nodeId) {
            final DotNodeDescriptor dotNodeDescriptor = store.create(DotNodeDescriptor.class);
            dotNodeDescriptor.setDotId(nodeId);
            dotGraphDescriptor.getNodes().add(dotNodeDescriptor);
            attributesContainer = dotNodeDescriptor;
            nameToNodeMapping.put(nodeId, dotNodeDescriptor);
            type = "node";
        }

        @Override
        public String visitId_(DOTParser.Id_Context ctx) {
            return ctx.getText();
        }

        @Override
        public String visitNode_id(DOTParser.Node_idContext ctx) {
            return ctx.getText();
        }

        @Override
        public String visitAttr_stmt(DOTParser.Attr_stmtContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public String visitAttr_list(DOTParser.Attr_listContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public String visitA_list(DOTParser.A_listContext ctx) {
            if (ctx.id_() != null) {
                for (int i = 0; i < ctx.id_().size(); i += 2) {
                    final String name = visitId_(ctx.id_(i));
                    final String value = visitId_(ctx.id_(i+1));
                    attributesContainer.setAttribute(name, value, type);
                }
            }
            return visitChildren(ctx);
        }

        @Override
        public String visitStmt_list(DOTParser.Stmt_listContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public String visitEdgeRHS(DOTParser.EdgeRHSContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public String visitEdgeop(DOTParser.EdgeopContext ctx) {
            return ctx.getText();
        }

        @Override
        public String visitPort(DOTParser.PortContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public String visitSubgraph(DOTParser.SubgraphContext ctx) {
            return visitChildren(ctx);
        }

        TransformVisitor(Store store, HasGraph graphParent, AttributesContainer attributesContainer) {
            this.store = store;
            this.graphParent = graphParent;
            this.attributesContainer = attributesContainer;
        }
    }

    void readStream(Store store, DotFileDescriptor dotFileDescriptor, InputStream inputStream) throws IOException {
        CharStream cs = CharStreams.fromStream(inputStream);
        DOTLexer dotLexer = new DOTLexer(cs);
        CommonTokenStream commonTokenStream = new CommonTokenStream(dotLexer);
        DOTParser dotParser = new DOTParser(commonTokenStream);
        TransformVisitor visitor = new TransformVisitor(store, dotFileDescriptor, null);
        visitor.visit(dotParser.graph());
    }

    @Test
    void testEdgeFromNodeToNode() throws IOException {
        var inputStream = streamOf("graph G { a -- b; c -> d }");
        when(mockStore.create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class))).thenReturn(mockDotRelationshipDescriptor);
        when(mockStore.create(DotNodeDescriptor.class)).then(invocation -> mock(DotNodeDescriptor.class));
        when(mockDotGraphDescriptor.getNodes()).thenReturn(nodes);

        readStream(mockStore, mockDotFileDescriptor, inputStream);

        // expect 3 relationships: 1 for the directed and 2 for unidirectional connections
        verify(mockStore, times(3)).create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class));
        assertEquals(4, nodes.size());
    }

    @Test
    void testEdgeFromPredeclaredNodes() throws IOException {
        var inputStream = streamOf("graph G { a;b; a -> b; }");
        when(mockStore.create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class))).thenReturn(mockDotRelationshipDescriptor);
        when(mockStore.create(DotNodeDescriptor.class)).then(invocation -> mock(DotNodeDescriptor.class));
        when(mockDotGraphDescriptor.getNodes()).thenReturn(nodes);

        readStream(mockStore, mockDotFileDescriptor, inputStream);

        verify(mockStore, times(1)).create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class));
        assertEquals(2, nodes.size());
    }

    @Test
    void testEdgeWithAttributes() throws IOException {
        var inputStream = streamOf("graph G { a -> b [att=val]; }");
        when(mockStore.create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class))).thenReturn(mockDotRelationshipDescriptor);
        when(mockStore.create(DotNodeDescriptor.class)).then(invocation -> mock(DotNodeDescriptor.class));
        when(mockDotGraphDescriptor.getNodes()).thenReturn(nodes);

        readStream(mockStore, mockDotFileDescriptor, inputStream);

        verify(mockStore, times(1)).create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class));
        verify(mockDotRelationshipDescriptor).setAttribute("att","val","edge");
        assertEquals(2, nodes.size());
    }

    // work in progress - resolving subgraphs in edges makes the code quite complex
    // is it worth the effort?
    @Test
    void testEdgeWithSubgraph() throws IOException {
        var inputStream = streamOf("graph G { a -- {b c}; }");
        when(mockStore.create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class))).thenReturn(mockDotRelationshipDescriptor);
        when(mockStore.create(DotNodeDescriptor.class)).then(invocation -> mock(DotNodeDescriptor.class));
        when(mockDotGraphDescriptor.getNodes()).thenReturn(nodes);

        readStream(mockStore, mockDotFileDescriptor, inputStream);

        verify(mockStore, times(4)).create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class));
        assertEquals(3, nodes.size());
    }

    @Test
    void testEdgeWithSubgraphsOnBothEnds() throws IOException {
        var inputStream = streamOf("graph G { {d e} -> {f g} }");
        when(mockStore.create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class))).thenReturn(mockDotRelationshipDescriptor);
        when(mockStore.create(DotNodeDescriptor.class)).then(invocation -> mock(DotNodeDescriptor.class));
        when(mockDotGraphDescriptor.getNodes()).thenReturn(nodes);

        readStream(mockStore, mockDotFileDescriptor, inputStream);

        verify(mockStore, times(8)).create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class));
        assertEquals(7, nodes.size());
    }

    @Test
    void testGraphAttributes() throws IOException {
        InputStream inputStream = streamOf("strict graph G { size=\"10,10\"; used=true; }");

        readStream(mockStore, mockDotFileDescriptor, inputStream);

        verify(mockStore).create(DotGraphDescriptor.class);
        verify(mockDotGraphDescriptor).setStrict(true);
        verify(mockDotGraphDescriptor).setType("graph");
        verify(mockDotGraphDescriptor).setDotId("G");
        verify(mockDotGraphDescriptor).setAttribute("size","\"10,10\"","graph");
        verify(mockDotGraphDescriptor).setAttribute("used","true","graph");
        assertEquals(1, fileGraphs.size());
    }

    @Test
    void testNodes() throws IOException {
        var inputStream = streamOf("graph G { n1 [a1=1; a2 = \"str 1\"][a3=true, a4=x; a5 = a1];  }");
        final DotNodeDescriptor dotNodeDescriptor = mock(DotNodeDescriptor.class);
        when(mockStore.create(DotNodeDescriptor.class)).thenReturn(dotNodeDescriptor);
        when(mockDotGraphDescriptor.getNodes()).thenReturn(nodes);

        readStream(mockStore, mockDotFileDescriptor, inputStream);

        verify(dotNodeDescriptor).setDotId("n1");
        verify(dotNodeDescriptor).setAttribute("a1","1","node");
        verify(dotNodeDescriptor).setAttribute("a2","\"str 1\"","node");
        verify(dotNodeDescriptor).setAttribute("a3","true","node");
        verify(dotNodeDescriptor).setAttribute("a4","x","node");
        verify(dotNodeDescriptor).setAttribute("a5","a1","node");
        assertEquals(1, nodes.size());
    }

    @Disabled("Ports not supported yet")
    @Test
    void testNodeWithPort() throws IOException {
        var inputStream = streamOf("graph G { node_with_port:f0:s }");

        readStream(mockStore, mockDotFileDescriptor, inputStream);

    }

    @Test
    void testIllegalGraphType() throws IOException {
        InputStream inputStream = streamOf("strict illegal G { }");
        final DotGraphDescriptor mockDotGraphDescriptor = mock(DotGraphDescriptor.class);
        when(mockStore.create(DotGraphDescriptor.class)).thenReturn(mockDotGraphDescriptor);
        Set<DotGraphDescriptor> fileGraphs = new HashSet<>();
        when(mockDotFileDescriptor.getGraphs()).thenReturn(fileGraphs);

        readStream(mockStore, mockDotFileDescriptor, inputStream);

        verify(mockStore).create(DotGraphDescriptor.class);
        verify(mockDotGraphDescriptor).setStrict(true);
        verify(mockDotGraphDescriptor).setType("illegal graph type");
        verify(mockDotGraphDescriptor).setDotId("illegal");
    }

    @Test
    void testVisitor() throws IOException {
        InputStream inputStream = streamOf("graph G {size=\"10,10\"; used=true; a [shape=circle, label=\"Node A\"]; a -- b; b -- a [weight=1];}");
        readStream(null, null, inputStream);
    }

    @Test
    void testDefaultAttributesForGraphs() throws IOException {
        InputStream inputStream = streamOf("strict graph G { graph [size=\"10,10\"; used=true;}] ");
        final DotGraphDescriptor mockDotGraphDescriptor = mock(DotGraphDescriptor.class);
        when(mockStore.create(DotGraphDescriptor.class)).thenReturn(mockDotGraphDescriptor);
        Set<DotGraphDescriptor> fileGraphs = new HashSet<>();
        when(mockDotFileDescriptor.getGraphs()).thenReturn(fileGraphs);

        readStream(mockStore, mockDotFileDescriptor, inputStream);

        verify(mockStore).create(DotGraphDescriptor.class);
        verify(mockDotGraphDescriptor).setStrict(true);
        verify(mockDotGraphDescriptor).setType("graph");
        verify(mockDotGraphDescriptor).setDotId("G");
        verify(mockDotGraphDescriptor).setAttribute("size","\"10,10\"","graph");
        verify(mockDotGraphDescriptor).setAttribute("used","true","graph");
        assertEquals(1, fileGraphs.size());
    }


    @Test
    void testCreateEdgeWithoutPredeclaringNodes() throws IOException {
        InputStream inputStream = streamOf("graph G { a -- b }");
        final DotGraphDescriptor mockDotGraphDescriptor = mock(DotGraphDescriptor.class);
        when(mockStore.create(DotGraphDescriptor.class)).thenReturn(mockDotGraphDescriptor);
        final DotNodeDescriptor mockDotNodeDescriptor = mock(DotNodeDescriptor.class);
        when(mockStore.create(DotNodeDescriptor.class)).thenReturn(mockDotNodeDescriptor);

        plugin.readStream(mockStore, mockDotFileDescriptor, inputStream);

        verify(mockStore).create(DotGraphDescriptor.class);
        verify(mockDotGraphDescriptor).setStrict(false);
        verify(mockDotGraphDescriptor).setType("graph");
        verify(mockDotGraphDescriptor).setDotId("G");
        verify(mockStore, times(2)).create(DotNodeDescriptor.class);
        verify(mockDotNodeDescriptor).setDotId("a");
        verify(mockDotNodeDescriptor).setDotId("b");
    }


    @Test
    public void testImportSimpleGraph() throws IOException {
        // Arrange
        InputStream inputStream = createExapmle1();
        Set<DotNodeDescriptor> nodes = new HashSet<>();

        final DotGraphDescriptor mockDotGraphDescriptor = mock(DotGraphDescriptor.class);
        when(mockStore.create(DotGraphDescriptor.class)).thenReturn(mockDotGraphDescriptor);
        when(mockDotGraphDescriptor.getNodes()).thenReturn(nodes);

        final DotNodeDescriptor mockDotNodeDescriptor = mock(DotNodeDescriptor.class);
        when(mockStore.create(DotNodeDescriptor.class)).thenReturn(mockDotNodeDescriptor);
        when(mockStore.create(any(DotNodeDescriptor.class), eq(DotRelationDescriptor.class), any(DotNodeDescriptor.class))).thenReturn(mockDotRelationshipDescriptor);

        // Act
        plugin.readStream(mockStore, mockDotFileDescriptor, inputStream);

        // Assert
        verify(mockStore).create(DotGraphDescriptor.class);
        verify(mockStore, times(2)).create(DotNodeDescriptor.class);
        verify(mockDotNodeDescriptor).setDotId("node2");
        verify(mockDotNodeDescriptor).setDotId("n1");
        verify(mockDotNodeDescriptor).setAttribute("att1","\"val1\"","node");
        verify(mockDotNodeDescriptor).setAttribute("att2","\"val2\"","node");
        verify(mockDotRelationshipDescriptor, times(2)).setAttribute("style","dotted","edge");
    }

    private InputStream createExapmle1() {
        String example = "digraph mygraph {\n" +
                "   node2 [att1 = \"val1\", att2 = \"val2\"];\n" +
                "   n1;\n" +
                "   n1 -> node2 [ style = dotted ]\n" +
                "}";
        return new ByteArrayInputStream(example.getBytes(StandardCharsets.UTF_8));
    }

    private static InputStream streamOf(final String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }
}
