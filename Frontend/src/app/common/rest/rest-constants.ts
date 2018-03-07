import {WorkflowDefinition} from "./data-object";

export class RestConstants {
  public static DOCUMENT_EDITOR_URL="http://appserver9.metaventis.com/eduConDev/";
  public static HOME_REPOSITORY = "-home-";
  public static ME="-me-";
  public static ROOT="-root-";
  public static DEFAULT="-default-";

  public static DUPLICATE_NODE_RESPONSE=409;

  public static DEFAULT_SORT_CRITERIA : string[]=["cm:name"];
  public static DEFAULT_SORT_ASCENDING=true;
  public static API_VERSION="v1";
  public static API_VERSION_3_2=1.0;
  public static API_VERSION_4_0=1.1;

  public static CM_NAME = "cm:name";
  public static SORT_BY_FIELDS=[RestConstants.CM_NAME];

  public static AUTHORITY_TYPE_USER = "USER";
  public static AUTHORITY_TYPE_GROUP = "GROUP";
  public static AUTHORITY_TYPE_OWNER = "OWNER";
  public static AUTHORITY_TYPE_EVERYONE = "EVERYONE";
  public static AUTHORITY_EVERYONE = "GROUP_EVERYONE";
  public static getAuthorityEveryone(){
    return {authorityName:RestConstants.AUTHORITY_EVERYONE,authorityType:RestConstants.AUTHORITY_TYPE_EVERYONE};
  }
  public static PERMISSION_CONSUMER = "Consumer";
  public static PERMISSION_COLLABORATOR = "Collaborator";
  public static PERMISSION_COORDINATOR = "Coordinator";

  public static COLLECTIONSCOPE_MY = "MY";
  public static COLLECTIONSCOPE_ORGA = "EDU_GROUPS";
  public static COLLECTIONSCOPE_ALL = "EDU_ALL";
  public static COLLECTIONSCOPE_CUSTOM = "CUSTOM";
  public static COLLECTIONSCOPE_CUSTOM_PUBLIC = "CUSTOM_PUBLIC";
  public static COLLECTIONSCOPE_TYPE_EDITORIAL = "TYPE_EDITORIAL";

  public static CCM_ASPECT_TOOL_DEFINITION = "ccm:tool_definition";
  public static CCM_ASPECT_TOOL_OBJECT = "ccm:tool_object";
  public static CCM_ASPECT_IO_REFERENCE = "ccm:collection_io_reference";
  public static CCM_ASPECT_COLLECTION = "ccm:collection";
  public static CCM_ASPECT_LOMREPLICATION = "ccm:lomreplication";
  public static CCM_ASPECT_CCLOM_GENERAL = "cclom:general";

