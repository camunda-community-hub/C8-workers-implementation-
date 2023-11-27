package org.camunda.workerimplementation.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.workerimplementation.WorkerConfig;
import org.camunda.workerimplementation.monitor.MonitorWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ClassicalWorker implements JobHandler {

  private final MonitorWorker monitorWorker;
  Logger logger = LoggerFactory.getLogger(ClassicalWorker.class);

  public ClassicalWorker(WorkerConfig workerConfig, MonitorWorker monitorWorker) {
    this.monitorWorker = monitorWorker;
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob activatedJob) throws Exception {

    logger.debug("------------- Worker: ClassicalWorker " + Thread.currentThread().getName());

    monitorWorker.startHandle(this, activatedJob);

    // We do the job now
    WorkToComplete workToComplete = new WorkToComplete();
    workToComplete.executeJob(this, activatedJob, monitorWorker);

    // Complete the Job
    Map<String, Object> variables = new HashMap<>();
    variables.put("result", System.currentTimeMillis());
    jobClient.newCompleteCommand(activatedJob.getKey()).variables(variables).send().join();
    monitorWorker.stopHandle(this, activatedJob);
  }
}