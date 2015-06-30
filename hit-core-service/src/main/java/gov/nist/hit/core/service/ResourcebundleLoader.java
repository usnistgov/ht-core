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

import gov.nist.hit.core.domain.AppInfo;
import gov.nist.hit.core.domain.CFTestObject;
import gov.nist.hit.core.domain.ConformanceProfile;
import gov.nist.hit.core.domain.Constraints;
import gov.nist.hit.core.domain.IntegrationProfile;
import gov.nist.hit.core.domain.Message;
import gov.nist.hit.core.domain.ProfileModel;
import gov.nist.hit.core.domain.Stage;
import gov.nist.hit.core.domain.TestArtifact;
import gov.nist.hit.core.domain.TestCase;
import gov.nist.hit.core.domain.TestCaseGroup;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.domain.TestPlan;
import gov.nist.hit.core.domain.TestStep;
import gov.nist.hit.core.domain.TestStory;
import gov.nist.hit.core.domain.VocabularyLibrary;
import gov.nist.hit.core.repo.AppInfoRepository;
import gov.nist.hit.core.repo.CFTestObjectRepository;
import gov.nist.hit.core.repo.ConstraintsRepository;
import gov.nist.hit.core.repo.IntegrationProfileRepository;
import gov.nist.hit.core.repo.MessageRepository;
import gov.nist.hit.core.repo.TestPlanRepository;
import gov.nist.hit.core.repo.TestStepRepository;
import gov.nist.hit.core.repo.VocabularyLibraryRepository;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.core.service.util.FileUtil;
import gov.nist.hit.core.service.util.ResourceBundleHelper;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.icu.text.SimpleDateFormat;


/**
 * Harold Affo
 * 
 */
public abstract class ResourcebundleLoader {

  static final Logger logger = LoggerFactory.getLogger(ResourcebundleLoader.class);

  final static String PROFILE_PATTERN = "Global/Profiles/";
  final static String VALUESET_PATTERN = "Global/Tables/";
  final static String CONSTRAINT_PATTERN = "Global/Constraints/";
  final static String CONTEXTBASED_PATTERN = "Contextbased/";
  final static String CONTEXTFREE_PATTERN = "Contextfree/";
  final static String DOCUMENTATION_PATTERN = "Documentation/";
  final static String TEST_PLAN_FILE_PATTERN = "TestPlan.json";
  final static String TEST_CASE_FILE_PATTERN = "TestCase.json";
  final static String TEST_CASEGROUP_FILE_PATTERN = "TestCaseGroup.json";
  final static String TEST_STEP_FILE_PATTERN = "TestStep.json";
  final static String TEST_OBJECT_FILE_PATTERN = "TestObject.json";

  final static String TEST_PROCEDURE_FILE_PATTERN = "TestProcedure.json";
  final static String TEST_PACKAGE_FILE_PATTERN = "TestPackage.json";
  final static String TEST_STORY_FILE_PATTERN = "TestStory.json";
  final static String MESSAGE_PATTERN = "Message.txt";
  final static String CONSTRAINTS_FILE_PATTERN = "Constraints.xml";

  final static String ABOUT_PATTERN = "About/";

  Map<String, IntegrationProfile> profileMap = new HashMap<String, IntegrationProfile>();
  Map<String, VocabularyLibrary> vocabLibraryMap = new HashMap<String, VocabularyLibrary>();
  Map<String, Constraints> constraintMap = new HashMap<String, Constraints>();

  ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(ResourcebundleLoader.class);

  @Autowired
  TestPlanRepository testPlanRepository;

  @Autowired
  CFTestObjectRepository cfTestObjectRepository;

  @Autowired
  TestStepRepository testStepRepository;

  @Autowired
  AppInfoRepository appInfoRepository;

  @Autowired
  IntegrationProfileRepository integrationProfileRepository;

  @Autowired
  VocabularyLibraryRepository vocabularyLibraryRepository;

  @Autowired
  MessageRepository messageRepository;

  @Autowired
  ConstraintsRepository constraintsRepository;


  ObjectMapper obm;


  public ResourcebundleLoader() {
    obm = new ObjectMapper();
  }

