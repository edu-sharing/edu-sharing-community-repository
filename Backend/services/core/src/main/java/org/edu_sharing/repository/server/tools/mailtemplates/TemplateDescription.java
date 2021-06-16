package org.edu_sharing.repository.server.tools.mailtemplates;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Objects;
import java.util.Random;

public class TemplateDescription {
    public String name;
    public String context;

    public TemplateDescription(String name, String context) {
        this.name = name;
        this.context = context;
    }

    public static TemplateDescription fromNode(Node item) {
        NamedNodeMap attr = item.getAttributes();
        if(attr==null)
            return null;
        if(attr.getNamedItem("name")==null)
            return null;
        return new TemplateDescription(attr.getNamedItem("name").getTextContent(),
                attr.getNamedItem("context")==null ? null : attr.getNamedItem("context").getTextContent());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateDescription that = (TemplateDescription) o;
        return name.equals(that.name) &&
                Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, context);
    }
}
