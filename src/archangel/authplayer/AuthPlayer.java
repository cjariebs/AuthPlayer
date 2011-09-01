/**
 * AuthPlayer: AuthPlayer.java
 * @author Alex Riebs
 * Created Jul 6, 2011
 */
package archangel.authplayer;

import java.util.logging.Logger;

import net.minecraft.server.EntityPlayer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

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
public class AuthPlayer extends JavaPlugin {
  Logger log = Logger.getLogger("Minecraft");
  private final AuthPlayerPlayerListener playerListener = new AuthPlayerPlayerListener(this);
  private final AuthPlayerBlockListener blockListener = new AuthPlayerBlockListener(this);

  private AuthDatabase db;
  private PermissionHandler permissions;

  /*
   * @see org.bukkit.plugin.Plugin#onDisable()
   */
  @Override
  public void onDisable() {
    PluginDescriptionFile pdfFile = getDescription();
    log.info("AuthPlayer "+pdfFile.getVersion()+" disabled.");
  }

  /*
   * @see org.bukkit.plugin.Plugin#onEnable()
   */
  @Override
  public void onEnable() {
    PluginManager pm = this.getServer().getPluginManager();
    pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
    pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Event.Priority.High, this);
    pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.High, this);
    pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.High, this);

    db = new AuthDatabase(this);

    PluginDescriptionFile pdfFile = getDescription();
    log.info("AuthPlayer "+pdfFile.getVersion()+" enabled.");
    setupPermissions();
  }

  public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
    if (cmd.getName().equalsIgnoreCase("auth")) {
      // [?] Provide help on /auth
      if (args.length <= 0) return false;
      if (!(sender instanceof Player)) {
        sender.sendMessage("This command cannot be used from the console");
        return true;
      }

      if (args[0].equals("list")){
        if (hasPermission((Player)sender, "authplayer.list")){
          sender.sendMessage("Registered players: \n" + getAuthDatabase().toString());
        }else{
          sender.sendMessage("You do not have permission to get the registered players list.");
        }
        return true;
      }

      if (args[0].equals("del") || args[0].equals("delete")){
        if (hasPermission((Player)sender, "authplayer.delete")){
          if (args.length <= 2){
            sender.sendMessage("Usage: /auth delete <username>");
            return true;
          }
          if (getAuthDatabase().get(args[1]) != null){
            getAuthDatabase().delete(args[1]);
            sender.sendMessage("User "+args[1]+" deleted from registered users.");
          }else{
            sender.sendMessage("There is no such registered user: "+args[1]);
          }
        }else{
          sender.sendMessage("You do not have permission to delete registered players.");
        }
        return true;
      }

      if (args[0].equals("pass") || args[0].equals("password") || args[0].equals("passwd")){
        if (hasPermission((Player)sender, "authplayer.passwd")){
          if (args.length <= 2){
            sender.sendMessage("Usage: /auth password <new password>");
            return true;
          }
          String user = ((Player)sender).getName();
          if (getAuthDatabase().get(user) != null){
            getAuthDatabase().delete(args[1]);
            getAuthDatabase().add(user, new AuthInfo((Player)sender, args[1]));
            sender.sendMessage("Your password has been changed.");
          }else{
            sender.sendMessage("You are not currently logged in!");
          }
        }else{
          sender.sendMessage("You do not have permission to change your password.");
        }
        return true;
      }

      if (args[0].equals("create") || args[0].equals("new") || args[0].equals("newaccount") || args[0].equals("createaccount")){
        if (hasPermission((Player)sender, "authplayer.create")){
          if (args.length <= 3){
            sender.sendMessage("Usage: /auth create <new username> <new password>");
            return true;
          }
          if (getAuthDatabase().get(args[1]) == null){
            getAuthDatabase().add(args[1], new AuthInfo((Player)sender, args[2]));
            sender.sendMessage("New account created with name "+args[1]);
          }else{
            sender.sendMessage("This account already exists!");
          }
        }else{
          sender.sendMessage("You do not have permission to create accounts.");
        }
        return true;
      }

      return authLogin(sender, args);
    }
    return false;
  }

  public boolean authLogin(CommandSender sender, String[] args) {
    if (args.length < 1) return false;

    Player player = (Player)sender;
    String username = player.getName();

    AuthDatabase db = getAuthDatabase();

    // [?] Catch people that already have a name
    if (!username.substring(0, 6).equalsIgnoreCase("Player")) {
      // [?] Already have a name, and already in the db? Must be premium or already authenticated.
      if (db.get(username) != null) {
        player.sendMessage("You are already logged in!");
      } else {
        db.add(username, new AuthInfo(player, args[0]));
        player.sendMessage("Password saved! You can now play.");
      }
      return true;
    } else {
      // [?] I'm a guest, help!
      if (args.length < 2) {
        player.sendMessage("Please supply a username AND password.");
        return true;
      }

      String loginName = args[0];
      String password = args[1];

      if (loginName.substring(0, 6).equalsIgnoreCase("Player")) {
        player.sendMessage("Please do not start your username with \"player\". Try again.");
        return true;
      }

      // [?] Does the player exist on the server?
      if (db.get(loginName) == null) {
        if (hasPermission((Player)sender, "authplayer.create")){
          db.add(loginName, new AuthInfo(player, password));
          player.sendMessage("You have registered the name " + loginName + ". Use /auth again to authenticate.");
          log.info("AuthPlayer: New user " + loginName + " created");
        }else{
          player.sendMessage("You are not allowed to create new accounts.");
          player.sendMessage("To create a new account: login to this server using your minecraft account, or ask an admin to create one for you.");
        }
      } else {
        // [?] Player exists, is he online?
        Player checkPlayer = getServer().getPlayer(loginName);
        if (checkPlayer != null && checkPlayer.isOnline())
          player.sendMessage("Someone is already playing as " + loginName + "!");
        else {
          // [?] Player isn't currently playing, so authenticate.
          AuthInfo info = db.get(loginName);
          if (info.authenticate(password)) {
            EntityPlayer entity = ((CraftPlayer)player).getHandle();
            entity.name = loginName;
            entity.displayName = entity.name;

            player.loadData();
            player.teleport(player);

            player.sendMessage("You are now logged in. Welcome, " + entity.name + "!");
            log.info("AuthPlayer: " + entity.name + " identified via /auth");
          } else {
            player.sendMessage("Wrong username or password. Try again.");
          }
        }
      }
    }

    return true;
  }

  public AuthDatabase getAuthDatabase() {
    return db;
  }

  private void setupPermissions() {
    Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

    if (permissions == null) {
      if (test != null) {
        permissions = ((Permissions) test).getHandler();
      } else {
        log.info("[AuthPlayer] Permission system not detected, allowing everything for operators only");
      }
    }
  }

  private boolean hasPermission(Player player, String node) {
    if(permissions == null) {
      if (node.equals("authplayer.passwd")){
        return true; 
      }else{
        return player.isOp();
      }
    } else {
      return permissions.has(player, node);
    }
  }


}
