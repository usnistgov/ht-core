/**
 * This software was developed at the National Institute of Standards and Technology by employees of
 * the Federal Government in the course of their official duties. Pursuant to title 17 Section 105
 * of the United States Code this software is not subject to copyright protection and is in the
 * public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its
 * use by other parties, and makes no guarantees, expressed or implied, about its quality,
 * reliability, or any other characteristic. We would appreciate acknowledgement if the software is
 * used. This software can be redistributed and/or modified freely provided that any derivative
 * works bear some notice that they are derived from it, and any modified versions bear some notice
 * that they have been modified.
 */

package gov.nist.hit.core.api;

import gov.nist.hit.core.domain.Stage;
import gov.nist.hit.core.domain.TestCase;
import gov.nist.hit.core.domain.TestPlan;
import gov.nist.hit.core.domain.TestStep;
import gov.nist.hit.core.repo.TestCaseRepository;
import gov.nist.hit.core.repo.TestPlanRepository;
import gov.nist.hit.core.repo.TestStepRepository;
import gov.nist.hit.core.service.TestCaseService;
import gov.nist.hit.core.service.TestPlanService;
import gov.nist.hit.core.service.TestStepService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Harold Affo (NIST)
 */
@RequestMapping("/cb")
@RestController
public class CBTestingController {

  static final Logger logger = LoggerFactory.getLogger(CBTestingController.class);

  @Autowired
  private TestPlanService testPlanService;
  
  @Autowired
  private TestCaseService testCaseService;
  
  @Autowired
  private TestStepService testStepService;
  


  @Cacheable(value = "testCaseCache", key = "'cb-testcases'")
  @RequestMapping(value = "/testcases", method = RequestMethod.GET)
  public List<TestPlan> testCases() {
    logger.info("Fetching all testCases...");
    List<TestPlan> testPlans = testPlanService.findAllByStage(Stage.CB);
    return testPlans;
  }

  @RequestMapping(value = "/testcases/{testCaseId}", method = RequestMethod.GET)
  public TestCase testCase(@PathVariable final Long testCaseId) {
    logger.info("Fetching  test case...");
    TestCase testCase = testCaseService.findOne(testCaseId);
    return testCase;
  }

  @RequestMapping(value = "/teststeps/{testStepId}", method = RequestMethod.GET)
  public TestStep testStep(@PathVariable final Long testStepId) {
    logger.info("Fetching  test step...");
    TestStep testStep = testStepService.findOne(testStepId);
    return testStep;
  }


}