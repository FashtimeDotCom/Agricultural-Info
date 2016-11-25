package com.comm.ui;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TooManyListenersException;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.wb.swt.SWTResourceManager;
import org.jfree.chart.JFreeChart;
import org.jfree.experimental.chart.swt.ChartComposite;

import com.comm.dao.DBHelper;
import com.comm.utils.FormatUtil;
import com.comm.utils.PropertiesUtil;

public class MainActivity {
	protected Shell shell;
	private Display display;

	public static DBHelper dbHelper;
	public static ArrayList<String> deviceList;	//�洢��ʶ��Ľڵ�
	private CommPortIdentifier portId; // ͨ�Ŷ˿ڱ�ʶ��
	private SerialPort serialPort; // ���ж˿�
	private OutputStream outputStream; // ��˿ڷ�������
	private InputStream inputStream; // ��ȡ�˿�����
	private int sendTerm = 0;	//�Զ���������ʱ����
	private int baudRates = 9600; // �����ʳ�ʼ��ֵ
	private int dataBits = 8; // ����λ��ʼ��ֵ
	private int stopBits = 1; // ֹͣλ��ʼ��ֵ
	private int parityBits = SerialPort.PARITY_NONE; // У��λ��ʼ��ֵ
	private int rxCount = 0; // ���յ��ֽ���
	private int txCount = 0; // ���͵��ֽ���
	private int rxCountForClear = 0; // ͳ�ƽ��յ��ַ�������ַ���������Զ����
	private byte[] commTxData = null;
	private ArrayList<String> commList; // ���ڵĶ˿�
	private boolean commBeOpened = false; // �����Ƿ񱻴�
	private boolean autoSendState = false;	//�Ƿ��Զ�����
	private boolean selectState = false;	//��̬ͼ���Ƿ���
	private Timer selectTimer ;
	private Thread autoSendThread; // �Զ������߳�
	private String sendFilePath; // �����͵��ļ�
    
