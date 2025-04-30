import java.io.*;
import java.security.KeyStore;
import java.util.*;

public class Main {
    private static Set<String> allGraphNodes = null;// Store all nodes after graph creation
    private static Map<String, Double> calculatedPageRanks = null;// Cache calculated PageRank values

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        List<String> wordList = parseFile("C:/Users/86178/Desktop/Software Engineering/Easy Test.txt");
        Map<String, Map<String, Integer>> graph = buildGraph(wordList);
        allGraphNodes = getAllNodes(graph);
        System.out.println("图已构建，包含 " + allGraphNodes.size() + " 个节点。");


        //防止节点过多绘图爆内存
        if(allGraphNodes.size()<30)
        {
            System.out.println("是否展示有向图结构？(y/n): ");
            if (scanner.nextLine().equalsIgnoreCase("y")) {
                showDirectedGraph(graph);
            }
            System.out.println("是否导出图形图像文件？(y/n): ");
            if (scanner.nextLine().equalsIgnoreCase("y")) {
                exportToDotFile(graph, "graph.dot");
                generateGraphImage("C:/Code/java/lab1/graph.dot", "graph.png");
            }
        }


        System.out.println("是否查询桥接词？(y/n): ");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            System.out.print("请输入第一个单词 word1: ");
            String word1 = scanner.nextLine();
            System.out.print("请输入第二个单词 word2: ");
            String word2 = scanner.nextLine();

