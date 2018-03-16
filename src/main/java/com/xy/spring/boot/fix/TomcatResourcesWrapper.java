package com.xy.spring.boot.fix;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;

import java.net.URL;
import java.util.List;

/**
 * Created by xiaoyao9184 on 2018/3/14.
 */
public class TomcatResourcesWrapper {

    private Context context;

    public TomcatResourcesWrapper(Context context){
        this.context = context;
    }

    public void addResourceJars(List<URL> resourceJarUrls) {
        for (URL url : resourceJarUrls) {
            String file = url.getFile();
            if (file.endsWith(".jar") || file.endsWith(".jar!/")) {
                String jar = url.toString();
                if (!jar.startsWith("jar:")) {
                    // A jar file in the file system. Convert to Jar URL.
                    jar = "jar:" + jar + "!/";
                }
                addJar(jar);
            }
            else {
                addDir(file, url);
            }
        }
    }

    protected void addJar(String jar) {
        addResourceSet(jar);
    }

    protected void addDir(String dir, URL url) {
        addResourceSet(url.toString());
    }

    private void addResourceSet(String resource) {
        try {
            if (isInsideNestedJar(resource)) {
                // It's a nested jar but we now don't want the suffix because Tomcat
                // is going to try and locate it as a root URL (not the resource
                // inside it)
                resource = resource.substring(0, resource.length() - 2);
            }
            URL url = new URL(resource);
            String path = "/META-INF/resources";
            context.getResources().createWebResourceSet(
                    WebResourceRoot.ResourceSetType.RESOURCE_JAR, "/", url, path);
        }
        catch (Exception ex) {
            // Ignore (probably not a directory)
        }
    }

    private boolean isInsideNestedJar(String dir) {
        return dir.indexOf("!/") < dir.lastIndexOf("!/");
    }


}
