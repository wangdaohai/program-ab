package org.alicebot.ab;

import org.alicebot.ab.aiml.AIMLDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Tuple {

    private static final Logger logger = LoggerFactory.getLogger(Tuple.class);

    private static final Map<String, Tuple> tupleMap = new HashMap<>();
    private static final AtomicInteger index = new AtomicInteger(0);

    private final Map<String, String> valueMap = new HashMap<>();
    private final Set<String> visibleVars = new HashSet<>();
    private final String name;

    private Tuple() {
        name = "tuple" + index.getAndIncrement();
        tupleMap.put(name, this);
    }

    public Tuple(Tuple tuple) {
        this();
        valueMap.putAll(tuple.valueMap);
        this.visibleVars.addAll(tuple.visibleVars);
    }

    public Tuple(Set<String> varSet, Set<String> visibleVars) {
        this();
        if (visibleVars != null) {
            this.visibleVars.addAll(visibleVars);
        }
        if (varSet != null) {
            for (String key : varSet) {
                valueMap.put(key, AIMLDefault.unbound_variable);
            }
        }
    }

    /** Retrieve an existing tuple by name */
    public static Tuple forName(String tupleName) {
        return tupleMap.get(tupleName);
    }

    public String name() {
        return name;
    }

    public Set<String> getVars() {
        return Collections.unmodifiableSet(valueMap.keySet());
    }

    public String getValue(String var) {
        String result = get(var);
        return result == null ? AIMLDefault.default_get : result;
    }

    public void bind(String var, String value) {
        if (get(var) != null && !get(var).equals(AIMLDefault.unbound_variable)) {
            logger.warn("{} already bound to {}", var, get(var));
        } else {
            valueMap.put(var, value);
        }

    }

    private String get(String key) {
        return valueMap.get(key);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Tuple tuple = (Tuple) o;

        // if (!visibleVars.equals(tuple.visibleVars)) return false;
        if (visibleVars.size() != tuple.visibleVars.size()) {
            //System.out.println("Tuple: "+name+"!="+tuple.name+" because size "+visibleVars.size()+"!="+tuple.visibleVars.size());
            return false;
        }
        //System.out.println("Tuple visibleVars = "+visibleVars+" tuple.visibleVars = "+tuple.visibleVars);
        for (String x : visibleVars) {
            //System.out.println("Tuple: get("+x+")="+get(x)+"tuple.get("+x+")="+tuple.get(x));
            if (!tuple.visibleVars.contains(x)) {
                //System.out.println("Tuple: "+name+"!="+tuple.name+" because !tuple.visibleVars.contains("+x+")");
                return false;
            } else if (get(x) != null && !get(x).equals(tuple.get(x))) {
                //System.out.println("Tuple: "+name+"!="+tuple.name+" because get("+x+")="+get(x)+" and tuple.get("+x+")="+tuple.get(x));
                return false;
            }
        }
        if (valueMap.values().contains(AIMLDefault.unbound_variable)) { return false; }
        if (tuple.valueMap.values().contains(AIMLDefault.unbound_variable)) { return false; }
        return true;
    }

    @Override
    public int hashCode() {
        //System.out.println("Calling hashCode");
        int result = 1;
        for (String x : visibleVars) {
            result = 31 * result + x.hashCode();
            if (get(x) != null) {
                result = 31 * result + get(x).hashCode();
            }
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder().append("\n");
        for (String x : valueMap.keySet()) {
            result.append(x).append("=").append(get(x)).append("\n");
        }
        return result.toString().trim();
    }

}
