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

package io.telicent.jena.abac;

import java.util.*;

import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.attributes.syntax.tokens.Words;
import io.telicent.jena.abac.core.HierarchyGetter;

/**
 * A hierarchy is a controlled set of values for an attribute where a user request
 * with one attribute value implies other values for the same attribute.
 * <p>
 * Hierarchies are written low to high. For example, suppose there is an attribute
 * 'clearance' which has the hierarchy: "ordinary" &lt; "secret" &lt; "top secret".
 * <p>
 * A user request with attribute value 'clearance=secret' will
 * give visibility to data items with 'clearance=ordinary'.
 */
@SuppressWarnings("java:S1700")
public class Hierarchy {

    /** Hierarchy getter function that always return null (no hierarchy). */
    public static final HierarchyGetter noHierarchy = a->null;

    // Low (index 0) to high
    private final Attribute attribute;
    private final List<ValueTerm> hierarchy;

    public static Hierarchy create(String attrName, String ... strings) {
        Attribute attr = Attribute.create(attrName);
        return create(attr, strings);
    }

    public static Hierarchy create(Attribute attr, String ... strings) {
        ValueTerm[] a = new ValueTerm[strings.length];
        for (int i = 0 ; i < strings.length ; i ++) {
            if ( strings[i] == null )
                throw new IllegalArgumentException("Null in attribute value hierarchy: "+Arrays.asList(strings));
            a[i] = ValueTerm.value(strings[i]);
        }
        return new Hierarchy(attr, Arrays.asList(a));
    }

    public static Hierarchy create(Attribute attr, List<String> strings) {
        ValueTerm[] a = new ValueTerm[strings.size()];
        for (int i = 0 ; i < strings.size() ; i ++) {
            if ( strings.get(i) == null )
                throw new IllegalArgumentException("Null in attribute value hierarchy: "+ List.of(strings));
            a[i] = ValueTerm.value(strings.get(i));
        }
        return new Hierarchy(attr, Arrays.asList(a));
    }

    public Hierarchy(Attribute attr, List<ValueTerm> values) {
        this.attribute = attr;
        this.hierarchy = values;
        checkName();
        checkNoNulls();
        checkNoDuplicates();
    }

    private void checkName() {
        String name = attribute.name();
        if ( name.isEmpty() )
            throw new IllegalArgumentException("Hierarchy name is empty");
        if ( name.contains(" ") )
            throw new IllegalArgumentException("Hierarchy name must not contain spaces: "+attribute);
        if ( name.contains(":") )
            throw new IllegalArgumentException("Hierarchy name must not contain colon: "+attribute);
    }

    private void checkNoNulls() {
        // ArrayList supports null.
        for (int i = 0; i < hierarchy.size(); i++)
            if (hierarchy.get(i) == null )
                throw new IllegalArgumentException("Null in attribute value hierarchy: "+hierarchy);
    }

    private void checkNoDuplicates() {
        Set<ValueTerm> setOf = new HashSet<>(hierarchy);
        if ( setOf.size() != hierarchy.size() )
            throw new IllegalArgumentException("Duplicate in attribute value hierarchy: "+hierarchy);
    }

    public Attribute attribute() { return attribute; }

    public List<ValueTerm> values() { return hierarchy; }

    /** Format: "name: a,b,c" - syntactically valid comma separated list */
    public String asString() {
        StringJoiner sj = new StringJoiner(", ");
        hierarchy.forEach(valueTerm-> sj.add(valueTerm.asString()) );
        return Words.wordStr(attribute.name())+": "+sj.toString();
    }

    /** From a valid comma separated list */
    public static Hierarchy fromString(String string) {
        return AE.parseHierarchy(string);
    }

    public enum Comparison {
        LT, EQ, GT,
        NONE,
    }
    /**
     * Compare two AttrValues.
     * <p>
     * Cost is linear in the number of attribute values in the hierarchy.
     * <p>
     * Returns: Comparison; v1 CMP v2.
     * Hierarchy list are stored  "low to high"
     * <ul>
     * <li> LT, EQ, GT if both elements are in the hierarchy.
     * <li> NONE if either of v1 and v2 is not in the hierarchy.
     * </ul>
     */
    public Comparison compareTo(ValueTerm v1, ValueTerm v2) {
        Objects.requireNonNull(v1);
        Objects.requireNonNull(v2);
        int idx1 = hierarchy.indexOf(v1);
        if ( idx1 < 0 )
            return Comparison.NONE;

        int idx2 = hierarchy.indexOf(v2);
        if ( idx2 < 0 )
            return Comparison.NONE;

        return compareIndexes(idx1, idx2);
    }

    private Comparison compareIndexes(int idx1, int idx2) {
        return switch (Integer.compare(idx1, idx2)) {
            case -1 -> Comparison.LT;
            case 0 -> Comparison.EQ;
            case 1 -> Comparison.GT;
            default -> throw new IllegalStateException("Unexpected hierarchy comparison result");
        };
    }

    @Override
    public String toString() {
        return attribute.name()+": "+hierarchy.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(hierarchy, attribute);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        Hierarchy other = (Hierarchy)obj;
        return Objects.equals(hierarchy, other.hierarchy) && Objects.equals(attribute, other.attribute);
    }
}
