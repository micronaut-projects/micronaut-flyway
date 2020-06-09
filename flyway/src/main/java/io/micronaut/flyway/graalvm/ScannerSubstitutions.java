/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.flyway.graalvm;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.internal.resource.LoadableResource;
import org.flywaydb.core.internal.scanner.LocationScannerCache;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.flywaydb.core.internal.scanner.classpath.ResourceAndClassScanner;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This substitution replaces the Flyway dynamic scanners with a fixed path scanner in native mode.
 *
 * Forked from the Quarkus implementation.
 */
@TargetClass(className = "org.flywaydb.core.internal.scanner.Scanner")
public final class ScannerSubstitutions {

    @Alias
    private List<LoadableResource> resources = new ArrayList<>();

    @Alias
    private List<Class<?>> classes = new ArrayList<>();

    /**
     * Creates only {@link MicronautPathLocationScanner} instances.
     * Replaces the original method that tries to detect migrations using reflection techniques that are not allowed
     * in native mode.
     *
     * @see org.flywaydb.core.internal.scanner.Scanner#Scanner(Class, Collection, ClassLoader, Charset, ResourceNameCache,
     * LocationScannerCache)
     */
    @SuppressWarnings("checkstyle:javadocmethod")
    @Substitute
    public ScannerSubstitutions(Class<?> implementedInterface, Collection<Location> locations, ClassLoader classLoader,
                                Charset encoding, ResourceNameCache resourceNameCache, LocationScannerCache locationScannerCache) {
        ResourceAndClassScanner scanner = new MicronautPathLocationScanner(locations);

        Collection resources = scanner.scanForResources();
        this.resources.addAll(resources);

        Collection scanForClasses = scanner.scanForClasses();
        classes.addAll(scanForClasses);
    }

}
