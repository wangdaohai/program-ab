package org.alicebot.ab;

import org.alicebot.ab.aiml.AIMLDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TripleStore {

    private static final Logger logger = LoggerFactory.getLogger(TripleStore.class);
    private static final String UNDEFINED_TRIPLE = "NIL";

    private int idCnt = 0;
    private String name = "unknown";
    private Chat chatSession;
    private Bot bot;
    private Map<String, Triple> idTriple = new HashMap<>();
    private Map<String, String> tripleStringId = new HashMap<>();
    private Map<String, HashSet<String>> subjectTriples = new HashMap<>();
    private Map<String, HashSet<String>> predicateTriples = new HashMap<>();
    private Map<String, HashSet<String>> objectTriples = new HashMap<>();

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

        @Override
        public String toString() {
            return "Triple(id=" + id + ",subject=" + subject + ",predicate=" + predicate + ",object=" + object;
        }
    }

    private String mapTriple(Triple triple) {
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

    private String unMapTriple(Triple triple) {
        String s = triple.subject;
        String p = triple.predicate;
        String o = triple.object;

        s = s.toUpperCase();
        p = p.toUpperCase();
        o = o.toUpperCase();

        String tripleString = s + ":" + p + ":" + o;

        logger.info("unMapTriple {}", tripleString);
        tripleString = tripleString.toUpperCase();

        triple = idTriple.get(tripleStringId.get(tripleString));

        logger.info("unMapTriple {}", triple);
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
            id = UNDEFINED_TRIPLE;
        }

        return id;

    }

    private Set<String> allTriples() {
        return new HashSet<>(idTriple.keySet());
    }

    public String addTriple(String subject, String predicate, String object) {
        if (subject == null || predicate == null || object == null) { return UNDEFINED_TRIPLE; }
        Triple triple = new Triple(subject, predicate, object);
        return mapTriple(triple);
    }

    public String deleteTriple(String subject, String predicate, String object) {
        if (subject == null || predicate == null || object == null) { return UNDEFINED_TRIPLE; }
        logger.debug("Deleting {} {} {}", subject, predicate, object);
        Triple triple = new Triple(subject, predicate, object);
        return unMapTriple(triple);
    }

    public void printTriples() {
        for (Map.Entry<String, Triple> entry : idTriple.entrySet()) {
            Triple triple = entry.getValue();
            logger.info("{}:{}:{}:{}", entry.getKey(), triple.subject, triple.predicate, triple.object);
        }
    }

    private Set<String> emptySet() {
        return new HashSet<>();
    }

    private Set<String> getTriples(String s, String p, String o) {
        logger.debug("TripleStore: getTriples [{}] {}:{}:{}", idTriple.size(), s, p, o);
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

        return new HashSet<>(resultSet);
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
        String result = AIMLDefault.default_list_item;//"NIL"
        for (String x : triples) {
            result = x + " " + result;//"CONS "+x+" "+result;
        }
        return result.trim();
    }

    private String getSubject(String id) {
        return idTriple.containsKey(id) ? idTriple.get(id).subject : "Unknown subject";
    }

    private String getPredicate(String id) {
        return idTriple.containsKey(id) ? idTriple.get(id).predicate : "Unknown predicate";
    }

    private String getObject(String id) {
        return idTriple.containsKey(id) ? idTriple.get(id).object : "Unknown object";
    }

    private String stringTriple(String id) {
        Triple triple = idTriple.get(id);
        return id + " " + triple.subject + " " + triple.predicate + " " + triple.object;
    }

    public void printAllTriples() {
        idTriple.keySet().stream().map(this::stringTriple).forEach(logger::info);
    }

    public Set<Tuple> select(HashSet<String> vars, Set<String> visibleVars, List<Clause> clauses) {
        Set<Tuple> result = new HashSet<>();
        try {

            Tuple tuple = new Tuple(vars, visibleVars);
            result = selectFromRemainingClauses(tuple, clauses);
            if (logger.isDebugEnabled()) {
                result.stream().map(Tuple::toString).forEach(logger::info);
            }

        } catch (Exception ex) {
            logger.error("Something went wrong with select {}", visibleVars, ex);
        }
        return result;
    }

    private Clause adjustClause(Tuple tuple, Clause clause) {
        Set<String> vars = tuple.getVars();
        String subj = clause.subj;
        String pred = clause.pred;
        String obj = clause.obj;
        Clause newClause = clause.copy();
        if (vars.contains(subj)) {
            String value = tuple.getValue(subj);
            if (!value.equals(AIMLDefault.unbound_variable)) {
                newClause.subj = value;
            }
        }
        if (vars.contains(pred)) {
            String value = tuple.getValue(pred);
            if (!value.equals(AIMLDefault.unbound_variable)) {
                newClause.pred = value;
            }
        }
        if (vars.contains(obj)) {
            String value = tuple.getValue(obj);
            if (!value.equals(AIMLDefault.unbound_variable)) {
                newClause.obj = value;
            }
        }
        return newClause;

    }

    private Tuple bindTuple(Tuple partial, String triple, Clause clause) {
        Tuple tuple = new Tuple(partial);
        if (clause.subj.startsWith("?")) { tuple.bind(clause.subj, getSubject(triple)); }
        if (clause.pred.startsWith("?")) { tuple.bind(clause.pred, getPredicate(triple)); }
        if (clause.obj.startsWith("?")) { tuple.bind(clause.obj, getObject(triple)); }
        return tuple;
    }

    public Set<Tuple> selectFromSingleClause(Tuple partial, Clause clause, boolean affirm) {
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

    private Set<Tuple> selectFromRemainingClauses(Tuple partial, List<Clause> clauses) {
        //System.out.println("TripleStore: partial = "+partial.toString()+" clauses.size()=="+clauses.size());
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
