package org.edu_sharing.repository.server.jobs;

import jakarta.servlet.*;
import org.edu_sharing.repository.server.jobs.JobQueueContextHolder;

import java.io.IOException;

public class JobQueueContextCleanupFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      chain.doFilter(request, response);
    } finally {
      JobQueueContextHolder.clear();
    }
  }
}
