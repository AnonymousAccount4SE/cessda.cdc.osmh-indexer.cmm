/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.exception.IndexerException;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyConverter;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class RecordXMLParserNesstarTest {

    private final Repo nesstarRepo = ReposTestData.getNSDRepo();

    private final CMMStudyMapper cmmStudyMapper = new CMMStudyMapper();
    private final CMMStudyConverter cmmConverter = new CMMStudyConverter();
    private final ObjectMapper mapper = new ObjectMapper();

    private static HashMap<String, String> getAbstractFixture() {
        var expectedAbstract = new HashMap<String, String>();
        expectedAbstract.put("de", "de de");
        expectedAbstract.put("fi", "Haastattelu");
        expectedAbstract.put("en", "First: English abstract<br>Second: English abstract");
        expectedAbstract.put("xy", "Jyväskylä<br>Jyväskylä");
        return expectedAbstract;
    }

    private static HashMap<String, String> getTitleFixture() {
        var expectedTitle = new HashMap<String, String>();
        expectedTitle.put("xy", "European Social Survey in Switzerland - 2004");
        expectedTitle.put("yy", "Enquête sociale européenne");
        return expectedTitle;
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyComplaintCmmDdiRecord() throws IOException, ProcessingException, JSONException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar.xml");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(result).hasSize(1);
        validateCMMStudyResultAgainstSchema(result.get(0));

        var actualJson = cmmConverter.toJsonString(result.get(0));
        var expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record_nesstar.json");

        // Compare the generated JSON to the expected result
        assertEquals("Generated JSON differs from expected JSON\n", expectedJson, actualJson, true);
    }

    @Test
    public void shouldReturnValidCMMStudyRecordFromOaiPmhDDI2_5MetadataRecord() throws IOException, ProcessingException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(record).hasSize(1);
        validateCMMStudyResultAgainstSchema(record.get(0));
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldOnlyExtractSingleDateAsStartDateForRecordsWithASingleDateAttr() throws IOException, ProcessingException, IndexerException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar_single_date.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));
        then(record).hasSize(1);
        validateCMMStudyResultAgainstSchema(record.get(0));
        var jsonString = cmmConverter.toJsonString(record.get(0));
        var actualTree = mapper.readTree(jsonString);

        // Then
        then(actualTree.get("dataCollectionPeriodStartdate").asText()).isEqualTo("2004-09-16");
        then(actualTree.get("dataCollectionPeriodEnddate")).isNull();
        then(actualTree.get("dataCollectionYear").asInt()).isEqualTo(2004);
    }

    @Test
    public void shouldExtractDefaultLanguageFromCodebookXMLLagIfPresent() throws IndexerException, IOException, URISyntaxException {

        // Given
        var expectedCmmStudyJsonString = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(expectedCmmStudyJsonString.toURI())).get(0);

        // Then
        then(record.getTitleStudy().containsKey("xy")).isTrue();
        then(record.getAbstractField().containsKey("xy")).isTrue();
        then(record.getKeywords().containsKey("xy")).isTrue();
        then(record.getDataAccessFreeTexts().containsKey("xy")).isTrue();
    }

    @Test
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldReturnCMMStudyRecordWithRepeatedAbstractConcatenated() throws IndexerException, IOException, ProcessingException, URISyntaxException {

        // Given
        var expectedAbstract = getAbstractFixture();
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar_repeated_abstract.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(record).hasSize(1);
        then(record.get(0).getAbstractField().size()).isEqualTo(4);
        then(record.get(0).getAbstractField()).isEqualTo(expectedAbstract);
        validateCMMStudyResultAgainstSchema(record.get(0));
    }

    @Test // https://github.com/cessda/cessda.cdc.versions/issues/135
    @SuppressWarnings("PreferJavaTimeOverload")
    public void shouldReturnCMMStudyRecordWithOutParTitleWhenThereIsALangDifferentFromDefault() throws IndexerException, IOException, ProcessingException, URISyntaxException {

        // Given
        var expectedTitle = getTitleFixture();

        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar_with_perTitl_xml_lang.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(record).hasSize(1);
        then(record.get(0).getTitleStudy().size()).isEqualTo(2);
        then(record.get(0).getTitleStudy()).isEqualTo(expectedTitle);
        validateCMMStudyResultAgainstSchema(record.get(0));
    }

    @Test()
    public void shouldReturnEmptyOptionalFromOaiPmhDDI2_5MetadataRecord_MarkedAsNotActive()
        throws IndexerException, IOException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_nesstar_deleted_record.xml");

        // When
        var record = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(record).isEmpty();
    }

    @Test
    public void shouldThrowExceptionForRecordWithErrorElement() throws IndexerException, IOException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_nesstar_record_with_error.xml");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(result).isEmpty();
    }

    @Test
    public void shouldExtractAllRequiredCMMFieldsForAGivenAUKDSRecord() throws IndexerException, IOException, ProcessingException, JSONException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar.xml");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(nesstarRepo, Path.of(recordXML.toURI()));

        // Then
        then(result).hasSize(1);
        validateCMMStudyResultAgainstSchema(result.get(0));
        assertThatCmmRequiredFieldsAreExtracted(result.get(0));
    }

    private void validateCMMStudyResultAgainstSchema(CMMStudy record) throws IOException, ProcessingException {
        var jsonString = cmmConverter.toJsonString(record);

        var jsonNodeRecord = JsonLoader.fromString(jsonString);
        var schema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/json/schema/CMMStudy.schema.json");

        var validate = schema.validate(jsonNodeRecord);
        if (!validate.isSuccess()) {
            fail("Validation not successful: " + validate);
        }
    }

    private void assertThatCmmRequiredFieldsAreExtracted(CMMStudy record) throws JSONException, IOException {

        var jsonString = cmmConverter.toJsonString(record);
        var expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record_nesstar.json");
        final var actualTree = mapper.readTree(jsonString);
        final var expectedTree = mapper.readTree(expectedJson);

        // CMM Model Schema required fields
        assertEquals(expectedTree.get("abstract").toString(), actualTree.get("abstract").toString(), true);
        assertEquals(expectedTree.get("titleStudy").toString(), actualTree.get("titleStudy").toString(), true);
        assertEquals(expectedTree.get("studyUrl").toString(), actualTree.get("studyUrl").toString(), true);
        then(expectedTree.get("studyNumber").toString()).isEqualTo(actualTree.get("studyNumber").toString());
        assertEquals(expectedTree.get("publisher").toString(), actualTree.get("publisher").toString(), true);
    }

    @Test
    public void shouldOverrideGlobalLanguageDefaultIfAPerRepositoryOverrideIsSpecified() throws IndexerException, IOException, ProcessingException, URISyntaxException {

        // Given
        var recordXML = ResourceHandler.getResource("xml/nesstar/synthetic_compliant_cmm_nesstar_no_language.xml");

        var langRepo = ReposTestData.getNSDRepo();
        langRepo.setDefaultLanguage("zz");

        // When
        var result = new RecordXMLParser(cmmStudyMapper).getRecord(langRepo, Path.of(recordXML.toURI()));

        then(result).hasSize(1);
        validateCMMStudyResultAgainstSchema(result.get(0));

        // Assert the language is as expected
        Assert.assertNotNull(result.get(0).getTitleStudy().get("zz"));
    }
}
