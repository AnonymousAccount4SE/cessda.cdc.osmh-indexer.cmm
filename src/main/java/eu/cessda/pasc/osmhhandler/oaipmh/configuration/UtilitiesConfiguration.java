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

package eu.cessda.pasc.osmhhandler.oaipmh.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Extra Util configuration
 *
 * @author moses AT doraventures DOT com
 */
@Configuration
@Slf4j
public class UtilitiesConfiguration {

  private final HandlerConfigurationProperties handlerConfigurationProperties;

  @Autowired
  public UtilitiesConfiguration(HandlerConfigurationProperties handlerConfigurationProperties) {
    this.handlerConfigurationProperties = handlerConfigurationProperties;
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public DocumentBuilderFactory documentBuilderFactory() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setNamespaceAware(true);
    return factory;
  }

  @Bean
  public HttpClient httpClient() {
    return HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(handlerConfigurationProperties.getRestTemplateProps().getConnTimeout()))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  @Bean
  public org.jdom2.xpath.XPathFactory jdomXPathFactory() {
    return org.jdom2.xpath.XPathFactory.instance();
  }
}
