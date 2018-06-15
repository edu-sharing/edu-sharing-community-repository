package org.edu_sharing.repository.client.tools;

import org.apache.commons.io.FileUtils;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.Context;
import org.json.JSONObject;

import javax.servlet.ServletContext;
import java.io.File;

/**
 * Class to load language data from the angular i18n files (json)
 */
public class I18nAngular {
    public static String getTranslationAngular(String scope,String key){
        return getTranslationAngular(scope,key,new AuthenticationToolAPI().getCurrentLanguage());
    }

    /**
     * Get the translation
     * @param scope equals the folder name in assets/i18n, e.g. workspace, common, search
     * @param key The key. Usually UPPERCASE. You can use "." for getting sub-keys, e.g. "WORKSPACE.TOAST.MY_KEY"
     * @param language
     * @return
     */
    public static String getTranslationAngular(String scope,String key,String language){
        ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
        try {
            String json=FileUtils.readFileToString(new File(context.getRealPath("/assets/i18n/"+scope+"/"+language+".json")),"UTF-8");
            JSONObject object=new JSONObject(json);
            String[] list=key.split("\\.");
            for(int i=0;i<list.length-1;i++){
                object=object.getJSONObject(list[i]);
            }
            String result=object.getString(list[list.length-1]);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return key;
        }
    }

}
