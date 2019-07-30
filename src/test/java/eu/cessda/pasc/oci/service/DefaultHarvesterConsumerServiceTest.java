/**
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
package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.AbstractSpringTestProfileContext;
import eu.cessda.pasc.oci.helpers.FileHandler;
import eu.cessda.pasc.oci.helpers.TimeUtility;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.repository.HarvesterDao;
import eu.cessda.pasc.oci.service.impl.DefaultHarvesterConsumerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static eu.cessda.pasc.oci.data.RecordTestData.*;
import static eu.cessda.pasc.oci.data.ReposTestData.getUKDSRepo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.when;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
public class DefaultHarvesterConsumerServiceTest extends AbstractSpringTestProfileContext {

  @Mock
  private HarvesterDao harvesterDao;

  @InjectMocks
  @Autowired
  DefaultHarvesterConsumerService defaultHarvesterConsumerService;

  @Test
  public void shouldReturnASuccessfulResponseForListingRecordHeaders() throws ExternalSystemException {

    when(harvesterDao.listRecordHeaders(anyString())).thenReturn(LIST_RECORDER_HEADERS_BODY_EXAMPLE);
    Repo repo = getUKDSRepo();

    List<RecordHeader> recordHeaders = defaultHarvesterConsumerService.listRecordHeaders(repo, null);
    assertThat(recordHeaders).hasSize(2);
  }

  @Test
  public void shouldReturnFilterRecordHeadersByLastModifiedDate() throws ExternalSystemException {
    Repo repo = getUKDSRepo();
    LocalDateTime lastModifiedDateCutOff = TimeUtility.getLocalDateTime("2018-02-01T07:48:38Z").get();

    when(harvesterDao.listRecordHeaders(anyString())).thenReturn(LIST_RECORDER_HEADERS_X6);
    List<RecordHeader> recordHeaders = defaultHarvesterConsumerService.listRecordHeaders(repo, lastModifiedDateCutOff);

    assertThat(recordHeaders).hasSize(2);
    recordHeaders.forEach(recordHeader -> {
      LocalDateTime currentLastModified = TimeUtility.getLocalDateTime(recordHeader.getLastModified()).orElse(null);
      then(currentLastModified).isGreaterThan(lastModifiedDateCutOff);
    });
  }

  @Test
  public void shouldFilterRecordHeadersByLastModifiedDateAndInvalidLastDateTimeStrings() throws ExternalSystemException {
    Repo repo = getUKDSRepo();
    LocalDateTime lastModifiedCutOff = TimeUtility.getLocalDateTime("2018-02-10T07:48:38Z").orElse(null);

    when(harvesterDao.listRecordHeaders(anyString())).thenReturn(LIST_RECORDER_HEADERS_WITH_INVALID_DATETIME);
    List<RecordHeader> recordHeaders = defaultHarvesterConsumerService.listRecordHeaders(repo, lastModifiedCutOff);

    assertThat(recordHeaders).hasSize(1);
    recordHeaders.forEach(recordHeader -> {
      LocalDateTime currentLastModified = TimeUtility.getLocalDateTime(recordHeader.getLastModified()).orElse(null);
      then(currentLastModified).isGreaterThan(lastModifiedCutOff);
    });
  }

  @Test
  public void shouldReturnUnFilterRecordHeadersWhenLastModifiedDateIsNull() throws ExternalSystemException {

    // FIXME: add more mocks to LIST_RECORDER_HEADERS_BODY_EXAMPLE with very old timestamps to filter out
    when(harvesterDao.listRecordHeaders(anyString())).thenReturn(LIST_RECORDER_HEADERS_X6);
    Repo repo = getUKDSRepo();

    List<RecordHeader> recordHeaders = defaultHarvesterConsumerService.listRecordHeaders(repo, null);
    assertThat(recordHeaders).hasSize(6);
  }

  @Test
  public void shouldReturnASuccessfulResponseGetRecord() throws ExternalSystemException {
    FileHandler fileHandler = new FileHandler();
    String recordUkds998 = fileHandler.getFileWithUtil("record_ukds_998.json");
    String recordID = "998";

    when(harvesterDao.getRecord(anyString(), anyString())).thenReturn(recordUkds998);
    Repo repo = getUKDSRepo();

    Optional<CMMStudy> cmmStudy = defaultHarvesterConsumerService.getRecord(repo, recordID);

    assertThat(cmmStudy.isPresent()).isTrue();
    then(cmmStudy.get().getStudyNumber()).isEqualTo("998");
    then(cmmStudy.get().getLastModified()).isEqualTo("2018-02-22T07:48:38Z");
    then(cmmStudy.get().getKeywords()).hasSize(1);
    then(cmmStudy.get().getKeywords().get("en")).hasSize(62);
  }

  @Test
  public void shouldReturnDeletedRecordMarkedAsInactive() throws ExternalSystemException {
    FileHandler fileHandler = new FileHandler();
    String recordUkds1031 = fileHandler.getFileWithUtil("record_ukds_1031_deleted.json");
    String recordID = "1031";

    when(harvesterDao.getRecord(anyString(), matches(recordID))).thenReturn(recordUkds1031);
    Repo repo = getUKDSRepo();

    Optional<CMMStudy> cmmStudy = defaultHarvesterConsumerService.getRecord(repo, recordID);

    assertThat(cmmStudy.isPresent()).isTrue();
    then(cmmStudy.get().getStudyNumber()).isEqualTo("1031");
    then(cmmStudy.get().getLastModified()).isEqualTo("2017-05-02T08:31:32Z");
    then(cmmStudy.get().isActive()).isFalse();
    then(cmmStudy.get().getDataCollectionPeriodEnddate()).isNull();
    then(cmmStudy.get().getAbstractField()).isNull();
    then(cmmStudy.get().getTitleStudy()).isNull();
    then(cmmStudy.get().getPublisher()).isNull();
    then(cmmStudy.get().getKeywords()).isNull();
    then(cmmStudy.get().getCreators()).isNull();
  }
}
