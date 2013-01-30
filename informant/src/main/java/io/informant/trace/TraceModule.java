/**
 * Copyright 2011-2013 the original author or authors.
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
package io.informant.trace;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.informant.api.PluginServices;
import io.informant.common.Clock;
import io.informant.config.AdviceCache;
import io.informant.config.ConfigModule;
import io.informant.config.ConfigService;
import io.informant.markers.OnlyUsedByTests;
import io.informant.markers.ThreadSafe;
import io.informant.trace.model.WeavingMetricNameImpl;
import io.informant.weaving.ParsedTypeCache;
import io.informant.weaving.WeavingClassFileTransformer;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
@ThreadSafe
public class TraceModule {

    private final Ticker ticker;
    private final Clock clock;

    private final ConfigModule configModule;
    private final TraceSink traceSink;
    private final ParsedTypeCache parsedTypeCache;
    private final WeavingMetricNameImpl weavingMetricName;
    private final TraceRegistry traceRegistry;
    private final MetricCache metricCache;
    private final AdviceCache adviceCache;

    private final StuckTraceCollector stuckTraceCollector;
    private final CoarseProfiler coarseProfiler;
    private final FineProfileScheduler fineProfileScheduler;

    private final LoadingCache<String, PluginServices> pluginServices =
            CacheBuilder.newBuilder().build(new CacheLoader<String, PluginServices>() {
                @Override
                public PluginServices load(String pluginId) {
                    return new PluginServicesImpl(traceRegistry, traceSink,
                            configModule.getConfigService(), metricCache, fineProfileScheduler,
                            ticker, clock, weavingMetricName,
                            configModule.getPluginDescriptorCache(), pluginId);
                }
            });

    public TraceModule(Ticker ticker, Clock clock, ConfigModule configModule, TraceSink traceSink,
            ScheduledExecutorService scheduledExecutor) throws Exception {
        this.ticker = ticker;
        this.clock = clock;
        this.configModule = configModule;
        this.traceSink = traceSink;
        ConfigService configService = configModule.getConfigService();
        parsedTypeCache = new ParsedTypeCache();
        weavingMetricName = new WeavingMetricNameImpl(ticker);
        traceRegistry = new TraceRegistry();
        metricCache = new MetricCache(ticker);
        adviceCache = new AdviceCache(configModule.getPluginDescriptorCache(),
                configService.getPointcutConfigs());
        fineProfileScheduler = new FineProfileScheduler(scheduledExecutor, configService, ticker,
                new Random());
        stuckTraceCollector = new StuckTraceCollector(scheduledExecutor, traceRegistry, traceSink,
                configService, ticker);
        coarseProfiler = new CoarseProfiler(scheduledExecutor, traceRegistry, configService,
                ticker);
        stuckTraceCollector.start();
        coarseProfiler.start();
    }

    public WeavingClassFileTransformer createWeavingClassFileTransformer() {
        return new WeavingClassFileTransformer(adviceCache.getMixinTypes(),
                adviceCache.getAdvisorsSupplier(), parsedTypeCache, weavingMetricName);
    }

    public PluginServices getPluginServices(String pluginId) {
        return pluginServices.getUnchecked(pluginId);
    }

    public ParsedTypeCache getParsedTypeCache() {
        return parsedTypeCache;
    }

    public TraceRegistry getTraceRegistry() {
        return traceRegistry;
    }

    public AdviceCache getAdviceCache() {
        return adviceCache;
    }

    @OnlyUsedByTests
    public WeavingMetricNameImpl getWeavingMetricName() {
        return weavingMetricName;
    }

    @OnlyUsedByTests
    public void close() {
        stuckTraceCollector.close();
        coarseProfiler.close();
    }
}
