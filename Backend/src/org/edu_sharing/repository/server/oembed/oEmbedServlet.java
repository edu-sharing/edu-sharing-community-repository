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
                int maxWidth=oEmbedBase.DEFAULT_SIZE,maxHeight=oEmbedBase.DEFAULT_SIZE;
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
        // the oembed guidelines encourage to use different properties per type
        // however, most providers and consumers don't seem to fully follow them
        // so we'll always add all available properties, regardless of type
        oEmbedBase embed = new oEmbedBase();
        switch (nodeDao.getMediatype()) {
            case "file-image": {
                embed.setUrl(nodeDao.asNode().getPreview().getUrl());
            }
            default: {
                fillBaseData(embed, nodeDao);
                handleSize(embed,nodeDao,maxWidth,maxHeight);
                return embed;
            }
        }
    }
    private int[] getSize(double width,double height,int maxWidth,int maxHeight){
        double aspect=width/height;
        if(maxWidth<width){
            width=maxWidth;
            height= (int) (maxWidth/aspect);
        }
        else if(maxHeight<height){
            width= (int) (maxHeight*aspect);
            height=maxHeight;
        }
        return new int[]{(int)width,(int)height};
    }
    private void handleSize(oEmbedBase embed, NodeDao nodeDao, int maxWidth, int maxHeight) throws DAOException {
        double width;
        double height;
        try {
            width = Double.parseDouble((String) nodeDao.getNativeProperties().get(CCConstants.CCM_PROP_IO_WIDTH));
            height = Double.parseDouble((String) nodeDao.getNativeProperties().get(CCConstants.CCM_PROP_IO_HEIGHT));
        }catch(Throwable t){
            // unparsable or invalid size (or may something which has no size)
            width=oEmbedBase.DEFAULT_SIZE;
            height=oEmbedBase.DEFAULT_SIZE;
        }
        int[] size=getSize(width,height,maxWidth,maxHeight);
        int[] thumbSize=getSize(width,height,oEmbedBase.DEFAULT_THUMBNAIL_SIZE,oEmbedBase.DEFAULT_THUMBNAIL_SIZE);
        embed.setHtml("<iframe width=\""+size[0]+"\" height=\""+size[1]+"\" src=\""+URLTool.getEduservletUrl()+"render?node_id="+nodeDao.getRef().getId()+"\" frameborder=\"0\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture\"></iframe>");
        embed.setThumbnail_url(nodeDao.asNode().getPreview().getUrl()+"&crop=true&maxWidth="+thumbSize[0]+"&maxHeight="+thumbSize[1]);
        embed.setThumbnail_width(thumbSize[0]);
        embed.setThumbnail_height(thumbSize[1]);
        embed.setWidth(size[0]);
        embed.setHeight(size[1]);
    }

    private void fillBaseData(oEmbedBase embed, NodeDao nodeDao) throws DAOException {
        Node node=nodeDao.asNode();
        embed.setAuthor_name(node.getCreatedBy().getFirstName()+" "+node.getCreatedBy().getLastName());
        embed.setTitle(node.getTitle()!=null && !node.getTitle().isEmpty() ? node.getTitle() : node.getName());
        embed.setType(node.getMediatype().equals("file-image") ? "photo" : node.getMediatype().equals("file-video") ? "video" : "rich");
        embed.setProvider_url(URLTool.getBaseUrl());
    }
}
