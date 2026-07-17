package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.rating.RatingDetails;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Data
public class Node implements Serializable {

    @Schema(description = "Reference (id and repo details) for this node", required = true)
    @JsonProperty(required = true)
    private NodeRef ref = null;

    @Schema(description = "Parent node reference")
    private NodeRef parent = null;

    @Schema(description = "Node LTI deep linking information")
    private NodeLTIDeepLink nodeLTIDeepLink = null;

    @Schema(description = "Remote node information (in case this node is from a remote/federated repository)")
    private Remote remote = null;

    @Schema(description = "Node main type, i.e. ccm:io or ccm:map")
    private String type = null;

    @Schema(description = "Aspects applied to this node, i.e. ccm:collection_io_reference")
    private List<String> aspects = new ArrayList<>();

    @Schema(description = "Node (file) name - limited to file name patterns", required = true)
    @JsonProperty(required = true)
    private String name = null;

    @Schema(description = "Node title")
    private String title = null;

    @Schema(description = "Metadata set name")
    private String metadataset = null;

    @Schema(description = "Repository type of the repository this node is originated from")
    private String repositoryType = null;

    @Schema(description = "Creation date", required = true, type = "string", format = "date-time")
    @JsonProperty(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date createdAt = null;

    @Schema(description = "Person who created the node", required = true)
    @JsonProperty(required = true)
    private Person createdBy = null;

    @Schema(description = "Modification date", type = "string", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date modifiedAt = null;

    @Schema(description = "Person who last modified the node")
    private Person modifiedBy = null;

    @Schema(description = "Access permissions of the actual node object", required = true)
    @JsonProperty(required = true)
    private List<String> access = null;

    @Schema(description = "Indicates if access permissions are inherited from parent nodes")
    private Boolean inherited;

    @Schema(description = "the effective access; this is the effective access, i.e. if this element is used in a collection, it will get more permissions;  please use this field to check access")
    private java.util.Collection<String> accessEffective;

    @Schema(description = "Download url for this node")
    private String downloadUrl = null;

    @Schema(description = "Properties of the node; Dynamic key value pairs depending on the properties")
    private Map<String, String[]> properties = null;

    @Schema(description = "mime type of the node")
    private String mimetype = null;

    @Schema(description = "Media type of the node (simplified/grouped mimetype)")
    private String mediatype = null;

    @Schema(description = "Size of the node in bytes")
    private String size = null;

    @Schema(description = "Preview/Thumbnail information")
    private Preview preview = null;

    @Schema(description = "Content information")
    private Content content = null;
    @Schema(description = "icon url & details")
    private NodeIcon icon;

    @Schema(description = "license details")
    private License license;

    @Schema(description = "Whether this node is a directory")
    @JsonProperty("isDirectory")
    private boolean directory;

    @Schema(description = "In case this node is a collection, you'll find the details about the collection specific data here")
    private Collection collection;

    @Schema(description = "Owner of the node", required = true)
    @JsonProperty(required = true)
    private Person owner;

    @Schema(description = "Number of comments on this node")
    private int commentCount;

    @Schema(description = "Rating details")
    private RatingDetails rating;

    @Schema(description = "Collections in which this node is used (only filled for some requests)")
    private List<Node> usedInCollections = new ArrayList<>();

    @Schema(description = "Relations to other nodes")
    private Map<NodeRefImpl.Relation, Node> relations;

    @Schema(description = "Contributors (authors, publishers) for the node")
    private List<Contributor> contributors;

    private boolean isPublic;

    /**
     * Explicit getter on purpose: if the field carried the annotations directly, Lombok would
     * generate the getter WITHOUT copying @JsonProperty over. Jackson would then derive an
     * additional bean property "public" from the isPublic() getter, next to "isPublic".
     * That extra "public" is not part of the OpenAPI definition and breaks generated Java clients:
     * "The field `public` in the JSON string is not defined in the `Node` properties".
     */
    @Schema(description = "Whether the node is public (shared to everyone)")
    @JsonProperty("isPublic")
    boolean isPublic(){
        return isPublic;
    }

    /**
     * fake a node from a ref
     * i.e. if the real node could not be fetched or is deleted
     *
     * @param ref
     * @return
     */
    public static Node FakeFromRef(NodeRef ref) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		Node node = NodeDao.createEmptyDummy(Node.class, ref, CCConstants.CCM_TYPE_IO);
        node.setPreview(new Preview());
        node.setIcon(new NodeIcon(new MimeTypesV2().getDefaultIcon(), null));
        HashMap<String, String[]> props = new HashMap<>();
        props.put(CCConstants.getValidLocalName(CCConstants.CM_NAME), new String[]{ref.getId()});
        node.setProperties(props);
        return node;
    }


    @Override
    public String toString() {
        return "class Node {\n" +
                "  ref: " + ref + "\n" +
                "  parent: " + parent + "\n" +
                "  type: " + type + "\n" +
                "  aspects: " + aspects + "\n" +
                "  name: " + name + "\n" +
                "  title: " + title + "\n" +
                "  createdAt: " + createdAt + "\n" +
                "  createdBy: " + createdBy + "\n" +
                "  modifiedAt: " + modifiedAt + "\n" +
                "  modifiedBy: " + modifiedBy + "\n" +
                "  access: " + access + "\n" +
                "  properties: " + properties + "\n" +
                "  mimetype: " + mimetype + "\n" +
                "  size: " + size + "\n" +
                "  preview: " + preview + "\n" +
                "}\n";
    }
}
