package org.edu_sharing.service.dataprotection.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DataProtectionQueue {


    public enum Status {
        REQUESTED,
        RUNNING,
        FINISHED
    }

    @Autowired
    DataProtectionQueueMapper dataProtectionQueueMapper;

    public DataProtectionQueueEntry add(String user){
        DataProtectionQueueEntry entry = new DataProtectionQueueEntry();
        entry.setUser(user);
        entry.setRequested(new Date());
        entry.setStatus(Status.REQUESTED.toString());
        dataProtectionQueueMapper.insert(entry);
        return dataProtectionQueueMapper.findByUser(user);
    }

    public void update(String user, String nodeId, Status status){
        DataProtectionQueueEntry entry = dataProtectionQueueMapper.findByUser(user);
        entry.setStatus(status.toString());
        entry.setNode_id(nodeId);
        if(status == Status.FINISHED){
            entry.setFinished(new Date());
        }
        dataProtectionQueueMapper.update(entry);
    }

    public void update(DataProtectionQueueEntry entry){
        dataProtectionQueueMapper.update(entry);
    }

    public void delete(String user){
        DataProtectionQueueEntry entry = new DataProtectionQueueEntry();
        entry.setUser(user);
        dataProtectionQueueMapper.delete(entry);
    }

    public List<DataProtectionQueueEntry> getAll(){
        return dataProtectionQueueMapper.findAll();
    }

    public List<DataProtectionQueueEntry> get(Status status){
        return dataProtectionQueueMapper.findAllByStatus(status.toString());
    }

    public DataProtectionQueueEntry get(String user){
        return dataProtectionQueueMapper.findByUser(user);
    }
}
