package com.brianstempin.vindiniumclient.bot.advanced.versacebot;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.advanced.*;
import com.brianstempin.vindiniumclient.dto.GameState;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * An improvement upon com.brianstempin.vindiniumClient.bot.simple.MurderBot
 *
 * This class uses a built-in static method to perform the path search via Dijkstra and uses a simple version of
 * behavior trees to determine its next action.
 */
public class VersaceBot implements AdvancedBot {

    public static class GameContext {
        private final AdvancedGameState gameState;
        private final Map<GameState.Position, DijkstraResult> dijkstraResultMap;

        public GameContext(AdvancedGameState gameState, Map<GameState.Position, DijkstraResult> dijkstraResultMap) {
            this.gameState = gameState;
            this.dijkstraResultMap = dijkstraResultMap;
        }

        public boolean nearPub(GameState.Position pos)
        {
            for(Pub pub: this.gameState.getPubs().values())
            {
                if(Math.abs(pub.getPosition().getX() - pos.getX()) <= 1 &&
                        Math.abs(pub.getPosition().getY() - pos.getY()) <= 1)
                {
                    return true;
                }
            }

            return false;
        }

        public AdvancedGameState getGameState() {
            return gameState;
        }

        public Map<GameState.Position, DijkstraResult> getDijkstraResultMap() {
            return dijkstraResultMap;
        }
    }

    /**
     * Represents the result of a Dijkstra search for a given position
     */
    public static class DijkstraResult {
        private int distance;
        private GameState.Position previous;

        public DijkstraResult(int distance, GameState.Position previous) {
            this.distance = distance;
            this.previous = previous;
        }

        public int getDistance() {
            return distance;
        }

        public GameState.Position getPrevious() {
            return previous;
        }
    }

    public static synchronized Map<GameState.Position, DijkstraResult> dijkstraSearch(AdvancedGameState gameState) {
        Map<GameState.Position, DijkstraResult> result = new HashMap<>();

        DijkstraResult startingResult = new DijkstraResult(0, null);
        Queue<GameState.Position> queue = new ArrayBlockingQueue<>(gameState.getBoardGraph().size());
        queue.add(gameState.getMe().getPos());
        result.put(gameState.getMe().getPos(), startingResult);

        while(!queue.isEmpty()) {
            GameState.Position currentPosition = queue.poll();
            DijkstraResult currentResult = result.get(currentPosition);
            Vertex currentVertext = gameState.getBoardGraph().get(currentPosition);

            // If there's a bot here, then this vertex goes nowhere
            if(gameState.getHeroesByPosition().containsKey(currentPosition)
                    && !currentPosition.equals(gameState.getMe().getPos()))
                continue;

            int distance = currentResult.getDistance() + 1;

            for(Vertex neighbor : currentVertext.getAdjacentVertices()) {
                DijkstraResult neighborResult = result.get(neighbor.getPosition());
                if(neighborResult == null) {
                    neighborResult = new DijkstraResult(distance, currentPosition);
                    result.put(neighbor.getPosition(), neighborResult);
                    queue.remove(neighbor.getPosition());
                    queue.add(neighbor.getPosition());
                } else if(neighborResult.distance > distance) {
                    DijkstraResult newNeighborResult = new DijkstraResult(distance, currentPosition);
                    result.put(neighbor.getPosition(), newNeighborResult);
                    queue.remove(neighbor.getPosition());
                    queue.add(neighbor.getPosition());
                }
            }
        }

        return result;
    }

    private final Decision<GameContext, BotMove> decisioner;

    public VersaceBot() {

        // Chain decisioners together
        SquatDecisioner squatDecisioner = new SquatDecisioner();
        UnattendedMineDecisioner unattendedMineDecisioner = new UnattendedMineDecisioner(squatDecisioner);
        BotTargetingDecisioner botTargetingDecisioner = new BotTargetingDecisioner(unattendedMineDecisioner);
        EnRouteLootingDecisioner enRouteLootingDecisioner = new EnRouteLootingDecisioner(botTargetingDecisioner);

        HealDecisioner healDecisioner = new HealDecisioner();
        SuicideDecisioner suicideDecisioner = new SuicideDecisioner(healDecisioner);

        BotWellnessDecisioner botWellnessDecisioner = new BotWellnessDecisioner(unattendedMineDecisioner,
                                                                                botTargetingDecisioner,
                                                                                enRouteLootingDecisioner,
                                                                                healDecisioner,
                                                                                suicideDecisioner);

        this.decisioner = new StartDecisioner(squatDecisioner, botWellnessDecisioner);

    }

    @Override
    public BotMove move(AdvancedGameState gameState) {

        Map<GameState.Position, DijkstraResult> dijkstraResultMap = dijkstraSearch(gameState);

        GameContext context = new GameContext(gameState, dijkstraResultMap);
        return this.decisioner.makeDecision(context);

    }

    @Override
    public void setup() {
        // No-op
    }

    @Override
    public void shutdown() {
        // No-op
    }
}
