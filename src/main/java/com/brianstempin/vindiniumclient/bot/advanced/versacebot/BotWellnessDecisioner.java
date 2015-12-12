package com.brianstempin.vindiniumclient.bot.advanced.versacebot;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.BotUtils;
import com.brianstempin.vindiniumclient.bot.advanced.Vertex;
import com.brianstempin.vindiniumclient.dto.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

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
                if(me.getLife() < 60 && me.getMineCount() > 1) {
                    logger.info("Bot is next to a pub already and could use health.");
                    return BotUtils.directionTowards(me.getPos(), currentVertex.getPosition());
                }

                // Once we find a pub, we don't care about evaluating the rest
                break;
            }
        }

//        if(me.getMineCount() == 0)
//        {
//            for(GameState.Hero hero : context.getGameState().getHeroesById().values())
//            {
//                if(hero.getId() != me.getId() &&
//                        hero.getMineCount() >= ((3 * context.getGameState().getMines().size()) / 8) &&
//                        hero.getMineCount() >= 3 && !context.nearPub(hero.getPos()))
//                {
//                    VersaceBot.DijkstraResult currentDijkstraResult =
//                            context.getDijkstraResultMap().get(hero.getPos());
//                    GameState.Position nextPosition = hero.getPos();
//
//                    while(null != currentDijkstraResult && currentDijkstraResult.getDistance() > 1) {
//                        nextPosition = currentDijkstraResult.getPrevious();
//                        currentDijkstraResult = context.getDijkstraResultMap().get(nextPosition);
//                    }
//
//                    logger.info("Going after winning bot!");
//                    assert currentDijkstraResult != null;
//                    return BotUtils.directionTowards(currentDijkstraResult.getPrevious(), nextPosition);
//                }
//            }
//        }

        Set<GameState.Position> positionSet = context.getGameState().getHeroesByPosition().keySet();
        GameState.Position closest = null;

        for(GameState.Position position : positionSet)
        {
            if(closest == null ||
                    (Math.abs(context.getGameState().getMe().getPos().getX() - position.getX()) + Math.abs(context.getGameState().getMe().getPos().getY() - position.getY()))
                            < (Math.abs(context.getGameState().getMe().getPos().getX() - closest.getX()) + Math.abs(context.getGameState().getMe().getPos().getY() - closest.getY())))
            {
                closest = position;
            }
        }


        if((context.getGameState().getMe().getLife() >= 70 && (me.getMineCount() < 3 ||
                me.getMineCount() <= (context.getGameState().getMines().size()) / 4)) ||
                me.getMineCount() == 0) {
            logger.info("Bot is mining!");
            // UnattendedMineDecisioner
            return mineDecisioner.makeDecision(context);
        }
        // Is the bot well?
        else if (closest != null && !me.getPos().equals(closest) &&
                (Math.abs(context.getGameState().getMe().getPos().getX() - closest.getX()) + Math.abs(context.getGameState().getMe().getPos().getY() - closest.getY())) <= 2)
        {
            GameState.Position nextMove = closest;
            VersaceBot.DijkstraResult closestTargetDijkstraResult = context.getDijkstraResultMap().get(closest);
            while (closestTargetDijkstraResult.getDistance() > 1) {
                nextMove = closestTargetDijkstraResult.getPrevious();
                closestTargetDijkstraResult = context.getDijkstraResultMap().get(nextMove);
            }

            logger.info("Bot is attacking closest!");
            return BotUtils.directionTowards(closestTargetDijkstraResult.getPrevious(), nextMove);
        }
//        else if(me.getMineCount() == 0)
//        {
//            logger.info("Bot is attacking!");
//            // BotTargetingDecisioner
//            return combatDecisioner.makeDecision(context);
//        }
        else if(context.getGameState().getMe().getLife() >= 50) {
            logger.info("Bot is healthy.");
            // enRouteLootingDecisioner
            return yesDecisioner.makeDecision(context);
        }
        else if(context.getGameState().getMe().getMineCount() > 1 &&
                BotUtils.getVersaceHeroesAround(context.getGameState(), context.getDijkstraResultMap(), 1).size() > 0) {
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
