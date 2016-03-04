package org.alicebot.ab;

import java.util.*;

public class TripleStore {
    public int idCnt = 0;
    public String name = "unknown";
    public Chat chatSession;
    public Bot bot;
    public Map<String, Triple> idTriple = new HashMap<>();
    public Map<String, String> tripleStringId = new HashMap<>();
    public Map<String, HashSet<String>> subjectTriples = new HashMap<>();
    public Map<String, HashSet<String>> predicateTriples = new HashMap<>();
    public Map<String, HashSet<String>> objectTriples = new HashMap<>();

    public TripleStore(String name, Chat chatSession) {
        this.name = name;
        this.chatSession = chatSession;
        this.bot = chatSession.bot;
    }

    public class Triple {
        public String id;
        public String subject;
        public String predicate;
        public String object;

        public Triple(String s, String p, String o) {
            Bot bot = TripleStore.this.bot;
            if (bot != null) {
                s = bot.preProcessor.normalize(s);
                p = bot.preProcessor.normalize(p);
                o = bot.preProcessor.normalize(o);
            }
            if (s != null && p != null && o != null) {
                //System.out.println("New triple "+s+":"+p+":"+o);
                subject = s;
                predicate = p;
                object = o;
                id = name + idCnt++;
                // System.out.println("New triple "+id+"="+s+":"+p+":"+o);

            }
        }
    }

    public String mapTriple(Triple triple) {
        String id = triple.id;
        idTriple.put(id, triple);
        String s = triple.subject;
        String p = triple.predicate;
        String o = triple.object;

        s = s.toUpperCase();
        p = p.toUpperCase();
        o = o.toUpperCase();

        String tripleString = s + ":" + p + ":" + o;
        tripleString = tripleString.toUpperCase();

        if (tripleStringId.keySet().contains(tripleString)) {
            //System.out.println("Found "+tripleString+" "+tripleStringId.get(tripleString));
            return tripleStringId.get(tripleString); // triple already exists
        } else {
            //System.out.println(tripleString+" not found");
            tripleStringId.put(tripleString, id);

            HashSet<String> existingTriples;
            if (subjectTriples.containsKey(s)) {
                existingTriples = subjectTriples.get(s);
            } else {
                existingTriples = new HashSet<>();
            }
            existingTriples.add(id);
            subjectTriples.put(s, existingTriples);

            if (predicateTriples.containsKey(p)) {
                existingTriples = predicateTriples.get(p);
            } else {
                existingTriples = new HashSet<>();
            }
            existingTriples.add(id);
            predicateTriples.put(p, existingTriples);

            if (objectTriples.containsKey(o)) {
                existingTriples = objectTriples.get(o);
            } else {
                existingTriples = new HashSet<>();
            }
            existingTriples.add(id);
            objectTriples.put(o, existingTriples);

            return id;
        }
    }

    public String unMapTriple(Triple triple) {
        String s = triple.subject;
        String p = triple.predicate;
        String o = triple.object;

        s = s.toUpperCase();
        p = p.toUpperCase();
        o = o.toUpperCase();

        String tripleString = s + ":" + p + ":" + o;

        System.out.println("unMapTriple " + tripleString);
        tripleString = tripleString.toUpperCase();

        triple = idTriple.get(tripleStringId.get(tripleString));

        System.out.println("unMapTriple " + triple);
        String id;
        if (triple != null) {
            id = triple.id;
            idTriple.remove(id);
            tripleStringId.remove(tripleString);

            HashSet<String> existingTriples;
            if (subjectTriples.containsKey(s)) {
                existingTriples = subjectTriples.get(s);
            } else {
                existingTriples = new HashSet<>();
            }
            existingTriples.remove(id);
            subjectTriples.put(s, existingTriples);

            if (predicateTriples.containsKey(p)) {
                existingTriples = predicateTriples.get(p);
            } else {
                existingTriples = new HashSet<>();
            }
            existingTriples.remove(id);
            predicateTriples.put(p, existingTriples);

            if (objectTriples.containsKey(o)) {
                existingTriples = objectTriples.get(o);
            } else {
                existingTriples = new HashSet<>();
            }
            existingTriples.remove(id);
            objectTriples.put(o, existingTriples);
        } else {
            id = MagicStrings.undefined_triple;
        }

        return id;

    }

