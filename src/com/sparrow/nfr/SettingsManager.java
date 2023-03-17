package com.sparrow.nfr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.sparrow.dbf.DBFLiterals;

public class SettingsManager extends Properties {

	private static final long serialVersionUID = 8882608293565440939L;
	private static final SettingsManager THIS = new SettingsManager();

	public static SettingsManager instance() {
		return THIS;
	}

	private SettingsManager() {
		String file = System.getProperty(DBFLiterals.SETTINGS_ARGUMENT);
		if (file == null) {
			file = DBFLiterals.SETTINGS_ARGUMENT + ".properties";
		}
		try {
			load(new FileReader(new File(file)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
