package org.edu_sharing.repository.tomcat;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.jobs.quartz.AbstractJob;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassHelper {

    static Logger logger = Logger.getLogger(ClassHelper.class);

    public static List<Class> getSubclasses(Class clazz){
        return getSubclasses(clazz, null);
    }
    public static List<Class> getSubclasses(Class clazz, String packageName){
        if(packageName == null) packageName = "org.edu_sharing";
        List<Class> result = new ArrayList<>();
        try{
            for(Class c : getClasses(packageName)){
                try {
                    c.asSubclass(clazz);
                    result.add(c);
                }catch (ClassCastException e){};
            }
        }catch(ClassNotFoundException | IOException e){

        }
        return result;
    }

    private static Class[] getClasses(String packageName)
            throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String cName = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {

                    classes.add(Class.forName(cName));
                }catch(NoClassDefFoundError e){
                    logger.warn("maybe class import not found");
                }catch(VerifyError e){
                    logger.warn(e.getMessage());
                }
            }
        }
        return classes;
    }

    public static List<Field> getStaticFields(Class clazz){
        Field[] declaredFields = clazz.getDeclaredFields();
        List<Field> staticFields = new ArrayList<Field>();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                staticFields.add(field);
            }
        }
        return staticFields;
    }
}
