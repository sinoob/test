package com.sparrow.print;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.PageRanges;

import org.apache.commons.math3.util.Precision;
import org.springframework.web.client.RestClientException;

import test.Reader;

import com.boonis.greenbook.business.CopyOfSalesBillPrintPainter;
import com.boonis.greenbook.common.SystemParameterType;
import com.boonis.greenbook.data.BillPrintConfigData;
import com.boonis.greenbook.data.PrintRequestData;
import com.boonis.greenbook.data.SalesBillData;
import com.boonis.greenbook.data.SalesBillItemData;
import com.boonis.greenbook.data.SalesBillTaxData;
import com.boonis.greenbook.data.SalesBillTaxDetailData;
import com.boonis.greenbook.data.utils.SalesBillTaxSorter;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.sparrow.dbf.DBFLiterals;
import com.sparrow.dbf.dto.ItemData;
import com.sparrow.dbf.reader.RateReader;
import com.sparrow.nfr.ItemMapManager;
import com.sparrow.nfr.SettingsManager;

public class PricePrintInvoker {
	private PrintService[] printService;

	private static Collection<ItemData> itemsForRate;

	public PricePrintInvoker() {
		this.printService = PrinterJob.lookupPrintServices();
		printPrinterDetails();
	}

	private void printPrinterDetails() {
		int i = 0;
		for (PrintService s : printService) {
			System.out.println(i++ + "====" + s.getName());
		}
	}

	public static void main(String[] args) throws IOException, ParseException {
		PricePrintInvoker inv = new PricePrintInvoker();

		itemsForRate = ItemMapManager.instance().getList();
		Collection<String> itemList = ItemMapManager.instance().getItemCodes();
		String software = SettingsManager.instance().getProperty(DBFLiterals.BILLING_SOFTWARE);
		String db = SettingsManager.instance().getProperty(DBFLiterals.BILLING_SERVER_DB);
		Collection<ItemData> sowareList = new RateReader(software, db).read(itemList);
		modifyRateDisplay(sowareList);
		//inv.print(Long.valueOf(args[0]));
		inv.printSalesBill(Long.valueOf(args[0]));
	}

	private static void modifyRateDisplay(Collection<ItemData> sowareList) {
		for (ItemData d : sowareList) {
			ItemData rateItem = getItemData(d.getCode());
			String r = d.getRate();
			if ("Y".equals(SettingsManager.instance().getProperty(DBFLiterals.PAISA_REQUIRED))) {
				if (r.length() < 6) {
					r = " " + r;
				}
			} else {
				r = r.substring(0, r.indexOf("."));
				if (r.length() < 3) {
					r = " " + r;
				}
			}
			rateItem.setRate(r);
		}
	}

	private static ItemData getItemData(String itemCode) {
		for (ItemData d : itemsForRate) {
			if (d.getCode().equals(itemCode)) {
				return d;
			}
		}
		return null;
	}

	public void print(long billNumber) throws RestClientException, JsonParseException, JsonMappingException, IOException {
		SalesBillData data;
		SalesBillTaxData taxData;
		List<BillPrintConfigData> configData;
		Collection<ItemData> itemsForRate;

		
		String s = "http://2_counter:8080/barcodeBilling/";
		Reader r = new Reader(s);
		data = r.readBillData(billNumber);
		taxData = groupTaxInfo(data);
		configData = r.readConfigData().getBillPrintConigData();
		
		
		PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
		aset.add(new PageRanges(1, 1));
		aset.add(new Copies(1));
		try {

			PrinterJob printJob = PrinterJob.getPrinterJob();
			// printJob.setPrintable(this);
			// PageFormat f = printJob.defaultPage();
			// printJob.setPrintService(printService[5]);
			int printerNum = Integer.valueOf(SettingsManager.instance().getProperty(DBFLiterals.PRINTER_NUMBER));
			printJob.setPrintService(printService[printerNum]);
			PageFormat f = printJob.defaultPage();
			Paper paper = new Paper();
			double paperHeight = Double.valueOf(SettingsManager.instance().getProperty(DBFLiterals.PAPER_HEIGHT));
			double paperWidth = Double.valueOf(SettingsManager.instance().getProperty(DBFLiterals.PAPER_WIDTH));
			paper.setSize(paperWidth, paperHeight);
			// paper.setSize(300, 700);
			paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
			f.setPaper(paper);
			// f.setOrientation(PageFormat.LANDSCAPE);
			// printJob.setPrintable(new PriceDisplayPrinter(itemsForRate), f);
			printJob.setPrintable(new CopyOfSalesBillPrintPainter(data, taxData, configData), f);

			// for my canon printer
			// printJob.setPrintService(printService[4]);
			// index of installed printers on you system
			// not sure if default-printer is always '0'
			printJob.print(aset);
		} catch (PrinterException err) {
			System.err.println(err);
		}
	}
	
