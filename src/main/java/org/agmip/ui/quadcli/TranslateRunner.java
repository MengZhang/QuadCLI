package org.agmip.ui.quadcli;

import java.io.IOException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.agmip.core.types.TranslatorOutput;


public class TranslateRunner implements Runnable {
	private TranslatorOutput translator;
	private HashMap data;
	private String outputDirectory;
	private static Logger LOG = LoggerFactory.getLogger(TranslateRunner.class);
	
	public TranslateRunner(TranslatorOutput translator, HashMap data, String outputDirectory) {
		this.translator = translator;
		this.data = data;
		this.outputDirectory = outputDirectory;
	}
	
	
	@Override
	public void run() {
		LOG.debug("Starting new thread!");
		try {
			translator.writeFile(outputDirectory, data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}