  // private void documentations() throws IOException {
  // // Resource resource = resourceBundleHelper.getResource(DOCUMENTATION_PATTERN + "*");
  // // if (resource != null) {
  // // ObjectMapper mapper = new ObjectMapper();
  // // JsonNode documentationJson = mapper.readTree(FileUtil.getContent(resource));
  // // JsonNode userDocs = documentationJson.get("userDocs");
  // // if (userDocs != null) {
  // // Iterator<JsonNode> it = userDocs.iterator();
  // // while (it.hasNext()) {
  // // JsonNode node = it.next();
  // // String name = node.get("name").getTextValue();
  // // String fileName = node.get("fileName").getTextValue();
  // //
  // // }
  // //
  // // }
  // //
  // //
  // //
  // // }
  //
  // }

  public void appInfo() throws JsonProcessingException, IOException {
    Resource resource = resourceBundleHelper.getResource(ABOUT_PATTERN + "MetaData.json");
    if (resource == null)
      throw new RuntimeException("No MetaData.json found in the resource bundle");

    AppInfo appInfo = new AppInfo();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode appInfoJson = mapper.readTree(FileUtil.getContent(resource));
    appInfo.setAdminEmail(appInfoJson.get("adminEmail").getTextValue());
    appInfo.setDomain(appInfoJson.get("domain").getTextValue());
    appInfo.setHeader(appInfoJson.get("header").getTextValue());
    appInfo.setHomeContent(appInfoJson.get("homeContent").getTextValue());
    appInfo.setHomeTitle(appInfoJson.get("homeTitle").getTextValue());
    appInfo.setName(appInfoJson.get("name").getTextValue());
    appInfo.setVersion(appInfoJson.get("version").getTextValue());

    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    Date date = new Date();
    appInfo.setDate(dateFormat.format(date));

    appInfoRepository.save(appInfo);

  }

  public void constraints() throws IOException {
    List<Resource> resources = resourceBundleHelper.getResources(CONSTRAINT_PATTERN + "*");
    for (Resource resource : resources) {
      String content = FileUtil.getContent(resource);
      Constraints constraints = constraint(content);
      constraintMap.put(constraints.getSourceId(), constraints);
    }
  }

  public void integrationProfiles() throws IOException {
    List<Resource> resources = resourceBundleHelper.getResources(PROFILE_PATTERN + "*");
    for (Resource resource : resources) {
      IntegrationProfile integrationProfile = integrationProfile(FileUtil.getContent(resource));
      integrationProfileRepository.save(integrationProfile);
    }
  }


  public void vocabularyLibraries() throws IOException {
    List<Resource> resources = resourceBundleHelper.getResources(VALUESET_PATTERN + "*");
    for (Resource resource : resources) {
      String content = FileUtil.getContent(resource);
      VocabularyLibrary vocabLibrary = vocabLibrary(content);
      vocabLibraryMap.put(vocabLibrary.getSourceId(), vocabLibrary);
    }
  }

  private VocabularyLibrary vocabLibrary(String content) throws JsonGenerationException,
      JsonMappingException, IOException {
    Document doc = this.stringToDom(content);
    VocabularyLibrary vocabLibrary = new VocabularyLibrary();
    Element valueSetLibraryeElement = (Element) doc.getElementsByTagName("ValueSetLibrary").item(0);
    vocabLibrary.setSourceId(valueSetLibraryeElement.getAttribute("ValueSetLibraryIdentifier"));
    vocabLibrary.setName(valueSetLibraryeElement.getAttribute("Name"));
    vocabLibrary.setDescription(valueSetLibraryeElement.getAttribute("Description"));
    vocabLibrary.setXml(content);
    vocabLibrary.setJson(obm.writeValueAsString(getValueSetLibrarySerializer().toObject(content)));
    vocabularyLibraryRepository.save(vocabLibrary);

    return vocabLibrary;
  }


  private Constraints additionalConstraints(String location) {
    Resource resource = resourceBundleHelper.getResource(location);
    if (resource == null) {
      return null;
    }
    Constraints constraints = new Constraints();
    constraints.setXml(FileUtil.getContent(resource));
    return constraints;
  }

