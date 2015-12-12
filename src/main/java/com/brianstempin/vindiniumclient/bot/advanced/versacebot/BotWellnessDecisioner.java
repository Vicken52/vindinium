package com.brianstempin.vindiniumclient.bot.advanced.versacebot;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.BotUtils;
import com.brianstempin.vindiniumclient.bot.advanced.Vertex;
import com.brianstempin.vindiniumclient.dto.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Decides if the bot is "well" (healthy) and acts accordingly.
 *
 * This decisioner will check to make sure the bot is healthy enough to play on and act accordingly.
 *
 * On Maslow's Hierarchy of needs, this one services psychological and safety needs.
 */
public class BotWellnessDecisioner implements Decision<VersaceBot.GameContext, BotMove> {

    private static final Logger logger = LogManager.getLogger(BotWellnessDecisioner.class);

    private final Decision<VersaceBot.GameContext, BotMove> mineDecisioner;
    private final Decision<VersaceBot.GameContext, BotMove> combatDecisioner;
    private final Decision<VersaceBot.GameContext, BotMove> yesDecisioner;
    private final Decision<VersaceBot.GameContext, BotMove> noDecisioner;
    private final Decision<VersaceBot.GameContext, BotMove> suicideDecisioner;

    public BotWellnessDecisioner(Decision<VersaceBot.GameContext, BotMove> mineDecisioner,
                                 Decision<VersaceBot.GameContext, BotMove> combatDecisioner,
                                 Decision<VersaceBot.GameContext, BotMove> yesDecisioner,
                                 Decision<VersaceBot.GameContext, BotMove> noDecisioner,
                                 Decision<VersaceBot.GameContext, BotMove> suicideDecisioner) {
        this.mineDecisioner = mineDecisioner;
        this.combatDecisioner = combatDecisioner;
        this.yesDecisioner = yesDecisioner;
        this.noDecisioner = noDecisioner;
        this.suicideDecisioner = suicideDecisioner;
    }

    @Override
    public BotMove makeDecision(VersaceBot.GameContext context) {

        GameState.Hero me = context.getGameState().getMe();
        Vertex myVertex = context.getGameState().getBoardGraph().get(me.getPos());

        // Do we have money for a pub?
        if(me.getGold() < 2) {
            // We're broke...pretend like we're healthy.
            logger.info("Bot is broke.  Fighting on even if its not healthy.");
            return yesDecisioner.makeDecision(context);
        }

        // Is the bot already next to a pub?  Perhaps its worth a drink
        for(Vertex currentVertex : myVertex.getAdjacentVertices()) {
            if(context.getGameState().getPubs().containsKey(
                    currentVertex.getPosition())) {
                if(me.getLife() < 80) {
                    logger.info("Bot is next to a pub already and could use health.");
                    return BotUtils.directionTowards(me.getPos(), currentVertex.getPosition());
                }

                // Once we find a pub, we don't care about evaluating the rest
                break;
            }
        }

        // Is the bot well?
        if(context.getGameState().getMe().getLife() >= 60 && me.getMineCount() < 3) {
            logger.info("Bot is mining!");
            // UnattendedMineDecisioner
            return mineDecisioner.makeDecision(context);
        }
        else if(context.getGameState().getMe().getLife() >= 60) {
            logger.info("Bot is hunting!");
            // botTargetingDecisioner
            return combatDecisioner.makeDecision(context);
        }
        else if(context.getGameState().getMe().getLife() >= 30) {
            logger.info("Bot is healthy.");
            // enRouteLootingDecisioner
            return yesDecisioner.makeDecision(context);
        }
        else if(context.getGameState().getMe().getMineCount() > 1 &&
                BotUtils.getVersaceHeroesAround(context.getGameState(), context.getDijkstraResultMap(), 2).size() > 0) {
            logger.info("Attempting to suicide.");
            // suicideDecisioner
            return suicideDecisioner.makeDecision(context);
        }
        else {
            logger.info("Bot is damaged.");
            // healDecisioner
            return noDecisioner.makeDecision(context);
        }
    }
}
