package com.brianstempin.vindiniumclient.bot.advanced.versacebot;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.dto.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Created by Vicken on 12/11/2015.
 */
public class StartDecisioner implements Decision<VersaceBot.GameContext, BotMove> {

    private static final Logger logger = LogManager.getLogger(StartDecisioner.class);

    private final Decision<VersaceBot.GameContext, BotMove> yesDecisioner;
    private final Decision<VersaceBot.GameContext, BotMove> noDecisioner;

    public StartDecisioner(Decision<VersaceBot.GameContext, BotMove> yesDecisioner,
                           Decision<VersaceBot.GameContext, BotMove> noDecisioner) {
        this.yesDecisioner = yesDecisioner;
        this.noDecisioner = noDecisioner;
    }

    @Override
    public BotMove makeDecision(VersaceBot.GameContext context) {
        GameState.Hero me = context.getGameState().getMe();
        Map<GameState.Position, GameState.Hero> heroesByPosition = context.getGameState().getHeroesByPosition();

        int gold = 0;

        for(GameState.Hero hero : heroesByPosition.values())
        {
            if(hero.getGold() > gold && hero.getId() != me.getId())
            {
                gold = hero.getGold() + (hero.getMineCount() * ((1200 - context.getGameState().getTurn()) / 4));
            }
        }

        logger.info(context.getGameState().getTurn());

        int myGold = me.getGold() + (me.getMineCount() * ((1200 - context.getGameState().getTurn()) / 4));

        if(myGold > (gold + 100 + ((1200 - context.getGameState().getTurn()) / 20))) {
            // squatDecisioner
            logger.info("Predicting Win.");
            return yesDecisioner.makeDecision(context);
        }
        else
        {
            // botWellnessDecisioner
            logger.info("Might not win.");
            return noDecisioner.makeDecision(context);
        }
    }
}
