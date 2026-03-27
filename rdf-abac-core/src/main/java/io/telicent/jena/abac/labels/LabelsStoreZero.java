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

package io.telicent.jena.abac.labels;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.core.TransactionalNull;
import org.apache.jena.sparql.graph.GraphZero;

/** An unmodifiable store that stores nothing. */
public class LabelsStoreZero implements LabelsStore {

    private final Transactional transactional = TransactionalNull.create();

    /** No labels for this triple. */
    /*package*/ LabelsStoreZero() {}

    @Override
    public Transactional getTransactional() {
        return transactional;
    }

    @Override
    public Label labelForQuad(Quad quad) {
        if ( ! quad.isConcrete() ) {
            Log.error(Labels.class, "Asked for labels for a quad with wildcards: "+NodeFmtLib.displayStr(quad));
        }
        return null;
    }

    @Override
    public void add(Quad quad, Label label) {
        throw new UnsupportedOperationException("Can't add to LabelsStoreZero");
    }

    @Override
    public void remove(Quad quad) {
        throw new UnsupportedOperationException("Can't remove from LabelsStoreZero");
    }

    @Override
    public boolean isEmpty() { return true; }

    @Override
    public Graph asGraph() { return GraphZero.instance(); }

    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>();
    }

    @Override
    public void forEach(BiConsumer<Quad, Label> action) {}

    @Override
    public void close() throws Exception {}
}
