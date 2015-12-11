package com.brianstempin.vindiniumclient.bot.advanced.versace;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.advanced.Vertex;
import com.brianstempin.vindiniumclient.dto.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Decides if a battle is winnable and what to do about it.
 *
 * This decisioner will try to predict a battle's outcome and will decide whether or not to keep fighting.
 *
 * On the Maslow Hierarchy, this falls under safety.
 */
public class CombatOutcomeDecisioner implements Decision<VersaceBot.GameContext, BotMove> {

    private static final Logger logger = LogManager.getLogger(CombatOutcomeDecisioner.class);

    private final Decision<VersaceBot.GameContext, BotMove> winningDecisioner;
    private final Decision<VersaceBot.GameContext, BotMove> losingDecisioner;

    public CombatOutcomeDecisioner(Decision<VersaceBot.GameContext, BotMove> winningDecisioner,
                                   Decision<VersaceBot.GameContext, BotMove> losingDecisioner) {
        this.winningDecisioner = winningDecisioner;
        this.losingDecisioner = losingDecisioner;
    }

    @Override
    public BotMove makeDecision(VersaceBot.GameContext context) {
        // Who am I fighting?
        GameState.Hero opponent = null;
        GameState.Hero me = context.getGameState().getMe();
        Vertex myVertext = context.getGameState().getBoardGraph().get(me.getPos());
        for(Vertex vertex : myVertext.getAdjacentVertices()) {
            if(context.getGameState().getHeroesByPosition().containsKey(vertex.getPosition())) {
                opponent = context.getGameState().getHeroesByPosition().get(vertex.getPosition());
                break;
            }
        }

        // If we have more health than them or they only can take one more hit
        if(opponent.getLife() <= 20 || me.getLife() >= 30) {
            logger.info("Bot will most likely win the current battle.");
            return winningDecisioner.makeDecision(context);
        }  else {
            logger.info("Bot will most likely lose the current battle.");
            return losingDecisioner.makeDecision(context);
        }
    }
}