    public Set<String> allTriples() {
        return new HashSet<>(idTriple.keySet());
    }

    public String addTriple(String subject, String predicate, String object) {
        if (subject == null || predicate == null || object == null) { return MagicStrings.undefined_triple; }
        Triple triple = new Triple(subject, predicate, object);
        return mapTriple(triple);
    }

    public String deleteTriple(String subject, String predicate, String object) {
        if (subject == null || predicate == null || object == null) { return MagicStrings.undefined_triple; }
        if (MagicBooleans.trace_mode) { System.out.println("Deleting " + subject + " " + predicate + " " + object); }
        Triple triple = new Triple(subject, predicate, object);
        return unMapTriple(triple);
    }

    public void printTriples() {
        for (Map.Entry<String, Triple> stringTripleEntry : idTriple.entrySet()) {
            Triple triple = stringTripleEntry.getValue();
            System.out.println(stringTripleEntry.getKey() + ":" + triple.subject + ":" + triple.predicate + ":" + triple.object);
        }
    }

    Set<String> emptySet() {
        return new HashSet<>();
    }

    public Set<String> getTriples(String s, String p, String o) {
        if (MagicBooleans.trace_mode) {
            System.out.println("TripleStore: getTriples [" + idTriple.size() + "] " + s + ":" + p + ":" + o);
        }
        //printAllTriples();
        Set<String> subjectSet;
        if (s == null || s.startsWith("?")) {
            subjectSet = allTriples();
        } else {
            s = s.toUpperCase();
            // System.out.println("subjectTriples.keySet()="+subjectTriples.keySet());
            // System.out.println("subjectTriples.get("+s+")="+subjectTriples.get(s));
            // System.out.println("subjectTriples.containsKey("+s+")="+subjectTriples.containsKey(s));
            subjectSet = subjectTriples.containsKey(s) ? subjectTriples.get(s) : emptySet();
        }
        // System.out.println("subjectSet="+subjectSet);

        Set<String> predicateSet;
        if (p == null || p.startsWith("?")) {
            predicateSet = allTriples();
        } else {
            p = p.toUpperCase();
            predicateSet = predicateTriples.containsKey(p) ? predicateTriples.get(p) : emptySet();
        }

        Set<String> objectSet;
        if (o == null || o.startsWith("?")) {
            objectSet = allTriples();
        } else {
            o = o.toUpperCase();
            objectSet = objectTriples.containsKey(o) ? objectTriples.get(o) : emptySet();
        }

        Set<String> resultSet = new HashSet<>(subjectSet);
        resultSet.retainAll(predicateSet);
        resultSet.retainAll(objectSet);

        Set<String> finalResultSet = new HashSet<>(resultSet);

        //System.out.println("TripleStore.getTriples: "+finalResultSet.size()+" results");
        /* System.out.println("getTriples subjectSet="+subjectSet);
        System.out.println("getTriples predicateSet="+predicateSet);
        System.out.println("getTriples objectSet="+objectSet);
        System.out.println("getTriples result="+resultSet);*/

        return finalResultSet;
    }

    public HashSet<String> getSubjects(Set<String> triples) {
        HashSet<String> resultSet = new HashSet<>();
        for (String id : triples) {
            Triple triple = idTriple.get(id);
            resultSet.add(triple.subject);
        }
        return resultSet;
    }

    public HashSet<String> getPredicates(Set<String> triples) {
        HashSet<String> resultSet = new HashSet<>();
        for (String id : triples) {
            Triple triple = idTriple.get(id);
            resultSet.add(triple.predicate);
        }
        return resultSet;
    }

    public HashSet<String> getObjects(Set<String> triples) {
        HashSet<String> resultSet = new HashSet<>();
        for (String id : triples) {
            Triple triple = idTriple.get(id);
            resultSet.add(triple.object);
        }
        return resultSet;
    }

    public String formatAIMLTripleList(Set<String> triples) {
        String result = MagicStrings.default_list_item;//"NIL"
        for (String x : triples) {
            result = x + " " + result;//"CONS "+x+" "+result;
        }
        return result.trim();
    }

