/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cessda.pasc.oci.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.exception.HarvesterException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.http.HttpClient;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyConverter;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.JDOMException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * Tests related to {@link RecordXMLParser}
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
public class RecordXMLParserTest {

    private final CMMStudyConverter cmmConverter = new CMMStudyConverter();
    private final HttpClient httpClient = Mockito.mock(HttpClient.class);
    private final Repo repo;
    private final String recordIdentifier;
    private final URI fullRecordUrl;
    private final CMMStudyMapper cmmStudyMapper = new CMMStudyMapper();
    private final Record recordHeader;

    public RecordXMLParserTest() {
        // Needed because TimeUtility only works properly in UTC timezones
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        repo = ReposTestData.getUKDSRepo();
        recordIdentifier = "http://my-example_url:80/obj/fStudy/ch.sidos.ddi.468.7773";
        recordHeader = new Record(RecordHeader.builder().identifier(recordIdentifier).build(), repo.getUrl(),null);
        fullRecordUrl = URI.create(repo.getUrl() + "?verb=GetRecord&identifier=" + URLEncoder.encode(recordIdentifier, StandardCharsets.UTF_8) + "&metadataPrefix=ddi");
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyComplaintCmmDdiRecord() throws IOException, ProcessingException, JSONException, HarvesterException {

        // Given
        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/synthetic_compliant_cmm.xml")
        );

        // When
        CMMStudy result = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, recordHeader);

        then(result).isNotNull();
        validateCMMStudyResultAgainstSchema(result);
        assertFieldsAreExtractedAsExpected(result);
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldHarvestedContentForLanguageSpecificDimensionFromElementWithCorrectXmlLangAttribute() throws IOException, HarvesterException {

        // Given
        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/oai-fsd_uta_fi-FSD3187.xml")
        );

