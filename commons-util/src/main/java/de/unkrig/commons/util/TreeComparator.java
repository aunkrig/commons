
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2011, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Recursively compares two trees of nodes. Non-leaf nodes have a {@link SortedSet} of keys to child nodes.
 *
 * @param <N>  The node type
 * @param <EX> The exception that the abstract methods may throw
 * @see #compare(Node, Node)
 */
public abstract
class TreeComparator<N extends TreeComparator.Node<N>, EX extends Throwable> {

    /**
     * The base interface for leaf nodes and non-leaf nodes.
     *
     * @param <N> The "real" type of the node, e.g. a class "{@code Directory}" that would represent a directory in a
     *            file system
     */
    public
    interface Node<N extends Node<N>> {

        /**
         * @return {@code null} iff this is a leaf node
         */
        @Nullable SortedSet<N>
        children();
    }

    /**
     * If both nodes are non-leaf nodes, {@link #nodeDeleted} and {@link #nodeAdded} are called for each child that
     * exists only in the first resp. only in the second child set. For each child that exists in both child sets, this
     * method is invoked recursively.
     * <p>
     * Otherwise, if one of the nodes is a leaf node and the other is not, {@link #leafNodeChangedToNonLeafNode} or
     * {@link #nonLeafNodeChangedToLeafNode} is invoked.
     * <p>
     * Otherwise, both nodes are leaf nodes, and {@link #leafNodeRemains} is invoked.
     *
     * @return The number of differences between the two trees
     */
    public long
    compare(N node1, N node2) throws EX {

        SortedSet<N> children1 = node1.children();
        SortedSet<N> children2 = node2.children();

        if (children1 == null) {
            if (children2 == null) {
                this.leafNodeRemains(node1, node2);
                return 0;
            }
            this.leafNodeChangedToNonLeafNode(node1, node2);
            return 1;
        }
        if (children2 == null) {
            this.nonLeafNodeChangedToLeafNode(node1, node2);
            return 1;
        }

        Comparator<? super N> comparator = children1.comparator();
        if (comparator != children2.comparator()) {
            throw new IllegalArgumentException("Child sets have different comparators");
        }

        Iterator<N> it1 = children1.iterator();
        Iterator<N> it2 = children2.iterator();

        long differenceCount = 0;
        if (it1.hasNext() && it2.hasNext()) {

            // Read ahead one entry from both child sets.
            N subnode1 = it1.next();
            N subnode2 = it2.next();

            // Now compare the sorted sets element by element.
            for (;;) {

                @SuppressWarnings({ "unchecked", "rawtypes" }) int comp = (
                    comparator == null
                    ? ((Comparable) subnode1).compareTo(subnode2)
                    : comparator.compare(subnode1, subnode2)
                );

                if (comp < 0) {
                    this.nodeDeleted(subnode1);
                    differenceCount++;
                    if (!it1.hasNext()) {
                        this.nodeAdded(subnode2);
                        differenceCount++;
                        break;
                    }
                    subnode1 = it1.next();
                } else
                if (comp > 0) {
                    this.nodeAdded(subnode2);
                    differenceCount++;
                    if (!it2.hasNext()) {
                        this.nodeDeleted(subnode1);
                        differenceCount++;
                        break;
                    }
                    subnode2 = it2.next();
                } else
                {
                    differenceCount += this.compare(subnode1, subnode2);
                    if (!it1.hasNext() || !it2.hasNext()) break;
                    subnode1 = it1.next();
                    subnode2 = it2.next();
                }
            }
        }
        while (it1.hasNext()) {
            this.nodeDeleted(it1.next());
            differenceCount++;
        }
        while (it2.hasNext()) {
            this.nodeAdded(it2.next());
            differenceCount++;
        }

        return differenceCount;
    }

    /**
     * This abstract method is invoked by {@link #compare(Node, Node)} for every node that does not exist under {@code
     * node1}, but under {@code node2}.
     */
    protected abstract void nodeAdded(N node) throws EX;

    /**
     * This abstract method is invoked by {@link #compare(Node, Node)} for every node that exists under {@code node1},
     * but not under {@code node2}.
     */
    protected abstract void nodeDeleted(N node) throws EX;

    /**
     * This abstract method is invoked by {@link #compare(Node, Node)} for every node that is a non-leaf node under
     * {@code node1} and a leaf node under {@code node2}.
     */
    protected abstract void nonLeafNodeChangedToLeafNode(N node1, N node2) throws EX;

    /**
     * This abstract method is invoked by {@link #compare(Node, Node)} for every node that is a leaf node under {@code
     * node1} and a non-leaf node under {@code node2}.
     */
    protected abstract void leafNodeChangedToNonLeafNode(N node1, N node2) throws EX;

    /**
     * This abstract method is invoked by {@link #compare(Node, Node)} for every leaf node that exists under {@code
     * node1} and under {@code node2}.
     */
    protected abstract void leafNodeRemains(N node1, N node2) throws EX;
}
