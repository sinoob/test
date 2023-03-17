package com.sparrow.nfr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

import com.sparrow.dbf.DBFLiterals;
import com.sparrow.dbf.dto.ItemData;

public class ItemMapManager {
	private static final ItemMapManager THIS = new ItemMapManager();
	private Collection<ItemData> list = new ArrayList<ItemData>();

	public static ItemMapManager instance() {
		return THIS;
	}

	private ItemMapManager() {
		String file = System.getProperty(DBFLiterals.ITEM_MAP_ARGUMENT);
		if (file == null) {
			file = DBFLiterals.ITEM_MAP_ARGUMENT + ".txt";
		}
		try {
			load(new FileInputStream(new File(file)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void load(InputStream in) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
		String line;
		try {
			while ((line = br.readLine()) != null) {
				String[] items = line.split(DBFLiterals.ITEM_LIST_SPLITTER);
				ItemData data = new ItemData(items[0], items[1]);
				list.add(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			br.close();
		}
	}

	public Collection<ItemData> getList() {
		return list;
	}

	public Collection<String> getItemCodes() {
		Collection<String> codes = new ArrayList<String>();
		for (ItemData d : list) {
			codes.add(d.getCode());
		}
		return codes;
	}

}
