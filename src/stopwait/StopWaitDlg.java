package stopwait;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import stopwait.StopWaitDlg.setAddressListener;
import java.awt.FlowLayout;
import javax.swing.JProgressBar;

public class StopWaitDlg extends JFrame implements BaseLayer {

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	BaseLayer UnderLayer;
	KeyEvent key;
	private static LayerManager m_LayerMgr = new LayerManager();

	private JTextField ChattingWrite;
	Container contentPane;

	JTextArea ChattingArea;
	JTextArea srcAddress;
	JTextArea dstAddress;
	

	JLabel lblsrc;
	JLabel lbldst;
	JLabel nic_setting;
	JButton Setting_Button;
	JButton Chat_send_Button;
	JButton file_select_Button;
	JButton file_transmit_Button;
	byte[] data;
	int length;
	static JComboBox<String> NICComboBox;

	int adapterNumber = 0;

	String Text;
	
	private JTextField fileName;
	
	JProgressBar progressBar;

	File selectedFile=null;
	String filePath;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		m_LayerMgr.AddLayer(new NILayer("NI"));
		m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
		m_LayerMgr.AddLayer(new ChatAppLayer("Chat"));
		m_LayerMgr.AddLayer(new StopWaitDlg("GUI"));
		m_LayerMgr.AddLayer(new FileAppLayer("File"));

