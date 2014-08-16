/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.tests;

import java.io.IOException;
import java.util.List;

import com.google.common.base.StandardSystemProperty;
import com.google.common.io.Resources;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.container.AppUnderTest;
import org.glowroot.container.Container;
import org.glowroot.container.javaagent.JavaagentContainer;
import org.glowroot.weaving.ClassNames;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
public class ClassLoaderLeakTest {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        // need memory limited javaagent
        List<String> extraJvmArgs = Lists.newArrayList();
        String javaVersion = StandardSystemProperty.JAVA_VERSION.value();
        if (javaVersion.startsWith("1.6") || javaVersion.startsWith("1.7")) {
            // limit MaxPermSize for ClassLoaderLeakTest
            extraJvmArgs.add("-XX:MaxPermSize=64m");
        } else {
            // jdk8+ eliminated perm gen, so just limit overall memory
            extraJvmArgs.add("-Xmx64m");
        }
        container = new JavaagentContainer(null, false, true, false, extraJvmArgs);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
    }

    @After
    public void afterEachTest() throws Exception {
        container.checkAndReset();
    }

    @Test
    public void shouldNotRunOutOfPermGenSpace() throws Exception {
        // given
        // when
        container.executeAppUnderTest(ShouldGenerateLotsOfClassLoaders.class);
        // then
    }

    public static class ShouldGenerateLotsOfClassLoaders implements AppUnderTest {
        @Override
        public void executeApp() throws IOException {
            // javaagent container limits MaxPermSize to 64m (and limits normal memory to 64m in
            // jdk8+ since no more perm gen)
            // if these class loaders are not collected, they will fill up 64m around 36,000
            // iterations
            for (int i = 0; i < 50000; i++) {
                new TempClassLoader().defineTempClass();
            }
        }
    }

    private static class TempClassLoader extends ClassLoader {
        protected Class<?> defineTempClass() throws IOException {
            String resourceName = ClassNames.toInternalName(TempClass.class.getName()) + ".class";
            byte[] bytes = Resources.toByteArray(Resources.getResource(resourceName));
            return defineClass(TempClass.class.getName(), bytes, 0, bytes.length, null);
        }
    }

    private static class TempClass {}
}