  private IntegrationProfile integrationProfile(String content) {
    Document doc = this.stringToDom(content);
    IntegrationProfile integrationProfile = new IntegrationProfile();
    Element profileElement = (Element) doc.getElementsByTagName("ConformanceProfile").item(0);
    // integrationProfile.setType(profileElement.getAttribute("Type"));
    // integrationProfile.setHl7Version(profileElement.getAttribute("HL7Version"));
    // integrationProfile.setSchemaVersion(profileElement.getAttribute("SchemaVersion"));
    integrationProfile.setSourceId(profileElement.getAttribute("ID"));
    Element metaDataElement = (Element) profileElement.getElementsByTagName("MetaData").item(0);
    integrationProfile.setName(metaDataElement.getAttribute("Name"));
    integrationProfile.setXml(content);
    Element conformanceProfilElementRoot =
        (Element) profileElement.getElementsByTagName("Messages").item(0);
    NodeList messages = conformanceProfilElementRoot.getElementsByTagName("Message");
    for (int j = 0; j < messages.getLength(); j++) {
      Element elmCode = (Element) messages.item(j);
      String id = elmCode.getAttribute("ID");
      if (profileMap.containsKey(id)) {
        throw new RuntimeException("Found duplicate conformance profile ID " + id);
      }
      profileMap.put(id, integrationProfile);
    }

    return integrationProfile;
  }

  private Message message(String content) {
    if (content != null) {
      Message m = new Message();
      m.setContent(content);
      return m;
    }
    return null;
  }

  private Constraints constraint(String content) {
    Document doc = this.stringToDom(content);
    Constraints constraints = new Constraints();
    Element constraintsElement = (Element) doc.getElementsByTagName("ConformanceContext").item(0);
    constraints.setSourceId(constraintsElement.getAttribute("UUID"));
    Element metaDataElement = (Element) constraintsElement.getElementsByTagName("MetaData").item(0);
    constraints.setDescription(metaDataElement.getAttribute("Description"));
    constraints.setXml(content);
    constraintsRepository.save(constraints);
    return constraints;
  }

  private Document stringToDom(String xmlSource) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setIgnoringComments(false);
    factory.setIgnoringElementContentWhitespace(true);
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
      return builder.parse(new InputSource(new StringReader(xmlSource)));
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void cb() throws IOException {
    List<Resource> resources = resourceBundleHelper.getDirectories(CONTEXTBASED_PATTERN + "*/");
    // List<TestPlan> testPlans = new ArrayList<TestPlan>();
    for (Resource resource : resources) {
      String fileName = fileName(resource);
      String location =
          fileName.substring(fileName.indexOf(CONTEXTBASED_PATTERN), fileName.length());
      TestPlan testPlan = testPlan(location, Stage.CB);
      testPlanRepository.save(testPlan);
      // testPlans.add(testPlan);
      System.out.println(testPlan.getName());
    }
  }

  public void cf() throws IOException, ProfileParserException {
    List<Resource> resources = resourceBundleHelper.getDirectories(CONTEXTFREE_PATTERN + "*/");
    for (Resource resource : resources) {
      String fileName = fileName(resource);
      CFTestObject to =
          cfTestObject(fileName.substring(fileName.indexOf(CONTEXTFREE_PATTERN), fileName.length()));
      to.setRoot(true);
      cfTestObjectRepository.save(to);
    }
  }

