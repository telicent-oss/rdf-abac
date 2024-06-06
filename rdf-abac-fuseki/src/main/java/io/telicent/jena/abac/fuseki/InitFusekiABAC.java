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

package io.telicent.jena.abac.fuseki;

import io.telicent.jena.abac.core.InitABAC;
import org.apache.jena.sys.JenaSubsystemLifecycle;
import org.apache.jena.sys.JenaSystem;

public class InitFusekiABAC implements JenaSubsystemLifecycle {

    public static final int LEVEL = InitABAC.LEVEL+1;

    @Override
    public void start() {
        JenaSystem.logLifecycle("Fuseki ABAC - start") ;
        SysFusekiABAC.init();
        JenaSystem.logLifecycle("Fuseki ABAC - finish") ;
    }

    @Override
    public void stop() {}

    @Override
    public int level() {
        return LEVEL;
    }

}
