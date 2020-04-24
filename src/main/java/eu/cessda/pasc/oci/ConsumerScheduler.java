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
package eu.cessda.pasc.oci;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.metrics.Metrics;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.HarvesterConsumerService;
import eu.cessda.pasc.oci.service.IngestService;
import eu.cessda.pasc.oci.service.helpers.DebuggingJMXBean;
import eu.cessda.pasc.oci.service.helpers.LanguageAvailabilityMapper;
import eu.cessda.pasc.oci.service.helpers.LanguageDocumentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static net.logstash.logback.argument.StructuredArguments.value;

/**
 * Service responsible for triggering Metadata Harvesting and ingestion into the search engine
 *
 * @author moses AT doraventures DOT com
 */
@Component
@ManagedResource
@Slf4j
public class ConsumerScheduler {

  private static final String FULL_RUN = "Full Run";
  private static final String DAILY_INCREMENTAL_RUN = "Daily Incremental Run";
  private static final String REPO_NAME = "repo_name";
  private static final String LANG_CODE = "lang_code";
  private static final String REPO_ENDPOINT_URL = "repo_endpoint_url";
  private static final String DEFAULT_CDC_JOB_KEY = "indexer_job_id";
  private static final String DEFAULT_RESPONSE_TOKEN_HEADER = "cdc-";

  private final DebuggingJMXBean debuggingJMXBean;
  private final AppConfigurationProperties configurationProperties;
  private final HarvesterConsumerService harvesterConsumerService;
  private final IngestService esIndexerService;
  private final LanguageDocumentExtractor extractor;
  private final LanguageAvailabilityMapper languageAvailabilityMapper;
  private final Metrics micrometerMetrics;

  @Autowired
  public ConsumerScheduler(DebuggingJMXBean debuggingJMXBean, AppConfigurationProperties configurationProperties,
                           HarvesterConsumerService harvesterConsumerService, IngestService esIndexerService,
                           LanguageDocumentExtractor extractor, LanguageAvailabilityMapper languageAvailabilityMapper,
                           Metrics metrics) {
    this.debuggingJMXBean = debuggingJMXBean;
    this.configurationProperties = configurationProperties;
    this.harvesterConsumerService = harvesterConsumerService;
    this.esIndexerService = esIndexerService;
    this.extractor = extractor;
    this.languageAvailabilityMapper = languageAvailabilityMapper;
    this.micrometerMetrics = metrics;
  }

  /**
   * Auto Starts after delay of given time at startup.
   */
  @ManagedOperation(description = "Manual trigger to do a full harvest and ingest run")
  @Scheduled(initialDelayString = "${osmhConsumer.delay.initial}", fixedDelayString = "${osmhConsumer.delay.fixed}")
  public void fullHarvestAndIngestionAllConfiguredSPsReposRecords() {
    try (var ignored = MDC.putCloseable(ConsumerScheduler.DEFAULT_CDC_JOB_KEY, getCdcRunjobId())) {
      OffsetDateTime date = OffsetDateTime.now(ZoneId.systemDefault());
      logStartStatus(date, FULL_RUN);
      executeHarvestAndIngest(null);
      logEndStatus(date, FULL_RUN);
    }
  }

  /**
   * Daily Harvest and Ingestion run.
   */
  @ManagedOperation(description = "Manual trigger to do an incremental harvest and ingest")
  @Scheduled(cron = "${osmhConsumer.daily.run}")
  public void dailyIncrementalHarvestAndIngestionAllConfiguredSPsReposRecords() {
    try (var ignored = MDC.putCloseable(ConsumerScheduler.DEFAULT_CDC_JOB_KEY, getCdcRunjobId())) {
      OffsetDateTime date = OffsetDateTime.now(ZoneId.systemDefault());
      logStartStatus(date, DAILY_INCREMENTAL_RUN);
      executeHarvestAndIngest(esIndexerService.getMostRecentLastModified().orElse(null));
      logEndStatus(date, DAILY_INCREMENTAL_RUN);
    }
  }

  /**
   * Weekly run.
   */
  @Scheduled(cron = "${osmhConsumer.daily.sunday.run}")
  public void weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords() {
    log.info("Once a Week Full Run. Triggered by cron - STARTED");
    fullHarvestAndIngestionAllConfiguredSPsReposRecords();
    log.info("Once a Week Full Run. Triggered by cron - ENDED");
  }

