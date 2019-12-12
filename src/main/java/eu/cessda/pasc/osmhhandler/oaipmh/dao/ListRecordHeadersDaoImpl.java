/*
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.osmhhandler.oaipmh.dao;

import eu.cessda.pasc.osmhhandler.oaipmh.configuration.HandlerConfigurationProperties;
import eu.cessda.pasc.osmhhandler.oaipmh.configuration.UtilitiesConfiguration;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import eu.cessda.pasc.osmhhandler.oaipmh.exception.ExternalSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhHelpers.appendListRecordParams;

/**
 * Data access object contract implementation for querying remote repository for RecordHeaders.
 *
 * @author moses AT doraventures DOT com
 */
@Repository
public class ListRecordHeadersDaoImpl extends DaoBase implements ListRecordHeadersDao {

  private HandlerConfigurationProperties config;

  @Autowired
  public ListRecordHeadersDaoImpl(HandlerConfigurationProperties config, UtilitiesConfiguration configuration) {
    super(configuration);
    this.config = config;
  }

  @Override
  public String listRecordHeaders(String baseRepoUrl) throws CustomHandlerException {
    String finalListRecordUrl = appendListRecordParams(baseRepoUrl, config.getOaiPmh());
    return postForStringResponse(finalListRecordUrl);
  }

  @Override
  public String listRecordHeadersResumption(String repoUrlWithResumptionToken) throws ExternalSystemException {
    return postForStringResponse(repoUrlWithResumptionToken);
  }
}
