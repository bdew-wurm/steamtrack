package net.bdew.wurm.steamtracker;

import com.wurmonline.server.Servers;
import com.wurmonline.server.creatures.Communicator;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

public class SteamTrackerCommands {
    private static long validateId(String id) throws NumberFormatException, NullPointerException {
        if (id == null) throw new NullPointerException("SteamID is null");
        return Long.parseLong(id, 10);
    }

    MessagePolicy handleMessage(Communicator communicator, String message, String title) {
        if (communicator.player.getPower() < 4 || !message.startsWith("#steam")) return MessagePolicy.PASS;
        if (Servers.localServer.id != Servers.loginServer.id) {
            communicator.sendAlertServerMessage("SteamTracker commands should be used on login server, not here.");
            return MessagePolicy.DISCARD;
        }
        try {
            StringTokenizer tokens = new StringTokenizer(message);
            tokens.nextToken();

            if (message.equals("#steamhelp")) {
                communicator.sendHelpMessage("Available steam tracker commands:");
                communicator.sendHelpMessage("#steamplayers <steamid> - show all characters belonging to that steam id.");
                communicator.sendHelpMessage("#steamalts <name> - show all known alts for that character.");
                communicator.sendHelpMessage("#steamban <steamid> <reason> - issue a permanent ban by steam id.");
                communicator.sendHelpMessage("#steamunban <steamid> - remove a permanent ban by steam id.");
            } else if (message.startsWith("#steamplayers ")) {
                if (tokens.hasMoreElements()) {
                    Long steamId = validateId(tokens.nextToken());
                    List<Long> players = SteamTrackerDB.getPlayersForSteamId(steamId);
                    if (players.isEmpty()) {
                        communicator.sendNormalServerMessage(String.format("No known players for steam id %d", steamId));
                    } else {
                        List<String> names = new ArrayList<>();
                        for (Long wurmId : players) {
                            names.add(SteamTrackerDB.getNameForPlayer(wurmId).orElse("<unknown>") + " #" + wurmId);
                        }
                        communicator.sendNormalServerMessage(String.format("Known players for steam id %d:", steamId));
                        names.forEach(name -> communicator.sendNormalServerMessage(" - " + name));
                    }
                } else {
                    communicator.sendAlertServerMessage("Usage: #steamplayers <steamid>");
                }
            } else if (message.startsWith("#steamalts ")) {
                if (tokens.hasMoreElements()) {
                    String name = tokens.nextToken();
                    Optional<Long> wurmIdOpt = SteamTrackerDB.getPlayerId(name);
                    if (wurmIdOpt.isPresent()) {
                        Optional<Long> steamIdOpt = SteamTrackerDB.getSteamIdForPlayer(wurmIdOpt.get());
                        if (steamIdOpt.isPresent()) {
                            Long id = wurmIdOpt.get();
                            List<Long> players = SteamTrackerDB.getPlayersForSteamId(id);
                            List<String> names = new ArrayList<>();
                            for (Long wurmId : players) {
                                if (!wurmId.equals(wurmIdOpt.get()))
                                    names.add(SteamTrackerDB.getNameForPlayer(wurmId).orElse("<unknown>") + " #" + wurmId);
                            }
                            if (players.isEmpty()) {
                                communicator.sendNormalServerMessage(String.format("No known steam alts for steam player '%s'.", name));
                            } else {
                                communicator.sendNormalServerMessage(String.format("Known steam alts for player '%s':", id));
                                names.forEach(altName -> communicator.sendNormalServerMessage(" - " + altName));
                            }
                        } else {
                            communicator.sendAlertServerMessage(String.format("Player '%s' has no known steam id.", name));
                        }
                    } else {
                        communicator.sendAlertServerMessage(String.format("Player '%s' not found.", name));
                    }
                } else {
                    communicator.sendAlertServerMessage("Usage: #steamalts <name>");
                }
            } else if (message.startsWith("#steamban ")) {
                if (tokens.hasMoreElements()) {
                    long steamID = validateId(tokens.nextToken());
                    String reason = "";
                    while (tokens.hasMoreTokens()) {
                        reason = reason + tokens.nextToken() + ' ';
                    }
                    if (reason.length() < 4) {
                        communicator.sendAlertServerMessage("The reason is too short. Please explain a bit more.");
                    } else {
                        SteamTrackerDB.banSteamId(steamID, communicator.player.getName(), reason);
                        communicator.sendNormalServerMessage(String.format("Steam id %d was banned: %s.", steamID, reason));
                    }
                } else {
                    communicator.sendAlertServerMessage("Usage: #steamban <steamid> <reason>");
                }
            } else if (message.startsWith("#steamunban ")) {
                if (tokens.hasMoreElements()) {
                    long steamID = validateId(tokens.nextToken());
                    String reason = "";
                    while (tokens.hasMoreTokens()) {
                        reason = reason + tokens.nextToken() + ' ';
                    }
                    SteamTrackerDB.unbanSteamId(steamID);
                    communicator.sendNormalServerMessage(String.format("Steam id %d was unbanned.", steamID));
                } else {
                    communicator.sendAlertServerMessage("Usage: #steamunban <steamid>");
                }
            } else {
                communicator.sendAlertServerMessage("Unknown SteamTracker command. Use #steamhelp for list of valid commands.");
            }
            return MessagePolicy.DISCARD;
        } catch (Exception e) {
            communicator.sendAlertServerMessage("Error handling command: " + e.toString());
            return MessagePolicy.DISCARD;
        }
    }
}
