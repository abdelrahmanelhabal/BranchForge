package branchforge.model;

import java.util.*;

public class CommitGraph {

    private final Map<String, List<String>> parentsOf = new HashMap<>();
    private final Map<String , List<String>> childOf = new HashMap<>() ;

    public void addCommit(Commit commit) {
        Objects.requireNonNull(commit, "commit");
        String id = commit.getId();
        List<String> newParents = new ArrayList<>(commit.getParentIds());

        List<String> existing = parentsOf.get(id);
        if (existing != null && !existing.equals(newParents)) {
            throw new IllegalStateException("Commit " + id + " already exists with different parents");
        }

        parentsOf.put(id, newParents);
        childOf.computeIfAbsent(id, k -> new ArrayList<>());
        for (String parent : newParents) {
            List<String> kids = childOf.computeIfAbsent(parent, k -> new ArrayList<>());
            if (!kids.contains(id)) {
                kids.add(id);
            }
        }
    }

    public void rebuild(List<Commit> commits) {
        Objects.requireNonNull(commits, "commits");
        for (Commit commit : commits) {
            if (commit == null) {
                throw new NullPointerException("commit can't be null");
            }
        }
        CommitGraph temp = new CommitGraph();
        for (Commit commit : commits) {
            temp.addCommit(commit);
        }
        parentsOf.clear();
        childOf.clear();
        parentsOf.putAll(temp.parentsOf);
        childOf.putAll(temp.childOf);
    }

    public boolean contains(String id){
        return parentsOf.containsKey(id);
    }

    public List<String> getParents(String id){
        return Collections.unmodifiableList(parentsOf.getOrDefault(id , Collections.emptyList()));
    }

    public List<String> getChildren(String id){
        return Collections.unmodifiableList(childOf.getOrDefault(id , Collections.emptyList()));
    }

    public boolean isAncestorOf(String ancestor , String descendant){
        return bfsAncestors(descendant).contains(ancestor);
    }

    private Set<String> bfsAncestors(String start){
        Set<String> seen = new LinkedHashSet<>();
        if(!contains(start)){
            return seen ;
        }

        Deque<String> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()){
            String current = queue.poll();
            for(String parent : getParents(current)){
                if(contains(parent) && seen.add(parent)) queue.add(parent);
            }
        }
        return seen ;
    }

    private Map<String,Integer> edgeDistances(String start){
        Map<String,Integer> distance = new HashMap<>();
        if(!contains(start)){
            return distance;
        }
        Deque<String> deque = new ArrayDeque<>();
        distance.put(start , 0);
        deque.add(start);
        while (!deque.isEmpty()){
            String current = deque.poll();
            for(String parent : getParents(current)){
                if(contains(parent) && !distance.containsKey(parent)){
                    distance.put(parent , distance.get(current) + 1);
                    deque.add(parent);
                }
            }
        }
        return distance;
    }

    public String getLCA(String currentId, String targetId) {
        Set<String> lcas = getLCAs(currentId, targetId);
        return lcas.isEmpty() ? null : Collections.min(lcas);
    }

    private Set<String> latestCommonAncestors(String a , String b){
        Set<String > common = new HashSet<>(bfsAncestors(a));
        common.retainAll(bfsAncestors(b));
        if (common.isEmpty()) {
            return common;
        }
        Set<String> dominated = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        for (String c : common) {
            for (String p : getParents(c)) {
                if (contains(p) && dominated.add(p)) {
                    queue.add(p);
                }
            }
        }
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String p : getParents(current)) {
                if (contains(p) && dominated.add(p)) {
                    queue.add(p);
                }
            }
        }
        common.removeAll(dominated);
        return common ;
    }
    public Set<String> getLCAs(String currentId, String targetId) {
        Set<String> candidates = latestCommonAncestors(currentId, targetId);
        Set<String> result = new HashSet<>();
        if (candidates.isEmpty()) {
            return result;
        }

        Map<String, Integer> distA = edgeDistances(currentId);
        Map<String, Integer> distB = edgeDistances(targetId);

        int bestTotal = Integer.MAX_VALUE;
        for (String id : candidates) {
            int total = distA.get(id) + distB.get(id);
            if (total < bestTotal) {
                bestTotal = total;
                result.clear();
                result.add(id);
            } else if (total == bestTotal) {
                result.add(id);
            }
        }
        return result;
    }
}
