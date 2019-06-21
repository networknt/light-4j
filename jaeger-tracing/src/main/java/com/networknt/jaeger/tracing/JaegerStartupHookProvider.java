/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.jaeger.tracing;

import com.networknt.server.ServerConfig;
import com.networknt.server.StartupHookProvider;
import com.networknt.config.Config;
import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;

public class JaegerStartupHookProvider implements StartupHookProvider {

    public static JaegerTracer tracer;
    static JaegerConfig jaegerConfig = (JaegerConfig) Config.getInstance().getJsonObjectConfig(JaegerConfig.CONFIG_NAME, JaegerConfig.class);
    static ServerConfig serverConfig = (ServerConfig) Config.getInstance().getJsonObjectConfig(ServerConfig.CONFIG_NAME, ServerConfig.class);

    @Override
    public void onStartup() {
        // register the service instance to the Jaeger tracer agent and create a tracer object if it is enabled.
        if(jaegerConfig.isEnabled()) {
            Configuration.SamplerConfiguration samplerConfig = new Configuration.SamplerConfiguration().withType(jaegerConfig.getType()).withParam(jaegerConfig.getParam());
            Configuration.ReporterConfiguration reporterConfig = new Configuration.ReporterConfiguration().withLogSpans(true);
            tracer = new Configuration(serverConfig.getServiceId()).withSampler(samplerConfig).withReporter(reporterConfig).getTracer();
        }
    }
}
