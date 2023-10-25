package org.camunda.workerimplementation;


import io.camunda.operate.exception.OperateException;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.worker.JobWorker;
import org.camunda.workerimplementation.monitor.MonitorWorker;
import org.camunda.workerimplementation.workers.AsynchronousWorker;
import org.camunda.workerimplementation.workers.CalculateExecutionWorker;
import org.camunda.workerimplementation.workers.ClassicalWorker;
import org.camunda.workerimplementation.workers.SetListWorker;
import org.camunda.workerimplementation.workers.ThreadTokenWorker;
import org.camunda.workerimplementation.workers.ThreadWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;

@SpringBootApplication
@ConfigurationPropertiesScan
@Configuration
@PropertySource("classpath:application.yaml")

public class WorkerApplication {

  @Autowired
  public WorkerConfig workerConfig;
  @Autowired
  public MonitorWorker monitorWorker;
  Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

  public static void main(String[] args)  {

    SpringApplication.run(WorkerApplication.class, args);
  }

  @PostConstruct
  public void init() {

    // thanks to Spring, the class CherryJobRunnerFactory is active. All runners (worker, connectors) start then

    logger.info("Start WorkerApplication with maxJobActive[" + workerConfig.getNumberOfJobsActive() + "] threads["
        + workerConfig.getNumberOfJobsActive() + "]");
    ZeebeClient client = ZeebeClient.newClientBuilder()
        .gatewayAddress(workerConfig.getZeebeBrokerAddress())
        .usePlaintext()
        .defaultJobWorkerMaxJobsActive(workerConfig.getNumberOfJobsActive())
        .numJobWorkerExecutionThreads(workerConfig.getNumberOfJobsActive())
        .build();

    monitorWorker.setThreadsCampaign(workerConfig.getNumberOfJobsActive());


    final JobWorker setlistWorker = client.newWorker()
        .jobType("setlist-worker")
        .handler(new SetListWorker(workerConfig, monitorWorker))
        .open();

    final JobWorker synchronousWorker = client.newWorker()
        .jobType("classical-worker")
        .handler(new ClassicalWorker(workerConfig, monitorWorker))
        .open();

    final JobWorker threadWorker = client.newWorker()
        .jobType("thread-worker")
        .handler(new ThreadWorker(workerConfig, monitorWorker))
        .open();

    final JobWorker threadTokenWorker = client.newWorker()
        .jobType("thread-token-worker")
        .handler(new ThreadTokenWorker(workerConfig, monitorWorker))
        .open();

    final JobWorker asynchronousWorker = client.newWorker()
        .jobType("asynchronous-worker")
        .handler(new AsynchronousWorker(workerConfig, monitorWorker))
        .open();

    final JobWorker calculationWorker = client.newWorker()
        .jobType("calculation-worker")
        .handler(new CalculateExecutionWorker(workerConfig, monitorWorker))
        .open();

    // monitorWorker.monitor();

  }
  // https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

}
