package com.sparrow.soware;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.nmdm.framework.constants.BillType;
import com.nmdm.framework.constants.ConstantLiterals;
import com.nmdm.framework.constants.Literals;
import com.nmdm.framework.pool.SystemParameter;
import com.nmdm.soware.reader.DailySalesReader;
import com.nmdm.soware.vo.DailySalesVO;

public class DailySalesStats {

	private int billCount;
	private double cashSale;
	private double creditSale;
	private Date date;
	public final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/YY hh:mm a");

	private List<DailySalesVO> dailySales;

	public String generateDailySalesStats() throws IOException, ParseException {
		date = new Date();
		getDailySales();
		grepStats();
		return generateSummaryString();
	}

	private void getDailySales() throws IOException, ParseException {
		dailySales = new DailySalesReader().read(date);
	}

	private void grepStats() {
		billCount = 0;
		cashSale = 0;
		creditSale = 0;
		if (dailySales != null) {
			for (DailySalesVO vo : dailySales) {
				if (BillType.CASH == vo.getBillType()) {
					cashSale += vo.getAmount();
				} else {
					creditSale += vo.getAmount();
				}
				billCount++;
			}
		}
	}

	private String generateSummaryString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SALE at ");
		sb.append(DATE_FORMAT.format(date));
		sb.append(ConstantLiterals.NEW_LINE);
		sb.append("Total Sale : ");
		sb.append(Math.floor(cashSale) + Math.floor(creditSale));
		sb.append(ConstantLiterals.NEW_LINE);
		sb.append("Cash:").append(Math.floor(cashSale));
		sb.append(ConstantLiterals.NEW_LINE);
		sb.append("Credit:").append(Math.floor(creditSale));
		sb.append(ConstantLiterals.NEW_LINE);
		sb.append("Total no.of Bills:").append(billCount);
		return sb.toString();
	}
}
