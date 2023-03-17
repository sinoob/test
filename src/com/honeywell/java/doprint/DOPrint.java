package com.honeywell.java.doprint;

import java.awt.EventQueue;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import java.awt.TextArea;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.*;

import honeywell.printer.*;
import honeywell.connection.*;
import honeywell.connection.Connection_Serial.StopBits;
import honeywell.printer.DocumentDPL.ImageType;
import honeywell.printer.DocumentExPCL_PP.PaperWidth;
import honeywell.printer.ParametersDPL.DoubleByteSymbolSet;
import honeywell.printer.ParametersExPCL_LP.BarcodeExPCL_LP;
import honeywell.printer.ParametersExPCL_LP.GS1DataBar;
import honeywell.printer.ParametersExPCL_PP.BarcodeExPCL_PP;
import honeywell.printer.ParametersExPCL_PP.RotationAngle;
import honeywell.printer.configuration.dpl.*;
import honeywell.printer.configuration.dpl.MemoryModules_DPL.FileInformation;
import honeywell.printer.configuration.dpl.PrinterStatus_DPL.PrinterStatus;
import honeywell.printer.configuration.ez.*;
import honeywell.printer.configuration.ez.GeneralConfiguration.*;
import honeywell.printer.configuration.expcl.*;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class DOPrint implements Runnable {

	private String m_deviceAddress = "192.168.101.105";
    private int m_devicePort = 515;
    private String m_serialPort = "COM1";
    private int m_printHeadWidth = 384;

    private ConnectionBase conn = null;

    private DocumentDPL docDPL;
    private DocumentEZ docEZ;
    private DocumentLP docLP;
    private DocumentExPCL_LP docExPCL_LP;
    private DocumentExPCL_PP docExPCL_PP;
    private ParametersEZ paramEZ;
    private ParametersDPL paramDPL;
    private ParametersExPCL_LP paramExPCL_LP;
    private ParametersExPCL_PP paramExPCL_PP;
    private byte[] printData;

    private int selectedItemIndex;
    Preferences prefs = Preferences.userNodeForPackage(com.honeywell.java.doprint.DOPrint.class);
    //private List<String> itemsArray = new ArrayList<String>();
    private List<String> selectedFilesList = new ArrayList<String>();
    private JFileChooser fileDlg;
    String connType = "";
    String m_printerMode = "";
    
	private JFrame frmDoPrint;
	
	private JTextField m_addressTextField;
	private JComboBox<String>m_portComboBox;
	private JRadioButton m_printRadio;
	private JRadioButton m_queryRadio;
	private JButton m_browseButton;
	private JComboBox<String> m_connComboBox;
	private JComboBox<String> m_printerLanguageComboBox;
	private JComboBox<String> m_printHeadCmbo;
	private JComboBox<String> m_printItemsComboBox;
	private JButton m_performButton;
	private JButton m_saveButton;
	private TextArea m_statusBox;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DOPrint window = new DOPrint();
					window.frmDoPrint.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @throws Exception 
	 */
	public DOPrint() throws Exception {
		initialize();
		reloadItemsArray();
		
		//==========Load settings========/
		m_connComboBox.setSelectedIndex(prefs.getInt("Connection",m_connComboBox.getSelectedIndex()));
		m_deviceAddress = prefs.get("Device IP", m_deviceAddress);
		m_serialPort = prefs.get("COM Port", m_serialPort);
		m_devicePort = prefs.getInt("Port",m_devicePort);
		m_printerLanguageComboBox.setSelectedIndex(prefs.getInt("Language",m_printerLanguageComboBox.getSelectedIndex()));
		m_printHeadCmbo.setSelectedIndex(prefs.getInt("PrintHead Width",m_printHeadCmbo.getSelectedIndex()));
		m_printRadio.setSelected(prefs.getBoolean("Print Action", true));
		m_queryRadio.setSelected(!(prefs.getBoolean("Print Action", true)));
		
		connType = m_connComboBox.getSelectedItem().toString();
		m_printerMode = m_printerLanguageComboBox.getSelectedItem().toString();
		m_addressTextField.setText(m_deviceAddress);
		
		//Check the connection UI state
		 if (connType == "TCP/IP")
         {
			 m_addressTextField.setEnabled(true);
             m_portComboBox.insertItemAt(String.valueOf(m_devicePort),1);
             m_portComboBox.setSelectedItem(String.valueOf(m_devicePort));
         }
         else if (connType == "Serial")
         {
        	 m_addressTextField.setEnabled(false); 
        	 m_portComboBox.setSelectedItem(m_serialPort);
         }
		 
		 //check the action UI state
		 if (m_printRadio.isSelected())
         {
             m_performButton.setText("Print");
             m_browseButton.setVisible(true);
             reloadItemsArray();
         }
         else if (m_queryRadio.isSelected())
         {
             m_performButton.setText("Query");
             m_browseButton.setVisible(false);
             reloadItemsArray();
         }

		
		//Connection
		m_connComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				connType = m_connComboBox.getSelectedItem().toString();
				if (connType == "TCP/IP") 
				{ 
					m_addressTextField.setEnabled(true);
					m_addressTextField.setText(m_deviceAddress);
					m_portComboBox.setSelectedItem(String.valueOf(m_devicePort));
					}
				else if (connType == "Serial") 
				{ 
					m_addressTextField.setEnabled(false); 
					m_portComboBox.setSelectedItem(m_serialPort);
				} 
			}
		});
		
		//On printer mode select
		m_printerLanguageComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				m_printerMode = m_printerLanguageComboBox.getSelectedItem().toString();
				reloadItemsArray();
				
			}
		});
		
		//On print radio checked
		m_printRadio.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
	            if (m_printRadio.isSelected())
	            {
	                m_performButton.setText("Print");
	                m_browseButton.setVisible(true);
	            }
	            else
	            {
	                m_performButton.setText("Query");
	                m_browseButton.setVisible(false);
	            }
	            
	            reloadItemsArray();
			}
		});
		
		//On perform button click
		m_performButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				m_statusBox.setText("");
				try {
					m_performButton.setEnabled(false);
					selectedItemIndex = m_printItemsComboBox.getSelectedIndex();
					docDPL = new DocumentDPL();
	                docEZ = new DocumentEZ("MF204");
	                docLP = new DocumentLP("!");
	                docExPCL_LP = new DocumentExPCL_LP(3);
	                docExPCL_PP = new DocumentExPCL_PP(PaperWidth.PaperWidth_384);

	                paramEZ = new ParametersEZ();
	                paramDPL = new ParametersDPL();
	                paramExPCL_LP = new ParametersExPCL_LP();
	                paramExPCL_PP = new ParametersExPCL_PP();
	                
	              //if we are printing
					if (m_printRadio.isSelected()) {
						//Checks current Mode
						if(m_printerMode.equals("EZ"))
						{	
							//3-in sample
							if (selectedItemIndex == 0) 
							{
								//=============GENERATING RECEIPT====================================//
								 docEZ.writeText("For", 1, 200);
				                    
				                //Bold delivery
				                paramEZ.setIsBold(true);
				                docEZ.writeText("Delivery", 1,240,paramEZ);
				                    
				                //print image on same Delivery line
				                docEZ.writeImage("DOLGO", 1, 350);
			                    docEZ.writeText("Customer Code: 00146",50,1);
			                    docEZ.writeText("Address: Manila",75,1);
			                    docEZ.writeText("Tin No.: 27987641",100,1);
			                    docEZ.writeText("Area Code: PN1-0004",125,1);
			                    docEZ.writeText("Business Style: SUPERMARKET A",150,1);
			                    
			                    docEZ.writeText("PRODUCT CODE  PRODUCT DESCRIPTION         QTY.  Delivr." ,205,1);
			                    docEZ.writeText("------------  --------------------------  ----  -------",230,1);
			                    docEZ.writeText("    111       Wht Bread Classic 400g       51      51  ",255,1);
			                    docEZ.writeText("    112       Clsc Wht Bread 600g          77      77  ",280,1);
			            		docEZ.writeText("    113       Wht Bread Clsc 600g          153     25  ",305,1);
			            		docEZ.writeText("    121       H Fiber Wheat Bread 600g     144     77  ",330,1);
			            		docEZ.writeText("    122       H Fiber Wheat Bread 400g     112     36  ",355,1);
			            		docEZ.writeText("    123       H Calcium Loaf 400g          81      44  ",380,1);
			            		docEZ.writeText("    211       California Raisin Loaf       107     44  ",405,1);
			            		docEZ.writeText("    212       Chocolate Chip Loaf          159     102 ",430,1);
			            		docEZ.writeText("    213       Dbl Delights(Ube & Chse)     99      80  ",455,1);
			            		docEZ.writeText("    214       Dbl Delights(Choco & Mocha)  167     130 ",480,1);
			            		docEZ.writeText("    215       Mini Wonder Ube Cheese       171     179 ",505,1);
			            		docEZ.writeText("    216       Mini Wonder Ube Mocha        179     100 ",530,1);
								docEZ.writeText("  ",580,1);
								printData = docEZ.getDocumentData();
								//======================================================================//
							}
							
							//4-in sample
							else if (selectedItemIndex == 1) {
								docEZ.writeText("For Delivery", 1, 300);
			                    docEZ.writeText("Customer Code: 00146",50,1);
			                    docEZ.writeText("Address: Manila",75,1);
			                    docEZ.writeText("Tin No.: 27987641",100,1);
			                    docEZ.writeText("Area Code: PN1-0004",125,1);
			                    docEZ.writeText("Business Style: SUPERMARKET A",150,1);
			                    
			                    docEZ.writeText("PRODUCT CODE      PRODUCT DESCRIPTION             QTY.    Delivered ",205,1);
			                    docEZ.writeText("------------      --------------------------      ----    ----------",230,1);
			                    docEZ.writeText("    111           Wht Bread Classic 400g           51          51   ",255,1);
			                    docEZ.writeText("    112           Clsc Wht Bread 600g              77          77   ",280,1);
			            		docEZ.writeText("    113           Wht Bread Clsc 600g              153         25   ",305,1);
			            		docEZ.writeText("    121           H Fiber Wheat Bread 600g         144         77   ",330,1);
			            		docEZ.writeText("    122           H Fiber Wheat Bread 400g         112         36   ",355,1);
			            		docEZ.writeText("    123           H Calcium Loaf 400g              81          44   ",380,1);
			            		docEZ.writeText("    211           California Raisin Loaf           107         44   ",405,1);
			            		docEZ.writeText("    212           Chocolate Chip Loaf              159         102  ",430,1);
			            		docEZ.writeText("    213           Dbl Delights(Ube & Chse)         99          80   ",455,1);
			            		docEZ.writeText("    214           Dbl Delights(Choco & Mocha)      167         130  ",480,1);
			            		docEZ.writeText("    215           Mini Wonder Ube Cheese           171         179  ",505,1);
			            		docEZ.writeText("    216           Mini Wonder Ube Mocha            179         100  ",530,1);
								docEZ.writeText("  ",580,1);
								printData = docEZ.getDocumentData();
							}
							//Barcode Sample
							else if (selectedItemIndex == 2)
							{
								paramEZ.setHorizontalMultiplier(1);
			                    paramEZ.setVerticalMultiplier(2);
			                    
			                    //write GS1 barcodes with 2d Composite data
			                    int pixelMult = 3;

			                    docEZ.writeText("GS1 Barcode",1,1);
			                    docEZ.writeBarCodeGS1DataBar("GSONE","123456789","123",pixelMult,pixelMult,1,1,22,30,1,paramEZ);
			                    
			                    docEZ.writeText("GS1 Truncated",330,1);
			                    docEZ.writeBarCodeGS1DataBar("GS1TR","123456789","123",pixelMult,pixelMult,1,1,22,360,1,paramEZ);
			                    
			                    docEZ.writeText("GS1 Limited",530,1);
			                    docEZ.writeBarCodeGS1DataBar("GS1LM","123456789","123",pixelMult,pixelMult,1,1,22,560,1,paramEZ);
			                    
			                    docEZ.writeText("GS1 Stacked",730,1);
			                    docEZ.writeBarCodeGS1DataBar("GS1ST","123456789","123",pixelMult,pixelMult,1,1,22,760,1,paramEZ);
			                    
			                    
			                    docEZ.writeText("GS1 Stacked Omnidirection",930,1);
			                    docEZ.writeBarCodeGS1DataBar("GS1SO","123456789","123",pixelMult,pixelMult,1,1,22,960,1,paramEZ);
			                    
			                    docEZ.writeText("GS1 Expanded",1530,1);
			                    docEZ.writeBarCodeGS1DataBar("GS1EX","ABCDEFGHIJKL","helloWorld!123",pixelMult,2*pixelMult,1,1,4,1560,1,paramEZ);
			                    
			                    paramEZ.setHorizontalMultiplier(2);
			                    paramEZ.setVerticalMultiplier(10);
			                    //Interleave 2of 5 barcode ratio 2:1
			                    docEZ.writeText("Interleave 2of5 Barcode ratio 2:1",2230,1);
			                    docEZ.writeBarCode("BCI25","0123456789",2260,1,paramEZ);
			                    
			                    //barcode 128
			                    docEZ.writeText("Barcode 128",2330,1);
			                    docEZ.writeBarCode("BC128","00010203040506070809",2360,1,paramEZ);
			                    
			                    //barcode EAN 128
			                    docEZ.writeText("EAN 128",2430,1);
			                    docEZ.writeBarCode("EN128","00010203040506070809",2460,1,paramEZ);
			                    
			                    //Code 39 barcodes
			                    docEZ.writeText("Code 39 Barcodes",2530,1);
			                    docEZ.writeBarCode("BC39N","0123456789",2560,1,paramEZ);
			                    docEZ.writeBarCode("BC39W","0123456789",2660,1,paramEZ);
			                    
			                    //Code 93 barcode
			                    docEZ.writeText("Code 93",2730,1);
			                    docEZ.writeBarCode("BC093","0123456789",2760,1,paramEZ);
			                    
			                    //Codabar
			                    docEZ.writeText("CODABAR",2830,1);
			                    docEZ.writeBarCode("COBAR","00010203040506070809",2860,1,paramEZ);
			                    
			                    //8 digit europe art num
			                    docEZ.writeText("8 DIGIT EUROPE ART NUM",2930,1);
			                    docEZ.writeBarCode("EAN08","0123456",2960,1,paramEZ);
			                    
			                    //13 digit europ art num
			                    docEZ.writeText("13 DIGIT Europe Art Num",3030,1);
			                    docEZ.writeBarCode("EAN13","000123456789",3060,1,paramEZ);
			                    
			                    //INTLV 2of5
			                    docEZ.writeText("Interleaved 2of5",3130,1);
			                    docEZ.writeBarCode("I2OF5","0123456789",3160,1,paramEZ);
			                    
			                    //PDF417
			                    docEZ.writeText("PDF417",3230,1);
			                    docEZ.writeBarCodePDF417("00010203040506070809", 3260, 1, 2, 1, paramEZ);
			                    
			                    //Plessy
			                    docEZ.writeText("Plessy",3350,1);
			                    docEZ.writeBarCode("PLESY","8052",3380,1,paramEZ);
			                    
			                    //UPC-A
			                    docEZ.writeText("UPC-A",3450,1);
			                    docEZ.writeBarCode("UPC-A","01234567890",3480,1,paramEZ);
			                    
			                    //UPC-E
			                    docEZ.writeText("UPC-E",3550,1);
			                    docEZ.writeBarCode("UPC-E","0123456",3580,1,paramEZ);
			                    
			                    paramEZ.setHorizontalMultiplier(10);
			                    
			                    paramEZ.setVerticalMultiplier(1);
			                    //QR
			                    docEZ.writeText("QR Barcode Manual Formating",3650,1);
			                    docEZ.writeBarCodeQRCode("N0123456789,B0004(&#),QR//BARCODE",2,9,1,3680,1,paramEZ);
			                    
			                    docEZ.writeText("QR Barcode Auto Formatting 1",3950,1);
			                    docEZ.writeBarCodeQRCode("0123456789012345678901234567890123456789",2,9,0,3980,1,paramEZ);
			                    
			                    paramEZ.setHorizontalMultiplier(8);
			                    docEZ.writeText("QR Barcode Auto Formatting 2",4250,1);
			                    docEZ.writeBarCodeQRCode("0123456789ABCDE",2,9,0,4280,1,paramEZ);
			                    
			                    //Aztec
			                    docEZ.writeText("Aztec",4550,1);
			                    docEZ.writeBarCodeAztec("Code 2D!",104,4580,1,paramEZ);
			                    docEZ.writeText("",4500,1);
			                    printData = docEZ.getDocumentData();   
							}
							//User selected an unpredefine item(eg from browsing file)
			                else
			                {
			                	String selectedItem = (String)m_printItemsComboBox.getSelectedItem();
			                	BufferedImage anImage = null;
			                	//Check if item is an image
			                	String[] okFileExtensions =  new String[] {".jpg", ".png", ".gif",".jpeg",".bmp", ".tif", ".tiff",".pcx"};
			                	for (String extension : okFileExtensions) {
									if(selectedItem.toLowerCase(Locale.US).endsWith(extension))
									{
										anImage = ImageIO.read(new File(selectedItem));
										break;
									}
								}
			                	//selected item is not an image file
			                	if (selectedItem.toLowerCase(Locale.US).endsWith(".pdf"))
			                	{
			                		docLP.writePDF(selectedItem, m_printHeadWidth);
			                		printData = docLP.getDocumentData();
			                	}
			                	//selected item is not an image file
			                	else if (anImage == null)
			                	{
			                		File file = new File(selectedItem);
			                		byte[] readBuffer = new byte[(int)file.length()];
			                		InputStream inputStream= new BufferedInputStream(new FileInputStream(file));
			                		inputStream.read(readBuffer);
			                		inputStream.close();
			                		printData = readBuffer;
			                	}
			                	else 
			                	{
			                		m_statusBox.append("Processing image..\r\n");
									docLP.writeImage(anImage, m_printHeadWidth);
									printData = docLP.getDocumentData();
								}
			                }
						}
						//for LP 
						else if(m_printerMode.equals("LP"))
						{
							//3-inch sample to generate
			                if(selectedItemIndex == 0)
			                {
			                    docLP.writeText("                   For Delivery");
			                    docLP.writeText(" ");
			                    docLP.writeText("Customer Code: 00146");
			                    docLP.writeText("Address: Manila");
			                    docLP.writeText("Tin No.: 27987641");
			                    docLP.writeText("Area Code: PN1-0004");
			                    docLP.writeText("Business Style: SUPERMARKET A");
			                    docLP.writeText(" ");
			                    docLP.writeText("PRODUCT CODE   PRODUCT DESCRIPTION          QTY.  Delivr.");
			                    docLP.writeText("------------   --------------------------   ----  -------");
			                    docLP.writeText("    111        Wht Bread Classic 400g        51     51   ");
			                    docLP.writeText("    112        Clsc Wht Bread 600g           77     77   ");
			                    docLP.writeText("    113        Wht Bread Clsc 600g           153    25   ");
			                    docLP.writeText("    121        H Fiber Wheat Bread 600g      144    77   ");
			                    docLP.writeText("    122        H Fiber Wheat Bread 400g      112    36   ");
			                    docLP.writeText("    123        H Calcium Loaf 400g           81     44   ");
			                    docLP.writeText("    211        California Raisin Loaf        107    44   ");
			                    docLP.writeText("    212        Chocolate Chip Loaf           159    102  ");
			                    docLP.writeText("    213        Dbl Delights(Ube & Chse)      99     80   ");
			                    docLP.writeText("    214        Dbl Delights(Choco & Mocha)   167    130  ");
			                    docLP.writeText("    215        Mini Wonder Ube Cheese        171    79   ");
			                    docLP.writeText("    216        Mini Wonder Ube Mocha         179    100  ");
			                    docLP.writeText("  ");
			                    docLP.writeText("  ");
			                    printData = docLP.getDocumentData();
			                }
			                //4-inch sample to generate
			                else if(selectedItemIndex == 1)
			                {
			                    docLP.writeText("                            For Delivery");
			                    docLP.writeText(" ");
			                    docLP.writeText("Customer Code: 00146");
			                    docLP.writeText("Address: Manila");
			                    docLP.writeText("Tin No.: 27987641");
			                    docLP.writeText("Area Code: PN1-0004");
			                    docLP.writeText("Business Style: SUPERMARKET A");
			                    docLP.writeText(" ");
			                    docLP.writeText("PRODUCT CODE         PRODUCT DESCRIPTION          QTY.    Delivered");
			                    docLP.writeText("------------      --------------------------      ----    ---------- ");
			                    docLP.writeText("    111           Wht Bread Classic 400g           51         51     ");
			                    docLP.writeText("    112           Clsc Wht Bread 600g              77         77     ");
			                    docLP.writeText("    113           Wht Bread Clsc 600g              153        25     ");
			                    docLP.writeText("    121           H Fiber Wheat Bread 600g         144        77     ");
			                    docLP.writeText("    122           H Fiber Wheat Bread 400g         112        36     ");
			                    docLP.writeText("    123           H Calcium Loaf 400g              81         44     ");
			                    docLP.writeText("    211           California Raisin Loaf           107        44     ");
			                    docLP.writeText("    212           Chocolate Chip Loaf              159        102    ");
			                    docLP.writeText("    213           Dbl Delights(Ube & Chse)         99         80     ");
			                    docLP.writeText("    214           Dbl Delights(Choco & Mocha)      167        130    ");
			                    docLP.writeText("    215           Mini Wonder Ube Cheese           171        179    ");
			                    docLP.writeText("    216           Mini Wonder Ube Mocha            179        100    ");
			                    docLP.writeText("  ");
			                    docLP.writeText("  ");
			                    printData = docLP.getDocumentData();
			                }
			                //Print Image
			                else if(selectedItemIndex == 2)
			                {
			                	m_statusBox.append("Processing image..\r\n");
			                    BufferedImage anImage = ImageIO.read(getClass().getResourceAsStream("dologo.png"));
			                    
			                    docLP.writeImage(anImage,384);
			                    printData = docLP.getDocumentData();
			                }
			                else if(selectedItemIndex == 3)
			                {
			                	m_statusBox.append("Processing image..\r\n");
			                    BufferedImage anImage = ImageIO.read(getClass().getResourceAsStream("dologo.png"));
			                    
			                    docLP.writeImage(anImage,576);
			                    printData = docLP.getDocumentData();
			                }
			                else if(selectedItemIndex == 4)
			                {
			                	m_statusBox.append("Processing image..\r\n");
			                    BufferedImage anImage = ImageIO.read(getClass().getResourceAsStream("dologo.png"));
			                    
			                    docLP.writeImage(anImage,832);
			                    printData = docLP.getDocumentData();
			                }
			                //User selected an unpredefine item(eg from browsing file)
			                else
			                {
			                	String selectedItem = (String)m_printItemsComboBox.getSelectedItem();
			                	BufferedImage anImage = null;
			                	//Check if item is an image
			                	String[] okFileExtensions =  new String[] {".jpg", ".png", ".gif",".jpeg",".bmp", ".tif", ".tiff",".pcx"};
			                	for (String extension : okFileExtensions) {
									if(selectedItem.toLowerCase(Locale.US).endsWith(extension))
									{
										anImage = ImageIO.read(new File(selectedItem));
										break;
									}
								}
			                	//selected item is not an image file
			                	if (selectedItem.toLowerCase(Locale.US).endsWith(".pdf"))
			                	{
			                		docLP.writePDF(selectedItem, m_printHeadWidth);
			                		printData = docLP.getDocumentData();
			                	}
			                	//selected item is not an image file
			                	else if (anImage == null)
			                	{
			                		File file = new File(selectedItem);
			                		byte[] readBuffer = new byte[(int)file.length()];
			                		InputStream inputStream= new BufferedInputStream(new FileInputStream(file));
			                		inputStream.read(readBuffer);
			                		inputStream.close();
			                		printData = readBuffer;
			                	}
			                	else 
			                	{
			                		m_statusBox.append("Processing image..\r\n");
									docLP.writeImage(anImage, m_printHeadWidth);
									printData = docLP.getDocumentData();
								}
			                }
						}
						//for EXPCL(Apex Printers)
						else if (m_printerMode.equals("ExPCL_LP"))
						{
							 boolean TEST_PAPER_ADVANCE = false;

					        // TEXT SAMPLES
					        if(selectedItemIndex == 0) {
						        docExPCL_LP.writeText("Hello World I am a printing sample");
						        paramExPCL_LP.setFontIndex(5);
						        docExPCL_LP.writeText("Hello World I am a printing sample (Font - K5)",paramExPCL_LP);
						        paramExPCL_LP.setFontIndex(3);
						        paramExPCL_LP.setIsBold(true);
						        docExPCL_LP.writeText("Hello World I am a printing sample (Bold)", paramExPCL_LP);
						        paramExPCL_LP.setIsBold(false);
						        paramExPCL_LP.setIsInverse(true);
						        docExPCL_LP.writeText("Hello World I am a printing sample (White On Black)", paramExPCL_LP);
						        paramExPCL_LP.setIsInverse(false);
						        paramExPCL_LP.setIsPCLineDrawCharSet(true);
						        docExPCL_LP.writeText("Hello World I am a printing sample (PC Line Draw)", paramExPCL_LP);
						        for (int i = 179; i < 256; i++) {
						     		docExPCL_LP.writeTextPartial(String.valueOf((char)i),paramExPCL_LP);
						        }
						        paramExPCL_LP.setIsPCLineDrawCharSet(false);
						        docExPCL_LP.writeText("Hello World I am a printing sample (International)", paramExPCL_LP);
						        for (int i = 179; i < 256; i++) {
						        	docExPCL_LP.writeTextPartial(String.valueOf((char)i),paramExPCL_LP);
						        }
						        docExPCL_LP.writeText("", paramExPCL_LP);
						        docExPCL_LP.writeTextPartial("one ");
						        docExPCL_LP.writeTextPartial("two ");
						        docExPCL_LP.writeTextPartial("three ");
						        docExPCL_LP.writeText("<CR>");
						        paramExPCL_LP.setIsRightToLeftTextDirection(true);
						        docExPCL_LP.writeText("Hello World I am a printing sample (Right-To-Left Text Direction)", paramExPCL_LP);
						        paramExPCL_LP.setIsRightToLeftTextDirection(false);

						        paramExPCL_LP.setIsUnderline(true);
						        docExPCL_LP.writeText("Hello World I am a printing sample (Underline)", paramExPCL_LP);
						        paramExPCL_LP.setIsUnderline(false);
						        paramExPCL_LP.setPrintContrastLevel(6);
						        docExPCL_LP.writeText("Hello World I am a printing sample (Contrast = 6)", paramExPCL_LP);
						        paramExPCL_LP.setPrintContrastLevel(2);
						        paramExPCL_LP.setLineSpacing((byte)30);
						        docExPCL_LP.writeText("Hello World I am a printing sample (Line Spacing = 30)", paramExPCL_LP);
						        paramExPCL_LP.setLineSpacing((byte)3);
						        paramExPCL_LP.setVerticalTabHeight((byte)150);
	        					
						        String data = String.format("Tab0%cTab1%cTab2%cHello World I am a printing sample (Vertical Tab = 50)",11,11,11);
						        docExPCL_LP.writeText(data, paramExPCL_LP);
						        //paramExPCL_LP.setVerticalTabHeight(203);
						        paramExPCL_LP.setHorizontalTabWidth((byte)50);

	                            data = String.format("Tab0%cTab1%cTab2%cHello World I am a printing sample (Horizontal Tab = 200)",9,9,9);
						        docExPCL_LP.writeText(data, paramExPCL_LP);

						        //paramExPCL_LP.setHorizontalTabWidth(100);
						        paramExPCL_LP.setSensorSensitivity((byte)100);
						        docExPCL_LP.writeText("Hello World I am a printing sample (Sensor Sensitivity = 100)", paramExPCL_LP);
						        paramExPCL_LP.setSensorSensitivity((byte)255);
						        paramExPCL_LP.setPaperPresenter((byte)100);
						        docExPCL_LP.writeText("Hello World I am a printing sample (Paper Presenter = 100)", paramExPCL_LP);
						        paramExPCL_LP.setPaperPresenter((byte)190);
						        paramExPCL_LP.setAutoPowerDownTimer(30);
						        docExPCL_LP.writeText("Hello World I am a printing sample (Auto Power Down = 9 seconds)", paramExPCL_LP);
						        paramExPCL_LP.setAutoPowerDownTimer(0);
						        docExPCL_LP.writeText("Hello World I am a printing sample (Auto Power Down = 0 seconds)", paramExPCL_LP);
						        printData = docExPCL_LP.getDocumentData();
					        }

					        // BARCODE SAMPLES
					        else if (selectedItemIndex == 1) {
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code39,"DMITRIY",true,(byte)100);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code39,"DMITRIY",false,(byte)50);

						        paramExPCL_LP.setBarCodeHeight((byte)100);
						        paramExPCL_LP.setIsAnnotate(false);
						        paramExPCL_LP.setBarCodeType(BarcodeExPCL_LP.Code128);
						        paramExPCL_LP.setFontIndex(5);
						        paramExPCL_LP.setIsUnderline(true);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code39,"DMITRIY",true,(byte) 25,paramExPCL_LP);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code39,"DMITRIY",false,(byte) 50);

						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128,"DMITRIY",true,(byte) 25);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128,"dmitriy",true,(byte) 25);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128,"1234567890",true,(byte) 25);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Interleaved2of5,"1234567890",true,(byte) 25);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.UPC,"1234567890$",true,(byte) 25);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar,"12345678901",true,(byte) 25);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar,"123456",true,(byte) 25);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar,"1234567",true,(byte) 25);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar,"123456789012",true,(byte) 25);

						        paramExPCL_LP.setFontIndex(1);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code39,"DMITRIY",true,(byte) 25,paramExPCL_LP);
						        paramExPCL_LP.setFontIndex(2);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128,"DMITRIY",true,(byte) 25,paramExPCL_LP);
						        paramExPCL_LP.setFontIndex(3);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128,"dmitriy",true,(byte) 25,paramExPCL_LP);
						        paramExPCL_LP.setFontIndex(4);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128,"1234567890",true,(byte) 25,paramExPCL_LP);
						        paramExPCL_LP.setFontIndex(5);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Interleaved2of5,"1234567890",true,(byte) 25,paramExPCL_LP);
						        paramExPCL_LP.setFontIndex(6);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.UPC,"1234567890$",true,(byte) 25,paramExPCL_LP);
						        paramExPCL_LP.setFontIndex(7);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar,"12345678901",true,(byte) 25,paramExPCL_LP);
						        paramExPCL_LP.setFontIndex(8);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar,"123456",true,(byte) 25,paramExPCL_LP);
						        paramExPCL_LP.setFontIndex(9);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar,"1234567",true,(byte) 25,paramExPCL_LP);
						        paramExPCL_LP.setFontIndex(10);
						        docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar,"123456789012",true,(byte) 25,paramExPCL_LP);

						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional,"1234567890123",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional,"1234567890123",false,(byte)3,(byte)0,(byte)0,(byte)2);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional,"1234567890123",true,(byte)4,(byte)0,(byte)0,(byte)3);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional,"1234567890123",false,(byte)5,(byte)0,(byte)0,(byte)4);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional,"1234567890123",true,(byte)6,(byte)0,(byte)0,(byte)5);

						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarTruncated,"1234567890123",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarStacked,"1234567890123",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarStackedOmnidirectional,"1234567890123",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarLimited,"1234567890123",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarExpanded,"DATAMAX-O'NEIL",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.UPCA,"12345678901",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.UPCE,"1234500006",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.EAN13,"123456789012",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.EAN8,"1234567",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.UCCEAN128CCAB,"123456789012",true,(byte)2,(byte)0,(byte)0,(byte)1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.UCCEAN128CCC,"123456789012",true,(byte)2,(byte)0,(byte)0,(byte)1);

						        paramExPCL_LP.setFontIndex(1);
						        docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional,"1234567890123",true,(byte)2,(byte)0,(byte)0,(byte)1,paramExPCL_LP);

						        docExPCL_LP.writeBarCodeQRCode("www.datamax-oneil.com",false,2,(byte)'H',2);

						        paramExPCL_LP.setFontIndex(10);
						        docExPCL_LP.writeBarCodeQRCode("www.datamax-oneil.com",true,2,(byte)'L' ,3,paramExPCL_LP);

						        docExPCL_LP.writeBarCodePDF417("www.datamax-oneil.com",2);

						        paramExPCL_LP.setFontIndex(1);
						        docExPCL_LP.writeBarCodePDF417("www.datamax-oneil.com",2,paramExPCL_LP);
						        printData = docExPCL_LP.getDocumentData();
					        }
					        //Graphics
					        else if (selectedItemIndex == 2) {
					        	m_statusBox.append("Processing image..\r\n");
					        	BufferedImage anImage = ImageIO.read(getClass().getResourceAsStream("dologo.png"));
	                            docExPCL_LP.writeImage(anImage,m_printHeadWidth);
	                            printData = docExPCL_LP.getDocumentData();
					        }
					        //User selected an unpredefine item(eg from browsing file)
			                else
			                {
			                	String selectedItem = (String)m_printItemsComboBox.getSelectedItem();
			                	BufferedImage anImage = null;
			                	//Check if item is an image
			                	String[] okFileExtensions =  new String[] {".jpg", ".png", ".gif",".jpeg",".bmp", ".tif", ".tiff",".pcx"};
			                	for (String extension : okFileExtensions) {
									if(selectedItem.toLowerCase(Locale.US).endsWith(extension))
									{
										anImage = ImageIO.read(new File(selectedItem));
										break;
									}
								}
			                	//selected item is not an image file
			                	if (selectedItem.toLowerCase(Locale.US).endsWith(".pdf"))
			                	{
			                		docExPCL_LP.writePDF(selectedItem, m_printHeadWidth);
			                		printData = docExPCL_LP.getDocumentData();
			                	}
			                	//selected item is not an image file
			                	else if (anImage == null)
			                	{
			                		File file = new File(selectedItem);
			                		byte[] readBuffer = new byte[(int)file.length()];
			                		InputStream inputStream= new BufferedInputStream(new FileInputStream(file));
			                		inputStream.read(readBuffer);
			                		inputStream.close();
			                		printData = readBuffer;
			                	}
			                	else 
			                	{
			                		m_statusBox.append("Processing image..\r\n");
									docExPCL_LP.writeImage(anImage, m_printHeadWidth);
									printData = docExPCL_LP.getDocumentData();
								}
			                }
					        // Paper feed
					        if (TEST_PAPER_ADVANCE) {
						        docExPCL_LP.writeText("Start of advanceToNextPage");
						        docExPCL_LP.setPageLength(510);
						        docExPCL_LP.advanceToNextPage();
						        docExPCL_LP.writeText("End of advanceToNextPage");

						        docExPCL_LP.writeText("Start of advanceToQMark");
						        docExPCL_LP.advanceToQueueMark((byte)255);
						        docExPCL_LP.writeText("End of advanceToQMark");
						        printData = docExPCL_LP.getDocumentData();
					        }
					        
						}
						else if (m_printerMode.equals("ExPCL_PP"))
						{
							 // Text
					        if (selectedItemIndex == 0)
					        {
						        docExPCL_PP.drawText(0,1600,true,RotationAngle.RotationAngle_0,"<f=1>This is a sample");
						        docExPCL_PP.drawText(0,1625,true,RotationAngle.RotationAngle_0,"<f=2>This is a sample");
						        docExPCL_PP.drawText(0,1650,true,RotationAngle.RotationAngle_0,"<f=3>This is a sample");
						        docExPCL_PP.drawText(0,1675,true,RotationAngle.RotationAngle_0,"<f=4>This is a sample");
						        docExPCL_PP.drawText(0,1700,true,RotationAngle.RotationAngle_0,"<f=5>This is a sample");
						        docExPCL_PP.drawText(0,1725,true,RotationAngle.RotationAngle_0,"<f=6>This is a sample");
						        docExPCL_PP.drawText(0,1750,true,RotationAngle.RotationAngle_0,"<f=7>This is a sample");
						        docExPCL_PP.drawText(0,1775,true,RotationAngle.RotationAngle_0,"<f=8>This is a sample");
						        docExPCL_PP.drawText(0,1800,true,RotationAngle.RotationAngle_0,"<f=9>This is a sample");
						        docExPCL_PP.drawText(0,1825,true,RotationAngle.RotationAngle_0,"<f=10>This is a sample");
						        docExPCL_PP.drawText(0,1850,true,RotationAngle.RotationAngle_0,"<f=11>This is a sample");
						        docExPCL_PP.drawText(0,1875,true,RotationAngle.RotationAngle_0,"<f=12>This is a sample");
						        docExPCL_PP.drawText(0,1900,true,RotationAngle.RotationAngle_0,"<f=13>This is a sample");
						        docExPCL_PP.drawText(0,1950,true,RotationAngle.RotationAngle_0,"<f=14>This is a sample");
						        docExPCL_PP.drawText(0,2000,true,RotationAngle.RotationAngle_0,"<f=15>This is a sample");

						        // Rotate text by 180
						        docExPCL_PP.drawText(384,2425,true,RotationAngle.RotationAngle_180,"<f=1>This is a sample");
						        docExPCL_PP.drawText(384,2400,true,RotationAngle.RotationAngle_180,"<f=2>This is a sample");
						        docExPCL_PP.drawText(384,2375,true,RotationAngle.RotationAngle_180,"<f=3>This is a sample");
						        docExPCL_PP.drawText(384,2350,true,RotationAngle.RotationAngle_180,"<f=4>This is a sample");
						        docExPCL_PP.drawText(384,2325,true,RotationAngle.RotationAngle_180,"<f=5>This is a sample");
						        docExPCL_PP.drawText(384,2300,true,RotationAngle.RotationAngle_180,"<f=6>This is a sample");
						        docExPCL_PP.drawText(384,2275,true,RotationAngle.RotationAngle_180,"<f=7>This is a sample");
						        docExPCL_PP.drawText(384,2250,true,RotationAngle.RotationAngle_180,"<f=8>This is a sample");
						        docExPCL_PP.drawText(384,2225,true,RotationAngle.RotationAngle_180,"<f=9>This is a sample");
						        docExPCL_PP.drawText(384,2200,true,RotationAngle.RotationAngle_180,"<f=10>This is a sample");
						        docExPCL_PP.drawText(384,2175,true,RotationAngle.RotationAngle_180,"<f=11>This is a sample");
						        docExPCL_PP.drawText(384,2150,true,RotationAngle.RotationAngle_180,"<f=12>This is a sample");
						        docExPCL_PP.drawText(384,2125,true,RotationAngle.RotationAngle_180,"<f=13>This is a sample");
						        docExPCL_PP.drawText(384,2100,true,RotationAngle.RotationAngle_180,"<f=14>This is a sample");
						        docExPCL_PP.drawText(384,2050,true,RotationAngle.RotationAngle_180,"<f=15>This is a sample");

						        // Text
						        docExPCL_PP.writeText("<f=1>This is a sample",2450,0);
						        paramExPCL_PP.setIsAnnotate(true);
						        paramExPCL_PP.setIsBold(true);
						        paramExPCL_PP.setIsUnderline(true);
						        paramExPCL_PP.setFontIndex(5);
						        docExPCL_PP.writeText("This is a sample",2475,0,paramExPCL_PP);
						        paramExPCL_PP.setIsAnnotate(false);
						        paramExPCL_PP.setIsBold(false);
						        paramExPCL_PP.setIsUnderline(false);
						        paramExPCL_PP.setHorizontalMultiplier(2);
						        paramExPCL_PP.setVerticalMultiplier(2);
						        paramExPCL_PP.setFontIndex(5);
						        docExPCL_PP.writeText("This is a sample",2425,0,paramExPCL_PP);
						        docExPCL_PP.setPageHeight(3000);
						        printData = docExPCL_PP.getDocumentData();
					        }

					        // Print all barcodes
					        else if(selectedItemIndex == 1)
					        {
						        docExPCL_PP.drawBarCode(0,0,RotationAngle.RotationAngle_0,true,BarcodeExPCL_PP.Code39,(byte) 25,"12345");
						        docExPCL_PP.drawBarCode(0,50,RotationAngle.RotationAngle_0,true,BarcodeExPCL_PP.Code128,(byte) 25,"SAMPLE");
	                            docExPCL_PP.drawBarCode(0,100,RotationAngle.RotationAngle_0,true,BarcodeExPCL_PP.Code128,(byte) 25, "sample");
						        docExPCL_PP.drawBarCode(0,150,RotationAngle.RotationAngle_0,true,BarcodeExPCL_PP.Code128,(byte) 25,"12");
						        docExPCL_PP.drawBarCode(0,200,RotationAngle.RotationAngle_0,true,BarcodeExPCL_PP.Interleaved2of5,(byte) 25,"1234567890");
						        docExPCL_PP.drawBarCode(0,250,RotationAngle.RotationAngle_0,true,BarcodeExPCL_PP.UPC,(byte) 40,"123456789012");
						        docExPCL_PP.drawBarCode(0,325,RotationAngle.RotationAngle_0,true,BarcodeExPCL_PP.UPC,(byte) 40,"1234567");
						        docExPCL_PP.drawBarCode(0,400,RotationAngle.RotationAngle_0,true,BarcodeExPCL_PP.UPC,(byte) 40,"12345678");
						        docExPCL_PP.drawBarCode(0,475,RotationAngle.RotationAngle_0,true,BarcodeExPCL_PP.UPC,(byte) 40,"1234567890123");
						        docExPCL_PP.drawBarCode(0,550,RotationAngle.RotationAngle_0,true,BarcodeExPCL_PP.Codabar,(byte) 15,"1234567890");

						        // Rotate 180 all barcodes
						        docExPCL_PP.drawBarCode(384,1175,RotationAngle.RotationAngle_180,true,BarcodeExPCL_PP.Code39,(byte) 25,"12345");
						        docExPCL_PP.drawBarCode(384,1125,RotationAngle.RotationAngle_180,true,BarcodeExPCL_PP.Code128,(byte) 25,"SAMPLE");
						        docExPCL_PP.drawBarCode(384,1075,RotationAngle.RotationAngle_180,true,BarcodeExPCL_PP.Code128,(byte) 25,"sample");
						        docExPCL_PP.drawBarCode(384,1025,RotationAngle.RotationAngle_180,true,BarcodeExPCL_PP.Code128,(byte) 25,"12");
						        docExPCL_PP.drawBarCode(384,975,RotationAngle.RotationAngle_180,true,BarcodeExPCL_PP.Interleaved2of5,(byte) 25,"1234567890");
						        docExPCL_PP.drawBarCode(384,925,RotationAngle.RotationAngle_180,true,BarcodeExPCL_PP.UPC,(byte) 40,"123456789012");
						        docExPCL_PP.drawBarCode(384,850,RotationAngle.RotationAngle_180,true,BarcodeExPCL_PP.UPC,(byte) 40,"1234567");
						        docExPCL_PP.drawBarCode(384,775,RotationAngle.RotationAngle_180,true,BarcodeExPCL_PP.UPC,(byte) 40,"12345678");
						        docExPCL_PP.drawBarCode(384,700,RotationAngle.RotationAngle_180,true,BarcodeExPCL_PP.UPC,(byte) 40,"1234567890123");
						        docExPCL_PP.drawBarCode(384,625,RotationAngle.RotationAngle_180,true,BarcodeExPCL_PP.Codabar,(byte) 15,"1234567890");

						        // Barcodes
						        docExPCL_PP.writeBarCode(BarcodeExPCL_PP.Code39,3,"sample",2500,0);
						        paramExPCL_PP.setIsAnnotate(true);
						        paramExPCL_PP.setIsBold(true);
						        paramExPCL_PP.setIsUnderline(true);
						        paramExPCL_PP.setBarCodeHeight((byte) 50);
						        paramExPCL_PP.setRotation(RotationAngle.RotationAngle_180);
						        docExPCL_PP.writeBarCode(BarcodeExPCL_PP.Code39,3,"sample",2650,384,paramExPCL_PP);
						        //paramExPCL_PP.setIsAnnotate(true);
						        paramExPCL_PP.setIsBold(false);
						        paramExPCL_PP.setIsUnderline(false);
						        paramExPCL_PP.setBarCodeHeight((byte) 40);
						        paramExPCL_PP.setRotation(ParametersExPCL_PP.RotationAngle.RotationAngle_0);
						        docExPCL_PP.writeBarCode(BarcodeExPCL_PP.Code39,5,"sample",2650,0);
						        docExPCL_PP.setPageHeight(3000);
						        printData = docExPCL_PP.getDocumentData();

					        }
					        // Rectangle
					        else if(selectedItemIndex == 2)
					        {
						        docExPCL_PP.drawRectangle(0,1200,384,1584,true,0);
						        docExPCL_PP.drawRectangle(20,1220,364,1564,false,3);
						        docExPCL_PP.drawRectangle(40,1240,344,1544,false,10);
						        docExPCL_PP.drawRectangle(80,1280,304,1504,false,0);
						        docExPCL_PP.drawRectangle(110,1310,274,1474,true,3);
						        docExPCL_PP.drawRectangle(130,1330,254,1454,true,10);

						        // Lines
						        docExPCL_PP.writeHorizontalLine(2450,0,384,10);
						        docExPCL_PP.writeHorizontalLine(2475,0,384,5, paramExPCL_PP);
						        docExPCL_PP.writeVerticalLine(2550,5,84,10);
						        docExPCL_PP.writeVerticalLine(2550,200,84,5, paramExPCL_PP);
						        docExPCL_PP.writeRectangle(2550,50,2634,150);
						        paramExPCL_PP.setLineThickness(10);
						        docExPCL_PP.writeRectangle(2550,250,2634,379,paramExPCL_PP);
						        docExPCL_PP.setPageHeight(3000);
						        printData = docExPCL_PP.getDocumentData();
					        }
					        //User selected an unpredefine item(eg from browsing file)
			                else
			                {
			                	String selectedItem = (String)m_printItemsComboBox.getSelectedItem();
			                	BufferedImage anImage = null;
			                	//Check if item is an image
			                	String[] okFileExtensions =  new String[] {".jpg", ".png", ".gif",".jpeg",".bmp", ".tif", ".tiff",".pcx"};
			                	for (String extension : okFileExtensions) {
									if(selectedItem.toLowerCase(Locale.US).endsWith(extension))
									{
										anImage = ImageIO.read(new File(selectedItem));
										break;
									}
								}
			                	//selected item is not an image file
			                	if (selectedItem.toLowerCase(Locale.US).endsWith(".pdf"))
			                	{
			                		docExPCL_LP.writePDF(selectedItem, m_printHeadWidth);
			                		printData = docExPCL_LP.getDocumentData();
			                	}
			                	//selected item is not an image file
			                	else if (anImage == null)
			                	{
			                		File file = new File(selectedItem);
			                		byte[] readBuffer = new byte[(int)file.length()];
			                		InputStream inputStream= new BufferedInputStream(new FileInputStream(file));
			                		inputStream.read(readBuffer);
			                		inputStream.close();
			                		printData = readBuffer;
			                	}
			                	else 
			                	{
			                		m_statusBox.append("Processing image..\r\n");
									docExPCL_LP.writeImage(anImage, m_printHeadWidth);
									printData = docExPCL_LP.getDocumentData();
								}
			                }
						}
						//DPL printers
						else if(m_printerMode.equals("DPL"))
						{
							 //text sample to generate
			                if(selectedItemIndex == 0)
			                {
			                  //Enable text formatting (eg. bold, italic, underline)
                                docDPL.setEnableAdvanceFormatAttribute(true);

                                //Using Internal Bitmapped Font with ID 0
                                docDPL.writeTextInternalBitmapped("Hello World", 0, 100, 5, paramDPL);

                                //Using Downloaed Bitmapped Font with ID 100
                                docDPL.writeTextDownloadedBitmapped("Hello World", 100, 125, 5, paramDPL);

                                //Using Internal Smooth Font with size 14
                                paramDPL.setIsBold(true);
                                paramDPL.setIsItalic(true);
                                paramDPL.setIsUnderline(true);
                                docDPL.writeTextInternalSmooth("Hello World", 14, 150, 5, paramDPL);

                                //write normal ASCII Text Scalable
                                paramDPL.setIsBold(true);
                                paramDPL.setIsItalic(false);
                                paramDPL.setIsUnderline(false);
                                docDPL.writeTextScalable("Hello World", "00", 175, 5, paramDPL);

                                //write normal ASCII Text Scalable
                                paramDPL.setIsBold(false);
                                paramDPL.setIsItalic(false);
                                paramDPL.setIsUnderline(true);
                                docDPL.writeTextScalable("Hello World", "00", 200, 5, paramDPL);

                                //write normal ASCII Text Scalable
                                paramDPL.setIsBold(false);
                                paramDPL.setIsItalic(true);
                                paramDPL.setIsUnderline(false);
                                docDPL.writeTextScalable("Hello World", "00", 225, 5, paramDPL);

                                //Using Chinese Font example
                                paramDPL.setIsUnicode(true);
                                paramDPL.setDBSymbolSet(DoubleByteSymbolSet.Unicode);
                                paramDPL.setFontHeight(8);
                                paramDPL.setFontWidth(8);

                                int width = 5;

                                paramDPL.setIsBold(true);
                                paramDPL.setIsItalic(true);
                                paramDPL.setIsUnderline(false);
                                docDPL.writeTextScalable("你好，世界 (Hello World in Chinese!)", "50", 250, width, paramDPL);

                                printData = docDPL.getDocumentData();
			                }
			                else if (selectedItemIndex == 1)
	                        {
	                            docDPL.setPrintQuantity(3);
	                            paramDPL.setEmbeddedEnable( false);
	                            paramDPL.setIncrementDecrementValue( 5);
	                            paramDPL.setIncrementDecrementType( ParametersDPL.IncrementDecrementTypeValue.NumericIncrement);
	                            docDPL.writeTextInternalBitmapped("12345", 3, 0, 0,paramDPL);

	                            paramDPL.setEmbeddedEnable( false);
	                            paramDPL.setIncrementDecrementValue( 5);
	                            paramDPL.setIncrementDecrementType( ParametersDPL.IncrementDecrementTypeValue.AlphanumericIncrement);
	                            docDPL.writeTextInternalBitmapped("ABC123", 3, 35, 0,paramDPL);

	                            paramDPL.setEmbeddedEnable( false);
	                            paramDPL.setIncrementDecrementValue( 5);
	                            paramDPL.setIncrementDecrementType( ParametersDPL.IncrementDecrementTypeValue.HexdecimalIncrement);
	                            docDPL.writeTextInternalBitmapped("0A0D", 3, 70, 0,paramDPL);

	                            paramDPL.setEmbeddedEnable(true);
	                            paramDPL.setEmbeddedIncrementDecrementValue( "010010010");
	                            paramDPL.setIncrementDecrementType( ParametersDPL.IncrementDecrementTypeValue.NumericIncrement);
	                            docDPL.writeTextInternalBitmapped("AB1CD1EF1", 3, 105, 0,paramDPL);

	                            printData = docDPL.getDocumentData();
	                        }
			                //Barcodes
			                else if (selectedItemIndex == 2)
			                {
			                    //Test print Code 3 of 9
			                    //Barcode A with default parameter
			                    docDPL.writeBarCode("A", "BRCDA", 0,0);
			                    docDPL.writeTextInternalBitmapped("Barcode A",1,60,0);
			                    
			                    //Barecode A with specified parameters
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(3);
			                    paramDPL.setNarrowBarWidth(1);
			                    paramDPL.setSymbolHeight(20);
			                    
			                    docDPL.writeBarCode("A", "BRCDA", 100,0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Barcode A",1,135,0);
			                    
			                    //UPC-A with specified parameters
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(3);
			                    paramDPL.setNarrowBarWidth(1);
			                    paramDPL.setSymbolHeight(10);
			                    docDPL.writeBarCode("B", "012345678912", 160,0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("UPC-A",1,185,0);
			                    //Code 128
			                    //Barecode A with specified parameters
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(3);
			                    paramDPL.setNarrowBarWidth(1);
			                    paramDPL.setSymbolHeight(20);
			                    docDPL.writeBarCode("E", "ACODE128", 210,0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Code 128",1,250,0);
			                    
			                    //EAN-13
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(3);
			                    paramDPL.setNarrowBarWidth(1);
			                    paramDPL.setSymbolHeight(20);
			                    docDPL.writeBarCode("F", "0123456789012", 285,0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("EAN-13",1,315,0);
			                    //EAN Code 128
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(3);
			                    paramDPL.setNarrowBarWidth(1);
			                    paramDPL.setSymbolHeight(20);
			                    docDPL.writeBarCode("Q", "0123456789012345678", 355,0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("EAN Code 128",1,395,0);
			                    //UPS MaxiCode, Mode 2 & 3
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(0);
			                    paramDPL.setNarrowBarWidth(0);
			                    paramDPL.setSymbolHeight(0);
			                    
			                    UPSMessage upsMessage = new UPSMessage("920243507", 840, 1, "1Z00004951", "UPSN", "9BCJ43", 365, "625TH9", 1, 1, 10, true, "669 SECOND ST", "ENCINITAS", "CA");
			                    
			                    docDPL.writeBarCodeUPSMaxiCode(2, upsMessage, 445, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("UPS MaxiCode",1,560,0);
			                    
			                    //PDF-417
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(0);
			                    paramDPL.setNarrowBarWidth(0);
			                    paramDPL.setSymbolHeight(0);
			                   	docDPL.writeBarCodePDF417("ABCDEF1234", false, 1, 0, 0, 0, 590, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("PDF-417",1,630,0);
			                    
			                    //Data Matrix
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(4);
			                    paramDPL.setNarrowBarWidth(4);
			                    paramDPL.setSymbolHeight(0);
			                    
			                    docDPL.writeBarCodeDataMatrix("DATAMAX", 140, 0, 0, 0, 670, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Data Matrix w/ ECC 140",1,770,0);
			                    docDPL.writeBarCodeDataMatrix("DATAMAX", 200, 0, 0, 0, 810, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Data Matrix w/ ECC 200",1,880,0);
			                    
			                    //QRCODE
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(4);
			                    paramDPL.setNarrowBarWidth(4);
			                    paramDPL.setSymbolHeight(0);
			                    //AutoFormatting
			                    docDPL.writeBarCodeQRCode("This is the data portion", true, 0, "", "", "", "", 920, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("QR Barcode w/ Auto Formatting",1,1030,0);
			                    
			                    //Manual Formatting
			                    docDPL.writeBarCodeQRCode("1234This is the data portion", false, 2, "H", "4", "M", "A", 1070, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("QR Barcode w/ Manual formatting",1,1200,0);
			                    
			                    //Test BarcodeAzTec
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(12);
			                    paramDPL.setNarrowBarWidth(12);
			                    paramDPL.setSymbolHeight(0);
			                    docDPL.writeBarCodeAztec("ABCD1234", 0, false, 0, 1240, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Aztec Barcode ECI 0, ECC 0",1,1360,0);
			                    docDPL.writeBarCodeAztec("ABCD1234", 17, true, 232, 1400, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Aztec Barcode ECI 1, ECC 232",1,1500,0);
			                    
			                    //GS1 Databars
			                    paramDPL.setWideBarWidth(2);
			                    paramDPL.setNarrowBarWidth(2);
			                    paramDPL.setSymbolHeight(0);
			                    
			                    docDPL.writeBarCodeGS1DataBar("2001234567890","","E",1 ,0 ,0 ,2 ,1540, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("GS1 Databar Expanded",1,1760,0);
			                    
			                    docDPL.writeBarCodeGS1DataBar("2001234567890","hello123World","D",1 ,0 ,0 ,0 ,1800, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("GS1 Stacked Omni Direction",1,1980,0);
			                    
			                    //Austrailia 4-State
			                    docDPL.writeBarCodeAusPost4State("A124B", true, 59, 32211324, 2020, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Aus Post 4 State readable",1,2100,0);
			                    docDPL.writeBarCodeAusPost4State("123456789012345", false, 62, 39987520, 2140, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Aus Post 4 State non readable",1,2190,0);
			                    
			                    
			                    //write CodaBlock
			                    paramDPL.setWideBarWidth(0);
			                    paramDPL.setNarrowBarWidth(0);
			                    docDPL.writeBarCodeCODABLOCK("12345678", 25, "E", false, 4, 2, 2230, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("CODABLOCK",1,2320,0);
			                    
			                    //write TCIF
			                    paramDPL.setWideBarWidth(0);
			                    paramDPL.setNarrowBarWidth(0);
			                    docDPL.writeBarCodeTLC39("ABCD12345678901234589ABED", 0, 123456, 2360, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("TCIF",1,2480,0);
			                    
			                    //write MicroPDF417
			                    paramDPL.setWideBarWidth(0);
			                    paramDPL.setNarrowBarWidth(0);
			                    docDPL.writeBarCodeMicroPDF417("PDF417", 4, 4, false, false, 2520, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Micro PDF417",1,2560,0);
			                    printData = docDPL.getDocumentData();
			                }
			                //graphics
			                else if (selectedItemIndex == 3)
			                {
			                	//writeLine
			    				docDPL.writeLine(0, 0, 10, 25);
			    	
			    				//writeBox
			    				docDPL.writeBox(50, 0, 25, 25, 1, 1);
			    	
			    				//writeRectangle
			    				docDPL.writeRectangle(9, 100, 10, 150, 10, 150, 200, 100, 200);
			    				docDPL.writeTriangle(7, 200, 10, 250, 25, 200, 40);
			    				docDPL.writeCircle(4, 300, 25, 25);
			    				printData = docDPL.getDocumentData();
			                }
			                //image
			                else if (selectedItemIndex == 4)
			                {	
			                	m_statusBox.append("Processing image..\r\n");
			                    BufferedImage anImage = ImageIO.read(getClass().getResourceAsStream("dologo.png"));
			                    docDPL.writeTextInternalBitmapped("This is a D-O Logo", 1, 130, 200);
                                docDPL.writeImage(anImage, 0, 0, paramDPL);
			                	printData = docDPL.getDocumentData();
			                }
			                
			              //User selected a browsed file
			                else
			                {
			                	boolean isImage = false;
			                	String selectedItem = (String)m_printItemsComboBox.getSelectedItem();
			                	//Check if item is an image
			                	String[] okFileExtensions =  new String[] {".jpg", ".png", ".gif",".jpeg",".bmp", ".tif", ".tiff",".pcx"};
			                	for (String extension : okFileExtensions) {
									if(selectedItem.toLowerCase(Locale.US).endsWith(extension))
									{
										isImage = true;
										break;
									}
								}
			                	//selected item is a pdf file
			                	if (selectedItem.toLowerCase(Locale.US).endsWith(".pdf"))
			                	{
			                		docDPL.writePDF(selectedItem,m_printHeadWidth,0,0);
			                		printData = docDPL.getDocumentData();
			                	}
			                	//selected item is not an image file
			                	else if (!isImage)
			                	{
			                		File file = new File(selectedItem);
			                		byte[] readBuffer = new byte[(int)file.length()];
			                		InputStream inputStream= new BufferedInputStream(new FileInputStream(file));
			                		inputStream.read(readBuffer);
			                		inputStream.close();
			                		printData = readBuffer;
			                	}
			                	else 
			                	{
			                		m_statusBox.append("Processing image..\r\n");
			                		ImageType imgType = ImageType.Other;
			                		if(selectedItem.toLowerCase().endsWith(".pcx"))
			                		{
			                			imgType = ImageType.PCXFlipped_8Bit;
			                		}
			                		else
			                		{
			                			imgType = ImageType.Other;
									}
									docDPL.writeImage(selectedItem, imgType, 0, 0, paramDPL);
									printData = docDPL.getDocumentData();
								}//end else
			                }//end else
						}
					}
					//=====================Start Connection Thread=======================================//
					Thread commThread = new Thread("Comm Thread")
					{
						public void run(){
							DOPrint.this.run();
						}
					};
					commThread.start();
					
				} catch (Exception ex) {
					// TODO Auto-generated catch block
					m_performButton.setEnabled(true);
					m_statusBox.append(ex.getMessage());
				}
			}
		});
		
		//On address text change
		m_addressTextField.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				m_deviceAddress = m_addressTextField.getText().trim();
				
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				m_deviceAddress = m_addressTextField.getText().trim();
				
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				m_deviceAddress = m_addressTextField.getText().trim();
				
			}
		});
			
		//On port change
		m_portComboBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					if(connType == "TCP/IP")
					{
						if(m_portComboBox.getSelectedItem().toString() == "")
							throw new Exception("No port entered. Please specify a port for the connection.");
						m_devicePort = Integer.parseInt(m_portComboBox.getSelectedItem().toString());
					}
					else if( connType == "Serial")
					{
						m_serialPort = m_portComboBox.getSelectedItem().toString();
					}
					
				} catch (Exception ex) {
					// TODO: handle exception
					m_statusBox.setText(ex.getMessage());
				}
			}
		});
	
		m_browseButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				fileDlg = new JFileChooser();
				int retVal = fileDlg.showOpenDialog(null);
				if (retVal == JFileChooser.APPROVE_OPTION)
				{
					File file = fileDlg.getSelectedFile();
					selectedFilesList.add(file.getAbsolutePath());
					m_printItemsComboBox.addItem(file.getAbsolutePath());
					m_printItemsComboBox.setSelectedItem(file.getAbsolutePath());
				}
			}
		});
		
		m_printHeadCmbo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				String value = (String)m_printHeadCmbo.getSelectedItem();
	            m_printHeadWidth = Integer.parseInt(value.substring(0, 3));
			}
		});
	
		m_saveButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				prefs.put("Device IP", m_deviceAddress);
				prefs.put("COM Port", m_serialPort);
				prefs.putInt("Language", m_printerLanguageComboBox.getSelectedIndex());
				prefs.putInt("Connection", m_connComboBox.getSelectedIndex());
				prefs.putInt("Port", m_devicePort);
				prefs.putInt("PrintHead Width", m_printHeadCmbo.getSelectedIndex());
				prefs.putBoolean("Print Action", m_printRadio.isSelected());
				m_statusBox.setText("Settings saved.");
			}
		});
	}

	/**
	 * Initialize the contents of the frame.
	 * @throws Exception
	 */
	private void initialize() throws Exception {
		
		UIManager.setLookAndFeel(
	            UIManager.getSystemLookAndFeelClassName());
		frmDoPrint = new JFrame();
		frmDoPrint.setTitle("D-O Print!");
		frmDoPrint.setBounds(100, 100, 465, 488);
		frmDoPrint.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmDoPrint.getContentPane().setLayout(null);
		
		m_printRadio = new JRadioButton("Print");
		m_printRadio.setSelected(true);
		
		m_printRadio.setBounds(250, 153, 55, 23);
		frmDoPrint.getContentPane().add(m_printRadio);
		
		m_queryRadio = new JRadioButton("Query");
		m_queryRadio.setBounds(327, 153, 55, 23);
		frmDoPrint.getContentPane().add(m_queryRadio);
		ButtonGroup group = new ButtonGroup();
		group.add(m_printRadio);
		group.add(m_queryRadio);
		
		m_connComboBox = new JComboBox<String>();
		
		m_connComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"TCP/IP", "Serial"}));
		m_connComboBox.setBounds(250, 51, 142, 20);
		frmDoPrint.getContentPane().add(m_connComboBox);
		
		m_printerLanguageComboBox = new JComboBox<String>();

		m_printerLanguageComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"EZ", "LP", "DPL", "ExPCL_LP", "ExPCL_PP"}));
		m_printerLanguageComboBox.setBounds(250, 129, 142, 20);
		frmDoPrint.getContentPane().add(m_printerLanguageComboBox);
		
		m_printItemsComboBox = new JComboBox<String>();
		
		m_printItemsComboBox.setBounds(250, 206, 142, 20);
		frmDoPrint.getContentPane().add(m_printItemsComboBox);
		
		JLabel m_connLabel = new JLabel("Connection Type:");
		m_connLabel.setBounds(27, 51, 97, 14);
		frmDoPrint.getContentPane().add(m_connLabel);
		
		JLabel m_addressLabel = new JLabel("Device Address:");
		m_addressLabel.setBounds(27, 79, 97, 14);
		frmDoPrint.getContentPane().add(m_addressLabel);
		
		JLabel m_portLabel = new JLabel("Port:");
		m_portLabel.setBounds(27, 104, 97, 14);
		frmDoPrint.getContentPane().add(m_portLabel);
		
		JLabel m_printerLangLabel = new JLabel("Printer Language:");
		m_printerLangLabel.setBounds(27, 129, 97, 14);
		frmDoPrint.getContentPane().add(m_printerLangLabel);
		
		JLabel m_actionLabel = new JLabel("Select what to do:");
		m_actionLabel.setBounds(27, 154, 97, 14);
		frmDoPrint.getContentPane().add(m_actionLabel);
		
		JLabel m_itemLabel = new JLabel("Select an item:");
		m_itemLabel.setBounds(27, 206, 97, 14);
		frmDoPrint.getContentPane().add(m_itemLabel);
		
		JLabel m_statusLabel = new JLabel("Status:");
		m_statusLabel.setBounds(27, 261, 97, 14);
		frmDoPrint.getContentPane().add(m_statusLabel);
		
		m_performButton = new JButton("Print");
		
		m_performButton.setBounds(189, 237, 117, 34);
		frmDoPrint.getContentPane().add(m_performButton);
		
		m_addressTextField = new JTextField();
		
		m_addressTextField.setBounds(250, 79, 142, 20);
		frmDoPrint.getContentPane().add(m_addressTextField);
		m_addressTextField.setColumns(10);
		
		m_statusBox = new TextArea();
		m_statusBox.setBounds(27, 281, 401, 160);
		frmDoPrint.getContentPane().add(m_statusBox);
		
		m_portComboBox = new JComboBox<String>();
		
		m_portComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9"}));
		m_portComboBox.setEditable(true);
		m_portComboBox.setBounds(250, 104, 142, 20);
		frmDoPrint.getContentPane().add(m_portComboBox);
		
		JLabel m_printHeadWidthLabel = new JLabel("Select PrintHead Width:");
		m_printHeadWidthLabel.setBounds(27, 183, 126, 14);
		frmDoPrint.getContentPane().add(m_printHeadWidthLabel);
		
		m_printHeadCmbo = new JComboBox<String>();
		m_printHeadCmbo.setModel(new DefaultComboBoxModel<String>(new String[] {"384 (2 in.)", "576 (3 in.)", "832 (4 in.)"}));
		m_printHeadCmbo.setBounds(250, 183, 142, 20);
		frmDoPrint.getContentPane().add(m_printHeadCmbo);
		
		m_browseButton = new JButton("...");
		m_browseButton.setBounds(402, 205, 26, 23);
		frmDoPrint.getContentPane().add(m_browseButton);
		
		m_saveButton = new JButton("Save Settings");
		m_saveButton.setBounds(311, 237, 117, 34);
		frmDoPrint.getContentPane().add(m_saveButton);
	}
	
	void reloadItemsArray()
    {
		m_printItemsComboBox.removeAllItems();
        //For Printing
        if (m_printRadio.isSelected())
        {
            //EZprint
            if (m_printerLanguageComboBox.getSelectedIndex() == 0)
            {
                m_printItemsComboBox.addItem("3-inch Sample Receipt");
                m_printItemsComboBox.addItem("4-inch Sample Receipt");
                m_printItemsComboBox.addItem("Barcode Sample");
            }
            //LP
            else if (m_printerLanguageComboBox.getSelectedIndex() == 1)
            {
                m_printItemsComboBox.addItem("3-inch Sample Receipt");
                m_printItemsComboBox.addItem("4-inch Sample Receipt");
                m_printItemsComboBox.addItem("2-in Image Sample");
                m_printItemsComboBox.addItem("3-in Image Sample");
                m_printItemsComboBox.addItem("4-in Image Sample");
            }
            //EXPCL_LP
            else if (m_printerLanguageComboBox.getSelectedIndex() == 3)
            {
                m_printItemsComboBox.addItem("Text Sample");
                m_printItemsComboBox.addItem("Barcode Sample");
                m_printItemsComboBox.addItem("Graphics Sample");
            }
            //EXPCL_PP
            else if (m_printerLanguageComboBox.getSelectedIndex() == 4)
            {
                m_printItemsComboBox.addItem("Text Samples");
                m_printItemsComboBox.addItem("Barcode Samples");
                m_printItemsComboBox.addItem("Rectangles");
            }
            //DPL mode
            else
            {
                m_printItemsComboBox.addItem("Text Sample");
                m_printItemsComboBox.addItem("Incrementing Sample");
                m_printItemsComboBox.addItem("Barcode Sample");
                m_printItemsComboBox.addItem("Graphics sample");
                m_printItemsComboBox.addItem("Image sample");
            }
            
            //Add Files browsed to list
            if (selectedFilesList.size() > 0)
            {
                for (String file : selectedFilesList)
                {
                    m_printItemsComboBox.addItem(file);
                }
            }
        }
        //Query
        else {
            //EZprint or LP
            if (m_printerLanguageComboBox.getSelectedIndex() == 0 || m_printerLanguageComboBox.getSelectedIndex() == 1)
            {
                m_printItemsComboBox.addItem("Avalanche Settings");
                m_printItemsComboBox.addItem("Battery Condition");
                m_printItemsComboBox.addItem("Bluetooth Config");
                m_printItemsComboBox.addItem("Font List");
                m_printItemsComboBox.addItem("Format List");
                m_printItemsComboBox.addItem("General Config");
                m_printItemsComboBox.addItem("General Status");
                m_printItemsComboBox.addItem("Graphic List");
                m_printItemsComboBox.addItem("IrDA Config");
                m_printItemsComboBox.addItem("Label Config");
                m_printItemsComboBox.addItem("Magnetic Config");
                m_printItemsComboBox.addItem("Magnetic Card Data");
                m_printItemsComboBox.addItem("Manufacturing Date");
                m_printItemsComboBox.addItem("Memory Status");
                m_printItemsComboBox.addItem("Printer Options");
                m_printItemsComboBox.addItem("PrintHead Status");
                m_printItemsComboBox.addItem("Serial Number");
                m_printItemsComboBox.addItem("SmartCard Config");
                m_printItemsComboBox.addItem("Serial Port Config");
                m_printItemsComboBox.addItem("TCP/IP Status");
                m_printItemsComboBox.addItem("Upgrade Data");
                m_printItemsComboBox.addItem("Version Information");
            }
            //EXPCL_LP
            else if (m_printerLanguageComboBox.getSelectedIndex() == 3 || m_printerLanguageComboBox.getSelectedIndex() == 4)
            {
                m_printItemsComboBox.addItem("Battery Condition");
                m_printItemsComboBox.addItem("Bluetooth Configuration");
                m_printItemsComboBox.addItem("General Status");
                m_printItemsComboBox.addItem("Magnetic Card Data");
                m_printItemsComboBox.addItem("Memory Status");
                m_printItemsComboBox.addItem("Printer Options");
                m_printItemsComboBox.addItem("Print Head Status");
                m_printItemsComboBox.addItem("Version Information");
                
            }
            //DPL mode
            else
            {
                m_printItemsComboBox.removeAllItems();
                m_printItemsComboBox.addItem("Printer Information");
                m_printItemsComboBox.addItem("Files and Internal Fonts");
                m_printItemsComboBox.addItem("Media Label");
                m_printItemsComboBox.addItem("Print Controls");
                m_printItemsComboBox.addItem("System Settings");
                m_printItemsComboBox.addItem("Sensor Calibration");
                m_printItemsComboBox.addItem("Miscellaneous");
                m_printItemsComboBox.addItem("Serial Config");
                m_printItemsComboBox.addItem("Auto Update Settings");
                m_printItemsComboBox.addItem("Avalanche Enabler Settings");
                m_printItemsComboBox.addItem("Bluetooth Config");
                m_printItemsComboBox.addItem("Network General");
                m_printItemsComboBox.addItem("Network Wireless");
                m_printItemsComboBox.addItem("Printer status");
            }
        }
    }
	// --------------------------------
	// Enable controls after printing has completed
	// ---------------------------------
	public void EnableControls(final boolean value) {
		SwingUtilities.invokeLater((new Runnable() {

			@Override
			public void run() { m_performButton.setEnabled(value); }
		}));
	}	
	public void run() {
		// TODO Auto-generated method stub
		//Connection
		try
		{
			EnableControls(false);
			//Reset connection object
			conn = null;
			//====FOR TCP Connection==//
			if(connType == "TCP/IP")
			{
				//validate if its a IP Address
				Pattern pattern = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
				Matcher matcher = pattern.matcher(m_deviceAddress);
				if(!matcher.matches())
					throw new Exception("Invalid IP Address format.\r\n");
				
				//validate if port
    			pattern = Pattern.compile("^([0-9]+)$");
    			
    			matcher = pattern.matcher(m_portComboBox.getSelectedItem().toString());
    			if(!matcher.matches())
    				throw new Exception("Invalid port format entered.\r\n");
    			
                if (m_devicePort < 0 || m_devicePort > 65535)
                {
                	throw new Exception("Invalid port entered. Must be between 0 and 65535");
                }
                conn = Connection_TCP.createClient(m_deviceAddress, m_devicePort, false);  
			}
			
			//====FOR Serial CONNECTIONS========//
			else if( connType == "Serial")
			{
				Pattern pattern = Pattern.compile("^(COM([0-9]+))$");
				Matcher matcher = pattern.matcher(m_serialPort);
				if(!matcher.matches())
					throw new Exception("Invalid COM Port format");
				conn = Connection_Serial.createClient(m_serialPort, 115200, honeywell.printer.configuration.ez.GeneralConfiguration.ParityValueEZ.None, 8, StopBits.One, HandshakeValueEZ.None, false);
			}
			
			if (m_printRadio.isSelected())
			{
				m_statusBox.append("Establishing connection..\r\n");
				//Open bluetooth socket
				if(!conn.getIsOpen()) { conn.open(); }
				
				//Sends data to printer
				m_statusBox.append("Sending data to printer..\r\n");
				conn.write(printData);
				Thread.sleep(3000);
				//signals to close connection
				conn.close();
				m_statusBox.append("Print success.\r\n");
				EnableControls(true);
			}
			else if(m_queryRadio.isSelected())
			{
				String message = "";
				m_statusBox.append("Establishing connection..\r\n");
				//Open bluetooth socket
				if(!conn.getIsOpen()) { conn.open(); }
				
				m_statusBox.append("Querying data..\r\n");
				 //If ExPCL is selected
	            if (m_printerLanguageComboBox.getSelectedIndex() == 3 || m_printerLanguageComboBox.getSelectedIndex() == 4 ) {
	                
	              //Battery Condition
                    if (selectedItemIndex == 0)
                    {
                        BatteryCondition_ExPCL batteryCond = new BatteryCondition_ExPCL(conn);
                        batteryCond.queryPrinter(1000);

                        if (!batteryCond.getValid())
                        {
                            message += "No response from printer\r\n";
                        }
                        else {
                            message += String.format("Battery Voltage: %.2f\r\n", batteryCond.getVoltageBatterySingle());
                        }
                        m_statusBox.append(message);

                    }
                    //Bluetooth
                    else if (selectedItemIndex == 1)
                    {
                        BluetoothConfiguration_ExPCL btConfig = new BluetoothConfiguration_ExPCL(conn);
                        btConfig.queryPrinter(1000);

                        if (!btConfig.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format("Local Classic Name: %s\n", btConfig.getLocalClassicName());
                            message += String.format("Local COD: %s\n", btConfig.getDeviceClass());
                            message += String.format("Power Save Mode: %s\n", btConfig.getPowerSave());
                            message += String.format("Security Mode: %s\n", btConfig.getSecurity());
                            message += String.format("Discoverable: %s\n", btConfig.getDiscoverable());
                            message += String.format("Connectable: %s\n", btConfig.getConnectable());
                            message += String.format("Bondable: %s\n", btConfig.getBondable());
                            message += String.format("Bluetooth Address: %s\n", btConfig.getBluetoothAddress());
                        }
                        m_statusBox.append(message);
                    }
                    //General Status
                    if (selectedItemIndex == 2)
                    {
                        GeneralStatus_ExPCL generalStatus = new GeneralStatus_ExPCL(conn);
                        generalStatus.queryPrinter(1000);

                        if (!generalStatus.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format("Printer Error: %s\r\n", generalStatus.getPrinterError() ? "Yes" : "No");
                            message += String.format("Head Lever Latched: %s\r\n", generalStatus.getHeadLeverLatched() ? "Yes" : "No");
                            message += String.format("Paper Present: %s\r\n", generalStatus.getPaperPresent() ? "Yes" : "No");
                            message += String.format("Battery Status: %s\r\n", generalStatus.getBatteryVoltageStatus());
                            message += String.format("Print Head Temperature Acceptable: %s\r\n", generalStatus.getPrintheadTemperatureAcceptable() ? "Yes" : "No");
                            message += String.format("Text Queue Empty: %s\r\n", generalStatus.getTextQueueEmpty() ? "Yes" : "No");
                        }
                        m_statusBox.append(message);

                    }
                    //Magnetic Card Data
                    else if (selectedItemIndex == 3)
                    {
                        MagneticCardData_ExPCL mcrData = new MagneticCardData_ExPCL(conn);
                        mcrData.queryPrinter(1000);

                        if (!mcrData.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format("Track 1: %s\n", mcrData.getTrack1Data());
                            message += String.format("Track 2: %s\n", mcrData.getTrack2Data());
                            message += String.format("Track 3: %s\n", mcrData.getTrack3Data());
                        }
                        m_statusBox.append(message);
                    }
                    //Memory Status
                    if (selectedItemIndex == 4)
                    {
                        MemoryStatus_ExPCL memoryStatus = new MemoryStatus_ExPCL(conn);
                        memoryStatus.queryPrinter(1000);

                        if (!memoryStatus.getValid())
                        {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format("Print Buffer KB Remaining: %d\n", memoryStatus.getRemainingRAM());
                            message += String.format("Used RAM: %d\n", memoryStatus.getUsedRAM());
                        }
                        m_statusBox.append(message);

                    }
                    //Printer Options
                    if (selectedItemIndex == 5)
                    {
                        PrinterOptions_ExPCL printerOpt = new PrinterOptions_ExPCL(conn);
                        printerOpt.queryPrinter(1000);

                        if (!printerOpt.getValid())
                        {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format("Power Down Timer: %d\r\n", printerOpt.getPowerDownTimer());
                        }
                        m_statusBox.append(message);

                    }
                    //Printhead Status
                    if (selectedItemIndex == 6)
                    {
                        PrintheadStatus_ExPCL printheadStatus = new PrintheadStatus_ExPCL(conn);
                        printheadStatus.queryPrinter(1000);

                        if (!printheadStatus.getValid())
                        {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format("PrintHead Temperature: %.2f\r\n", printheadStatus.getPrintheadTemperature());
                        }
                        m_statusBox.append(message);

                    }
                    //Version information
                    else if (selectedItemIndex == 7)
                    {
                        VersionInformation_ExPCL versionInfo = new VersionInformation_ExPCL(conn);
                        versionInfo.queryPrinter(1000);

                        if (!versionInfo.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format("Hardware Version: %s\n", versionInfo.getHardwareControllerVersion());
                            message += String.format("Firmware Version: %s\n", versionInfo.getFirmwareVersion());

                        }
                        m_statusBox.append(message);
                    }
	                
	            }//end of ExPCL mode
	            //DPL Mode
	            else if (m_printerLanguageComboBox.getSelectedIndex() == 2 )
	            {
	                //Printer Info
	                if (selectedItemIndex == 0)
	                {
	                    //Query Printer info
	                    PrinterInformation_DPL printerInfo = new PrinterInformation_DPL(conn);
	                    printerInfo.queryPrinter(1000);
	                    
	                    if (printerInfo.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Serial Number: %s\n", printerInfo.getPrinterSerialNumber());
	                        message += String.format("Boot 1 Version: %s\n", printerInfo.getBoot1Version());
	                        message += String.format("Boot 1 Part Number: %s\n", printerInfo.getBoot1PartNumber());
	                        message += String.format("Boot 2 Version: %s\n", printerInfo.getBoot2Version());
	                        message += String.format("Boot 2 PartNumber: %s\n", printerInfo.getBoot1PartNumber());
	                        message += String.format("Firmware Version: %s\n", printerInfo.getVersionInformation());
	                        message += String.format("AVR Version: %s\n", printerInfo.getAVRVersionInformation());
	                    }
	                    m_statusBox.append(message);

	                }
	                //Fonts and files
	                else if (selectedItemIndex == 1)
	                {
	                    //Query Memory Module
	                    Fonts_DPL fontsDPL = new Fonts_DPL(conn);
	                    fontsDPL.queryPrinter(1000);


	                    if (fontsDPL.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += "FILES IN G: \n";
	                        
	                        //Get All Files
	                        FileInformation[] files = fontsDPL.getFiles("G");
	                        if(files != null)
	                        {
	                        	if(files.length == 0)
	                        		message+= "No files found in module.\n";
	                        	else {
			                        for (FileInformation file : files) {
			                            message += String.format("Name: %s, Size: %d, Type: %s\n",file.getFileName(),file.getFileSize(),file.getFileType());
			                        }
	                        	}
	                        }
	                        
	                        //Get internal Fonts
	                         message += "INTERNAL FONTS: \n";
	                        String[] internalFonts = fontsDPL.getInternalFonts();
	                        for (String internalFont:internalFonts) {
	                            message += String.format("Name: %s\n",internalFont);

	                        }
	                    }
	                    m_statusBox.append(message);
	                }
	                
	                //Media Label
	                else if (selectedItemIndex == 2)
	                {
	                    //Query Media Label
	                    MediaLabel_DPL mediaLabel = new MediaLabel_DPL(conn);
	                    mediaLabel.queryPrinter(1000);
	                
	                    if (mediaLabel.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Media Type: %s\n", mediaLabel.getMediaType());
	                        message += String.format("Max Label Length: %d\n", mediaLabel.getMaxLabelLength());
	                        message += String.format("Continuous Label Length: %d\n", mediaLabel.getContinuousLabelLength());
	                        message += String.format("Sensor Type: %s\n", mediaLabel.getSensorType());
	                        message += String.format("Paper Empty Distance: %d\n", mediaLabel.getPaperEmptyDistance());
	                        message += String.format("Label Width: %d\n", mediaLabel.getLabelWidth());
	                        message += String.format("Head Cleaning Threshold: %d\n", mediaLabel.getHeadCleaningThreshold());
	                        message += String.format("Ribbon Low Diameter: %d\n", mediaLabel.getRibbonLowDiameter());
	                        message += String.format("Ribbon Low Pause Enable: %s\n", mediaLabel.getRibbonLowPause()?"Yes":"No");
	                        message += String.format("Label Length Limit Enable: %s\n", mediaLabel.getLabelLengthLimit()?"Yes":"No");
	                        message += String.format("Present Backup Enable: %s\n", mediaLabel.getPresentBackup()?"Yes":"No");
	                        message += String.format("Present Location: %d\n", mediaLabel.getPresentDistance());
	                        message += String.format("Stop Location: %s\n", mediaLabel.getStopLocation());
	                        message += String.format("Backup After Print Enable: %s\n", mediaLabel.getBackupAfterPrint()?"Yes":"No");
	                        message += String.format("Gap Alternative Mode: %s\n", mediaLabel.getGapAlternateMode()?"Yes":"No");
	                    }
	                    
	                    m_statusBox.append(message);
	                }
	                
	                //Print Controls
	                else if (selectedItemIndex == 3){
	                    //Print Controls
	                    PrintSettings_DPL printSettings = new PrintSettings_DPL(conn);
	                    printSettings.queryPrinter(1000);

	                    if (printSettings.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Backup Delay: %d\n", printSettings.getBackupDelay());
	                        message += String.format("Row Offset: %d\n", printSettings.getRowOffset());
	                        message += String.format("Column Offset: %d\n", printSettings.getColumnOffset());
	                        message += String.format("Row Adjusted Fine Tune: %d\n", printSettings.getRowAdjustFineTune());
	                        message += String.format("Column Adjusted Fine Tune: %d\n", printSettings.getColumnAdjustFineTune());
	                        message += String.format("Present Fine Tune: %d\n", printSettings.getPresentAdjustFineTune());
	                        message += String.format("Darkness Level: %d\n", printSettings.getDarknessLevel());
	                        message += String.format("Contrast Level: %d\n", printSettings.getContrastLevel());
	                        message += String.format("Heat Level: %d\n", printSettings.getHeatLevel());
	                        message += String.format("Backup Speed: %f\n", printSettings.getBackupSpeed());
	                        message += String.format("Feed Speed: %f\n", printSettings.getFeedSpeed());
	                        message += String.format("Print Speed: %f\n", printSettings.getPrintSpeed());
	                        message += String.format("Slew Speed: %f\n", printSettings.getSlewSpeed());
	                    }
	                    
	                    m_statusBox.append(message);
	                }
	                
	                //System Settings
	                else if (selectedItemIndex == 4){
	                    
	                    //System Settings
	                    SystemSettings_DPL sysSettings = new SystemSettings_DPL(conn);
	                    sysSettings.queryPrinter(1000);

	                    if (sysSettings.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Unit Measure: %s\n", sysSettings.getUnitMeasure());
	                        message += String.format("ESC Sequence Enable: %s\n", sysSettings.getEscapeSequences()?"Yes":"No");
	                        message += String.format("Single Byte Symbol: %s\n", sysSettings.getSingleByteSymbolSet());
	                        message += String.format("Double Byte Symbol: %s\n", sysSettings.getDoubleByteSymbolSet());
	                        message += String.format("Disable Symbol Set Value Selection: %s\n", sysSettings.getSymbolSetSelection()?"Yes":"No");
	                        message += String.format("Menu Mode: %s\n", sysSettings.getMenuMode());
	                        message += String.format("Start of Print Emulation: %s\n", sysSettings.getStartOfPrintEmulation());
	                        message += String.format("Image mode: %s\n", sysSettings.getImageMode());
	                        message += String.format("Menu Language: %s\n", sysSettings.getMenuLanguage());
	                        message += String.format("Display Mode: %s\n", sysSettings.getDisplayMode());
	                        message += String.format("Block Allocated for Internal Module: %d\n", sysSettings.getInternalModuleSize());
	                        message += String.format("Scalable Font Cache: %d\n", sysSettings.getScalableFontCache());
	                        message += String.format("Legacy Emulation: %s\n", sysSettings.getLegacyEmulation());
	                        message += String.format("Column Emulation: %d\n", sysSettings.getColumnEmulation());
	                        message += String.format("Row Emulation: %d\n", sysSettings.getRowEmulation());
	                        message += String.format("Fault Handling Level: %s\n", sysSettings.getFaultHandlingLevel().name());
	                        message += String.format("Fault Handling Void Distance: %d\n", sysSettings.getFaultHandlingVoidDistance());
	                        message += String.format("Fault Handling Retry Counts: %d\n", sysSettings.getFaultHandlingRetryCount());
	                        message += String.format("Font Emulation: %s\n", sysSettings.getFontEmulation().name());
	                        message += String.format("Input Mode: %s\n", sysSettings.getInputMode().name());
	                        message += String.format("Retract Delay: %d\n", sysSettings.getRetractDelay());
	                        message += String.format("Label Rotation: %s\n", sysSettings.getLabelRotation().name());
	                        message += String.format("Label Store Level: %s\n", sysSettings.getLabelStoreLevel());
	                        message += String.format("Scalable Font Bolding: %d\n", sysSettings.getScalableFontBolding());
	                        message += String.format("Format Attribute: %s\n", sysSettings.getFormatAttribute());
	                        message += String.format("Beeper State: %s\n", sysSettings.getBeeperState());
	                        message += String.format("Host Timeout: %d\n", sysSettings.getHostTimeout());
	                        message += String.format("Printer Sleep Timeout: %d\n", sysSettings.getPrinterSleepTimeout());
	                        message += String.format("Backlight Mode: %s\n", sysSettings.getBacklightMode().name());
	                        message += String.format("Backlight Timer: %d\n", sysSettings.getBacklightTimer());
	                        message += String.format("Power Down Timeout: %d\n", sysSettings.getPowerDownTimeout());
	                        message += String.format("RF Power Down Timeout: %d\n", sysSettings.getRFPowerDownTimeout());
	                        message += String.format("User Label Mode Enable: %s\n", sysSettings.getUserLabelMode()?"Yes":"No");
	                        message += String.format("Radio Status: %s\n", sysSettings.getRadioPowerState() ?"Radio on":"Radio off");
	                        message += String.format("Supress Auto Reset: %s\n", sysSettings.getSuppressAutoReset()?"Yes":"No");
	                    }

	                    m_statusBox.append(message);
	                }
	                
	                //Sensor Calibration
	                else if (selectedItemIndex == 5){
	                    //Sensor Calibration
	                    SensorCalibration_DPL sensorCalibration = new SensorCalibration_DPL(conn);
	                    sensorCalibration.queryPrinter(1000);

	                    if (sensorCalibration.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Black Mark Paper value: %d\n", sensorCalibration.getBlackMarkPaperValue());
	                        message += String.format("Black Mark Sensor Gain value: %d\n", sensorCalibration.getBlackMarkSensorGain());
	                        message += String.format("Black Mark value: %d\n", sensorCalibration.getBlackMarkValue());
	                        message += String.format("Gap Sensor Gain value: %d\n", sensorCalibration.getGapSensorGain());
	                        message += String.format("Gap Sensor Gain should be used with Thermal Transfer Media value: %d\n", sensorCalibration.getGapSensorGainWithThermalTransferMedia());
	                        message += String.format("Gap Mark Level value: %d\n", sensorCalibration.getGapMarkLevel());
	                        message += String.format("Gap Mark Level should be used with Thermal Transfer Media value: %d\n", sensorCalibration.getGapMarkLevelWithThermalTransferMedia());
	                        message += String.format("Paper Level value: %d\n", sensorCalibration.getPaperLevel());
	                        message += String.format("Paper Level should be used with Thermal Transfer Media value: %d\n", sensorCalibration.getPaperLevelWithThermalTransferMedia());
	                        message += String.format("Presenter Sensor Gain value: %d\n", sensorCalibration.getPresenterSensorGain());
	                        message += String.format("Sensor Clear Value: %d\n", sensorCalibration.getSensorClearValue());
	                        message += String.format("Sensor Clear Value should be used with Thermal Transfer Media: %d\n", sensorCalibration.getSensorClearValueWithThermalTransferMedia());
	                        message += String.format("Auto Calibration Mode Enable: %s\n", sensorCalibration.getAutoCalibrationMode()?"Yes":"No");
	                    }
	                    m_statusBox.append(message);
	                }
	                
	                //Miscellaneous
	                else if (selectedItemIndex == 6){
	                    
	                    //Misc
	                    Miscellaneous_DPL misc = new Miscellaneous_DPL(conn);
	                    misc.queryPrinter(1000);
	                    
	                    if (misc.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Delay Rate: %d\n", misc.getDelayRate());
	                        message += String.format("Present Sensor Equipped: %s\n", misc.getPresentSensorEquipped());
	                        message += String.format("Cutter Equipped: %s\n", misc.getCutterEquipped());
	                        message += String.format("Control Code: %s\n", misc.getControlCode());
	                        message += String.format("Start of Print Signal: %s\n", misc.getStartOfPrintSignal().name());
	                        message += String.format("End of Print Signal: %s\n", misc.getEndOfPrintSignal().name());
	                        message += String.format("GPIO Slew: %s\n", misc.getGPIOSlew().name());
	                        message += String.format("Feedback Mode Enable: %s\n", misc.getFeedbackMode()?"Yes":"No");
	                        message += String.format("Comm Heat Commands Enable: %s\n", misc.getCommunicationHeatCommands()?"Yes":"No");
	                        message += String.format("Comm Speed Commands Enable: %s\n", misc.getCommunicationSpeedCommands()?"Yes":"No");
	                        message += String.format("Comm TOF Commands Enable: %s\n", misc.getCommunicationTOFCommands()?"Yes":"No");
	                        message += String.format("British Pound Enable: %s\n", misc.getBritishPound()?"Yes":"No");
	                        message += String.format("GPIO Backup Label: %s\n", misc.getGPIOBackupLabel().name());
	                        message += String.format("Ignore Control Code Enable: %s\n", misc.getIgnoreControlCode()?"Yes":"No");
	                        message += String.format("Sofware Switch Enable: %s\n", misc.getSoftwareSwitch()?"Yes":"No");
	                        message += String.format("Max Length Ignore Enable: %s\n", misc.getMaximumLengthIgnore()?"Yes":"No");
	                        message += String.format("Pause Mode Enable: %s\n", misc.getPauseMode()?"Yes":"No");
	                        message += String.format("Peel Mode Enable: %s\n", misc.getPeelMode()?"Yes":"No");
	                        message += String.format("USB Mode: %s\n", misc.getUSBMode().name());
	                        message += String.format("Windows Driver For EZ RLE Enable: %s\n", misc.getWindowsDriverForEZ_RLE()?"Yes":"No");
	                        message += String.format("Hex Dump Enable: %s\n", misc.getHexDumpMode()?"Yes":"No");
	                        message += String.format("Display Mode for IP Host Name: %s\n", misc.getDisplayModeForIPHostname().name());
	                    }
	                    m_statusBox.append(message);
	                }
	                //Serial Port
	                else if (selectedItemIndex == 7){
	                    //SerialPort
	                    SerialPortConfiguration_DPL serialConfig = new SerialPortConfiguration_DPL(conn);
	                    serialConfig.queryPrinter(1000);

	                    if (serialConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Serial Port A Baud Rate: %d\n", serialConfig.getBaudRate());
	                        message += String.format("Serial Port A Stop Bit: %d\n", serialConfig.getStopBit());
	                        message += String.format("Serial Port A Data Bits: %d\n", serialConfig.getDataBits());
	                        message += String.format("Serial Port A Parity: %s\n", serialConfig.getParity());
	                        message += String.format("Serial Port A HandShaking: %s\n", serialConfig.getHandshaking());
	                    }
	                    
	                    m_statusBox.append(message);
	                }
	                
	                //Auto Update
	                else if (selectedItemIndex == 8){
	                    //AutoUpdate
	                    AutoUpdate_DPL autoUpdate = new AutoUpdate_DPL(conn);
	                    autoUpdate.queryPrinter(1000);

	                    if (autoUpdate.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Wireless Upgrade Type: %s\n", autoUpdate.getWirelessUpgradeType().name());
	                        message += String.format("Status Message Print mode: %s\n", autoUpdate.getStatusMessagePrintMode().name());
	                        message += String.format("Security Credential File Format: %s\n", autoUpdate.getSecurityCredentialFileFormat().name());
	                        message += String.format("Config File Name: %s\n", autoUpdate.getConfigurationFileName());
	                        message += String.format("TFTP Server IP: %s\n", autoUpdate.getTFTPServerIPAddress());
	                        message += String.format("Upgrade Package Version: %s\n", autoUpdate.getUpgradePackageVersion());
	                        message += String.format("Beeper Enable: %s\n", autoUpdate.getBeeper()?"Yes":"No");
	                        message += String.format(" FTP Username: %s\n", autoUpdate.getFTPUsername());
	                        message += String.format("FTP Server Name: %s\n", autoUpdate.getFTPServerName());
	                        message += String.format("FTP Server Port: %d\n", autoUpdate.getFTPServerPort());
	                    }
	                    m_statusBox.append(message);
	                }
	                
	                //Avalanche
	                else if (selectedItemIndex == 9){
	                    //Avalanche
	                    AvalancheEnabler_DPL avaEnabler = new AvalancheEnabler_DPL(conn);
	                    avaEnabler.queryPrinter(1000);
	                   
	                    if (avaEnabler.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Agent IP Address: %s\n", avaEnabler.getAgentIPAddress());
	                        message += String.format("Agent Port: %d\n", avaEnabler.getAgentPort());
	                        message += String.format("Agent DNS Name: %s\n", avaEnabler.getAgentDNSName());
	                        message += String.format("Connectivity Type: %s\n", avaEnabler.getConnectivityType().name());
	                        message += String.format("Printer Name: %s\n", avaEnabler.getPrinterName());
	                        message += String.format("Printer Model: %s\n", avaEnabler.getPrinterModel());
	                        message += String.format("Update Package Version: %s\n", avaEnabler.getUpdatePackageVersion());
	                        message += String.format("Update Mode: %s\n", avaEnabler.getUpdateMode());
	                        message += String.format("Update Interval: %d\n", avaEnabler.getUpdateInterval());
	                        message += String.format("Update Package Name: %s\n", avaEnabler.getUpdatePackageName());
	                        message += String.format("Print Status Result Enable: %s\n", avaEnabler.getPrintStatusResult()?"Yes":"No");
	                        message += String.format("Avalanche Enabler Active: %s\n", avaEnabler.getAvalancheEnablerActive()?"Yes":"No");
	                        message += String.format("Remove old updates: %s\n", avaEnabler.getRemoveOldUpdatesBeforeUpdate()?"Yes":"No");
	                    }
	                    m_statusBox.append(message);
	                }
	                //Bluetooth Config
	                else if (selectedItemIndex == 10) {
						BluetoothConfiguration_DPL btConfig = new BluetoothConfiguration_DPL(conn);
						 btConfig.queryPrinter(1000);
						
						if (btConfig.getValid() == false) {
							message += "No response from printer\r\n";
						}
						else {
							message += String.format("Bluetooth Device Name: %s\n", btConfig.getBluetoothDeviceName());
	                        message += String.format("Bluetooth Service Name: %s\n", btConfig.getBluetoothServiceName());
	                        message += String.format("Authentication Type:%s\n", btConfig.getAuthenticationType().name());
	                        message += String.format("Discoverable: %s\n", btConfig.getDiscoverable()?"Yes":"No");
	                        message += String.format("Connectable: %s\n", btConfig.getConnectable()?"Yes":"No");
	                        message += String.format("Bondable: %s\n", btConfig.getBondable()?"Yes":"No");
	                        message += String.format("Encryption: %s\n", btConfig.getEncryption()?"Yes":"No");
	                        message += String.format("Inactive Disconnect Time: %d\n",btConfig.getInactiveDisconnectTime());
	                        message += String.format("Power Down Time: %d\n", btConfig.getPowerDownTime());
	                        message += String.format("Bluetooth Device Address: %s\n", btConfig.getBluetoothDeviceAddress());
						}
						m_statusBox.append(message);
					}
	                //Network General
	                else if (selectedItemIndex == 11){
	                    
	                    NetworkGeneralSettings_DPL netGen = new NetworkGeneralSettings_DPL(conn);
	                    netGen.queryPrinter(1000);
	                    
	                    if (netGen.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Primary Interface: %s\n", netGen.getPrimaryInterface());
//	                        message += String.format("WiFi Module Type: %s\n", netGen.getWiFiType().name());
	                        message += String.format("Network Password:%s\n", netGen.getNetworkPassword());
	                        message += String.format("SNMP Enable: %s\n", netGen.getSNMPEnable()?"Yes":"No");
	                        message += String.format("Telnet Enable: %s\n", netGen.getTelnetEnable()?"Yes":"No");
	                        message += String.format("FTP Enable: %s\n", netGen.getFTPEnable()?"Yes":"No");
	                        message += String.format("HTTP Enable: %s\n", netGen.getHTTPEnable()?"Yes":"No");
	                        message += String.format("LPD Enable: %s\n", netGen.getLPDEnable()?"Yes":"No");
	                        message += String.format("NetBIOS Enable: %s\n", netGen.getNetBIOSEnable()?"Yes":"No");
	                        message += String.format("Netcenter Enable: %s\n", netGen.getNetcenterEnable()?"Yes":"No");
	                        message += String.format("Gratuitous ARP Period: %d\n", netGen.getGratuitousARPPeriod());
	                    }
	                    m_statusBox.append(message);
	                }
	                //Wifi
	                else if (selectedItemIndex == 12){
	                    
	                    NetworkWirelessSettings_DPL wifiSettings = new NetworkWirelessSettings_DPL(conn);
	                    wifiSettings.queryPrinter(1000);

	                    if (wifiSettings.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        //DNS Settings
	                        message += String.format("Static DNS Enable: %s\n", wifiSettings.getStaticDNS()?"Yes":"No");
	                        message += String.format("Preferred DNS Server: %s\n", wifiSettings.getPreferredDNSServerIP());
	                        message += String.format("Secondary DNS Server: %s\n", wifiSettings.getSecondaryDNSServerIP());
	                        message += String.format("DNS Suffix: %s\n", wifiSettings.getDNSSuffix());
	                        
//	                        //Wifi G
//	                        message += String.format("MTU: %d\n", wifiSettings.getMTU());
//	                        message += String.format("Ad-Hoc Channel: %d\n", wifiSettings.getAdHocChannel());
//	                        message += String.format("Region Code: %s\n", wifiSettings.getRegionCode());
//	                        message += String.format("EAP Realm: %s\n", wifiSettings.getEAPRealm());

	                        //Network Settings
	                        message += String.format("Inactive Timeout: %d\n", wifiSettings.getInactiveTimeout());
	                        message += String.format("IP Address Method: %s\n", wifiSettings.getIPAddressMethod());
	                        message += String.format("Active IP Address: %s\n", wifiSettings.getActiveIPAddress());
	                        message += String.format("Active Subnet Mask: %s\n", wifiSettings.getActiveSubnetMask());
	                        message += String.format("Printer DNS name: %s\n", wifiSettings.getPrinterDNSName());
	                        message += String.format("Register to DNS: %s\n", wifiSettings.getRegisterToDNS()?"Yes":"No");
	                        message += String.format("Active Gateway: %s\n", wifiSettings.getActiveGatewayAddress());
	                        message += String.format("UDP Port: %d\n", wifiSettings.getUDPPort());
	                        message += String.format("TCP Port: %d\n", wifiSettings.getTCPPort());
	                        message += String.format("Use DNS Suffix: %s\n", wifiSettings.getUseDNSSuffix()?"Yes":"No");
	                        message += String.format("Enable Connection Status: %s\n", wifiSettings.getEnableConnectionStatusReport()?"Yes":"No");
	                        message += String.format("DHCP User Class Option: %s\n", new String(wifiSettings.getDHCPUserClassOption()));
	                        message += String.format("Static IP Address: %s\n", wifiSettings.getStaticIPAddress());
	                        message += String.format("Static Subnet Mask: %s\n", wifiSettings.getStaticSubnetMask());
	                        message += String.format("Static Gateway: %s\n", wifiSettings.getStaticGateway());
	                        message += String.format("LPD Port: %d\n", wifiSettings.getLPDPort());
	                        message += String.format("LPD Enable: %s\n", wifiSettings.getLPDEnable()?"Yes":"No");
	                        
	                        
	                      //Wifi Settings
                            message += String.format("Network Type: %s\n", wifiSettings.getNetworkType());
                            message += String.format("ESSID: %s\n", wifiSettings.getESSID());
                            message += String.format("Network Authentication: %s\n", wifiSettings.getNetworkAuthenticationType().name());
                            message += String.format("EAP Type: %s\n", wifiSettings.getEAPType().name());
                            message += String.format("Phase 2 Method: %s\n", wifiSettings.getPhase2Method().name());
                            message += String.format("WEP Authentication Type: %s\n", wifiSettings.getWEPAuthenticationMethod().name());
                            message += String.format("WEP Data Encryption: %s\n", wifiSettings.getWEPDataEncryption()?"Yes":"No");
                            message += String.format("Selected WEP Key: %d\n", wifiSettings.getWEPKeySelected().value());
                            message += String.format("Show Signal Strength: %s\n", wifiSettings.getShowSignalStrength()?"Yes":"No");
                            message += String.format("Power Saving Mode: %s\n", wifiSettings.getPowerSavingMode()?"Yes":"No");
                            message += String.format("Group Cipher: %s\n", wifiSettings.getGroupCipher().name());
                            message += String.format("MAC Address: %s\n", wifiSettings.getWiFiMACAddress());
                            message += String.format("Regulatory Domain: %s\n", wifiSettings.getRegulatoryDomain().name());
                            message += String.format("Radio Mode: %s\n", wifiSettings.getRadioMode().name());
                            message += String.format("Max Active Channel Dwell Time: %d\n", wifiSettings.getMaxActiveChannelDwellTime());
                            message += String.format("Min Active Channel Dwell Time: %d\n", wifiSettings.getMinActiveChannelDwellTime());
                            message += String.format("Active Scanning Radio Channel: %s\n", wifiSettings.getRadioChannelSelection());
                            message += String.format("Use Hex PSK: %s\n", wifiSettings.getUseHexPSK()?"Yes":"No");
                            message += String.format("WiFi Testing Mode: %s\n", wifiSettings.getWiFiTestingMode()?"Yes":"No");
                            message += String.format("Use Client Certificate: %s\n", wifiSettings.getUseClientCertificate()?"Yes":"No");
                            message += String.format("Signal Strength: %s\n", wifiSettings.getSignalStrength());
                            message += String.format("SSL Port: %d\n", wifiSettings.getSSLPort());
                            message += String.format("DHCP Host Name: %s\n", wifiSettings.getDHCPHostName());
	                    }
	                    
	                    m_statusBox.append(message);
	                }
	                //Printer status
	                else if (selectedItemIndex == 13)
	                {
	                	PrinterStatus_DPL printerStatus = new PrinterStatus_DPL(conn);
                        printerStatus.queryPrinter( 3000);
                        
                        if(printerStatus.getValid() == false)
                        {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            PrinterStatus currentStatus = printerStatus.getCurrentStatus();
                            switch(currentStatus)
                            {
                                case PrinterReady:
                                    message += "Printer is ready.\r\n";
                                    break;
                                case BusyPrinting:
                                    message += "Printer is busy.\r\n";
                                    break;
                                case PaperOutFault:
                                    message += "Printer is out of paper.\r\n";
                                    break;
                                case PrintHeadUp:
                                    message += "Print head lid is open.\r\n";
                                    break;
                                default:
                                    message += "Printer status unknown.\r\n";
                                    break;
                            }
                        }
                        m_statusBox.append(message);
                    }
	            }
	            //EZ and LP mode
	            else {
	                
	                //Avalanche Settings
	                if (selectedItemIndex == 0){
	                    
	                    AvalancheSettings avaSettings = new AvalancheSettings(conn);
	                    avaSettings.queryPrinter(1000);

	                    if (avaSettings.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Agent IP: %s\n", avaSettings.getAgentIP());
	                        message += String.format("Show All Data on Self Test: %s\n", avaSettings.getShowAllData()?"Yes":"No");
	                        message += String.format("Agent Name: %s\n", avaSettings.getAgentName());
	                        message += String.format("Agent Port: %d\n", avaSettings.getAgentPort());
	                        message += String.format("Connection Type: %s\n", avaSettings.getConnectionType().name());
	                        message += String.format("Avalanche Enable: %s\n", avaSettings.getIsAvalancheEnabled()?"Yes":"No");
	                        message += String.format("Printer Name: %s\n", avaSettings.getPrinterName());
	                        message += String.format("Printer Model: %s\n", avaSettings.getPrinterModelName());
	                        message += String.format("Is Prelicensed: %s\n", avaSettings.getIsPrelicensed()?"Yes":"No");
	                        message += String.format("Printer Result Flag: %s\n", avaSettings.getPrinterResultFlag()?"Yes":"No");
	                        message += String.format("Update Interval: %d\n", avaSettings.getUpdateInterval());
	                        message += String.format("Update Mode: %s\n", avaSettings.getUpdateFlags().name());
	                        message += String.format("Is Wired: %s\n", avaSettings.getIsWired()?"Yes":"No");
	                    }
	                    m_statusBox.append(message);
	                    
	                }
	                
	                //Battery Condition
	                else if (selectedItemIndex == 1){
	                    
	                    BatteryCondition battCond = new BatteryCondition(conn);
	                    battCond.queryPrinter(1000);

	                    if (battCond.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Power Source Plugged in: %s\n", battCond.getChargerConnected()?"Yes":"No");
	                        message += String.format("Power Source: %s\n", battCond.getPowerSource().name());
	                        message += String.format("Battery Temperature: %f\n", battCond.getBatteryTemperature());
	                        message += String.format("Voltage Battery: %f\n", battCond.getVoltageBatterySingle());
	                        message += String.format("Voltage Battery 1: %f\n", battCond.getVoltageBattery1());
	                        message += String.format("Votlage Battery 2: %f\n", battCond.getVoltageBattery2());
	                        message += String.format("Voltage of Battery Eliminator: %f\n", battCond.getVoltageBatteryEliminator());
	                    }
	                    m_statusBox.append(message);

	                }
	                //Bluetooth Config
	                else if (selectedItemIndex == 2){
	                    BluetoothConfiguration btConfig = new BluetoothConfiguration(conn);
	                    btConfig.queryPrinter(1000);
	                   
	                    if (btConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Authentication Enable: %s\n", btConfig.getAuthentication()?"Yes":"No");
	                        message += String.format("MAC Address: %s\n", btConfig.getBluetoothAddress());
	                        message += String.format("Bondable: %s\n", btConfig.getBondable()?"Yes":"No");
	                        message += String.format("Connectable: %s\n", btConfig.getConnectable()?"Yes":"No");
	                        message += String.format("Discoverable: %s\n", btConfig.getDiscoverable()?"Yes":"No");
	                        message += String.format("Friendly Name: %s\n", btConfig.getFriendlyName());
	                        message += String.format("Inactivity timeout: %d\n", btConfig.getInactivityTimeout());
	                        message += String.format("Passkey enable: %s\n", btConfig.getPasskey()?"Yes":"No");
	                        message += String.format("Bluetooth Profile: %s\n", btConfig.getProfile());
	                        message += String.format("Service Name: %s\n", btConfig.getServiceName());
	                        message += String.format("Watchdog Period: %d\n", btConfig.getWatchdogPeriod());
	                    }
	                    m_statusBox.append(message);
	                }
	                
	                //Font List
	                else if (selectedItemIndex == 3){
	                    FontList fontList = new FontList(conn);
	                    fontList.queryPrinter(1000);
	                   
	                    if (fontList.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        List<FontData> files = fontList.getFonts();
	                        for (FontData font : files) {
	                            message += String.format("Five Character Name: %s\n", font.getFiveCharacterName());
	                            message += String.format("One Character Name: %s\n", font.getOneCharacterName());
	                            message += String.format("Memory Location: %s\n", font.getMemoryLocation());
	                            message += String.format("User Date: %s\n", font.getUserDate());
	                            message += String.format("Description: %s\n", font.getUserDescription());
	                            message += String.format("Version: %s\n", font.getUserVersion());
	                            message += "\n";
	                        }
	                    }
	                    m_statusBox.append(message);
	                }
	                //Format list
	                else if (selectedItemIndex == 4){
	                    FormatList formatList = new FormatList(conn);
	                    formatList.queryPrinter(1000);

	                    if (formatList.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        List<FormatData> files = formatList.getFormats();
	                        for (FormatData formatData : files) {
	                            message += String.format("Five Character Name: %s\n", formatData.getFiveCharacterName());
	                            message += String.format("One Character Name: %s\n", formatData.getOneCharacterName());
	                            message += String.format("Memory Location: %s\n", formatData.getMemoryLocation());
	                            message += String.format("User Date: %s\n", formatData.getUserDate());
	                            message += String.format("Description: %s\n", formatData.getUserDescription());
	                            message += String.format("Version: %s\n", formatData.getUserVersion());
	                            message += "\n";
	                        }
	                    }
	                    m_statusBox.append(message);

	                }
	                //General Config
	                else if (selectedItemIndex == 5){
	                    GeneralConfiguration genConfig = new GeneralConfiguration(conn);
	                    genConfig.queryPrinter(1000);

	                    if (genConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("White Space Advance Enable: %s\n", genConfig.getWhiteSpaceAdvance()?"Yes":"No");
	                        message += String.format("Serial Baud Rate: %s\n", genConfig.getBaudRate().name());
	                        message += String.format("Darkness Adjustment: %d\n", genConfig.getDarknessAdjustment());
	                        message += String.format("Form Feed Enable: %s\n", genConfig.getFormFeed()?"Yes":"No");
	                        message += String.format("Charger Beep Enable: %s\n", genConfig.getChargerBeep()?"Yes":"No");
	                        message += String.format("Sound Enable(Beeper On): %s\n", genConfig.getSoundEnabled()?"Yes":"No");
	                        message += String.format("Serial Handshake: %s\n", genConfig.getRS232Handshake().name());
	                        message += String.format("Lines Per Page: %d\n", genConfig.getLinesPerPage());
	                        message += String.format("Print Job Status Report Enable: %s\n", genConfig.getEZPrintJobStatusReport()?"Yes":"No");
	                        message += String.format("Default Protocol: %s\n", genConfig.getDefaultProtocol());
	                        message += String.format("Self Test Print Language: %d\n", genConfig.getSelfTestPrintLanguage());
	                        message += String.format("Form Feed Centering: %s\n", genConfig.getFormFeedCentering()?"Yes":"No");
	                        message += String.format("Serial Data Bits: %d\n", genConfig.getRS232DataBits());
	                        message += String.format("Serial Parity: %s\n", genConfig.getRS232Parity().name());
	                        message += String.format("Form Feed Button Disabled: %s\n", genConfig.getFormfeedButtonDisabled()?"Yes":"No");
	                        message += String.format("Power Button Disabled: %s\n", genConfig.getPowerButtonDisabled()?"Yes":"No");
	                        message += String.format("RF Button Disabled: %s\n", genConfig.getPowerButtonDisabled()?"Yes":"No");
	                        message += String.format("QStop Multiplier: %d\n", genConfig.getQStopMultiplier());
	                        message += String.format("RF Timeout: %d\n", genConfig.getRFPowerTimeout());
	                        message += String.format("System Timeout: %s\n", genConfig.getSystemTimeout());
	                        message += String.format("Special Test Print: %d\n", genConfig.getSpecialTestPrint());
	                        message += String.format("Paper Out Beep: %s\n", genConfig.getPaperOutBeep().name());
	                        message += String.format("USB Class: %s\n", genConfig.getUSBClass().name());
	                        message += String.format("Using USB: %s\n", genConfig.getUsingUSB()?"Yes":"No");
	                        message += String.format("Deep Sleep Enable: %s\n", genConfig.getDeepSleep()?"Yes":"No");
	                    }
	                    	m_statusBox.append(message);

	                }
	                //General status
	                else if (selectedItemIndex == 6){
	                    GeneralStatus genStatus = new GeneralStatus(conn);
	                    genStatus.queryPrinter(1000);

	                    if (genStatus.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Battery Temp and Voltage Status: %s\n", genStatus.getBatteryTempandVoltageStatus().name());
	                        message += String.format("Error Status: %s\n", genStatus.getErrorStatus().name());
	                        message += String.format("Paper Jam: %s\n", genStatus.getPaperJam());
	                        message += String.format("Printer Status: %s\n", genStatus.getPrinterStatus().name());
	                        message += String.format("Remaining RAM: %d\n", genStatus.getRemainingRAM());
	                        message += String.format("Paper Present: %s\n", genStatus.getPaperPresent());
	                        message += String.format("Head Lever Position: %s\n", genStatus.getHeadLeverPosition());
	                    }
	                    m_statusBox.append(message);
	                }
	                //Graphic List
	                else if (selectedItemIndex == 7){
	                    GraphicList graphList = new GraphicList(conn);
	                    graphList.queryPrinter(1000);
	                    
	                    if (graphList.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        List<GraphicData> files = graphList.getGraphics();
	                        for (GraphicData graphic : files) {
	                            message += String.format("Name: %s\n", graphic.getFiveCharacterName());
	                            message += String.format("Name: %s\n", graphic.getOneCharacterName());
	                            message += String.format("Memory Location: %s\n", graphic.getMemoryLocation());
	                            message += String.format("User Date: %s\n", graphic.getUserDate());
	                            message += String.format("Description: %s\n", graphic.getUserDescription());
	                            message += String.format("Version: %s\n", graphic.getUserVersion());
	                            message += "\n";
	                        }
	                    }
	                    
	                    m_statusBox.append(message);
	                }
	                //IrDA Config
	                else if (selectedItemIndex == 8){
	                    IrDAConfiguration irDAConfig = new IrDAConfiguration(conn);
	                    irDAConfig.queryPrinter(1000);
	                   
	                    if (irDAConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Direct Version: %s\n", irDAConfig.getDirectVersion());
	                        message += String.format("IrDA Name: %s\n", irDAConfig.getIrDAName());
	                        message += String.format("IrDA Nickname: %s\n", irDAConfig.getIrDANickname());
	                        message += String.format("IrDA Version: %s\n", irDAConfig.getIrDAVersion());
	                        message += String.format("Protocol: %s\n", irDAConfig.getProtocol());
	                    }
	                    m_statusBox.append(message);
	                }
	                //Label Config
	                else if (selectedItemIndex == 9){
	                    LabelConfiguration labelConfig = new LabelConfiguration(conn);
	                    labelConfig.queryPrinter(1000);

	                    if (labelConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Backup Distance: %d\n", labelConfig.getBackUpDistance());
	                        message += String.format("Use Presenter: %s\n", labelConfig.getUsePresenter()?"Yes":"No");
	                        message += String.format("Auto QMark Advance: %s\n", labelConfig.getAutoQMarkAdvance()?"Yes":"No");
	                        message += String.format("Backup Offset: %d\n", labelConfig.getBackupOffset());
	                        message += String.format("Horizontal Offset: %d\n", labelConfig.getHorizontalOffset());
	                        message += String.format("QMark Stop Length: %d\n", labelConfig.getQMarkStopLength());
	                        message += String.format("Additional Self Test Prints: %d\n", labelConfig.getAdditionalSelfTestPrints());
	                        message += String.format("Max QMark Advance: %d\n", labelConfig.getMaximumQMarkAdvance());
	                        message += String.format("QMARKB offset: %d\n", labelConfig.getQMARKBOffset());
	                        message += String.format("QMARKG Offset: %d\n", labelConfig.getQMARKGOffset());
	                        message += String.format("QMARKT Offset: %d\n", labelConfig.getQMARKTOffset());
	                        message += String.format("White QMark Enable: %s\n", labelConfig.getWhiteQMark()?"Yes":"No");
	                        message += String.format("Paperout Sensor: %s\n", labelConfig.getPaperoutSensor().name());
	                        message += String.format("Paper Stock Type: %s\n", labelConfig.getPaperStockType().name());
	                        message += String.format("Presenter Timeout: %d\n", labelConfig.getPresenterTimeout());
	                        message += String.format("Auto QMark Backup: %s\n", labelConfig.getAutoQMarkBackup()?"Yes":"No");
	                    }
	                    m_statusBox.append(message);

	                }
	                //Magnetic Card
	                else if (selectedItemIndex == 10){
	                    MagneticCardConfiguration magConfig = new MagneticCardConfiguration(conn);
	                    magConfig.queryPrinter(1000);
	                    
	                    if (magConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Auto Print: %s\n", magConfig.getAutoPrint()?"Yes":"No");
	                        message += String.format("Card Read Direction: %s\n", magConfig.getCardReadDirection());
	                        message += String.format("Magnetic Card Enabled: %s\n", magConfig.getEnabled()?"Yes":"No");
	                        message += String.format("Auto Send: %s\n", magConfig.getAutoSend()?"On":"Off");
	                        message += String.format("Track 1 Enabled: %s\n", magConfig.getTrack1Enabled()?"Yes":"No");
	                        message += String.format("Track 2 Enabled: %s\n", magConfig.getTrack2Enabled()?"Yes":"No");
	                        message += String.format("Track 3 Enabled: %s\n", magConfig.getTrack3Enabled()?"Yes":"No");
	                    }
	                    m_statusBox.append(message);
	                }
	                //Magnetic Card Data
	                else if (selectedItemIndex == 11){
	                    MagneticCardData magCardData = new MagneticCardData(conn);
	                    magCardData.queryPrinter(1000);
	                    
	                    if (magCardData.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Track 1 Data: %s\n", magCardData.getTrack1Data());
	                        message += String.format("Track 2 Data: %s\n", magCardData.getTrack2Data());
	                        message += String.format("Track 3 Data: %s\n", magCardData.getTrack3Data());
	                    }
	                    m_statusBox.append(message);

	                }
	                //Manufacturing Date
	                else if (selectedItemIndex == 12){
	                    ManufacturingDate manuDate = new ManufacturingDate(conn);
	                    manuDate.queryPrinter(1000);
	   
	                    if (manuDate.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Manufacturing Date: %s\n", manuDate.getMD());
	                    }
	                    m_statusBox.append(message);
	                }
	                //Memory status
	                else if (selectedItemIndex == 13){
	                    MemoryStatus memStatus = new MemoryStatus(conn);
	                    memStatus.queryPrinter(1000);
	  
	                    if (memStatus.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Download memory remaining: %d\n", memStatus.getDownloadMemoryRemaining());
	                        message += String.format("Download memory total: %d\n", memStatus.getDownloadMemoryTotal());
	                        message += String.format("EEPROM Size: %d\n", memStatus.getEEPROMSize());
	                        message += String.format("Flash Memory Size: %d\n", memStatus.getFlashMemorySize());
	                        message += String.format("RAM size: %d\n", memStatus.getRAMSize());
	                        message += String.format("Flash type: %s\n", memStatus.getFlashType());
	                        message += String.format("Download Format Memory Remaining: %d\n", memStatus.getDownloadFormatMemoryRemaining());
	                        message += String.format("Download Format Memory Total: %d\n", memStatus.getDownloadFormatMemoryTotal());
	                    }
	                    m_statusBox.append(message);

	                }
	                //Printer Options
	                else if (selectedItemIndex == 14){
	                    PrinterOptions printerOpt = new PrinterOptions(conn);
	                    printerOpt.queryPrinter(1000);
	
	                    if (printerOpt.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("SCR Device: %d\n", printerOpt.getSCRDevice());
	                        message += String.format("CF Device: %d\n", printerOpt.getCFDevice());
	                        message += String.format("Printer Description: %s\n", printerOpt.getPrinterDescription());
	                        message += String.format("Part Number: %s\n", printerOpt.getPartNumber());
	                        message += String.format("Serial Number: %s\n", printerOpt.getSerialNumber());
	                        message += String.format("Printer Type: %d\n", printerOpt.getPrinterType());
	                        message += String.format("SPI Device: %d\n", printerOpt.getSPIDevice());
	                        message += String.format("Manufacturing Date: %s\n", printerOpt.getManufacturingDate());
	                        message += String.format("Text Fixture String: %s\n", printerOpt.getTextFixtureString());
	                        message += String.format("SDIO Device: %d\n", printerOpt.getSDIODevice());
	                        message += String.format("Certification Flag Status: %s\n", printerOpt.getCertificationFlagStatus()?"On":"Off");
	                    }
	                    m_statusBox.append(message);
	                }
	                //PrintHead Status
	                else if (selectedItemIndex == 15){
	                    PrintheadStatus printHeadStats = new PrintheadStatus(conn);
	                    printHeadStats.queryPrinter(1000);
	           
	                    if (printHeadStats.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("DPI: %d\n", printHeadStats.getDPI());
	                        message += String.format("PrintHead Model: %s\n", printHeadStats.getPrintheadModel());
	                        message += String.format("Print Time: %d\n", printHeadStats.getPrintTime());
	                        message += String.format("PrintHead Pins: %d\n", printHeadStats.getPrintheadPins());
	                        message += String.format("PrintHead Temperature: %f\n", printHeadStats.getPrintheadTemperature());
	                        message += String.format("PrintHead Width: %d\n", printHeadStats.getPrintheadWidth());
	                        message += String.format("Page Width: %d\n", printHeadStats.getPageWidth());
	                    }
	                    m_statusBox.append(message);

	                }
	                //Serial Number
	                else if (selectedItemIndex == 16){
	                    SerialNumber serialNum = new SerialNumber(conn);
	                    serialNum.queryPrinter(1000);
	
	                    if (serialNum.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Serial Number: %s\n", serialNum.getSN());
	                    }
	                    m_statusBox.append(message);
	                }
	                //Smart Card Config
	                else if (selectedItemIndex == 17){
	                    SmartCardConfiguration smartCardConfig = new SmartCardConfiguration(conn);
	                    smartCardConfig.queryPrinter(1000);
	                   
	                    if (smartCardConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Command Format: %s\n", smartCardConfig.getCommandFormat());
	                        message += String.format("Enable: %s\n", smartCardConfig.getEnabled()?"Yes":"No");
	                        message += String.format("Memory Tye: %s\n", smartCardConfig.getMemoryType());
	                        message += String.format("Response Format: %s\n", smartCardConfig.getResponseFormat());
	                        message += String.format("Smart Card Protocol: %s\n", smartCardConfig.getProtocol());
	                        message += String.format("Smart Card Type: %s\n", smartCardConfig.getType());
	                    }
	                    m_statusBox.append(message);
	                }
	                //TCPIPStatus
	                else if (selectedItemIndex == 19){
	                    TCPIPStatus tcpStatus = new TCPIPStatus(conn);
	                    tcpStatus.queryPrinter(1000);
	                    
	                    if (tcpStatus.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Wireless Card Info: %s\n", tcpStatus.getWirelessCardInfo());
	                        message += String.format("Valid Cert. Present: %s\n", tcpStatus.getValidCertificatePresent()?"Yes":"No");
	                        message += String.format("Conn. Reporting Enable: %s\n", tcpStatus.getConnectionReporting()?"Yes":"No");
	                        message += String.format("Acquired IP: %s\n", tcpStatus.getAcquireIP().name());
	                        message += String.format("Radio Disable: %s\n", tcpStatus.getRadioDisabled()?"Yes":"No");
	                        message += String.format("ESSID: %s\n", tcpStatus.getESSID());
	                        message += String.format("EAP Type: %s\n", tcpStatus.getEAPType().name());
	                        message += String.format("Gateway Address: %s\n", tcpStatus.getGatewayAddress());
	                        message += String.format("IP Address: %s\n", tcpStatus.getIPAddress());
	                        message += String.format("Inactivity Timeout: %d\n", tcpStatus.getInactivityTimeout());
	                        message += String.format("Key to Use: %d\n", tcpStatus.getKeyToUse());
	                        message += String.format("Key 1 Type: %s\n", tcpStatus.getKey1Type().name());
	                        message += String.format("Key 2 Type: %s\n", tcpStatus.getKey2Type().name());
	                        message += String.format("Key 3 Type: %s\n", tcpStatus.getKey3Type().name());
	                        message += String.format("Key 4 Type: %s\n", tcpStatus.getKey4Type().name());
	                        message += String.format("Subnet Mask: %s\n", tcpStatus.getSubnetMask());
	                        message += String.format("MAC Address: %s\n", tcpStatus.getMACAddress());
	                        message += String.format("Station Name: %s\n", tcpStatus.getStationName());
	                        message += String.format("Network Authentication: %s\n", tcpStatus.getNetworkAuthentication().name());
	                        message += String.format("TCP Printing Port: %d\n", tcpStatus.getTCPPrintingPort());
	                        message += String.format("Power Saving Mode: %s\n", tcpStatus.getPowerSavingMode()?"Yes":"No");
	                        message += String.format("Phase 2 Method: %s\n", tcpStatus.getPhase2Method().name());
	                        message += String.format("UDP Printing Port: %d\n", tcpStatus.getUDPPrintingPort());
	                        message += String.format("Card Powered: %s\n", tcpStatus.getCardPowered()?"On":"Off");
	                        message += String.format("Signal Quality Indicator: %s\n", tcpStatus.getSignalQualityIndicator()?"Yes":"No");
	                        message += String.format("Authentication Algorithm: %s\n", tcpStatus.getAuthenticationAlgorithm().name());
	                        message += String.format("Network Type: %s\n", tcpStatus.getNetworkType().name());
	                        message += String.format("Encryption Enabled: %d\n", tcpStatus.getEncryptionEnabled());
	                        message += String.format("Current Certificate CRC: %s\n", tcpStatus.getCurrentCertificateCRC());
	                        message += String.format("DNS1 Address: %s\n", tcpStatus.getDNS1Address());
	                        message += String.format("Register to DNS: %s\n", tcpStatus.getRegisterToDNS()?"Yes":"No");
	                        message += String.format("DNS2 Address: %s\n", tcpStatus.getDNS2Address());
	                        message += String.format("Static DNS Enable: %s\n", tcpStatus.getStaticDNS()?"Yes":"No");
	                        message += String.format("Group Cipher: %d\n", tcpStatus.getGroupCipher());
	                        message += String.format("Radio Type: %s\n", tcpStatus.getRadioType().name());
	                        message += String.format("Use DNS: %s\n", tcpStatus.getUseDNS()?"Yes":"No");
	                        message += String.format("DNS Suffix: %s\n", tcpStatus.getDNSSuffix());
	                        message += String.format("Encryption Key Size: %d\n", tcpStatus.getEncryptionKeySize());
	                        message += String.format("Encryption Key Type: %d\n", tcpStatus.getEncryptionKeyType());
	                    }
	                    m_statusBox.append(message);

	                }
	                //Upgrade Data
	                else if (selectedItemIndex == 20){
	                    UpgradeData upgradeData = new UpgradeData(conn);
	                    upgradeData.queryPrinter(1000);
	                    
	                    if (upgradeData.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Path and File: %s\n", upgradeData.getPathAndFile());
	                        message += String.format("Server IP: %s\n", upgradeData.getServerIPAddress());
	                        message += String.format("Server Port: %d\n", upgradeData.getServerPort());
	                        message += String.format("Upgrade Type: %d\n", upgradeData.getDataType());
	                        message += String.format("Upgrade Package Version: %s\n", upgradeData.getVersion());
	                    }
	                    m_statusBox.append(message);

	                }
	                //Version Info
	                else if (selectedItemIndex == 21){
	                    VersionInformation versionInfo = new VersionInformation(conn);
	                    versionInfo.queryPrinter(1000);
	                   
	                    if (versionInfo.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Boot Version: %s\n", versionInfo.getBootVersion());
	                        message += String.format("Comm Controller Version: %s\n", versionInfo.getCommControllerVersion());
	                        message += String.format("Download version: %s\n", versionInfo.getDownloadVersion());
	                        message += String.format("Firmware version: %s\n", versionInfo.getFirmwareVersion());
	                        message += String.format("Hardware Controller Version: %s\n", versionInfo.getHardwareControllerVersion());
	                        message += String.format("SCR Version: %s\n", versionInfo.getSCRVersion());
	                        message += String.format("Build Timestamp: %s\n", versionInfo.getBuildTimestamp());
	                    }
	             
	                    m_statusBox.append(message);
	                }
	            }
	            Thread.sleep(2000);
				//signals to close connection
				conn.close();
                m_statusBox.append("Query success.\r\n");
                EnableControls(true);
			}

		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			//signals to close connection
			if(conn != null)
				conn.close();
			e.printStackTrace();
			m_statusBox.append("Error: " + e.getMessage());
			EnableControls(true);
		}
	}// run()
}
