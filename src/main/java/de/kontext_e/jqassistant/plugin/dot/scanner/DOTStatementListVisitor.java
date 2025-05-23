package de.kontext_e.jqassistant.plugin.dot.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.dot.store.descriptor.DotGraphDescriptor;
import de.kontext_e.jqassistant.plugin.dot.store.descriptor.DotNodeDescriptor;
import de.kontext_e.jqassistant.plugin.dot.store.descriptor.DotRelationDescriptor;

public class DOTStatementListVisitor extends DOTBaseVisitor<String>{
    private final Store store;
    private final DotGraphDescriptor dotGraphDescriptor;

    public DOTStatementListVisitor(Store store, DotGraphDescriptor dotGraphDescriptor) {
        this.store = store;
        this.dotGraphDescriptor = dotGraphDescriptor;
    }

    @Override
    public String visitStmt_list(DOTParser.Stmt_listContext ctx) {
        for (DOTParser.StmtContext stmtContext : ctx.stmt()) {
            visitStmt(stmtContext);
        }

        return null;
    }

    @Override
    public String visitStmt(DOTParser.StmtContext ctx) {
        if (ctx.node_stmt() != null) {
            final DotNodeDescriptor dotNodeDescriptor = store.create(DotNodeDescriptor.class);
            dotNodeDescriptor.setDotId(ctx.node_stmt().node_id().getText());
            dotGraphDescriptor.getNodes().add(dotNodeDescriptor);

            if(ctx.node_stmt().attr_list() != null) {
                DOTAttributeVisitor.visit(ctx.node_stmt().attr_list(), dotNodeDescriptor, "node");
            }
        } else if (ctx.edge_stmt() != null) {
            DOTParser.Edge_stmtContext edgeContext = ctx.edge_stmt();
            if(edgeContext.node_id() != null) {

            }

/*
            final DotRelationDescriptor edgeDescriptor = store.create(DotRelationDescriptor.class);

            if (edgeContext.attr_list() != null) {
                DOTAttributeVisitor.visit(edgeContext.attr_list(), edgeDescriptor, "edge");
            }
*/
        }


        return null;
    }

}
