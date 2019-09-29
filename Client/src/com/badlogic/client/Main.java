package com.badlogic.client;

import com.badlogic.core.factory.Factory;
import com.badlogic.core.factory.FactoryProvider;
import com.badlogic.core.factory.bonuses.Bonus;
import com.badlogic.core.factory.bonuses.BonusType;
import com.badlogic.core.factory.weapons.Weapon;
import com.badlogic.core.factory.weapons.WeaponType;

public class Main {
    public static void main(String[] args)  {
        // new Loop().start();

        // Weapon factory
        var weaponFactory = FactoryProvider.getFactory(Factory.WEAPON);
        var pistol = (Weapon)weaponFactory.create(WeaponType.PISTOL);
        var rifle = (Weapon)weaponFactory.create(WeaponType.RIFLE);
        var shotgun = (Weapon)weaponFactory.create(WeaponType.SHOTGUN);
        pistol.shoot();
        rifle.shoot();
        shotgun.shoot();

        // Bonus factory
        var bonusFactory = FactoryProvider.getFactory(Factory.BONUS);
        var ammoBonus = (Bonus)bonusFactory.create(BonusType.AMMO);
        var healthBonus = (Bonus)bonusFactory.create(BonusType.HEALTH);
        var speedBonus = (Bonus)bonusFactory.create(BonusType.SPEED);
        ammoBonus.applyBonus(null);
        healthBonus.applyBonus(null);
        speedBonus.applyBonus(null);

        /*
        CountDownLatch latch = new CountDownLatch(1);
        Connector connector = new Connector(latch);
        WebSocket webSocket = HttpClient.newHttpClient()
                                        .newWebSocketBuilder()
                                        .buildAsync(URI.create(Constants.SOCKET_CONNECTION_STRING), connector)
                                        .join();
        webSocket.sendText("Hello", true);
        latch.await();
         */
    }
}
