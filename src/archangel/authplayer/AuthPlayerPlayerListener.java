/**
 * AuthPlayer: AuthPlayerPlayerListener.java
 * @author Alex Riebs
 * Created Jul 6, 2011
 */
package archangel.authplayer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import net.minecraft.server.EntityPlayer;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.Bukkit;

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

	public void onPlayerPreLogin(PlayerPreLoginEvent event) {
	  plugin.log.info("AuthPlayer: "+event.getName()+" prelogin: "+event.getKickMessage());
	  
	}

	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

    if (name.substring(0, 6).equalsIgnoreCase("Player")) {
      // rename to player_entID to prevent people kicking each other off
      player.sendMessage("You are currently a guest, and cannot play until you authenticate yourself. Use /auth to authenticate.");
      EntityPlayer entity = ((CraftPlayer)player).getHandle();
      entity.name = "Player_"+entity.id;
      entity.displayName = entity.name;
      player.loadData();
      player.teleport(player);
		} else {
      // Check if player is real player, first...
		  String inputLine = "";
		  try{
		    URL mcheck = new URL("http://www.minecraft.net/game/checkserver.jsp?user="+name+"&serverId="+Bukkit.getServer().getServerId());
		    plugin.log.info("AuthPlayer:  Checking URL: http://www.minecraft.net/game/checkserver.jsp?user="+name+"&serverId="+Bukkit.getServer().getServerId());
		    URLConnection mcheckc = mcheck.openConnection();
		    mcheckc.setReadTimeout(1500);
		    BufferedReader in = new BufferedReader(new InputStreamReader(mcheckc.getInputStream()));
		    inputLine = in.readLine();
		    in.close();
		  } catch(Exception e){
		    plugin.log.info("AuthPlayer: minecraft.net check error: "+e.getMessage());
		  }
      plugin.log.info("AuthPlayer: minecraft.net response was "+inputLine);
      if (inputLine.equals("YES")){
        // [?] Tell real players to enter themselves into the AuthDB
        if (plugin.getAuthDatabase().get(name) == null) {
          player.sendMessage("Welcome, " + name + "! It appears this is your first time playing on this server. " +
              "Please authenticate yourself with the server by using /auth <password> to create a password for this account.");
        } else {
          player.sendMessage("You are now authenticated. Welcome, " + name + "!");
          plugin.log.info("AuthPlayer: " + name + " identified on join");
        }
      } else {
        // Not a real player. Rename to "player_entID" and display login request.
        player.sendMessage("You are currently a guest, and cannot play until you authenticate yourself. Use /auth to authenticate.");
        EntityPlayer entity = ((CraftPlayer)player).getHandle();
        entity.name = "Player_"+entity.id;
        entity.displayName = entity.name;
        plugin.log.info("AuthPlayer: " + name + " UNidentified on join, failed minecraft.net check!");
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