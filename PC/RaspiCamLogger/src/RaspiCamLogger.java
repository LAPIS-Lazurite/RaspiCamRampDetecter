import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.time.LocalDateTime;
import javax.swing.*;
import javax.swing.Timer;

import gnu.io.CommPortIdentifier;


public class RaspiCamLogger extends JFrame implements ActionListener{
	/** 
	 * 
	 */
	private JComboBox<String> comPortList;
	private JComboBox<Integer> comBaudList;
	private JComboBox<String> fileSplitBox;
	private JButton FileOpenButton;
	private JButton startButton;
	private JButton stopButton;
	private JTextField fileNameField;
	private JTextField folderNameField;
	private String folderName;
	private String fileName;
	private Timer timer;
	private Boolean bLogging = false;
	private LocalDateTime now;
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		RaspiCamLogger fm;
		System.out.println(System.getProperty("user.dir"));
//		System.out.println(System.getProperty("java.library.path"));
		fm = new RaspiCamLogger();
		
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

	public RaspiCamLogger() {
		CreateWindow();
	}
	private void CreateWindow() {
				
//		updateComList();
		setLayout(null);
		JLabel ComLabel = new JLabel("COM PORT");
		ComLabel.setBounds(10, 10, 100, 20);
		add(ComLabel);

		comPortList = new JComboBox<String>();
//		updateComList();
		comPortList.setPreferredSize(new Dimension(100, 20));
		comPortList.setBounds(120, 10, 100, 20);
		add(comPortList);

		JButton ComButton = new JButton("update");
		ComButton.setBounds(240, 10, 80, 20);
		ComButton.addActionListener(this);
		add(ComButton);

		JLabel BaudLabel = new JLabel("baudrate");
		BaudLabel.setBounds(10, 40, 100, 20);
		add(BaudLabel);

//		comBaudList = new JComboBox<Integer>(baud);
		comBaudList = new JComboBox<Integer>(new Integer[] {9600,115200});
		comBaudList.setPreferredSize(new Dimension(100, 20));
		comBaudList.setBounds(120, 40, 100, 20);
		comBaudList.setSelectedIndex(1);
		add(comBaudList);
		
		JLabel LabelFileSplit = new JLabel("File Split");
		LabelFileSplit.setBounds(10, 70, 100, 20);
		add(LabelFileSplit);
		
		String[] fileSplit = {"min","hours","day"};
		fileSplitBox = new JComboBox<String>(fileSplit);
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
		startButton.setEnabled(false);
		add(startButton);
		
		stopButton = new JButton("Stop");
		stopButton.setBounds(240,190,80,20);
		stopButton.addActionListener(this);
		stopButton.setEnabled(false);
		add(stopButton);
		
		now = LocalDateTime.now();
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
		startButton.setEnabled(false);
		stopButton.setEnabled(true);
		FileOpenButton.setEnabled(false);
		timer = new Timer(1000,this);
		timer.setActionCommand("timer");
		timer.start();
		bLogging = true;
	}
	private void loggingStop(){
		startButton.setEnabled(true);
		stopButton.setEnabled(false);
		FileOpenButton.setEnabled(true);
		timer.stop();
		bLogging = false;
	}
	protected void close() {
		// TODO 自動生成されたメソッド・スタブ
		if(bLogging) loggingStop();
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
				JFileChooser filechooser = new JFileChooser();
				filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			    int selected = filechooser.showSaveDialog(this);
			    if (selected == JFileChooser.APPROVE_OPTION){
			      File file = filechooser.getSelectedFile();
			      folderName = file.getAbsolutePath();
			      folderNameField.setText(folderName);
			      startButton.setEnabled(true);
			    }
			} else if (cmd == "Start"){
				System.out.println("Start");
				loggingStart();
			} else if (cmd == "Stop"){
				System.out.println("Stop");
				loggingStop();
			} else if(cmd == "timer") {
				now = LocalDateTime.now();
				
				if (fileSplitBox.getSelectedItem() == "hours") {
					fileName = String.format("%04d%02d%2d%02d",
							now.getYear(),now.getMonthValue(),now.getDayOfMonth(),
							now.getHour());
				} else if (fileSplitBox.getSelectedItem() == "date") {
					fileName = String.format("%04d%02d%2d",
							now.getYear(),now.getMonthValue(),now.getDayOfMonth());
				} else if (fileSplitBox.getSelectedItem() == "min") {
					fileName = String.format("%04d%02d%02d%02d%02d",
							now.getYear(),now.getMonthValue(),now.getDayOfMonth(),
							now.getHour(),now.getMinute());
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
							now.getYear(),
							now.getMonthValue(),
							now.getDayOfMonth(),
							now.getHour(),
							now.getMinute(),
							now.getSecond()));
					bw.newLine();
					bw.close();
				} catch (IOException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
				}
			}
	}

}
