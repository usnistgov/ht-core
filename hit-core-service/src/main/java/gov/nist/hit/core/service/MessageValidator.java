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
package gov.nist.hit.core.service;

import gov.nist.hit.core.domain.MessageValidationCommand;
import gov.nist.hit.core.domain.MessageValidationResult;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.service.exception.MessageValidationException;

public interface MessageValidator {

  /**
   * 
   * @param testContext
   * @param command
   * @return
   * @throws MessageValidationException
   */
  public MessageValidationResult validate(TestContext testContext, MessageValidationCommand command)
      throws MessageValidationException;

  /**
   * 
   * @return
   */
  public String getValidationServiceName();

  /**
   * 
   * @return
   */
  public String getProviderName();

}
