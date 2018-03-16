package com.xy.spring.boot.fix;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.servlet.Servlet;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * Created by xiaoyao9184 on 2018/3/14.
 */
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({ Servlet.class, Tomcat.class })
@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, search = SearchStrategy.CURRENT)
public class DevToolClassPathResourceJarsSupportAutoConfigure {

    @Bean
    public EmbeddedServletContainerFactory getEmbeddedServletContainerFactory() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        factory.addContextCustomizers(new TomcatContextCustomizer() {
            @Override
            public void customize(Context context) {
                context.addLifecycleListener(new LifecycleListener() {
                    @Override
                    public void lifecycleEvent(LifecycleEvent event) {
                        if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
                            new TomcatResourcesWrapper((Context) event.getLifecycle())
                                    .addResourceJars(getUrlsOfJarsWithMetaInfResources());
                        }
                    }
                });
            }
        });
        return factory;
    }


    private List<URL> getUrlsOfJarsWithMetaInfResources() {
        ClassLoader classLoader = getClass().getClassLoader();
        return Arrays.asList(getJarUrlsFromManifests(classLoader));
    }

    private URL[] getJarUrlsFromManifests(ClassLoader cl) {
        try {
            Set<URL> urlSet = new LinkedHashSet<>();

            URL url = cl.getResource("META-INF/MANIFEST.MF");

            if (url != null) {
                Manifest manifest = new Manifest(url.openStream());

                String classPath = manifest.getMainAttributes().getValue("Class-Path");

                if (classPath != null) {
                    for (String urlStr : classPath.split(" ")) {
                        try {
                            urlSet.add(new URL(urlStr));
                        } catch (MalformedURLException ex) {
                            throw new AssertionError();
                        }
                    }
                }
            }

            return urlSet.toArray(new URL[urlSet.size()]);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
