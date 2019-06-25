package stopwait;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

public class NILayer implements BaseLayer {
	int m_iNumAdapter;
	public int nUpperLayerCount = 0;
	public Pcap m_AdapterObject;
	public PcapIf device;
	public List<PcapIf> m_pAdapterList;
	StringBuilder errbuf = new StringBuilder();
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	public NILayer(String pName) {
		pLayerName = pName;
		m_pAdapterList = new ArrayList<PcapIf>();
		m_iNumAdapter = 0;
		SetAdapterList();
	}

	public String get_description(int index) {
		return this.m_pAdapterList.get(index).getDescription();
	}

	public List get_m_List() {
		return this.m_pAdapterList;
	}

	public void SetAdapterList() {
		int r = Pcap.findAllDevs(m_pAdapterList, errbuf);
		if (r == Pcap.NOT_OK || m_pAdapterList.isEmpty()) {
			System.out.printf("Can't read list of devices, error is %s,", errbuf.toString());
			return;
		}
	}

	public String getSrcAddressToString() {
		byte[] mac = new byte[6];
		String temp = new String();
		try {
			mac = this.m_pAdapterList.get(m_iNumAdapter).getHardwareAddress();
			for (int i = 0; i < 6; i++) {
				temp += String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : "");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return temp;
	}

	public byte[] getDstAddress() {
		try {
			return this.m_pAdapterList.get(m_iNumAdapter).getHardwareAddress();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public String getDstAddressToString(int index) {
		byte[] mac = new byte[6];
		String temp = new String();
		try {
			mac = this.m_pAdapterList.get(index).getHardwareAddress();
			for (int i = 0; i < 6; i++) {
				temp += String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : "");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return temp;
	}

	public void SetAdapterNumber(int iNum) {
		m_iNumAdapter = iNum;
		PacketStartDriver();
		Receive();
		this.device = this.m_pAdapterList.get(iNum);
	}

	public void PacketStartDriver() {
		int snaplen = 64 * 1024;
		int flags = Pcap.MODE_PROMISCUOUS;
		int timeout = 10 * 1000;
		m_AdapterObject = Pcap.openLive(m_pAdapterList.get(m_iNumAdapter).getName(), snaplen, flags, timeout, errbuf);
	}

	public boolean Receive() {
		Receive_Thread thread = new Receive_Thread(m_AdapterObject, this.GetUpperLayer(0));
		Thread obj = new Thread(thread);
		obj.start();

		return false;
	}

	class Receive_Thread implements Runnable {
		byte[] data;
		Pcap AdapterObject;
		BaseLayer UpperLayer;

		public Receive_Thread(Pcap m_AdapterObject, BaseLayer m_UpperLayer) {
			AdapterObject = m_AdapterObject;
			UpperLayer = m_UpperLayer;
		}

		@Override
		public void run() {
			while (true) {
				PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
					public void nextPacket(PcapPacket packet, String user) {
						data = packet.getByteArray(0, packet.size());
						UpperLayer.Receive(data);
					}
				};
				AdapterObject.loop(100000, jpacketHandler, "");
			}
		}
	}

	public boolean Send(byte[] input, int length) {
		ByteBuffer buf = ByteBuffer.wrap(input);
		if (m_AdapterObject.sendPacket(buf) != Pcap.OK) {
			System.err.println(m_AdapterObject.getErr());
			return false;
		}
		return true;
	}

	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		// TODO Auto-generated method stub
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		// TODO Auto-generated method stub
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}
}