  private TestPlan testPlan(String testPlanPath, Stage stage) throws IOException {
    TestPlan tp = new TestPlan();
    Resource res = resourceBundleHelper.getResource(testPlanPath + "TestPlan.json");
    if (res == null)
      throw new IllegalArgumentException("No TestPlan.json found at " + testPlanPath);
    String descriptorContent = FileUtil.getContent(res);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode testPlanObj = mapper.readTree(descriptorContent);
    tp.setName(testPlanObj.findValue("name").getTextValue());
    tp.setDescription(testPlanObj.findValue("description").getTextValue());
    tp.setStage(stage);

    if (resourceBundleHelper.getResource(testPlanPath + "TestProcedure.pdf") != null) {
      TestArtifact testProcedure = new TestArtifact();
      testProcedure.setPdfPath(testPlanPath + "TestProcedure.pdf");
    }

    if (resourceBundleHelper.getResource(testPlanPath + "TestProcedure.json") != null) {
      TestArtifact testProcedure = new TestArtifact();
      testProcedure.setJsonPath(testPlanPath + "TestProcedure.json");
    }

    List<Resource> resources = resourceBundleHelper.getDirectories(testPlanPath + "*/");
    for (Resource resource : resources) {
      String fileName = fileName(resource);
      String location = fileName.substring(fileName.indexOf(testPlanPath), fileName.length());
      Resource descriptorResource = getDescriptorResource(location);
      String filename = descriptorResource.getFilename();
      if (filename.endsWith("TestCaseGroup.json")) {
        TestCaseGroup testCaseGroup = testCaseGroup(location, stage);
        tp.getTestCaseGroups().add(testCaseGroup);
        System.out.println(testCaseGroup.getName());
      } else if (filename.endsWith("TestCase.json")) {
        TestCase testCase = testCase(location, stage);
        tp.getTestCases().add(testCase);
        System.out.println(testCase.getName());
      }
    }
    return tp;
  }

  private CFTestObject cfTestObject(String testObjectPath) throws IOException {
    CFTestObject parent = new CFTestObject();
    Resource res = resourceBundleHelper.getResource(testObjectPath + "TestObject.json");
    if (res == null)
      throw new IllegalArgumentException("No TestObject.json found at " + testObjectPath);
    String descriptorContent = FileUtil.getContent(res);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode testPlanObj = mapper.readTree(descriptorContent);
    parent.setName(testPlanObj.findValue("name").getTextValue());
    parent.setDescription(testPlanObj.findValue("description").getTextValue());
    JsonNode messageId = testPlanObj.findValue("messageId");
    JsonNode constraintId = testPlanObj.findValue("constraintId");
    JsonNode valueSetLibraryId = testPlanObj.findValue("valueSetLibraryId");
    if (messageId != null && constraintId != null && valueSetLibraryId != null) {
      parent.setTestContext(testContext(testObjectPath, testPlanObj));
    }
    List<Resource> resources = resourceBundleHelper.getDirectories(testObjectPath + "*/");
    for (Resource resource : resources) {
      String fileName = fileName(resource);
      String location = fileName.substring(fileName.indexOf(testObjectPath), fileName.length());
      CFTestObject testObject = cfTestObject(location);
      parent.getChildren().add(testObject);
    }
    return parent;
  }

  private TestContext testContext(String path, JsonNode parentObj) throws IOException {
    TestContext testContext = new TestContext();
    JsonNode messageId = parentObj.findValue("messageId");
    JsonNode constraintId = parentObj.findValue("constraintId");
    JsonNode valueSetLibraryId = parentObj.findValue("valueSetLibraryId");
    if (messageId != null && valueSetLibraryId != null) {
      if (constraintId != null) {
        testContext.setConstraints(getConstraints(constraintId.getTextValue()));
      }
      testContext.setVocabularyLibrary((getVocabularyLibrary(valueSetLibraryId.getTextValue())));
      testContext.setAddditionalConstraints(additionalConstraints(path));
      testContext.setMessage(message(FileUtil.getContent(resourceBundleHelper.getResource(path
          + "Message.txt"))));
      try {
        ConformanceProfile conformanceProfile = new ConformanceProfile();
        IntegrationProfile integrationProfile = getIntegrationProfile(messageId.getTextValue());
        conformanceProfile.setJson(generateJsonConformanceProfile(integrationProfile.getXml(),
            messageId.getTextValue(), testContext.getConstraints() != null ? testContext
                .getConstraints().getXml() : null,
            testContext.getAddditionalConstraints() != null ? testContext
                .getAddditionalConstraints().getXml() : null));
        conformanceProfile.setIntegrationProfile(integrationProfile);
        testContext.setConformanceProfile(conformanceProfile);
      } catch (ProfileParserException e) {
        throw new RuntimeException("Failed to parse integrationProfile at " + path);
      }
    }
    return testContext;
  }

