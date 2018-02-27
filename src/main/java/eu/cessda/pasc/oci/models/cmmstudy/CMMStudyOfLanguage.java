package eu.cessda.pasc.oci.models.cmmstudy;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;
import java.util.Set;

/**
 * Model representing a CMMStudy.
 *
 * @author moses@doraventures.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "studyNumber",
    "titleStudy",
    "abstract",
    "classifications",
    "keywords",
    "typeOfTimeMethods",
    "studyAreaCountries",
    "unitTypes",
    "publisher",
    "publicationYear",
    "pidStudies",
    "fileLanguages",
    "creators",
    "typeOfSamplingProcedures",
    "samplingProcedureFreeTexts",
    "typeOfModeOfCollections",
    "dataCollectionPeriodStartdate",
    "dataCollectionPeriodEnddate",
    "dataCollectionFreeTexts",
    "dataAccessFreeTexts",
    "lastModified",
    "isActive"
})
@Builder
@ToString
@Document(indexName = "test")
public class CMMStudyOfLanguage {

  @Id
  @Setter
  private String id;

  @JsonProperty("creators")
  private List<String> creators;

  @JsonProperty("dataCollectionPeriodStartdate")
  private String dataCollectionPeriodStartdate;

  @JsonProperty("dataCollectionPeriodEnddate")
  private String dataCollectionPeriodEnddate;

  @JsonProperty("dataCollectionFreeTexts")
  private List<DataCollectionFreeText> dataCollectionFreeTexts;

  @JsonProperty("dataAccessFreeTexts")
  private List<String> dataAccessFreeTexts;

  @JsonProperty("publicationYear")
  private String publicationYear;

  @JsonProperty("typeOfModeOfCollections")
  private List<TermVocabAttributes> typeOfModeOfCollections;

  @JsonProperty("keywords")
  private List<TermVocabAttributes> keywords;

  @JsonProperty("samplingProcedureFreeTexts")
  private List<String> samplingProcedureFreeTexts;

  @JsonProperty("classifications")
  private List<TermVocabAttributes> classifications;

  @JsonProperty("abstract")
  private String abstractField;

  @JsonProperty("titleStudy")
  private String titleStudy;

  @JsonProperty("studyNumber")
  private String studyNumber;

  @JsonProperty("typeOfTimeMethods")
  private List<TermVocabAttributes> typeOfTimeMethods;

  @JsonProperty("fileLanguages")
  private Set<String> fileLanguages;

  @JsonProperty("typeOfSamplingProcedures")
  private List<VocabAttributes> typeOfSamplingProcedures;

  @JsonProperty("publisher")
  private Publisher publisher;

  @JsonProperty("studyAreaCountries")
  private List<Country> studyAreaCountries;

  @JsonProperty("unitTypes")
  private List<TermVocabAttributes> unitTypes;

  @JsonProperty("pidStudies")
  private List<Pid> pidStudies;

  @JsonProperty("lastModified")
  private String lastModified;

  @JsonProperty("isActive")
  private boolean active;

  public String getId() {
    return id;
  }
}