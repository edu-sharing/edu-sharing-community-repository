package org.edu_sharing.repository.server;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.RestrictedAccessException;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.server.tools.ImageTool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StreamUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class PreviewServlet extends HttpServlet {


    private static final int MAX_IMAGE_SIZE = PreviewCache.MAX_IMAGE_SIZE;
    private static final float DEFAULT_QUALITY = 0.8f;

    private ServiceRegistry serviceRegistry;

    private org.alfresco.service.cmr.repository.NodeService dbNodeService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
        serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        dbNodeService = (org.alfresco.service.cmr.repository.NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");
    }

    private boolean isCacheable(int width, int height, int maxWidth, int maxHeight) {
        if (width == -1)
            return true;
        if (maxWidth > 0 && maxHeight > 0) {
            for (int i = 0; i < PreviewCache.CACHE_SIZES_MAX_WIDTH.length; i++) {
                if (PreviewCache.CACHE_SIZES_MAX_WIDTH[i] == maxWidth && PreviewCache.CACHE_SIZES_MAX_HEIGHT[i] == maxHeight)
                    return true;
            }
        }
        for (int i = 0; i < PreviewCache.CACHE_SIZES_WIDTH.length; i++) {
            if (PreviewCache.CACHE_SIZES_WIDTH[i] == width && PreviewCache.CACHE_SIZES_HEIGHT[i] == height)
                return true;
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        fetchNodeData(req, resp);
    }

    static class UnsupportedTypeException extends Exception {

    }

    private void fetchNodeData(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletOutputStream op = resp.getOutputStream();
        String nodeId = null;
        MimeTypesV2 mime = new MimeTypesV2(ApplicationInfoList.getHomeRepository(), MimeTypesV2.PathType.Relative);
        mime.setPreferredFormat(
                "png".equals(req.getParameter("format")) ?
                        MimeTypesV2.Format.Png : MimeTypesV2.Format.Svg);
        boolean isCollection = false;

        final StoreRef storeRef;
        String storeProtocol = req.getParameter("storeProtocol");
        String storeId = req.getParameter("storeId");
        if (storeProtocol != null && storeId != null) {
            storeRef = new StoreRef(storeProtocol, storeId);
        } else {
            storeRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
        }
        final NodeService nodeService;
        String repository = req.getParameter("repository");
        boolean remoteNode = false;
        if (repository != null) {
            nodeService = NodeServiceFactory.getNodeService(repository);
            remoteNode = true;
        } else {
            nodeService = NodeServiceFactory.getLocalService();
        }
        try {
            nodeId = req.getParameter("nodeId");
            String version = req.getParameter("version");
            NodeRef nodeRef = new NodeRef(storeRef, nodeId);
            String nodeType = nodeService.getType(nodeId);

            // check nodetype for security reasons
            String inNodeId = nodeId;
            Map<String, Object> props = new HashMap<>();
            try {
                String[] aspectsArray = nodeService.getAspects(storeRef.getProtocol(), storeRef.getIdentifier(), nodeId);
                List<String> aspects;
                if (aspectsArray == null) {
                    aspects = new ArrayList<>();
                } else {
                    aspects = Arrays.asList(aspectsArray);
                }

                if (!remoteNode) {
                    props.putAll(getBasicProperties(nodeRef));
                }

                if (remoteNode || nodeType.equals(CCConstants.CCM_TYPE_REMOTEOBJECT) || aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY)) {
                    props = nodeService.getProperties(storeRef.getProtocol(), storeRef.getIdentifier(), nodeId);
                    if (aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY)) {
                        // just fetch dynamic data which needs to be fetched, because the local io already has metadata
                        props.putAll(NodeServiceFactory.getNodeService(
                                (String) props.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID)
                        ).getPropertiesDynamic(storeProtocol, storeId, (String) props.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID)));
                    }
                    // if its local stored, load the url directly
                    String thumbnail = (String) props.get(CCConstants.CCM_PROP_IO_THUMBNAILURL);
                    if (StringUtils.isNotBlank(thumbnail)) {
                        handleExternalThumbnail(nodeId, req, resp, thumbnail);
                        return;
                    }

                    if (nodeType.equals(CCConstants.CCM_TYPE_REMOTEOBJECT) || aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY)) {
                        props = NodeServiceFactory.getNodeService((String) props.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID))
                                .getProperties(storeProtocol, storeId, (String) props.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID));
                    }

                    if (props != null) {
                        thumbnail = (String) props.get(CCConstants.CCM_PROP_IO_THUMBNAILURL);
                        if (StringUtils.isNotBlank(thumbnail)) {
                            handleExternalThumbnail(nodeId, req, resp, thumbnail);
                            return;
                        }
                    }

                    throw new UnsupportedTypeException();
                }

                // For collections: Fetch the original object for preview
                if (aspects.contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE) && props.containsKey(CCConstants.CCM_PROP_IO_ORIGINAL)) {

                    String original = (String) props.get(CCConstants.CCM_PROP_IO_ORIGINAL);

                    if (!nodeId.equals(original)) {
                        nodeId = (String) props.get(CCConstants.CCM_PROP_IO_ORIGINAL);
                        isCollection = true;
                    }
                }

                validateScope(req, props);
                // we need to check permissions and allow or deny access by using the READ_PREVIEW permission
                validatePermissions(storeRef, nodeId);

                if (!nodeType.equals(CCConstants.CCM_TYPE_IO)
                        && !nodeType.equals(CCConstants.CCM_TYPE_MAP)
                        && !nodeType.equals(CCConstants.CCM_TYPE_SAVED_SEARCH)) {
                    throw new UnsupportedTypeException();
                }
            } catch (InvalidNodeRefException e) {
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
                return;
            }

            if (CCConstants.CCM_TYPE_MAP.equals(nodeType)) {
                PreviewDetail preview = getPreview(nodeService, storeProtocol, storeId, nodeId);
                if (deliverContentAsSystem(nodeRef, CCConstants.CCM_PROP_MAP_ICON, req, resp, preview))
                    return;
            }


            //get previewurl to find out type (generated/userdefined)
            //Attention the url of GetPreviewResult of generated/userdefined previews points on this servlet so don't use it

            PreviewDetail getPrevResult = null;
            // check if version is requested and version seems to be NOT the current node version
            if (StringUtils.isNotBlank(version) && !isCollection && !version.equals(props.get(CCConstants.LOM_PROP_LIFECYCLE_VERSION))) {
                Map<String, Map<String, Object>> versionHistory = nodeService.getVersionHistory(nodeId);
                if (versionHistory != null) {
                    for (Map.Entry<String, Map<String, Object>> entry : versionHistory.entrySet()) {
                        String tmpVers = (String) entry.getValue().get(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
                        if (version.equals(tmpVers)) {

                            String vNodeId = (String) entry.getValue().get(CCConstants.SYS_PROP_NODE_UID);
                            System.out.println("vNodeId:" + vNodeId + " entry key:" + entry.getKey());
                            try {
                                getPrevResult = getPreview(nodeService, VersionService.VERSION_STORE_PROTOCOL, "version2Store", entry.getKey());
                            } catch (InvalidNodeRefException e) {
                                // ignoring this error since versioned files don't have a preview
                                // converting it to an other exception so that the mime type handler will take care
                                throw new Exception("Versioned files don't have a preview", e);
                            }

                        }

                    }
                }
            }
            final String nodeIdFinal = nodeId;

            if (getPrevResult == null) {
                // we need to access the actual object as admin
                // for collections, this is required
                // and since may there is no right to access binary content (but READ_PREVIEW is present and validated before)
                getPrevResult = AuthenticationUtil.runAsSystem(() -> getPreview(nodeService, storeRef.getProtocol(), storeRef.getIdentifier(), nodeIdFinal));
            }

            if (isCollection) {
                final PreviewDetail getPrevResultFinal = getPrevResult;
                NodeServiceHelper.validatePermissionRestrictedAccess(new NodeRef(storeRef, inNodeId), CCConstants.PERMISSION_READ_PREVIEW);
                if (AuthenticationUtil.runAsSystem(() -> loadPreview(req, resp, op, storeRef, nodeIdFinal, nodeService, getPrevResultFinal))) {
                    return;
                }
            } else {
                if (loadPreview(req, resp, op, storeRef, nodeId, nodeService, getPrevResult)) {
                    return;
                }
            }

            // generated or userdefined
            if (getPrevResult != null && getPrevResult.getType() != null && (getPrevResult.getType().equals(PreviewDetail.TYPE_USERDEFINED) || getPrevResult.getType().equals(PreviewDetail.TYPE_GENERATED))) {
                NodeRef prevNodeRef = null;
                String property = CCConstants.CM_PROP_CONTENT;
                if (getPrevResult.getType().equals(PreviewDetail.TYPE_USERDEFINED)) {
                    prevNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
                    property = CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW;
                }
                if (getPrevResult.getType().equals(PreviewDetail.TYPE_GENERATED)) {


                    final String fnodeId = nodeId;
                    if (isCollection) {
                        prevNodeRef = AuthenticationUtil.runAsSystem(
                                () -> nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, fnodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
                                        CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png));
                    } else {
                        prevNodeRef = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
                                CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
                    }
                }
                if (prevNodeRef != null) {

                    if (isCollection) {
                        boolean result = deliverContentAsSystem(prevNodeRef, property, req, resp, getPrevResult);
                        if (result) {
                            return;
                        }
                        op.close();
                    } else {
                        if (deliverContentAsSystem(prevNodeRef, property, req, resp, getPrevResult)) {

                            return;
                        }
                        op.close();


                    }

                }
            }


        } catch (org.alfresco.repo.security.permissions.AccessDeniedException | RestrictedAccessException |
                 AuthenticationCredentialsNotFoundException e) {
            log.debug(e.getMessage(), e);
            resp.sendRedirect(mime.getNoPermissionsPreview());
            return;
        } catch (InvalidNodeRefException e) {
            log.debug(e.getMessage(), e);
            resp.sendRedirect(mime.getNodeDeletedPreview());
            return;
        } catch (UnsupportedTypeException e) {
            // ignore, the node type ist not supported for image previews
        } catch (Throwable e) {
            // smaller logging for collection ref (i.e. original may deleted, that occurs often)
            if (isCollection)
                log.warn(e.getMessage());
            else
                log.warn(e.getMessage(), e);
        }

        // fallback to mime first, then default
        try {
            Map<String, Object> props;
            String[] aspects = new String[]{};
            String type = null;
            if (isCollection) {
                final String nodeIdFinal = nodeId;
                props = AuthenticationUtil.runAsSystem(() -> {
                    try {
                        return getPropertiesForDefaultHandling(storeRef, nodeIdFinal);
                    } catch (Throwable t) {
                        throw new Exception(t);
                    }
                });
            } else {
                props = getPropertiesForDefaultHandling(storeRef, nodeId);
                aspects = nodeService.getAspects(storeRef.getProtocol(), storeRef.getIdentifier(), nodeId);
                type = nodeService.getType(nodeId);
            }
            setResponseHeader(PreviewDetail.TYPE_DEFAULT, true, resp);
            resp.sendRedirect(mime.getPreview(type, props, Arrays.asList(aspects)));
            return;
        } catch (Throwable t) {
            setResponseHeader(PreviewDetail.TYPE_DEFAULT, true, resp);
            resp.sendRedirect(mime.getDefaultPreview());
        }
    }

    private Map<String, Object> getBasicProperties(NodeRef nodeRef) {
        Map<String, Object> props = new HashMap<>();
        String original = (String) dbNodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));
        if (original != null) props.put(CCConstants.CCM_PROP_IO_ORIGINAL, original);

        String scope = (String) dbNodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME));
        if (scope != null) props.put(CCConstants.CCM_PROP_EDUSCOPE_NAME, scope);

        MLText lifecycleVersion = (MLText) dbNodeService.getProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION));
        if (lifecycleVersion != null)
            props.put(CCConstants.LOM_PROP_LIFECYCLE_VERSION, lifecycleVersion.getDefaultValue());
        return props;
    }

    Map<String, Object> getPropertiesForDefaultHandling(StoreRef storeRef, String nodeId) {
        Map<String, Object> props = new HashMap<>();
        NodeRef nodeRef = new NodeRef(storeRef, nodeId);
        String wwwUrl = (String) dbNodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
        if (wwwUrl != null) props.put(CCConstants.CCM_PROP_IO_WWWURL, wwwUrl);

        String resourceType = (String) dbNodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_CCRESSOURCETYPE));
        if (resourceType != null) props.put(CCConstants.CCM_PROP_CCRESSOURCETYPE, resourceType);

        List<String> resourceTypeSub = (List<String>) dbNodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_CCRESSOURCESUBTYPE));
        if (resourceTypeSub != null && !resourceTypeSub.isEmpty())
            props.put(CCConstants.CCM_PROP_CCRESSOURCESUBTYPE, resourceTypeSub.get(0));

        String lomFormat = (String) dbNodeService.getProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT));
        if (lomFormat != null) props.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, lomFormat);

        return props;
    }

    private void validatePermissions(StoreRef storeRef, String nodeId) {
        boolean result = PermissionServiceFactory.getLocalService().hasPermission(storeRef.getProtocol(), storeRef.getIdentifier(), nodeId, CCConstants.PERMISSION_READ_PREVIEW);
        if (!result)
            throw new AccessDeniedException("No " + CCConstants.PERMISSION_READ_PREVIEW + " on " + nodeId);
    }

    private void validateScope(HttpServletRequest req, Map<String, Object> props) {
        String scope = (String) req.getSession().getAttribute(CCConstants.AUTH_SCOPE);
        // Allow only valid scope
        if (props.containsKey(CCConstants.CCM_PROP_EDUSCOPE_NAME)) {
            String nodeScope = (String) props.get(CCConstants.CCM_PROP_EDUSCOPE_NAME);
            if (!nodeScope.equals(scope)) {
                throw new AccessDeniedException("Node has an other scope");
            }
        }
        // This happens if the user tries to access a non-scoped node from a scope
        else if (scope != null) {
            throw new AccessDeniedException("Node does not have a scope");
        }
    }

    /**
     * returns true wenn a redirect was done
     */
    private boolean loadPreview(HttpServletRequest req, HttpServletResponse resp, ServletOutputStream op, StoreRef storeRef, String nodeId,
                                NodeService nodeService, PreviewDetail getPrevResult) throws IOException {

        if (getPrevResult == null || getPrevResult.getType() == null) {
            return false;
        }

        // external URL
        if (getPrevResult.getType().equals(PreviewDetail.TYPE_EXTERNAL)) {
            String extThumbUrl = getPrevResult.getUrl();
            if (StringUtils.isNotBlank(extThumbUrl)) {
                return handleExternalThumbnail(nodeId, req, resp, extThumbUrl);
            }
        }

        // generated or user defined
        if (getPrevResult.getType().equals(PreviewDetail.TYPE_USERDEFINED) || getPrevResult.getType().equals(PreviewDetail.TYPE_GENERATED)) {
            NodeRef prevNodeRef = null;
            String property = CCConstants.CM_PROP_CONTENT;
            if (getPrevResult.getType().equals(PreviewDetail.TYPE_USERDEFINED)) {
                prevNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
                property = CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW;
            }
            if (getPrevResult.getType().equals(PreviewDetail.TYPE_GENERATED)) {
                prevNodeRef = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
                        CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
            }
            if (prevNodeRef != null) {
                if (deliverContentAsSystem(prevNodeRef, property, req, resp, getPrevResult))
                    return true;
                op.close();
            }
        }
        if (getPrevResult.getType().equals(PreviewDetail.TYPE_DEFAULT)) {
            NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
            String mimetype = NodeServiceFactory.getLocalService().getContentMimetype(storeRef.getProtocol(), storeRef.getIdentifier(), nodeId);
            if (mimetype != null && mimetype.startsWith("image")) {
                return deliverContentAsSystem(nodeRef, CCConstants.CM_PROP_CONTENT, req, resp, getPrevResult);
            }
        }
        return false;
    }

    void setResponseHeader(String previewType, boolean isIcon, HttpServletResponse response) {
        if (previewType != null) response.setHeader("X-Edu-PreviewType", previewType);
        response.setHeader("X-Edu-IsIcon", String.valueOf(isIcon));
    }

    private boolean handleExternalThumbnail(String nodeId, HttpServletRequest req, HttpServletResponse resp, String url) throws IOException {
        if ("false".equalsIgnoreCase(req.getParameter("allowRedirect")) &&
                LightbendConfigLoader.get().getStringList("repository.communication.preview.remoteAllowList").stream().anyMatch((reg) -> {
                    Pattern pattern = Pattern.compile(reg);
                    Matcher matched = pattern.matcher(url);
                    return matched.matches();
                })) {
            log.debug("Follow redirect allowed for {}", url);
            try {
                new HttpQueryTool().queryStream(url, new HttpQueryTool.Callback<Void>() {
                    @Override
                    public void handle(InputStream httpResult) {
                        resp.setHeader("Content-Type", "image/jpeg");
                        try {
                            DataInputStream extImgTransformed = postProcessImage(nodeId, new DataInputStream(httpResult), req, null);
                            if (extImgTransformed == null) {
                                log.warn("Failed to transform external image for node {}", nodeId);
                                return;
                            }
                            StreamUtils.copy(extImgTransformed, resp.getOutputStream());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                return true;
            } catch (Throwable t) {
                log.info("Fetching preview via http failed for: {}", url, t);
            }
        }
        resp.sendRedirect(url);
        return true;
    }

    private DataInputStream postProcessImage(String nodeId, DataInputStream in, HttpServletRequest req, String mimetype) {
        float quality = DEFAULT_QUALITY;

        int width = 0, height = 0, maxHeight = 0, maxWidth = 0;
        boolean crop = false;
        boolean hasAnyValue = false;
        try {
            quality = Integer.parseInt(req.getParameter("quality")) / 100.f;
            hasAnyValue = true;
        } catch (Throwable ignored) {
        }

        try {
            crop = req.getParameter("crop").equals("true");
            hasAnyValue = true;
        } catch (Throwable ignored) {
        }
        try {
            width = Integer.parseInt(req.getParameter("width"));
        } catch (Throwable ignored) {
        }
        try {
            height = Integer.parseInt(req.getParameter("height"));
        } catch (Throwable ignored) {
        }
        try {
            maxWidth = Integer.parseInt(req.getParameter("maxWidth"));
        } catch (Throwable ignored) {
        }
        try {
            maxHeight = Integer.parseInt(req.getParameter("maxHeight"));
        } catch (Throwable ignored) {
        }
        boolean fullsize = false;
        if (!hasAnyValue) {
            width = -1;
            height = -1;
            crop = true;
            fullsize = true;
            maxWidth = MAX_IMAGE_SIZE;
            maxHeight = MAX_IMAGE_SIZE;
        }

        boolean fromCache = false;
        if (fullsize || isCacheable(width, height, maxWidth, maxHeight)) {
            File file = PreviewCache.getFileForNode(nodeId, fullsize ? -1 : width, height, maxWidth, maxHeight, false);
            if (file != null && file.exists()) {
                try {
                    in.close();
                    in = new DataInputStream(new FileInputStream(file));
                    fromCache = true;
                } catch (Throwable ignored) {
                }
            }
        }
        // if the image is cached, allow only 70% quality because it's useless to compress in higher ratio
        quality = Math.min(Math.max(quality, 0), fromCache ? DEFAULT_QUALITY : 1);
        width = Math.min(Math.max(width, 0), MAX_IMAGE_SIZE);
        height = Math.min(Math.max(height, 0), MAX_IMAGE_SIZE);
        maxWidth = Math.min(Math.max(maxWidth, 0), MAX_IMAGE_SIZE);
        maxHeight = Math.min(Math.max(maxHeight, 0), MAX_IMAGE_SIZE);

        try {
            // cache optimization, if no other tasks, just return the cached preview
            if (fromCache && Math.abs(quality - DEFAULT_QUALITY) < 0.1) {
                log.debug("Sending direct image cache to client: " + nodeId);
                byte[] img = StreamUtils.copyToByteArray(in);
                return new DataInputStream(new ByteArrayInputStream(img));
            }
            BufferedImage img;
            boolean svg = false;
            if (Objects.equals("image/svg+xml", mimetype)) {
                svg = true;
                img = ImageIO.read(new ByteArrayInputStream(ImageTool.convertSvgToPng(in)));
            } else {
                img = ImageIO.read(in);
            }

            try {
                float aspect = Float.parseFloat(req.getParameter("aspect"));
                if (aspect > 1) {
                    width = img.getWidth();
                    height = (int) (width / aspect);
                } else {
                    height = img.getHeight();
                    width = (int) (height * aspect);
                }
            } catch (Throwable ignored) {
            }
            boolean scale = true;
            if (crop && !fromCache) {
                float aspectOriginal = (float) img.getWidth() / (float) img.getHeight();
                if (maxWidth > 0) {
                    width = (int) Math.min(height * aspectOriginal, maxWidth);
                }
                if (maxHeight > 0) {
                    height = (int) Math.min(width / aspectOriginal, maxHeight);
                }
                if (maxWidth > 0 && maxHeight > 0) {
                    if (aspectOriginal > 1) {
                        width = maxWidth;
                        height = (int) (width / aspectOriginal);
                    } else {
                        height = maxHeight;
                        width = (int) (height * aspectOriginal);
                    }
                    if (width > img.getWidth() || height > img.getHeight()) {
                        scale = false;
                    }
                }
                if (!scale && !svg && quality >= 1.0f) {
                    return null;
                }
                if (!scale) {
                    // we will not increase size
                    width = img.getWidth();
                    height = img.getHeight();
                }
                BufferedImage cropped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); // getType() sometimes return 0
                float aspectCrop = (float) width / (float) height;
                Graphics g = cropped.getGraphics();
                if (aspectCrop > aspectOriginal) {
                    float scaledHeight = cropped.getWidth() / aspectOriginal;
                    Image scaled = img.getScaledInstance((int) (scaledHeight * aspectOriginal), (int) scaledHeight, BufferedImage.SCALE_SMOOTH);
                    g.drawImage(scaled, 0, (int) (-(scaledHeight - cropped.getHeight()) / 2), cropped.getWidth(), (int) scaledHeight, null);
                } else {
                    float scaledWidth = cropped.getHeight() * aspectOriginal;
                    Image scaled = img.getScaledInstance((int) scaledWidth, (int) (scaledWidth / aspectOriginal), BufferedImage.SCALE_SMOOTH);
                    g.drawImage(scaled, (int) (-(scaledWidth - cropped.getWidth()) / 2), 0, (int) scaledWidth, cropped.getHeight(), null);
                }
                img = cropped;

            }
            BufferedImage imgOut = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            imgOut.getGraphics().setColor(java.awt.Color.WHITE);
            imgOut.getGraphics().fillRect(0, 0, imgOut.getWidth(), imgOut.getHeight());
            imgOut.getGraphics().drawImage(img, 0, 0, null);

            if (!fromCache && (isCacheable(width, height, maxWidth, maxHeight) || fullsize)) {
                // Drop alpha (weird colors in jpg otherwise)
                File fileForNode = PreviewCache.getFileForNode(nodeId, fullsize ? -1 : width, height, maxWidth, maxHeight, true);
                if (fileForNode != null) {
                    ImageIO.write(imgOut, "JPG", fileForNode);
                } else {
                    log.warn("Could not write preview for node {}. Node was null", nodeId);
                }
            }

            JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
            jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpegParams.setCompressionQuality(quality);
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            MemoryCacheImageOutputStream imgOutStream = new MemoryCacheImageOutputStream(os);

            writer.setOutput(imgOutStream);
            writer.write(null, new IIOImage(imgOut, null, null), jpegParams);
            imgOutStream.close();
            in.close();
            return new DataInputStream(new ByteArrayInputStream(os.toByteArray()));
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Throwable t) {
            if (t.getCause() instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) t.getCause();
            }
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }

    }

    private boolean deliverContentAsSystem(NodeRef nodeRef, String contentProp, HttpServletRequest req, HttpServletResponse resp, PreviewDetail previewDetail) throws IOException {

        return AuthenticationUtil.runAsSystem(() -> {
            ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef,
                    QName.createQName(contentProp));

            if (reader == null || reader.getSize() == 0) {
                return false;
            }

            String mimetype = reader.getMimetype();


            InputStream is = reader.getContentInputStream();
            DataInputStream in = new DataInputStream(is);
            if (mimetype.startsWith("image")) {
                try {
                    DataInputStream tmp = postProcessImage(nodeRef.getId(), in, req, mimetype);
                    if (tmp != null) {
                        in = tmp;
                        mimetype = "image/jpeg";
                    } else {
                        // image was broken but stream is consumed, open a new one
                        reader = serviceRegistry.getContentService().getReader(nodeRef,
                                QName.createQName(contentProp));
                        is = reader.getContentInputStream();
                        in = new DataInputStream(is);
                    }

                } catch (OutOfMemoryError e) {
                    log.debug("Image too large for memory, falling back to icon {}", nodeRef);
                    throw e;
                }
            }
            if (previewDetail != null) {
                setResponseHeader(previewDetail.getType(), previewDetail.isIcon, resp);
            }
            deliverImage(mimetype, in, resp);
            return true;
        });

    }

    private void deliverImage(String mimetype, DataInputStream in, HttpServletResponse resp) throws IOException {
        ServletOutputStream op = resp.getOutputStream();
        int length;
        // fix to proper mimetype (usually comes at "image/svg xml" which is not valid)
        if (mimetype.startsWith("image/svg")) {
            mimetype = "image/svg+xml";
        }
        if (mimetype.equals("image/svg+xml")) {
            throw new RuntimeException("svg is not supported and could not be converted");
        }
        resp.setContentType(mimetype);

        resp.setContentLength((int) in.available());

        byte[] bbuf = new byte[1024];
        while ((length = in.read(bbuf)) != -1) {
            op.write(bbuf, 0, length);
        }

        in.close();

        op.flush();
        op.close();
    }


    public static PreviewDetail getPreview(NodeService nodeService, String storeProtocol, String storeIdentifier, String nodeId) {
        return getPreview(nodeService, storeProtocol, storeIdentifier, nodeId, null);
    }

    public static PreviewDetail getPreview(NodeService nodeService, String storeProtocol, String storeIdentifier, String nodeId, Map<String, Object> nodeProps) {
        StoreRef storeRef = new StoreRef(storeProtocol, storeIdentifier);
        String nodeType = nodeService.getType(nodeId);

        if (nodeType.equals(CCConstants.CCM_TYPE_MAP)) {

            boolean isIcon = true;
            String prevType = PreviewDetail.TYPE_DEFAULT;
            if (nodeProps != null) {
                if (nodeProps.get(CCConstants.CCM_PROP_MAP_ICON) != null){
                    isIcon = false;
                    prevType = PreviewDetail.TYPE_USERDEFINED;
                }
            } else {
                if (nodeService.getProperty(storeProtocol, storeIdentifier, nodeId, CCConstants.CCM_PROP_MAP_ICON) != null){
                    isIcon = false;
                    prevType = PreviewDetail.TYPE_USERDEFINED;
                }
            }
            return new PreviewDetail(null, prevType, false, isIcon);
        }

        if (!nodeType.equals(CCConstants.CCM_TYPE_IO)) {
            return null;
        }

        String extThumbnail = (nodeProps == null) ? nodeService.getProperty(storeProtocol, storeIdentifier, nodeId, CCConstants.CCM_PROP_IO_THUMBNAILURL)
                : (String) nodeProps.get(CCConstants.CCM_PROP_IO_THUMBNAILURL);
        if (StringUtils.isNotBlank(extThumbnail)) {
            return new PreviewDetail(extThumbnail, PreviewDetail.TYPE_EXTERNAL, false, false);
        }

        String defaultImageUrl = URLTool.getBaseUrl() + "/"
                + CCConstants.DEFAULT_PREVIEW_IMG;

        try (InputStream crUserDefinedPreview = nodeService.getContent(storeProtocol, storeIdentifier, nodeId, null, CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW)) {
            // userdefined
            if (crUserDefinedPreview != null && crUserDefinedPreview.available() > 0) {
                String url = nodeService.getPreview(storeProtocol, storeIdentifier, nodeId, null, null).getUrl();
                return new PreviewDetail(url, PreviewDetail.TYPE_USERDEFINED, false, false);
            }

        } catch (Throwable t) {
            // may fails if the user does not has access for content
        }

        // generated and no action active
        NodeRef previewProps = nodeService.getChild(storeRef, nodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
                CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
        InputStream generatedIs = null;
        try {
            generatedIs = nodeService.getContent(previewProps.getStoreRef().getProtocol(), previewProps.getStoreRef().getIdentifier(), previewProps.getId(), null, CCConstants.CM_PROP_CONTENT);
        } catch (Throwable ignored) {

        }
        if (previewProps != null && generatedIs != null) {
            String url = NodeServiceHelper.getPreview(new NodeRef(storeRef, nodeId)).getUrl();
            return new PreviewDetail(url, PreviewDetail.TYPE_GENERATED, false, false);
        }

        return new PreviewDetail(defaultImageUrl, PreviewDetail.TYPE_DEFAULT, false, true);
    }

    @Getter
    public static class PreviewDetail {
        private final String url;
        private final String type;

        private final boolean createActionIsRunning;

        private final boolean isIcon;

        public static final String TYPE_EXTERNAL = "TYPE_EXTERNAL";
        public static final String TYPE_USERDEFINED = "TYPE_USERDEFINED";
        public static final String TYPE_GENERATED = "TYPE_GENERATED";
        public static final String TYPE_DEFAULT = "TYPE_DEFAULT";

        public PreviewDetail(String url, String type, boolean createActionIsRunning, boolean isIcon) {
            this.url = url;
            this.type = type;
            this.createActionIsRunning = createActionIsRunning;
            this.isIcon = isIcon;
        }

    }
}
