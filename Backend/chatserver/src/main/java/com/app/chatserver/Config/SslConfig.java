// package com.app.chatserver.Config;

// import org.apache.catalina.connector.Connector;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
// import org.springframework.boot.web.server.WebServerFactoryCustomizer;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Profile;

// @Configuration
// @Profile("ssl") // ✅ chỉ kích hoạt khi bật profile ssl
// public class SslConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

//     @Value("${server.ssl.key-store}")
//     private String keyStore;

//     @Value("${server.ssl.key-store-password}")
//     private String keyStorePassword;

//     @Override
//     public void customize(TomcatServletWebServerFactory factory) {
//         factory.addAdditionalTomcatConnectors(createSslConnector());
//     }

//     private Connector createSslConnector() {
//         Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
//         connector.setPort(8443);
//         connector.setSecure(true);
//         connector.setScheme("https");

//         connector.setProperty("keystoreFile", keyStore);
//         connector.setProperty("keystorePass", keyStorePassword);
//         connector.setProperty("keystoreType", "PKCS12");
//         connector.setProperty("sslProtocol", "TLS");
//         connector.setProperty("SSLEnabled", "true");
//         return connector;
//     }
// }
