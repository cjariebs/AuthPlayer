/**
 * AuthPlayer: AuthPlayerBlockListener.java
 * @author Alex Riebs
 * Created Jul 8, 2011
 */
package archangel.authplayer;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;

/**
 * @author Alex "Arcalyth" Riebs
 * License: Don't steal my shit. Give credit if you modify and/or redistribute any of my code.
 * Otherwise, you're free to do whatever else you want with it.
 * 
 * Also, the Bukkit license applies.
 */
public class AuthPlayerBlockListener extends BlockListener {
	public static AuthPlayer plugin;
	
	public AuthPlayerBlockListener(AuthPlayer instance) {
		plugin = instance;
	}
	
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();

		if (plugin.getAuthDatabase().get(player.getName()) == null) {
			event.setCancelled(true);
			player.sendMessage("You cannot play until you are authenticated.");
		}
	}
}
