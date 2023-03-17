package com.sparrow.sms;

import java.io.IOException;
import java.io.OutputStream;

import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

import com.sparrow.scheduler.PortAllocator;

public class SmsEngine {

	private OutputStream outputStream;
	// private InputStream inputStream = null;
	private SerialPort serialPort;

	public void sendTextModeSms(String number, String message) throws PortInUseException, IOException, UnsupportedCommOperationException {
		serialPort = PortAllocator.getInstance().getSerialPort();
		System.out.println("Got serial port : " + serialPort.getName());
		outputStream = serialPort.getOutputStream();
		// inputStream = serialPort.getInputStream();
		serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		printf("AT+CMGF=1\r\n");
		delayz(2);
		printf("AT+CMGS=\"");
		printf(number);
		printf("\"");
		printf("\r\n");
		printf(message);
		printf("\u001A");
		outputStream.write('\032');
		printf("\r\n");
		delayz(2);

		outputStream.flush();
		outputStream.close();
		// serialPort.close();
		delayz(1);

	}

	private void printf(String str) throws IOException {
		outputStream.write(str.getBytes());
	}

	private static void delayz(int i) {
		try {
			Thread.sleep(i * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