	private Combo commChoice; // �˿�ѡ��
	private Combo baudChoice; // ������ѡ��
	private Combo parityChoice; // У��λѡ��
	private Combo dataChoice; // ����λѡ��
	private Combo stopChoice; // ֹͣλѡ��
	private Text commRxTxt; // ��ʾ���յ�����
	private Button openCommButton; // �򿪴���
	private Button autoEmpty; // �Զ����
	private Button hexDisplay; // HEX��ʾ
	private Button singleLineDisplay; // �Զ�����
	private Button hexSend; // HEX����
	private Text commTxTxt; // �������ݴ���
	private Button autoSend;// �Զ�����
	private Label stateTxt; // �������״̬��
	private Text sendTermTxt; // �������ݼ��
	private Label rxCountLabel; // �����ַ�����
	private Label txCountLabel; // �����ַ�����
	private Label filePathLable;// ��ʾ�ļ�·��

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MainActivity window = new MainActivity();
			window.initDate();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private void initDate() {
		dbHelper = new DBHelper(); // �������ݿ�
		if (deviceList == null) {
			deviceList = new ArrayList<String>();
			int deviceNum = Integer.valueOf(PropertiesUtil.getKeyValue(
					"deviceNum", "0"));
			for (int index = 0; index < deviceNum; index++) {
				deviceList.add(PropertiesUtil.getKeyValue("deviceID" + index,
						""));
			}
		}
	}

	
	/**
	 * Create contents of the window.
	 */
	public void createContents() {
		shell = new Shell();
		shell.setSize(1024, 768);
		shell.setText("ũҵ��Ϣʵʱ���");
		shellCentered(); // ���ھ���
		shell.setLayout(new FormLayout()); // ������
		shell.addShellListener(new ShellAdapter() {
			// ��������
			@Override
			public void shellClosed(ShellEvent e) {
				super.shellClosed(e);
				closeSerialPort();	//��ȫ�رմ���
				if (selectTimer != null) {
					selectTimer.cancel();
				}
			}
		});

		// ����������ʾ��(�Ѿ����ش˿ؼ�)
		commRxTxt = new Text(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.CANCEL | SWT.MULTI);
		commRxTxt.setFont(SWTResourceManager.getFont("Microsoft YaHei UI", 12,
				SWT.NORMAL));
		commRxTxt.setEditable(false);
		FormData content_fd = new FormData();
		content_fd.top = new FormAttachment(0);
		content_fd.bottom = new FormAttachment(0); // ��ռshell(����)���0%
		content_fd.left = new FormAttachment(0);
		content_fd.right = new FormAttachment(0); // ��ռshell(����)�ߵ�0%
		commRxTxt.setLayoutData(content_fd);

		//��̬����ͼ��
		Composite right_top_cps = new Composite(shell, SWT.None);
		right_top_cps.setLayout(new FillLayout(SWT.HORIZONTAL));
		FormData right_top_fd = new FormData();
		right_top_fd.top = new FormAttachment(0);
		right_top_fd.bottom = new FormAttachment(60); // ��ռshell(����)���60%
		right_top_fd.left = new FormAttachment(40);
		right_top_fd.right = new FormAttachment(100); // ��ռshell(����)�ߵ�60%
		right_top_cps.setLayoutData(right_top_fd);
		
		JFreeChart chart = ArgInfoChart.createChart(ArgInfoChart.dataset);	//����ͼ��
		ChartComposite frame = new ChartComposite(right_top_cps, SWT.NONE, chart, true);
        frame.setDisplayToolTips(true);
        frame.setHorizontalAxisTrace(false);
        frame.setVerticalAxisTrace(false);
		
		
		// ���ϴ��ڹ���ѡ��,left_top_cps�����ϰ����left_top_cps_above���°����left_top_cps_below
		Composite left_top_cps = new Composite(shell, SWT.None);
		left_top_cps.setLayout(new FormLayout());
		FormData left_top_fd = new FormData();
		left_top_fd.top = new FormAttachment(0);
		left_top_fd.bottom = new FormAttachment(60); // ��ռshell(����)�ߵ�60%
		left_top_fd.left = new FormAttachment(0);
		left_top_fd.right = new FormAttachment(40); // ��ռshell(����)���40%
		left_top_cps.setLayoutData(left_top_fd);
		{
			Composite left_top_cps_above = new Composite(left_top_cps, SWT.NONE);
			left_top_cps_above.setLayout(new org.eclipse.swt.layout.GridLayout(
					2, true)); // 2�зֲ�
			FormData left_top_fd_above = new FormData();
			left_top_fd_above.top = new FormAttachment(0);
			left_top_fd_above.bottom = new FormAttachment(60); // ��ռleft_top_cps���ߵ�60%
			left_top_fd_above.left = new FormAttachment(0);
			left_top_fd_above.right = new FormAttachment(100); // ��ռleft_top_cps�����100%
			left_top_cps_above.setLayoutData(left_top_fd_above);
			{
				Label commLb = new Label(left_top_cps_above, SWT.NONE);
				commLb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
						true, 1, 1));
				commLb.setText("���ں�");

				commChoice = new Combo(left_top_cps_above, SWT.NONE);
				commChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
						true, true, 1, 1));
				commList = new ArrayList<String>();
				// ���ڳ�ʼ��
				Enumeration<?> en = CommPortIdentifier.getPortIdentifiers();
				// commChoice��ӿ���ѡ��Ķ˿�
				while (en.hasMoreElements()) {
					portId = (CommPortIdentifier) en.nextElement();
					if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
						String commName = portId.getName().toUpperCase();
						commChoice.add(commName);
						commList.add(commName);
					}
				}
				commChoice.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseDown(MouseEvent e) {
						super.mouseDown(e);
						Enumeration<?> en = CommPortIdentifier
								.getPortIdentifiers();
						while (en.hasMoreElements()) {
							portId = (CommPortIdentifier) en.nextElement();
							if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
								String commName = portId.getName()
										.toUpperCase();
								if (!commList.contains(commName)) {
									commList.add(commName);
									commChoice.add(commName);
								}
							}
						}
					}

				});

				Label baudLb = new Label(left_top_cps_above, SWT.NONE);
				baudLb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
						true, 1, 1));
				baudLb.setText("������");

				baudChoice = new Combo(left_top_cps_above, SWT.NONE);
				baudChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
						true, true, 1, 1));
				baudChoice.add("300"); // baudChoice��ӿ���ѡ��Ĳ�����
				baudChoice.add("600");
				baudChoice.add("1200");
				baudChoice.add("2400");
				baudChoice.add("4800");
				baudChoice.add("9600");
				baudChoice.add("19200");
				baudChoice.add("38400");
				baudChoice.add("43000");
				baudChoice.add("56000");
				baudChoice.add("57600");
				baudChoice.add("115200");
				baudChoice.select(5); // Ĭ��ѡ���5��

				Label parityLb = new Label(left_top_cps_above, SWT.NONE);
				parityLb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,
						true, true, 1, 1));
				parityLb.setText("У��λ");

				parityChoice = new Combo(left_top_cps_above, SWT.NONE);
				parityChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
						true, true, 1, 1));
				parityChoice.add("��NONE"); // parityChoice��ӿ���ѡ���У��ֵ
				parityChoice.add("��ODD");
				parityChoice.add("żEVEN");
				parityChoice.select(0);

				Label dataLb = new Label(left_top_cps_above, SWT.NONE);
				dataLb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
						true, 1, 1));
				dataLb.setText("����λ");

				dataChoice = new Combo(left_top_cps_above, SWT.NONE);
				dataChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
						true, true, 1, 1));
				dataChoice.add("8"); // dataChoice��ӿ���ѡ�������λ
				dataChoice.add("7");
				dataChoice.add("6");
				dataChoice.select(0);

				Label stopLb = new Label(left_top_cps_above, SWT.NONE);
				stopLb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
						true, 1, 1));
				stopLb.setText("ֹͣλ");

				stopChoice = new Combo(left_top_cps_above, SWT.NONE);
				stopChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
						true, true, 1, 1));
				stopChoice.add("1"); // stopChoice��ӿ���ѡ���ֹͣλ
				stopChoice.add("2");
				stopChoice.select(0);
			}

			// ���������°벿��
			Composite left_top_cps_below = new Composite(left_top_cps, SWT.None);
			left_top_cps_below.setLayout(new org.eclipse.swt.layout.GridLayout(
					2, true));
			FormData left_top_fd_2 = new FormData();
			left_top_fd_2.top = new FormAttachment(60);
			left_top_fd_2.right = new FormAttachment(100); // ��ռleft_top_Cps�ߵ�40%
			left_top_fd_2.left = new FormAttachment(0);
			left_top_fd_2.bottom = new FormAttachment(100); // ��ռleft_top_Cps���100%
			left_top_cps_below.setLayoutData(left_top_fd_2);
			{
				openCommButton = new Button(left_top_cps_below, SWT.NONE);
				openCommButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
						true, true, 1, 1));
				openCommButton.setText("�򿪴���");
				openCommButton.addSelectionListener(new OpenCommListenter());
				
				final Button startButton = new Button(left_top_cps_below, SWT.NONE);
				startButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if(!selectState) {
							startSelectTimer();
							startButton.setText("��ͣ");
						}else {
							selectTimer.cancel();
							startButton.setText("����");
						}
						selectState = !selectState;
					}
				});
				startButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
						true, true, 1, 1));
				startButton.setText("����");

				autoEmpty = new Button(left_top_cps_below, SWT.CHECK);
				autoEmpty.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,
						true, true, 1, 1));
				autoEmpty.setText("�Զ����");

				singleLineDisplay = new Button(left_top_cps_below, SWT.CHECK);
				singleLineDisplay.setLayoutData(new GridData(SWT.CENTER,
						SWT.CENTER, true, true, 1, 1));
				singleLineDisplay.setText("�Զ�����");

				Button emptyReceiveTxt = new Button(left_top_cps_below,
						SWT.NONE);
				emptyReceiveTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
						true, true, 1, 1));
				emptyReceiveTxt.setText("��ս�����");
				emptyReceiveTxt
						.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent e) {
								commRxTxt.setText("");
								rxCountForClear = 0;
							};
						});

				hexDisplay = new Button(left_top_cps_below, SWT.CHECK);
				hexDisplay.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,
						true, true, 1, 1));
				hexDisplay.setText("HEX��ʾ");
			}

		}

		// �м䲿λ
		Composite mid_cps = new Composite(shell, SWT.None);
		mid_cps.setLayout(new org.eclipse.swt.layout.GridLayout(5, true));
		FormData mid_fd = new FormData();
		mid_fd.top = new FormAttachment(60);
		mid_fd.bottom = new FormAttachment(85); // ��ռshell(����)�ߵ�25%
		mid_fd.left = new FormAttachment(0);
		mid_fd.right = new FormAttachment(100); // ��ռshell(����)100%
		mid_cps.setLayoutData(mid_fd);
		{
			Button manuallySend = new Button(mid_cps, SWT.PUSH);
			manuallySend.setLayoutData(new GridData(GridData.FILL_BOTH));
			manuallySend.setText("�ֶ�����");
			manuallySend.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					if (commBeOpened) {
						try {
							// �Ƿ���HEX��ʽ����
							if (hexSend.getSelection()) {
								commTxData = FormatUtil.stringAsHex(commTxTxt
										.getText());
							} else {
								commTxData = commTxTxt.getText().getBytes();
							}
							outputStream.write(commTxData);
						} catch (IOException e1) {
							e1.printStackTrace();
						}

						if (autoEmpty.getSelection()
								&& commRxTxt.getLineCount() > 8) {
							commRxTxt.setText("");
						}
						commRxTxt.append("TX:" + commTxTxt.getText());
						if (singleLineDisplay.getSelection()) {
							commRxTxt.append("\n\r");
						}

						txCount += commTxData.length;
						txCountLabel.setText("TX:" + txCount);
					} else {
						warmDialog("����û�д�");
						return;
					}
				}
			});

			hexSend = new Button(mid_cps, SWT.CHECK);
			hexSend.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
					true, 1, 1));
			hexSend.setText("HEX����");

			commTxTxt = new Text(mid_cps, SWT.MULTI); // Ҫ���˿ڷ��͵�����
			commTxTxt.setEnabled(true);
			commTxTxt.setText("�˴�����Ҫ���͵�����");
			commTxTxt.setFont(SWTResourceManager.getFont("Microsoft YaHei UI",
					11, SWT.NORMAL));
			GridData commTxTxt_GData = new GridData(GridData.FILL_BOTH);
			commTxTxt_GData.horizontalSpan = 3;
			commTxTxt_GData.verticalSpan = 2;
			commTxTxt.setLayoutData(commTxTxt_GData);

			Button emptyRefill = new Button(mid_cps, SWT.PUSH);
			emptyRefill.setLayoutData(new GridData(GridData.FILL_BOTH));
			emptyRefill.setText("�������");
			emptyRefill.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					commTxTxt.setText("");
				}
			});

			Label sendTip = new Label(mid_cps, SWT.NONE);
			sendTip.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
					true, 1, 1));
			sendTip.setText("���͵�����==>");
		}

		// �����
		Composite north_cps = new Composite(shell, SWT.NONE);
		north_cps.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		north_cps.setLayout(new org.eclipse.swt.layout.GridLayout(10, true));
		FormData north_fd = new FormData();
		north_fd.top = new FormAttachment(mid_cps);
		north_fd.bottom = new FormAttachment(100); // ��Ϊmid_cps�ױߵ����ڵײ�
		north_fd.right = new FormAttachment(100);
		north_fd.left = new FormAttachment(0); // ��ռshell(����)100%
		north_cps.setLayoutData(north_fd);
		{
			autoSend = new Button(north_cps, SWT.CHECK | SWT.CENTER);
			autoSend.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
					true, 2, 1));
			autoSend.setText("�Զ�����");
			autoSend.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					if (autoSend.getSelection()) {
						sendTermTxt.setEnabled(false);
						if (commBeOpened) {
							startAutoSendThread();
						}
					} else {
						sendTermTxt.setEnabled(true);
						if (autoSendThread != null) {
							autoSendThread.stop();
						}
						autoSendThread = null;
					}
				}
			});

			sendTermTxt = new Text(north_cps, SWT.RIGHT);
			sendTermTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
					true, 1, 1));
			sendTermTxt.setText("1000");
			sendTermTxt.setFont(SWTResourceManager.getFont(
					"Microsoft YaHei UI", 13, SWT.ITALIC));

			Label ms_lb = new Label(north_cps, SWT.NONE);		//��λms
			ms_lb.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true,
					1, 1));
			ms_lb.setText("ms");

			final Button selectSendFile = new Button(north_cps, SWT.NONE);
			selectSendFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog dialog = new FileDialog(shell, SWT.OPEN);
					dialog.setFilterPath("");// ����Ĭ�ϵ�·��
					dialog.setText("ѡ��Ҫ���͵��ļ�");// ���öԻ���ı���
					dialog.setFileName("");// ����Ĭ�ϵ��ļ���
					dialog.setFilterNames(new String[] { "�ı��ļ� (*.txt)",
							"�����ļ�(*.*)" });// ������չ��
					dialog.setFilterExtensions(new String[] { "*.txt", "*.*" });// �����ļ���չ��
					sendFilePath = dialog.open(); // ��ȡ�ļ�·��
					if (sendFilePath != null) {
						filePathLable.setText(sendFilePath);
					} else {
						filePathLable.setText("��û��ѡ���ļ�");
						sendFilePath = null;
					}
				}
			});
			selectSendFile.setText("ѡ���͵��ļ�");
			selectSendFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					true, 2, 1));

			filePathLable = new Label(north_cps, SWT.BORDER | SWT.CENTER);
			filePathLable.setText("��û��ѡ����ļ�");
			filePathLable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					true, 3, 1));

			Button sendFile = new Button(north_cps, SWT.NONE);
			sendFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (commBeOpened && !autoSend.getSelection()) {
						if (sendFilePath == null) {
							warmDialog("����ѡ���ļ���");
							return;
						}
						new Thread(new FileSendRunnable()).start();
					} else if (!commBeOpened) {
						warmDialog("����û�д�! ");
					}
				}
			});
			sendFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
					1, 1));
			sendFile.setText("�����ļ�");

			final Button onTop = new Button(north_cps, SWT.CHECK | SWT.CENTER);
			onTop.setToolTipText("���ʹ����ʼ������ǰ");
			onTop.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true,
					true, 1, 1));
			onTop.setText("�ö�����");

			// ����ʼ����ǰ����
			onTop.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					if (onTop.getSelection()) {
						OS.SetWindowPos(shell.handle, OS.HWND_TOPMOST, 0, 0,
								shell.getSize().x, shell.getSize().y, SWT.NULL);
						onTop.setToolTipText("���ȡ������ʼ������ǰ");
						onTop.setBackground(new Color(display, 135, 206, 250));
					} else {
						OS.SetWindowPos(shell.handle, OS.HWND_NOTOPMOST, 0, 0,
								shell.getSize().x, shell.getSize().y, SWT.NULL);
						onTop.setToolTipText("���ʹ����ʼ������ǰ");
						onTop.setBackground(new Color(display, 238, 238, 238));
					}
					shellCentered();
				}
			});

			stateTxt = new Label(north_cps, SWT.NONE);
			stateTxt.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
					true, 4, 1));
			stateTxt.setText("�㶫ʡ�ƻ����˼����о�����--��������");

			rxCountLabel = new Label(north_cps, SWT.NONE);
			rxCountLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
					false, false, 2, 1));
			rxCountLabel.setText("RX:0       ");

			txCountLabel = new Label(north_cps, SWT.NONE);
			txCountLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true,
					true, 2, 1));
			txCountLabel.setText("RTX:0       ");

			Button countReset = new Button(north_cps, SWT.NONE);
			countReset.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					true, 1, 1));
			countReset.setText("����");
		}
	}

	/*���ھ���*/
	private void shellCentered() {
		Rectangle bounds = display.getPrimaryMonitor().getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation(x, y);
	}

	/*�򿪴��ڼ���*/
	class OpenCommListenter extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			super.widgetSelected(e);
			if (commBeOpened) { 
				//��ȫ�رմ���
				closeSerialPort();	
				openCommButton.setText("�򿪴���");
				stateTxt.setText("�����ѹر�");
			} else {
				//�򿪴���
				baudRates = Integer.valueOf(baudChoice.getText());
				if (parityChoice.getText().contains("NONE")) {
					parityBits = SerialPort.PARITY_NONE;
				} else if (parityChoice.getText().contains("ODD")) {
					parityBits = SerialPort.PARITY_ODD;
				} else if (parityChoice.getText().contains("EVEN")) {
					parityBits = SerialPort.PARITY_EVEN;
				}
				dataBits = Integer.valueOf(dataChoice.getText());
				stopBits = Integer.valueOf(stopChoice.getText());
				serialPort_set(commChoice.getText());	//�򿪴��ڣ���д��������
				if (!commBeOpened) {
					return;
				}
				// �Զ���������
				if (autoSend.getSelection()) {
					startAutoSendThread();
				}
				//UI����
				openCommButton.setText("�رմ���");
				stateTxt.setText(commChoice.getText() + "�Ѵ򿪡�" + baudRates
						+ "," + parityChoice.getText() + "," + dataBits + ","
						+ stopBits);
			}
		}
	}
	
	/*��ȫ�رմ���*/
	public void closeSerialPort() {
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
		if (autoSendThread != null) { // �ر��߳�
			autoSendThread.stop();
		}
		autoSendThread = null;
		commBeOpened = false;
	}

	/* ���ô��ڲ��� */
	public void serialPort_set(String com) {
		try {
			/* �򿪴��� */
			portId = CommPortIdentifier.getPortIdentifier(com);
			serialPort = (SerialPort) portId.open("Comm", 2000);
			inputStream = serialPort.getInputStream();
			commBeOpened = true;
			serialPort.setSerialPortParams(baudRates, dataBits, stopBits,
					parityBits);
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			outputStream = serialPort.getOutputStream();

			// �����¼���������ȡ�˿�����
			serialPort.addEventListener(new SerialPortEventListener() {
				public void serialEvent(SerialPortEvent event) {
					int a = 0;
					final byte[] readBuffer = new byte[1024];
					// ��GUI�̸߳��£����ѡ���Զ������8���Զ���գ�������ַ�>1000�����
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							if (autoEmpty.getSelection()
									&& (singleLineDisplay.getSelection() && commRxTxt
											.getLineCount() > 8)
									|| rxCountForClear > 1000) {
								commRxTxt.setText("");
								rxCountForClear = 0;
							}
						}
					});
					
					try { /* ����·�϶�ȡ������ */
						while (inputStream.available() > 0) {
							a = inputStream.read(readBuffer);
							rxCountForClear += a;
							rxCount += a;
						}
					} catch (IOException e) {
						e.printStackTrace();

					}
					final String str = new String(readBuffer).substring(0, a);
					final int aTemp = a;
					// GUI�̸߳���
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							/* ���յ������ݴ�ŵ��ı����� */
							if (hexDisplay.getSelection()) {
								commRxTxt.append("RX:"
										+ FormatUtil.byteToHex(readBuffer,
												aTemp));
							} else {
								commRxTxt.append("RX:" + str);
							}
							if (singleLineDisplay.getSelection()) {
								commRxTxt.append("\n\r");
							}
							rxCountLabel.setText("RX:" + rxCount);
						}
					});
				}
			});
		} catch (TooManyListenersException | UnsupportedCommOperationException
				| IOException e) {
			e.printStackTrace();
		} catch (NoSuchPortException e1) {
			warmDialog("���ڲ�����");
			commBeOpened = false;
			return;
		} catch (PortInUseException e) {
			commBeOpened = false;
			warmDialog("���ڴ򿪴�������˿��Ƿ���ڻ��ѱ�ռ�ã�");
			return;
		}
		/* ����������������,���������¼� */
		serialPort.notifyOnDataAvailable(true);
	}

	/*���洰��*/
	public void warmDialog(String content) {
		MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING
				| SWT.CENTER);
		messageBox.setMessage(content);
		messageBox.open();
	}

	/*����ũҵ��Ϣʵʱ��ʾ*/
	public void startSelectTimer() {
		selectTimer = new Timer();
		selectTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
                display.asyncExec(new Runnable() {

                    public void run() {
                        ArgInfoChart.addToDataset();
                    }

                });						
			}
		}, 2000, 2000);
	}
	
	/*�����Զ���������*/
	public void startAutoSendThread() {
		autoSendState = autoSend.getSelection();
		if (autoSendState) {
			if (Integer.valueOf(sendTermTxt.getText()) > 0) {
				sendTerm = Integer.valueOf(sendTermTxt.getText());
			}
			if (autoSendThread == null) {
				autoSendThread = new AutoSendThread();
			}
			autoSendThread.start();
		}
	}
	
	/* �Զ��������ݵ��߳� */
	public class AutoSendThread extends Thread {
		@Override
		public void run() {
			super.run();
			while (commBeOpened && autoSendState) {
				/* ����������(������data[]�е����ݷ��ͳ�ȥ) */
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						try {
							// �Ƿ���HEX��ʽ����
							if (hexSend.getSelection()) {
								commTxData = FormatUtil.stringAsHex(commTxTxt
										.getText());
							} else {
								commTxData = commTxTxt.getText().getBytes();
							}
							outputStream.write(commTxData);
							txCount += commTxData.length;
							txCountLabel.setText("TX:" + txCount);

							if (autoEmpty.getSelection()
									&& commRxTxt.getLineCount() > 8) {
								commRxTxt.setText("");
							}
							commRxTxt.append("TX:" + commTxTxt.getText());
							if (singleLineDisplay.getSelection()) {
								commRxTxt.append("\n\r");
							}

							/* �������ݺ�����,Ȼ�����ط� */
						} catch (IOException e) {
							e.printStackTrace();
							System.out.println("���ݷ��ʹ���");
						}
					}
				});
				try {
					if (sendTerm > 1) {
						Thread.sleep(sendTerm);
					} else {
						return;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/* �����ļ� */
	class FileSendRunnable implements Runnable {
		private BufferedInputStream bis;

		public void run() {
			int b = 0;
			byte[] buff = new byte[1024];
			if (commBeOpened) {
				try {
					bis = new BufferedInputStream(new FileInputStream(
							sendFilePath));
					/* ����������(������data[]�е����ݷ��ͳ�ȥ) */
					while ((b = bis.read(buff, 0, buff.length)) != -1) {
						outputStream.write(buff, 0, b);
						outputStream.flush();
						txCount += b;
						display.asyncExec(new Runnable() {

							@Override
							public void run() {
								txCountLabel.setText("TX:" + txCount);
							}
						});
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
