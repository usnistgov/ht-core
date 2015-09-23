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

import gov.nist.hit.core.domain.Document;
import gov.nist.hit.core.domain.Message;
import gov.nist.hit.core.domain.Stage;
import gov.nist.hit.core.domain.TestArtifact;
import gov.nist.hit.core.domain.TestCase;
import gov.nist.hit.core.domain.TestCaseDocumentation;
import gov.nist.hit.core.domain.TestCaseGroup;
import gov.nist.hit.core.domain.TestObject;
import gov.nist.hit.core.domain.TestPlan;
import gov.nist.hit.core.repo.DocumentRepository;
import gov.nist.hit.core.repo.TestCaseDocumentationRepository;
import gov.nist.hit.core.repo.TestContextRepository;
import gov.nist.hit.core.repo.TestObjectRepository;
import gov.nist.hit.core.repo.TestPlanRepository;
import gov.nist.hit.core.repo.TestStepRepository;
import gov.nist.hit.core.service.exception.DownloadDocumentException;
import gov.nist.hit.core.service.exception.MessageException;
import gov.nist.hit.core.service.exception.TestCaseException;
import gov.nist.hit.core.service.exception.ValidationReportException;
import gov.nist.hit.core.service.util.ResourcebundleHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Harold Affo (NIST)
 * 
 */
@RequestMapping("/documentation")
@RestController
public class DocumentationController {

  static final Logger logger = LoggerFactory.getLogger(DocumentationController.class);

  @Autowired
  private TestPlanRepository testPlanRepository;

  @Autowired
  private TestCaseDocumentationRepository testCaseDocumentationRepository;

  @Autowired
  private DocumentRepository documentRepository;


  @Autowired
  private TestContextRepository testContextRepository;


  @RequestMapping(value = "/testcases", method = RequestMethod.GET)
  public TestCaseDocumentation testCases(@RequestParam("stage") Stage stage) {
    logger.info("Fetching " + stage + " test case documentation");
    TestCaseDocumentation doc = testCaseDocumentationRepository.findOneByStage(stage);
    return doc;
  }

  @RequestMapping(value = "/releasenotes", method = RequestMethod.GET)
  public List<Document> releaseNotes() {
    logger.info("Fetching  all release notes");
    return documentRepository.findAllReleaseNotes();
  }

  @RequestMapping(value = "/userdocs", method = RequestMethod.GET)
  public List<Document> userDocs() {
    logger.info("Fetching  all release notes");
    return documentRepository.findAllUserDocs();
  }

  @RequestMapping(value = "/knownissues", method = RequestMethod.GET)
  public List<Document> knownIssues() {
    logger.info("Fetching  all known issues");
    return documentRepository.findAllKnownIssues();
  }



  @RequestMapping(value = "/downloadDocument", method = RequestMethod.POST)
  public void downloadDocument(@RequestParam("path") String path, HttpServletRequest request,
      HttpServletResponse response) throws DownloadDocumentException {
    try {
      if (path != null) {
        String fileName = null;
        path = !path.startsWith("/") ? "/" + path : path;
        InputStream content = DocumentationController.class.getResourceAsStream(path);
        if (content != null) {
          fileName = path.substring(path.lastIndexOf("/") + 1);
          response.setContentType(getContentType(path));
          fileName = fileName.replaceAll(" ", "-");
          response.setHeader("Content-disposition", "attachment;filename=" + fileName);
          FileCopyUtils.copy(content, response.getOutputStream());
        }
      }
    } catch (IOException e) {
      logger.debug("Failed to download the test packages ");
      throw new DownloadDocumentException("Cannot download the test packages");
    }
  }



  @RequestMapping(value = "/testPackages", method = RequestMethod.POST)
  public void testPackages(@RequestParam("stage") Stage stage, HttpServletRequest request,
      HttpServletResponse response) throws DownloadDocumentException {
    try {
      InputStream stream = zipTestPackages(stage);
      response.setContentType("application/zip");
      response
          .setHeader("Content-disposition", "attachment;filename=" + stage + "TestPackages.zip");
      FileCopyUtils.copy(stream, response.getOutputStream());

    } catch (IOException e) {
      logger.debug("Failed to download the test packages ");
      throw new DownloadDocumentException("Cannot download the test packages");
    }
  }

  @RequestMapping(value = "/exampleMessages", method = RequestMethod.POST)
  public void exampleMessages(@RequestParam("stage") Stage stage, HttpServletRequest request,
      HttpServletResponse response) throws DownloadDocumentException {
    try {
      InputStream stream = zipExampleMessages(stage);
      response.setContentType("application/zip");
      response.setHeader("Content-disposition", "attachment;filename=" + stage
          + "ExampleMessages.zip");
      FileCopyUtils.copy(stream, response.getOutputStream());

    } catch (IOException e) {
      logger.debug("Failed to download the test packages ");
      throw new DownloadDocumentException("Cannot download the test packages");
    }
  }

