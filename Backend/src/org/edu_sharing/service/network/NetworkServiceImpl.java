package org.edu_sharing.service.network;

import com.google.gson.Gson;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.apache.commons.codec.digest.DigestUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.service.network.model.Service;
import org.edu_sharing.service.network.model.StoredService;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


public class NetworkServiceImpl implements NetworkService {
    NodeService nodeService;
    public NetworkServiceImpl(){
        nodeService=NodeServiceFactory.getLocalService();
    }
    @Override
    public Collection<StoredService> getServices() throws Throwable{
        return AuthenticationUtil.runAsSystem(()->{
            try{
                List<ChildAssociationRef> services = nodeService.getChildrenChildAssociationRef(
                        new UserEnvironmentTool().getEdu_SharingServiceFolder());
                Collection<StoredService> result=new ArrayList<>();
                for(ChildAssociationRef service : services){
                    HashMap<String, Object> props = nodeService.getProperties(service.getChildRef().getStoreRef().getProtocol(), service.getChildRef().getStoreRef().getIdentifier(), service.getChildRef().getId());
                    String data= (String) props.get(CCConstants.CCM_PROP_SERVICE_NODE_DATA);
                    StoredService serviceObject = new Gson().fromJson(data, StoredService.class);
                    serviceObject.setId((String) props.get(CCConstants.CM_NAME));
                    result.add(serviceObject);
                }
                return result;
            }
            catch(Throwable t){
                throw new RuntimeException(t);
            }
        });
    }

    @Override
    public StoredService addService(Service service) throws Throwable {
        return AuthenticationUtil.runAsSystem(()->{
            try{
                String parent=new UserEnvironmentTool().getEdu_SharingServiceFolder();

                String id=generateId(service);
                HashMap<String, String[]> props = generateProps(id,service);
                nodeService.createNode(parent,CCConstants.CCM_TYPE_SERVICE_NODE,props);
                return new StoredService(id,service);
            }
            catch(Throwable t){
                throw new RuntimeException(t);
            }
        });
    }

    @Override
    public StoredService updateService(String id, Service service) throws Throwable {
        try{
            String parent=new UserEnvironmentTool().getEdu_SharingServiceFolder();
            String node = nodeService.findNodeByName(parent, id);
            HashMap<String, String[]> props = generateProps(id,service);
            nodeService.updateNode(node,props);
            return new StoredService(id,service);
        }
        catch(Throwable t){
            throw new RuntimeException(t);
        }
    }
    private String generateId(Service service) {
        return DigestUtils.sha1Hex(service.getUrl());
    }

    private HashMap<String, String[]> generateProps(String id, Service service) {
        HashMap<String, String[]> props = new HashMap<>();
        props.put(CCConstants.CM_NAME,new String[]{id});

        String json=new Gson().toJson(service);
        props.put(CCConstants.CCM_PROP_SERVICE_NODE_NAME,new String[]{service.getName()});
        props.put(CCConstants.CCM_PROP_SERVICE_NODE_DESCRIPTION,
                service.getDescription().stream().map(d -> d.getValue()).collect(Collectors.toList()).toArray(new String[0])
        );
        props.put(CCConstants.CCM_PROP_SERVICE_NODE_TYPE,new String[]{service.getType()});
        props.put(CCConstants.CCM_PROP_SERVICE_NODE_DATA,new String[]{json});
        return props;
    }
}
