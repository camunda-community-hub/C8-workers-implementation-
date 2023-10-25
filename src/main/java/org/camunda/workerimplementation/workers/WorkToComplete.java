package org.camunda.workerimplementation.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.workerimplementation.monitor.MonitorWorker;

public class WorkToComplete {

  /**
   * All workers call this method to realize the work
   */
  public void executeJob(JobHandler job, ActivatedJob activatedJob, MonitorWorker monitorWorker) {

    monitorWorker.startExecution(job);
    int timeToExecute = (int) activatedJob.getVariablesAsMap().get(ProcessVariables.TIME_TO_EXECUTE);
    // waits timeToExecute millisecond
    try {

      Thread.sleep(timeToExecute);
    } catch (InterruptedException e) {
      // nothing to do
    }
    monitorWorker.stopExecution(job);
  }
}
