package branchforge.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class CommitGraphTest {
    private CommitGraph graph;
    @BeforeEach
    void setUp() {
        graph = new CommitGraph();
    }
    private static Commit commit(String id, String... parentIds) {
        return new Commit(
                id,
                "test commit " + id,
                new Date(0L),
                Arrays.asList(parentIds),
                new TreeMap<>()
        );
    }

    @Test
    void testAddCommit_OverwritingExistingCommit_CleansUpStaleChildren() {
        graph.addCommit(commit("P1"));
        graph.addCommit(commit("P2"));
        graph.addCommit(commit("C1", "P1"));
        graph.addCommit(commit("C1", "P2"));
        assertEquals(Collections.singletonList("P2"), graph.getParents("C1"));
        assertFalse(graph.getChildren("P1").contains("C1"),
                "Replacing a commit's parents must remove stale child links.");
        assertTrue(graph.getChildren("P2").contains("C1"));
    }

    @Test
    void testAddCommit_OverwritingCommit_MaintainsItsOwnChildren() {
        graph.addCommit(commit("P1"));
        graph.addCommit(commit("C1", "P1"));
        graph.addCommit(commit("P1"));
        assertTrue(graph.getChildren("P1").contains("C1"),
                "Overwriting a parent commit should not erase its existing children.");
    }

    @Test
    void testGetLCA_WithUnaddedParentCommit_DoesNotReturnPhantomCommit() {
        graph.addCommit(commit("C1", "P0"));
        graph.addCommit(commit("C2", "P0"));

        assertFalse(graph.contains("P0"));
        assertNull(graph.getLCA("C1", "C2"),
                "An ID absent from the graph must not be returned as an LCA.");
    }

    @Test
    void testGetLCA_OneCommitIsAncestorOfAnother_ReturnsAncestor() {
        graph.addCommit(commit("A"));
        graph.addCommit(commit("B", "A"));
        graph.addCommit(commit("C", "B"));

        assertEquals("A", graph.getLCA("A", "C"), "LCA of a commit and its descendant is the commit itself.");
        assertEquals("B", graph.getLCA("C", "B"));
    }

    @Test
    void testGetLCA_ComplexAsymmetricDistances_MinimizesTotalDistance() {
        graph.addCommit(commit("O"));
        graph.addCommit(commit("L1", "O"));
        graph.addCommit(commit("L2", "O"));
        graph.addCommit(commit("C", "L2"));
        graph.addCommit(commit("CUR", "L1", "C"));
        graph.addCommit(commit("TM", "L2"));
        graph.addCommit(commit("T1", "L1"));
        graph.addCommit(commit("T2", "T1"));
        graph.addCommit(commit("T3", "T2"));
        graph.addCommit(commit("T4", "T3"));
        graph.addCommit(commit("T5", "T4"));
        graph.addCommit(commit("TAR", "T5", "TM"));
        assertEquals("L2", graph.getLCA("CUR", "TAR"),
                "LCA must be L2 because its total distance (4) is less than L1 (7).");

        assertEquals("L2", graph.getLCA("TAR", "CUR"),
                "LCA must be symmetric and return L2 regardless of argument order.");
    }

    @Test
    void testGetLCAs_MultipleTies_ReturnsAllEquidistantAncestors() {
        graph.addCommit(commit("Zebra_Commit"));
        graph.addCommit(commit("Apple_Commit"));
        graph.addCommit(commit("Mango_Commit"));
        graph.addCommit(commit("C_ZA", "Zebra_Commit", "Apple_Commit"));
        graph.addCommit(commit("C_M", "Mango_Commit"));
        graph.addCommit(commit("Current", "C_ZA", "C_M"));
        graph.addCommit(commit("T_ZA", "Zebra_Commit", "Apple_Commit"));
        graph.addCommit(commit("T_M", "Mango_Commit"));
        graph.addCommit(commit("Target", "T_ZA", "T_M"));

        Set<String> expectedLCAs = new HashSet<>(Arrays.asList(
                "Apple_Commit", "Mango_Commit", "Zebra_Commit"
        ));
        assertEquals(expectedLCAs, graph.getLCAs("Current", "Target"),
                "Algorithm must return all LCAs in a Set when they have identical minimum distances.");

        assertEquals(expectedLCAs, graph.getLCAs("Target", "Current"),
                "Symmetry should be maintained - the order of arguments must not change the returned Set.");
    }

    @Test
    void testGetLCAs_SingleLCA_ReturnsSetWithOneCommit() {
        graph.addCommit(commit("Root"));
        graph.addCommit(commit("BranchA", "Root"));
        graph.addCommit(commit("BranchB", "Root"));

        Set<String> expected = Collections.singleton("Root");
        assertEquals(expected, graph.getLCAs("BranchA", "BranchB"),
                "Should return a Set containing the single LCA for normal forks.");
    }


    @Test
    void testRebuild_CyclicGraph_IsRejectedWithoutDestroyingExistingGraph() {
        graph.addCommit(commit("OLD_ROOT"));

        List<Commit> cyclicCommits = Arrays.asList(
                commit("A", "B"),
                commit("B", "A")
        );

        assertThrows(IllegalArgumentException.class, () -> graph.rebuild(cyclicCommits),
                "A commit graph must be acyclic; rebuild should reject cycles.");

        assertTrue(graph.contains("OLD_ROOT"),
                "A failed rebuild must leave the prior graph intact.");
        assertFalse(graph.contains("A"));
        assertFalse(graph.contains("B"));
    }

    @Test
    void testRebuild_DeepCycle_IsRejected() {
        List<Commit> deepCycleCommits = Arrays.asList(
                commit("A", "C"),
                commit("B", "A"),
                commit("C", "B")
        );

        assertThrows(IllegalArgumentException.class, () -> graph.rebuild(deepCycleCommits));
    }

    @Test
    void testAddCommit_DuplicateParentIdsInCommit_DeduplicatesParentsList() {
        graph.addCommit(commit("P1"));
        graph.addCommit(commit("C", "P1", "P1"));

        assertEquals(Collections.singletonList("P1"), graph.getParents("C"));
        assertEquals(Collections.singletonList("C"), graph.getChildren("P1"));
    }

    @Test
    void testGetParentsAndGetChildren_ReturnedListImmutability_ThrowsOnMutation() {
        graph.addCommit(commit("P"));
        graph.addCommit(commit("C", "P"));

        assertThrows(UnsupportedOperationException.class, () -> graph.getParents("C").add("X"));
        assertThrows(UnsupportedOperationException.class, () -> graph.getParents("MISSING").clear());
        assertThrows(UnsupportedOperationException.class, () -> graph.getChildren("P").add("X"));
        assertThrows(UnsupportedOperationException.class, () -> graph.getChildren("MISSING").clear());

        assertEquals(List.of("P"), graph.getParents("C"));
        assertEquals(Collections.singletonList("C"), graph.getChildren("P"));
    }

    @Test
    void testRebuild_WithNullElement_IsAtomicAndMaintainsPreviousGraph() {
        graph.addCommit(commit("OLD_ROOT"));
        graph.addCommit(commit("OLD_CHILD", "OLD_ROOT"));
        List<Commit> invalidCommits = Arrays.asList(
                commit("NEW1"),
                null,
                commit("NEW3")
        );
        assertThrows(RuntimeException.class, () -> graph.rebuild(invalidCommits),
                "Rebuild must reject invalid input.");

        assertTrue(graph.contains("OLD_ROOT"), "A failed rebuild must not erase the previously valid graph.");
        assertTrue(graph.contains("OLD_CHILD"));
        assertFalse(graph.contains("NEW1"), "A failed rebuild must not leave a partially rebuilt graph.");
    }
    @Test
    void testGetLCA_DisconnectedGraphs_ReturnsNull() {
        graph.addCommit(commit("Root1"));
        graph.addCommit(commit("Child1", "Root1"));

        graph.addCommit(commit("Root2"));
        graph.addCommit(commit("Child2", "Root2"));

        assertNull(graph.getLCA("Child1", "Child2"),
                "LCA of commits from entirely disconnected histories must be null.");
    }

    @Test
    void testGetLCA_SameCommit_ReturnsCommitItself() {
        graph.addCommit(commit("A"));

        assertEquals("A", graph.getLCA("A", "A"),
                "The lowest common ancestor of a commit with itself is the commit itself.");
    }

    @Test
    void testGetLCA_MissingCommit_ReturnsNull() {
        graph.addCommit(commit("A"));
        assertNull(graph.getLCA("A", "MISSING_COMMIT"),
                "If one of the commits does not exist in the graph, LCA should return null.");
        assertNull(graph.getLCA("MISSING_COMMIT", "A"));
    }

    @Test
    void testIsAncestorOf_DeepLineage_CorrectlyIdentifiesAncestry() {
        graph.addCommit(commit("A"));
        graph.addCommit(commit("B", "A"));
        graph.addCommit(commit("C", "B"));
        graph.addCommit(commit("D", "C"));

        assertTrue(graph.isAncestorOf("A", "D"), "Should identify a great-grandparent as an ancestor.");
        assertTrue(graph.isAncestorOf("B", "D"), "Should identify a grandparent as an ancestor.");
        assertFalse(graph.isAncestorOf("D", "A"), "Descendants are not ancestors of their parents.");
        assertFalse(graph.isAncestorOf("C", "B"));
    }

    @Test
    void testRebuild_EmptyList_ClearsGraphCompletely() {
        graph.addCommit(commit("A"));
        graph.addCommit(commit("B", "A"));
        graph.rebuild(Collections.emptyList());

        assertFalse(graph.contains("A"), "Rebuilding with an empty list should clear previous commits.");
        assertFalse(graph.contains("B"));
        assertTrue(graph.getParents("B").isEmpty());
        assertTrue(graph.getChildren("A").isEmpty());
    }

    @Test
    void testContains_HandlesNullOrEmptyStringSafely() {
        assertFalse(graph.contains(null), "Graph should return false for null IDs.");
        assertFalse(graph.contains(""), "Graph should return false for empty string IDs if unadded.");
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testPerformance_DeepLineage_LCA_ExecutesQuickly() {
        int numCommits = 10000;
        graph.addCommit(commit("Commit_0"));
        for (int i = 1; i < numCommits; i++) {
            graph.addCommit(commit("Commit_" + i, "Commit_" + (i - 1)));
        }
        graph.addCommit(commit("ForkA", "Commit_" + (numCommits - 1)));
        graph.addCommit(commit("ForkB", "Commit_" + (numCommits - 1)));
        String lca = graph.getLCA("ForkA", "ForkB");
        assertEquals("Commit_" + (numCommits - 1), lca);
    }
}