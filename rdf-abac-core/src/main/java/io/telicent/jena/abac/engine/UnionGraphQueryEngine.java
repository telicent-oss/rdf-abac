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

import io.telicent.jena.abac.core.DatasetGraphABAC;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import org.apache.jena.sparql.engine.Plan;
import org.apache.jena.sparql.engine.QueryEngineFactory;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.main.QueryEngineMain;
import org.apache.jena.sparql.mgt.Explain;
import org.apache.jena.sparql.util.Context;

import java.util.function.BooleanSupplier;

/**
 * A query engine for {@link DatasetGraphABAC} datasets that routes default graph queries to the union of all named
 * graphs.
 * <p>
 * Activated by the {@value #ENV_ROUTE_TO_NAMED_GRAPHS} environment variable. When the variable is absent or
 * {@code false}, the factory will not accept queries and Jena's standard {@code QueryEngineMain} handles them instead.
 * When {@code true}, default-graph queries are resolved against the union of all named graphs.
 */
public class UnionGraphQueryEngine extends QueryEngineMain {

    /**
     * Environment variable that enables union-graph routing.
     */
    static final String ENV_ROUTE_TO_NAMED_GRAPHS = "ROUTE_TO_NAMED_GRAPHS";

    /**
     * Reads {@value #ENV_ROUTE_TO_NAMED_GRAPHS} from the environment. Can be overridden in tests.
     */
    static BooleanSupplier routingCheck =
            () -> Boolean.parseBoolean(System.getenv(ENV_ROUTE_TO_NAMED_GRAPHS));

    public UnionGraphQueryEngine(Query query, DatasetGraph dsg, Binding input, Context context) {
        super(query, dsg, input, context);
    }

    public UnionGraphQueryEngine(Op op, DatasetGraph dsg, Binding input, Context context) {
        super(op, dsg, input, context);
    }

    /**
     * Converts the optimised algebra to quad form and applies {@link OpLib#unionDefaultGraphQuads} so that default
     * graph patterns are resolved against the union of all named graphs.
     */
    @Override
    protected Op modifyOp(Op op) {
        op = super.modifyOp(op);
        op = OpLib.unionDefaultGraphQuads(Algebra.toQuadForm(op));
        Explain.explain("REWRITE(Union default graph)", op, context);
        return op;
    }

    // ---- Factory

    private static final QueryEngineFactory factory = new UnionGraphQueryEngineFactory();

    public static QueryEngineFactory getFactory() {
        return factory;
    }

    /**
     * Register with the global {@link QueryEngineRegistry}.
     */
    public static void register() {
        if (!QueryEngineRegistry.containsFactory(factory))
            QueryEngineRegistry.addFactory(factory);
    }

    /**
     * Remove from the global {@link QueryEngineRegistry}.
     */
    public static void unregister() {
        QueryEngineRegistry.removeFactory(factory);
    }

    private static class UnionGraphQueryEngineFactory implements QueryEngineFactory {

        @Override
        public boolean accept(Query query, DatasetGraph dsg, Context context) {
            return routingCheck.getAsBoolean() && dsg instanceof DatasetGraphFilteredView;
        }

        @Override
        public Plan create(Query query, DatasetGraph dsg, Binding input, Context context) {
            return new UnionGraphQueryEngine(query, dsg, input, context).getPlan();
        }

        @Override
        public boolean accept(Op op, DatasetGraph dsg, Context context) {
            return routingCheck.getAsBoolean() && dsg instanceof DatasetGraphABAC;
        }

        @Override
        public Plan create(Op op, DatasetGraph dsg, Binding input, Context context) {
            return new UnionGraphQueryEngine(op, dsg, input, context).getPlan();
        }
    }

}
