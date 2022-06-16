package org.edu_sharing.alfresco.service.search.cmis;

public class Table extends QueryStatement {
    protected Table(Selection selection, String from) {
        super(selection, from);
    }

    public QueryStatement where(Predicate predicate) {
        this.setWhere(predicate);
        return this;
    }
}