  public String generateJsonConformanceProfile(String integrationProfileXml,
      String conformanceProfileId, String constraintsXml, String additionalConstraintsXml)
      throws ProfileParserException, JsonProcessingException,
      com.fasterxml.jackson.core.JsonProcessingException {
    ProfileModel profileModel =
        getProfileParser().parse(integrationProfileXml, conformanceProfileId, constraintsXml,
            additionalConstraintsXml);
    com.fasterxml.jackson.databind.ObjectMapper obm =
        new com.fasterxml.jackson.databind.ObjectMapper();
    obm.setSerializationInclusion(Include.NON_NULL);
    String json = obm.writeValueAsString(profileModel);
    return json;
  }

  private String fileName(Resource resource) throws IOException {
    String location = resource.getURL().toString();
    return location.replaceAll("%20", " ");
  }

  private TestCase testCase(String location, Stage stage) throws IOException {
    TestCase tc = new TestCase();
    Resource res = resourceBundleHelper.getResource(location + "TestCase.json");
    if (res == null)
      throw new IllegalArgumentException("No TestCase.json found at " + location);
    String descriptorContent = FileUtil.getContent(res);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode testPlanObj = mapper.readTree(descriptorContent);
    tc.setName(testPlanObj.findValue("name").getTextValue());
    tc.setDescription(testPlanObj.findValue("description").getTextValue());
    tc.setTestStory(testStory(location));
    tc.setTestPackage(testPackage(location));
    tc.setJurorDocument(jurorDocument(location));
    List<Resource> resources = resourceBundleHelper.getDirectories(location + "*");
    for (Resource resource : resources) {
      String fileName = fileName(resource);
      String tcLocation = fileName.substring(fileName.indexOf(location), fileName.length());
      tc.getTestSteps().add(testStep(tcLocation, stage));
    }
    return tc;
  }

  private TestStep testStep(String location, Stage stage) throws IOException {
    TestStep testStep = new TestStep();
    Resource res = resourceBundleHelper.getResource(location + "TestStep.json");
    if (res == null)
      throw new IllegalArgumentException("No TestStep.json found at " + location);
    String descriptorContent = FileUtil.getContent(res);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode testPlanObj = mapper.readTree(descriptorContent);
    testStep.setName(testPlanObj.findValue("name").getTextValue());
    testStep.setDescription(testPlanObj.findValue("description").getTextValue());
    testStep.setTestContext(testContext(location, testPlanObj));
    testStep.setTestStory(testStory(location));
    testStep.setTestPackage(testPackage(location));
    testStep.setJurorDocument(jurorDocument(location));
    return testStep;
  }

  private TestStory testStory(String location) throws IOException {
    TestStory tStory = new TestStory();
    Resource json = resourceBundleHelper.getResource(location + "TestStory.json");
    if (json != null) {
      String descriptorContent = FileUtil.getContent(json);
      ObjectMapper mapper = new ObjectMapper();
      JsonNode tStoryObj = mapper.readTree(descriptorContent);
      tStory.setDescription(tStoryObj.findValue("teststorydesc") != null ? tStoryObj.findValue(
          "teststorydesc").getTextValue() : null);
      tStory.setHtmlPath(location + "TestStory.html");
      tStory.setNotes(tStoryObj.findValue("notes") != null ? tStoryObj.findValue("notes")
          .getTextValue() : null);
      tStory.setPdfPath(location + "TestStory.pdf");
      tStory.setPostCondition(tStoryObj.findValue("postCondition") != null ? tStoryObj.findValue(
          "postCondition").getTextValue() : null);
      tStory.setPreCondition(tStoryObj.findValue("preCondition") != null ? tStoryObj.findValue(
          "preCondition").getTextValue() : null);
      tStory.setTestObjectives(tStoryObj.findValue("testObjectives") != null ? tStoryObj.findValue(
          "testObjectives").getTextValue() : null);
      tStory.setComments(tStoryObj.findValue("comments") != null ? tStoryObj.findValue("comments")
          .getTextValue() : null);
      return tStory;
    }
    return null;
  }

