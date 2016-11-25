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
	public static ArrayList<String> deviceList;	//存储已识别的节点
	private CommPortIdentifier portId; // 通信端口标识符
	private SerialPort serialPort; // 串行端口
	private OutputStream outputStream; // 向端口发送数据
	private InputStream inputStream; // 读取端口数据
	private int sendTerm = 0;	//自动发送数据时间间隔
	private int baudRates = 9600; // 波特率初始数值
	private int dataBits = 8; // 数据位初始数值
	private int stopBits = 1; // 停止位初始数值
	private int parityBits = SerialPort.PARITY_NONE; // 校验位初始数值
	private int rxCount = 0; // 接收的字节数
	private int txCount = 0; // 发送的字节数
	private int rxCountForClear = 0; // 统计接收的字符，如果字符数溢出，自动清空
	private byte[] commTxData = null;
	private ArrayList<String> commList; // 存在的端口
	private boolean commBeOpened = false; // 串口是否被打开
	private boolean autoSendState = false;	//是否自动发送
	private boolean selectState = false;	//动态图表是否工作
	private Timer selectTimer ;
	private Thread autoSendThread; // 自动发送线程
	private String sendFilePath; // 待发送的文件
    
	private Combo commChoice; // 端口选择
	private Combo baudChoice; // 波特率选择
	private Combo parityChoice; // 校验位选择
	private Combo dataChoice; // 数据位选择
	private Combo stopChoice; // 停止位选择
	private Text commRxTxt; // 显示接收的内容
	private Button openCommButton; // 打开串口
	private Button autoEmpty; // 自动清空
	private Button hexDisplay; // HEX显示
	private Button singleLineDisplay; // 自动换行
	private Button hexSend; // HEX发送
	private Text commTxTxt; // 发送数据窗口
	private Button autoSend;// 自动发送
	private Label stateTxt; // 最下面的状态栏
	private Text sendTermTxt; // 发送数据间隔
	private Label rxCountLabel; // 接收字符数量
	private Label txCountLabel; // 发送字符数量
	private Label filePathLable;// 显示文件路径

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
		dbHelper = new DBHelper(); // 连接数据库
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
		shell.setText("农业信息实时监控");
		shellCentered(); // 窗口居中
		shell.setLayout(new FormLayout()); // 表单布局
		shell.addShellListener(new ShellAdapter() {
			// 结束关流
			@Override
			public void shellClosed(ShellEvent e) {
				super.shellClosed(e);
				closeSerialPort();	//安全关闭串口
				if (selectTimer != null) {
					selectTimer.cancel();
				}
			}
		});

		// 右上内容显示框(已经隐藏此控件)
		commRxTxt = new Text(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.CANCEL | SWT.MULTI);
		commRxTxt.setFont(SWTResourceManager.getFont("Microsoft YaHei UI", 12,
				SWT.NORMAL));
		commRxTxt.setEditable(false);
		FormData content_fd = new FormData();
		content_fd.top = new FormAttachment(0);
		content_fd.bottom = new FormAttachment(0); // 宽占shell(窗口)宽的0%
		content_fd.left = new FormAttachment(0);
		content_fd.right = new FormAttachment(0); // 高占shell(窗口)高的0%
		commRxTxt.setLayoutData(content_fd);

		//动态曲线图表
		Composite right_top_cps = new Composite(shell, SWT.None);
		right_top_cps.setLayout(new FillLayout(SWT.HORIZONTAL));
		FormData right_top_fd = new FormData();
		right_top_fd.top = new FormAttachment(0);
		right_top_fd.bottom = new FormAttachment(60); // 宽占shell(窗口)宽的60%
		right_top_fd.left = new FormAttachment(40);
		right_top_fd.right = new FormAttachment(100); // 高占shell(窗口)高的60%
		right_top_cps.setLayoutData(right_top_fd);
		
		JFreeChart chart = ArgInfoChart.createChart(ArgInfoChart.dataset);	//创建图表
		ChartComposite frame = new ChartComposite(right_top_cps, SWT.NONE, chart, true);
        frame.setDisplayToolTips(true);
        frame.setHorizontalAxisTrace(false);
        frame.setVerticalAxisTrace(false);
		
		
		// 左上串口功能选项,left_top_cps包含上半面板left_top_cps_above和下半面板left_top_cps_below
		Composite left_top_cps = new Composite(shell, SWT.None);
		left_top_cps.setLayout(new FormLayout());
		FormData left_top_fd = new FormData();
		left_top_fd.top = new FormAttachment(0);
		left_top_fd.bottom = new FormAttachment(60); // 高占shell(窗口)高的60%
		left_top_fd.left = new FormAttachment(0);
		left_top_fd.right = new FormAttachment(40); // 宽占shell(窗口)宽的40%
		left_top_cps.setLayoutData(left_top_fd);
		{
			Composite left_top_cps_above = new Composite(left_top_cps, SWT.NONE);
			left_top_cps_above.setLayout(new org.eclipse.swt.layout.GridLayout(
					2, true)); // 2列分布
			FormData left_top_fd_above = new FormData();
			left_top_fd_above.top = new FormAttachment(0);
			left_top_fd_above.bottom = new FormAttachment(60); // 高占left_top_cps面板高的60%
			left_top_fd_above.left = new FormAttachment(0);
			left_top_fd_above.right = new FormAttachment(100); // 宽占left_top_cps面板宽的100%
			left_top_cps_above.setLayoutData(left_top_fd_above);
			{
				Label commLb = new Label(left_top_cps_above, SWT.NONE);
				commLb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
						true, 1, 1));
				commLb.setText("串口号");

				commChoice = new Combo(left_top_cps_above, SWT.NONE);
				commChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
						true, true, 1, 1));
				commList = new ArrayList<String>();
				// 串口初始化
				Enumeration<?> en = CommPortIdentifier.getPortIdentifiers();
				// commChoice添加可以选择的端口
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
				baudLb.setText("波特率");

				baudChoice = new Combo(left_top_cps_above, SWT.NONE);
				baudChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
						true, true, 1, 1));
				baudChoice.add("300"); // baudChoice添加可以选择的波特率
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
				baudChoice.select(5); // 默认选择第5个

				Label parityLb = new Label(left_top_cps_above, SWT.NONE);
				parityLb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,
						true, true, 1, 1));
				parityLb.setText("校验位");

				parityChoice = new Combo(left_top_cps_above, SWT.NONE);
				parityChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
						true, true, 1, 1));
				parityChoice.add("无NONE"); // parityChoice添加可以选择的校验值
				parityChoice.add("奇ODD");
				parityChoice.add("偶EVEN");
				parityChoice.select(0);

				Label dataLb = new Label(left_top_cps_above, SWT.NONE);
				dataLb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
						true, 1, 1));
				dataLb.setText("数据位");

				dataChoice = new Combo(left_top_cps_above, SWT.NONE);
				dataChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
						true, true, 1, 1));
				dataChoice.add("8"); // dataChoice添加可以选择的数据位
				dataChoice.add("7");
				dataChoice.add("6");
				dataChoice.select(0);

				Label stopLb = new Label(left_top_cps_above, SWT.NONE);
				stopLb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
						true, 1, 1));
				stopLb.setText("停止位");

				stopChoice = new Combo(left_top_cps_above, SWT.NONE);
				stopChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
						true, true, 1, 1));
				stopChoice.add("1"); // stopChoice添加可以选择的停止位
				stopChoice.add("2");
				stopChoice.select(0);
			}

			// 左上面板的下半部分
			Composite left_top_cps_below = new Composite(left_top_cps, SWT.None);
			left_top_cps_below.setLayout(new org.eclipse.swt.layout.GridLayout(
					2, true));
			FormData left_top_fd_2 = new FormData();
			left_top_fd_2.top = new FormAttachment(60);
			left_top_fd_2.right = new FormAttachment(100); // 高占left_top_Cps高的40%
			left_top_fd_2.left = new FormAttachment(0);
			left_top_fd_2.bottom = new FormAttachment(100); // 宽占left_top_Cps宽的100%
			left_top_cps_below.setLayoutData(left_top_fd_2);
			{
				openCommButton = new Button(left_top_cps_below, SWT.NONE);
				openCommButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
						true, true, 1, 1));
				openCommButton.setText("打开串口");
				openCommButton.addSelectionListener(new OpenCommListenter());
				
				final Button startButton = new Button(left_top_cps_below, SWT.NONE);
				startButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if(!selectState) {
							startSelectTimer();
							startButton.setText("暂停");
						}else {
							selectTimer.cancel();
							startButton.setText("开启");
						}
						selectState = !selectState;
					}
				});
				startButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
						true, true, 1, 1));
				startButton.setText("启动");

				autoEmpty = new Button(left_top_cps_below, SWT.CHECK);
				autoEmpty.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,
						true, true, 1, 1));
				autoEmpty.setText("自动清空");

				singleLineDisplay = new Button(left_top_cps_below, SWT.CHECK);
				singleLineDisplay.setLayoutData(new GridData(SWT.CENTER,
						SWT.CENTER, true, true, 1, 1));
				singleLineDisplay.setText("自动换行");

				Button emptyReceiveTxt = new Button(left_top_cps_below,
						SWT.NONE);
				emptyReceiveTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
						true, true, 1, 1));
				emptyReceiveTxt.setText("清空接收区");
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
				hexDisplay.setText("HEX显示");
			}

		}

		// 中间部位
		Composite mid_cps = new Composite(shell, SWT.None);
		mid_cps.setLayout(new org.eclipse.swt.layout.GridLayout(5, true));
		FormData mid_fd = new FormData();
		mid_fd.top = new FormAttachment(60);
		mid_fd.bottom = new FormAttachment(85); // 高占shell(窗口)高的25%
		mid_fd.left = new FormAttachment(0);
		mid_fd.right = new FormAttachment(100); // 宽占shell(窗口)100%
		mid_cps.setLayoutData(mid_fd);
		{
			Button manuallySend = new Button(mid_cps, SWT.PUSH);
			manuallySend.setLayoutData(new GridData(GridData.FILL_BOTH));
			manuallySend.setText("手动发送");
			manuallySend.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					if (commBeOpened) {
						try {
							// 是否以HEX形式发送
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
						warmDialog("串口没有打开");
						return;
					}
				}
			});

			hexSend = new Button(mid_cps, SWT.CHECK);
			hexSend.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
					true, 1, 1));
			hexSend.setText("HEX发送");

			commTxTxt = new Text(mid_cps, SWT.MULTI); // 要给端口发送的内容
			commTxTxt.setEnabled(true);
			commTxTxt.setText("此处输入要发送的数据");
			commTxTxt.setFont(SWTResourceManager.getFont("Microsoft YaHei UI",
					11, SWT.NORMAL));
			GridData commTxTxt_GData = new GridData(GridData.FILL_BOTH);
			commTxTxt_GData.horizontalSpan = 3;
			commTxTxt_GData.verticalSpan = 2;
			commTxTxt.setLayoutData(commTxTxt_GData);

			Button emptyRefill = new Button(mid_cps, SWT.PUSH);
			emptyRefill.setLayoutData(new GridData(GridData.FILL_BOTH));
			emptyRefill.setText("清空重填");
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
			sendTip.setText("发送的数据==>");
		}

		// 下面板
		Composite north_cps = new Composite(shell, SWT.NONE);
		north_cps.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		north_cps.setLayout(new org.eclipse.swt.layout.GridLayout(10, true));
		FormData north_fd = new FormData();
		north_fd.top = new FormAttachment(mid_cps);
		north_fd.bottom = new FormAttachment(100); // 高为mid_cps底边到窗口底部
		north_fd.right = new FormAttachment(100);
		north_fd.left = new FormAttachment(0); // 宽占shell(窗口)100%
		north_cps.setLayoutData(north_fd);
		{
			autoSend = new Button(north_cps, SWT.CHECK | SWT.CENTER);
			autoSend.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
					true, 2, 1));
			autoSend.setText("自动发送");
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

			Label ms_lb = new Label(north_cps, SWT.NONE);		//单位ms
			ms_lb.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true,
					1, 1));
			ms_lb.setText("ms");

			final Button selectSendFile = new Button(north_cps, SWT.NONE);
			selectSendFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog dialog = new FileDialog(shell, SWT.OPEN);
					dialog.setFilterPath("");// 设置默认的路径
					dialog.setText("选择要发送的文件");// 设置对话框的标题
					dialog.setFileName("");// 设置默认的文件名
					dialog.setFilterNames(new String[] { "文本文件 (*.txt)",
							"所有文件(*.*)" });// 设置扩展名
					dialog.setFilterExtensions(new String[] { "*.txt", "*.*" });// 设置文件扩展名
					sendFilePath = dialog.open(); // 获取文件路径
					if (sendFilePath != null) {
						filePathLable.setText(sendFilePath);
					} else {
						filePathLable.setText("还没有选择文件");
						sendFilePath = null;
					}
				}
			});
			selectSendFile.setText("选择发送的文件");
			selectSendFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					true, 2, 1));

			filePathLable = new Label(north_cps, SWT.BORDER | SWT.CENTER);
			filePathLable.setText("还没有选择的文件");
			filePathLable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					true, 3, 1));

			Button sendFile = new Button(north_cps, SWT.NONE);
			sendFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (commBeOpened && !autoSend.getSelection()) {
						if (sendFilePath == null) {
							warmDialog("请先选择文件！");
							return;
						}
						new Thread(new FileSendRunnable()).start();
					} else if (!commBeOpened) {
						warmDialog("串口没有打开! ");
					}
				}
			});
			sendFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
					1, 1));
			sendFile.setText("发送文件");

			final Button onTop = new Button(north_cps, SWT.CHECK | SWT.CENTER);
			onTop.setToolTipText("点击使窗口始终在最前");
			onTop.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true,
					true, 1, 1));
			onTop.setText("置顶窗口");

			// 窗口始终最前设置
			onTop.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					if (onTop.getSelection()) {
						OS.SetWindowPos(shell.handle, OS.HWND_TOPMOST, 0, 0,
								shell.getSize().x, shell.getSize().y, SWT.NULL);
						onTop.setToolTipText("点击取消窗口始终在最前");
						onTop.setBackground(new Color(display, 135, 206, 250));
					} else {
						OS.SetWindowPos(shell.handle, OS.HWND_NOTOPMOST, 0, 0,
								shell.getSize().x, shell.getSize().y, SWT.NULL);
						onTop.setToolTipText("点击使窗口始终在最前");
						onTop.setBackground(new Color(display, 238, 238, 238));
					}
					shellCentered();
				}
			});

			stateTxt = new Label(north_cps, SWT.NONE);
			stateTxt.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
					true, 4, 1));
			stateTxt.setText("广东省云机器人技术研究中心--串口助手");

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
			countReset.setText("清零");
		}
	}

	/*窗口居中*/
	private void shellCentered() {
		Rectangle bounds = display.getPrimaryMonitor().getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation(x, y);
	}

	/*打开串口监听*/
	class OpenCommListenter extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			super.widgetSelected(e);
			if (commBeOpened) { 
				//安全关闭串口
				closeSerialPort();	
				openCommButton.setText("打开串口");
				stateTxt.setText("串口已关闭");
			} else {
				//打开串口
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
				serialPort_set(commChoice.getText());	//打开串口，读写数据设置
				if (!commBeOpened) {
					return;
				}
				// 自动发送数据
				if (autoSend.getSelection()) {
					startAutoSendThread();
				}
				//UI更新
				openCommButton.setText("关闭串口");
				stateTxt.setText(commChoice.getText() + "已打开。" + baudRates
						+ "," + parityChoice.getText() + "," + dataBits + ","
						+ stopBits);
			}
		}
	}
	
	/*安全关闭串口*/
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
		if (autoSendThread != null) { // 关闭线程
			autoSendThread.stop();
		}
		autoSendThread = null;
		commBeOpened = false;
	}

	/* 设置串口参数 */
	public void serialPort_set(String com) {
		try {
			/* 打开串口 */
			portId = CommPortIdentifier.getPortIdentifier(com);
			serialPort = (SerialPort) portId.open("Comm", 2000);
			inputStream = serialPort.getInputStream();
			commBeOpened = true;
			serialPort.setSerialPortParams(baudRates, dataBits, stopBits,
					parityBits);
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			outputStream = serialPort.getOutputStream();

			// 设置事件监听，读取端口数据
			serialPort.addEventListener(new SerialPortEventListener() {
				public void serialEvent(SerialPortEvent event) {
					int a = 0;
					final byte[] readBuffer = new byte[1024];
					// 在GUI线程更新，如果选择自动清空则8行自动清空，否则等字符>1000才清空
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
					
					try { /* 从线路上读取数据流 */
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
					// GUI线程更新
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							/* 接收到的数据存放到文本区中 */
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
			warmDialog("串口不存在");
			commBeOpened = false;
			return;
		} catch (PortInUseException e) {
			commBeOpened = false;
			warmDialog("串口打开错误，请检查端口是否存在或已被占用！");
			return;
		}
		/* 侦听到串口有数据,触发串口事件 */
		serialPort.notifyOnDataAvailable(true);
	}

	/*警告窗口*/
	public void warmDialog(String content) {
		MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING
				| SWT.CENTER);
		messageBox.setMessage(content);
		messageBox.open();
	}

	/*开启农业信息实时显示*/
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
	
	/*开启自动发送数据*/
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
	
	/* 自动发送数据的线程 */
	public class AutoSendThread extends Thread {
		@Override
		public void run() {
			super.run();
			while (commBeOpened && autoSendState) {
				/* 发送数据流(将数组data[]中的数据发送出去) */
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						try {
							// 是否以HEX形式发送
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

							/* 发送数据后休眠,然后再重发 */
						} catch (IOException e) {
							e.printStackTrace();
							System.out.println("数据发送错误");
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

	/* 发送文件 */
	class FileSendRunnable implements Runnable {
		private BufferedInputStream bis;

		public void run() {
			int b = 0;
			byte[] buff = new byte[1024];
			if (commBeOpened) {
				try {
					bis = new BufferedInputStream(new FileInputStream(
							sendFilePath));
					/* 发送数据流(将数组data[]中的数据发送出去) */
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
