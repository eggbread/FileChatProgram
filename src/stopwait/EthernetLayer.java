package stopwait;

import java.io.IOException;
import java.util.ArrayList;


public class EthernetLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	_ETHERNET_Frame frame = new _ETHERNET_Frame();
	//_ACK_Frame ackFrame = new _ACK_Frame();
	private class _ETHERNET_ADDR{
		private byte[] addr = new byte[6];
		
		public _ETHERNET_ADDR() {
			this.addr[0]=(byte)0x00;
			this.addr[1]=(byte)0x00;
			this.addr[2]=(byte)0x00;
			this.addr[3]=(byte)0x00;
			this.addr[4]=(byte)0x00;
			this.addr[5]=(byte)0x00;
		}
	}
	private class _ETHERNET_Frame{
		_ETHERNET_ADDR enet_dstaddr;
		_ETHERNET_ADDR enet_srcaddr;
		byte[] enet_type;
		byte[] enet_data;
		public _ETHERNET_Frame() {
			// TODO Auto-generated constructor stub
			this.enet_dstaddr=new _ETHERNET_ADDR();
			this.enet_srcaddr=new _ETHERNET_ADDR();
			this.enet_type=new byte[2];
			this.enet_data=null;
		}
	}

	public void setType(int type) {
		this.frame.enet_type[0]=(byte)type;
	}
	public void setAddress(byte[] input) { 
		this.frame.enet_dstaddr.addr=input;
		try {
			frame.enet_srcaddr.addr=((NILayer)this.GetUnderLayer()).device.getHardwareAddress();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean Send(byte[] input,int length){
		this.frame.enet_type[0]=(byte)0x20;
		if(input[0]==0x00) {//처음 byte가 0이면 채팅 1이면 파일
			this.frame.enet_type[1]=(byte) 0x80;
		}else if(input[0]==0x01) {	
			this.frame.enet_type[1]=(byte) 0x90;
		}

		byte[] byteFrame = new byte[input.length+14];
		System.arraycopy(this.frame.enet_dstaddr.addr, 0, byteFrame, 0, 6);
		System.arraycopy(this.frame.enet_srcaddr.addr, 0, byteFrame, 6, 6);
		System.arraycopy(this.frame.enet_type, 0, byteFrame, 12, 2);
		System.arraycopy(input, 0, byteFrame, 14, length);
		this.GetUnderLayer().Send(byteFrame , length+14);
		return true;
	}
	
	
	public boolean Receive(byte[] input) {
		byte broad = (byte) 0xff;
		boolean found = false;
		byte[] data;
		//주소 및 브래드캐스트 확인
		if ((input[0] == broad && input[1] == broad && input[2] == broad && input[3] == broad && input[4] == broad
				&& input[5] == broad)) {
			found = true;
			input[14] = (byte) 0xff;
		} else {
			found = true;
			for (int i = 0; i < 6; i++) {//목적지 주소가 자신의 이더넷 주소
				if (input[i] != this.frame.enet_srcaddr.addr[i]) {
					found = false;
				}
			}
		}
		
		if (found) {
			if(input[12]==(byte)0x20&&input[13]==(byte)0x80) {//채팅			
				if (input[17] == 0) {//단편화 안됌
					data = RemoveEtherHeader(input, input.length);
					this.GetUpperLayer(0).Receive(data);
					return true;
				} else if (input[17] == 1) {//처음
					data = RemoveEtherHeader(input, input.length);
					this.GetUpperLayer(0).Receive(data);
					return true;
				} else if (input[17] == 2) {//중간
					data = RemoveEtherHeader(input, input.length);
					this.GetUpperLayer(0).Receive(data);
					return true;
				} else if (input[17] == 3) {//끝
					data = RemoveEtherHeader(input, input.length);
					this.GetUpperLayer(0).Receive(data);
					return true;
				}
				return false;
			}

			if (input[12] == (byte) 0x20 && input[13] == (byte) 0x90) {// 파일
				data = RemoveEtherHeader(input, input.length);
				this.GetUpperLayer(1).Receive(data);
				return true;
			}
//			if (input[12] == (byte) 0x20 && input[13] == (byte) 0x70) {// 파일
//				data = RemoveEtherHeader(input, input.length);
//				((StopWaitDlg)this.GetUpperLayer(1).GetUpperLayer(0)).startSend();
//				return true;
//			}
		}
		return false;
	}

	public byte[] RemoveEtherHeader(byte[] input, int length) {
		byte[] temp = new byte[length - 14];
		for (int i = 0; i < length-14;i++) {
			temp[i]=input[i+14];
		}
		return temp;
	}
	public EthernetLayer(String pName){
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		
	}
	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return this.pLayerName;
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
		if(pUnderLayer==null)
			return;
		p_UnderLayer=pUnderLayer;
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
