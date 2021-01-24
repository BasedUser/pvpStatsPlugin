package plstats;


import arc.Core;
import arc.files.Fi;
import arc.struct.ObjectIntMap;
import arc.util.Log;
import arc.util.Timer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.file.Files;
import java.nio.file.Paths;

//all the IO methods
public class dataStorage {
    private static int VERSION = 1;

    private static String diPath = "stats";
    private static String fileName = "pvpStats.json";
    private static String fileNameBU = "pvpStats_backup.json";
    private String totalPath, totalPathBU;
    int writeCount = 0;

    JSONObject pvpStats;
    boolean writing = false;

    public dataStorage(){
        Fi path = Core.settings.getDataDirectory().child(diPath);
        totalPath = path.child(fileName).absolutePath();
        totalPathBU = path.child(fileNameBU).absolutePath();

        if(path.exists()){
            Log.info("[pvpStats] PATH EXISTS");
            //load data
            String pureJSON = path.child(fileName).readString();
            pvpStats = new JSONObject(new JSONTokener(pureJSON));
            //maybe can throw an error?
            if(!pvpStats.has("version")){
                Log.info("[pvpStats] stats file had invalid version...");
                makeFile();
            }else if(pvpStats.getInt("version") < VERSION){
                //stats file has invalid version
                Log.info("[pvpStats] stats file had invalid version...");
                makeFile();
            }
        }else{
            makeFile();
        }
    }

    public ObjectIntMap<String> getData(){
        Log.info("[pvpStats] Get data...");
        ObjectIntMap<String> ret = new ObjectIntMap<String>();
        JSONArray da = pvpStats.getJSONArray("pvpstats");
        JSONObject iterObj;
        for(int i=0; i < da.length(); i++){
            iterObj = (JSONObject)da.get(i);
            ret.put(iterObj.getString("name"), iterObj.getInt("points"));
        }
        return ret;
    }


    public void makeFile(){
        Log.info("[pvpStats] CREATING JSON FILE");
        Fi directory = Core.settings.getDataDirectory().child(diPath);
        if(!directory.isDirectory()){
            directory.mkdirs();
        }
        pvpStats = new JSONObject();
        JSONArray _stats = new JSONArray();
        JSONObject entry1 = new JSONObject();
        entry1.put("name", "TEST").put("points",5);
        _stats.put(entry1);
        pvpStats.put("version", 1);
        pvpStats.put("pvpstats", _stats);

        //make file
        try {
            Files.write(Paths.get(totalPath), pvpStats.toString().getBytes());
        }catch (Exception e){
            Log.info("[pvpStats] Failed to make .json file.");
        }
    }

    //meant to run in another thread!
    public void writeData(ObjectIntMap<String> data){
        writeData(totalPath, data);
    }


    public void writeData(String path, ObjectIntMap<String> data){
        Log.info("[pvpStats] writing data...");
        //ObjectIntMap -->

        while(this.writing){
            //sleep
            try{
                Thread.sleep(1000L);
            }catch(Exception e){
                Log.info("[pvpStats] Writing exception!");
            }
        }
        this.writing = true;
        //update
        JSONObject updatedStats = new JSONObject();
        JSONArray statsArray = new JSONArray();
        for(String s: data.keys()){
            statsArray.put(new JSONObject().put("name", s).put("points", data.get(s)));
        }
        updatedStats.put("pvpstats", statsArray);

        //make file
        try {
            Files.write(Paths.get(path), updatedStats.toString().getBytes());
        }catch (Exception e){
            Log.info("[pvpStats] Failed to make .json file.");
        }
        pvpStats = updatedStats;
        this.writing = false;
        Log.info("[pvpStats] writing done");
        if(writeCount > 10){
            writeCount = 0;
            writeBackUp(totalPathBU);
        }else{
            writeCount++;
        }
    }

    public void writeBackUp(String path) {
        Log.info("[pvpStats] writing BACKUP of pvpstats...");
        try {
            Files.write(Paths.get(path), pvpStats.toString().getBytes());
        }catch (Exception e){
            Log.info("[pvpStats] Failed to make back up json file.");
        }
    }
}
