package org.edu_sharing.repository.client.tools;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.alfresco.service.config.model.KeyValuePair;
import org.json.JSONObject;

import javax.servlet.ServletContext;
import java.io.File;

/**
 * Class to load language data from the angular i18n files (json)
 */
public class I18nAngular {
    public static Logger logger=Logger.getLogger(I18nAngular.class);
    public static String getTranslationAngular(String scope,String key){
        return getTranslationAngular(scope,key,new AuthenticationToolAPI().getCurrentLanguage());
    }

    public static JSONObject getLanguageStrings() throws Exception{
        String language=new AuthenticationToolAPI().getCurrentLanguage();
        ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
        File[] dirs = new File(context.getRealPath("/assets/i18n/")).listFiles(File::isDirectory);
        JSONObject result=new JSONObject();
        for(File dir : dirs) {
            File i18n = new File(dir, language + ".json");
            String json = FileUtils.readFileToString(i18n,"UTF-8");
            JSONObject jsonObject = new JSONObject(json);
            try {
                for (String key : JSONObject.getNames(jsonObject)) {
                    result.put(key, jsonObject.get(key));
                }
            }catch(NullPointerException e){
                // the override is usually empty, can be ignored
            }
        }
        return result;
    }
    /**
     * Get the translation
     * @param scope equals the folder name in assets/i18n, e.g. workspace, common, search
     * @param key The key. Usually UPPERCASE. You can use "." for getting sub-keys, e.g. "WORKSPACE.TOAST.MY_KEY"
     * @param language
     * @return
     */
    public static String getTranslationAngular(String scope,String key,String language){
        try {
            String override=getTranslationFromOverride(key,language);
            if(override!=null)
                return override;
            // Using global instance singe it only is used for file reading
            ServletContext servletContext = Context.getGlobalContext();
            String json=FileUtils.readFileToString(new File(servletContext.getRealPath("/assets/i18n/"+scope+"/"+language+".json")),"UTF-8");
            JSONObject object=new JSONObject(json);
            String[] list=key.split("\\.");
            for(int i=0;i<list.length-1;i++){
                object=object.getJSONObject(list[i]);
            }
            String result=object.getString(list[list.length-1]);
            return result;
        } catch (Exception e) {
            logger.info("No translation in Angular found for "+scope+" "+key);
            return key;
        }
    }

    /**
     * try to find the given override string in the client.config
     * will return null if it is not overriden
     * @param key
     * @param language
     */
    private static String getTranslationFromOverride(String key, String language) {
        try {
            for (KeyValuePair pair:ConfigServiceFactory.getLanguageData()) {
                if(pair.key.equals(key))
                    return pair.value;
            }
        }catch(Exception e){
            logger.debug(e);
        }
        return null;
    }

}
