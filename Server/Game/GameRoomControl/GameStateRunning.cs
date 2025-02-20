﻿using System.Diagnostics;

namespace Server.Game.GameRoomControl
{
    // Game started
    public class GameStateRunning : IGameState
    {
        public GameContext Context { get; }

        public GameStateRunning(GameContext context) =>
            Context = context;

        public void Tick()
        {
            if (Context.TimeTillStateChange > 0)
            {
                Context.TimeTillStateChange--;
                return;
            }

            EndGame();
        }

        public void EndGame()
        {
            Debug.WriteLine("[Running -> Ended]");
            Context.SetState(new GameStateEnded(Context));
        }
    }
}