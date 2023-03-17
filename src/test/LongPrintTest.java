package test;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.PageRanges;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.util.Precision;
import org.springframework.web.client.RestClientException;

import com.boonis.greenbook.business.CopyOfSalesBillPrintPainter;
import com.boonis.greenbook.business.PrintingService;
import com.boonis.greenbook.data.BillPrintConfigData;
import com.boonis.greenbook.data.SalesBillData;
import com.boonis.greenbook.data.SalesBillItemData;
import com.boonis.greenbook.data.SalesBillTaxData;
import com.boonis.greenbook.data.SalesBillTaxDetailData;
import com.boonis.greenbook.data.utils.SalesBillTaxSorter;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.sparrow.dbf.dto.ItemData;
import com.sparrow.print.PriceDisplayPrinter;

public class LongPrintTest {

	public static void main(String[] args) {
		int i = 0;
		for (PrintService s : PrinterJob.lookupPrintServices()) {
			System.out.println(i++ + "====" + s.getName());
		}

		if (args == null || args.length == 0) {
			System.out.println("first argument -> bill number");
			System.out.println("second argument -> printer number");
		} else {
			try {
				printBill(args[0], args[1]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void printBill(String string, String printerNumber) throws RestClientException, JsonParseException, JsonMappingException, IOException {
		String s = "http://2_counter:8080/barcodeBilling/";
		Reader r = new Reader(s);
		SalesBillData data;
		SalesBillTaxData taxData;
		List<BillPrintConfigData> configData;
		long billNumber = Long.valueOf(string);
		data = r.readBillData(billNumber);
		taxData = groupTaxInfo(data);
		configData = r.readConfigData().getBillPrintConigData();
		System.out.println(data);
		System.out.println(configData);
		new SparrowPrint(data, taxData, configData).print(Integer.valueOf(printerNumber));
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

}

class SparrowPrint {
	SalesBillData data;
	SalesBillTaxData taxData;
	List<BillPrintConfigData> configData;
	Collection<ItemData> itemsForRate;

	public SparrowPrint(SalesBillData s, SalesBillTaxData t, List<BillPrintConfigData> l) {
		data = s;
		taxData = t;
		configData = l;
		itemsForRate = new ArrayList<ItemData>();
		for (int i = 0; i < 300; i++) {
			String j = String.valueOf(i);
			ItemData d = new ItemData(j, j);
			itemsForRate.add(d);
		}
	}

	public void print(int printerNum) {
		PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
		aset.add(new PageRanges(1, 1));
		aset.add(new Copies(1));
		try {

			PrinterJob printJob = PrinterJob.getPrinterJob();
			// printJob.setPrintable(this);
			// PageFormat f = printJob.defaultPage();
			// printJob.setPrintService(printService[5]);
			printJob.setPrintService(PrinterJob.lookupPrintServices()[printerNum]);
			PageFormat f = printJob.defaultPage();
			Paper paper = new Paper();
			double paperHeight = Double.valueOf(2000.0);
			double paperWidth = Double.valueOf(200.0);
			paper.setSize(paperWidth, paperHeight);
			// paper.setSize(300, 700);
			paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
			f.setPaper(paper);
			// f.setOrientation(PageFormat.LANDSCAPE);
			// printJob.setPrintable(new CopyOfSalesBillPrintPainter(data,
			// taxData, configData), f);
			printJob.setPrintable(new XPriceDisplayPrinter(itemsForRate), f);

			// for my canon printer
			// printJob.setPrintService(printService[4]);
			// index of installed printers on you system
			// not sure if default-printer is always '0'
			printJob.print(aset);
		} catch (PrinterException err) {
			System.err.println(err);
		}
	}
}
