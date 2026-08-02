package com.ces.service.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded inventory images back out as plain static files. */
@Configuration
public class UploadStaticResourceConfig implements WebMvcConfigurer {

    private final String uploadsDir;
    private final String urlPrefix;

    public UploadStaticResourceConfig(
            @Value("${ces.uploads.dir}") String uploadsDir,
            @Value("${ces.uploads.url-prefix}") String urlPrefix) {
        this.uploadsDir = uploadsDir;
        this.urlPrefix = urlPrefix;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path absoluteDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        String location = "file:" + absoluteDir + File.separator;
        registry.addResourceHandler(urlPrefix + "/**").addResourceLocations(location);
    }
}
