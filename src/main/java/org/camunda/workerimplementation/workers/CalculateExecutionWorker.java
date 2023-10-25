package org.camunda.workerimplementation.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.workerimplementation.WorkerConfig;
import org.camunda.workerimplementation.monitor.MonitorWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalculateExecutionWorker implements JobHandler {

  private final WorkerConfig workerConfig;
  private final MonitorWorker monitorWorker;
  Logger logger = LoggerFactory.getLogger(CalculateExecutionWorker.class);

  public CalculateExecutionWorker(WorkerConfig workerConfig,
                                  MonitorWorker monitorWorker) {
    this.workerConfig = workerConfig;
    this.monitorWorker = monitorWorker;
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob job) throws Exception {

    monitorWorker.endCampaign();

    String typeWorker = (String) job.getVariablesAsMap().get(ProcessVariables.TYPE_WORKER);
    Long startDate = (Long) job.getVariablesAsMap().get(ProcessVariables.START_TIME);

    long processInstanceKey = job.getProcessInstanceKey();

    long currentExecution = System.currentTimeMillis() - startDate;

    logger.info(
        "------------- Worker: calculateExecutionWorker PID[" + processInstanceKey + "] Type[" + typeWorker + "] in "
            + currentExecution + " ms " + monitorWorker.calculateSynthesis());

    // Complete the Job
    jobClient.newCompleteCommand(job.getKey()).send().join();
  }
}