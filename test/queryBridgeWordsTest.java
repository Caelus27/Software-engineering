import org.junit.Test;
import org.junit.Before; // 如果需要重复设置
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class queryBridgeWordsTest {

    // 创建图的辅助方法
    private Map<String, Map<String, Integer>> createGraph(Object... data) {
        Map<String, Map<String, Integer>> graph = new HashMap<>();
        for (int i = 0; i < data.length; i += 3) {
            String source = (String) data[i];
            String target = (String) data[i + 1];
            Integer weight = (Integer) data[i + 2];
            graph.computeIfAbsent(source, k -> new HashMap<>()).put(target, weight);
        }
        return graph;
    }

    // 比较列表的辅助方法（忽略顺序）
    private void assertListEqualsIgnoringOrder(List<String> expected, List<String> actual) {
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        assertTrue(actual.containsAll(expected));
        assertTrue(expected.containsAll(actual));
    }

    @Test
    public void testWord1NotInGraph() { // 测试用例1（路径1）
        Map<String, Map<String, Integer>> graph = createGraph(); // 空图
        List<String> result = queryBridgeWords.findBridgeWordsEfficiently(graph, "seek", "new");
        assertTrue("当word1不在图中时应返回空列表", result.isEmpty());
    }

    @Test
    public void testWord1InGraphButNoNeighbors() { // 测试用例2（路径2）
        Map<String, Map<String, Integer>> graph = new HashMap<>();
        graph.put("seek", new HashMap<>()); // seek存在但没有邻居
        List<String> result = queryBridgeWords.findBridgeWordsEfficiently(graph, "seek", "new");
        assertTrue("当word1没有邻居时应返回空列表", result.isEmpty());
    }

    @Test
    public void testPotentialBridgeNotInGraphAsSource() { // 测试用例3（路径3）
        // explore -> to (1)，但'to'不作为源节点存在
        Map<String, Map<String, Integer>> graph = createGraph("explore", "to", 1);
        List<String> result = queryBridgeWords.findBridgeWordsEfficiently(graph, "explore", "strange");
        assertTrue("当潜在桥节点不是源节点时应返回空列表", result.isEmpty());
    }

    @Test
    public void testPotentialBridgeNotLinkedToWord2() { // 测试用例4（路径4）
        // to -> seek (1), seek -> out (1). 查找从"to"到"new"的桥。"seek"是潜在桥节点
        Map<String, Map<String, Integer>> graph = createGraph(
                "to", "seek", 1,
                "seek", "out", 1
        );
        List<String> result = queryBridgeWords.findBridgeWordsEfficiently(graph, "to", "new");
        assertTrue("当潜在桥节点不连接到word2时应返回空列表", result.isEmpty());
    }

    @Test
    public void testSingleBridgeWordExists() { // 测试用例5（路径5）
        // explore -> strange (1), strange -> new (1). 桥节点: "strange"
        Map<String, Map<String, Integer>> graph = createGraph(
                "explore", "strange", 1,
                "strange", "new", 1
        );
        List<String> expected = Arrays.asList("strange");
        List<String> actual = queryBridgeWords.findBridgeWordsEfficiently(graph, "explore", "new");
        assertListEqualsIgnoringOrder(expected, actual);
    }

}