package org.camunda.workerimplementation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@org.springframework.context.annotation.Configuration
@PropertySource("classpath:application.yaml")
@Component
public class WorkerConfig {

  @Value("${zeebe.gatewayaddress:127.0.0.1:26500}")
  private String zeebeBrokerGatewayAddress;

  @Value("${zeebe.worker.homegeneousWorker:true}")
  private boolean homegeneousWorker=true;

  @Value("${zeebe.worker.jobsactive:100}")
  private int numberOfJobsActive = 10;
  @Value("${zeebe.worker.semaphore:100}")
  private int numberOfSemaphores = 10;
  @Value("${zeebe.worker.sizelist:100}")
  private Integer sizeOfTheList;


  public int getNumberOfJobsActive() {
    return numberOfJobsActive;
  }

  public int getSizeOfTheList() {
    return sizeOfTheList == null ? 100 : sizeOfTheList;
  }

  public String getZeebeBrokerAddress() {
    return zeebeBrokerGatewayAddress;
  }

  public boolean getHomogeneousWorker() {
    return homegeneousWorker;
  }


  public int getNumberOfSemaphores() {
    return numberOfSemaphores;
  }
}
