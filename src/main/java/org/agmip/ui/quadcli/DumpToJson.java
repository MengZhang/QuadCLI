package org.agmip.ui.quadcli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import static org.agmip.util.JSONAdapter.toJSON;

public class DumpToJson {

    private HashMap data;
    private String fileName, directoryName;

    public DumpToJson(String file, String dirName, HashMap data) {
        this.fileName = file;
        this.directoryName = dirName;
        this.data = data;
    }

    public String execute() throws Exception {
        FileWriter fw;
        BufferedWriter bw;
        File file = new File(fileName);
        String[] base = file.getName().split("\\.(?=[^\\.]+$)");
        String outputJson = directoryName + "/" + base[0] + ".json";
        file = new File(outputJson);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            fw = new FileWriter(file.getAbsoluteFile());
            bw = new BufferedWriter(fw);
            bw.write(toJSON(data));
            bw.close();
            fw.close();
        } catch (IOException ex) {
            throw new Exception(ex);
        }
        return null;
    }
}