  private TestArtifact jurorDocument(String location) throws IOException {
    TestArtifact doc = null;
    Resource pdf = resourceBundleHelper.getResource(location + "JurorDocument.pdf");
    if (pdf != null) {
      doc = new TestArtifact();
      doc.setPdfPath(location + "JurorDocument.pdf");
    }

    Resource html = resourceBundleHelper.getResource(location + "JurorDocument.html");
    if (html != null) {
      doc = doc == null ? new TestArtifact() : doc;
      doc.setHtmlPath(location + "JurorDocument.html");
    }
    return doc;
  }

  private TestArtifact testPackage(String location) throws IOException {
    TestArtifact doc = null;
    Resource pdf = resourceBundleHelper.getResource(location + "TestPackage.pdf");
    if (pdf != null) {
      doc = new TestArtifact();
      doc.setPdfPath(location + "TestPackage.pdf");
    }

    Resource html = resourceBundleHelper.getResource(location + "TestPackage.html");
    if (html != null) {
      doc.setHtmlPath(location + "TestPackage.html");
    }
    return doc;
  }


  private TestCaseGroup testCaseGroup(String location, Stage stage) throws IOException {
    TestCaseGroup tcg = new TestCaseGroup();
    Resource descriptorRsrce = resourceBundleHelper.getResource(location + "TestCaseGroup.json");
    if (descriptorRsrce == null)
      throw new IllegalArgumentException("No TestCaseGroup.json found at " + location);
    String descriptorContent = FileUtil.getContent(descriptorRsrce);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode testPlanObj = mapper.readTree(descriptorContent);
    tcg.setName(testPlanObj.findValue("name").getTextValue());
    tcg.setDescription(testPlanObj.findValue("description").getTextValue());
    List<Resource> resources = resourceBundleHelper.getDirectories(location + "*/");
    for (Resource resource : resources) {
      String fileName = fileName(resource);
      String tcLocation = fileName.substring(fileName.indexOf(location), fileName.length());
      Resource descriptorResource = getDescriptorResource(tcLocation);
      String filename = descriptorResource.getFilename();
      if (filename.endsWith("TestCaseGroup.json")) {
        TestCaseGroup testCaseGroup = testCaseGroup(tcLocation, stage);
        tcg.getTestCaseGroups().add(testCaseGroup);
      } else if (filename.endsWith("TestCase.json")) {
        TestCase testCase = testCase(tcLocation, stage);
        tcg.getTestCases().add(testCase);
      }
    }
    return tcg;
  }

  private Resource getDescriptorResource(String location) throws IOException {
    Resource resource = resourceBundleHelper.getResource(location + "TestPlan.json");
    resource =
        resource == null ? resourceBundleHelper.getResource(location + "TestCaseGroup.json")
            : resource;
    resource =
        resource == null ? resourceBundleHelper.getResource(location + "TestCase.json") : resource;
    resource =
        resource == null ? resourceBundleHelper.getResource(location + "TestStep.json") : resource;
    resource = resource == null ? null : resource;
    return resource;
  }


  private IntegrationProfile getIntegrationProfile(String id) throws IOException {
    IntegrationProfile p = profileMap.get(id);
    if (p == null) {
      throw new IllegalArgumentException(
          "Cannot find IntegrationProfile associated to ConformanceProfile with id = " + id);
    }

    return p;
  }

  private Constraints getConstraints(String id) throws IOException {
    Constraints c = constraintMap.get(id);
    if (c == null) {
      throw new IllegalArgumentException("Constraints with id = " + id + " not found");
    }

    return c;
  }

  private VocabularyLibrary getVocabularyLibrary(String id) throws IOException {
    VocabularyLibrary v = vocabLibraryMap.get(id);
    if (v == null) {
      throw new IllegalArgumentException("VocabularyLibrary with id = " + id + " not found");
    }

    return v;
  }

  public abstract ProfileParser getProfileParser();

  public abstract ValueSetLibrarySerializer getValueSetLibrarySerializer();



}