	private static SalesBillTaxData groupTaxInfo(SalesBillData salesBillData) {
		double totalTax = 0;
		SalesBillTaxData taxData = new SalesBillTaxData();
		Map<Double, SalesBillTaxDetailData> map = new HashMap<>();
		SalesBillTaxDetailData ref;
		for (SalesBillItemData item : salesBillData.getSaleItemsList()) {
			if (map.containsKey(item.getTaxPercentage())) {
				ref = map.get(item.getTaxPercentage());
				ref.setTotalTaxAmount(ref.getTotalTaxAmount() + item.getTaxAmount());
				// TODO for time being.. this needs to change
				ref.setCentralTaxAmount(ref.getTotalTaxAmount() / 2);
				ref.setStateTaxAmount(ref.getTotalTaxAmount() / 2);
			} else {
				ref = new SalesBillTaxDetailData();
				ref.setTotalTaxAmount(item.getTaxAmount());
				// TODO for time being.. this needs to change
				ref.setCentralTaxAmount(ref.getTotalTaxAmount() / 2);
				ref.setStateTaxAmount(ref.getTotalTaxAmount() / 2);
				ref.setTaxPercentage(item.getTaxPercentage());
				map.put(item.getTaxPercentage(), ref);
			}
			totalTax += item.getTaxAmount();
		}
		List<SalesBillTaxDetailData> taxList = new ArrayList<>(map.values());
		if (taxList != null) {
			Collections.sort(taxList, new SalesBillTaxSorter());
		}
		/*
		 * for rounding the tax data
		 */
		for (SalesBillTaxDetailData t : taxList) {
			t.setCentralTaxAmount(Precision.round(t.getCentralTaxAmount(), 2));
			t.setStateTaxAmount(Precision.round(t.getStateTaxAmount(), 2));
			t.setTotalTaxAmount(Precision.round(t.getTotalTaxAmount(), 2));
		}
		taxData.setTaxDetails(taxList);
		salesBillData.setTotalTaxAmount(Precision.round(totalTax, 2));
		return taxData;
	}
	
	public void printSalesBill( long billNumber ) throws RestClientException, JsonParseException, JsonMappingException, IOException {
		
		SalesBillData data;
		SalesBillTaxData taxData;
		List<BillPrintConfigData> configData;
		Collection<ItemData> itemsForRate;

		
		String s = "http://2_counter:8080/barcodeBilling/";
		Reader r = new Reader(s);
		data = r.readBillData(billNumber);
		taxData = groupTaxInfo(data);
		configData = r.readConfigData().getBillPrintConigData();
		
		
		PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
		aset.add(new PageRanges(1, 1));
		aset.add(new Copies(1));
		try {

			PrinterJob printJob = PrinterJob.getPrinterJob();
			// printJob.setPrintable(this);
			// PageFormat f = printJob.defaultPage();
			// printJob.setPrintService(printService[5]);
			int printerNum = Integer.valueOf(SettingsManager.instance().getProperty(DBFLiterals.PRINTER_NUMBER));
			printJob.setPrintService(printService[printerNum]);
			PageFormat f = printJob.defaultPage();
			Paper paper = new Paper();
			double paperHeight = Double.valueOf(SettingsManager.instance().getProperty(DBFLiterals.PAPER_HEIGHT));
			double paperWidth = Double.valueOf(SettingsManager.instance().getProperty(DBFLiterals.PAPER_WIDTH));
			paper.setSize(paperWidth, paperHeight);
			// paper.setSize(300, 700);
			paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
			f.setPaper(paper);
			// f.setOrientation(PageFormat.LANDSCAPE);
			// printJob.setPrintable(new PriceDisplayPrinter(itemsForRate), f);
			printJob.setPrintable(new CopyOfSalesBillPrintPainter(data, taxData, configData), f);

			// for my canon printer
			// printJob.setPrintService(printService[4]);
			// index of installed printers on you system
			// not sure if default-printer is always '0'
			printJob.print(aset);
		} catch (PrinterException err) {
			System.err.println(err);
		}
	}
	public void printSalesBill2( long billNumber ) throws RestClientException, JsonParseException, JsonMappingException, IOException {
		
		SalesBillData data;
		SalesBillTaxData taxData;
		List<BillPrintConfigData> configData;
		Collection<ItemData> itemsForRate;
		
		
		String s = "http://2_counter:8080/barcodeBilling/";
		Reader r = new Reader(s);
		data = r.readBillData(billNumber);
		taxData = groupTaxInfo(data);
		configData = r.readConfigData().getBillPrintConigData();
		
		PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
		aset.add(new PageRanges(1, 1));
		aset.add(new Copies(1));
		try {
			PrinterJob printJob = PrinterJob.getPrinterJob();
			double paperWidth = 3 * 72d; // 3 inch
			double paperHeight = 700f;
			String paperHeightVal = "2000.0";
			if (paperHeightVal != null) {
				paperHeight = Double.valueOf(paperHeightVal);
			}
			// double paperHeight = 4 * 72d; // 4 inch
			PrintService service = PrinterJob.lookupPrintServices()[2];
			printJob.setPrintService(service);
			PageFormat f = printJob.defaultPage();
			Paper paper = new Paper();
			paper.setSize(paperWidth, 2000f);
			paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
			f.setPaper(paper);
			printJob.setPrintable(new CopyOfSalesBillPrintPainter(data, taxData, configData), f);
			printJob.print(aset);
		} catch (PrinterException err) {
			err.printStackTrace();
		}
	}
}
