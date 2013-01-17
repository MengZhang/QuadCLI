package org.agmip.ui.quadcli;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.agmip.core.types.TranslatorInput;
import org.agmip.translators.agmip.AgmipInput;
import org.agmip.translators.csv.CSVInput;
import org.agmip.translators.dssat.DssatControllerInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslateFromTask {

    private static final Logger LOG = LoggerFactory.getLogger(TranslateFromTask.class);
    private String file;
    private TranslatorInput translator;

    public TranslateFromTask(String file) throws Exception {
        this.file = file;
        if (file.toLowerCase().endsWith(".zip")) {
            FileInputStream f = new FileInputStream(file);
            ZipInputStream z = new ZipInputStream(new BufferedInputStream(f));
            ZipEntry ze;
            while ((ze = z.getNextEntry()) != null) {
                if (ze.getName().toLowerCase().endsWith(".csv")) {
                    translator = new CSVInput();
                    break;
                }
                if (ze.getName().toLowerCase().endsWith(".wth")) {
                    translator = new DssatControllerInput();
                    break;
                }
                if (ze.getName().toLowerCase().endsWith(".agmip")) {
                    translator = new AgmipInput();
                    break;
                }
            }
            if (translator == null) {
                translator = new DssatControllerInput();
            }
            z.close();
            f.close();
        } else if (file.toLowerCase().endsWith(".agmip")) {
            translator = new AgmipInput();
        } else if (file.toLowerCase().endsWith(".csv")) {
            translator = new CSVInput();
        } else {
            LOG.error("Unsupported file: {}", file);
            throw new Exception("Unsupported file type");
        }
    }

    public HashMap<String, Object> execute() {
        HashMap<String, Object> output = new HashMap<String, Object>();
        try {
            output = (HashMap<String, Object>) translator.readFile(file);
            LOG.debug("Translate From Results: {}", output.toString());
            return output;
        } catch (Exception ex) {
            output.put("errors", ex.getMessage());
            return output;
        }
    }
}
