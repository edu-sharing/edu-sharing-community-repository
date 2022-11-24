package org.alfresco.repo.module;

import java.lang.reflect.Field;

public class EduSharingModuleServiceImpl extends ModuleServiceImpl {


    public EduSharingModuleServiceImpl()
    {
        super();
        try {
            Field moduleComponentHelperField = ModuleServiceImpl.class.getDeclaredField("moduleComponentHelper");
            moduleComponentHelperField.setAccessible(true);

            EduSharingModuleComponentHelper moduleComponentHelper = new EduSharingModuleComponentHelper();
            moduleComponentHelperField.set(this, moduleComponentHelper);
            moduleComponentHelper.setModuleService(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


}
