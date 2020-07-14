/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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

package eu.cessda.pasc.oci.service.impl;

import eu.cessda.pasc.oci.exception.InternalSystemException;
import eu.cessda.pasc.oci.exception.OaiPmhException;
import eu.cessda.pasc.oci.helpers.CMMStudyMapper;
import eu.cessda.pasc.oci.helpers.OaiPmhHelpers;
import eu.cessda.pasc.oci.helpers.RecordResponseValidator;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.repository.DaoBase;
import eu.cessda.pasc.oci.service.GetRecordService;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Service Class responsible for querying the repository to fetch remote records.
 *
 * @author moses AT doraventures DOT com
 */
@Service
@Slf4j
public class GetRecordServiceImpl implements GetRecordService {

    private final CMMStudyMapper cmmStudyMapper;
    private final DaoBase daoBase;
    private final RecordResponseValidator recordResponseValidator;

    @Autowired
    public GetRecordServiceImpl(CMMStudyMapper cmmStudyMapper, DaoBase daoBase, RecordResponseValidator recordResponseValidator) {
        this.daoBase = daoBase;
        this.cmmStudyMapper = cmmStudyMapper;
        this.recordResponseValidator = recordResponseValidator;
    }

    @Override
    public CMMStudy getRecord(Repo repo, String studyIdentifier) throws InternalSystemException, OaiPmhException {
        log.debug("[{}] Querying for StudyID [{}]", repo.getCode(), studyIdentifier);
        URI fullUrl = null;
        try {
            fullUrl = OaiPmhHelpers.buildGetStudyFullUrl(repo, studyIdentifier);
            try (InputStream recordXML = daoBase.getInputStream(fullUrl)) {
                return mapDDIRecordToCMMStudy(recordXML, repo).studyXmlSourceUrl(fullUrl.toString()).build();
            }
        } catch (JDOMException | IOException e) {
            throw new InternalSystemException(String.format("Unable to parse xml! FullUrl [%s]: %s", fullUrl, e), e);
        } catch (URISyntaxException e) {
            throw new InternalSystemException(e);
        }
    }

    private CMMStudy.CMMStudyBuilder mapDDIRecordToCMMStudy(InputStream recordXML, Repo repository) throws JDOMException, IOException, OaiPmhException {

        CMMStudy.CMMStudyBuilder builder = CMMStudy.builder();
        Document document = getSaxBuilder().build(recordXML);

        if (log.isTraceEnabled()) {
            log.trace("Record XML String [{}]", new XMLOutputter().outputString(document));
        }

        // We exit if the record has an <error> element
        recordResponseValidator.validateResponse(document);

        // Short-Circuit. We carry on to parse beyond the headers only if the record is active.
        var headerElement = cmmStudyMapper.parseHeaderElement(document);
        headerElement.getStudyNumber().ifPresent(builder::studyNumber);
        headerElement.getLastModified().ifPresent(builder::lastModified);
        builder.active(headerElement.isRecordActive());
        if (headerElement.isRecordActive()) {
            String defaultLangIsoCode = cmmStudyMapper.parseDefaultLanguage(document, repository);
            builder.titleStudy(cmmStudyMapper.parseStudyTitle(document, defaultLangIsoCode));
            builder.studyUrl(cmmStudyMapper.parseStudyUrl(document, defaultLangIsoCode));
            builder.abstractField(cmmStudyMapper.parseAbstract(document, defaultLangIsoCode));
            builder.pidStudies(cmmStudyMapper.parsePidStudies(document, defaultLangIsoCode));
            builder.creators(cmmStudyMapper.parseCreator(document, defaultLangIsoCode));
            builder.dataAccessFreeTexts(cmmStudyMapper.parseDataAccessFreeText(document, defaultLangIsoCode));
            builder.classifications(cmmStudyMapper.parseClassifications(document, defaultLangIsoCode));
            builder.keywords(cmmStudyMapper.parseKeywords(document, defaultLangIsoCode));
            builder.typeOfTimeMethods(cmmStudyMapper.parseTypeOfTimeMethod(document, defaultLangIsoCode));
            builder.studyAreaCountries(cmmStudyMapper.parseStudyAreaCountries(document, defaultLangIsoCode));
            builder.unitTypes(cmmStudyMapper.parseUnitTypes(document, defaultLangIsoCode));
            builder.publisher(cmmStudyMapper.parsePublisher(document, defaultLangIsoCode));
            cmmStudyMapper.parseYrOfPublication(document).ifPresent(builder::publicationYear);
            builder.fileLanguages(cmmStudyMapper.parseFileLanguages(document));
            builder.typeOfSamplingProcedures(cmmStudyMapper.parseTypeOfSamplingProcedure(document, defaultLangIsoCode));
            builder.samplingProcedureFreeTexts(cmmStudyMapper.parseSamplingProcedureFreeTexts(document, defaultLangIsoCode));
            builder.typeOfModeOfCollections(cmmStudyMapper.parseTypeOfModeOfCollection(document, defaultLangIsoCode));
            var dataCollectionPeriod = cmmStudyMapper.parseDataCollectionDates(document);
            dataCollectionPeriod.getStartDate().ifPresent(builder::dataCollectionPeriodStartdate);
            dataCollectionPeriod.getEndDate().ifPresent(builder::dataCollectionPeriodEnddate);
            builder.dataCollectionYear(dataCollectionPeriod.getDataCollectionYear());
            builder.dataCollectionFreeTexts(cmmStudyMapper.parseDataCollectionFreeTexts(document, defaultLangIsoCode));
        }
        return builder;
    }

    private SAXBuilder getSaxBuilder() {
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return saxBuilder;
    }
}
