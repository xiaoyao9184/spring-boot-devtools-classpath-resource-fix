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
import org.springframework.util.StringUtils;

import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
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

    private static URL[] getJarUrlsFromManifests(ClassLoader cl) {
        List<URL> list;
        if(cl instanceof URLClassLoader){
            URLClassLoader urlClassLoader = (URLClassLoader) cl;
            list = getUrlsFromClassPathOfJarManifestIfPossibleUseURLClassLoader(urlClassLoader);
        }else{
            list = getUrlsFromClassPathOfJarManifestIfPossibleUseClassLoaderGetResources(cl);
        }
        return list.toArray(new URL[]{});
    }


    private static List<URL> getUrlsFromClassPathOfJarManifestIfPossibleUseClassLoaderGetResources(ClassLoader classLoader){
        try {
            List<URL> urlSet = new LinkedList<>();

            Enumeration<URL> enumeration = classLoader.getResources("META-INF/MANIFEST.MF");
            while (enumeration.hasMoreElements()){
                URL url = enumeration.nextElement();
                if (url != null) {
                    Manifest manifest = new Manifest(url.openStream());
                    urlSet.addAll(getUrlsFromClassPathAttribute(url,manifest));
                }
            }
            return urlSet;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static List<URL> getUrlsFromClassPathOfJarManifestIfPossibleUseURLClassLoader(URLClassLoader classLoader){
        List<URL> result = new ArrayList<>();
        List<URL> jarList = Arrays.asList(classLoader.getURLs());
        for (URL url : jarList) {
            result.addAll(getUrlsFromClassPathOfJarManifestIfPossible(url));
        }
        return result;
    }

    private static List<URL> getUrlsFromClassPathOfJarManifestIfPossible(URL url) {
        JarFile jarFile = getJarFileIfPossible(url);
        if (jarFile == null) {
            return Collections.<URL>emptyList();
        }
        try {
            return getUrlsFromClassPathAttribute(url, jarFile.getManifest());
        }
        catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to read Class-Path attribute from manifest of jar " + url,
                    ex);
        }
    }

    private static JarFile getJarFileIfPossible(URL url) {
        try {
            File file = new File(url.toURI());
            if (file.isFile()) {
                return new JarFile(file);
            }
        }
        catch (Exception ex) {
            // Assume it's not a jar and continue
        }
        return null;
    }

    private static List<URL> getUrlsFromClassPathAttribute(URL base, Manifest manifest) {
        if (manifest == null) {
            return Collections.<URL>emptyList();
        }
        String classPath = manifest.getMainAttributes()
                .getValue(Attributes.Name.CLASS_PATH);
        if (!StringUtils.hasText(classPath)) {
            return Collections.emptyList();
        }
        String[] entries = StringUtils.delimitedListToStringArray(classPath, " ");
        List<URL> urls = new ArrayList<URL>(entries.length);
        for (String entry : entries) {
            try {
                urls.add(new URL(base, entry));
            }
            catch (MalformedURLException ex) {
                throw new IllegalStateException(
                        "Class-Path attribute contains malformed URL", ex);
            }
        }
        return urls;
    }
}