		/*
		 * 과제 ChatApp 연결하기 아래부분 수정
		 * 
		 */
		m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( *Chat ( *GUI ) *File ( *GUI ) ) ) ");
	
		
	}

	/**
	 * Create the frame.
	 */
	public StopWaitDlg(String pName) {
		pLayerName = pName;
		setTitle("Chat_File_Transfer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(250, 250, 644, 425);
		contentPane = new JPanel();
		((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JPanel filePanel = new JPanel();
		filePanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "파일전송",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		filePanel.setBounds(10, 291, 360, 85);
		contentPane.add(filePanel);
		filePanel.setLayout(null);
		
		JPanel fileNamePanel = new JPanel();
		fileNamePanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		fileNamePanel.setBounds(14, 26, 250, 20);
		filePanel.add(fileNamePanel);
		fileNamePanel.setLayout(null);
		
		fileName = new JTextField();
		fileName.setBounds(2,2,250,20);
		fileNamePanel.add(fileName);
		fileName.setColumns(10);
		
		file_select_Button = new JButton("파일...");
		file_select_Button.setBounds(270, 26, 80, 20);
		filePanel.add(file_select_Button);
		file_select_Button.addActionListener(new setAddressListener());
		
		file_transmit_Button = new JButton("전송");
		file_transmit_Button.setBounds(270, 53, 80, 20);
		filePanel.add(file_transmit_Button);
		file_transmit_Button.addActionListener(new setAddressListener());
		file_transmit_Button.setEnabled(false);
		
		progressBar = new JProgressBar();
		progressBar.setBounds(14, 53, 250, 20);
		progressBar.setStringPainted(true);
		filePanel.add(progressBar);

		JPanel chattingPanel = new JPanel();// chatting panel
		chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "채팅",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		chattingPanel.setBounds(10, 5, 360, 276);
		contentPane.add(chattingPanel);
		chattingPanel.setLayout(null);

		JPanel chattingEditorPanel = new JPanel();// chatting write panel
		chattingEditorPanel.setBounds(10, 15, 340, 210);
		chattingPanel.add(chattingEditorPanel);
		chattingEditorPanel.setLayout(null);

		ChattingArea = new JTextArea();
		ChattingArea.setEditable(false);
		ChattingArea.setBounds(0, 0, 340, 210);
		chattingEditorPanel.add(ChattingArea);// chatting edit

		JPanel chattingInputPanel = new JPanel();// chatting write panel
		chattingInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		chattingInputPanel.setBounds(10, 230, 250, 20);
		chattingPanel.add(chattingInputPanel);
		chattingInputPanel.setLayout(null);

		ChattingWrite = new JTextField();
		ChattingWrite.setBounds(2, 2, 250, 20);// 249
		chattingInputPanel.add(ChattingWrite);
		ChattingWrite.setColumns(10);// writing area

		JPanel settingPanel = new JPanel();
		settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "설정",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		settingPanel.setBounds(380, 5, 236, 371);
		contentPane.add(settingPanel);
		settingPanel.setLayout(null);

		JPanel sourceAddressPanel = new JPanel();
		sourceAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		sourceAddressPanel.setBounds(10, 96, 170, 20);
		settingPanel.add(sourceAddressPanel);
		sourceAddressPanel.setLayout(null);

		lblsrc = new JLabel("시작 주소");
		lblsrc.setBounds(10, 70, 170, 20);
		settingPanel.add(lblsrc);

		srcAddress = new JTextArea();
		srcAddress.setBounds(2, 2, 170, 20);
		sourceAddressPanel.add(srcAddress);// src address

		JPanel destinationAddressPanel = new JPanel();
		destinationAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		destinationAddressPanel.setBounds(10, 212, 170, 20);
		settingPanel.add(destinationAddressPanel);
		destinationAddressPanel.setLayout(null);

		lbldst = new JLabel("목적지 주소");
		lbldst.setBounds(10, 187, 190, 20);
		settingPanel.add(lbldst);

		dstAddress = new JTextArea();
		dstAddress.setBounds(2, 2, 170, 20);
		destinationAddressPanel.add(dstAddress);// dst address

		Setting_Button = new JButton("설정");// setting
		Setting_Button.setBounds(50, 271, 100, 20);
		Setting_Button.addActionListener(new setAddressListener());
		settingPanel.add(Setting_Button);// setting

		nic_setting = new JLabel("NIC 선택");
		nic_setting.setBounds(10, 22, 170, 18);
		settingPanel.add(nic_setting);

		NICComboBox = new JComboBox<String>();
		int i = ((NILayer) m_LayerMgr.GetLayer("NI")).get_m_List().size();
		int j = 0;
		while (j < i) {
			NICComboBox.addItem(((NILayer) m_LayerMgr.GetLayer("NI")).get_description(j));
			j++;
		}
		NICComboBox.addActionListener(new setAddressListener());
		NICComboBox.setBounds(10, 42, 170, 24);
		settingPanel.add(NICComboBox);

		Chat_send_Button = new JButton("Send");
		Chat_send_Button.setBounds(270, 230, 80, 20);
		Chat_send_Button.addActionListener(new setAddressListener());
		chattingPanel.add(Chat_send_Button);// chatting send button
		

		setVisible(true);
	}

	class setAddressListener implements ActionListener {
		public byte hexStringToByteArray(String s) {
		    int len = 2;
		    byte data = 0;
		    for (int i = 0; i < len; i += 2) {
		        data = (byte) ((Character.digit(s.charAt(i), 16) << 4)
		                             + Character.digit(s.charAt(i+1), 16));
		    }
		    return data;
		}
		@Override
		public void actionPerformed(ActionEvent e) {

			int index = NICComboBox.getSelectedIndex();

			srcAddress.setText((((NILayer) m_LayerMgr.GetLayer("NI")).getDstAddressToString(index)));
			if (e.getSource() == Setting_Button) {
				if (Setting_Button.getText() == "Reset") {
					dstAddress.setEnabled(true);
					srcAddress.setEnabled(true);
					NICComboBox.setEnabled(true);
					srcAddress.setText("");
					dstAddress.setText("");
					Setting_Button.setText("Setting");
				} else {
					((NILayer) m_LayerMgr.GetLayer("NI")).SetAdapterNumber(index);
					String dst = dstAddress.getText();
					String[] Sdst = new String[6];
					Sdst = dst.split("-");
					byte[] address = new byte[6];
					for (int i = 0; i < 6; i++) {
						address[i] = this.hexStringToByteArray(Sdst[i]);
					}
					
					((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).setAddress(address);

					((NILayer)m_LayerMgr.GetLayer("NI")).Receive();
					progressBar.setMinimum(0);
					progressBar.setMaximum(100);
					Setting_Button.setText("Reset");
					dstAddress.setEnabled(false);
					srcAddress.setEnabled(false);
					NICComboBox.setEnabled(false);
				}
			}
			if (e.getSource() == Chat_send_Button) {
				if (Setting_Button.getText() == "Reset") {
					String message = ChattingWrite.getText();
					ChattingArea.append("[SEND] " + message + "\n");
					((ChatAppLayer) m_LayerMgr.GetLayer("Chat")).setData(message.getBytes());
					((ChatAppLayer) m_LayerMgr.GetLayer("Chat")).Send(message.getBytes(), message.getBytes().length);
					ChattingWrite.setText("");
				} else {
					JOptionPane.showMessageDialog(ChattingArea, "주소설정오류");
				}
			}
			if(e.getSource() == file_select_Button) {
				JFileChooser chooser = new JFileChooser();
				int ret = chooser.showOpenDialog(null);
				if(ret != JFileChooser.APPROVE_OPTION) {
					JOptionPane.showMessageDialog(null, "파일을 선택하지 않았습니다","경고",JOptionPane.WARNING_MESSAGE);
					return;
				}
				filePath = chooser.getSelectedFile().getPath();
				selectedFile = chooser.getSelectedFile();
				fileName.setText(filePath);

				file_transmit_Button.setEnabled(true);
				((NILayer) m_LayerMgr.GetLayer("NI")).Receive();
			}
			if (e.getSource() == file_transmit_Button) {
				if (selectedFile != null) {
					length = (int) selectedFile.length();
				
					try {
						((FileAppLayer) m_LayerMgr.GetLayer("File")).SendInf(filePath,Files.readAllBytes(Paths.get(filePath)), length);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				}
			}
		}
	}

	public boolean Receive(byte[] input) {
		ChattingArea.append("[RECV] " + new String(input) + "\n");
		return true;
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
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);

	}
}
