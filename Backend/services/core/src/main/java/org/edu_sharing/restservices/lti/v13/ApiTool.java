package org.edu_sharing.restservices.lti.v13;

import org.springframework.extensions.surf.util.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class ApiTool {

    public static Response processError(HttpServletRequest req, Throwable e, String errorType){
        try {
            return Response.seeOther(new URI(req.getScheme() +"://"
                            + req.getServerName()
                            + "/edu-sharing/components/messages/"+errorType+"/"+ URLEncoder.encode(e.getMessage())))
                    .build();
        } catch (URISyntaxException ex) {
            return Response.status(Response.Status.OK).entity(getHTML(null,null,"error:" + ex.getMessage())).build();
        }
    }

    public static String getHTML(String formTargetUrl, Map<String,String> params){
        return getHTML(formTargetUrl,params,null,null);
    }

    public static String getHTML(String formTargetUrl, Map<String,String> params, String errorMessage){
        return getHTML(formTargetUrl,params,errorMessage,null);
    }
    /**
     * @TODO use template engine?
     * @param formTargetUrl
     * @param params
     * @return
     */
    public static String getHTML(String formTargetUrl, Map<String,String> params, String message, String javascript){
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        if(javascript != null){
            sb.append("<script type=\"text/javascript\">"+javascript+"</script>");
        }
        if(message == null) {
            String FORMNAME = "ltiform";
            sb.append("<script type=\"text/javascript\">window.onload=function(){document.forms[\""+FORMNAME+"\"].submit();}</script>");
            sb.append("<form action=\"" + formTargetUrl + "\" method=\"post\" name=\"" + FORMNAME + "\">");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append("<input type=\"hidden\" id=\"" + entry.getKey() + "\" name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" class=\"form-control\"/>");
            }
            sb.append("<input type=\"submit\" value=\"Submit POST\" class=\"btn btn-primary\">")
                    .append("</form>");
        }else {
            sb.append(message);
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}
