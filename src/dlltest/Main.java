package dlltest;
import com.sun.jna.Library;
import com.sun.jna.Native;
public class Main {
    public interface TscLibDll extends Library {
        TscLibDll INSTANCE = (TscLibDll) Native.loadLibrary ("TSCLIB", TscLibDll.class);
       
        int about ();
        int openport (String pirnterName);
        int closeport ();
        int sendcommand (String printerCommand);
        int setup (String width,String height,String speed,String density,String sensor,String vertical,String offset);
        int downloadpcx (String filename,String image_name);
        int barcode (String x,String y,String type,String height,String readable,String rotation,String narrow,String wide,String code);
        int printerfont (String x,String y,String fonttype,String rotation,String xmul,String ymul,String text);
        int clearbuffer ();
        int printlabel (String set, String copy);
        int formfeed ();
        int nobackfeed ();
        int windowsfont (int x, int y, int fontheight, int rotation, int fontstyle, int fontunderline, String szFaceName, String content);
    }


    public static void main(String[] args) {
 
    	//Runtime.getRuntime().loadLibrary("TSCLIB");
    	TscLibDll.INSTANCE.about();
    	
    	TscLibDll.INSTANCE.openport("TSC TTP-244 Pro");

    	TscLibDll.INSTANCE.sendcommand("REM ***** This is a test by JAVA. *****");
    	TscLibDll.INSTANCE.setup("70", "25", "3", "1", "0", "2", "0");
    	TscLibDll.INSTANCE.clearbuffer();
    	TscLibDll.INSTANCE.printerfont ("1", "10", "3", "0", "1", "1", "SUGAR 1 KG");
    	TscLibDll.INSTANCE.printerfont ("2", "35", "3", "0", "1", "1", "EASTERN 1 KG");
    	TscLibDll.INSTANCE.printerfont ("10", "60", "3", "0", "1", "1", "SUREKHA 1 KG");
    	TscLibDll.INSTANCE.printerfont ("20", "85", "3", "0", "1", "1", "NIRAPARA 1 KG");
    	
    	TscLibDll.INSTANCE.printerfont ("321", "10", "3", "0", "1", "1", "SUGAR 1 KG");
    	TscLibDll.INSTANCE.printerfont ("322", "35", "3", "0", "1", "1", "EASTERN 1 KG");
    	TscLibDll.INSTANCE.printerfont ("330", "60", "3", "0", "1", "1", "SUREKHA 1 KG");
    	TscLibDll.INSTANCE.printerfont ("350", "85", "3", "0", "1", "1", "NIRAPARA 1 KG");
    	
    	//TscLibDll.INSTANCE.printerfont ("30", "50", "3", "0", "1", "1", "EASTERN 3 KG");
    	//TscLibDll.INSTANCE.barcode("10", "30", "128", "50", "1", "0", "2", "2", "123456789");
    	TscLibDll.INSTANCE.printlabel("1", "1");
    	TscLibDll.INSTANCE.closeport();
    }
}
