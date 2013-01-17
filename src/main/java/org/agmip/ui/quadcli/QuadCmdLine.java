package org.agmip.ui.quadcli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import static org.agmip.util.JSONAdapter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Meng Zhang
 */
public class QuadCmdLine {

    public enum DomeMode {

        NONE, FIELD, STRATEGY
    }

    public enum Model {

        DSSAT, APSIM, JSON
    }
    private static Logger LOG = LoggerFactory.getLogger(QuadCmdLine.class);
    private DomeMode mode = DomeMode.NONE;
    private String convertPath = null;
    private String fieldPath = null;
    private String strategyPath = null;
    private String outputPath = null;
    private ArrayList<String> models = new ArrayList();
    private boolean helpFlg = false;

    public void run(String[] args) {
        // Debug
        for (int i = 0; i < args.length; i++) {
            System.out.print("[" + args[i] + "]");
        }
        System.out.println();

        readCommand(args);
        if (!validate()) {
            printHelp();
            return;
        } else {
            debug();
        }

        LOG.info("Starting translation job");
        try {
            startTranslation();
        } catch (Exception ex) {
            LOG.error(getStackTrace(ex));
        }

        System.out.println("Done");
//        System.exit(0);

    }

    private void readCommand(String[] args) {
        int i = 0;
        int pathNum = 2;
        while (i < args.length && args[i].startsWith("-")) {
            if (args[i].equalsIgnoreCase("-n") || args[i].equalsIgnoreCase("-none")) {
                mode = DomeMode.NONE;
                pathNum = 2;
            } else if (args[i].equalsIgnoreCase("-f") || args[i].equalsIgnoreCase("-field")) {
                mode = DomeMode.FIELD;
                pathNum = 3;
            } else if (args[i].equalsIgnoreCase("-s") || args[i].equalsIgnoreCase("-strategy")) {
                mode = DomeMode.STRATEGY;
                pathNum = 4;
            } else if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("-help")) {
                helpFlg = true;
            } else {
                if (args[i].contains("d")) {
                    models.add(Model.DSSAT.toString());
                }
                if (args[i].contains("a")) {
                    models.add(Model.APSIM.toString());
                }
                if (args[i].contains("j")) {
                    models.add(Model.JSON.toString());
                }
            }
            i++;
        }
        try {
            if (pathNum > 1) {
                convertPath = args[i++];
            }
            if (pathNum > 2) {
                fieldPath = args[i++];
            }
            if (pathNum > 3) {
                strategyPath = args[i++];
            }
            if (i < args.length) {
                outputPath = args[i];
            } else {
                try {
                    outputPath = new File(convertPath).getCanonicalFile().getParent();
                } catch (IOException ex) {
                    outputPath = null;
                    LOG.error(getStackTrace(ex));
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            LOG.error("Path arguments are not enough for selected dome mode");
        }
    }

    private boolean validate() {

        if (!isValidPath(convertPath, true)) {
            LOG.warn("convert_path is invalid : " + convertPath);
            return false;
        } else if (!isValidPath(outputPath, false)) {
            LOG.warn("output_path is invalid : " + outputPath);
            return false;
        }

        if (mode.equals(DomeMode.NONE)) {
        } else if (mode.equals(DomeMode.FIELD)) {
            if (!isValidPath(fieldPath, true)) {
                LOG.warn("field_path is invalid : " + fieldPath);
                return false;
            }
        } else if (mode.equals(DomeMode.STRATEGY)) {
            if (!isValidPath(fieldPath, true)) {
                LOG.warn("field_path is invalid : " + fieldPath);
                return false;
            } else if (!isValidPath(strategyPath, true)) {
                LOG.warn("strategy_path is invalid : " + strategyPath);
                return false;
            }
        } else {
            LOG.warn("Unsupported mode option : " + mode);
            return false;
        }

        if (models.isEmpty()) {
            LOG.warn("<model_option> is required for running translation");
            return false;
        }

        return true;
    }

    private boolean isValidPath(String path, boolean isFile) {
        if (path == null) {
            return false;
        } else {
            File f = new File(path);
            if (isFile) {
                return f.isFile();
            } else {
                if (!path.matches(".*\\.\\w+$")) {
                    System.out.println("OK1");
                    return f.isDirectory();
                } else {
                    System.out.println("OK2");
                    return false;
                }
            }
        }
    }

    private void startTranslation() throws Exception {
        LOG.info("Importing data...");
        if (convertPath.endsWith(".json")) {
            try {
                // Load the JSON representation into memory and send it down the line.
                String json = new Scanner(new File(convertPath), "UTF-8").useDelimiter("\\A").next();
                HashMap data = fromJSON(json);

                if (mode.equals(DomeMode.NONE)) {
                    toOutput(data);
                } else {
                    LOG.debug("Attempting to apply a new DOME");
                    applyDome(data, mode.toString().toLowerCase());
                }
            } catch (Exception ex) {
                LOG.error(getStackTrace(ex));
            }
        } else {
            TranslateFromTask task = new TranslateFromTask(convertPath);
            HashMap data;
            try {
                data = task.execute();
            } catch (Exception e) {
                LOG.error(getStackTrace(e));
                return;
            }
            if (!data.containsKey("errors")) {
                if (mode.equals(DomeMode.NONE)) {
                    toOutput(data);
                } else {
                    applyDome(data, mode.toString().toLowerCase());
                }
            } else {
                LOG.error((String) data.get("errors"));
            }
        }
    }

    private void applyDome(HashMap map, String mode) {
        LOG.info("Applying DOME...");
        ApplyDomeTask task = new ApplyDomeTask(fieldPath, strategyPath, mode, map);
        HashMap data;
        try {
            data = task.execute();
        } catch (Exception e) {
            LOG.error(getStackTrace(e));
            return;
        }
        if (!data.containsKey("errors")) {
            //LOG.error("Domeoutput: {}", data.get("domeoutput"));
            toOutput((HashMap) data.get("domeoutput"));
        } else {
            LOG.error((String) data.get("errors"));
        }
    }

    private void toOutput(HashMap map) {
        LOG.info("Generating model input files...");

        if (models.size() == 1 && models.get(0).equals("JSON")) {
            DumpToJson task = new DumpToJson(convertPath, outputPath, map);
            try {
                task.execute();
                LOG.info("Translation completed");
            } catch (Exception e) {
                LOG.error(getStackTrace(e));
            }
        } else {
            if (models.indexOf("JSON") != -1) {
                DumpToJson task = new DumpToJson(convertPath, outputPath, map);
                try {
                    task.execute();
                } catch (Exception e) {
                    LOG.error(getStackTrace(e));
                }
            }
            TranslateToTask task = new TranslateToTask(models, map, outputPath);
            try {
                task.execute();
                LOG.info("=== Completed translation job ===");
            } catch (Exception e) {
                LOG.error(getStackTrace(e));
            }
        }
    }

    private void printHelp() {
        if (helpFlg) {
            System.out.println("***********************************************************************************************************************");
            System.out.println(" The arguments format : <dome_mode_option><model_option><convert_path><field_path><strategy_path><output_path>");
            System.out.println("\t<dome_mode_option>");
            System.out.println("\t\t-n | -none\tRaw Data Only, Default");
            System.out.println("\t\t-f | -filed\tField Overlay, will require Field Overlay File");
            System.out.println("\t\t-s | -strategy\tSeasonal Strategy, will require both Field Overlay and Strategy File");
            System.out.println("\t<model_option>");
            System.out.println("\t\t-d\t\tDSSAT");
            System.out.println("\t\t-a\t\tAPSIM");
            System.out.println("\t\t-j\t\tJSON");
            System.out.println("\t\t* Could be combined input like -daj or -dj");
            System.out.println("\t<convert_path>");
            System.out.println("\t\tThe path for file to be converted");
            System.out.println("\t<field_path>");
            System.out.println("\t\tThe path for file to be used for field overlay");
            System.out.println("\t<strategy_path>");
            System.out.println("\t\tThe path for file to be used for strategy");
            System.out.println("\t<output_path>");
            System.out.println("\t\tThe path for output.");
            System.out.println("\t\t* If not provided, will use convert_path");
            System.out.println("***********************************************************************************************************************");
        } else {
            System.out.println("\nType -h or -help for arguments info\n");
        }
    }

    private void debug() {
        System.out.println("Dome mode:\t" + mode);
        System.out.println("convertPath:\t" + convertPath);
        System.out.println("fieldPath:\t" + fieldPath);
        System.out.println("strategyPath:\t" + strategyPath);
        System.out.println("outputPath:\t" + outputPath);
        System.out.println("Models:\t\t" + models);
    }

    private static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}
