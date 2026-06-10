/* Copyright (C) Red Hat 2023 */
package com.redhat.jws.insights;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.redhat.insights.jars.JarAnalyzer;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.reports.InsightsSubreport;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.IntrospectionUtils;

public class JWSSubreportSerializer extends JsonSerializer<InsightsSubreport> {

    private static final Log log = LogFactory.getLog(JWSSubreportSerializer.class);

    private final Server server;
    private final InsightsLogger logger;

    JWSSubreportSerializer(Server server, InsightsLogger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void serialize(InsightsSubreport subreport, JsonGenerator generator, SerializerProvider serializerProvider)
            throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Serializing JWS subreport");
        }
        generator.writeStartObject();
        generator.writeStringField("version", subreport.getVersion());
        JarAnalyzer analyzer = new JarAnalyzer(logger, true);
        Service[] services = server.findServices();
        for (Service service : services) {
            Connector[] connectors = service.findConnectors();
            generator.writeFieldName("connectors");
            generator.writeStartArray();
            for (Connector connector : connectors) {
                String name = String.valueOf(IntrospectionUtils.getProperty(connector, "name"));
                generator.writeStartObject();
                if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
                    name = name.substring(1, name.length() - 1);
                }
                generator.writeStringField("name", name);
                generator.writeEndObject();
            }
            generator.writeEndArray();
            if (service.getContainer() == null) {
                // No engine, so nothing more to do
                break;
            }
            Container[] hosts = service.getContainer().findChildren();
            generator.writeFieldName("hosts");
            generator.writeStartArray();
            for (Container host : hosts) {
                String hostName = host.getName();
                generator.writeStartObject(); //host
                generator.writeStringField("name", hostName);
                Container[] contexts = host.findChildren();
                generator.writeFieldName("contexts");
                generator.writeStartArray(); //contexts
                for (Container context : contexts) {
                    String contextName = ((Context) context).getBaseName();
                    if (log.isDebugEnabled()) {
                        log.debug("Processing context: " + contextName);
                    }
                    generator.writeStartObject(); //context
                    generator.writeStringField("name", contextName);
                    ArrayList<JarInfo> jarInfos = new ArrayList<>();
                    Loader loader = ((Context) context).getLoader();
                    if (loader != null && loader.getClassLoader() instanceof URLClassLoader) {
                        URLClassLoader classLoader = (URLClassLoader) loader.getClassLoader();
                        URL[] urls = classLoader.getURLs();
                        for (URL url : urls) {
                            try {
                                JarInfo jarInfo = analyzer.process(url).orElse(null);
                                if (jarInfo != null) {
                                    jarInfos.add(jarInfo);
                                }
                            } catch (Exception e) {
                                log.info("Error processing JAR with URL: " + url, e);
                                // Skipping a JAR is not critical, so continue
                            }
                        }
                    }
                    generator.writeFieldName("jars");
                    generator.writeStartArray();
                    for (JarInfo jarInfo : jarInfos) {
                        generator.writeStartObject();
                        generator.writeStringField("name", jarInfo.name());
                        generator.writeStringField("version", jarInfo.version());
                        generator.writeObjectField("attributes", jarInfo.attributes());
                        generator.writeEndObject();
                    }
                    generator.writeEndArray();
                    generator.writeEndObject(); //context
                }
                generator.writeEndArray(); //contexts
                generator.writeEndObject(); //host
            }
            generator.writeEndArray();
            // Only process the first service; additional services are not reported
            // as the multi services configurations in Tomcat are exceedingly rare and
            // are not worth supporting here
            break;
        }
        generator.writeEndObject();
        generator.flush();
    }

}
