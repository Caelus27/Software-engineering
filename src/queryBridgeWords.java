import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class queryBridgeWords {
    public queryBridgeWords() {}
    public static List<String> findBridgeWordsEfficiently(Map<String, Map<String, Integer>> graph, String word1, String word2) {
        List<String> bridgeWords = new ArrayList<>();

        // Check if word1 exists as a source node in the graph
        if (!graph.containsKey(word1)) {
            return bridgeWords; // No outgoing edges from word1, so no bridge words possible
        }

        // Get the direct neighbors of word1
        Map<String, Integer> neighborsOfWord1 = graph.get(word1);

        // Iterate through each potential bridge word (neighbors of word1)
        for (String potentialBridge : neighborsOfWord1.keySet()) {
            // Check if the potential bridge word exists as a source node AND links to word2
            if (graph.containsKey(potentialBridge)) {
                Map<String, Integer> neighborsOfBridge = graph.get(potentialBridge);
                if (neighborsOfBridge.containsKey(word2)) {
                    // Found a bridge word!
                    bridgeWords.add(potentialBridge);
                }
            }
        }

        return bridgeWords;
    }
}
