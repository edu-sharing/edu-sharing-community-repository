package org.edu_sharing.repository.server.jobs;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.repository.server.jobs.ibatis.JobQueueMapper;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobQueueService {

    private final JobQueueMapper jobQueueMapper;

    public void deleteQueuedJobs(List<Long> jobIds) {
        if(!AuthorityServiceHelper.isAdmin()){
            throw new InsufficientPermissionException("Not allowed to delete queued jobs");
        }
        jobQueueMapper.deleteByJobIds(jobIds);
    }

    public List<JobQueueEntry> getQueuedJobs(int skip, int limit) {
        if(!AuthorityServiceHelper.isAdmin()){
            throw new InsufficientPermissionException("Not allowed to get queued jobs");
        }
        return jobQueueMapper.getJobs(skip, limit);
    }

    public void resetJobStatus(Long jobId) {
        if(!AuthorityServiceHelper.isAdmin()){
            throw new InsufficientPermissionException("Not allowed to get queued jobs");
        }
        jobQueueMapper.resetStatus(jobId);
    }
}
