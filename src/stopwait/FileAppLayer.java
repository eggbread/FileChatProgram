package stopwait;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import javax.imageio.ImageIO;



public class FileAppLayer implements BaseLayer{
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public int ackCount=0,fragNum=0,rest=0,reCount=0;
	byte[] buf;
	public String filename;

	
	public class _FAPP_HEADER{
		byte fapp_check;
		byte[] fapp_totlen;
		byte[] fapp_type;
		byte fapp_msg_type;
		byte ed;
		byte[] fapp_seq_num;
		byte[] fapp_data;
		
		public _FAPP_HEADER() {
			this.fapp_check = 0x01;
			this.fapp_totlen = new byte[4];
			this.fapp_type = new byte[2];
			this.fapp_msg_type = 0x00;
			this.ed = 0x00;
			this.fapp_seq_num = new byte[4];
			this.fapp_data = null;
		}
	}
	public byte[] ObjToByte(_FAPP_HEADER Header, byte[] input, int length) {
		byte[] buf = new byte[length + 13];
		byte[] totlen = Header.fapp_totlen;
		byte[] type = Header.fapp_type;

		buf[0] = Header.fapp_check;
		buf[1] = totlen[0];
		buf[2] = totlen[1];
		buf[3] = totlen[2];
		buf[4] = totlen[3];
		buf[5] = type[0];
		buf[6] = type[1];
		buf[7] = Header.fapp_msg_type;
		buf[8] = Header.ed;
		buf[9] = Header.fapp_seq_num[0];
		buf[10] = Header.fapp_seq_num[1];
		buf[11] = Header.fapp_seq_num[2];
		buf[12] = Header.fapp_seq_num[3];

		for (int i = 0; i < length; i++)
			buf[13 + i] = input[i];
		return buf;
	}
	public void ResetHeader() {
		m_sHeader = new _FAPP_HEADER();
	}
	_FAPP_HEADER m_sHeader = new _FAPP_HEADER();
	
	

	public class Send_Thread implements Runnable{
		byte[] buffer;
		public void setBuf(byte[] input) {
			this.buffer=input;
		}
		@Override
		public void run() {
			Send(buffer,buffer.length);
		}
	}
	
