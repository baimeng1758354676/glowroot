/**
 * Copyright 2013 the original author or authors.
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
package io.informant.test;

import java.io.File;

import checkers.nullness.quals.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.informant.Containers;
import io.informant.container.AppUnderTest;
import io.informant.container.Container;
import io.informant.container.TempDirs;
import io.informant.container.TraceMarker;
import io.informant.container.config.PointcutConfig;
import io.informant.container.config.PointcutConfig.CaptureItem;
import io.informant.container.config.PointcutConfig.MethodModifier;
import io.informant.container.trace.Trace;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
public class DynamicAdviceTest {

    protected static Container container;
    private static File dataDir;

    @BeforeClass
    public static void setUp() throws Exception {
        dataDir = TempDirs.createTempDir("informant-test-datadir");
        container = Containers.createWithFileDb(dataDir);
        addPointcutForExecute1();
        addPointcutForExecute1MetricOnly();
        addPointcutForExecuteWithReturn();
        addPointcutForExecuteWithArgs();
        // re-start now with dynamic pointcuts
        container.close();
        container = Containers.createWithFileDb(dataDir);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
        TempDirs.deleteRecursively(dataDir);
    }

    @After
    public void afterEachTest() throws Exception {
        container.checkAndReset();
    }

    @Test
    public void shouldExecute1() throws Exception {
        // given
        container.getConfigService().setStoreThresholdMillis(0);
        // when
        container.executeAppUnderTest(ShouldExecute1.class);
        // then
        Trace trace = container.getTraceService().getLastTrace();
        assertThat(trace.getSpans()).hasSize(2);
        assertThat(trace.getMetricNames()).hasSize(3);
        assertThat(trace.getMetricNames()).contains("mock trace marker", "execute one");
        assertThat(trace.getMetricNames()).contains("mock trace marker", "execute one metric only");
        assertThat(trace.getSpans().get(1).getMessage().getText()).isEqualTo("execute1() => void");
    }

    @Test
    public void shouldExecuteWithReturn() throws Exception {
        // given
        container.getConfigService().setStoreThresholdMillis(0);
        // when
        container.executeAppUnderTest(ShouldExecuteWithReturn.class);
        // then
        Trace trace = container.getTraceService().getLastTrace();
        assertThat(trace.getSpans()).hasSize(2);
        assertThat(trace.getMetricNames()).hasSize(2);
        assertThat(trace.getMetricNames()).contains("mock trace marker", "execute with return");
        assertThat(trace.getSpans().get(1).getMessage().getText())
                .isEqualTo("executeWithReturn() => xyz");
    }

    @Test
    public void shouldExecuteWithArgs() throws Exception {
        // given
        container.getConfigService().setStoreThresholdMillis(0);
        // when
        container.executeAppUnderTest(ShouldExecuteWithArgs.class);
        // then
        Trace trace = container.getTraceService().getLastTrace();
        assertThat(trace.getSpans()).hasSize(2);
        assertThat(trace.getMetricNames()).hasSize(2);
        assertThat(trace.getMetricNames()).contains("mock trace marker", "execute with args");
        assertThat(trace.getSpans().get(1).getMessage().getText())
                .isEqualTo("executeWithArgs(): abc, 123");
    }

    protected static void addPointcutForExecute1() throws Exception {
        PointcutConfig config = new PointcutConfig();
        config.setCaptureItems(Lists.newArrayList(CaptureItem.METRIC, CaptureItem.SPAN));
        config.setTypeName("io.informant.test.DynamicAdviceTest$Misc");
        config.setMethodName("execute1");
        config.setMethodArgTypeNames(ImmutableList.<String>of());
        config.setMethodReturnTypeName("");
        config.setMethodModifiers(Lists.newArrayList(MethodModifier.PUBLIC));
        config.setMetricName("execute one");
        config.setSpanTemplate("execute1() => {{ret}}");
        container.getConfigService().addPointcutConfig(config);
    }

    protected static void addPointcutForExecute1MetricOnly() throws Exception {
        PointcutConfig config = new PointcutConfig();
        config.setCaptureItems(Lists.newArrayList(CaptureItem.METRIC));
        config.setTypeName("io.informant.test.DynamicAdviceTest$Misc");
        config.setMethodName("execute1");
        config.setMethodArgTypeNames(ImmutableList.<String>of());
        config.setMethodReturnTypeName("");
        config.setMethodModifiers(Lists.newArrayList(MethodModifier.PUBLIC));
        config.setMetricName("execute one metric only");
        container.getConfigService().addPointcutConfig(config);
    }

    protected static void addPointcutForExecuteWithReturn() throws Exception {
        PointcutConfig config = new PointcutConfig();
        config.setCaptureItems(Lists.newArrayList(CaptureItem.METRIC, CaptureItem.SPAN));
        config.setTypeName("io.informant.test.DynamicAdviceTest$Misc");
        config.setMethodName("executeWithReturn");
        config.setMethodArgTypeNames(ImmutableList.<String>of());
        config.setMethodReturnTypeName("");
        config.setMethodModifiers(Lists.newArrayList(MethodModifier.PUBLIC));
        config.setMetricName("execute with return");
        config.setSpanTemplate("executeWithReturn() => {{ret}}");
        container.getConfigService().addPointcutConfig(config);
    }

    protected static void addPointcutForExecuteWithArgs() throws Exception {
        PointcutConfig config = new PointcutConfig();
        config.setCaptureItems(Lists.newArrayList(CaptureItem.METRIC, CaptureItem.SPAN));
        config.setTypeName("io.informant.test.DynamicAdviceTest$Misc");
        config.setMethodName("executeWithArgs");
        config.setMethodArgTypeNames(ImmutableList.of("java.lang.String", "int"));
        config.setMethodReturnTypeName("void");
        config.setMethodModifiers(Lists.newArrayList(MethodModifier.PUBLIC));
        config.setMetricName("execute with args");
        config.setSpanTemplate("executeWithArgs(): {{0}}, {{1}}");
        container.getConfigService().addPointcutConfig(config);
    }

    public interface Misc {
        public void execute1();
        @Nullable
        public CharSequence executeWithReturn();
        public void executeWithArgs(String one, int two);
    }

    public static class BasicMisc implements Misc {
        public void execute1() {}
        public String executeWithReturn() {
            return "xyz";
        }
        public void executeWithArgs(String one, int two) {}
    }

    public static class ShouldExecute1 implements AppUnderTest, TraceMarker {
        public void executeApp() {
            traceMarker();
        }
        public void traceMarker() {
            new BasicMisc().execute1();
        }
    }

    public static class ShouldExecuteWithReturn implements AppUnderTest, TraceMarker {
        public void executeApp() {
            traceMarker();
        }
        public void traceMarker() {
            new BasicMisc().executeWithReturn();
        }
    }

    public static class ShouldExecuteWithArgs implements AppUnderTest, TraceMarker {
        public void executeApp() {
            traceMarker();
        }
        public void traceMarker() {
            new BasicMisc().executeWithArgs("abc", 123);
        }
    }
}
