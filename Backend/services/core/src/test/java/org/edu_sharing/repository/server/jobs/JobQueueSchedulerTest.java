package org.edu_sharing.repository.server.jobs;

import org.edu_sharing.repository.server.jobs.ibatis.JobQueueMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobQueueSchedulerTest {

    private final JobQueueMapper mapper = mock(JobQueueMapper.class);
    private final BeanFactory beanFactory = mock(BeanFactory.class);
    private final JobQueueScheduler underTest = new JobQueueScheduler(mapper, beanFactory);

    private static JobQueueEntry entry(long id) {
        return new JobQueueEntry(id, false, "", null, null, null, null, 0, Object.class, "unknownMethod", new Class<?>[0], new String[0], "admin");
    }

    @Test
    void runsQueuedJobsUntilTheQueueIsEmpty() {
        doReturn(new Object()).when(beanFactory).getBean(any(Class.class));
        when(mapper.getNext()).thenReturn(entry(1), entry(2), entry(3), null);

        underTest.retrieveJobFromJobQueue();

        // all three jobs in one run, not one per interval
        verify(mapper, times(4)).getNext();
        verify(mapper, times(3)).delete(any());
    }

    @Test
    void anEmptyQueueStopsImmediately() {
        underTest.retrieveJobFromJobQueue();

        verify(mapper, times(1)).getNext();
        verify(mapper, never()).delete(any());
    }
}
