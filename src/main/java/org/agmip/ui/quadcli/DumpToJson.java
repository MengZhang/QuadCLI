package org.agmip.ui.quadcli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.pivot.util.concurrent.Task;
import org.apache.pivot.util.concurrent.TaskExecutionException;


import static org.agmip.util.JSONAdapter.toJSON;

public class DumpToJson extends Task<String> {
	private HashMap data;
	private String fileName, directoryName;
	
	public DumpToJson(String file, String dirName, HashMap data) {
		this.fileName = file;
		this.directoryName = dirName;
		this.data = data;
	}

	@Override
	public String execute() throws TaskExecutionException {
		FileWriter fw = null;
		BufferedWriter bw = null;
		File file = new File(fileName);
		String[] base = file.getName().split("\\.(?=[^\\.]+$)");
		String outputJson = directoryName+"/"+base[0]+".json";
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
		} catch (IOException ex ) {
			throw new TaskExecutionException(ex);
		}
		return null;
	}
}
