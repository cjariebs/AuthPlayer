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
import org.bukkit.event.player.PlayerPickupItemEvent;

/**
 * @author Alex "Arcalyth" Riebs
 * License: Don't steal my shit. Give credit if you modify and/or redistribute any of my code.
 * Otherwise, you're free to do whatever else you want with it.
 * Also, the Bukkit license applies.
 * 
 * @author Thulinma
 * Edited original work by Arclyth so it actually works as advertised.
 * Also added an item pickup disabler for unauthed users.
 */
public class AuthPlayerPlayerListener extends PlayerListener {
	public static AuthPlayer plugin;
	
	private void setPlayerGuest(Player player){
    player.sendMessage("You are currently a guest, and cannot play until you login to your account.");
    player.sendMessage("Use /auth <username> <password> to login.");
    // rename to player_entID to prevent people kicking each other off
    EntityPlayer entity = ((CraftPlayer)player).getHandle();
    entity.name = "Player_"+entity.id;
    entity.displayName = entity.name;
    //clear inventory
    player.getInventory().clear();
    //teleport to default spawn loc
    player.teleport(player.getWorld().getSpawnLocation());	  
    plugin.log.info("AuthPlayer: Nonpremium user has been asked to login.");
	}

	/**
	 * @param authPlayer
	 */
	public AuthPlayerPlayerListener(AuthPlayer instance) {
		plugin = instance;
	}

	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

    if (name.substring(0, 6).equalsIgnoreCase("Player")) {
      setPlayerGuest(player);
		} else {
      // Check if player is real player, first...
		  String inputLine = "";
		  try{
		    URL mcheck = new URL("http://www.minecraft.net/game/checkserver.jsp?premium="+name);
		    URLConnection mcheckc = mcheck.openConnection();
		    mcheckc.setReadTimeout(1500);
		    BufferedReader in = new BufferedReader(new InputStreamReader(mcheckc.getInputStream()));
		    inputLine = in.readLine();
		    in.close();
		  } catch(Exception e){
		    plugin.log.info("AuthPlayer: Premium check error, assuming nonpremium: "+e.getMessage());
		  }
      if (inputLine.equals("PREMIUM")){
        // [?] Tell real players to enter themselves into the AuthDB
        if (plugin.getAuthDatabase().get(name) == null) {
          player.sendMessage("Welcome, " + name + "! It appears this is your first time playing on this server.");
          player.sendMessage("Please create a password for your account by typing /auth <password>");
          player.sendMessage("This will allow you to play even if minecraft login servers are down.");
          player.sendMessage("The server admin can see what you pick as password so don't use the same password as your Minecraft account!");
        } else {
          plugin.log.info("AuthPlayer: Premium user " + name + " auto-identified.");
        }
      } else {
        setPlayerGuest(player);
      }
		}
	}

	public void onPlayerInteract(PlayerInteractEvent event){
		Player player = event.getPlayer();
		if (plugin.getAuthDatabase().get(player.getName()) == null) {
			event.setCancelled(true);
			player.sendMessage("You cannot play until you are authenticated.");
		}
	}
	
	public void onPlayerPickupItem(PlayerPickupItemEvent event){
    Player player = event.getPlayer();
    if (plugin.getAuthDatabase().get(player.getName()) == null) {
      event.setCancelled(true);
      player.sendMessage("You cannot play until you are authenticated.");
    }
  }
	
}