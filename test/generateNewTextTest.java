import org.junit.Before;
import org.junit.Test;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class generateNewTextTest {

    private Map<String, Map<String, Integer>> graph;
    private generateNewText generator;

    @Before
    public void setUp() {
        graph = new HashMap<>();
        generator = new generateNewText(); // Assuming generateNewText has a public constructor

        // Populate the graph with some data for testing
        // new -> life -> new
        // new -> strange -> new
        // to -> seek -> out -> new
        // to -> explore -> strange -> new

        Map<String, Integer> newNeighbors = new HashMap<>();
        newNeighbors.put("life", 1);
        newNeighbors.put("strange", 1);
        graph.put("new", newNeighbors);

        Map<String, Integer> lifeNeighbors = new HashMap<>();
        lifeNeighbors.put("new", 1);
        graph.put("life", lifeNeighbors);

        Map<String, Integer> strangeNeighbors = new HashMap<>();
        strangeNeighbors.put("new", 1);
        graph.put("strange", strangeNeighbors);

        Map<String, Integer> toNeighbors = new HashMap<>();
        toNeighbors.put("seek", 1);
        toNeighbors.put("explore", 1);
        graph.put("to", toNeighbors);

        Map<String, Integer> seekNeighbors = new HashMap<>();
        seekNeighbors.put("out", 1);
        graph.put("seek", seekNeighbors);

        Map<String, Integer> outNeighbors = new HashMap<>();
        outNeighbors.put("new", 1);
        graph.put("out", outNeighbors);

        Map<String, Integer> exploreNeighbors = new HashMap<>();
        exploreNeighbors.put("strange", 1); // to -> explore -> strange -> new
        graph.put("explore", exploreNeighbors);

        // Words that are targets but not sources for more bridge words in this simple setup
        graph.put("civilizations", new HashMap<>()); // For "life new civilizations" example
        newNeighbors.put("civilizations", 1); // new -> civilizations
    }

    @Test
    public void testGenerateWithNoBridgeWords() {
        // "life strange" - No word 'x' such that "life -> x -> strange"
        String inputText = "life strange";
        String expectedText = "life new strange";
        assertEquals(expectedText, generateNewText.generate(graph, inputText));

        String inputText2 = "explore life"; // No bridge from "explore" to "life" via intermediate
        String expectedText2 = "explore life";
        assertEquals(expectedText2, generateNewText.generate(graph, inputText2));
    }

    @Test
    public void testGenerateWithOnePossibleBridgeWord() {
        // "seek new" -> bridge word "out" => "seek out new"
        String inputText = "seek new";
        String expectedText = "seek out new";
        assertEquals(expectedText, generateNewText.generate(graph, inputText));

        // "to strange" -> bridge word "explore" => "to explore strange"
        // to -> explore -> strange
        String inputText2 = "to strange";
        String expectedText2 = "to explore strange";
        assertEquals(expectedText2, generateNewText.generate(graph, inputText2));
    }

    @Test
    public void testGenerateWithMultiplePossibleBridgeWords() {
        // Setup:
        // a -> b1 -> d
        // a -> b2 -> d
        Map<String, Integer> aNeighbors = new HashMap<>();
        aNeighbors.put("b1", 1);
        aNeighbors.put("b2", 1);
        graph.put("a", aNeighbors);

        Map<String, Integer> b1Neighbors = new HashMap<>();
        b1Neighbors.put("d", 1);
        graph.put("b1", b1Neighbors);

        Map<String, Integer> b2Neighbors = new HashMap<>();
        b2Neighbors.put("d", 1);
        graph.put("b2", b2Neighbors);
        graph.put("d", new HashMap<>());


        String inputText = "a d";
        String result = generateNewText.generate(graph, inputText);
        // Result could be "a b1 d" or "a b2 d"
        assertTrue("Result should be 'a b1 d' or 'a b2 d', but was: " + result,
                result.equals("a b1 d") || result.equals("a b2 d"));
    }

    @Test
    public void testGenerateWithShortInputText() {
        String inputText1 = "hello";
        assertEquals("hello", generateNewText.generate(graph, inputText1));

        String inputText2 = ""; // Empty input
        assertEquals("", generateNewText.generate(graph, inputText2));

        String inputText3 = "one"; // Single word
        assertEquals("one", generateNewText.generate(graph, inputText3));
    }

    @Test
    public void testGenerateWithPunctuationAndMixedCase() {
        // "Seek New life."
        // words: "seek", "new", "life"
        // Pair 1: "seek", "new" -> bridge: "out" -> "seek out new"
        // Pair 2: "new", "life" -> no bridge in simple graph (life is direct neighbor)
        // Result: "seek out new life"
        String inputText = "Seek New life.";
        String expectedText = "seek out new life";
        assertEquals(expectedText, generateNewText.generate(graph, inputText));

        // "To explore strange, New civilizations!"
        // to explore strange new civilizations
        // pair "to", "explore" -> no bridge
        // pair "explore", "strange" -> no bridge
        // pair "strange", "new" -> no bridge
        // pair "new", "civilizations" -> no bridge
        String inputText2 = "To explore strange, New civilizations!";
        // Graph has: to->explore, explore->strange, strange->new, new->civilizations
        // No intermediate bridge words here
        String expectedText2 = "to explore strange new civilizations";
        assertEquals(expectedText2, generateNewText.generate(graph, inputText2));
    }

    @Test
    public void testGenerateWord1NotInGraph() {
        String inputText = "unknownword explore";
        String expectedText = "unknownword explore";
        assertEquals(expectedText, generateNewText.generate(graph, inputText));
    }

    @Test
    public void testGenerateWord2NotLinkedByBridge() {
        // "seek life" - "seek" is in graph, "life" is in graph.
        // seek -> out. "out" does not link to "life". So no bridge.
        String inputText = "seek life";
        String expectedText = "seek life";
        assertEquals(expectedText, generateNewText.generate(graph, inputText));
    }

    @Test
    public void testGenerateWithEmptyGraph() {
        graph.clear(); // Make the graph empty
        String inputText = "this is a test";
        String expectedText = "this is a test";
        assertEquals(expectedText, generateNewText.generate(graph, inputText));
    }

    @Test
    public void testGenerateMultipleSegmentsWithAndWithoutBridges() {
        // "to seek new civilizations"
        // Pair 1: "to", "seek" -> no bridge word (seek is direct neighbor)
        // Pair 2: "seek", "new" -> bridge word "out"
        // Pair 3: "new", "civilizations" -> no bridge word (civilizations is direct neighbor)
        // Result: "to seek out new civilizations"
        String inputText = "To seek New civilizations.";
        String expectedText = "to seek out new civilizations";
        assertEquals(expectedText, generateNewText.generate(graph, inputText));
    }
}