            String result = queryBridgeWords(graph, word1, word2);
            System.out.println(result);
        }


        System.out.println("是否根据 bridge word 生成新文本？(y/n): ");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            System.out.println("请输入新文本：");
            String input = scanner.nextLine();

            String output = generateNewText(graph, input);
            System.out.println("生成的新文本：\n" + output);
        }


        System.out.println("是否计算最短路径？(y/n): ");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            System.out.print("请输入第一个单词 word1 (如果只输入一个单词请直接回车): ");
            String word1 = scanner.nextLine();
            System.out.print("请输入第二个单词 word2: ");
            String word2 = scanner.nextLine();

            String result;
            if (word1.isEmpty()) {
                result = calcShortestPathToAll(graph, word2);
            } else {
                result = calcShortestPath(graph, word1, word2);
            }
            System.out.println(result);
        }



        //计算PR值
        System.out.println("是否计算 PageRank 值？(y/n): ");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            // Calculate all ranks only if not already cached
            if (calculatedPageRanks == null) {
                System.out.println("正在计算所有节点的 PageRank (d=0.85, 100 iterations)...");
                calculatedPageRanks = calculateAllPageRanks(graph, allGraphNodes, 0.85, 100,1e-6);
                System.out.println("PageRank 计算完成。");
            } else {
                System.out.println("使用已缓存的 PageRank 值。");
            }

            System.out.print("请输入要查询 PageRank 的单词 (或输入 'all' 显示所有): ");
            String pageRankWord = scanner.nextLine().toLowerCase();

            if (pageRankWord.equals("all")) {
                // Sort by rank descending for better readability
                List<Map.Entry<String, Double>> sortedRanks = new ArrayList<>(calculatedPageRanks.entrySet());
                sortedRanks.sort(Map.Entry.<String, Double>comparingByValue().reversed());
                System.out.println("\n--- 所有节点的 PageRank (Top 10 或全部) ---");
                int count = 0;
                for(Map.Entry<String, Double> entry : sortedRanks) {
                    System.out.printf("  %s: %.6f\n", entry.getKey(), entry.getValue());
                    count++;
                }
                System.out.println("------------------------------------");

            } else {
                double rank = getPageRank(pageRankWord, calculatedPageRanks, allGraphNodes);
                if (rank < 0) { // Using negative value as error indicator
                    System.out.println("单词 \"" + pageRankWord + "\" 不在图中。");
                } else {
                    System.out.printf("单词 \"%s\" 的 PageRank 值 (d=0.85, 100 iterations): %.6f\n", pageRankWord, rank);
                }
            }
        }


        //随机遍历
        System.out.println("是否开始随机遍历？(y/n): ");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            String walkResult = randomWalk(graph, allGraphNodes, scanner);
            System.out.println("\n--- 随机遍历完成 ---");
            System.out.println("遍历路径: " + walkResult);
            // File writing confirmation happens inside randomWalk
            System.out.println("--------------------");
        }

        System.out.println("\n程序结束。");
        scanner.close(); // Close the scanner at the very end

    }

    //Helper Function to Get All Nodes
    public static Set<String> getAllNodes(Map<String, Map<String, Integer>> graph) {
        Set<String> nodes = new HashSet<>();
        if (graph == null) return nodes; // Handle null graph

        nodes.addAll(graph.keySet()); // Add all source nodes
        for (Map<String, Integer> neighborsMap : graph.values()) {
            if (neighborsMap != null) {
                nodes.addAll(neighborsMap.keySet()); // Add all target nodes
            }
        }
        return nodes;
    }

    public static List<String> parseFile(String filePath) {
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String delimiters = "[^a-zA-Z]+";//正则：[^a-zA-Z]：匹配不是大小写英文字母的任何一个字符。+：表示一个或多个这样的非字母字符。

            while ((line = reader.readLine()) != null) {
                line = line.toLowerCase(); //转成小写
                String[] tokens = line.split(delimiters);//按照分隔符拆成单词
                for (String word : tokens) {
                    if (!word.isEmpty()) {
                        words.add(word);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件失败：" + e.getMessage());
        }

        return words;
    }

    public static Map<String, Map<String, Integer>> buildGraph(List<String> words) {
        Map<String, Map<String, Integer>> graph = new HashMap<>();

        for (int i = 0; i < words.size() - 1; i++) {
            String from = words.get(i);
            String to = words.get(i + 1);

            graph.putIfAbsent(from, new HashMap<>());
            Map<String, Integer> neighbors = graph.get(from);//在外层map中，from是key，返回的是内层map
            neighbors.put(to, neighbors.getOrDefault(to, 0) + 1);//更新neighbor中to的出现次数
        }

        return graph;
    }

    public static void showDirectedGraph(Map<String, Map<String, Integer>> graph) {
        System.out.println("\n======== 有向图结构 ========");
        for (String from : graph.keySet()) {
            Map<String, Integer> neighbors = graph.get(from);
            for (String to : neighbors.keySet()) {
                int weight = neighbors.get(to);
                System.out.printf("%s -> %s [权重: %d]\n", from, to, weight);
            }
        }
    }

    // 写入 DOT 文件
    public static void exportToDotFile(Map<String, Map<String, Integer>> graph, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("digraph G {");
            for (String from : graph.keySet()) {
                for (Map.Entry<String, Integer> entry : graph.get(from).entrySet()) {
                    String to = entry.getKey();
                    int weight = entry.getValue();
                    writer.printf("    \"%s\" -> \"%s\" [label=\"%d\"];\n", from, to, weight);
                }
            }
            writer.println("}");
            System.out.println("DOT 文件已保存：" + filename);
        } catch (IOException e) {
            System.err.println("写入 DOT 文件失败：" + e.getMessage());
        }
    }

    // 调用 Graphviz 生成图像
    public static void generateGraphImage(String dotFilePath, String outputImagePath) {
        try {
            String command = String.format("dot -Tpng %s -o %s", dotFilePath, outputImagePath);
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            System.out.println("图像文件已生成：" + outputImagePath);
        } catch (IOException | InterruptedException e) {
            System.err.println("图像生成失败：" + e.getMessage());
        }
    }

    // 查询桥接词
    public static String queryBridgeWords(Map<String, Map<String, Integer>> graph, String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        if (allGraphNodes == null) { // Safety check
            allGraphNodes = getAllNodes(graph); // Recalculate if null somehow
        }
        boolean word1Exists = allGraphNodes.contains(word1);
        boolean word2Exists = allGraphNodes.contains(word2);

        if (!word1Exists || !word2Exists) {
            String missing = "";
            if (!word1Exists) missing += "\"" + word1 + "\" ";
            if (!word2Exists) missing += "\"" + word2 + "\"";
            return "No " + missing.trim() + " in the graph!";
        }

        List<String> bridgeWords = findBridgeWordsEfficiently(graph, word1, word2);

        // Format the output based on the results
        if (bridgeWords.isEmpty()) {
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        } else if (bridgeWords.size() == 1) {
            return "The bridge word from \"" + word1 + "\" to \"" + word2 + "\" is: " + bridgeWords.get(0);
        } else {
            // Sort for consistent output (optional)
            // Collections.sort(bridgeWords);
            return "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" are: " + String.join(", ", bridgeWords) + ".";
        }
    }

    /**
     * Finds all bridge words between word1 and word2 efficiently.
     * A bridge word 'b' exists if there are edges word1 -> b and b -> word2.
     * Assumes word1 and word2 are already lowercased.
     *
     * @param graph The graph structure.
     * @param word1 The starting word (lowercase).
     * @param word2 The ending word (lowercase).
     * @return A list of bridge words found. Returns an empty list if none are found or if word1 has no outgoing edges.
     */
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


    //根据bridge生成新文本
    public static String generateNewText(Map<String, Map<String, Integer>> graph, String inputText) {
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

    // 计算两个词之间的最短路径
    public static String calcShortestPath(Map<String, Map<String, Integer>> graph, String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        //获取所有节点
        Set<String> allNodes = new HashSet<>(graph.keySet());
        for (Map<String, Integer> neighborsMap : graph.values()) {
            allNodes.addAll(neighborsMap.keySet());
        }

        //检查输入词是否在图中
        if (!allNodes.contains(word1) || !allNodes.contains(word2)) {
            String missing = "";
            if (!allNodes.contains(word1)) missing += "\"" + word1 + "\" ";
            if (!allNodes.contains(word2)) missing += "\"" + word2 + "\"";
            return "Word(s) not found in graph: " + missing.trim();
        }

        // Dijkstra algorithm
        Map<String, Integer> distances = new HashMap<>();
        Map<String, List<String>> predecessors = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));

        // Initialize for all nodes
        for (String node : allNodes) {
            distances.put(node, Integer.MAX_VALUE);//所有节点距离设为无穷
            predecessors.put(node, new ArrayList<>()); // Ensure every node has an initialized list
        }
        distances.put(word1, 0);
        queue.add(new Node(word1, 0));

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            String currentNode = current.word;

            // If we pull a node whose recorded distance is worse than what's already in `distances`, skip it.
            if (current.distance > distances.get(currentNode)) {
                continue;
            }
            // Optimization: If current node's distance is already >= shortest path found to target, skip.
            if (distances.get(word2) != Integer.MAX_VALUE && current.distance >= distances.get(word2)) {
                continue;
            }

            // Use getOrDefault for nodes without outgoing edges
            Map<String, Integer> neighbors = graph.getOrDefault(currentNode, Collections.emptyMap()); // Use emptyMap
            for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                String neighbor = entry.getKey();
                int weight = entry.getValue();

                // Should exist due to initialization
                if (!distances.containsKey(neighbor)) continue;

                // Handle potential Integer.MAX_VALUE overflow
                int currentDist = distances.get(currentNode);
                int newDist;
                if (currentDist == Integer.MAX_VALUE) {
                    newDist = Integer.MAX_VALUE;
                } else {
                    newDist = currentDist + weight;
                }

                int neighborDist = distances.get(neighbor);

                // Optimization: Don't explore paths already longer than the known shortest path to target
                if (distances.get(word2) != Integer.MAX_VALUE && newDist > distances.get(word2)) {
                    continue;
                }

                if (newDist < neighborDist) {
                    distances.put(neighbor, newDist);
                    List<String> preds = predecessors.get(neighbor);
                    preds.clear(); // Safe now
                    preds.add(currentNode);
                    queue.add(new Node(neighbor, newDist));
                } else if (newDist == neighborDist && neighborDist != Integer.MAX_VALUE) { // Avoid MAX_VALUE comparison issue
                    List<String> preds = predecessors.get(neighbor);
                    if (preds != null) { // Safety
                        preds.add(currentNode);
                    }
                }
            }
        }

        // If unreachable
        if (distances.get(word2) == Integer.MAX_VALUE) {
            return "No path from \"" + word1 + "\" to \"" + word2 + "\"!";
        }

        // Build all shortest paths
        List<List<String>> allPaths = new ArrayList<>();
        buildAllPaths(word1, word2, predecessors, new LinkedList<>(), allPaths);

        // Prepare result string
        StringBuilder result = new StringBuilder();
        int shortestLength = distances.get(word2); // Get the calculated shortest distance
        result.append("Shortest path(s) from \"").append(word1).append("\" to \"").append(word2)
                .append("\" (length: ").append(shortestLength).append("):\n"); // Use calculated length

        if (allPaths.isEmpty()){
            result.append("Path reconstruction failed or target is the source itself.\n"); // More informative
        } else {
            for (List<String> path : allPaths) {
                result.append(String.join(" -> ", path)).append("\n");
            }
        }

        return result.toString().trim(); // Trim trailing newline if any
    }

    // 计算一个单词到所有其他单词的最短路径
    public static String calcShortestPathToAll(Map<String, Map<String, Integer>> graph, String word) {
        word = word.toLowerCase();

        // --- Start: Added code to find all nodes ---
        Set<String> allNodes = new HashSet<>(graph.keySet());
        for (Map<String, Integer> neighborsMap : graph.values()) {
            allNodes.addAll(neighborsMap.keySet());
        }
        // --- End: Added code to find all nodes ---


        // Check if the starting word exists *after* identifying all nodes
        if (!allNodes.contains(word)) { // Modified check
            return "No \"" + word + "\" in the graph!";
        }

        // 使用Dijkstra算法计算最短路径
        Map<String, Integer> distances = new HashMap<>();
        Map<String, List<String>> predecessors = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));

        // 初始化 - Use allNodes instead of graph.keySet()
        for (String node : allNodes) { // Modified loop
            distances.put(node, Integer.MAX_VALUE);
            predecessors.put(node, new ArrayList<>()); // Ensure every node has an initialized list
        }
        distances.put(word, 0);
        queue.add(new Node(word, 0));

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            String currentNode = current.word;

            if (current.distance > distances.get(currentNode)) {
                continue; // Already found a shorter path to this node
            }

            // Use getOrDefault in case a node exists in allNodes but not in graph.keySet()
            // (meaning it has no outgoing edges)
            Map<String, Integer> neighbors = graph.getOrDefault(currentNode, Collections.emptyMap()); // Use emptyMap for safety
            for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                String neighbor = entry.getKey();
                int weight = entry.getValue();

                // Check if neighbor exists in distances (it should due to allNodes initialization)
                if (!distances.containsKey(neighbor)) {
                    // This case should theoretically not happen anymore with the allNodes initialization
                    System.err.println("Warning: Neighbor " + neighbor + " not found in distances map. Graph inconsistency?");
                    continue;
                }

                // Important: Handle potential Integer.MAX_VALUE overflow
                int currentDist = distances.get(currentNode);
                int newDist;
                if (currentDist == Integer.MAX_VALUE) {
                    newDist = Integer.MAX_VALUE; // Avoid overflow
                } else {
                    newDist = currentDist + weight;
                }

                int neighborDist = distances.get(neighbor); // Get current distance to neighbor

                if (newDist < neighborDist) {
                    distances.put(neighbor, newDist);
                    // predecessors.get(neighbor) should not be null now
                    List<String> preds = predecessors.get(neighbor);
                    preds.clear(); // Now safe
                    preds.add(currentNode);
                    queue.add(new Node(neighbor, newDist));
                } else if (newDist == neighborDist && neighborDist != Integer.MAX_VALUE) { // Avoid adding predecessors for unreachable nodes being compared at MAX_VALUE
                    // predecessors.get(neighbor) should not be null now
                    List<String> preds = predecessors.get(neighbor);
                    if (preds != null) { // Add extra safety check, though it shouldn't be needed
                        preds.add(currentNode);
                    } else {
                        System.err.println("Warning: Predecessor list for " + neighbor + " is unexpectedly null.");
                    }
                }
            }
        }

        // 准备结果字符串
        StringBuilder result = new StringBuilder();
        result.append("Shortest paths from \"").append(word).append("\":\n");

        // Iterate through all known nodes
        for (String target : allNodes) { // Iterate over allNodes
            if (target.equals(word)) continue;

            if (distances.get(target) == Integer.MAX_VALUE) {
                result.append("No path to \"").append(target).append("\"\n");
                continue;
            }

            // 构建所有最短路径
            List<List<String>> allPaths = new ArrayList<>();
            buildAllPaths(word, target, predecessors, new LinkedList<>(), allPaths);

            if (allPaths.isEmpty() && !target.equals(word)) {
                if (distances.get(target) != Integer.MAX_VALUE) {
                    result.append("To \"").append(target).append("\" (length: ").append(distances.get(target)).append("): Path reconstruction failed or complex case.\n");
                } else {
                    result.append("No path to \"").append(target).append("\"\n"); // Redundant but safe
                }
            } else {
                result.append("To \"").append(target).append("\" (length: ").append(distances.get(target)).append("): ");
                for (int i = 0; i < allPaths.size(); i++) {
                    if (i > 0) result.append(" OR ");
                    result.append(String.join(" -> ", allPaths.get(i)));
                }
                result.append("\n");
            }
        }

        return result.toString();
    }

    // 辅助类用于优先队列
    private static class Node {
        String word;
        int distance;

        public Node(String word, int distance) {
            this.word = word;
            this.distance = distance;
        }
    }

    // 递归构建所有最短路径
    private static void buildAllPaths(String start, String current,
                                      Map<String, List<String>> predecessors,
                                      LinkedList<String> currentPath,
                                      List<List<String>> allPaths) {
        currentPath.addFirst(current);

        if (current.equals(start)) {
            allPaths.add(new ArrayList<>(currentPath));
        } else {
            for (String pred : predecessors.get(current)) {
                buildAllPaths(start, pred, predecessors, currentPath, allPaths);
            }
        }

        currentPath.removeFirst();
    }


    // --- PageRank Calculation ---
    public static Map<String, Double> calculateAllPageRanks(
            Map<String, Map<String, Integer>> graph,
            Set<String> allNodes,
            double d,
            int maxIterations,
            double epsilon) {

        Map<String, Double> pageRank = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();
        Map<String, List<String>> inLinks = new HashMap<>();

        int N = allNodes.size();
        if (N == 0) return pageRank;

        // Step 1: 初始化 PageRank、inLinks、出度
        for (String node : allNodes) {
            pageRank.put(node, 1.0 / N);
            inLinks.put(node, new ArrayList<>());
            outDegree.put(node, 0);
        }

        // Step 2: 构建出度和反向链接表
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String source = entry.getKey();
            Map<String, Integer> neighbors = entry.getValue();
            int currentOutDegree = (neighbors != null) ? neighbors.size() : 0;
            outDegree.put(source, currentOutDegree);

            if (neighbors != null) {
                for (String target : neighbors.keySet()) {
                    if (inLinks.containsKey(target)) {
                        inLinks.get(target).add(source);
                    }
                }
            }
        }

        // Step 3: PageRank 迭代
        double baseRankComponent = (1.0 - d) / N;
        Map<String, Double> nextPageRank = new HashMap<>();

        for (int iter = 0; iter < maxIterations; iter++) {
            double totalSinkPR = 0.0;

            // 计算所有 sink 节点的 PageRank 总和
            for (String node : allNodes) {
                if (outDegree.getOrDefault(node, 0) == 0) {
                    totalSinkPR += pageRank.getOrDefault(node, 0.0);
                }
            }

            // 计算每个节点的新 PageRank
            for (String p : allNodes) {
                double sumFromInLinks = 0.0;
                List<String> incomingNodes = inLinks.getOrDefault(p, Collections.emptyList());

                for (String q : incomingNodes) {
                    int qOutDegree = outDegree.getOrDefault(q, 0);
                    if (qOutDegree > 0) {
                        sumFromInLinks += pageRank.get(q) / qOutDegree;
                    }
                }

                double sinkContribution = d * totalSinkPR / N;
                nextPageRank.put(p, baseRankComponent + d * sumFromInLinks + sinkContribution);
            }

            // 判断是否收敛
            boolean converged = true;
            for (String node : allNodes) {
                double oldRank = pageRank.getOrDefault(node, 0.0);
                double newRank = nextPageRank.getOrDefault(node, 0.0);
                if (Math.abs(oldRank - newRank) > epsilon) {
                    converged = false;
                    break;
                }
            }

            // 更新 pageRank 并检查收敛
            pageRank = new HashMap<>(nextPageRank);
            if (converged) {
                break;
            }
        }

        return pageRank;
    }


    /**
     * Retrieves the pre-calculated PageRank for a specific word.
     *
     * @param word The word (node name) to query (case-insensitive).
     * @param calculatedRanks The map returned by calculateAllPageRanks.
     * @param allNodes Set of all unique node names in the graph.
     * @return The PageRank value, or -1.0 if the word is not in the graph, or -2.0 if ranks haven't been calculated.
     */
    public static double getPageRank(String word, Map<String, Double> calculatedRanks, Set<String> allNodes) {
        String lowerCaseWord = word.toLowerCase(); // Ensure case-insensitivity
        if (!allNodes.contains(lowerCaseWord)) {
            return -1.0; // Word not in the graph
        }
        if (calculatedRanks == null) {
            System.err.println("错误: PageRank 尚未计算。请先运行 PageRank 计算功能。");
            return -2.0; // Ranks not calculated
        }
        // Return the rank, default to 0.0 if somehow missing after calculation (shouldn't happen)
        return calculatedRanks.getOrDefault(lowerCaseWord, 0.0);
    }

    // --- Random Walk ---
    /**
     * Performs a random walk starting from a random node in the graph.
     * Stops when a sink node is reached, an edge is traversed twice, or the user interrupts.
     * Outputs the path to the console and saves it to a file.
     *
     * @param graph The graph structure.
     * @param allNodes Set of all node names in the graph.
     * @param scanner Scanner object to read user input for interruption.
     * @return A string representing the traversed path (e.g., "node1 -> node2 -> node3").
     */
    public static String randomWalk(Map<String, Map<String, Integer>> graph, Set<String> allNodes, Scanner scanner) {
        if (allNodes == null || allNodes.isEmpty()) {
            return "图为空，无法进行随机遍历。";
        }

        List<String> nodesList = new ArrayList<>(allNodes); // Convert set to list for random access
        Random random = new Random();

        // 1. Select a random starting node
        String currentNode = nodesList.get(random.nextInt(nodesList.size()));

        List<String> pathNodes = new ArrayList<>(); // Stores the sequence of nodes visited
        Set<String> visitedEdges = new HashSet<>(); // Stores visited edges "source->target"
        pathNodes.add(currentNode); // Add the starting node to the path

        System.out.println("\n--- 开始随机遍历 ---");
        System.out.println("起始节点: " + currentNode);
        System.out.println("在每一步后，按 Enter 继续，或输入 's'/'stop' 停止。");

        // 2. Loop for traversal
        while (true) {
            System.out.print("当前节点: " + currentNode + ". 继续? (Enter/s/stop): ");
            String userInput = scanner.nextLine().trim().toLowerCase();
            if (userInput.equals("s") || userInput.equals("stop")) {
                System.out.println("用户请求停止遍历。");
                break; // Exit loop if user stops
            }

            // 3. Check for outgoing edges
            Map<String, Integer> neighbors = graph.get(currentNode); // Use get, check for null later

            // Check if the current node is a sink (no outgoing edges listed in the graph map)
            if (neighbors == null || neighbors.isEmpty()) {
                System.out.println("节点 '" + currentNode + "' 没有出边 (sink node)。遍历停止。");
                break; // Exit loop if sink node is reached
            }

            // 4. Select a random next node from neighbors
            List<String> neighborKeys = new ArrayList<>(neighbors.keySet());
            String nextNode = neighborKeys.get(random.nextInt(neighborKeys.size()));
            String edge = currentNode + "->" + nextNode; // Represent the edge

            // 5. Check if the edge has been visited before
            if (visitedEdges.contains(edge)) {
                System.out.println("遇到重复边: " + edge + "。遍历停止。");
                pathNodes.add(nextNode); // Add the final node leading to the repeated edge
                System.out.println("  --> " + nextNode); // Show the last step
                break; // Exit loop if edge is repeated
            }

            // 6. Mark edge as visited, add next node to path, and update current node
            visitedEdges.add(edge);
            pathNodes.add(nextNode);
            System.out.println("  --> " + nextNode + " (边: " + edge + ")"); // Show step
            currentNode = nextNode; // Move to the next node
        }

        // 7. Format the result path string
        String resultPath = String.join(" -> ", pathNodes);

        // 8. Write the result path to a file
        String filename = "random_walk_result.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(resultPath);
            System.out.println("遍历路径已成功写入文件: " + filename);
        } catch (IOException e) {
            System.err.println("错误：写入随机遍历结果文件 '" + filename + "' 失败: " + e.getMessage());
        }

        return resultPath; // Return the path string
    }
}