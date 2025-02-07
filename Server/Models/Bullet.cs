﻿using System.Collections.Generic;
using Server.Game;
using Server.Game.Entities;
using System.Runtime.Serialization;
using System.Text;
using Server.Game.Physics;
using Server.Network;

namespace Server.Models
{
    public class Bullet : GameObject, ISubject<string>
    {
        public int Id { get; set; }
        public int Damage { get; set; }
        public int Speed { get; set; }
        public string GunType { get; set; }
        public bool IsActive { get; set; }
        public BulletCollider Collider { get; }
        public List<IObserver<string>> Observers { get; }

        public Bullet(int damage, string gunType)
        {
            Damage = damage;
            GunType = gunType;
            Position = new Vector(0, 0);
            Direction = new Vector(0, 0);
            Collider = new BulletCollider();
            Observers = new List<IObserver<string>>();
        }

        public void SetPosition(Vector position)
        {
            Position = new Vector(position.X, position.Y);
        }

        public void SetDirection(Vector direction)
        {
            Direction = new Vector(direction.X, direction.Y);
        }

        public override void Update(long delta)
        {
            if (!Collider.ProcessMotion(delta, this))
                IsActive = false;
        }

        public override string ToString()
        {
            var builder = new StringBuilder();
            builder.AppendFormat("| {0, -30} | {1, -15} |", Position.ToString(), Direction.ToString());
            return builder.ToString();
        }
        
        public SerializableBullet GetSerializable() =>
            new SerializableBullet(Id, Position, Direction);

        [DataContract]
        public class SerializableBullet
        {
            [DataMember]
            public int Id { get; set; }
            [DataMember]
            public Vector Position { get; set; }
            [DataMember]
            public Vector Direction { get; set; }

            public SerializableBullet(int id, Vector position, Vector direction)
            {
                Id = id;
                Position = position;
                Direction = direction;
            }
        }

        public void Attach(IObserver<string> observer)
        {
            if (Observers.Contains(observer))
                return;

            Observers.Add(observer);
        }

        public void Detach(IObserver<string> observer) =>
            Observers.Remove(observer);

        public void NotifyAllObservers(string data) =>
            Observers.ForEach((observer) => observer.Update(data));
    }
}