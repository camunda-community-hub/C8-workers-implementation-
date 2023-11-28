package org.camunda.workerimplementation.monitor;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.workerimplementation.workers.WorkToComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@EnableScheduling
@PropertySource("classpath:application.yaml")

public class MonitorWorker {

  Logger logger = LoggerFactory.getLogger(MonitorWorker.class);

  @Value("${workerapplication.monitor.logjobs:false}")
  private Boolean logJobs;

  @Value("${workerapplication.monitor.registerjobs:false}")
  private Boolean registerJobs;

  /**
   * just here to ensure the synchronization between thread
   */
  final static Boolean atomic = Boolean.TRUE;
  final static Boolean atomicJobs = Boolean.TRUE;

  Map<String, Integer> handler = new HashMap<>();
  Map<String, Integer> execution = new HashMap<>();

  Map<String, Long> registerExecution = new HashMap<>();
  /**
   * Register a job. Goal is to detect any double, two time execution
   */
  Map<Long, List<TrackJob>> jobs = new HashMap<>();

  private int totalExecutions = 0;
  private long beginCampaign = 0;
  private long endCampaign = 0;
  private long cumulExecutionTime = 0;
  private int nbThreadsCampaign = 1;

  public void setThreadsCampaign(int nbThreadsCampaign) {
    this.nbThreadsCampaign = nbThreadsCampaign == 0 ? 1 : nbThreadsCampaign;
  }

  public void clearCampaign() {
    beginCampaign = 0;
  }

  public boolean isCampaignActive() {
    return beginCampaign != 0;
  }

  public boolean isCampaignFinished() {
    return beginCampaign != 0 && endCampaign != 0;
  }

  /**
   * beginCampaign
   * register the beginning of the campaign to calculate the efficency
   */
  public synchronized long beginCampaign() {
    beginCampaign = System.currentTimeMillis();
    cumulExecutionTime = 0;
    endCampaign = 0;
    totalExecutions = 0;
    jobs.clear();
    return beginCampaign;
  }

  /**
   * endCampaign
   * register the end of the campaign to calculate the efficiency
   */
  public synchronized long endCampaign() {
    endCampaign = System.currentTimeMillis();
    return endCampaign;
  }

  public long getDurationCampaign() {
    return endCampaign - beginCampaign;
  }

  /**
   * StartHandle
   * register when the worker handle the work (start of the handle method)
   *
   * @param jobHandler job Handler
   * @param activatedJob
   */
  public void startHandle(JobHandler jobHandler, ActivatedJob activatedJob) {
    if (Boolean.TRUE.equals(logJobs))
       logger.info("## StartHandle Job [" + activatedJob.getKey() + "]");

    int current;
    synchronized (atomic) {
      totalExecutions++;
      current = handler.getOrDefault(jobHandler.getClass().getSimpleName(), 0);
      current++;
      handler.put(jobHandler.getClass().getSimpleName(), current);
    }
    log("StartHandle :" + current + " t:" + Thread.currentThread().getName());
  }

  /**
   * StopHandle
   * register when the worker finish the handle : the execution is maybe not already done.
   * In Classical, execution is done, but not in the ThreadExecutor
   *
   * @param jobHandler job Handler
   * @param activatedJob job submitted
   */
  public synchronized void stopHandle(JobHandler jobHandler, ActivatedJob activatedJob) {
    int current;
    synchronized (atomic) {
      current = handler.getOrDefault(jobHandler.getClass().getSimpleName(), 0);
      current--;
      handler.put(jobHandler.getClass().getSimpleName(), current);
    }
    log("StopHandle :" + current + " t:" + Thread.currentThread().getName());

  }

  /**
   * Register a job. The goal is to detect if a job is suplied multiple time
   *
   * @param activatedJob job submitted
   * @return true if the job was already submitted
   */
  public boolean registerJob(ActivatedJob activatedJob) {
    if (Boolean.TRUE.equals(registerJobs)) {
      synchronized (atomicJobs) {
        long currentTime = System.currentTimeMillis();
        if (jobs.containsKey(activatedJob.getKey())) {
          List<TrackJob> alreadyRegisteredList = jobs.get(activatedJob.getKey());
          TrackJob lastTrackJob = alreadyRegisteredList.get(alreadyRegisteredList.size() - 1);
          logger.error(">>>>> Job [" + activatedJob.getKey() + "] already registered " + alreadyRegisteredList.size() + " time,"
              + " last submission was " + (currentTime - lastTrackJob.submissionTime) + " ms ago, last job executed? " + (lastTrackJob.executionTime > 0) + (
              lastTrackJob.executionTime > 0 ?
                  (" and was executed in " + (lastTrackJob.executionTime - lastTrackJob.submissionTime) + " ms") :
                  ""));
          alreadyRegisteredList.add(new TrackJob(currentTime));
          jobs.put(activatedJob.getKey(), alreadyRegisteredList);
          return true;
        } else {
          ArrayList list = new ArrayList();
          list.add(new TrackJob(currentTime));
          jobs.put(activatedJob.getKey(), list);
          return false;
        }
      }
    }

    return false;
  }
  public void startExecuteJob(ActivatedJob activatedJob) {
    if (Boolean.TRUE.equals(logJobs))
      logger.info("## Start Job [" + activatedJob.getKey() + "]");

  }
  public void executeJob(ActivatedJob activatedJob) {
    if (Boolean.TRUE.equals(logJobs))
      logger.info("## execute Job [" + activatedJob.getKey() + "]");

    if (Boolean.TRUE.equals(registerJobs)) {
      synchronized (atomicJobs) {
        if (jobs.containsKey(activatedJob.getKey())) {
          List<TrackJob> alreadyRegisteredList = jobs.get(activatedJob.getKey());
          TrackJob lastTrackJob = alreadyRegisteredList.get(alreadyRegisteredList.size() - 1);
          lastTrackJob.executionTime = System.currentTimeMillis();
        } else {
          logger.error(">>>>> Job [" + activatedJob.getKey() + "] is not registered !");
        }
      }
    }
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
        "Monitor: Total Exec.:" + totalExecutions + " Efficiency: " + getEfficiency() + " %, InstantEfficiency: "
            + instantEfficiency + " % Threads:" + workerInExecution + " HandleWorker: " + describeRegister(handler)
            + " Execution:" + describeRegister(execution));
  }

  public int getEfficiency() {
    int efficiency = -1;
    if (beginCampaign != 0) {
      long markerEndCampaign = endCampaign == 0 ? System.currentTimeMillis() : endCampaign;
      long durationCampaign = markerEndCampaign - beginCampaign;
      if (durationCampaign != 0)
        efficiency = (int) (100.0 * cumulExecutionTime / nbThreadsCampaign / durationCampaign);
    }
    return efficiency;
  }

  public String getSynthesis() {
    return "Duration " + getDurationCampaign() + " ms, Efficiency: " + getEfficiency() + " %";
  }

  private String describeRegister(Map<String, Integer> register) {
    return register.entrySet().stream().map(t -> {
      return "(" + t.getKey() + ":" + t.getValue() + ")";
    }).collect(Collectors.joining(","));

  }

  private void log(String message) {
    logger.debug(message);
  }

  private class TrackJob {
    public long submissionTime = 0;
    public long executionTime = 0;

    public TrackJob(long submissionTime) {
      this.submissionTime = submissionTime;
    }
  }
}
