﻿using Server.Game.Entities;
using Server.Network;
using Server.Utilities;
using System;
using System.Collections.Generic;
using System.Diagnostics;

namespace Server.Game
{
    public class PlayerManager
    {
        public static void AddPlayer(List<GameRoom> games, Guid clientId, Guid roomId)
        {
            var gameToJoin = games.Find(game => game.RoomId == roomId);

            if (gameToJoin != null)
            {
                if (gameToJoin.Players.ContainsKey(clientId))
                {
                    Debug.WriteLine("Player is already in the game. Bailing out.");
                    return;
                }

                // 1. Find room and prepare client.
                var joiningPlayer = new Player(clientId, roomId);
                var initPos = new Vector(Map.CenterX, Map.CenterY);
                joiningPlayer.Position = initPos;
                gameToJoin.AddPlayer(joiningPlayer);

                // 3. Notify about successful join
                var gameJoinString = new Message(ResponseCode.GameJoined, JsonParser.Serialize(initPos));
                ConnectionsPool.GetInstance().GetClient(clientId).Send(JsonParser.Serialize(gameJoinString));
                Debug.WriteLine("Player added to the game");
                return;
            }

            Debug.WriteLine("This game room does not exist.");
        }

        public static void RemovePlayer(List<GameRoom> games, Guid clientId, Guid roomId)
        {
            // TODO: Search by room.
            int? emptyRoomIndex = null;

            for (var i = 0; i < games.Count; i++)
            {
                var playerInThisRoom = games[i].Players.ContainsKey(clientId);

                if (!playerInThisRoom)
                    continue;

                games[i].Players.Remove(clientId);

                if (games[i].Players.Count == 0)
                {
                    Debug.WriteLine("Room has no more players...");
                    emptyRoomIndex = i;
                }

                break;
            }

            if (emptyRoomIndex.HasValue)
                games.RemoveAt(emptyRoomIndex.Value);
        }
    }
}