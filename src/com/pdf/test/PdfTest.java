package com.pdf.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfTest {

	
	public ByteArrayOutputStream generatePurchaseReturnPdf(String billNumber) throws IOException {
		PdfFromConfigTest generator = new PdfFromConfigTest();

		return generator.createLongscapePdf();
	}

	
	public static void main(String[] args) throws IOException {
		PdfTest t = new PdfTest();
		FileOutputStream fos = null;
		try {
		    fos = new FileOutputStream(new File("c:/work/deleteable/mypdf.pdf")); 
		    ByteArrayOutputStream baos = t.generatePurchaseReturnPdf("sd");

		    // Put data in your baos

		    baos.writeTo(fos);
		} catch(IOException ioe) {
		    // Handle exception here
		    ioe.printStackTrace();
		} finally {
		    fos.close();
		}
	}
	
}
