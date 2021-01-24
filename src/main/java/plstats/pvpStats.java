package plstats;

import arc.*;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.world.blocks.storage.CoreBlock;

public class pvpStats extends Plugin {
    private long minScoreTime = 90000L;
    private long RageTime = 60000L; // 60 seconds
    private long canScore = 30000L; // if you switch 30 seconds before your team loses a core you still gets deducted some points

    public ObjectIntMap<String> playerPoints; // UUID - INTEGER
    public ObjectMap<String, timePlayerInfo> playerInfo;
    public dataStorage dS;

    //private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();

    //register event handlers and create variables in the constructor
    public pvpStats(){
        dS = new dataStorage();
        playerPoints = dS.getData();

        Events.on(PlayerConnect.class, event ->{
        //change the name of the player
            int points = playerPoints.get(event.player.uuid(),0);
            event.player.name(String.format("[sky]%d [white]#[] %s", points, event.player.name()));
        });

        Events.on(PlayerJoin.class, event -> {
            //save the timer!
            event.player.sendMessage("[orange] alpha version[white] - The number before your name equals your PVP points[]");
            playerInfo.put(event.player.uuid(), new timePlayerInfo(event.player));
        });

        Events.on(EventType.UnitChangeEvent.class, event -> {
            //event.player.sendMessage(event.player.unit().team().name);
        });

        Events.on(PlayerLeave.class, event -> {
            //save the timer!
            playerInfo.get(event.player.uuid()).left();

        });


        Events.on(EventType.BlockDestroyEvent.class, event -> {
           if(event.tile.build instanceof CoreBlock.CoreBuild && !Vars.state.gameOver){
               Log.info("Core destroyed");
               System.out.println(event.tile.build.team);
               System.out.println(event.tile.build.team.cores().size);
               if(event.tile.build.team.cores().size <= 1){
                   Call.sendMessage(String.format("[gold] %s lost...", event.tile.build.team.name));
                   //other player get a point
                   updatePoints(event.tile.team(), -1, 1, false);

               }
           }
        });

        Events.on(EventType.GameOverEvent.class, event -> {
            Log.info(String.format("Winner %s",event.winner));
            //new map
            //updatePoints(event.winner, 1, -1, true);

        });
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("writestats", "update the json stats file", (args)->{
           Core.app.post(() -> dS.writeData(playerPoints));

        });

    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        /*
        handler.<Player>register("forceteam", "[team] [transfer_players]","[scarlet]Admin only[] For info use the command without arguments.", (args, player) -> {
                if(!player.admin()){
                    player.sendMessage("[scarlet]Admin only");
                    return;
                }
        });

         */
    }

    private void updatePoints(Team selectTeam, int addSelect, int addOther, boolean write){
        Seq<Team> validTeams = new Seq<>();
        Seq<Player> allPlayers = new Seq<>().with(Groups.player);
        Player p;
        String uuid;
        for(int i=0; i < allPlayers.size; i++){
            p = allPlayers.get(i);
            uuid = p.uuid();
            if(p.team() == selectTeam){
                playerPoints.put(uuid, playerPoints.get(uuid,0)+addSelect);
            }else{
                if(validTeams.contains(p.team())) {
                    playerPoints.put(uuid, playerPoints.get(uuid,0)+addOther);
                }else{
                    //check if valid
                    if(p.team().cores().size > 0){
                        validTeams.add(p.team());
                        playerPoints.put(uuid, playerPoints.get(uuid, 0)+addOther);
                    }
                }
            }
            // UPDATE PLAYER NAMES
            String oldName = p.name().substring(p.name().indexOf("#")+1);
            p.name(String.format("[sky]%d [white]#[] %s", playerPoints.get(uuid, 0), oldName));
        }
        if(write){
            Core.app.post(() -> dS.writeData(playerPoints));
        }
    }
}
