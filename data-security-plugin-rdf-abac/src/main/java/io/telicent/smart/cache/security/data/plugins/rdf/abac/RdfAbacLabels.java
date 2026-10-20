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

import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.smart.cache.security.data.AbstractSecurityPrimitive;
import io.telicent.smart.cache.security.data.labels.SecurityLabels;
import org.apache.jena.atlas.io.IndentedWriter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RdfAbacLabels extends AbstractSecurityPrimitive implements SecurityLabels<List<AttributeExpr>> {

    private final List<AttributeExpr> expressions;

    RdfAbacLabels(byte[] encoded, List<AttributeExpr> expressions) {
        super(encoded);
        this.expressions = Objects.requireNonNull(expressions);
    }

    @Override
    public List<AttributeExpr> decodedLabels() {
        return this.expressions;
    }

    @Override
    public String toDebugString() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IndentedWriter writer = new IndentedWriter(output);
        for (int i = 0; i < this.expressions.size(); i++) {
            this.expressions.get(i).print(writer);
            if (i < this.expressions.size() - 1) {
                writer.write(" && ");
            }
        }
        writer.flush();
        return output.toString(StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof RdfAbacLabels labels) {
            return Arrays.equals(this.encoded(), labels.encoded());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.encoded());
    }

    @Override
    public String toString() {
        return this.getClass()
                   .getSimpleName() + "{ encodedSize=" + this.encoded().length + ", labels=" + this.toDebugString() + "}";
    }
}
