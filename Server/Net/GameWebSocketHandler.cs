﻿using Microsoft.Web.WebSockets;

namespace Server.Net
{
    public class GameWebSocketHandler : WebSocketHandler
    {
        private static readonly WebSocketCollection clients = new WebSocketCollection();

        // Add new client to client connections list
        public override void OnOpen()
        {
            clients.Add(this);
        }

        // Called on message received from client
        public override void OnMessage(string message)
        {
            Send($"Echo: {message}.");
        }

        // Called if client disconnects
        public override void OnClose()
        {
            base.OnClose();
        }

        // An error occoured
        public override void OnError()
        {
            base.OnError();
        }
    }
}