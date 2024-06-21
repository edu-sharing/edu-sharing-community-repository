package org.edu_sharing.service.handleservice;

import org.apache.log4j.Logger;
import org.edu_sharing.service.handleservicedoi.DOIService;

public class HandleServiceFactory {

    Logger logger = Logger.getLogger(HandleServiceFactory.class);

    public enum IMPLEMENTATION {handle,doi};

    public static HandleService instance(IMPLEMENTATION type) throws HandleServiceNotConfiguredException{
        if(type  == IMPLEMENTATION.doi){
            return new DOIService();
        } else {
            return new HandleServiceImpl();
        }
    }

    public static HandleService instance() throws HandleServiceNotConfiguredException{
        try{ return instance(IMPLEMENTATION.handle); }catch (HandleServiceNotConfiguredException e){}
        try{ return instance(IMPLEMENTATION.doi); }catch (HandleServiceNotConfiguredException e){}

        throw new HandleServiceNotConfiguredException();
    }


}
