package de.kontext_e.jqassistant.plugin.dot.scanner;

import de.kontext_e.jqassistant.plugin.dot.store.descriptor.AttributesContainer;
import de.kontext_e.jqassistant.plugin.dot.store.descriptor.DotNodeDescriptor;

public class DOTAttributeVisitor {
    public static void visit(DOTParser.Attr_listContext attrListContext, AttributesContainer attributesContainer, String type) {
        for (DOTParser.A_listContext att : attrListContext.a_list()) {
            for (int i = 0; i < att.id_().size(); i+=2) {
                attributesContainer.setAttribute(att.id_(i).getText(), att.id_(i+1).getText(), type);
            }
        }

    }
}