	public boolean SendInf(String inputfilename,byte[] input,int length) {
		fragNum=length/1447;//단편화 조각개수이다.
		rest=length%1447;//1447으로 나눴을 때 남은 수이다.
		this.m_sHeader.fapp_totlen[0]=(byte) (length>>24);//헤더에 전체 길이를 넣는다.
		this.m_sHeader.fapp_totlen[1]=(byte) (length>>16);
		this.m_sHeader.fapp_totlen[2]=(byte) (length>>8);
		this.m_sHeader.fapp_totlen[3]=(byte) (length);
		int i=0;
		for(i=inputfilename.length()-1;inputfilename.charAt(i)!='\\';i--) {}
		filename = inputfilename.substring(i+1);
		byte[] fileName = filename.getBytes();
		byte[] bytes;
		bytes=ObjToByte(m_sHeader, fileName, fileName.length);
		this.GetUnderLayer().Send(bytes, bytes.length);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Send_Thread thread = new Send_Thread();
		thread.setBuf(input);
		Thread th = new Thread(thread);
		th.start();
		return true;
	}
	public boolean Send(byte[] input, int length) {
		this.m_sHeader.fapp_msg_type=1;
		if (length > 1447) {//10보다 클 때 단편화가 필요하다. 
			byte[] bytes;
			this.m_sHeader.fapp_type[1]=0x01;
			for(int i=0;i<fragNum;i++) {
				this.m_sHeader.fapp_seq_num[0]=(byte) ((i>>24)&0xff);
				this.m_sHeader.fapp_seq_num[1]=(byte) ((i>>16)&0xff);
				this.m_sHeader.fapp_seq_num[2]=(byte) ((i>>8)&0xff);
				this.m_sHeader.fapp_seq_num[3]=(byte) (i&0xff);
				byte[] temp = new byte[1447];
				System.arraycopy(input, i * 1447, temp, 0, 1447);
				bytes = ObjToByte(m_sHeader, temp, 1447);//헤더를 붙인다.
				this.ackCount++;//다음 조각을 알수 있도록 1증가한다. 
				this.GetUnderLayer().Send(bytes, 1460);//Ethernet으로 전달한다.
				((StopWaitDlg)this.GetUpperLayer(0)).progressBar.setValue((int)((i+1)*((double)100/fragNum)));
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.m_sHeader.fapp_seq_num[0]=(byte) (fragNum>>24);
			this.m_sHeader.fapp_seq_num[1]=(byte) (fragNum>>16);
			this.m_sHeader.fapp_seq_num[2]=(byte) (fragNum>>8);
			this.m_sHeader.fapp_seq_num[3]=(byte) fragNum;
			byte[] temp=new byte[rest];//남은 길이만큼 배열을 생성하고 복사한다.
			System.arraycopy(input, this.fragNum * 1447, temp, 0,rest);
			this.m_sHeader.fapp_type[1]=0x02;
			bytes = ObjToByte(m_sHeader, temp, rest);//헤더를 붙인다.
			this.GetUnderLayer().Send(bytes, rest+13);
			this.fragNum=0;
			this.rest=0;
			this.ackCount=0;//다 보냈으니 0으로 초기화한다.
			this.ResetHeader();
		} else {//1447이하의 길이는 단편화 없이 전송한다.
			byte[] bytes = ObjToByte(m_sHeader, input, length);
			this.GetUnderLayer().Send(bytes, length+13);
		}
		return true;
	}
	
	public byte[] RemoveCappHeader(byte[] input, int length) {
		byte[] temp = new byte[length-13];
		for(int i =0;i<length-13;i++) {
			temp[i]=input[i+13];
		}
		return temp;
	}

	public int calTotlen() {
		return ((((int)this.m_sHeader.fapp_totlen[0] & 0xff) << 24) |
				(((int)this.m_sHeader.fapp_totlen[1] & 0xff) << 16) |
				(((int)this.m_sHeader.fapp_totlen[2] & 0xff) << 8) |
				(((int)this.m_sHeader.fapp_totlen[3] & 0xff)));
	}

	public synchronized boolean Receive(byte[] input) {
		byte[] data;

		if (input[0] == (byte) 0xff) {// 브로드캐스트일 때 버린다.
			// this.GetUpperLayer(0).Receive(new String("보로드캐스트입니다.").getBytes());
			return true;
		}
		if (input[7] == 0) {//msg_type이 0
			this.m_sHeader.fapp_totlen[0] = input[1];
			this.m_sHeader.fapp_totlen[1] = input[2];
			this.m_sHeader.fapp_totlen[2] = input[3];
			this.m_sHeader.fapp_totlen[3] = input[4];
			int totlen = this.calTotlen();
			this.fragNum = totlen / 1447;// 단편화 조각 길이
			this.rest = totlen % 1447;// 남은 길이
			data = RemoveCappHeader(input, input.length);
	
			buf = new byte[totlen];
			
			this.filename = new String(data);
			this.filename = this.filename.trim();
		} else {
			if (input[6] == 1) {// 단편화 첫 조각일 때
				data = RemoveCappHeader(input, input.length);// 헤더 제거
				int seq_num = ((((int)input[9] & 0xff) << 24) |
						(((int)input[10] & 0xff) << 16) |
						(((int)input[11] & 0xff) << 8) |
						(((int)input[12] & 0xff)));
				System.arraycopy(data, 0, this.buf, seq_num*1447, 1447);
				((StopWaitDlg)this.GetUpperLayer(0)).progressBar.setValue((int)((seq_num+1)*((double)100/fragNum)));
				
			}else if (input[6] == 2) {// 마지막 조각일 때
				data = RemoveCappHeader(input, input.length);// 헤더 제거
				byte[] restSave = new byte[rest];
				for(int i=0;i<rest;i++) {
					restSave[i]=data[i];
				}
			
				int seq_num = ((((int)input[9] & 0xff) << 24) |
						(((int)input[10] & 0xff) << 16) |
						(((int)input[11] & 0xff) << 8) |
						(((int)input[12] & 0xff)));
				System.arraycopy(restSave, 0, this.buf, seq_num*1447, rest);
				((StopWaitDlg)this.GetUpperLayer(0)).progressBar.setValue((int)((seq_num+1)*((double)100/fragNum)));
				
				try {
					Files.write(Paths.get(this.filename),buf);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				this.reCount = 0;// 변수들 초기화
				this.rest = 0;
				this.fragNum = 0;
				this.buf = null;// 버퍼 초기화
				String temp ="Transmission is done!";
				this.GetUpperLayer(0).Receive(temp.getBytes());// 버퍼를 위로 전달
				
			}else {
				data = RemoveCappHeader(input, input.length);
				try {
					FileOutputStream fos = new FileOutputStream(filename);
					try {
						fos.write(data);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.GetUpperLayer(0).Receive();
			}
		}
		// 주소설정
		return true;
	}
	
	public FileAppLayer(String pName) {
		// TODO Auto-generated constructor stub
		pLayerName=pName;
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
		// TODO Auto-generated method stub
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}
}