  @RequestMapping(value = "/artifact", method = RequestMethod.POST,
      consumes = "application/x-www-form-urlencoded; charset=UTF-8")
  public void download(@RequestParam("title") String title, @RequestParam("path") String path,
      HttpServletRequest request, HttpServletResponse response) throws DownloadDocumentException {
    try {
      InputStream content = getContent(path);
      String fileName = path.substring(path.lastIndexOf("/") + 1);
      if (content != null && fileName != null) {
        response.setContentType(getContentType(path));
        fileName = title + "-" + fileName;
        fileName = fileName.replaceAll(" ", "-");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName);
        FileCopyUtils.copy(content, response.getOutputStream());
      } else {
        throw new DownloadDocumentException("Invalid Path Provided");
      }
    } catch (IOException e) {
      logger.debug("Failed to download the test package ");
      throw new DownloadDocumentException("Cannot download the artifact " + e.getMessage());
    }
  }



  public InputStream zipTestPackages(Stage stage) throws IOException {
    List<Resource> resources = null;
    if (stage == Stage.CB) {
      resources = ResourcebundleHelper.getResources("/*Contextbased/**/TestPackage.pdf");
    } else if (stage == Stage.CB) {
      resources = ResourcebundleHelper.getResources("/*Contextfree/**/TestPackage.pdf");
    } else if (stage == Stage.CB) {
      resources = ResourcebundleHelper.getResources("/*Isolated/**/TestPackage.pdf");
    }

    for (Resource resource : resources) {
      System.out.println(resource.getFilename());
    }

    return null;
  }


  public InputStream zipExampleMessages(Stage stage) throws IOException {
    List<Message> messages = testContextRepository.findAllExampleMessages(stage);
    ByteArrayOutputStream outputStream = null;
    byte[] bytes;
    outputStream = new ByteArrayOutputStream();
    ZipOutputStream out = new ZipOutputStream(outputStream);
    for (Message message : messages) {
      this.zipMessage(out, message);
    }
    out.close();
    bytes = outputStream.toByteArray();
    return new ByteArrayInputStream(bytes);
  }


  private void zipMessage(ZipOutputStream out, Message message) throws IOException {
    byte[] buf = new byte[1024];
    out.putNextEntry(new ZipEntry(message.getName() + "Message.txt"));
    int lenTP;
    InputStream io = IOUtils.toInputStream(message.getContent());
    while ((lenTP = io.read(buf)) > 0) {
      out.write(buf, 0, lenTP);
    }
    out.closeEntry();
    io.close();
  }

  // public InputStream zipTestPackages(List<TestArtifact> testPackages) throws IOException {
  // ByteArrayOutputStream outputStream = null;
  // byte[] bytes;
  // outputStream = new ByteArrayOutputStream();
  // ZipOutputStream out = new ZipOutputStream(outputStream);
  // for (TestArtifact testPackage : testPackages) {
  // this.zipTestPackage(out, testPackage.getPdfPath());
  // }
  // out.close();
  // bytes = outputStream.toByteArray();
  // return new ByteArrayInputStream(bytes);
  // }
  //
  //
  // private void zipTestPackage(String path, String title) throws IOException {
  // byte[] buf = new byte[1024];
  // out.putNextEntry(new ZipEntry(path.substring(path.lastIndexOf("/") + 1).replaceAll(" ", "-")));
  // int lenTP;
  // InputStream io = getContent(path);
  // while ((lenTP = io.read(buf)) > 0) {
  // out.write(buf, 0, lenTP);
  // }
  // out.closeEntry();
  // io.close();
  // }

  // private void zipTestPackage(TestPlan testPlan) throws IOException {
  //
  // File tpFolder = new File(testPlan.getName());
  // tpFolder.mkdir();
  //
  // if (testPlan.getTestPackage() != null) {
  //
  // }
  //
  //
  // if (testPlan.getTestCaseGroups() != null && !testPlan.getTestCaseGroups().isEmpty()) {
  // for (TestCaseGroup tcg : tp.getTestCaseGroups()) {
  // doc.getChildren().add(generateTestCaseDocument(tcg));
  // }
  // }
  // if (testPlan.getTestCases() != null && !testPlan.getTestCases().isEmpty()) {
  // for (TestCase tc : testPlan.getTestCases()) {
  // doc.getChildren().add(generateTestCaseDocument(tc));
  // }
  // }
  //
  //
  //
  // }



  private InputStream getContent(String path) {
    InputStream content = null;
    if (!path.startsWith("/")) {
      content = DocumentationController.class.getResourceAsStream("/" + path);
    } else {
      content = DocumentationController.class.getResourceAsStream(path);
    }
    return content;
  }

  private String getContentType(String fileName) {
    String contentType = "application/octet-stream";
    String fileExtension = getExtension(fileName);
    if (fileExtension != null) {
      fileExtension = fileExtension.toLowerCase();
    }
    if (fileExtension.equals("pdf")) {
      contentType = "application/pdf";
    } else if (fileExtension.equals("doc")) {
      contentType = "application/msword";
    } else if (fileExtension.equals("docx")) {
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    } else if (fileExtension.equals("xls")) {
      contentType = "application/vnd.ms-excel";
    } else if (fileExtension.equals("xlsx")) {
      contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    } else if (fileExtension.equals("jpeg")) {
      contentType = "image/jpeg";
    } else if (fileExtension.equals("xml")) {
      contentType = "text/xml";
    } else if (fileExtension.equals("war") || fileExtension.equals("zip")) {
      contentType = "application/x-zip";
    } else if (fileExtension.equals("pptx")) {
      contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    } else if (fileExtension.equals("ppt")) {
      contentType = "application/vnd.ms-powerpoint";
    }
    return contentType;
  }

  private String getExtension(String fileName) {
    String ext = "";
    int i = fileName.lastIndexOf('.');
    if (i != -1) {
      ext = fileName.substring(i + 1);
    }
    return ext;
  }


}
