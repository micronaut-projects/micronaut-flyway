/*
 * Copyright 2017-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.flyway;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.util.StringUtils;
import org.graalvm.nativeimage.ImageSingletons;

/**
 * Internal class for resolving migrations at build time with GraalVM.
 */
@Internal
final class StaticResourceProvider implements ResourceProvider {
    private static final String FLYWAY_LOCATIONS = "flyway.locations";
    private static final String DEFAULT_FLYWAY_LOCATIONS = "classpath:db/migration";
    private static final String CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL = "classpath";
    private static final String JAR_APPLICATION_MIGRATIONS_PROTOCOL = "jar";
    private static final String FILE_APPLICATION_MIGRATIONS_PROTOCOL = "file";
    private final List<StaticLoadableResource> resources;

    private StaticResourceProvider(List<StaticLoadableResource> resources) {
        this.resources = resources;
    }

    static StaticResourceProvider get() {
        return findStaticResourceProvider();
    }

    static void install(ClassLoader classLoader) {
        StaticResourceProvider staticResourceProvider = create(classLoader);
        if (hasImageSingletons()) {
            ImageSingletons.add(StaticResourceProvider.class, staticResourceProvider);
        }
    }

    static StaticResourceProvider create(ClassLoader classLoader) {

        List<String> locations = Stream
            .of(System.getProperty(FLYWAY_LOCATIONS, DEFAULT_FLYWAY_LOCATIONS).split(","))
            .toList();
        try {
            List<StaticLoadableResource> resources = discoverApplicationMigrations(locations, classLoader);
            return new StaticResourceProvider(resources);
        } catch (IOException | URISyntaxException e) {
            throw new ConfigurationException("Error loading Flyway migrations: " + e.getMessage(), e);
        }
    }

    @Override
    public LoadableResource getResource(String name) {
        for (StaticLoadableResource resource : resources) {
            if (resource.getAbsolutePath().equals(name)) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public Collection<LoadableResource> getResources(String prefix, String[] suffixes) {
        List<LoadableResource> result = new ArrayList<>();
        for (LoadableResource resource : resources) {
            String fileName = resource.getFilename();
            if (StringUtils.startsAndEndsWith(fileName, prefix, suffixes)) {
                result.add(resource);
            }
        }
        return result;
    }

    @Nullable
    private static StaticResourceProvider findStaticResourceProvider() {
        if (hasImageSingletons()) {
            return ImageSingletons.contains(StaticResourceProvider.class) ? ImageSingletons.lookup(StaticResourceProvider.class) : null;
        } else {
            return null;
        }
    }

    @SuppressWarnings("java:S1181")
    private static boolean hasImageSingletons() {
        try {
            //noinspection ConstantValue
            return ImageSingletons.class != null;
        } catch (Throwable e) {
            // not present or not a GraalVM JDK
            return false;
        }
    }

    private static List<StaticLoadableResource> discoverApplicationMigrations(List<String> locations, ClassLoader classLoader) throws IOException, URISyntaxException {
        List<StaticLoadableResource> applicationMigrationResources = new ArrayList<>();
        // Locations can be a comma separated list
        for (String location : locations) {
            // Strip any 'classpath:' protocol prefixes because they are assumed
            // but not recognized by ClassLoader.getResources()
            if (location != null && location.startsWith(CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL + ':')) {
                location = location.substring(CLASSPATH_APPLICATION_MIGRATIONS_PROTOCOL.length() + 1);
            }
            Enumeration<URL> migrations = classLoader.getResources(location);
            while (migrations.hasMoreElements()) {
                URL path = migrations.nextElement();
                final Set<StaticLoadableResource> applicationMigrations;
                if (JAR_APPLICATION_MIGRATIONS_PROTOCOL.equals(path.getProtocol())) {
                    try (FileSystem fileSystem = initFileSystem(path.toURI())) {
                        applicationMigrations = getApplicationMigrationsFromPath(location, path);
                    }
                } else if (FILE_APPLICATION_MIGRATIONS_PROTOCOL.equals(path.getProtocol())) {
                    applicationMigrations = getApplicationMigrationsFromPath(location, path);
                } else {
                    applicationMigrations = null;
                }
                if (applicationMigrations != null) {
                    applicationMigrationResources.addAll(applicationMigrations);
                }
            }
        }
        return applicationMigrationResources;
    }

    private static Set<StaticLoadableResource> getApplicationMigrationsFromPath(final String location, final URL path)
        throws IOException, URISyntaxException {
        try (Stream<Path> pathStream = Files.walk(Paths.get(path.toURI()))) {
            return pathStream.filter(Files::isRegularFile)
                // we don't want windows paths here since the paths are going to be used as classpath paths anyway
                .map(it -> new StaticLoadableResource(location, it))
                .collect(Collectors.toSet());
        }
    }

    private static FileSystem initFileSystem(final URI uri) throws IOException {
        return FileSystems.newFileSystem(uri, Collections.singletonMap("create", "true"));
    }

    static final class StaticLoadableResource extends LoadableResource {

        private final String location;
        private final String absolutePath;
        private final String fileName;
        private final byte[] bytes;

        StaticLoadableResource(String location, Path it) {
            byte[] readBytes;
            this.location = location;
            this.absolutePath = it.toAbsolutePath().toString();
            this.fileName = it.getFileName().toString();
            try {
                readBytes = Files.readAllBytes(it);
            } catch (IOException e) {
                readBytes = new byte[0];
            }
            this.bytes = readBytes;
        }

        @Override
        public Reader read() {
            return new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
        }

        @Override
        public String getAbsolutePath() {
            return absolutePath;
        }

        @Override
        public String getAbsolutePathOnDisk() {
            return absolutePath;
        }

        @Override
        public String getFilename() {
            return fileName;
        }

        @Override
        public String getRelativePath() {
            return location + File.pathSeparator + fileName;
        }

        @Override
        public String toString() {
            return "StaticLoadableResource{" +
                "location='" + location + '\'' +
                ", absolutePath='" + absolutePath + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
        }
    }
}