  public static CM_TYPE_FOLDER = "cm:folder";
  public static CM_TYPE_PERSON = "cm:person";
  public static SIZE = "size";
  public static MEDIATYPE = "mediatype";
  public static DIMENSIONS = "dimensions";
  public static CM_MODIFIED_DATE = "cm:modified";
  public static CM_CREATOR = "cm:creator";
  public static CM_OWNER = "cm:owner";
  public static CM_ARCHIVED_DATE = "sys:archivedDate";
  public static CM_PROP_TITLE = "cm:title";
  public static CM_TYPE_CONTENT = "cm:content";
  public static CM_TYPE_OBJECT = "cm:cmobject";
  public static CM_TYPE_CONTAINER = "cm:container";
  public static CM_TYPE_AUTHORITY_CONTAINER = "cm:authorityContainer";
  public static CM_PROP_C_CREATED = "cm:created";
  public static CCM_TYPE_IO = "ccm:io";
  public static CCM_TYPE_REMOTEOBJECT="ccm:remoteobject";
  public static CCM_TYPE_TOOL_INSTANCE = "ccm:tool_instance";
  public static CCM_TYPE_SAVED_SEARCH = "ccm:saved_search";
  public static CCM_FILENAME = "ccm:filename";
  public static CCM_PROP_WIDTH = "ccm:width";
  public static EXIF_PROP_DATE_TIME_ORIGINAL = "exif:dateTimeOriginal";
  public static CCM_PROP_HEIGHT = "ccm:height";
  public static LOM_PROP_GENERAL_KEYWORD = "cclom:general_keyword";
  public static LOM_PROP_TECHNICAL_DURATION = "cclom:duration";
  public static LOM_PROP_GENERAL_DESCRIPTION = "cclom:general_description";
  public static LOM_PROP_LIFECYCLE_VERSION = "cclom:version";
  public static LOM_PROP_TECHNICAL_FORMAT = "cclom:format";
  public static LOM_PROP_DESCRIPTION = "cclom:general_description";
  public static LOM_PROP_TITLE = "cclom:title";
  public static CCM_PROP_METADATACONTRIBUTER_CREATOR = "ccm:metadatacontributer_creator";
  public static CCM_PROP_METADATACONTRIBUTER_CREATOR_FN = "ccm:metadatacontributer_creatorFN";
  public static CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR = "ccm:lifecyclecontributer_author";
  public static CCM_PROP_AUTHOR_FREETEXT = "ccm:author_freetext";
  public static CCM_PROP_EAF_PRODUCTIONYEAR = "ccm:eaf_productionyear";
  public static CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR_FN = "ccm:lifecyclecontributer_authorFN";
  public static CCM_PROP_EDITOR_TYPE = "ccm:editorType";
  public static CCM_PROP_CCRESSOURCETYPE = "ccm:ccressourcetype";
  public static CCM_PROP_CCRESSOURCESUBTYPE = "ccm:ccresourcesubtype";
  public static CCM_PROP_CCRESSOURCEVERSION = "ccm:ccressourceversion";
  public static CCM_PROP_COLLECTION_PINNED_STATUS = "ccm:collection_pinned_status";
  public static CCM_PROP_COLLECTION_PINNED_ORDER = "ccm:collection_pinned_order";
  public static CCM_PROP_IO_WWWURL = "ccm:wwwurl";
  public static CCM_PROP_IO_ORIGINAL = "ccm:original";
  public static CM_PROP_AUTHORITY_AUTHORITYNAME = "cm:authorityName";
  public static VIRTUAL_PROP_USAGECOUNT = "virtual:usagecount";
  public static GROUP_PREFIX="GROUP_";

  public static DATE_FIELDS=[RestConstants.CM_ARCHIVED_DATE,
                            RestConstants.CM_MODIFIED_DATE,
                            RestConstants.CM_PROP_C_CREATED,
                            RestConstants.CCM_PROP_EAF_PRODUCTIONYEAR,
                            RestConstants.EXIF_PROP_DATE_TIME_ORIGINAL];


  public static STATUS_CODE_OK = "OK";
  public static STATUS_CODE_PREVIOUS_SESSION_REQUIRED = "PREVIOUS_SESSION_REQUIRED";
  public static STATUS_CODE_PREVIOUS_USER_WRONG = "PREVIOUS_USER_WRONG";

  public static FILTER_FILES="files";
  public static FILTER_FOLDERS="folders";
  public static ALL="-all-";
  public static COUNT_UNLIMITED = 2147483647;
  public static USERHOME='-userhome-';
  public static SHARED_FILES='-shared_files-';
  public static MY_SHARED_FILES='-my_shared_files-';
  public static TO_ME_SHARED_FILES='-to_me_shared_files-';
  public static INBOX = "-inbox-";
  public static WORKFLOW_RECEIVE='-workflow_receive-';
  public static SAFE_SCOPE="safe";

  public static CCM_PROP_LICENSE = 'ccm:commonlicense_key';
  public static CCM_PROP_LICENSE_TITLE_OF_WORK = 'ccm:license_title_of_work';
  public static CCM_PROP_LICENSE_SOURCE_URL = 'ccm:license_source_url';
  public static CCM_PROP_LICENSE_PROFILE_URL = 'ccm:license_profile_url';
  public static CCM_PROP_LICENSE_CC_VERSION = 'ccm:commonlicense_cc_version';
  public static CCM_PROP_LICENSE_CC_LOCALE = 'ccm:commonlicense_cc_locale';
  public static CCM_PROP_REPLICATIONSOURCE = 'ccm:replicationsource';
  public static LOM_PROP_RIGHTS_DESCRIPTION= 'cclom:rights_description';
  public static CCM_PROP_QUESTIONSALLOWED= 'ccm:questionsallowed';
  public static CM_PROP_METADATASET_EDU_METADATASET="cm:edu_metadataset";
  public static CM_PROP_METADATASET_EDU_FORCEMETADATASET = "cm:edu_forcemetadataset";
  public static CCM_PROP_TOOL_CATEGORY= 'ccm:tool_category';
  public static CCM_PROP_TOOL_PRODUCER= 'ccm:tool_producer';
  public static CCM_PROP_TOOL_INSTANCE_REF= 'ccm:tool_instance_ref';

