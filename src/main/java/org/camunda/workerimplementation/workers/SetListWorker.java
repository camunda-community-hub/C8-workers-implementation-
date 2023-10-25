package org.camunda.workerimplementation.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.workerimplementation.WorkerConfig;
import org.camunda.workerimplementation.monitor.MonitorWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetListWorker implements JobHandler {

  private final WorkerConfig workerConfig;
  private final MonitorWorker monitorWorker;
  Logger logger = LoggerFactory.getLogger(SetListWorker.class);

  public SetListWorker(WorkerConfig workerConfig, MonitorWorker monitorWorker) {
    this.workerConfig = workerConfig;
    this.monitorWorker = monitorWorker;
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob job) throws Exception {

    String typeWorker = (String) job.getVariablesAsMap().get(ProcessVariables.TYPE_WORKER);

    logger.info("------------- Worker: setListWorker ["+typeWorker+"]");
    monitorWorker.beginCampaign();
    int totalTime = workerConfig.getSizeOfTheList() * 5000;
    int sumOfTime = 0;

    // in case of an heteregenous time, we want to keep the same total amount of time
    List<Integer> listValues = new ArrayList<>();
    for (int i = 0; i < workerConfig.getSizeOfTheList(); i++) {
      if (workerConfig.getHomogeneousWorker())
        listValues.add(5000);
      else {
        int stepTime = 2500 + (i * 500) % 10000;
        if (sumOfTime + stepTime > totalTime) {
          stepTime = totalTime - sumOfTime;
        }
        if (i == workerConfig.getSizeOfTheList() - 1) {
          // set the relicat
          stepTime = totalTime - sumOfTime;
        }

        listValues.add(stepTime);
        sumOfTime += stepTime;
      }
    }
    // double check
    Integer sum = listValues.stream().reduce(0, Integer::sum);
    if (sum != totalTime) {
      logger.error("Sum is different !");
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put(ProcessVariables.LIST_VALUES, listValues);
    variables.put(ProcessVariables.START_TIME, System.currentTimeMillis());
    // Complete the Job
    jobClient.newCompleteCommand(job.getKey()).variables(variables).send().join();
  }
}