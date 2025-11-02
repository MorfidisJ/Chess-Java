package chess.ai;

import chess.Move;
import chess.PieceColor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Tracks and analyzes performance of different AI implementations.
 */
public class AIPerformanceTracker {
    private final Map<String, AIPerformanceStats> stats = new HashMap<>();
    private final Map<String, Long> moveStartTimes = new HashMap<>();
    
    public void startMove(String aiName) {
        moveStartTimes.put(aiName, System.currentTimeMillis());
        stats.putIfAbsent(aiName, new AIPerformanceStats(aiName));
    }
    
    public void recordMove(String aiName, Move move, int depth, long nodesEvaluated) {
        long moveTime = System.currentTimeMillis() - moveStartTimes.getOrDefault(aiName, System.currentTimeMillis());
        AIPerformanceStats aiStats = stats.get(aiName);
        if (aiStats != null) {
            aiStats.recordMove(moveTime, depth, nodesEvaluated);
        }
    }
    
    public void recordGameResult(String winnerAI, String loserAI, int moveCount) {
        if (winnerAI != null) {
            AIPerformanceStats winnerStats = stats.get(winnerAI);
            if (winnerStats != null) winnerStats.recordWin(moveCount);
        }
        if (loserAI != null) {
            AIPerformanceStats loserStats = stats.get(loserAI);
            if (loserStats != null) loserStats.recordLoss(moveCount);
        }
    }
    
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("AI Performance Summary\n");
        sb.append("Generated at: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        sb.append("-".repeat(80)).append("\n");
        
        stats.values().stream()
            .sorted(Comparator.comparingDouble(AIPerformanceStats::getWinRate).reversed())
            .forEach(stat -> sb.append(stat).append("\n"));
            
        return sb.toString();
    }
    
    /**
     * Saves the performance summary to a file in the 'stats' directory.
     * The filename will be in the format: ai_performance_YYYYMMDD_HHmmss.txt
     * 
     * @return The path to the generated file, or null if saving failed
     */
    public String saveSummaryToFile() {
        try {
            // Create stats directory if it doesn't exist
            Path statsDir = Paths.get("stats");
            if (!Files.exists(statsDir)) {
                Files.createDirectories(statsDir);
            }
            
            // Generate filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = String.format("ai_performance_%s.txt", timestamp);
            Path filePath = statsDir.resolve(filename);
            
            // Write the summary to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
                writer.write(getSummary());
                writer.newLine();
                writer.write("Detailed Move Times (ms):");
                writer.newLine();
                
                // Add detailed move times for each AI
                for (Map.Entry<String, AIPerformanceStats> entry : stats.entrySet()) {
                    AIPerformanceStats stat = entry.getValue();
                    if (!stat.moveTimes.isEmpty()) {
                        writer.write(String.format("%s move times: %s\n", 
                            stat.aiName, 
                            stat.moveTimes.toString()));
                    }
                }
            }
            
            return filePath.toString();
        } catch (IOException e) {
            System.err.println("Error saving performance summary: " + e.getMessage());
            return null;
        }
    }
    
    public void reset() {
        stats.clear();
        moveStartTimes.clear();
    }
    
    public static class AIPerformanceStats {
        private final String aiName;
        private int gamesPlayed = 0;
        private int wins = 0;
        private int losses = 0;
        private long totalMoveTimeMs = 0;
        private long totalNodesEvaluated = 0;
        private int totalMoves = 0;
        private int maxDepthReached = 0;
        public final List<Long> moveTimes = new ArrayList<>();
        
        public AIPerformanceStats(String aiName) {
            this.aiName = aiName;
        }
        
        public void recordMove(long moveTimeMs, int depth, long nodesEvaluated) {
            totalMoveTimeMs += moveTimeMs;
            totalNodesEvaluated += nodesEvaluated;
            totalMoves++;
            maxDepthReached = Math.max(maxDepthReached, depth);
            moveTimes.add(moveTimeMs);
        }
        
        public void recordWin(int moveCount) {
            gamesPlayed++;
            wins++;
        }
        
        public void recordLoss(int moveCount) {
            gamesPlayed++;
            losses++;
        }
        
        public double getWinRate() {
            return gamesPlayed > 0 ? (wins * 100.0) / gamesPlayed : 0;
        }
        
        public double getAverageMoveTime() {
            return totalMoves > 0 ? (double) totalMoveTimeMs / totalMoves : 0;
        }
        
        public double getAverageNodesPerMove() {
            return totalMoves > 0 ? (double) totalNodesEvaluated / totalMoves : 0;
        }
        
        @Override
        public String toString() {
            return String.format("%-20s | Games: %3d | Win: %5.1f%% | " +
                               "Avg Move: %4.0fms | Nodes/move: %,.0f | Max Depth: %d",
                    aiName, gamesPlayed, getWinRate(),
                    getAverageMoveTime(), getAverageNodesPerMove(), maxDepthReached);
        }
    }
}