  public static CCM_PROP_WF_RECEIVER= 'ccm:wf_receiver';
  public static CCM_PROP_WF_STATUS= 'ccm:wf_status';
  public static CCM_PROP_WF_INSTRUCTIONS= 'ccm:wf_instructions';
  public static CCM_PROP_WF_PROTOCOL= 'ccm:wf_protocol';
  public static NODE_ID = "sys:node-uuid";
  public static LUCENE_SCORE = "score";

  public static COMMENT_MAIN_FILE_UPLOAD="MAIN_FILE_UPLOAD";
  public static COMMENT_EDITOR_UPLOAD="EDITOR_UPLOAD";
  public static ACCESS_ADD_CHILDREN="AddChildren";
  public static ACCESS_WRITE="Write";
  public static ACCESS_DELETE="Delete";
  public static ACCESS_CHANGE_PERMISSIONS="ChangePermissions";
  public static ACCESS_CONSUMER="Consumer";
  public static ACCESS_CC_PUBLISH="CCPublish";
  public static ACCESS_COMMENT="Comment";
  public static CONTENT_TYPE_FILES="FILES";
  public static CONTENT_TYPE_FILES_AND_FOLDERS="FILES_AND_FOLDERS";
  public static CONTENT_TYPE_ALL="ALL";
  public static CONTENT_TYPE_COLLECTIONS="COLLECTIONS";

  public static COMBINE_MODE_AND="AND";
  public static COMBINE_MODE_OR="OR";

  public static AUTHORITY_NAME="authorityName";
  public static AUTHORITY_DISPLAYNAME="displayName";
  public static AUTHORITY_FIRSTNAME="firstName";
  public static AUTHORITY_LASTNAME="lastName";
  public static AUTHORITY_EMAIL="email";
  public static AUTHORITY_GROUPTYPE="groupType";


  public static TOOLPERMISSION_INVITE="TOOLPERMISSION_INVITE";
  public static TOOLPERMISSION_INVITE_SHARE="TOOLPERMISSION_INVITE_SHARE";
  public static TOOLPERMISSION_INVITE_HISTORY="TOOLPERMISSION_INVITE_HISTORY";
  public static TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH="TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH";
  public static TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE="TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE";
  public static TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE="TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE";
  public static TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE="TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE";
  public static TOOLPERMISSION_CONFIDENTAL="TOOLPERMISSION_CONFIDENTAL";
  public static TOOLPERMISSION_INVITE_SAFE="TOOLPERMISSION_INVITE_SAFE";
  public static TOOLPERMISSION_INVITE_SHARE_SAFE="TOOLPERMISSION_INVITE_SHARE_SAFE";
  public static TOOLPERMISSION_INVITE_ALLAUTHORITIES="TOOLPERMISSION_INVITE_ALLAUTHORITIES";
  public static TOOLPERMISSION_LICENSE="TOOLPERMISSION_LICENSE";
  public static TOOLPERMISSION_WORKSPACE="TOOLPERMISSION_WORKSPACE";
  public static TOOLPERMISSION_UNCHECKEDCONTENT="TOOLPERMISSION_UNCHECKEDCONTENT";
  public static TOOLPERMISSION_COLLECTION_EDITORIAL="TOOLPERMISSION_COLLECTION_EDITORIAL";
  public static TOOLPERMISSION_COLLECTION_CURRICULUM="TOOLPERMISSION_COLLECTION_CURRICULUM";
  public static TOOLPERMISSION_COLLECTION_PINNING="TOOLPERMISSION_COLLECTION_PINNING";

