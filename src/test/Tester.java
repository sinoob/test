package test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Tester {
	public String sendSms() {
		try {
			// Construct data
			String apiKey = "apikey=" + "OPMY3iz6Ots-4dbyeyb8UQPu2iDLLSKb56BlvWGFBr";
			String message = "&message=" + "Sales Billed: Ajwa\nBillNo: 123\nAmount: 444.00\nTime: 12:00PM";
			String sender = "&sender=" + "AJWAFM";
			String numbers = "&numbers=" + "919895805341" ;

			// Send data
			String data = apiKey + sender + numbers + message;
			HttpURLConnection conn = (HttpURLConnection) new URL("https://api.textlocal.in/send/?").openConnection();
			System.out.println(data);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Length", Integer.toString(data.length()));
			conn.getOutputStream().write(data.getBytes("UTF-8"));
			final BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			final StringBuffer stringBuffer = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) {
				stringBuffer.append(line);
			}
			rd.close();

			System.out.println(stringBuffer.toString());
			return stringBuffer.toString();
		} catch (Exception e) {
			System.out.println("Error SMS " + e);
			return "Error " + e;
		}
	}
	
	public static void main(String[] args) {
		new Tester().sendSms();
	}
}