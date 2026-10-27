package branchforge.model;

import java.util.*;

public class CommitGraph {

    private final Map<String, List<String>> parentsOf = new HashMap<>();
    private final Map<String , List<String>> childOf = new HashMap<>() ;

    public void addCommit(Commit commit){
        String id = commit.getId();
        parentsOf.put(id , new ArrayList<>(commit.getParentIds()));
        childOf.computeIfAbsent(id , K -> new ArrayList<>());
        for(String parent : commit.getParentIds()){
            List<String> kids = childOf.computeIfAbsent(parent , K -> new ArrayList<>());
            if(!kids.contains(id)){
                kids.add(id);
            }
        }
    }

    public void rebuild(List<Commit> commits){
        parentsOf.clear();
        childOf.clear();
        for(Commit commit : commits){
            addCommit(commit);
        }
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
                if(seen.add(parent)) queue.add(parent);
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
                if(!distance.containsKey(parent)){
                    distance.put(parent , distance.get(current) + 1);
                    deque.add(parent);
                }
            }
        }
        return distance;
    }

    public String getLCA(String currentId , String targetId){
        Set<String> candidates = latestCommonAncestors(currentId , targetId);

        if(candidates.isEmpty()){
            return null ;
        }
        Map<String , Integer> distance = edgeDistances(currentId);
        String best = null ;
        for(String id : candidates){
            if(best == null){
                best = id ;
                continue;
            }
            int d = distance.get(id);
            int bestD = distance.get(best);
            if(d < bestD || (d == bestD && id.compareTo(best) < 0)){
                best = id ;
            }
        }
        return best ;
    }

    private Set<String> latestCommonAncestors(String a , String b){
        Set<String > common = new HashSet<>(bfsAncestors(a));
        common.retainAll(bfsAncestors(b));

        Set<String> dominated = new HashSet<>();

        for(String c : common){
            Set<String> ancestors = bfsAncestors(c);
            ancestors.remove(c);
            dominated.addAll(ancestors);
        }
        common.removeAll(dominated);
        return common ;
    }
    public Set<String> getLCAs(String currentId, String targetId) {
        return new HashSet<>();
    }
}
