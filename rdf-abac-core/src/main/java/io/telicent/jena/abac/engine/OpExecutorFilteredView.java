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

package io.telicent.jena.abac.engine;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpDatasetNames;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.engine.iterator.QueryIterRepeatApply;
import org.apache.jena.sparql.engine.main.OpExecutor;
import org.apache.jena.sparql.engine.main.OpExecutorFactory;

import java.util.Iterator;

/**
 * The {@link OpExecutor} used by {@link UnionGraphQueryEngine}.
 * <p>
 * The engine runs over a filtered view of the dataset, but the view shares the wrapped dataset's context, so without
 * this a TDB2 dataset's own executor would be used.  That executor casts the dataset to {@code DatasetGraphTDB} for
 * {@code GRAPH ?g { }} ({@code OpDatasetNames}), which fails over a view, and over a view it otherwise defers to the
 * general executor anyway.  The general executor doesn't implement {@code OpDatasetNames} at all, so this class does.
 * </p>
 * <p>
 * A graph is only reported if the user can see at least one triple in it.  The view's {@code listGraphNodes()} isn't
 * enough on its own, because it lists the candidate graphs without applying the security filter, so it would reveal
 * the names of graphs whose data the user can't see.  This gives the same graphs as {@code GRAPH ?g { ?s ?p ?o }}.
 * </p>
 */
class OpExecutorFilteredView extends OpExecutor {

    static final OpExecutorFactory FACTORY = OpExecutorFilteredView::new;

    OpExecutorFilteredView(ExecutionContext execCxt) {
        super(execCxt);
    }

    @Override
    protected QueryIterator execute(OpDatasetNames dsNames, QueryIterator input) {
        return new QueryIterRepeatApply(input, execCxt) {
            @Override
            protected QueryIterator nextStage(Binding binding) {
                return QueryIterPlainWrapper.create(graphNames(dsNames.getGraphNode(), binding), getExecContext());
            }
        };
    }

    private Iterator<Binding> graphNames(Node graphNode, Binding binding) {
        DatasetGraph dsg = execCxt.getDataset();
        Node gn = Var.lookup(binding, graphNode);
        if (Var.isVar(gn)) {
            Iterator<Node> graphs = Iter.filter(dsg.listGraphNodes(), g -> isVisible(dsg, g));
            return Iter.map(graphs, g -> BindingFactory.binding(binding, Var.alloc(gn), g));
        }
        boolean found = isVisible(dsg, gn) && Iter.anyMatch(dsg.listGraphNodes(), gn::equals);
        return found ? Iter.singletonIterator(binding) : Iter.nullIterator();
    }

    private static boolean isVisible(DatasetGraph dsg, Node g) {
        if (Quad.isDefaultGraph(g) || Quad.isUnionGraph(g)) {
            return false;
        }
        // The view's contains() applies the security filter, so this is true only if the user can see some triple
        return dsg.contains(g, Node.ANY, Node.ANY, Node.ANY);
    }
}