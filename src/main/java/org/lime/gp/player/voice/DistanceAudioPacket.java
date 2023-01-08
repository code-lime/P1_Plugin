package org.lime.gp.player.voice;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.plugins.impl.ServerLevelImpl;
import de.maxhenkel.voicechat.plugins.impl.VoicechatServerApiImpl;
import de.maxhenkel.voicechat.voice.common.LocationSoundPacket;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.UUID;

public class DistanceAudioPacket {
    public final UUID sender;
    public final byte[] data;
    public final long sequenceNumber;
    public final double distance;
    public final Position location;
    public DistanceAudioPacket(UUID sender, byte[] data, long sequenceNumber, double distance, Position location) {
        this.sender = sender;
        this.data = data;
        this.sequenceNumber = sequenceNumber;
        this.distance = distance;
        this.location = location;
    }
    public DistanceAudioPacket(LocationalSoundPacket packet, double distance) {
        this(packet.getSender(), packet.getOpusEncodedData(), packet.getSequenceNumber(), distance, packet.getPosition());
    }

    /*private static Location ofDistance(Position listener, Position sender, double distance, double voiceDistance) {
        return ofDistance(
                new Vector(listener.getX(), listener.getY(), listener.getZ()),
                new Vector(sender.getX(), sender.getY(), sender.getZ()),
                distance,
                voiceDistance
        ).toLocation(null);
    }*/
    public static Vector ofDistance(Vector listener, Vector sender, double distance, double voiceDistance) {
        Vector total = new Vector().add(listener).subtract(sender);
        double totalDistance = total.length();
        double percent = totalDistance / distance;
        return new Vector()
                .add(total)
                .normalize()
                .multiply(distance * percent - voiceDistance * percent)
                .add(sender);
    }

    public void send(World world) {
        //double totalDistance = Voicechat.SERVER_CONFIG.voiceChatDistance.get();
        Voice.getPlayersRange(new ServerLevelImpl(world), location, distance).forEach(player -> {
            if (sender.equals(player.getUuid())) return;
            Voice.getConnectionOf(player.getUuid())
                    .ifPresent(connection -> {
                        //Location sender = ofDistance(player.getPosition(), location, distance, totalDistance);
                        //Displays.drawPoint(sender.toVector());
                        VoicechatServerApiImpl.sendPacket(connection, new LocationSoundPacket(this.sender, new Location(null, location.getX(), location.getY(), location.getZ()), data, sequenceNumber, (float) distance, ""));
                    });
        });
    }
    public void send(Collection<Player> players) {
        //double voiceDistance = Voicechat.SERVER_CONFIG.voiceChatDistance.get();
        players.forEach(player -> {
            if (sender.equals(player.getUniqueId())) return;
            Voice.getConnectionOf(player.getUniqueId())
                    .ifPresent(connection -> {
                        //Location sender = ofDistance(new PositionImpl(player.getLocation()), location, distance, voiceDistance);
                        //Displays.drawPoint(sender.toVector());
                        VoicechatServerApiImpl.sendPacket(connection, new LocationSoundPacket(this.sender, new Location(null, location.getX(), location.getY(), location.getZ()), data, sequenceNumber, (float) distance, ""));
                    });
        });
    }
}














