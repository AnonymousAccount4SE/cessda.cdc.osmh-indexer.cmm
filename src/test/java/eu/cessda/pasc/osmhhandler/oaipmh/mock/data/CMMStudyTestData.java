package eu.cessda.pasc.osmhhandler.oaipmh.mock.data;


import eu.cessda.pasc.osmhhandler.oaipmh.helpers.FileHandler;
import eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy.CMMStudy;
import org.assertj.core.util.Maps;

import java.util.Map;

/**
 * mock data for Record headers.
 *
 * @author moses@doraventures.com
 */
public class CMMStudyTestData {

  public static CMMStudy getCMMStudy() {

    CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();
    Map<String, String> titleStudy = Maps.newHashMap("en", "study title");
    titleStudy.put("no", "et study title");
    builder.titleStudy(titleStudy);

    builder.studyNumber("Noi1254");
    builder.abstractField(Maps.newHashMap("en", "my abstract description text"));
    return builder.build();
  }

  public static String getDdiRecord1683() {
    FileHandler fileHandler = new FileHandler();
    return fileHandler.getFileWithUtil("xml/ddi_record_1683.xml");
  }

  public static String getDdiRecordWithRepeatedAbstract() {
    FileHandler fileHandler = new FileHandler();
    return fileHandler.getFileWithUtil("xml/ddi_record_2305_fsd_repeat_abstract.xml");
  }

  /**
   * Loads up a valid but deleted xml record example
   * @return the xml String
   */
  public static String getDdiRecord1031Deleted() {
    FileHandler fileHandler = new FileHandler();
    return fileHandler.getFileWithUtil("xml/ddi_record_1031_deleted.xml");
  }

  /**
   * Loads up a record with an <error> element
   * @return the xml String
   */
  public static String getDdiRecordWithError() {
    FileHandler fileHandler = new FileHandler();
    return fileHandler.getFileWithUtil("xml/ddi_record_WithError.xml");
  }
}