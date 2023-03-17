package com.pdf.test;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.boonis.greenbook.common.Literals;
import com.boonis.greenbook.data.AccountTxnData;
import com.boonis.greenbook.data.PrintTemplateConfigData;
import com.boonis.greenbook.data.SalesBillData;
import com.boonis.greenbook.data.SalesBillTaxData;
import com.inventeon.framework.utils.InventeonFormatterUtils;
import com.itextpdf.text.Font;

public class PdfFromConfigTest {
	private PDPageContentStream contentStream = null;
	private PDFont font = null;
	private PDDocument document = null;
	private PDPage page = null;

	ByteArrayOutputStream output = new ByteArrayOutputStream();

	int size = 15;
	
	
	private float delta = 20f;

	private static final int NAME_LENGTH = 20;
	private static final int RECORD_HEIGHT = 10;
	private static final float REDUCTION_CONSTANT = 4f;
	private static final String FONT_FAMILY = "Tahoma";
	
	
	
	
	public PdfFromConfigTest() {
	}

	

	public ByteArrayOutputStream createLongscapePdf() throws IOException {

		document = new PDDocument();
		for (int i = 0; i < 1; i++) {
			page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
			document.addPage(page);
			font = PDType1Font.HELVETICA;
			contentStream = new PDPageContentStream(document, page);

			float x = 30;
			float y = PDRectangle.A4.getWidth() - 30;

			float dx, dy;

			if(textColorHex != null){
				//g.setColor(Color.decode(textColorHex));
			}
			drawLine(2, 900);
			//g.drawLine(0, 3, 900, 3);
			drawLine(5, 900);
			//g.drawLine(0, 5, 900, 5);
			float ypos = createHeader();
			ypos = createReportRows(ypos);
			ypos = createReportSummary(ypos);
			ypos = createSubReportRows(ypos);
			ypos = createCreditSummary(ypos);
			ypos = createFooter(ypos);



			// Let's close the content stream
			contentStream.close();
		}

		// Finally Let's save the PDF
		document.save(output);
		document.close();

		return output;
	}

	

	private float drawItems(float xx, float yy) throws IOException {
		double totalQty = 0;
		double totalGrossAmt = 0;
		double totalGst = 0;
		double total = 0;
		float x = xx;
		float y = yy;
		drawString(x, y, "SlNo.", 9);
		drawString(x += 30, y, "Item Name", 9);
		drawString(x += 180, y, "HSN", 9);
		drawString(x += 50, y, "MRP", 9);
		drawString(x += 50, y, "Qty", 9);
		drawString(x += 50, y, "Rate", 9);
		drawString(x += 50, y, "GrossAmt", 9);
		drawString(x += 50, y, "SGST%", 9);
		drawString(x += 50, y, "SGST-Amt", 9);
		drawString(x += 50, y, "CGST%", 9);
		drawString(x += 50, y, "CGST-Amt", 9);
		drawString(x += 50, y, "TotalGST", 9);
		drawString(x += 60, y, "Total Amt", 9);

		int ind = 0;
		x = xx;
		y -= 10;
		drawLine(x, y);
		y -= 10;

		drawString(x, y, "", 9);
		drawString(x += 30, y, "Totals", 9);
		drawString(x += 180, y, "", 9);
		drawString(x += 50, y, "", 9);
		drawStringRight(x += 50, y, totalQty, 9);
		drawString(x += 50, y, "", 9);
		drawStringRight(x += 50, y, totalGrossAmt, 9);
		drawString(x += 50, y, "", 9);
		drawString(x += 50, y, "", 9);
		drawString(x += 50, y, "", 9);
		drawString(x += 50, y, "", 9);
		drawStringRight(x += 50, y, totalGst, 9);
		drawStringRight(x += 80, y, total, 9);
		return y;
	}

	private void drawStringRight(float x, float y, double val, int size) throws IOException {
		String myString = InventeonFormatterUtils.indianFormatMaxTwoDecimals(val);
		float text_width = (font.getStringWidth(myString) / 1000.0f) * size;
		drawString(x - text_width + 20, y, myString, size);
	}

	private void drawString(float x, float y, Object obj, int size) throws IOException {
		if (obj != null)
			drawString(x, y, String.valueOf(obj), size);
	}

	private void drawString(float x, float y, String text, int size) throws IOException {
		if (text != null) {
			contentStream.setNonStrokingColor(0, 0, 0); // black text
			contentStream.beginText();
			contentStream.setFont(font, size);
			contentStream.newLineAtOffset(x, y);
			contentStream.showText(text);
			contentStream.endText();
		}
	}