        // When
        CMMStudy result = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, recordHeader);

        then(result).isNotNull();

        // Verifies timeMeth extraction
        then(result.getTypeOfTimeMethods().size()).isEqualTo(2);
        then(result.getTypeOfTimeMethods().get("fi").get(0).getTerm()).isEqualTo("Pitkittäisaineisto: trendi/toistuva poikkileikkausaineisto");
        then(result.getTypeOfTimeMethods().get("en").get(0).getTerm()).isEqualTo("Longitudinal: Trend/Repeated cross-section");

        // Verifies unitTypes extraction
        then(result.getUnitTypes().size()).isEqualTo(2);
        then(result.getUnitTypes().get("fi").get(0).getTerm()).isEqualTo("Henkilö");
        then(result.getUnitTypes().get("en").get(0).getTerm()).isEqualTo("Individual");
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord() throws IOException, ProcessingException, JSONException, HarvesterException {

        // Given
        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/ddi_record_1683.xml")
        );

        // When
        CMMStudy record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, recordHeader);

        // Then
        then(record).isNotNull();
        validateCMMStudyResultAgainstSchema(record);
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldOnlyExtractSingleDateAsStartDateForRecordsWithASingleDateAttr() throws IOException, ProcessingException, JSONException, HarvesterException {

        // Given
        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/ddi_record_1683.xml")
        );

        // When
        CMMStudy record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, recordHeader);
        then(record).isNotNull();
        validateCMMStudyResultAgainstSchema(record);
        final ObjectMapper mapper = new ObjectMapper();
        String jsonString = cmmConverter.toJsonString(record);
        final JsonNode actualTree = mapper.readTree(jsonString);

        then(actualTree.get("dataCollectionPeriodStartdate").asText()).isEqualTo("1976-01-01T00:00:00Z");
        then(actualTree.get("dataCollectionPeriodEnddate")).isNull();
        then(actualTree.get("dataCollectionYear").asInt()).isEqualTo(1976);
    }

    @Test
    public void shouldExtractDefaultLanguageFromCodebookXMLLagIfPresent() throws IOException, JSONException, HarvesterException {

        // Given
        String expectedCmmStudyJsonString = ResourceHandler.getResourceAsString("json/ddi_record_1683_with_codebookXmlLag.json");
        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/ddi_record_1683_with_codebookXmlLag.xml")
        );

        // When
        CMMStudy record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, recordHeader);
        String actualCmmStudyJsonString = cmmConverter.toJsonString(record);

        // then
        assertEquals(expectedCmmStudyJsonString, actualCmmStudyJsonString, false);
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldReturnCMMStudyRecordWithRepeatedAbstractConcatenated() throws IOException, ProcessingException, JSONException, HarvesterException {

        Map<String, String> expectedAbstract = new HashMap<>();
        expectedAbstract.put("de", "de de");
        expectedAbstract.put("fi", "Haastattelu<br>Jyväskylä");
        expectedAbstract.put("en", "1. The data<br>2. The datafiles");

        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/ddi_record_2305_fsd_repeat_abstract.xml")
        );

        // When
        CMMStudy record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, recordHeader);

        then(record).isNotNull();
        then(record.getAbstractField().size()).isEqualTo(3);
        then(record.getAbstractField()).isEqualTo(expectedAbstract);
        validateCMMStudyResultAgainstSchema(record);
    }

    @Test // https://bitbucket.org/cessda/cessda.cdc.version2/issues/135
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldReturnCMMStudyRecordWithOutParTitleWhenThereIsALangDifferentFromDefault() throws IOException, ProcessingException, JSONException, HarvesterException {

        Map<String, String> expectedTitle = new HashMap<>();
        expectedTitle.put("en", "Machinery of Government, 1976-1977");
        expectedTitle.put("no", "2 - Et Machinery of Government, 1976-1977");
        expectedTitle.put("yy", "Enquête sociale européenne");

        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/ddi_record_1683.xml")
        );

        // When
        CMMStudy record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, recordHeader);

        then(record).isNotNull();
        then(record.getTitleStudy().size()).isEqualTo(3);
        then(record.getTitleStudy()).isEqualTo(expectedTitle);
        validateCMMStudyResultAgainstSchema(record);
    }

    @Test()
    public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord_MarkedAsNotActive() throws IOException, HarvesterException {

        // Given
        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/ddi_record_1031_deleted.xml")
        );

        // When
        CMMStudy record = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, recordHeader);

        // Then
        then(record).isNotNull();
        then(record.isActive()).isFalse();
    }

    @Test(expected = OaiPmhException.class)
    public void shouldThrowExceptionForRecordWithErrorElement() throws IOException, HarvesterException {

        // Given
        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/ddi_record_WithError.xml")
        );

        // When
        new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, recordHeader);

        // Then an exception is thrown.
    }

    @Test
    public void shouldExtractAllRequiredCMMFieldsForAGivenAUKDSRecord() throws IOException, ProcessingException, JSONException, HarvesterException {

        // Given
        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/ddi_record_ukds_example.xml")
        );

        // When
        CMMStudy result = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, recordHeader);

        then(result).isNotNull();
        validateCMMStudyResultAgainstSchema(result);
        assertThatCmmRequiredFieldsAreExtracted(result);
    }

    private void validateCMMStudyResultAgainstSchema(CMMStudy record) throws IOException, ProcessingException, JSONException {

        then(record.isActive()).isTrue(); // No need to carry on validating other fields if marked as inActive

        String jsonString = cmmConverter.toJsonString(record);
        JSONObject json = new JSONObject(jsonString);
        log.debug("RETRIEVED STUDY JSON: \n" + json.toString(4));

        JsonNode jsonNodeRecord = JsonLoader.fromString(jsonString);
        final JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/json/schema/CMMStudySchema.json");

        ProcessingReport validate = schema.validate(jsonNodeRecord);
        if (!validate.isSuccess()) {
            fail("Validation not successful : " + validate);
        }
    }

    @SuppressWarnings("PreferJavaTimeOverload")
    private void assertFieldsAreExtractedAsExpected(CMMStudy record) throws IOException, JSONException {

        final ObjectMapper mapper = new ObjectMapper();
        String jsonString = cmmConverter.toJsonString(record);
        String expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record.json");
        final JsonNode actualTree = mapper.readTree(jsonString);
        final JsonNode expectedTree = mapper.readTree(expectedJson);

        // This following could be compared with one single Uber Json compare, but probably best this way to easily know
        // which field test assertion line below that fails.
        then(expectedTree.get("publicationYear").toString()).isEqualTo(actualTree.get("publicationYear").toString());
        then(expectedTree.get("dataCollectionPeriodStartdate").toString()).isEqualTo(actualTree.get("dataCollectionPeriodStartdate").toString());
        then(expectedTree.get("dataCollectionPeriodEnddate").toString()).isEqualTo(actualTree.get("dataCollectionPeriodEnddate").toString());
        then(expectedTree.get("dataCollectionYear").asInt()).isEqualTo(actualTree.get("dataCollectionYear").asInt());
        assertEquals(expectedTree.get("abstract").toString(), actualTree.get("abstract").toString(), true);
        assertEquals(expectedTree.get("classifications").toString(), actualTree.get("classifications").toString(), true);
        assertEquals(expectedTree.get("keywords").toString(), actualTree.get("keywords").toString(), true);
        assertEquals(expectedTree.get("typeOfTimeMethods").toString(), actualTree.get("typeOfTimeMethods").toString(), true);
        assertEquals(expectedTree.get("studyAreaCountries").toString(), actualTree.get("studyAreaCountries").toString(), true);
        assertEquals(expectedTree.get("pidStudies").toString(), actualTree.get("pidStudies").toString(), true);
        assertEquals(expectedTree.get("unitTypes").toString(), actualTree.get("unitTypes").toString(), true);
        assertEquals(expectedTree.get("titleStudy").toString(), actualTree.get("titleStudy").toString(), true);
        assertEquals(expectedTree.get("publisher").toString(), actualTree.get("publisher").toString(), true);
        assertEquals(expectedTree.get("creators").toString(), actualTree.get("creators").toString(), true);
        assertEquals(expectedTree.get("fileLanguages").toString(), actualTree.get("fileLanguages").toString(), true);
        assertEquals(expectedTree.get("typeOfSamplingProcedures").toString(), actualTree.get("typeOfSamplingProcedures").toString(), true);
        assertEquals(expectedTree.get("samplingProcedureFreeTexts").toString(), actualTree.get("samplingProcedureFreeTexts").toString(), true);
        assertEquals(expectedTree.get("typeOfModeOfCollections").toString(), actualTree.get("typeOfModeOfCollections").toString(), true);
        assertEquals(expectedTree.get("dataCollectionFreeTexts").toString(), actualTree.get("dataCollectionFreeTexts").toString(), true);
        assertEquals(expectedTree.get("dataAccessFreeTexts").toString(), actualTree.get("dataAccessFreeTexts").toString(), true);
        assertEquals(expectedTree.get("studyUrl").toString(), actualTree.get("studyUrl").toString(), true);
    }

    private void assertThatCmmRequiredFieldsAreExtracted(CMMStudy record) throws IOException, JSONException {

        final ObjectMapper mapper = new ObjectMapper();
        String jsonString = cmmConverter.toJsonString(record);
        String expectedJson = ResourceHandler.getResourceAsString("json/ddi_record_ukds_example_extracted.json");
        final JsonNode actualTree = mapper.readTree(jsonString);
        final JsonNode expectedTree = mapper.readTree(expectedJson);

        // CMM Model Schema required fields
        assertEquals(expectedTree.get("abstract").toString(), actualTree.get("abstract").toString(), true);
        assertEquals(expectedTree.get("titleStudy").toString(), actualTree.get("titleStudy").toString(), true);
        assertEquals(expectedTree.get("studyUrl").toString(), actualTree.get("studyUrl").toString(), true);
        then(actualTree.get("studyNumber").toString()).isEqualTo(expectedTree.get("studyNumber").toString());
        assertEquals(expectedTree.get("publisher").toString(), actualTree.get("publisher").toString(), true);
    }

    @Test
    public void shouldOverrideGlobalLanguageDefaultIfAPerRepositoryOverrideIsSpecified() throws IOException, ProcessingException, JSONException, HarvesterException {

        var repository = ReposTestData.getUKDSLanguageOverrideRepository();

        // Given
        given(httpClient.getInputStream(fullRecordUrl)).willReturn(
            ResourceHandler.getResourceAsStream("xml/ddi_2_5/ddi_record_ukds_example.xml")
        );

        // When
        CMMStudy result = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repository, recordHeader);

        then(result).isNotNull();
        validateCMMStudyResultAgainstSchema(result);

        // Assert the language is as expected
        Assert.assertNotNull(result.getTitleStudy().get("zz"));
    }

    @Test
    public void shouldUseAlreadyParsedDocumentIfPresent() throws IOException, JDOMException, JSONException, ProcessingException, HarvesterException {
        // Given
        var document = OaiPmhHelpers.getSaxBuilder().build(ResourceHandler.getResourceAsStream("xml/ddi_2_5/ddi_record_ukds_example.xml"));

        var header = new Record(recordHeader.getRecordHeader(), repo.getUrl(), document);

        // When
        var result = new RecordXMLParser(cmmStudyMapper, httpClient).getRecord(repo, header);

        then(result).isNotNull();
        validateCMMStudyResultAgainstSchema(result);

        then(result.getStudyXmlSourceUrl()).isEqualTo("https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=GetRecord&identifier=http%3A%2F%2Fmy-example_url%3A80%2Fobj%2FfStudy%2Fch.sidos.ddi.468.7773&metadataPrefix=ddi");
    }
}
