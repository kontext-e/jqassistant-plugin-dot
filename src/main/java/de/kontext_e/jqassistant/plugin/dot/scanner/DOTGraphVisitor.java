package de.kontext_e.jqassistant.plugin.dot.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.dot.store.descriptor.DotGraphDescriptor;
import de.kontext_e.jqassistant.plugin.dot.store.descriptor.DotNodeDescriptor;

public class DOTGraphVisitor extends DOTBaseVisitor<String>{
    private final Store store;

    public DOTGraphVisitor(Store store) {
        this.store = store;
    }

    @Override
    public String visitGraph(DOTParser.GraphContext ctx) {
        final DotGraphDescriptor dotGraphDescriptor = store.create(DotGraphDescriptor.class);
        dotGraphDescriptor.setStrict(ctx.STRICT() != null);
        dotGraphDescriptor.setType(ctx.GRAPH() != null ? "graph" : "digraph");
        dotGraphDescriptor.setDotId(ctx.id_().getText());

        DOTStatementListVisitor statementListVisitor = new DOTStatementListVisitor(store, dotGraphDescriptor);
        statementListVisitor.visitStmt_list(ctx.stmt_list());

        return null;
    }
}
