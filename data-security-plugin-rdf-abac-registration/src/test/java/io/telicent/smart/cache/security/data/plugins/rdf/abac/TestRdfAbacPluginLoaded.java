/**
 * Copyright (C) Telicent Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.telicent.smart.cache.security.data.plugins.rdf.abac;

import io.telicent.smart.cache.security.data.plugins.DataSecurityPlugin;
import io.telicent.smart.cache.security.data.plugins.DataSecurityPluginLoader;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRdfAbacPluginLoaded {

    @Test
    public void givenThisPlugin_whenLoadingPlugin_thenRdfAbacPluginLoaded_andLoadingAgainReturnsSameInstance() {
        // Given and When
        DataSecurityPlugin plugin = DataSecurityPluginLoader.load();

        // Then
        Assert.assertTrue(plugin instanceof RdfAbacPlugin);

        // And
        DataSecurityPlugin other = DataSecurityPluginLoader.load();
        Assert.assertSame(plugin, other);
    }
}
