import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class LazuriteLogger extends JFrame implements ActionListener,SerialPortEventListener{
	/**
	 *
	 */
	private JComboBox comPortList;
	private JComboBox comBaudList;
	private JComboBox fileSplitBox;
	JButton comButton;
	private JButton FileOpenButton;
	private JButton startButton;
	private JButton stopButton;
	private JTextField fileNameField;
	private JTextField folderNameField;
	private String folderName;
	private String fileName;
	private Boolean bLogging = false;
	private Calendar now;
	private String comPortName;
	private int comBaudrate;
	private String fileSplitUnit;
	private SerialPort serialPort; // serial port object

	private String inputData;
	private BufferedReader input; // input reader

	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		final LazuriteLogger fm;
//		System.out.println(System.getProperty("user.dir"));
//		System.out.println(System.getProperty("java.library.path"));
		fm = new LazuriteLogger();

		fm.setBounds(10,20,400,300);
		fm.setVisible(true);
		fm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// graph.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		// shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {

		public void run() {
				System.out.println("***** end of program *****");
				fm.close();
			}
		});
	}

	public LazuriteLogger() {
		CreateWindow();
	}
	private void CreateWindow() {

		setResizable(false);
		setLayout(null);
		JLabel ComLabel = new JLabel("COM PORT");
		ComLabel.setBounds(10, 10, 100, 20);
		add(ComLabel);

		comPortList = new JComboBox();
//		updateComList();
		comPortList.setPreferredSize(new Dimension(100, 20));
		comPortList.setBounds(120, 10, 100, 20);
		add(comPortList);

		comButton = new JButton("update");
		comButton.setBounds(240, 10, 80, 20);
		comButton.addActionListener(this);
		add(comButton);

		JLabel BaudLabel = new JLabel("baudrate");
		BaudLabel.setBounds(10, 40, 100, 20);
		add(BaudLabel);

		comBaudList = new JComboBox(new Integer[] {9600,115200});
		comBaudList.setPreferredSize(new Dimension(100, 20));
		comBaudList.setBounds(120, 40, 100, 20);
		comBaudList.setSelectedIndex(1);
		add(comBaudList);

		JLabel LabelFileSplit = new JLabel("File Split");
		LabelFileSplit.setBounds(10, 70, 100, 20);
		add(LabelFileSplit);

		String[] fileSplit = {"min","hours","day"};
		fileSplitBox = new JComboBox(fileSplit);
		fileSplitBox.setBounds(120, 70, 100, 20);
		add(fileSplitBox);

		JLabel LabelFileOpen = new JLabel("Select Folder");
		LabelFileOpen.setBounds(10, 100, 100, 20);
		add(LabelFileOpen);

		FileOpenButton = new JButton("Select Folder");
		FileOpenButton.setBounds(120, 100, 100, 20);
		FileOpenButton.addActionListener(this);
		add(FileOpenButton);

		JLabel labelFolderName = new JLabel("Folder");
		labelFolderName.setBounds(10, 130, 100, 20);
		add(labelFolderName);

		folderNameField = new JTextField("");
		folderNameField.setBounds(120, 130, 240, 20);
		folderNameField.setEnabled(false);
		add(folderNameField);

		JLabel labelFileName = new JLabel("File");
		labelFileName.setBounds(10, 160, 100, 20);
		add(labelFileName);

		fileNameField = new JTextField("");
		fileNameField.setBounds(120, 160, 240, 20);
		fileNameField.setEnabled(false);
		add(fileNameField);

		JLabel labelStart = new JLabel("Start");
		labelStart.setBounds(10, 190, 100, 20);
		add(labelStart);

		startButton= new JButton("Start");
		startButton.setBounds(120, 190, 80, 20);
		startButton.addActionListener(this);
		add(startButton);

		stopButton = new JButton("Stop");
		stopButton.setBounds(240,190,80,20);
		stopButton.addActionListener(this);
		stopButton.setEnabled(false);
		add(stopButton);

		updateComList();
		now = Calendar.getInstance();
		getInitalParameters();

		File directory = new File(folderNameField.getText());
		if(directory.isDirectory()) startButton.setEnabled(true);
		else startButton.setEnabled(false);

	}

	private void getInitalParameters() {
		String appDir = System.getProperty("user.dir") +"/"+ "logger.ini";
		Properties properties = new Properties();
        try {
            InputStream inputStream = new FileInputStream(appDir);
            properties.load(inputStream);
            inputStream.close();
            // 値の取得
            comPortList.setSelectedItem(properties.getProperty("comPort"));
            comBaudList.setSelectedItem(Integer.valueOf(properties.getProperty("comBaudrate")));
            fileSplitBox.setSelectedItem(properties.getProperty("fileSplit"));
            folderNameField.setText(properties.getProperty("filePath"));
            System.out.println(properties.getProperty("comBaudrate"));
            System.out.println(properties.getProperty("fileSplit"));
            System.out.println(properties.getProperty("filePath"));
        } catch (Exception ex) {
            System.out.println(ex.getMessage());

        }

		return;
	}

	private void storeInitalParameters() {
		String appDir = System.getProperty("user.dir") +"/"+ "logger.ini";
		Properties properties = new Properties();
		properties.setProperty("comPort",(String)comPortList.getSelectedItem() );
		properties.setProperty("comBaudrate", String.valueOf(comBaudList.getSelectedItem()));
		properties.setProperty("fileSplit", (String)fileSplitBox.getSelectedItem());
		properties.setProperty("filePath",folderNameField.getText() );
       try {
    	   properties.store(new FileOutputStream(appDir),"Comments");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());

        }

		return;
	}

	private void updateComList() {
		// CommPortIdentifier portId = null;
		Enumeration<?> portEnum;
		portEnum = CommPortIdentifier.getPortIdentifiers();
		// ComBox.addItem("COM1");

		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currentPortIdentifier = (CommPortIdentifier) portEnum.nextElement();
			comPortList.addItem(currentPortIdentifier.getName());
		}
	}

	private void loggingStart(){

		// get parameters
		comPortName = (String)comPortList.getSelectedItem();
		comBaudrate = (Integer)comBaudList.getSelectedItem();
		fileSplitUnit = (String)fileSplitBox.getSelectedItem();
		folderName = folderNameField.getText();

		// change button status;
		comButton.setEnabled(false);
		startButton.setEnabled(false);
		stopButton.setEnabled(true);
		FileOpenButton.setEnabled(false);
		comPortList.setEnabled(false);
		comBaudList.setEnabled(false);
		fileSplitBox.setEnabled(false);
		if(initializeSerial())
		{
			bLogging = true;
		} else {
			loggingStop();
		}
	}
	private void loggingStop(){
		// change button status
		startButton.setEnabled(true);
		comButton.setEnabled(true);
		stopButton.setEnabled(false);
		FileOpenButton.setEnabled(true);
		comPortList.setEnabled(true);
		comBaudList.setEnabled(true);;
		fileSplitBox.setEnabled(true);;

		if(bLogging) {
			closeSerial();
			storeInitalParameters();
		}
		bLogging = false;
	}
	protected void close() {
		// TODO 自動生成されたメソッド・スタブ
		if(bLogging) {
			closeSerial();
			storeInitalParameters();
		}
	}

	private Boolean initializeSerial() {
		Boolean succeeded = true;
		CommPortIdentifier portId = null;
		Enumeration<?> portEnum = CommPortIdentifier.getPortIdentifiers();

		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currentPortIdentifier = (CommPortIdentifier) portEnum.nextElement();
			if (currentPortIdentifier.getName().equals(comPortName)) {
				portId = currentPortIdentifier;
				break;
			}
		}

		if (portId == null) {
		      JOptionPane.showMessageDialog(this, "Port not found", "Error",
		    		  JOptionPane.ERROR_MESSAGE);
			return false;
		}

		try {

//			serialPort = (SerialPort) portId.open(this.getClass().getName(), 2000);
			serialPort = (SerialPort) portId.open(comPortName, 2000);
			serialPort.setSerialPortParams(comBaudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			serialPort.disableReceiveTimeout();
			serialPort.enableReceiveThreshold(1);

			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			// output = serialPort.getOutputStream();

			serialPort.addEventListener(this );
			serialPort.notifyOnDataAvailable(true);
			succeeded = true;
		} catch (Exception e) {
			System.err.println("Initialization failed : " + e.toString());
			succeeded = false;
		}
		inputData = new String("");

		return succeeded;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
			// TODO update com port list
			String cmd = e.getActionCommand();
			if (cmd == "update"){
				// System.out.println("update");
				comPortList.removeAllItems();
				updateComList();
				// add(comPortList);
			} else if (cmd == "Select Folder"){
				JFileChooser filechooser = new JFileChooser(folderNameField.getText());
				filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			    int selected = filechooser.showSaveDialog(this);
			    if (selected == JFileChooser.APPROVE_OPTION){
			      File file = filechooser.getSelectedFile();
			      folderNameField.setText(file.getAbsolutePath());
			      startButton.setEnabled(true);
			    }
			} else if (cmd == "Start"){
				System.out.println("Start");
				loggingStart();
			} else if (cmd == "Stop"){
				System.out.println("Stop");
				loggingStop();
			} else if(cmd == "timer") {
				now = Calendar.getInstance();

				if (fileSplitUnit == "hours") {
					fileName = String.format("%04d%02d%2d%02d",
							now.get(Calendar.YEAR),
							now.get(Calendar.MONTH),
							now.get(Calendar.DAY_OF_MONTH),
							now.get(Calendar.HOUR_OF_DAY)
							);

				} else if (fileSplitUnit == "date") {
					fileName = String.format("%04d%02d%2d",
							now.get(Calendar.YEAR),
							now.get(Calendar.MONTH),
							now.get(Calendar.DAY_OF_MONTH)
							);
				} else if (fileSplitUnit == "min") {
					fileName = String.format("%04d%02d%02d%02d%02d",
							now.get(Calendar.YEAR),
							now.get(Calendar.MONTH),
							now.get(Calendar.DAY_OF_MONTH),
							now.get(Calendar.HOUR_OF_DAY),
							now.get(Calendar.MINUTE)
							);
				}
				fileNameField.setText(fileName);

				String outputLogFile = folderName +"/" + fileName+".csv";
				System.out.println(outputLogFile);
				File file = new File(outputLogFile);
				FileWriter filewriter;

				try {
					filewriter = new FileWriter(file,true);
					BufferedWriter bw = new BufferedWriter(filewriter);
					bw.write(String.format("%04d%02d%02d,%02d%02d%02d",
							now.get(Calendar.YEAR),
							now.get(Calendar.MONTH),
							now.get(Calendar.DAY_OF_MONTH),
							now.get(Calendar.HOUR_OF_DAY),
							now.get(Calendar.MINUTE),
							now.get(Calendar.SECOND)
							));
					bw.newLine();
					bw.close();
				} catch (IOException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
				}
			}
	}

	public synchronized void closeSerial() {
		if (serialPort != null) {
			System.out.println("closeSerial");
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
	// TODO 自動生成されたメソッド・スタブ
		Boolean writeLine=false;
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				while(true) {
					char[] buf = new char[1];
					if(input.read(buf) != -1) {
						if(buf[0] != 13) {
							inputData += String.valueOf(buf);
						}
						if(buf[0] == 10) {
							writeLine = true;
							break;
						}
					}
				}
			} catch(Exception e) {
//				System.out.println("Serial port error::" + e);
//				System.out.println(inputData);
				return;
			}
			if(writeLine) {
				now = Calendar.getInstance();
				if (fileSplitUnit == "hours") {
					fileName = String.format("%04d%02d%2d%02d",
							now.get(Calendar.YEAR),
							now.get(Calendar.MONTH)+1,
							now.get(Calendar.DAY_OF_MONTH),
							now.get(Calendar.HOUR_OF_DAY)
							);
				} else if (fileSplitUnit == "date") {
					fileName = String.format("%04d%02d%2d",
							now.get(Calendar.YEAR),
							now.get(Calendar.MONTH)+1,
							now.get(Calendar.DAY_OF_MONTH)
							);
				} else {
					fileName = String.format("%04d%02d%02d%02d%02d",
							now.get(Calendar.YEAR),
							now.get(Calendar.MONTH)+1,
							now.get(Calendar.DAY_OF_MONTH),
							now.get(Calendar.HOUR_OF_DAY),
							now.get(Calendar.MINUTE)
							);
				}
				fileNameField.setText(fileName);

				String outputLogFile = folderName +"\\" + fileName+".csv";
				System.out.println(outputLogFile);
				System.out.print(inputData);
				System.out.println(String.format("%04d/%02d/%02d %02d:%02d:%02d",
						now.get(Calendar.YEAR),
						now.get(Calendar.MONTH)+1,
						now.get(Calendar.DAY_OF_MONTH),
						now.get(Calendar.HOUR_OF_DAY),
						now.get(Calendar.MINUTE),
						now.get(Calendar.SECOND)));

				File file = new File(outputLogFile);
				try {
					FileWriter filewriter = new FileWriter(file,true);
					filewriter.write(String.format("%04d/%02d/%02d %02d:%02d:%02d",
							now.get(Calendar.YEAR),
							now.get(Calendar.MONTH)+1,
							now.get(Calendar.DAY_OF_MONTH),
							now.get(Calendar.HOUR_OF_DAY),
							now.get(Calendar.MINUTE),
							now.get(Calendar.SECOND))+","+inputData);
//					filewriter.write("\r\n");
					filewriter.close();
				} catch (Exception e) {
					System.out.println("File output error::" + e);
				}
				inputData = "";
			}
		}
	}
}
	/*	public void serialEvent(SerialPortEvent event) {
		// TODO 自動生成されたメソッド・スタブ
		String inputData = null;
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				inputData = input.readLine();
				if(inputData == null) return;
			} catch(Exception e) {
				System.out.println("Serial port error::" + e);
				return;
			}
			now = Calendar.getInstance();
			if (fileSplitUnit == "hours") {
				fileName = String.format("%04d%02d%2d%02d",
						now.get(Calendar.YEAR),
						now.get(Calendar.MONTH)+1,
						now.get(Calendar.DAY_OF_MONTH),
						now.get(Calendar.HOUR_OF_DAY)
						);
					} else if (fileSplitUnit == "date") {
				fileName = String.format("%04d%02d%2d",
						now.get(Calendar.YEAR),
						now.get(Calendar.MONTH)+1,
						now.get(Calendar.DAY_OF_MONTH)
						);
			} else if (fileSplitUnit == "min") {
				fileName = String.format("%04d%02d%02d%02d%02d",
						now.get(Calendar.YEAR),
						now.get(Calendar.MONTH)+1,
						now.get(Calendar.DAY_OF_MONTH),
						now.get(Calendar.HOUR_OF_DAY),
						now.get(Calendar.MINUTE)
						);
			}
			fileNameField.setText(fileName);

			String outputLogFile = folderName +"\\" + fileName+".csv";
			System.out.println(outputLogFile);
			System.out.println(inputData);
			System.out.println(String.format("%04d/%02d/%02d %02d:%02d:%02d",
					now.get(Calendar.YEAR),
					now.get(Calendar.MONTH)+1,
					now.get(Calendar.DAY_OF_MONTH),
					now.get(Calendar.HOUR_OF_DAY),
					now.get(Calendar.MINUTE),
					now.get(Calendar.SECOND)));

			File file = new File(outputLogFile);
			try {
				FileWriter filewriter = new FileWriter(file,true);
				filewriter.write(String.format("%04d/%02d/%02d %02d:%02d:%02d",
						now.get(Calendar.YEAR),
						now.get(Calendar.MONTH)+1,
						now.get(Calendar.DAY_OF_MONTH),
						now.get(Calendar.HOUR_OF_DAY),
						now.get(Calendar.MINUTE),
						now.get(Calendar.SECOND))+","+inputData);
				filewriter.write("\r\n");
				filewriter.close();
			} catch (Exception e) {
				System.out.println("File output error::" + e);
		}
			}
//			} catch (IOException e) {
//				// TODO 自動生成された catch ブロック
//				e.printStackTrace();
//			}
//		}
	}
	*/