  public static SHARE_LINK = "LINK";
  public static SHARE_EXPIRY_UNLIMITED = -1;

  public static WORKFLOW_STATUS_UNCHECKED:WorkflowDefinition={id:'100_unchecked',color:"#539DD5",hasReceiver:true,next:null};
  public static WORKFLOW_STATUS_TO_CHECK:WorkflowDefinition={id:'200_tocheck',color:"#3CB0B0",hasReceiver:true,next:null};
  public static WORKFLOW_STATUS_HASFLAWS:WorkflowDefinition={id:'300_hasflaws',color:"#D58553",hasReceiver:true,next:null};
  public static WORKFLOW_STATUS_CHECKED:WorkflowDefinition={id:'400_checked',color:"#42A053",hasReceiver:false,next:null};

  public static POSSIBLE_SORT_BY_FIELDS=[RestConstants.CM_NAME,
    RestConstants.CM_MODIFIED_DATE,
    RestConstants.CM_PROP_C_CREATED,
    RestConstants.CM_CREATOR,
    RestConstants.CCM_PROP_WF_STATUS,
    RestConstants.CM_ARCHIVED_DATE,
    RestConstants.LOM_PROP_GENERAL_KEYWORD,
    RestConstants.CCM_PROP_LICENSE,
    RestConstants.AUTHORITY_DISPLAYNAME,
    RestConstants.AUTHORITY_FIRSTNAME,
  ];
  public static LICENSE_URLS={
    "CC_BY_ABOUT":"https://creativecommons.org/licenses/?lang=de",
    "CC_BY":"https://creativecommons.org/licenses/by/#version/legalcode.de",
    "CC_BY_ND":"https://creativecommons.org/licenses/by-nd/#version/legalcode.de",
    "CC_BY_SA":"https://creativecommons.org/licenses/by-sa/#version/legalcode.de",
    "CC_BY_NC":"https://creativecommons.org/licenses/by-nc/#version/legalcode.de",
    "CC_BY_NC_ND":"https://creativecommons.org/licenses/by-nc-nd/#version/legalcode.de",
    "CC_BY_NC_SA":"https://creativecommons.org/licenses/by-nc-sa/#version/legalcode.de",
    "CC_0":"https://creativecommons.org/publicdomain/zero/1.0/legalcode",
    "PDM":"https://creativecommons.org/choose/mark/"

  }
  public static DEFAULT_QUERY_NAME="ngsearch";

  public static HTTP_UNAUTHORIZED = 401;
  public static HTTP_FORBIDDEN = 403;
  public static HTTP_NOT_FOUND = 404;
  public static HOME_APPLICATION_XML="homeApplication.properties.xml";
  public static CCMAIL_APPLICATION_XML="ccmail.properties.xml";
  public static NODE_VERSION_CURRENT = "-1";
  public static PRIMARY_SEARCH_CRITERIA='ngsearchword';
  public static DISPLAYNAME_SUFFIX = "_DISPLAYNAME";
  public static SAVED_SEARCH="-saved_search-";
  public static CCM_PROP_SAVED_SEARCH_MDS="ccm:saved_search_mds";
  public static CCM_PROP_SAVED_SEARCH_REPOSITORY="ccm:saved_search_repository";
  public static CCM_PROP_SAVED_SEARCH_PARAMETERS="ccm:saved_search_parameters";
  public static GROUP_TYPE_ADMINISTRATORS="ORG_ADMINISTRATORS";
  public static GROUP_TYPE_EDITORIAL="EDITORIAL";
  public static VALID_GROUP_TYPES = ["",RestConstants.GROUP_TYPE_ADMINISTRATORS,RestConstants.GROUP_TYPE_EDITORIAL];
  public static COLLECTIONTYPE_DEFAULT = "default";
  public static COLLECTIONTYPE_EDITORIAL = "EDITORIAL";
  public static COLLECTIONTYPE_CURRICULUM = "CURRICULUM";
  public static REPOSITORY_TYPE_ALFRESCO = "ALFRESCO";
  static COLLECTION_ORDER_MODE_CUSTOM = "custom";
}
