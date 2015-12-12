package com.brianstempin.vindiniumclient.bot.advanced.versacebot;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.BotUtils;
import com.brianstempin.vindiniumclient.bot.advanced.Mine;
import com.brianstempin.vindiniumclient.bot.advanced.Vertex;
import com.brianstempin.vindiniumclient.dto.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by Vicken on 12/11/2015.
 */
public class SuicideDecisioner implements Decision<VersaceBot.GameContext, BotMove> {
    private final static Logger logger = LogManager.getLogger(SuicideDecisioner.class);

    private final Decision<VersaceBot.GameContext, BotMove> noDecisioner;

    public SuicideDecisioner(Decision<VersaceBot.GameContext, BotMove> noDecisioner) {
        this.noDecisioner = noDecisioner;
    }

    @Override
    public BotMove makeDecision(VersaceBot.GameContext context) {
        GameState.Position myPosition = context.getGameState().getMe().getPos();
        Map<GameState.Position, Vertex> boardGraph = context.getGameState().getBoardGraph();

        for(Vertex currentVertex : boardGraph.get(myPosition).getAdjacentVertices()) {
            Mine mine = context.getGameState().getMines().get(currentVertex.getPosition());
            if(mine != null && (mine.getOwner() == null
                    || mine.getOwner().getId() != context.getGameState().getMe().getId())) {
                logger.info("Going toward abandoned mine to suicide.");
                return BotUtils.directionTowards(myPosition, mine.getPosition());
            }
        }

        logger.info("No opportunistic mines exist.");
        // healDecisioner
        return noDecisioner.makeDecision(context);
    }
}
