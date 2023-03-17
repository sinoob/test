package com.sparrow.scheduler;

import javax.comm.SerialPort;

public class PortAllocator {

	private static PortAllocator alloc = new PortAllocator();
	private static SerialPort serialPort;

	private PortAllocator() {

	}

	public static PortAllocator getInstance() {
		return alloc;
	}

	public void saveSerialPort(SerialPort sp) {
		alloc.serialPort = sp;
	}

	public SerialPort getSerialPort() {
		return alloc.serialPort;
	}

}
