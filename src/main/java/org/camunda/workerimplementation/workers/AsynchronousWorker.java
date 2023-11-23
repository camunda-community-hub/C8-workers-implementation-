package org.camunda.workerimplementation.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.workerimplementation.WorkerConfig;
import org.camunda.workerimplementation.monitor.MonitorWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsynchronousWorker implements JobHandler {

  private final MonitorWorker monitorWorker;
  Logger logger = LoggerFactory.getLogger(AsynchronousWorker.class);

  public AsynchronousWorker(WorkerConfig workerConfig, MonitorWorker monitorWorker) {
    this.monitorWorker = monitorWorker;
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob activatedJob) throws Exception {

    logger.debug("------------- Worker: AsynchronousWorker");

    monitorWorker.startHandle(this);
    jobClient.newCompleteCommand(activatedJob.getKey()).send().join();
    monitorWorker.stopHandle(this);

    // Job will be executed here, in a different thread
    doWorkInDifferentThread(jobClient, activatedJob);
  }

  private void doWorkInDifferentThread(JobClient jobClient, ActivatedJob activatedJob) {
    Thread thread = new Thread(() -> {
      WorkToComplete workToComplete = new WorkToComplete();
      workToComplete.executeJob(this, activatedJob, monitorWorker);
    });
    thread.start();
  }
}