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

package com.networknt.swagger;

import com.networknt.config.Config;
import io.swagger.models.Swagger;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

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
    public static String oauth2Name;

    static {
        final SwaggerDeserializationResult swaggerParseResult = new SwaggerParser().readWithInfo(Config.getInstance().getStringFromFile(SWAGGER_CONFIG));

        swagger = swaggerParseResult.getSwagger();
        if (swagger == null) {
            logger.error("Unable to load swagger.json");
        } else {
            oauth2Name = getOAuth2Name();
        }
    }

    public static Optional<NormalisedPath> findMatchingApiPath(final NormalisedPath requestPath) {
        if(SwaggerHelper.swagger != null) {
            return SwaggerHelper.swagger.getPaths().keySet()
                    .stream()
                    .map(p -> (NormalisedPath) new ApiNormalisedPath(p))
                    .filter(p -> pathMatches(requestPath, p))
                    .findFirst();
        } else {
            return Optional.empty();
        }
    }

    private static String getOAuth2Name() {
        String name = null;
        Map<String, SecuritySchemeDefinition> defMap = swagger.getSecurityDefinitions();
        if(defMap != null) {
            for(Map.Entry<String, SecuritySchemeDefinition> entry : defMap.entrySet()) {
                if(entry.getValue().getType().equals("oauth2")) {
                    name = entry.getKey();
                    break;
                }
            }
        }
        return name;
    }

    private static boolean pathMatches(final NormalisedPath requestPath, final NormalisedPath apiPath) {
        if (requestPath.parts().size() != apiPath.parts().size()) {
            return false;
        }
        for (int i = 0; i < requestPath.parts().size(); i++) {
            if (requestPath.part(i).equalsIgnoreCase(apiPath.part(i)) || apiPath.isParam(i)) {
                continue;
            }
            return false;
        }
        return true;
    }

}
