<?
//Rerouting Auth script, PHP version
//By Thulinma, Sep 1 2011
//License: Do what you want with it, but give me credit for at least the concept.
//
//Description:
//Changes the reply to the server from miencraft to always be "YES", allowing all users to login.
//A plugin can then check the login status afterwards, giving access to the named account or not.
//
//Usage:
//Put an entry in the hosts file on the (minecraft) server rerouting "www.minecraft.net" to the IP
//of the (web / HTTP) server this script is installed on.
//Make sure the folder this script is in is writeable to the webserver, or at least make a file
//called "logins.json" that is writeable to the webserver.
//Configure the webserver to serve this file when http://www.minecraft.net/game/checkserver.jsp is
//called. An example config excerpt for lighttpd:
/*
$HTTP["host"] =~ "(^|\.)minecraft.net$" {
  server.document-root = "/home/thulinma/minecraft/fakeserver"
  url.rewrite = ("^/game/checkserver.jsp(.*)$" => "/checkserver.php$1")
}
*/
//Instead of /home/thulinma/minecraft/fakeserver you would of course put the path where you saved
//this script yourself! The url rewrite will turn the jsp requests into PHP requests to this script.
//
//Server plugins can then call http://www.minecraft.net/game/checkserver.jsp?premium=[USERNAME HERE]
//and will get a single word as response to indicate if the user is premium or not.
//Responses: PREMIUM or NOTPREMIUM
//This check will work only once per login, so new users signing in with the same name afterwards are
//not seen as the same person unless they actually are the same person.
//
//Finally, this script has only one setting, it controls what login servers are accepted.
//My default accepts both the official minecraft server as well as the mineshafter server.
//You can add your own login server here or some other - I don't know how many exist.
//If you do not want to accept mineshafter logins, simply remove it here from the array.
//These servers will be checked in the order you put them in.
$servers = Array("http://minecraft.net/game/checkserver.jsp", "http://mineshafter.appspot.com/game/checkserver.jsp");



//Script starts here, you probably don't want to modify anything below here.


//Load the current waitinglist.
$log = json_decode(file_get_contents("logins.json"));

//Check a user in the waitinglist
if ($_REQUEST['premium']){
  foreach ($log AS $num => $entry){
    if ($entry->user == $_REQUEST['premium']){
      if ($entry->authed == "YES"){echo "PREMIUM";}else{echo "NOTPREMIUM";}
      unset($log[$num]);//remove the user from the waitinglist
      file_put_contents("logins.json", json_encode($log));
      die();
    }
  }
  //not found? return the save answer (not premium) by default
  die("NOTPREMIUM");
}

//check for proper config, if called without variables.
if (!$_REQUEST['user'] && !$_REQUEST['serverId']){
  if ($_SERVER["HTTP_HOST"] == "www.minecraft.net"){
    die("The script is installed properly! Yay!");
  }else{
    die("Please add a line to your hosts file to reroute www.minecraft.net to the address of this server (localhost?)!");
  }
}

$newvalue = Array("user" => $_REQUEST['user'], "authed" => "NO");

//check all servers
foreach ($servers AS $currserv){
  $response = file_get_contents($currserv."?user=".$_REQUEST['user']."&serverId=".$_REQUEST['serverId']);
  if ($response == "YES"){
    $newvalue->authed = "YES";
    $log[] = $newvalue;
    file_put_contents("logins.json", json_encode($log));
    //Return yes after a correct login is found.
    die("YES");
  }
}

//Store the failed auth.
$log[] = $newvalue;
file_put_contents("logins.json", json_encode($log));
//Always return yes to the server - a plugin will further check it.
die("YES");
