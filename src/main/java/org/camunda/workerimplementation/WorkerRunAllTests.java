package org.camunda.workerimplementation;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.camunda.workerimplementation.monitor.MonitorWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WorkerRunAllTests {

  @Autowired
  MonitorWorker monitorWorker;

  @Autowired
  WorkerConfig workerConfig;

  Logger logger = LoggerFactory.getLogger(WorkerRunAllTests.class);

  /**
   * Run all test
   */
  public void run(ZeebeClient zeebeClient) {

    List<String> listTests = workerConfig.getRunListTests();
    List<Result> listResult = new ArrayList<>();
    for (String testName : listTests) {
      for (int i = 0; i < 2; i++) {
        // i=0 : Stream Test, i==1: noStream
        boolean isStream = (i == 0);
        if (i == 0 && !workerConfig.runStreamTest())
          continue;

        if (i == 1 && !workerConfig.runNoStreamTest())
          continue;

        for (int j = 0; j < 2; j++) {
          // j=0 : Homogeneous Test, i==1: Heterogeneous
          boolean isHomogeneous = (j == 0);
          if (j == 0 && !workerConfig.runHomogeneousTest())
            continue;
          if (j == 1 && !workerConfig.runHeterogeneousTest())
            continue;

          listResult.add(runOneTest(zeebeClient, testName, isStream, isHomogeneous));

          // log temporary result
          logResult(listResult);

        }
      }

    }

    // publish synthesis
    logResult(listResult);
  }

  public Result runOneTest(ZeebeClient zeebeClient, String testName, boolean isStream, boolean isHomogeneous) {
    Result result = new Result();
    result.testName = testName;
    result.isStream = isStream;
    result.isHomogeneous = isHomogeneous;
    try {
      // Deploy the process
      String processNameId = testName + (isStream ? "Stream" : "") + "Worker";
      String resourcePath = processNameId + ".bpmn";

      logger.info("------------------------------------ Run process[" + processNameId + "]  Stream " + isStream
          + " Homogeneous " + isHomogeneous);
      // Create a ClassPathResource
      Resource resource = new ClassPathResource(resourcePath);

      // Get the path of the resource
      String path = resource.getURL().getPath();
      logger.info("Deploy " + path);
      zeebeClient.newDeployResourceCommand().addResourceFile(path).send().join();

      // Set up the environment
      workerConfig.setOverlapHomogeneousWorker(isHomogeneous);
      monitorWorker.clearCampaign();

      // Create one process instance

      final ProcessInstanceEvent processInstanceEvent = zeebeClient.newCreateInstanceCommand()
          .bpmnProcessId(processNameId)
          .latestVersion()
          .send()
          .join();

      logger.info("ProcessInstance created [" + processInstanceEvent.getProcessInstanceKey()+"]");

      // wait for the end of the process instance
      while (!monitorWorker.isCampaignFinished()) {
        Thread.sleep(10 * 1000);
      }

      // collect the result
      result.error = null;
      result.totalDuration = monitorWorker.getDurationCampaign();
      result.efficiency = monitorWorker.getEfficiency();
      logger.info("------------------------------------ End " + processNameId + "Stream " + isStream + " Homogeneous "
          + isHomogeneous + " " + monitorWorker.getSynthesis());

    } catch (Exception e) {
      result.error = e.toString();

      logger.error(
          "Error during executing test " + testName + " isStream " + isStream + " isHomogeneous " + isHomogeneous + " "
              + e);
    }
    return result;
  }

  /*
  LogResult
   */
  private void logResult(List<Result> listResult) {
    // publish synthesis
    logger.info(Result.getHeader(false));
    logger.info(Result.getHeader(true));
    for (Result result : listResult) {
      logger.info(result.getSynthesis());
    }
  }

  private class Result {
    String testName;
    boolean isStream;
    boolean isHomogeneous;

    String error;
    long totalDuration;
    long efficiency;

    public static String getHeader(boolean separatorLine) {
      if (separatorLine)
        return "|--------------------------------|---------|---------------|-----------:|-----------:|----------------------|";
      return "| " + format("Worker", 30, false) // testName
          + " | " + format("Stream?", 7, false) // Stream
          + " | " + format("Execution", 13, false) // execution
          + " | " + format("Total time", 10, false)// total time
          + " | " + format("Efficiency", 10, false)// efficiency
          + " | " + format("Error", 20, false) // error
          + " |";
    }

    private static String format(String value, int number, boolean rightJustification) {

      // too large, but we don't want to loose value
      if (value.length() > number)
        return value;
      if (rightJustification) {
        int missingSpace = number - value.length();
        return "                       ".substring(0, missingSpace) + value;
      }
      return (value + "                            ").substring(0, number);
    }

    public String getSynthesis() {
      return "| " + format(testName, 30, false) // test name
          + " | " + (isStream ? "Stream " : "       ") // duration
          + " | " + (isHomogeneous ? "Homogeneous  " : "Heterogeneous") // Homogeneous or Heterogeneous
          + " | " + format(String.format("%,10d", totalDuration), 10, true) // duration
          + " | " + format(String.format("%5d", efficiency) + " %", 10, true) // efficiency
          + " | " + format((error == null ? "" : error), 20, false) // error
          + " | ";
    }
  }
}
