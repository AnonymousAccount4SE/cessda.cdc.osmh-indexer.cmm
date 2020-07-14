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

package eu.cessda.pasc.oci.helpers;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.net.URISyntaxException;

import static eu.cessda.pasc.oci.helpers.OaiPmhHelpers.buildGetStudyFullUrl;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Test class for {@link OaiPmhHelpers}
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class OaiPmhHelpersTest {


    @Autowired
    private AppConfigurationProperties appConfigurationProperties;

    @Test
    public void ShouldAppendMetaDataPrefixForGivenFSD() throws URISyntaxException {

        // Given
        Repo fsdEndpoint = appConfigurationProperties.getEndpoints().getRepos()
                .stream().filter(repo -> repo.getCode().equals("FSD")).findAny().orElseThrow();
        String expectedReqUrl = "http://services.fsd.uta.fi/v0/oai?verb=GetRecord&identifier=15454&metadataPrefix=oai_ddi25";

        // When
        URI builtUrl = buildGetStudyFullUrl(fsdEndpoint, "15454");

        then(builtUrl.toString()).isEqualTo(expectedReqUrl);
    }

    @Test
    public void ShouldAppendMetaDataPrefixForGivenUKDS() throws URISyntaxException {

        // Given
        Repo ukdsEndpoint = appConfigurationProperties.getEndpoints().getRepos()
                .stream().filter(repo -> repo.getCode().equals("UKDS")).findAny().orElseThrow();
        String expectedReqUrl = "https://oai.ukdataservice.ac.uk:8443/oai/provider?verb=GetRecord&identifier=15454&metadataPrefix=ddi";

        // When
        URI builtUrl = buildGetStudyFullUrl(ukdsEndpoint, "15454");

        then(builtUrl.toString()).isEqualTo(expectedReqUrl);
    }
}