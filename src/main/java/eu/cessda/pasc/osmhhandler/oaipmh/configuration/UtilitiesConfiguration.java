package eu.cessda.pasc.osmhhandler.oaipmh.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import static org.apache.http.ssl.SSLContexts.custom;

/**
 * Extra Util configuration
 *
 * @author moses@doraventures.com
 */
@Configuration
@Slf4j
public class UtilitiesConfiguration {

  @Autowired
  HandlerConfigurationProperties handlerConfigurationProperties;

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public DocumentBuilder documentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder();
  }

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());
    restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
    return restTemplate;
  }

  @Bean
  public RestTemplate restTemplateWithNoSSLVerification() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactoryWithoutSSL());
    restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
    return restTemplate;
  }

  private ClientHttpRequestFactory getClientHttpRequestFactory() {
    HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
    clientHttpRequestFactory.setConnectTimeout(handlerConfigurationProperties.getRestTemplateProps().getConnTimeout());
    clientHttpRequestFactory.setReadTimeout(handlerConfigurationProperties.getRestTemplateProps().getReadTimeout());
    clientHttpRequestFactory.setConnectionRequestTimeout(handlerConfigurationProperties.getRestTemplateProps()
        .getConnRequestTimeout());
    return clientHttpRequestFactory;
  }

  // FIXME:  A "temp" to work around untrusted certificate for UKDA oai-pmh endpoint
  /**
   * Builds a {@link ClientHttpRequestFactory} with ssl off.
   */
  private ClientHttpRequestFactory getClientHttpRequestFactoryWithoutSSL()
      throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

    TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
    SSLContext sslContext = custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
    SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
    CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();

    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
    requestFactory.setConnectTimeout(handlerConfigurationProperties.getRestTemplateProps().getConnTimeout());
    requestFactory.setReadTimeout(handlerConfigurationProperties.getRestTemplateProps().getReadTimeout());
    requestFactory.setConnectionRequestTimeout(handlerConfigurationProperties.getRestTemplateProps().getConnRequestTimeout());
    requestFactory.setHttpClient(httpClient);
    return requestFactory;
  }

  public RestTemplate getRestTemplate() {
    if (handlerConfigurationProperties.getRestTemplateProps().isVerifySSL()) {
      return restTemplate();
    }

    try {
      return restTemplateWithNoSSLVerification();
    } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
      log.error("Failed to build restTemplate with SSL off, building and return default Template with SSL on");
      return restTemplate();
    }
  }
}
