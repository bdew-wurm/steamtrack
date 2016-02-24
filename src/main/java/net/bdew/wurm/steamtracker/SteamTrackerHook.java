package net.bdew.wurm.steamtracker;

import com.wurmonline.server.players.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class SteamTrackerHook {
    private static long validateId(String id) throws NumberFormatException, NullPointerException {
        if (id == null) throw new NullPointerException("SteamID is null");
        return Long.parseLong(id, 10);
    }

    public static boolean check(String name, String steamId) {
        return true;
    }

    public static void track(Player player, String steamIdAsString) {
        try {
            long steamId = validateId(steamIdAsString);
            SteamTrackerDB.updateSteamIdForPlayer(player.getWurmId(), steamId);
            List<Long> alts = SteamTrackerDB.getPlayersForSteamId(steamId);
            List<String> names = new ArrayList<>();
            for (Long id : alts) {
                if (id != player.getWurmId())
                    names.add(SteamTrackerDB.getNameForPlayer(id).orElse(String.format("<Unknown #%d>", id)));
            }
            if (names.size() > 0)
                SteamTracker.logger.info(String.format("Player Logged In: %s (%d) with SteamID %d. Known alts: %s", player.getName(), player.getWurmId(), steamId, String.join(", ", names)));
            else
                SteamTracker.logger.info(String.format("Player Logged In: %s (%d) with SteamID %d. No known alts.", player.getName(), player.getWurmId(), steamId));
        } catch (Throwable e) {
            SteamTracker.logger.log(Level.WARNING, String.format("Error tracking steamId for player %s", player), e);
        }
    }
}
