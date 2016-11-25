package com.comm.ui;

import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.Date;
import java.util.Random;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import com.comm.bean.FixedLengthMap;
import com.comm.bean.Node;
import com.comm.utils.PropertiesUtil;

public class ArgInfoChart {
	public static TimeSeriesCollection dataset = new TimeSeriesCollection();
	private static FixedLengthMap dataMp = new FixedLengthMap();
	private static FixedLengthMap dataMp1 = new FixedLengthMap();
	private static TimeSeries s1 = new TimeSeries("温度");
	private static TimeSeries s2 = new TimeSeries("噪音");

	/**
	 * Creates a chart.
	 * 
	 * @param dataset
	 *            a dataset.
	 * 
	 * @return A chart.
	 */
	public static JFreeChart createChart(XYDataset dataset) {

		JFreeChart chart = ChartFactory.createTimeSeriesChart(
				"Environmental info", // title
				"time", // x-axis label
				"state", // y-axis label
				dataset, // data
				true, // create legend?
				true, // generate tooltips?
				false // generate URLs?
				);

		chart.setBackgroundPaint(java.awt.Color.white);
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(java.awt.Color.lightGray);
		plot.setDomainGridlinePaint(java.awt.Color.white);
		plot.setRangeGridlinePaint(java.awt.Color.white);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.getRangeAxis().setUpperBound(100);
		XYItemRenderer r = plot.getRenderer();
		if (r instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			renderer.setBaseShapesVisible(true);
			renderer.setBaseShapesFilled(true);
		}

		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("mm:ss"));

		return chart;

	}

	/**
	 * Creates a dataset, consisting of two series of monthly data.
	 *
	 * @return The dataset.
	 */
	public static XYDataset addToDataset() {// 更新数据
		int deviceID = new Random().nextInt(3); // id
		createTable("" + deviceID); // 首次捕获到该节点信息时建表

		Random r = new Random();// 随机数
		int tep = 24 + r.nextInt(2);
		int noice = 45 + r.nextInt(10);
		dataMp.put(new Second(), tep);
		dataMp1.put(new Second(), noice);
		s1.clear();
		s2.clear();
		for (Entry<Second, Integer> e : dataMp.entrySet()) {
			s1.add(e.getKey(), e.getValue());
		}
		for (Entry<Second, Integer> e : dataMp1.entrySet()) {
			s2.add(e.getKey(), e.getValue());
		}
		dataset.removeAllSeries();
		dataset.addSeries(s1);
		dataset.addSeries(s2);

		// 保存数据
		Node node = new Node("" + deviceID, tep, noice, new Date().getTime());
		MainActivity.dbHelper.insertSQL(node);
		// 删除24秒的数据s
		// dbHelper.deleteSQL(node.getId(), new Date().getTime());

		return dataset;
	}

	/* 动态建表 */
	private static synchronized void createTable(String deviceID) {
		if (!MainActivity.deviceList.contains(String.valueOf(deviceID))) {
			MainActivity.deviceList.add(String.valueOf(deviceID));

			int devNum = MainActivity.deviceList.size();
			String tableSql = "create table device_a"
					+ deviceID
					+ " (_id integer auto_increment primary key, tep float, noice float, date bigint);";
			MainActivity.dbHelper.updateSQL(tableSql);
			// 存储节点数
			PropertiesUtil.writeProperties("deviceNum", String.valueOf(devNum));
			PropertiesUtil.writeProperties("deviceID" + (devNum - 1),
					String.valueOf(devNum - 1));
			System.out.println("deviceID=" + deviceID);
		}

	}
}
