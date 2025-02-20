package com.badlogic.core;

import com.badlogic.core.factory.Bonus;
import com.badlogic.core.factory.BonusFactory;
import com.badlogic.core.factory.BonusType;
import com.badlogic.core.observer.Observer;
import com.badlogic.game.GameManager;
import com.badlogic.game.Player;
import com.badlogic.game.RemotePlayer;
import com.badlogic.gfx.Assets;
import com.badlogic.gfx.HeadUpDisplay;
import com.badlogic.gfx.Map;
import com.badlogic.network.GameRoom;
import com.badlogic.network.Message;
import com.badlogic.network.MessageEmitter;
import com.badlogic.network.RequestCode;
import com.badlogic.network.ResponseCode;
import com.badlogic.serializables.*;
import com.badlogic.util.*;
import com.badlogic.util.Point;
import com.badlogic.util.Vector;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class Loop implements Observer {
    // Status
    private boolean clientInGame = false;
    private HeadUpDisplay hud;

    // Game loop
    private static final long timeStep = 1000 / Constants.FPS;
    private ScheduledExecutorService backgroundEventsExecutor;
    private ScheduledExecutorService executor;
    private ExecutorService messageExecutor;
    private MessageEmitter messageEmitter;
    private JsonParser jsonParser;
    private boolean isRunning;
    private GameRoom gameRoom;
    private long delta = 0;
    private long lastTime;

    // Game entities
    private HashMap<String, RemotePlayer> roomPlayers;
    private ArrayList<Bonus> bonuses;
    private GameManager gameManager;
    private Player player;
    private Map map;

    public Loop() {
        Assets.load();
        messageExecutor = Executors.newSingleThreadExecutor();
        messageEmitter = new MessageEmitter();
        executor = Executors.newSingleThreadScheduledExecutor();
        backgroundEventsExecutor = Executors.newSingleThreadScheduledExecutor();
        jsonParser = new JsonParser();
        gameRoom = new GameRoom();
        gameManager = new GameManager();
        map = new Map(Constants.MAP_WIDTH, Constants.MAP_HEIGHT, gameManager);
        player = new Player(gameManager, messageEmitter, "Undefined", -1);
        bonuses = new ArrayList<>();
        roomPlayers = new HashMap<>();
        hud = new HeadUpDisplay(gameManager.getWindow());
        this.initialize();
    }

    // Starts game loop
    public void start() {
        if (isRunning)
            return;

        isRunning = true;
        lastTime = System.currentTimeMillis();

        executor.scheduleAtFixedRate(() -> {
            update();
            render();
        }, 0, timeStep, TimeUnit.MILLISECONDS);

        backgroundEventsExecutor.scheduleAtFixedRate(() -> {
            messageEmitter.send(jsonParser.serialize(new Message(RequestCode.FormGameList, "")));
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    // Initializes game resources
    private void initialize() {
        // Check if communication with server is possible.
        if (messageEmitter.isConnectionFailed())
            return;

        // Set event listener
        messageEmitter.addListener(this);

        // SET UI EVENTS
        // # Create game
        gameManager.getWindow().setCreateGameBtnEvent(actionEvent -> {
            ArrayList<ActionListener> listeners = new ArrayList<>();

            // Bind game creation events to team selection buttons.
            for (int i = 0; i < Constants.TEAM_COUNT; i++) {
                int finalI = i;
                listeners.add((action) -> {
                    var message = new Message(RequestCode.CreateGame, Integer.toString(finalI));
                    messageEmitter.send(jsonParser.serialize(message));
                });
            }

            gameManager.getWindow().setTeamSelectionEvents(messageEmitter, listeners);
            gameManager.getWindow().showTeamSelectionWindow();
        });

        // # Exit game
        gameManager.getWindow().setQuitGameBtnEvent(actionEvent -> {
            var message = new Message(RequestCode.QuitGame, "Exiting.");
            messageEmitter.send(jsonParser.serialize(message));
        });
    }

    // Updates game entities (e.g. position)
    private void update() {
        gameManager.getInputManager().tick();
        roomPlayers.forEach((key, value) -> value.update((int) delta));
        player.update((int)delta);
    }

    // Renders game objects
    private void render() {
        var bufferStrategy = gameManager.getWindow().getCanvas().getBufferStrategy();
        long currentTime = System.currentTimeMillis();
        delta = (currentTime - lastTime);

        if (bufferStrategy == null) {
            gameManager.getWindow().getCanvas().createBufferStrategy(Constants.BUFFER_COUNT);
            return;
        }

        var graphics = bufferStrategy.getDrawGraphics();
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        ((Graphics2D)graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.clearRect(0, 0, gameManager.getWindow().getWidth(), gameManager.getWindow().getHeight());

        if (clientInGame) {
            map.render(graphics);
            bonuses.forEach(bonus -> bonus.render(graphics));
            roomPlayers.forEach((key, value) -> value.render(graphics));
            player.render(graphics);
            hud.render(graphics, roomPlayers, gameRoom.getPlayers().get(0).getTeam());
        }

        bufferStrategy.show();
        graphics.dispose();
        lastTime = currentTime;
    }

    // Handle messages received from server.
    @Override
    public void update(Object data) {
        if (data == null)
            return;

        // Deserialize received message.
        var message = jsonParser.deserialize(data.toString(), Message.class);

        if (message == null)
            return;

        // Take action according to event type.
        // # List all available games in the UI
        if (message.getType() == ResponseCode.GameListFormed) {
            List<SerializableGame> gameList = jsonParser.deserializeList(message.getPayload(), SerializableGame.class);
            gameManager.getWindow().updateGameList(new ArrayList<>(gameList), messageEmitter);
        }
        // # Create and place player on the map.
        else if (message.getType() == ResponseCode.GameCreated || message.getType() == ResponseCode.GameJoined) {
            var serializablePlayer = jsonParser.deserialize(message.getPayload(), SerializablePlayer.class);
            player = new Player(gameManager, messageEmitter, serializablePlayer.getPlayerId(), serializablePlayer.getTeam());
            player.setTeam(serializablePlayer.getTeam());
            player.getPosition().set(Vector.fromPoint(serializablePlayer.getPosition()));
            gameRoom.addPlayer(player);
            this.setActiveGameMode();
        }
        // # Update positions af all players in the room.
        else if (message.getType() == ResponseCode.PositionUpdated) {
            messageExecutor.submit(() -> {
                var players = jsonParser.deserializeList(message.getPayload(), SerializablePlayer.class);

                players.forEach(serializablePlayer -> {
                    if (serializablePlayer.getHealth() > 0) {
                        // Update this player.
                        if (serializablePlayer.getType() == 10) {
                            gameRoom.getPlayers().get(0).position.set(Vector.fromPoint(serializablePlayer.getPosition()));
                            gameManager.getWindow().getClientHealthBar().setCurrentValue(serializablePlayer.getHealth());
                        }

                        // Update other player in the game room.
                        else {
                            // Player is new
                            if (!roomPlayers.containsKey(serializablePlayer.getPlayerId())) {
                                var isFriendly = gameRoom.getPlayers().get(0).getTeam() == serializablePlayer.getTeam();
                                var roomPlayer = new RemotePlayer(gameManager.getWindow(), gameManager.getCamera(), isFriendly);
                                roomPlayer.setPosition(Vector.fromPoint(serializablePlayer.getPosition()));
                                roomPlayer.setDirection(Vector.fromPoint(serializablePlayer.getDirection()));
                                roomPlayer.parseBullets(serializablePlayer.getBullets());
                                roomPlayer.setHealth(serializablePlayer.getHealth());
                                roomPlayer.setTeam(serializablePlayer.getTeam());
                                roomPlayer.setId(serializablePlayer.getPlayerId());
                                roomPlayers.put(serializablePlayer.getPlayerId(), roomPlayer);
                            }
                            // Old player
                            else {
                                var roomPlayer = roomPlayers.get(serializablePlayer.getPlayerId());
                                roomPlayer.setPosition(Vector.fromPoint(serializablePlayer.getPosition()));
                                roomPlayer.setDirection(Vector.fromPoint(serializablePlayer.getDirection()));
                                roomPlayer.parseBullets(serializablePlayer.getBullets());
                                roomPlayer.setHealth(serializablePlayer.getHealth());
                            }
                        }
                    } else {
                        if (serializablePlayer.getType() == 10) {
                            gameManager.getWindow().getClientHealthBar().setVisible(false);
                        }
                    }
                });
            });
        }
        // # Show bonuses in the map
        else if (message.getType() == ResponseCode.BonusesCreated) {
            messageExecutor.submit(() -> {
                var serializableBonuses = jsonParser.deserializeList(message.getPayload(), SerializableBonus.class);
                createBonuses(serializableBonuses);
            });
        }
        // # Player is quitting the game
        else if (message.getType() == ResponseCode.GameQuit) {
            roomPlayers.remove(message.getPayload());
        }
        else if (message.getType() == ResponseCode.NewTimerValue) {
            hud.updateTimer(jsonParser.deserialize(message.getPayload(), SerializableTimer.class));
        }
        else if (message.getType() == ResponseCode.GameEnded) {
            var gameResults = jsonParser.deserializeList(message.getPayload(), SerializablePlayerState.class);
            gameManager.getWindow().setGameEndedMode(new ArrayList<SerializablePlayerState>(gameResults));
        } else if (message.getType() == ResponseCode.PlayerDied) {
            var serializablePlayer = jsonParser.deserialize(message.getPayload(), SerializablePlayer.class);
            var id = serializablePlayer.getPlayerId();

            if (roomPlayers.containsKey(id)) {
                roomPlayers.remove(id);
            } else {
                player.setDead();
            }
        }
    }

    private void createBonuses(List<SerializableBonus> serializableBonuses) {
        bonuses = new ArrayList<>();

        serializableBonuses.forEach(serializableBonus -> {
            Bonus bonus = null;

            if (Objects.equals(serializableBonus.getType(), BonusType.HEALTH))
                bonus = BonusFactory.create(BonusType.HEALTH, gameManager);
            else if (serializableBonus.getType().equals(BonusType.AMMO))
                bonus = BonusFactory.create(BonusType.AMMO, gameManager);
            else if (Objects.equals(serializableBonus.getType(), BonusType.SPEED))
                bonus = BonusFactory.create(BonusType.SPEED, gameManager);

            if (bonus != null) {
                bonus.setBonusAmount(serializableBonus.getBonusAmount());
                bonus.setLifespan(serializableBonus.getLifespan());
                bonus.position.set(new Vector(serializableBonus.getPosition().getX(), serializableBonus.getPosition().getY()));
                bonuses.add(bonus);
            }
        });
    }

    private void setActiveGameMode() {
        clientInGame = true;
        backgroundEventsExecutor.shutdown();
        gameManager.getWindow().getClientHealthBar().setVisible(true);
        gameManager.getWindow().setCanvasColor(Constants.CANVAS_COLOR);
        this.gameManager.getWindow().setActiveGameMode();
    }
}
