package com.brianstempin.vindiniumclient.bot.advanced.versace;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.BotUtils;
import com.brianstempin.vindiniumclient.bot.advanced.Pub;
import com.brianstempin.vindiniumclient.dto.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Decides the best way to get healed.
 *
 * This decisioner will do its best to steer the bot towards a tavern without confrontation.
 *
 * On the Maslow Hierarchy, this falls under safety.
 */
public class HealDecisioner implements Decision<VersaceBot.GameContext, BotMove> {

    private static final Logger logger = LogManager.getLogger(HealDecisioner.class);

    @Override
    public BotMove makeDecision(VersaceBot.GameContext context) {
        logger.info("Need to heal; running to nearest pub.");

        Map<GameState.Position, VersaceBot.DijkstraResult> dijkstraResultMap = context.getDijkstraResultMap();

        // Run to the nearest pub
        Pub nearestPub = null;
        VersaceBot.DijkstraResult nearestPubDijkstraResult = null;
        for(Pub pub : context.getGameState().getPubs().values()) {
            VersaceBot.DijkstraResult dijkstraToPub = dijkstraResultMap.get(pub.getPosition());
            if(dijkstraToPub != null) {
                if(nearestPub == null || nearestPubDijkstraResult.getDistance() >
                    dijkstraToPub.getDistance()) {
                    nearestPub = pub;
                    nearestPubDijkstraResult = dijkstraResultMap.get(pub.getPosition());
                }
            }
        }

        if(nearestPub == null)
            return BotMove.STAY;

        // TODO How do we know that we're not walking too close to a foe?
        GameState.Position nextMove = nearestPub.getPosition();
        while(nearestPubDijkstraResult.getDistance() > 1) {
            nextMove = nearestPubDijkstraResult.getPrevious();
            nearestPubDijkstraResult = dijkstraResultMap.get(nextMove);
        }

        return BotUtils.directionTowards(nearestPubDijkstraResult.getPrevious(), nextMove);
    }
}
