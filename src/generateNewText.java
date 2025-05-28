import java.io.*;
import java.security.KeyStore;
import java.util.*;


public class generateNewText {
    public generateNewText() {}
    private static List<String> findBridgeWordsEfficiently(Map<String, Map<String, Integer>> graph, String word1, String word2) {
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

    public static String generate(Map<String, Map<String, Integer>> graph, String inputText) {
        // 预处理输入文本：统一大小写 + 去除标点符号 + 替换为单词数组
        inputText = inputText.toLowerCase().replaceAll("[^a-zA-Z ]", " ").replaceAll("\\s+", " ").trim();
        String[] words = inputText.split(" ");

        if (words.length < 2) return inputText;

        StringBuilder result = new StringBuilder();
        Random rand = new Random();

        result.append(words[0]);

        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];

            // Use the efficient helper function to find bridge words
            List<String> bridgeWords = findBridgeWordsEfficiently(graph, word1, word2);

            // If bridge words are found, pick one randomly and insert it
            if (!bridgeWords.isEmpty()) {
                String chosenBridge = bridgeWords.get(rand.nextInt(bridgeWords.size()));
                result.append(" ").append(chosenBridge); // Append the bridge word
            }

            // Always append the next word from the original sequence
            result.append(" ").append(word2);
        }

        return result.toString();
    }
}

