/**
 * AuthPlayer: AuthPlayerPlayerListener.java
 * @author Alex Riebs
 * Created Jul 6, 2011
 */
package archangel.authplayer;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;

/**
 * @author Alex "Arcalyth" Riebs
 * License: Don't steal my shit. Give credit if you modify and/or redistribute any of my code.
 * Otherwise, you're free to do whatever else you want with it.
 * 
 * Also, the Bukkit license applies.
 */
public class AuthPlayerPlayerListener extends PlayerListener {
	public static AuthPlayer plugin;
	
	/**
	 * @param authPlayer
	 */
	public AuthPlayerPlayerListener(AuthPlayer instance) {
		plugin = instance;
	}
	
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if (name.equals("Player")) {
			player.sendMessage("You are currently a guest, and cannot play until" +
					" you authenticate yourself. Use /auth to authenticate.");
		} else {
			// [?] Tell real players to enter themselves into the AuthDB
			if (plugin.getAuthDatabase().get(name) == null) {
				player.sendMessage("Welcome, " + name + "! It appears this is your first time playing on this server. " +
						"Please authenticate yourself with the server by using /auth <password> to create a password for this account.");
			} else {
				player.sendMessage("You are now authenticated. Welcome, " + name + "!");
				plugin.log.info("AuthPlayer: " + name + " identified on join");
			}
		}
	}
	
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		
		if (plugin.getAuthDatabase().get(player.getName()) == null) {
			event.setCancelled(true);
			player.sendMessage("You cannot play until you are authenticated.");
		}
	}
}