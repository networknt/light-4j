/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package com.networknt.security;

import com.networknt.config.Config;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * This class load and cache swagger.json in a static block so that it can be
 * shared by security for scope validation and validator for schema validation
 *
 * Created by steve on 17/09/16.
 */
public class SwaggerHelper {

    static final String SWAGGER_CONFIG = "swagger.json";
    static final Logger logger = LoggerFactory.getLogger(SwaggerHelper.class);

    public static Swagger swagger;

    static {
        final SwaggerDeserializationResult swaggerParseResult = new SwaggerParser().readWithInfo(Config.getInstance().getStringFromFile(SWAGGER_CONFIG));

        swagger = swaggerParseResult.getSwagger();
        if (swagger == null) {
            logger.error("Unable to load swagger.json");
        }
    }
}
