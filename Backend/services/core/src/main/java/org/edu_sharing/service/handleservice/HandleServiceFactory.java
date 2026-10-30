package org.edu_sharing.service.handleservice;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.service.handleservicedoi.DOIService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HandleServiceFactory {
    public enum IMPLEMENTATION {
        /**
         * handle.net service implementation, see repository.handleservice.mode (protocol, rest or epic)
         */
        handle,
        /**
         * doi datacite implementation
         */
        doi
    }
    private final HandleServiceImpl handleService;
    private final HandleServiceRest handleServiceRest;
    private final HandleServiceEpic handleServiceEpic;
    private final DOIService doiService;

    public HandleService instance(IMPLEMENTATION type) throws HandleServiceNotConfiguredException{
        if(type == IMPLEMENTATION.doi){
            return doiService;
        }
        return switch (handleService.getMode()) {
            case rest -> handleServiceRest;
            case epic -> handleServiceEpic;
            case protocol -> handleService;
        };
    }
}
