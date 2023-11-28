package org.camunda.workerimplementation.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.workerimplementation.WorkerConfig;
import org.camunda.workerimplementation.monitor.MonitorWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

public class ThreadTokenWorker implements JobHandler {

  private final MonitorWorker monitorWorker;
  private final Semaphore semaphore;
  Logger logger = LoggerFactory.getLogger(ThreadTokenWorker.class);

  public ThreadTokenWorker(WorkerConfig workerConfig, MonitorWorker monitorWorker) {
    this.monitorWorker = monitorWorker;
    semaphore = new Semaphore(workerConfig.getNumberOfSemaphores());
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob activatedJob) throws Exception {
    logger.debug("------------- Worker: ThreadTokenWorker " + Thread.currentThread().getName());
    monitorWorker.startHandle(this,activatedJob);
    if (monitorWorker.registerJob(activatedJob)) {
      // already registered: so ignore it
      monitorWorker.stopHandle(this, activatedJob);
      return;
    }
    // We ask for a token, then a limited number of threads can continue after, else the worker waits
    // doing that, if there is no more token, Zeebe Client waits and do not ask again a new batch
    try {
      semaphore.acquire();
    } catch (Exception e) {
      return;
    }
    // Now, we get a token, do the job, and release the ZeebeClient thread
    // The thread must release the token at the end
    doWorkInDifferentThread(jobClient, activatedJob);

    monitorWorker.stopHandle(this, activatedJob);
  }

  private void doWorkInDifferentThread(JobClient jobClient, ActivatedJob activatedJob) {
    Thread thread = new Thread(() -> {
      monitorWorker.startExecuteJob(activatedJob);

      WorkToComplete workToComplete = new WorkToComplete();
      workToComplete.executeJob(this, activatedJob, monitorWorker);
      jobClient.newCompleteCommand(activatedJob.getKey()).send().join();
      monitorWorker.executeJob(activatedJob);

        semaphore.release();

    });
    thread.start();

  }
}
