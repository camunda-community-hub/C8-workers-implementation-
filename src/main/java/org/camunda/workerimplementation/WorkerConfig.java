package org.camunda.workerimplementation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;

@org.springframework.context.annotation.Configuration
@PropertySource("classpath:application.yaml")
@Component
public class WorkerConfig {

  @Value("${zeebe.gatewayaddress:127.0.0.1:26500}")
  private String zeebeBrokerGatewayAddress;

  @Value("${zeebe.worker.homegeneousWorker:true}")
  private boolean homegeneousWorker = true;

  /**
   * If define, overlap the configuration value
   */
  private Boolean overlapHomogeneous = null;

  @Value("${zeebe.worker.jobsactive:100}")
  private int numberOfJobsActive = 10;
  @Value("${zeebe.worker.semaphore:100}")
  private int numberOfSemaphores = 10;
  @Value("${zeebe.worker.sizelist:100}")
  private Integer sizeOfTheList;

  @Value("${workerapplication.runTests}")
  private Boolean runTests;

  @Value("#{'${workerapplication.runListTests}'.split(',')}") // Split the string into a list
  private List<String> runListTests;

  @Value("${workerapplication.runModeStream}")
  private String runModeStream;
  @Value("${workerapplication.runModeHeterogeneous}")
  private String runModeHeterogeneous;

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
    return (overlapHomogeneous != null ? overlapHomogeneous : homegeneousWorker);
  }

  /**
   * Overlap the value
   *
   * @param overlapHomogeneous the value, null to stop overlaping
   */
  public void setOverlapHomogeneousWorker(Boolean overlapHomogeneous) {
    this.overlapHomogeneous = overlapHomogeneous;
  }

  public int getNumberOfSemaphores() {
    return numberOfSemaphores;
  }

  public boolean runTests() {
    return Boolean.TRUE.equals(runTests);
  }

  public List<String> getRunListTests() {
    return runListTests;
  }

  public boolean runNoStreamTest() {
    return "NOSTREAM".equalsIgnoreCase(runModeStream) || "ALL".equalsIgnoreCase(runModeStream);
  }

  public boolean runStreamTest() {
    return "STREAM".equalsIgnoreCase(runModeStream) || "ALL".equalsIgnoreCase(runModeStream);
  }

  public boolean runHeterogeneousTest() {
    return "HETEROGENEOUS".equalsIgnoreCase(runModeHeterogeneous) || "ALL".equalsIgnoreCase(runModeHeterogeneous);
  }

  public boolean runHomogeneousTest() {
    return "HOMOGENEOUS".equalsIgnoreCase(runModeHeterogeneous) || "ALL".equalsIgnoreCase(runModeHeterogeneous);
  }

}
