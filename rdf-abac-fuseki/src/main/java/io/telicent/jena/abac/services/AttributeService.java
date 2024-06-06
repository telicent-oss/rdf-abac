/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.telicent.jena.abac.services;

/**
 * Constants for the attribute store service.
 */
public class AttributeService {
    // User lookup
    public static final String lookupUserAttributeTemplate = "/users/lookup/{user}";
    public static final String lookupUserAttributePath = LibAuthService.templateToPathName(lookupUserAttributeTemplate);
    public static final String lookupUserAttributeServletPathSpec = lookupUserAttributePath+"*";

    // Hierarchy lookup
    public static final String lookupHierarchyTemplate = "/hierarchies/lookup/{name}";
    public static final String lookupHierarchyPath = LibAuthService.templateToPathName(lookupHierarchyTemplate);
    public static final String lookupHierarchyServletPathSpec = lookupHierarchyPath+"*";
}