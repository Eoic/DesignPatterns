package com.badlogic.core.factory.bonuses;

import com.badlogic.game.GameManager;
import com.badlogic.game.Player;

import java.awt.image.BufferedImage;

public class SpeedBonus extends Bonus {
    public SpeedBonus(GameManager gameManager, BufferedImage sprite) {
        super(gameManager, sprite);
    }

    @Override
    public void applyBonus(Player player) {
        System.out.println("Adding " +  this.bonusAmount + " speed to player.");
    }

    @Override
    public int getBonus() {
        return 0;
    }

    @Override
    public int getLifespan() {
        return 0;
    }

    @Override
    public void setLifespan(int lifespan) {
        this.lifespan = lifespan;
    }

    @Override
    public void setBonusAmount(int bonusAmount) {
        this.bonusAmount = bonusAmount;
    }

    @Override
    public void update(int delta) {

    }
}
