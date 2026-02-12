package org.camunda.workerimplementation;

import io.camunda.client.CamundaClient;
import org.camunda.workerimplementation.monitor.MonitorWorker;
import org.camunda.workerimplementation.workers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

@SpringBootApplication
@ConfigurationPropertiesScan
@Configuration
@PropertySource("classpath:application.yaml")
@EnableScheduling

public class WorkerApplication {


    @Autowired
    public WorkerConfig workerConfig;
    @Autowired
    public MonitorWorker monitorWorker;
    @Autowired
    public WorkerRunAllTests workerRunAllTests;
    Logger logger = LoggerFactory.getLogger(WorkerApplication.class);
    @Autowired
    private CamundaClient zeebeClient;

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }


    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        logger.info("Start WorkerApplication with maxJobActive[" + zeebeClient.getConfiguration().getNumJobWorkerExecutionThreads() + "] threads["
                + zeebeClient.getConfiguration().getDefaultJobWorkerMaxJobsActive() + "]");

        monitorWorker.setThreadsCampaign(zeebeClient.getConfiguration().getNumJobWorkerExecutionThreads());

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
