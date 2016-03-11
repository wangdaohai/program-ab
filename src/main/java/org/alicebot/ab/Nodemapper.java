package org.alicebot.ab;
/* Program AB Reference AIML 2.0 implementation
        Copyright (C) 2013 ALICE A.I. Foundation
        Contact: info@alicebot.org

        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Library General Public
        License as published by the Free Software Foundation; either
        version 2 of the License, or (at your option) any later version.

        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Library General Public License for more details.

        You should have received a copy of the GNU Library General Public
        License along with this library; if not, write to the
        Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
        Boston, MA  02110-1301, USA.
*/

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Nodemapper data structure. */
public class Nodemapper {

    public Category category;
    public int height = MagicNumbers.max_graph_height;
    public StarBindings starBindings;
    private Map<String, Nodemapper> map;
    private String key;
    private Nodemapper value;
    public boolean shortCut = false;
    public List<String> sets;

    /** number of branches from node */
    public int size() {
        Set<String> set = new HashSet<>();
        if (shortCut) { set.add("<THAT>"); }
        if (key != null) { set.add(key); }
        if (map != null) { set.addAll(map.keySet()); }
        return set.size();
    }

    /**
     * insert a new link from this node to another, by adding a key, value pair
     *
     * @param key   key word
     * @param value word maps to this next node
     */
    public void put(String key, Nodemapper value) {
        if (map != null) {
            map.put(key, value);
        } else { // node.type == unary_node_mapper
            this.key = key;
            this.value = value;
        }
    }

    /**
     * get the node linked to this one by the word key
     *
     * @param key key word to map
     * @return the mapped node or null if the key is not found
     */
    public Nodemapper get(String key) {
        if (map != null) {
            return map.get(key);
        } else {// node.type == unary_node_mapper
            return key.equals(this.key) ? this.value : null;
        }
    }

    /**
     * check whether a node contains a particular key
     *
     * @param key key to test
     * @return true or false
     */
    public boolean containsKey(String key) {
        if (map != null) {
            return map.containsKey(key);
        } else {// node.type == unary_node_mapper
            return key.equals(this.key);
        }
    }

    /** get key set of a node */
    public Set<String> keySet() {
        if (map != null) {
            return map.keySet();
        }
        if (key != null) {
            return Collections.singleton(key);
        }
        return Collections.emptySet();
    }

    /** test whether a node is a leaf */
    public boolean isLeaf() {
        return category != null;
    }

    public boolean isSingleton() {
        return key != null;
    }

    /** upgrade a node from a singleton to a multi-way map */
    public void upgrade() {
        //System.out.println("Upgrading "+id);
        //type = MagicNumbers.hash_node_mapper;
        map = new HashMap<>();
        map.put(key, value);
        key = null;
        value = null;
    }

}


