import com.virtenio.vm.Time;
import com.virtenio.radio.RadioDriverException;
import com.virtenio.radio.ieee_802_15_4.Frame;
import java.io.IOException;
import java.util.Arrays;

import com.virtenio.driver.device.ADXL345;
import com.virtenio.driver.device.at86rf231.*;
import com.virtenio.driver.gpio.GPIO;
import com.virtenio.driver.gpio.NativeGPIO;
import com.virtenio.driver.spi.NativeSPI;

import com.virtenio.io.ChannelBusyException;
import com.virtenio.io.NoAckException;

import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.preon32.shuttle.Shuttle;

import com.virtenio.driver.usart.USART;
import com.virtenio.preon32.examples.common.RadioInit;

public class nSensor {
	private int COMMON_CHANNEL = 24;
	private int COMMON_PANID = 0xCAFE;
	//private int BROADCAST_ADDR = 0xFFFF;
	private int NODESENSOR_ADDR = 0xDFFE;
	private int BASESTATION_ADDR = 0xBABE;
	
	private ADXL345 accelerationSensor;
	private GPIO accelIrqPin1;
	private GPIO accelIrqPin2;
	private GPIO accelCs;
	
	AT86RF231 radio;
	Shuttle shuttle;
	
	long hour7 = 25200000;

	public void initRadio() throws Exception {
		radio = RadioInit.initRadio();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(BASESTATION_ADDR);
	}
	
	private void init() throws Exception 
	{
		accelIrqPin1 = NativeGPIO.getInstance(37);
		accelIrqPin2 = NativeGPIO.getInstance(25);
		accelCs = NativeGPIO.getInstance(20);
		
		NativeSPI spi = NativeSPI.getInstance(0);
		spi.open(ADXL345.SPI_MODE, ADXL345.SPI_BIT_ORDER, ADXL345.SPI_MAX_SPEED);
		accelerationSensor = new ADXL345(spi, accelCs);
		accelerationSensor.open();
		accelerationSensor.setDataFormat(ADXL345.DATA_FORMAT_RANGE_2G);
		accelerationSensor.setDataRate(ADXL345.DATA_RATE_3200HZ);
		accelerationSensor.setPowerControl(ADXL345.POWER_CONTROL_MEASURE);
	}
	

	public void sendMessage(Frame frame) throws Exception 
	{
		new Thread() 
		{
			public void run()
			{
				try {
					radio.transmitFrame(frame); // 
					
				} catch (RadioDriverException e) {
					//e.printStackTrace();
				} catch (NoAckException e) {
					//e.printStackTrace();
				} catch (ChannelBusyException e) {
					//e.printStackTrace();
			}
			}
		}.start();
	}
	
	public void senseAccl() throws Exception {
		init();
		short[] values = new short[3];
		int sn = 1;
		String mesg = null;
		Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
				| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
			frame.setSrcAddr(NODESENSOR_ADDR);
			frame.setSrcPanId(COMMON_PANID);
			frame.setDestAddr(BASESTATION_ADDR);
			frame.setDestPanId(COMMON_PANID);
			radio.setState(AT86RF231.STATE_TX_ARET_ON);
		while (true) {
			try {
				accelerationSensor.getValuesRaw(values, 0);
				mesg = Arrays.toString(values);
				frame.setSequenceNumber(sn);
				frame.setPayload(mesg.getBytes());
				sendMessage(frame);
			} catch (Exception e) {
				System.out.println("ADXL345 error");
			}
			Thread.sleep(500);
			sn++;
		}
	}
	
	public static void main(String [] args ) throws Exception 
	{ 		
		nSensor ns = new nSensor();
		ns.initRadio();	
		long getTime = Time.currentTimeMillis()+ ns.hour7;
		System.out.println(stringFormatTime.SFFull(getTime) + "\n");
		ns.senseAccl();
	}
	
}