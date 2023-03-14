package org.edu_sharing.alfresco.service.search.cmis;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class QueryBuilderTest {

  @Mock
  private DictionaryService dictionaryService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private PropertyDefinition propertyDefinition;

  private QueryBuilder queryBuilder;

  @BeforeEach
  void setUp() {
    queryBuilder = new QueryBuilder(dictionaryService);
  }

  @Test
  void selectAllTest(){

    String expected = "SELECT * FROM ccm:io AS io";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO);

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void selectNativeFields(){

    String expected = "SELECT " +
            "io.cmis:objectId, " +
            "io.cmis:name, " +
            "io.cmis:versionLabel, " +
            "io.cmis:createdBy, " +
            "io.cmis:creationDate, " +
            "io.cmis:lastModifiedBy, " +
            "io.cmis:lastModificationDate FROM ccm:io AS io";

    QueryStatement query = Query
            .select(CCConstants.SYS_PROP_NODE_UID,
                    CCConstants.CM_NAME,
                    CCConstants.CM_PROP_VERSIONABLELABEL,
                    CCConstants.CM_PROP_C_CREATOR,
                    CCConstants.CM_PROP_C_CREATED,
                    CCConstants.CM_PROP_C_MODIFIER,
                    CCConstants.CM_PROP_C_MODIFIED)
            .from(CCConstants.CCM_TYPE_IO);

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void selectWithJoinFields(){

    String expected = "SELECT iometadata.ccm:original FROM ccm:io AS io JOIN null AS iometadata ON iometadata.cmis:objectId = io.cmis:objectId";

    Mockito.when(dictionaryService.getProperty(ArgumentMatchers.notNull())).thenReturn(propertyDefinition);
    Mockito.when(propertyDefinition.getContainerClass().getName().getLocalName()).thenReturn("iometadata");
    Mockito.when(propertyDefinition.getContainerClass().getName().toString()).thenReturn("ccm:iometadata");
    Mockito.when(propertyDefinition.getName().toString()).thenReturn(CCConstants.CCM_PROP_IO_ORIGINAL);

    QueryStatement query = Query
            .select(CCConstants.CCM_PROP_IO_ORIGINAL)
            .from(CCConstants.CCM_TYPE_IO);

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void selectTrhowsUnknownPropertyDefinitionException(){
    QueryStatement query = Query
            .select(CCConstants.CCM_PROP_IO_ORIGINAL)
            .from(CCConstants.CCM_TYPE_IO);

    Assertions.assertThrows(RuntimeException.class, ()->queryBuilder.build(query));
  }



  @Test
  void whereEqNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE io.cmis:objectId = '1234'";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.eq(CCConstants.SYS_PROP_NODE_UID, "1234"));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereNeqNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE io.cmis:objectId <> '1234'";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.neq(CCConstants.SYS_PROP_NODE_UID, "1234"));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereGtNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE io.cmis:objectId > '1234'";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.gt(CCConstants.SYS_PROP_NODE_UID, "1234"));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereGteNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE io.cmis:objectId >= '1234'";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.gte(CCConstants.SYS_PROP_NODE_UID, "1234"));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereLtNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE io.cmis:objectId < '1234'";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.lt(CCConstants.SYS_PROP_NODE_UID, "1234"));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereLteNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE io.cmis:objectId <= '1234'";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.lte(CCConstants.SYS_PROP_NODE_UID, "1234"));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereLikeNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE io.cmis:objectId LIKE '1234'";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.like(CCConstants.SYS_PROP_NODE_UID, "1234"));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereAndNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE ( io.cmis:objectId = '1234' AND io.cmis:name = 'test' )";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.and(Filters.eq(CCConstants.SYS_PROP_NODE_UID, "1234"), Filters.eq(CCConstants.CM_NAME, "test")));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereOrNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE ( io.cmis:objectId = '1234' OR io.cmis:name = 'test' )";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.or(Filters.eq(CCConstants.SYS_PROP_NODE_UID, "1234"), Filters.eq(CCConstants.CM_NAME, "test")));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereIsNotNullNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE io.cmis:objectId IS NOT NULL";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.isNotNull(CCConstants.SYS_PROP_NODE_UID));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereIsNullNativeFields(){

    String expected = "SELECT * FROM ccm:io AS io WHERE io.cmis:objectId IS NULL";

    QueryStatement query = Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.isNull(CCConstants.SYS_PROP_NODE_UID));

    String actual = queryBuilder.build(query);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void whereValueNullThrowsIllegalArgumentException() {
    Assertions.assertThrows(IllegalArgumentException.class, ()-> Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.eq(CCConstants.SYS_PROP_NODE_UID, null)));
  }

  @Test
  void whereUnknownArgumentTypeThrowsIllegalArgumentException() {
    Assertions.assertThrows(IllegalArgumentException.class, ()-> Query
            .selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(Filters.eq(CCConstants.SYS_PROP_NODE_UID, new Argument() {
              @Override
              public int hashCode() {
                return super.hashCode();
              }
            })));
  }


  @Test
  void buildWithoutQueryStatementThrowsIllegalArgumentException() {
    Assertions.assertThrows(IllegalArgumentException.class, ()-> queryBuilder.build(null));
  }

  @Test
  void buildUnknownArgumentTypeThrowsIllegalArgumentException() {
    QueryStatement query = Query.selectAll()
            .from(CCConstants.CCM_TYPE_IO)
            .where(new Predicate("op", Property.create(CCConstants.SYS_PROP_NODE_UID), new Argument() {
              @Override
              public int hashCode() {
                return super.hashCode();
              }
            }));

    Assertions.assertThrows(IllegalArgumentException.class, ()-> queryBuilder.build(query));
  }
}
