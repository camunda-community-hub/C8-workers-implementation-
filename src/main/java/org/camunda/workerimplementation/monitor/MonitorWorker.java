package org.camunda.workerimplementation.monitor;

import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.workerimplementation.workers.WorkToComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@EnableScheduling
public class MonitorWorker {

  Logger logger = LoggerFactory.getLogger(MonitorWorker.class);

  Map<String, Integer> handler = new HashMap<>();
  Map<String, Integer> execution = new HashMap<>();

  Map<String, Long> registerExecution = new HashMap<>();

  private int totalExecutions = 0;

  private long beginCampaign = 0;
  private long endCampaign = 0;
  private long cumulExecutionTime = 0;
  private int nbThreadsCampaign = 1;

  public void setThreadsCampaign(int nbThreadsCampaign) {
    this.nbThreadsCampaign = nbThreadsCampaign == 0 ? 1 : nbThreadsCampaign;
  }

  /**
   * beginCampaign
   * register the beginning of the campaign to calculate the efficency
   */
  public synchronized void beginCampaign() {
    beginCampaign = System.currentTimeMillis();
    cumulExecutionTime = 0;
    endCampaign = 0;
    totalExecutions = 0;

  }

  /**
   * endCampaign
   * register the end of the campaign to calculate the efficency
   */
  public synchronized void endCampaign() {
    endCampaign = System.currentTimeMillis();
  }

  /**
   * StartHandle
   * register when the worker handle the work (start of the handle method)
   *
   * @param jobHandler job Handler
   */
  public synchronized void startHandle(JobHandler jobHandler) {
    totalExecutions++;
    int current = handler.getOrDefault(jobHandler.getClass().getSimpleName(), 0);
    current++;
    handler.put(jobHandler.getClass().getSimpleName(), current);
    log("StartHandle :" + current + " t:" + Thread.currentThread().getName());
  }

  /**
   * StopHandle
   * register when the worker finish the handle : the execution is maybe not already done.
   * In Classical, execution is done, but not in the ThreadExecutor
   *
   * @param jobHandler job Handler
   */
  public synchronized void stopHandle(JobHandler jobHandler) {
    int current = handler.getOrDefault(jobHandler.getClass().getSimpleName(), 0);
    current--;
    handler.put(jobHandler.getClass().getSimpleName(), current);
    log("StopHandle :" + current + " t:" + Thread.currentThread().getName());

  }

  public synchronized void startExecution(JobHandler jobHandler) {
    registerExecution.put(Thread.currentThread().getName(), System.currentTimeMillis());
    int current = execution.getOrDefault(jobHandler.getClass().getSimpleName(), 0);
    current++;
    execution.put(jobHandler.getClass().getSimpleName(), current);
    log("StartExecution :" + current + " t:" + Thread.currentThread().getName());
  }

  public synchronized void stopExecution(JobHandler jobHandler) {
    Long startExecution = registerExecution.getOrDefault(Thread.currentThread().getName(), 0L);
    if (startExecution != 0) {
      cumulExecutionTime += System.currentTimeMillis() - startExecution;
    }
    int current = execution.getOrDefault(jobHandler.getClass().getSimpleName(), 0);
    current--;
    execution.put(jobHandler.getClass().getSimpleName(), current);
    log("StopExecution :" + current + " t:" + Thread.currentThread().getName());
  }

  /**
   * monitor
   */
  @Scheduled(fixedDelay = 15000)
  public void monitor() {
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    int workerInExecution = 0;
    for (ThreadInfo threadInfo : threadMXBean.dumpAllThreads(true, true)) {
      log("  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ " + threadInfo.getThreadName());
      boolean isAWorker = false;
      for (StackTraceElement oneLevel : threadInfo.getStackTrace()) {
        log(" " + oneLevel.getClassName());
        if (oneLevel.getClassName().equals(WorkToComplete.class.getName())) {
          isAWorker = true;
        }
      }
      if (isAWorker) {
        workerInExecution++;
        log("  WORKER DETECTED " + threadInfo.getThreadName());

      }
      log("  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ " + threadInfo.getThreadName());
    }

    // calculate the efficiency
    int instantEfficiency = (int) (100.0 * workerInExecution / nbThreadsCampaign);

    logger.info(
        "Monitor: Total Exec.:" + totalExecutions + " Efficiency: " + calculateEfficiency() + " %, InstantEfficiency: "
            + instantEfficiency + " % Threads:" + workerInExecution + " HandleWorker: " + describeRegister(handler)
            + " Execution:" + describeRegister(execution));
  }

  public int calculateEfficiency() {
    int efficiency = -1;
    if (beginCampaign != 0) {
      long markerEndCampaign = endCampaign == 0 ? System.currentTimeMillis() : endCampaign;
      long durationCampaign = markerEndCampaign - beginCampaign;
      if (durationCampaign != 0)
        efficiency = (int) (100.0 * cumulExecutionTime / nbThreadsCampaign / durationCampaign);
    }
    return efficiency;
  }

  public String calculateSynthesis() {
    return "Efficiency: " + calculateEfficiency() + " %";
  }

  private String describeRegister(Map<String, Integer> register) {
    return register.entrySet().stream().map(t -> {
      return "(" + t.getKey() + ":" + t.getValue() + ")";
    }).collect(Collectors.joining(","));

  }

  private void log(String message) {
    logger.debug(message);
  }

}
