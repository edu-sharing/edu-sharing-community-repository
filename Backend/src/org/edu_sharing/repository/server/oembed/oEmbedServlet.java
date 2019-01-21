package org.edu_sharing.repository.server.oembed;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.rendering.RenderingErrorServlet;
import org.edu_sharing.repository.server.rendering.RenderingException;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.rendering.RenderingService;
import org.edu_sharing.service.rendering.RenderingServiceFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class oEmbedServlet extends HttpServlet {
    private static final int DEFAULT_MAX = 600;
    Logger logger = Logger.getLogger(oEmbedServlet.class);
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            try {
                String format = req.getParameter("format");
                if(format==null)
                    format="json";
                String node_id = req.getParameter("node_id");
                String version = req.getParameter("version");
                String maxwidth = req.getParameter("maxwidth");
                String maxheight = req.getParameter("maxheight");
                RenderingService renderingService = RenderingServiceFactory.getLocalService();
                Map<String, String> params = new HashMap<>();
                for (Object key : req.getParameterMap().keySet()) {
                    params.put((String) key, req.getParameter((String) key));
                }
                NodeDao nodeDao = NodeDao.getNodeWithVersion(RepositoryDao.getHomeRepository(), node_id, version);
                int maxWidth=DEFAULT_MAX,maxHeight=DEFAULT_MAX;
                if(maxwidth!=null)
                    maxWidth=Integer.parseInt(maxwidth);
                if(maxheight!=null)
                    maxHeight=Integer.parseInt(maxheight);
                oEmbedBase base=getOEmbed(nodeDao,maxWidth,maxHeight);
                if(format.equals("json")){
                    resp.setContentType("application/json");
                    String json=new Gson().toJson(base);
                    resp.getWriter().write(json);
                }
                else if(format.equals("xml")){
                    resp.setContentType("application/xml");
                    JAXBContext jaxbContext = JAXBContext.newInstance(base.getClass());
                    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                    StringWriter sw = new StringWriter();
                    jaxbMarshaller.marshal(base, sw);
                    String xmlContent = sw.toString();
                    resp.getWriter().write(xmlContent);
                }
                else{
                    resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
                }
            }catch(Throwable t){
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,t.getMessage());
            }
        }

    private oEmbedBase getOEmbed(NodeDao nodeDao, int maxWidth, int maxHeight) throws DAOException {
        switch (nodeDao.getMediatype()) {
            case "image": {
                oEmbedPhoto embed = new oEmbedPhoto();
                fillBaseData(embed, nodeDao);
                embed.setUrl(nodeDao.getDownloadUrl());
                return embed;
            }
            // video is basically the same as "rich", so to prevent heavy casting, simply use the rich object
            /*
            case "video": {
                oEmbedVideo embed = new oEmbedVideo();
                fillBaseData(embed, node);
                handleSize(embed,nodeDao,maxWidth,maxHeight);
                return embed;
            }
            */
            default: {
                oEmbedRich embed = new oEmbedRich();
                fillBaseData(embed, nodeDao);
                handleSize(embed,nodeDao,maxWidth,maxHeight);
                return embed;
            }
        }
    }

    private void handleSize(oEmbedRich embed, NodeDao nodeDao, int maxWidth, int maxHeight) throws DAOException {
        double width,height;
        try {
            width = Double.parseDouble((String) nodeDao.getNativeProperties().get(CCConstants.CCM_PROP_IO_WIDTH));
            height = Double.parseDouble((String) nodeDao.getNativeProperties().get(CCConstants.CCM_PROP_IO_HEIGHT));
        }catch(Throwable t){
            // unparsable or invalid size (or may something which has no size)
            width=DEFAULT_MAX;
            height=DEFAULT_MAX;
        }
        double aspect=width/height;
        if(maxWidth<width){
            width=maxWidth;
            height=maxWidth/aspect;
        }
        if(maxHeight<height){
            width=maxHeight*aspect;
            height=maxHeight;
        }
        width=Math.round(width);
        height=Math.round(height);
        embed.setHtml("<iframe width=\""+width+"\" height=\""+height+"\" src=\""+URLTool.getEduservletUrl()+"render?node_id="+nodeDao.getRef().getId()+"\" frameborder=\"0\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture\"></iframe>");
        embed.setWidth((int) width);
        embed.setHeight((int) height);
    }

    private void fillBaseData(oEmbedBase embed, NodeDao nodeDao) throws DAOException {
        Node node=nodeDao.asNode();
        embed.setAuthor_name(node.getCreatedBy().getFirstName()+" "+node.getCreatedBy().getLastName());
        embed.setTitle(node.getTitle()!=null && !node.getTitle().isEmpty() ? node.getTitle() : node.getName());
        embed.setThumbnail_url(node.getPreview().getUrl());
        embed.setType(node.getMediatype().equals("file-image") ? "photo" : node.getMediatype().equals("file-video") ? "video" : "rich");
        embed.setProvider_url(URLTool.getBaseUrl());
    }
}