    public String getSubject(String id) {
        return idTriple.containsKey(id) ? idTriple.get(id).subject : "Unknown subject";
    }

    public String getPredicate(String id) {
        return idTriple.containsKey(id) ? idTriple.get(id).predicate : "Unknown predicate";
    }

    public String getObject(String id) {
        return idTriple.containsKey(id) ? idTriple.get(id).object : "Unknown object";
    }

    public String stringTriple(String id) {
        Triple triple = idTriple.get(id);
        return id + " " + triple.subject + " " + triple.predicate + " " + triple.object;
    }

    public void printAllTriples() {
        for (String id : idTriple.keySet()) {
            System.out.println(stringTriple(id));
        }
    }

    public Set<Tuple> select(HashSet<String> vars, Set<String> visibleVars, List<Clause> clauses) {
        Set<Tuple> result = new HashSet<>();
        try {

            Tuple tuple = new Tuple(vars, visibleVars);
            //System.out.println("TripleStore: select vars = "+tuple.printVars());
            result = selectFromRemainingClauses(tuple, clauses);
            if (MagicBooleans.trace_mode) {
                for (Tuple t : result) {
                    System.out.println(t.printTuple());
                }
            }

        } catch (Exception ex) {
            System.out.println("Something went wrong with select " + visibleVars);
            ex.printStackTrace();

        }
        return result;
    }

    public Clause adjustClause(Tuple tuple, Clause clause) {
        Set<String> vars = tuple.getVars();
        String subj = clause.subj;
        String pred = clause.pred;
        String obj = clause.obj;
        Clause newClause = new Clause(clause);
        if (vars.contains(subj)) {
            String value = tuple.getValue(subj);
            if (!value.equals(MagicStrings.unbound_variable)) {/*System.out.println("adjusting "+subj+" "+value);*/
                newClause.subj = value;
            }
        }
        if (vars.contains(pred)) {
            String value = tuple.getValue(pred);
            if (!value.equals(MagicStrings.unbound_variable)) {/*System.out.println("adjusting "+pred+" "+value);*/
                newClause.pred = value;
            }
        }
        if (vars.contains(obj)) {
            String value = tuple.getValue(obj);
            if (!value.equals(MagicStrings.unbound_variable)) {/*System.out.println("adjusting "+obj+" "+value); */
                newClause.obj = value;
            }
        }
        return newClause;

    }

    public Tuple bindTuple(Tuple partial, String triple, Clause clause) {
        Tuple tuple = new Tuple(partial);
        if (clause.subj.startsWith("?")) { tuple.bind(clause.subj, getSubject(triple)); }
        if (clause.pred.startsWith("?")) { tuple.bind(clause.pred, getPredicate(triple)); }
        if (clause.obj.startsWith("?")) { tuple.bind(clause.obj, getObject(triple)); }
        return tuple;
    }

    public Set<Tuple> selectFromSingleClause(Tuple partial, Clause clause, Boolean affirm) {
        Set<Tuple> result = new HashSet<>();
        Set<String> triples = getTriples(clause.subj, clause.pred, clause.obj);
        //System.out.println("TripleStore: selected "+triples.size()+" from single clause "+clause.subj+" "+clause.pred+" "+clause.obj);
        if (affirm) {
            for (String triple : triples) {
                Tuple tuple = bindTuple(partial, triple, clause);
                result.add(tuple);
            }
        } else {
            if (triples.isEmpty()) { result.add(partial); }
        }
        return result;
    }

    public Set<Tuple> selectFromRemainingClauses(Tuple partial, List<Clause> clauses) {
        //System.out.println("TripleStore: partial = "+partial.printTuple()+" clauses.size()=="+clauses.size());
        Set<Tuple> result = new HashSet<>();
        Clause clause = clauses.get(0);
        clause = adjustClause(partial, clause);
        Set<Tuple> tuples = selectFromSingleClause(partial, clause, clause.affirm);
        if (clauses.size() > 1) {
            List<Clause> remainingClauses = new ArrayList<>(clauses);
            remainingClauses.remove(0);
            for (Tuple tuple : tuples) {
                result.addAll(selectFromRemainingClauses(tuple, remainingClauses));
            }
        } else {
            result = tuples;
        }
        return result;
    }

}
