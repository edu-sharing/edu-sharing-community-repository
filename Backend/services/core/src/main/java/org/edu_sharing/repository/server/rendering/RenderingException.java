package org.edu_sharing.repository.server.rendering;

import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.HttpException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class RenderingException extends ServletException {

    public enum I18N{
        invalid_parameters,
        encryption,
        usage_missing,
        node_missing,
        usage_missing_permissions,
        internal,
        unknown,
    }
    private int statusCode;
    private String technicalDetail;
    private I18N i18nName;
    private Throwable nested;

    public RenderingException(int statusCode,String technicalDetail, I18N i18nName) {
        this.statusCode=statusCode;
        this.technicalDetail=technicalDetail;
        this.i18nName=i18nName;
    }
    public RenderingException(HttpException exception){
        this.statusCode=exception.getStatusCode();
        this.nested=exception;
        try{
            JSONObject json = new JSONObject(exception.getMessage());
            this.i18nName=I18N.valueOf(json.getString("i18nName"));
            this.technicalDetail=json.getString("technicalDetail");
        }
        catch(Throwable t){
            this.technicalDetail=exception.getMessage();
            this.i18nName=I18N.unknown;
        }
    }
    public RenderingException(int statusCode,String technicalDetail, I18N i18nName,Throwable nested) {
        this(statusCode,technicalDetail,i18nName);
        this.nested=nested;

    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getTechnicalDetail() {
        return technicalDetail;
    }

    public String getI18nName() {
        return i18nName.name();
    }

    public Throwable getNested() {
        return nested;
    }
}
