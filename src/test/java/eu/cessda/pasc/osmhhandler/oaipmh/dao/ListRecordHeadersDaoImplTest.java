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

package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import com.pgssoft.httpclient.HttpClientMock;
import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class ListRecordHeadersDaoImplTest {

    @Autowired
    private HandlerConfigurationProperties handlerConfigurationProperties;

    private HttpClientMock httpClient = new HttpClientMock();

    @Test
    public void shouldReturnXmlPayloadOfRecordHeadersFromRemoteRepository() throws CustomHandlerException, IOException {

        // Given
        String fullListRecordHeadersUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=ListIdentifiers&metadataPrefix=ddi";

        httpClient.onGet(fullListRecordHeadersUrl).doReturn(fullListRecordHeadersUrl, StandardCharsets.UTF_8);

        // When
        ListRecordHeadersDao listRecordHeadersDao = new ListRecordHeadersDaoImpl(httpClient);
        try (InputStream inputStream = listRecordHeadersDao.listRecordHeaders(fullListRecordHeadersUrl)) {
            String recordHeadersXML = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Actual: " + recordHeadersXML);

            then(recordHeadersXML).isNotNull();
            then(recordHeadersXML).isNotEmpty();
            then(recordHeadersXML).contains(fullListRecordHeadersUrl);
        }
    }

    @Test
    public void shouldReturnXmlPayloadOfGivenSpecSetRecordHeadersFromRemoteRepository() throws CustomHandlerException, IOException {

        // Given
        String fullListRecordHeadersUrl = "http://services.fsd.uta.fi/v0/oai?verb=ListIdentifiers&metadataPrefix=oai_ddi25&set=study_groups:energia";

        httpClient.onGet(fullListRecordHeadersUrl).doReturn(fullListRecordHeadersUrl, StandardCharsets.UTF_8);

        // When
        ListRecordHeadersDao listRecordHeadersDao = new ListRecordHeadersDaoImpl(httpClient);
        try (InputStream inputStream = listRecordHeadersDao.listRecordHeaders(fullListRecordHeadersUrl)) {
            String recordHeadersXML = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            System.out.println("Actual: " + recordHeadersXML);

            then(recordHeadersXML).isNotNull();
            then(recordHeadersXML).isNotEmpty();
            then(recordHeadersXML).contains(fullListRecordHeadersUrl);
        }
    }
}