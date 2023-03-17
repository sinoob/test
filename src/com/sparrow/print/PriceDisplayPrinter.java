package com.sparrow.print;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Collection;

import com.sparrow.dbf.DBFLiterals;
import com.sparrow.dbf.dto.ItemData;
import com.sparrow.nfr.SettingsManager;

public class PriceDisplayPrinter implements Printable {

	private Collection<ItemData> list;
	private static int rowHeight;
	private static int fontType;
	private static int fontSize;
	private static int displayStartPosition;
	private static int rateStartPosition;
	private static String lineRequired;
	private static int lineGap;

	public PriceDisplayPrinter(Collection<ItemData> list) {
		this.list = list;
		rowHeight = Integer.valueOf(SettingsManager.instance().getProperty(DBFLiterals.ROW_HEIGHT));
		fontType = Integer.valueOf(SettingsManager.instance().getProperty(DBFLiterals.FONT_TYPE));
		fontSize = Integer.valueOf(SettingsManager.instance().getProperty(DBFLiterals.FONT_SIZE));
		displayStartPosition = Integer.valueOf(SettingsManager.instance().getProperty(DBFLiterals.DISPLAY_START_POS));
		rateStartPosition = Integer.valueOf(SettingsManager.instance().getProperty(DBFLiterals.RATE_START_POS));
		lineRequired = SettingsManager.instance().getProperty(DBFLiterals.LINE_REQUIRED);
		lineGap = Integer.valueOf(SettingsManager.instance().getProperty(DBFLiterals.LINE_GAP));
	}

	@Override
	public int print(Graphics arg0, PageFormat arg1, int arg2) throws PrinterException {
		Graphics2D g = (Graphics2D) arg0;
		int row = 30;
		// g.setFont(new Font("AnjaliOldLipi", 1, 19));
		// g.setFont(new Font("Kartika", 1, 19));
		g.setFont(new Font("ML-NILA01", fontType, fontSize));
		for (ItemData d : list) {
			// g.drawString(row + "", 10, row);
			g.drawString(d.getDisplayName(), displayStartPosition, row);
			if (d.getRate() == null) {
				g.drawString(DBFLiterals.ITEM_RATE_NULL, rateStartPosition, row);
			} else {
				g.drawString(d.getRate(), rateStartPosition, row);
			}
			if ("Y".equals(lineRequired)) {
				g.drawLine(0, row + lineGap, (int) arg1.getWidth(), row + lineGap);
			}
			row += rowHeight;
		}
		// g.drawString("ബീ൯സ്", 100, 100);
		g.dispose();
		return PAGE_EXISTS;
	}
}
