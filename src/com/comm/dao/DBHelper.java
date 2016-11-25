package com.comm.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.comm.bean.Node;

public class DBHelper {
	// 驱动程序名
	private static String driver = "com.mysql.jdbc.Driver";
	// URL指向要访问的原生数据库名mysql "jdbc:mysql://[ip地址]:[端口号]/[数据库名]"
	private static String url = "jdbc:mysql://127.0.0.1:3306/mysql";
	// MySQL配置时的用户名
	private static String user = "root";
	// MySQL配置时的密码
	private static String password = "123456";
	private static Connection conn = null;
	private static PreparedStatement statement = null;

	public DBHelper() {
		getConn();
	}

	static {
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			System.out.println("加载Mysql数据库驱动失败！");
		}
	}

	/**
	 * 连接数据库
	 */
	public void getConn() {
		try {
			if (conn == null) {
				conn = DriverManager.getConnection(url, user, password);
			}
			if (!conn.isClosed()) {
				System.out.println("Succeeded connecting to the Database!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 关闭数据库
	 */
	public void deconnSQL() {
		try {
			if (conn != null)
				conn.close();
		} catch (Exception e) {
			System.out.println("关闭数据库异常：");
			e.printStackTrace();
		}
	}

	/**
	 * 执行查询sql语句
	 * 
	 * @param sql
	 * @return
	 */
	public ResultSet selectSQL(String sql) {
		ResultSet rs = null;
		try {
			statement = conn.prepareStatement(sql);
			rs = statement.executeQuery(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rs;
	}

	/**
	 * 根据设备ID查询设备的属性
	 * 
	 * @param deviceID
	 * @return node
	 */
	public Node selectByID(String deviceID) {
		ResultSet rs = null;
		Node node = null;
		String sql = "select tep,noice,date from device_a" + deviceID;
		try {
			statement = conn.prepareStatement(sql);
			rs = statement.executeQuery(sql);
			while (rs.next()) {
				node = new Node(deviceID, rs.getFloat(1), rs.getFloat(2),
						rs.getLong(3));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return node;
	}

	/**
	 * 执行插入sql语句
	 * 
	 * @param sql
	 * @return
	 */
	public boolean insertSQL(String sql) {
		try {
			statement = conn.prepareStatement(sql);
			statement.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			System.out.println("插入数据库时出错：");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("插入时出错：");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 把Node的属性insert数据库
	 * 
	 * @param Node
	 * @return boolean
	 */
	public boolean insertSQL(Node node) {
		String sql = "insert into device_a" + node.getId()
				+ "(tep, noice, date) values(?,?,?);";
		try {
			statement = conn.prepareStatement(sql);
			statement.setFloat(1, node.getTep());
			statement.setFloat(2, node.getNoice());
			statement.setLong(3, node.getDate());
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			System.out.println("插入数据库时出错：");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("插入时出错：");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 执行删除sql语句
	 * 
	 * @param sql
	 * @return boolean
	 */
	public boolean deleteSQL(String sql) {
		try {
			statement = conn.prepareStatement(sql);
			statement.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			System.out.println("删除数据库时出错：");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("删除时出错：");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 执行删除sql语句 删除date - interval前的数据
	 * 
	 * @param deviceID
	 *            设备ID
	 * @param date
	 *            时间戳
	 * @return boolean
	 */
	public boolean deleteSQL(String deviceID, long date) {
		String sql = "delete from device_a" + deviceID + " where date < ?";
		int interval = 24000; // 24秒
		try {
			statement = conn.prepareStatement(sql);
			statement.setLong(1, date - interval);
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			System.out.println("删除数据库时出错：");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("删除时出错：");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 执行更新sql语句
	 * 
	 * @param sql
	 * @return boolean
	 */
	public boolean updateSQL(String sql) {
		try {
			statement = conn.prepareStatement(sql);
			statement.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			System.out.println("更新数据库时出错：");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("更新时出错：");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 打印结果集
	 * 
	 * 具体列根据自己的数据库表结构更改
	 * 
	 * @param rs
	 */
	public void print(ResultSet rs) {
		System.out.println("-----------------");
		System.out.println("查询结果:");
		System.out.println("-----------------");

		try {
			while (rs.next()) {
				System.out.println("content=" + rs.getString(1));
			}
		} catch (SQLException e) {
			System.out.println("显示时数据库出错。");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("显示出错。");
			e.printStackTrace();
		}
	}

}