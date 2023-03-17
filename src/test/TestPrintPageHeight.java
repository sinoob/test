package test;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.PageRanges;

public class TestPrintPageHeight {

	public void print() throws PrinterException {
		Book pBook = new Book();
		PrintService ps = null;
		PrintService[] services = PrinterJob.lookupPrintServices();
		String printerName = "Pdf";
		for (PrintService service : services) {
			if (service.getName().toUpperCase().indexOf(printerName.toUpperCase()) >= 0) {
				ps = service;
				break;
			}
		}
		PrinterJob printJob = PrinterJob.getPrinterJob();
		
		printJob.setPrintService(ps);

		PageFormat f = printJob.defaultPage();
		Paper paper = f.getPaper();

		double paperWidth = 3.91d * 72d;
		double paperHeight = 98d * 72d;
		double margin = 1d * 10d;
		// margin = 1d * 2d;
		margin = 1d;

		paper.setSize(paperWidth, paperHeight);
		// paper.setImageableArea(margin, margin,paperWidth - (margin * 2), paperHeight
		// - (margin * 2) );
		paper.setImageableArea(margin, margin, paper.getWidth(), paper.getHeight());

		f.setPaper(paper);
		// f.setOrientation(PageFormat.PORTRAIT);

		//f.getPaper().setSize(100, 2700);
		//f.setPaper(new XPaper());
		
		printJob.setPrintable(new LinePrinter(f), f);
		
		//pBook.append(new LinePrinter(f), f);
		//pBook.setPage(0, new LinePrinter(f), f);
		
		PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
		aset.add(new PageRanges(1,1));
		aset.add(new Copies(1));
		printJob.setJobName(Math.random()+ "");
		//printJob.setPageable(pBook);
		printJob.print();
	}

	public static void main(String[] args) throws PrinterException {
		new TestPrintPageHeight().print();
	}
}

class LinePrinter implements Printable {

	private Graphics2D g;
	private PageFormat pf = null;

	public LinePrinter(PageFormat pf) {
		this.pf = pf;
	}
	
	@Override
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
		if(pageIndex > 0) {
			return NO_SUCH_PAGE;
		}
		
		//pageFormat = pf;
		//pageFormat.getPaper().setSize(200d, 6585d);
		System.out.println("Printing with height " + pageFormat.getPaper().getHeight());
		g = (Graphics2D) graphics;
		g.setColor(Color.black);

		
		System.out.println(pageFormat.getHeight());
		if (pageIndex > 0) {
			//return NO_SUCH_PAGE;
		}
		for (int i = 1; i < 200; i++) {
			g.drawString("" + i, 10, i * 10);
		}
		g.dispose();
		return PAGE_EXISTS;
	}

}

class XPaper extends Paper {
	public void Paper() {
		//mHeight = 10;
	}
}

class XPageFormat extends PageFormat {
	public XPaper mPaper = new XPaper();
}