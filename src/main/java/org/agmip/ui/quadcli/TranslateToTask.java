package org.agmip.ui.quadcli;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.agmip.core.types.TranslatorOutput;
import org.agmip.translators.apsim.ApsimOutput;
import org.agmip.translators.dssat.DssatControllerOutput;
import org.agmip.translators.dssat.DssatWeatherOutput;
import org.agmip.util.AcmoUtil;
import static org.agmip.util.JSONAdapter.toJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslateToTask {

    private HashMap data;
    private ArrayList<String> translateList;
    private ArrayList<String> weatherList, soilList;
    private String destDirectory;
    private static Logger LOG = LoggerFactory.getLogger(TranslateToTask.class);

    public TranslateToTask(ArrayList<String> translateList, HashMap data, String destDirectory) {
        this.data = data;
        this.destDirectory = destDirectory;
        this.translateList = new ArrayList<String>();
        this.weatherList = new ArrayList<String>();
        this.soilList = new ArrayList<String>();
        for (String trType : translateList) {
            if (!trType.equals("JSON")) {
                this.translateList.add(trType);
            }
        }
        if (data.containsKey("weathers")) {
            for (HashMap<String, Object> stations : (ArrayList<HashMap>) data.get("weathers")) {
                weatherList.add((String) stations.get("wst_id"));
            }
        }

        if (data.containsKey("soils")) {
            for (HashMap<String, Object> soils : (ArrayList<HashMap>) data.get("soils")) {
                soilList.add((String) soils.get("soil_id"));
            }
        }
    }

    public String execute() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(64);
        try {
            for (String tr : translateList) {
                // Generate the ACMO here (pre-generation) so we know what
                // we should get out of everything.
                AcmoUtil.writeAcmo(destDirectory + File.separator + tr.toUpperCase(), data, tr.toLowerCase());
                if (tr.equals("DSSAT")) {
                    if (data.size() == 1 && data.containsKey("weather")) {
                        LOG.info("Running in weather only mode");
                        submitTask(executor, tr, data, true);
                    } else {
                        submitTask(executor, tr, data, false);
                    }
                } else {
                    // Handle translators that do not support the multi-experiment
                    // format.
                    if (data.containsKey("experiments")) {
                        for (HashMap<String, Object> experiment : (ArrayList<HashMap>) data.get("experiments")) {
                            HashMap<String, Object> temp = new HashMap<String, Object>(experiment);
                            int wKey, sKey;
                            if (temp.containsKey("wst_id")) {
                                if ((wKey = weatherList.indexOf((String) temp.get("wst_id"))) != -1) {
                                    temp.put("weather", ((ArrayList<HashMap<String, Object>>) data.get("weathers")).get(wKey));
                                }
                            }
                            if (temp.containsKey("soil_id")) {
                                if ((sKey = soilList.indexOf((String) temp.get("soil_id"))) != -1) {
                                    temp.put("soil", ((ArrayList<HashMap<String, Object>>) data.get("soils")).get(sKey));
                                }
                            }
                            LOG.debug("JSON of temp:" + toJSON(temp));
                            // need to re-implement properly for threading apsim
                            //submitTask(executor, tr, temp);
                            if (tr.equals("APSIM")) {
                                ApsimOutput translator = new ApsimOutput();
                                translator.writeFile(destDirectory + File.separator + "APSIM", temp);
                            }
                        }
                    } else {
                        boolean wthOnly = false;
                        if (data.size() == 1 && data.containsKey("weather")) {
                            wthOnly = true;
                        }
                        //Assume this is a single complete experiment
                        submitTask(executor, tr, data, wthOnly);
                    }
                }
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
            executor = null;
            //this.data = null;
        } catch (Exception ex) {
            throw new Exception(ex);
        }
        return null;
    }

    /**
     * Submit a task to an executor to start translation.
     *
     * @param executor The <code>ExecutorService</code> to execute this thread
     * on.
     * @param trType The model name to translate to (used to instantiate the
     * proper <code>TranslatorOutput</code>
     * @param data The data to translate
     */
    private void submitTask(ExecutorService executor, String trType, HashMap<String, Object> data, boolean wthOnly) {
        TranslatorOutput translator = null;
        String destination = "";
        if (trType.equals("DSSAT")) {
            if (wthOnly) {
                translator = new DssatWeatherOutput();
            } else {
                translator = new DssatControllerOutput();
            }
        } else if (trType.equals("APSIM")) {
            translator = new ApsimOutput();
        }
        destination = destDirectory + File.separator + trType;
        LOG.debug("Translating with :" + translator.getClass().getName());
        Runnable thread = new TranslateRunner(translator, data, destination);
        executor.execute(thread);
    }
}