	private void drawLine(float x, float y) throws IOException {
		contentStream.moveTo(x, y);
		contentStream.lineTo(x + page.getMediaBox().getWidth() - 30, y);
		contentStream.stroke();
	}
	
	
	
	
	
	private SalesBillData salesBillData;
	private List<PrintTemplateConfigData> configData;
	private SalesBillTaxData taxData;
	private AccountTxnData txnData;
	private List<AccountTxnData> txnList;
	private String textColorHex;

	public PdfFromConfigTest(SalesBillData salesBillData, SalesBillTaxData taxData, List<PrintTemplateConfigData> configData) {
		this.salesBillData = salesBillData;
		this.configData = configData;
		this.taxData = taxData;
	}



	private float createCreditSummary(float ypos) throws IOException {
		if (txnData != null) {
			ypos += 10;
			return createStaticData("creditdata", ypos, txnData);
		} else if (txnList != null) {
			ypos += 20;
			ypos = createDataGeneric(ypos, "creditdata", txnList);
		}
		return ypos;
	}

	private float createSubReportRows(float ypos) throws IOException {
		return createDataGeneric(ypos, "taxdata", taxData.getTaxDetails());
	}

	private float createReportSummary(float ypos) throws IOException {
		return createStaticData("saleSummary", ypos, salesBillData);
	}

	private float createFooter(float ypos) throws IOException {
		return createStaticData("footer", ypos, salesBillData);
	}

	private float createReportRows(float ypos) throws IOException {
		return createDataGeneric(ypos + 3, "items", salesBillData.getSaleItemsList());
	}

	private float createHeader() throws IOException {
		return createStaticData("header", delta, salesBillData);
	}

	private float createStaticData(String type, float topCorrection, Object obj) throws IOException {
		float ypos = 0;
		float yposMax = 0;
		for (PrintTemplateConfigData data : configData) {
			if (data.getType().equals(type)) {
				//g.setFont(new Font(FONT_FAMILY, 0, data.getFontSize()));
				String val = data.getLabel();
				if (data.isExpressionFlag()) {
					try {
						val = obj.getClass().getMethod(data.getLabel()).invoke(obj).toString();
					} catch (Exception e) {
						val = Literals.BLANK;
					}
				}
				ypos = topCorrection + (data.getTop() / REDUCTION_CONSTANT);
				//g.drawString(data.getLeft() / REDUCTION_CONSTANT, ypos, data.getLeft() / REDUCTION_CONSTANT, ypos);
				drawString(data.getLeft() / REDUCTION_CONSTANT, ypos, val, 12);
				yposMax = Math.max(yposMax, ypos);
			}
		}
		return yposMax;
	}

	private float createDataGeneric(float topCorrection, String type, List<?> list) throws IOException {
		int i = 1;
		float ypos = 0, xpos, yposMax = topCorrection;

		for (Object item : list) {
			for (PrintTemplateConfigData data : configData) {
				if (data.getType().equals(type)) {

					//g.setFont(new Font(FONT_FAMILY, 0, data.getFontSize()));
					String val = data.getLabel();
					if (data.isExpressionFlag()) {
						try {
							val = item.getClass().getMethod(data.getLabel()).invoke(item).toString();
						} catch (Exception e) {
							val = Literals.BLANK;
						}
						/*
						 * if (val.length() > NAME_LENGTH) { val =
						 * val.substring(0, NAME_LENGTH); }
						 */
					}

					ypos = topCorrection + (data.getTop() / REDUCTION_CONSTANT);// +
																				// (i
																				// *
																				// RECORD_HEIGHT);
					if (data.isRightAlignFlag()) {
						//xpos = data.getLeft() / REDUCTION_CONSTANT - g.getFontMetrics().stringWidth(val);
						xpos = data.getLeft() / REDUCTION_CONSTANT ;
					} else {
						xpos = data.getLeft() / REDUCTION_CONSTANT;
					}

					//g.drawString(val, xpos, ypos);
					drawString(xpos, ypos, val, size);
					yposMax = Math.max(yposMax, ypos);
				}
			}
			topCorrection = yposMax + RECORD_HEIGHT;
			// yposMax = Math.max(topCorrection, ypos);
			i++;

		}
		return yposMax;
	}

	public AccountTxnData getTxnData() {
		return txnData;
	}

	public void setTxnData(AccountTxnData txnData) {
		this.txnData = txnData;
	}

	public void setTxnList(List<AccountTxnData> txnList) {
		this.txnList = txnList;
	}

	public void setTextColorHex(String textColorHex) {
		this.textColorHex = textColorHex;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	
}
