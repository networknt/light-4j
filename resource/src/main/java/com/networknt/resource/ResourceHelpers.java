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

package com.networknt.resource;

import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.resource.ResourceHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nicholas Azar
 * Created on April 21, 2018
 */
public class ResourceHelpers {

    /**
     * Helper to add given PathResourceProviders to a PathHandler.
     *
     * @param pathResourceProviders List of instances of classes implementing PathResourceProvider.
     * @param pathHandler The handler that will have these handlers added to it.
     */
    public static void addProvidersToPathHandler(PathResourceProvider[] pathResourceProviders, PathHandler pathHandler) {
        if (pathResourceProviders != null && pathResourceProviders.length > 0) {
            for (PathResourceProvider pathResourceProvider : pathResourceProviders) {
                if (pathResourceProvider.isPrefixPath()) {
                    pathHandler.addPrefixPath(pathResourceProvider.getPath(), new ResourceHandler(pathResourceProvider.getResourceManager()));
                } else {
                    pathHandler.addExactPath(pathResourceProvider.getPath(), new ResourceHandler(pathResourceProvider.getResourceManager()));
                }
            }
        }
    }

    /**
     * Helper for retrieving all PredicatedHandlers from the given list of PredicatedHandlersProviders.
     *
     * @param predicatedHandlersProviders The list of PredicatedHandlersProviders that will be checked for retrieval of PredicatedHandlers.
     * @return The list of PredicatedHandlers
     */
    public static List<PredicatedHandler> getPredicatedHandlers(PredicatedHandlersProvider[] predicatedHandlersProviders) {
        List<PredicatedHandler> predicatedHandlers = new ArrayList<>();
        if (predicatedHandlersProviders != null && predicatedHandlersProviders.length > 0) {
            for (PredicatedHandlersProvider predicatedHandlersProvider : predicatedHandlersProviders) {
                predicatedHandlers.addAll(predicatedHandlersProvider.getPredicatedHandlers());
            }
        }
        return predicatedHandlers;
    }

    /**
     * Helper to check if a given requestPath could resolve to a PathResourceProvider.
     * @param requestPath The client request path.
     * @param pathResourceProviders The list of PathResourceProviders that could potentially resolve this path.
     * @return true if the path could resolve, false otherwise.
     */
    public static boolean isResourcePath(String requestPath, PathResourceProvider[] pathResourceProviders) {
        boolean isResourcePath = false;
        if (pathResourceProviders != null && pathResourceProviders.length > 0) {
            for (PathResourceProvider pathResourceProvider : pathResourceProviders) {
                if ((pathResourceProvider.isPrefixPath() && requestPath.startsWith(pathResourceProvider.getPath()))
                        || (!pathResourceProvider.isPrefixPath() && requestPath.equals(pathResourceProvider.getPath()))) {
                    isResourcePath = true;
                }
            }
        }
        return isResourcePath;
    }
}