  /**
   * Gets the correlation id of this run
   *
   * @return the correlation id
   */
  private String getCdcRunjobId() {
    return DEFAULT_RESPONSE_TOKEN_HEADER + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneId.systemDefault()));
  }

  private void executeHarvestAndIngest(LocalDateTime lastModifiedDateTime) {
    List<Repo> repos = configurationProperties.getEndpoints().getRepos();
    repos.forEach(repo -> {
      Map<String, List<CMMStudyOfLanguage>> langStudies = getCmmStudiesOfEachLangIsoCodeMap(repo, lastModifiedDateTime);
      langStudies.forEach((langIsoCode, cmmStudies) -> executeBulk(repo, langIsoCode, cmmStudies));
    });
    micrometerMetrics.updateMetrics();
    log.info("Total number of records stands {}", keyValue("total_cmm_studies", esIndexerService.getTotalHitCount("*")));
  }


  private void executeBulk(Repo repo, String langIsoCode, List<CMMStudyOfLanguage> cmmStudies) {
    if (cmmStudies.isEmpty()) {
      log.debug("CmmStudies list is empty and henceforth there is nothing to BulkIndex for repo[{}] with LangIsoCode [{}].", keyValue(REPO_NAME, repo.getName()), keyValue(LANG_CODE, langIsoCode));
    } else {
      int studiesUpdated = getUpdatedStudies(langIsoCode, cmmStudies);
      log.info("[{}] studies updated", value("updated_cmm_studies", studiesUpdated));
      log.info("BulkIndexing repo [{}] with lang code [{}] index with [{}] CmmStudies", keyValue(REPO_NAME, repo.getName()), keyValue(LANG_CODE, langIsoCode), keyValue("cmm_studies_added", cmmStudies.size()));
      boolean isSuccessful = esIndexerService.bulkIndex(cmmStudies, langIsoCode);
      if (isSuccessful) {
        log.info("BulkIndexing was Successful. For repo name [{}] and its corresponding [{}].  LangIsoCode [{}].", keyValue(REPO_NAME, repo.getName()), keyValue(REPO_ENDPOINT_URL, repo.getUrl()), keyValue(LANG_CODE, langIsoCode));
      } else {
        log.error("BulkIndexing was UnSuccessful. For repo name [{}] and its corresponding [{}].  LangIsoCode [{}].", keyValue(REPO_NAME, repo.getName()), keyValue(REPO_ENDPOINT_URL, repo.getUrl()), keyValue(LANG_CODE, langIsoCode));
      }
    }
  }

  private int getUpdatedStudies(String langIsoCode, List<CMMStudyOfLanguage> cmmStudies) {
    var studiesUpdated = new AtomicInteger(0);
    cmmStudies.parallelStream().forEach(study -> {
      var esStudy = esIndexerService.getStudy(study.getId(), langIsoCode);
      if (esStudy.isEmpty() || !study.equals(esStudy.get())) {
        studiesUpdated.getAndIncrement();
      }
    });
    return studiesUpdated.get();
  }

  private Map<String, List<CMMStudyOfLanguage>> getCmmStudiesOfEachLangIsoCodeMap(Repo repo, LocalDateTime lastModifiedDateTime) {
    log.info("Processing Repo [{}]", repo);
    List<RecordHeader> recordHeaders = harvesterConsumerService.listRecordHeaders(repo, lastModifiedDateTime);

    int recordHeadersSize = recordHeaders.size();
    log.info("Repo [{}].  Returned with [{}] record headers", repo, recordHeadersSize);

    List<Optional<CMMStudy>> totalCMMStudies = recordHeaders.stream()
            .map(recordHeader -> harvesterConsumerService.getRecord(repo, recordHeader.getIdentifier()))
            .collect(Collectors.toList());

    List<CMMStudy> presentCMMStudies = totalCMMStudies.stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    String msgTemplate = "Repo Name [{}] of [{}] Endpoint. There are [{}] presentCMMStudies out of [{}] totalCMMStudies from [{}] Record Identifiers. Therefore CMMStudiesRejected is  "
            + "";
    log.info(msgTemplate + "" + (totalCMMStudies.size() - presentCMMStudies.size()), keyValue(REPO_NAME, repo.getName()), keyValue(REPO_ENDPOINT_URL, repo.getUrl()), keyValue("present_cmm_record", presentCMMStudies.size()), totalCMMStudies.size(), keyValue("cmm_records_rejected", totalCMMStudies.size() - presentCMMStudies.size()));

    presentCMMStudies.forEach(languageAvailabilityMapper::setAvailableLanguages);
    return extractor.mapLanguageDoc(presentCMMStudies, repo.getName());
  }

  private void logStartStatus(OffsetDateTime localDateTime, String runDescription) {
    log.info("[{}] Consume and Ingest All SPs Repos : Started at [{}]", runDescription, localDateTime);
    log.info("Currents state before run");
    log.info(debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints());
    log.info(debuggingJMXBean.printElasticSearchInfo());
  }

  private void logEndStatus(OffsetDateTime offsetDateTime, String runDescription) {
    String formatMsg = "[{}] Consume and Ingest All SPs Repos Ended at [{}], Duration [{}]";
    log.info(formatMsg, runDescription, offsetDateTime, keyValue("job_duration", Duration.between(offsetDateTime.toInstant(), Instant.now()).getSeconds()));

  }

}
