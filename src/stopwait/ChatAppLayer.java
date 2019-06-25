package stopwait;

import java.util.ArrayList;


public class ChatAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public int ackCount=0,fragNum=0,rest=0,reCount=0;
	public byte[] buf;
	
	private class _CAPP_APP {
		byte[] capp_totlen;
		byte capp_type;
		byte capp_unused;
		byte[] capp_data;
		byte capp_check;

		public _CAPP_APP() {
			this.capp_check=0x00;
			this.capp_totlen = new byte[2];
			this.capp_type = 0x00;
			this.capp_unused = 0x00;
			this.capp_data = null;
		}
	}

	_CAPP_APP m_sHeader = new _CAPP_APP();

	public ChatAppLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}

	public void ResetHeader() {
		for (int i = 0; i < 2; i++) {
			m_sHeader.capp_totlen[i] = (byte) 0x00;
		}
		m_sHeader.capp_data = null;
	}

	public byte[] ObjToByte(_CAPP_APP Header, byte[] input, int length,int types) {
		byte[] buf = new byte[length + 5];
		byte[] totlen = Header.capp_totlen;
		byte type = (byte)types;
		byte unused=Header.capp_unused;
		
		buf[0] = Header.capp_check;
		buf[1] = totlen[0];
		buf[2] = totlen[1];
		buf[3] = type;
		buf[4] = unused;

		for (int i = 0; i < length; i++)
			buf[5 + i] = input[i];
		return buf;
	}
	public boolean Send() {
		this.Send(this.m_sHeader.capp_data, this.m_sHeader.capp_data.length);
		return false;
	}
	public void makeBuf(byte[] input) {//버퍼에 추가하는 메소드
		int i=0;
		if(reCount==fragNum) {//마지막 조각일 때 
			while(i<rest) {//rest만큼 반복
				this.buf[(this.reCount)*1456+i]=input[i];
				i++;
			}
			return;
		}
		while(i<1456) {//10번 반복하며 버퍼에 단편화 조각 추가
			this.buf[(this.reCount)*1456+i]=input[i];
			i++;
		}
		this.reCount++;
	}
	public boolean Send(byte[] input, int length) {
		byte inputLength = (byte) input.length;//전체 길이를 저장한다.
		this.m_sHeader.capp_totlen[0]=(byte) (inputLength&0xff00);//헤더의 전체 길이를 넣는다.
		this.m_sHeader.capp_totlen[1]=(byte) (inputLength&0xff);
		if (length > 1456) {//10보다 클 때 단편화가 필요하다.
			byte[] bytes;
			byte[] temp;
			if(this.ackCount== fragNum-1) {//단편화의 마지막 조각일 때이다.
				temp=new byte[rest];//남은 길이만큼 배열을 생성하고 복사한다.
				System.arraycopy(this.m_sHeader.capp_data, this.ackCount * 1456, temp, 0,rest);
			}else {
				temp = new byte[1456];//10길이의 배열을 생성하고 복사한다.
				System.arraycopy(this.m_sHeader.capp_data, this.ackCount * 1456, temp, 0, 1456);
			}
			if (this.ackCount == 0) {//첫번째 조각일 때
				bytes = ObjToByte(m_sHeader, temp, 1456, 1);//헤더를 붙인다.
				this.ackCount++;//다음 조각을 알수 있도록 1증가한다.
			} else if (this.ackCount == fragNum-1) {//단편화의 마지막 조각일 때
				bytes = ObjToByte(m_sHeader, temp, rest, 3);//헤더를 붙인다.
				this.fragNum=0;
				this.rest=0;
				this.ackCount=0;//다 보냈으니 0으로 초기화한다.
			} else {//중간 조각일 때
				bytes = ObjToByte(m_sHeader, temp, 1456, 2);//헤더를 붙인다.
				this.ackCount++;//다음 조각을 알수 있도록 1증가한다.
			}
			this.GetUnderLayer().Send(bytes, 1460);//Ethernet으로 전달한다.
		} else {//10이하의 길이는 단편화 없이 전송한다.
			byte[] bytes = ObjToByte(m_sHeader, input, length, 0);
			this.GetUnderLayer().Send(bytes, length+5);
		}
		return true;
	}
	public void setData(byte[] input) {//채팅창에서 입력하고 send버튼을 눌렀을 때 실행
		this.m_sHeader.capp_data=new byte[input.length];
		this.m_sHeader.capp_data=input;//데이터의 길이를 헤더에 저장한다.
		fragNum=input.length/1456+1;//단편화 조각개수이다.
		rest=input.length%1456;//10으로 나눴을 때 남은 수이다.
	}
	public byte[] RemoveCappHeader(byte[] input, int length) {
		byte[] temp = new byte[length-4];
		for(int i =0;i<length-5;i++) {
			temp[i]=input[i+5];
		}
		return temp;// 변경하세요 필요하시면
	}
	
	public synchronized boolean Receive(byte[] input) {
		byte[] data;
		
		if(input[0]==(byte) 0xff) {//브로드캐스트일 때 버린다.
			//this.GetUpperLayer(0).Receive(new String("보로드캐스트입니다.").getBytes());
			return true;
		}
		if(input[3]==1) {//단편화 첫 조각일 때
			int totlen=((((int)input[1]&0xff)<<8)|((int)input[0]&0xff));//전체길이 저장
			this.buf=new byte[totlen];//전체길이만큼 버퍼 생성
			this.fragNum=totlen/1456;//단편화 조각 길이
			this.rest=totlen%1456;//남은 길이
			data = RemoveCappHeader(input, input.length);//헤더 제거
			this.makeBuf(data);//버퍼에 추가
		}else if(input[3]==2) {//중간 조각일 때
			data = RemoveCappHeader(input, input.length);//헤더 제거
			this.makeBuf(data);//버퍼에 추가
		}else if(input[3]==3) {//마지막 조각일 때
			data = RemoveCappHeader(input, input.length);//헤더 제거
			this.makeBuf(data);//버퍼에 추가
			this.reCount=0;//변수들 초기화
			this.rest=0;
			this.fragNum=0;
			this.GetUpperLayer(0).Receive(buf);//버퍼를 위로 전달
			this.buf=null;//버퍼 초기화
		}else {
		data = RemoveCappHeader(input, input.length);
		this.GetUpperLayer(0).Receive(data);
		}
		// 주소설정
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
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
		// nUpperLayerCount++;

	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}

}
