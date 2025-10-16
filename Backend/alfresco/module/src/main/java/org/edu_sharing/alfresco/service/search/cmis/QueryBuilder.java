package org.edu_sharing.alfresco.service.search.cmis;

import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class QueryBuilder {
    private final DictionaryService dictionaryService;

    private static final Map<String, String> cmisAlfMapping = new HashMap<>() {{
        put(CCConstants.SYS_PROP_NODE_UID, "objectId");
        put(CCConstants.CM_NAME, "name");
        put(CCConstants.CM_PROP_VERSIONABLELABEL, "versionLabel");
        put(CCConstants.CM_PROP_C_CREATOR, "createdBy");
        put(CCConstants.CM_PROP_C_CREATED, "creationDate");
        put(CCConstants.CM_PROP_C_MODIFIER, "lastModifiedBy");
        put(CCConstants.CM_PROP_C_MODIFIED, "lastModificationDate");
    }};

    public String build(QueryStatement queryStatement) {
        if (queryStatement == null) {
            throw new IllegalArgumentException("queryStatement can not be null");
        }

        Selection selection = queryStatement.getSelection();
        String table = CCConstants.getValidLocalName(queryStatement.getFrom());
        String tableAlias = QName.createQName(queryStatement.getFrom()).getLocalName();
        String from = String.format("FROM %s AS %s", table, tableAlias);


        Property[] properties = selection.getProperties();
        Set<Join> joinTables = new HashSet<>();

        String select = "*";
        if (properties != null && properties.length > 0) {
            select = Arrays.stream(properties)
                    .map(x -> getPropertyName(x, tableAlias, joinTables))
                    .collect(Collectors.joining(", "));
        }

        String where = "";
        Predicate whereStatement = queryStatement.getWhere();
        if (whereStatement != null) {
            where = "WHERE " + buildWhere(whereStatement, tableAlias, joinTables);
        }

        String join = joinTables.stream()
                .map(x -> x.joinOn(tableAlias))
                .collect(Collectors.joining(" "));

        return String.join(" ", "SELECT", select, from, join, where).trim().replaceAll("\\s+", " ");
    }


    private String getPropertyName(Property property, String fromAlias, Set<Join> joinTables) {
        String nativeValue = cmisAlfMapping.get(property.getValue());
        if (StringUtils.isNotBlank(nativeValue)) {
            return fromAlias + ".cmis:" + nativeValue;
        }
        QName qName = QName.createQName(property.getValue());
        PropertyDefinition propertyDefinition = dictionaryService.getProperty(qName);
        if (propertyDefinition == null) {
            throw new RuntimeException(
                    "The following properties were not found in alfresco dictionary: " + property.getValue());
        }

        String joinTableAlias = propertyDefinition.getContainerClass().getName().getLocalName();
        String column = CCConstants.getValidLocalName(propertyDefinition.getName().toString());
        joinTables.add(new Join("LEFT", propertyDefinition.getContainerClass().getName()));
        return joinTableAlias + "." + column;
    }

    private String getAspectName(Aspect aspect, Set<Join> joinTables) {
        QName qName = QName.createQName(aspect.getValue());
        AspectDefinition aspectDefinition = dictionaryService.getAspect(qName);
        if (aspectDefinition == null) {
            throw new RuntimeException(
                    "The following aspects were not found in alfresco dictionary: " + aspect.getValue());
        }

        joinTables.add(new Join("INNER", aspectDefinition.getName()));
        return "";
    }

    private String buildWhere(Predicate predicate, String fromAlias, Set<Join> joinTables) {

        String operation = predicate.getOperation();
        String lhs = buildWhere(fromAlias, joinTables, predicate.getLhs());
        String rhs = buildWhere(fromAlias, joinTables, predicate.getRhs());

        if(StringUtils.isBlank(lhs)){
            return rhs;
        }

        if(StringUtils.isBlank(rhs)){
            return lhs;
        }

        return String.join(" ",
                lhs,
                operation,
                rhs);
    }


    private String buildWhere(String fromAlias, Set<Join> joinTables, Argument arg) {
        if (arg == null) {
            return "";
        }

        if (arg instanceof Predicate) {
            return buildWhere((Predicate) arg, fromAlias, joinTables);
        } else if (arg instanceof Property) {
            return getPropertyName((Property) arg, fromAlias, joinTables);
        } else if (arg instanceof Aspect) {
            return getAspectName((Aspect) arg, joinTables);
        } else if (arg instanceof Value) {
            return String.format("'%s'", ((Value) arg).getValue()
                    .replace("\\", "\\\\")
                    .replace("'", "\\'"));
        } else {
            throw new IllegalArgumentException(String.format("Unknown type for arg: %s", arg.getClass().getName()));
        }
    }

    record Join(String kind, QName table) {
        public String joinOn(String tableAlias) {
                String joinTable = CCConstants.getValidLocalName(table.toString());
                String joinTableAlias = table.getLocalName();
                return String.format("%s JOIN %s AS %s ON %s.cmis:objectId = %s.cmis:objectId", kind, joinTable, joinTableAlias, joinTableAlias, tableAlias);

        }
    }
}
