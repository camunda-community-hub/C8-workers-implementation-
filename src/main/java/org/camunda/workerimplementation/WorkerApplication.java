package org.camunda.workerimplementation;

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
import java.time.Duration;

@SpringBootApplication
@ConfigurationPropertiesScan
@Configuration
@PropertySource("classpath:application.yaml")

public class WorkerApplication {

  @Autowired
  public WorkerConfig workerConfig;
  @Autowired
  public MonitorWorker monitorWorker;

  @Autowired
  public WorkerRunAllTests workerRunAllTests;

  Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

  public static void main(String[] args) {

    SpringApplication.run(WorkerApplication.class, args);
  }

  @PostConstruct
  public void init() {

    // thanks to Spring, the class CherryJobRunnerFactory is active. All runners (worker, connectors) start then

    logger.info("Start WorkerApplication with maxJobActive[" + workerConfig.getNumberOfJobsActive() + "] threads["
        + workerConfig.getNumberOfJobsActive() + "]");
    ZeebeClient zeebeClient = ZeebeClient.newClientBuilder()
        .gatewayAddress(workerConfig.getZeebeBrokerAddress())
        .usePlaintext()
        .defaultJobWorkerMaxJobsActive(workerConfig.getNumberOfJobsActive())
        .numJobWorkerExecutionThreads(workerConfig.getNumberOfJobsActive())
        .build();

    monitorWorker.setThreadsCampaign(workerConfig.getNumberOfJobsActive());

    zeebeClient.newWorker()
        .jobType("setlist-worker")
        .handler(new SetListWorker(workerConfig, monitorWorker))
        .timeout(Duration.ofMinutes(1))
        .open();


    zeebeClient.newWorker()
        .jobType("classical-worker")
        .handler(new ClassicalWorker(workerConfig, monitorWorker))
        .timeout(Duration.ofMinutes(1))
        .open();
    zeebeClient.newWorker()
        .jobType("classical-stream-worker")
        .handler(new ClassicalWorker(workerConfig, monitorWorker))
        .timeout(Duration.ofMinutes(1))
        .streamEnabled(true)
        .open();


    zeebeClient.newWorker()
        .jobType("thread-worker")
        .handler(new ThreadWorker(workerConfig, monitorWorker))
        .timeout(Duration.ofMinutes(1))
        .open();
    zeebeClient.newWorker()
        .jobType("thread-stream-worker")
        .handler(new ThreadWorker(workerConfig, monitorWorker))
        .timeout(Duration.ofMinutes(1))
        .streamEnabled(true)
        .open();


    zeebeClient.newWorker()
        .jobType("thread-token-worker")
        .handler(new ThreadTokenWorker(workerConfig, monitorWorker))
        .timeout(Duration.ofMinutes(5))
        .open();
    zeebeClient.newWorker()
        .jobType("thread-token-stream-worker")
        .handler(new ThreadTokenWorker(workerConfig, monitorWorker))
        .timeout(Duration.ofMinutes(5))
        .streamEnabled(true)
        .open();


    zeebeClient.newWorker()
        .jobType("asynchronous-worker")
        .handler(new AsynchronousWorker(workerConfig, monitorWorker))
        .timeout(Duration.ofMinutes(1))
        .open();
    zeebeClient.newWorker()
        .jobType("asynchronous-stream-worker")
        .handler(new AsynchronousWorker(workerConfig, monitorWorker))
        .timeout(Duration.ofMinutes(1))
        .streamEnabled(true)
        .open();



    zeebeClient.newWorker()
        .jobType("calculation-worker")
        .handler(new CalculateExecutionWorker(workerConfig, monitorWorker))
        .timeout(Duration.ofMinutes(1))
        .open();

    if (workerConfig.runTests()) {
      workerRunAllTests.run(zeebeClient);
    }
    // monitorWorker.monitor();

  }
  // https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

}
