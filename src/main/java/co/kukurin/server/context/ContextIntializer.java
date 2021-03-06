package co.kukurin.server.context;

import co.kukurin.server.logging.ServerLoggerImpl;
import co.kukurin.server.request.ResourceRequest;
import co.kukurin.server.response.ResourceResponse;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;

public class ContextIntializer {

    static final char PACKAGE_SEPARATOR = '.';

    private final ClassLoader classLoader;
    private final Map<ResourceRequest, ResourceResponse> resourceHandler;
    private final ServerLoggerImpl logger;

    public ContextIntializer(Class<?> applicationMainClass,
                             ServerLoggerImpl logger) throws IOException {
        this.classLoader = applicationMainClass.getClassLoader();
        this.logger = logger;

        String applicationMainPackage = extractPackageFromClassname(applicationMainClass);
        this.resourceHandler = searchForClassesInPackage(applicationMainPackage);
    }

    public Map<ResourceRequest, ResourceResponse> getResourceHandler() {
        return resourceHandler;
    }

    private String extractPackageFromClassname(Class<?> applicationMainClass) {
        String fqcn = applicationMainClass.getName();
        return fqcn.substring(0, fqcn.lastIndexOf(PACKAGE_SEPARATOR));
    }

    private Map<ResourceRequest, ResourceResponse> searchForClassesInPackage(String packageName) throws IOException {
        logger.info("Loading resources...");

        String path = packageName.replace(PACKAGE_SEPARATOR, File.separatorChar);
        Enumeration<URL> resources = classLoader.getResources(path);
        ResourceMapPopulatingVisitor resourceMapPopulatingVisitor = new ResourceMapPopulatingVisitor(packageName, logger);

        while (resources.hasMoreElements()) {
            try {
                Path resourcePath = Paths.get(resources.nextElement().toURI());
                Files.walkFileTree(resourcePath, resourceMapPopulatingVisitor);
            } catch (URISyntaxException shouldNeverOccur) {}
        }

        logger.info("Loaded:", resourceMapPopulatingVisitor.getResourceHandler());
        return resourceMapPopulatingVisitor.getResourceHandler();
    }
}
