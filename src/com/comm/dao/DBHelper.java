package com.comm.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.comm.bean.Node;

public class DBHelper {
	// ����������
	private static String driver = "com.mysql.jdbc.Driver";
	// URLָ��Ҫ���ʵ�ԭ�����ݿ���mysql "jdbc:mysql://[ip��ַ]:[�˿ں�]/[���ݿ���]"
	private static String url = "jdbc:mysql://127.0.0.1:3306/mysql";
	// MySQL����ʱ���û���
	private static String user = "root";
	// MySQL����ʱ������
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
			System.out.println("����Mysql���ݿ�����ʧ�ܣ�");
		}
	}

	/**
	 * �������ݿ�
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
	 * �ر����ݿ�
	 */
	public void deconnSQL() {
		try {
			if (conn != null)
				conn.close();
		} catch (Exception e) {
			System.out.println("�ر����ݿ��쳣��");
			e.printStackTrace();
		}
	}

	/**
	 * ִ�в�ѯsql���
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
	 * �����豸ID��ѯ�豸������
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
	 * ִ�в���sql���
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
			System.out.println("�������ݿ�ʱ����");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("����ʱ����");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * ��Node������insert���ݿ�
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
			System.out.println("�������ݿ�ʱ����");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("����ʱ����");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * ִ��ɾ��sql���
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
			System.out.println("ɾ�����ݿ�ʱ����");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("ɾ��ʱ����");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * ִ��ɾ��sql��� ɾ��date - intervalǰ������
	 * 
	 * @param deviceID
	 *            �豸ID
	 * @param date
	 *            ʱ���
	 * @return boolean
	 */
	public boolean deleteSQL(String deviceID, long date) {
		String sql = "delete from device_a" + deviceID + " where date < ?";
		int interval = 24000; // 24��
		try {
			statement = conn.prepareStatement(sql);
			statement.setLong(1, date - interval);
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			System.out.println("ɾ�����ݿ�ʱ����");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("ɾ��ʱ����");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * ִ�и���sql���
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
			System.out.println("�������ݿ�ʱ����");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("����ʱ����");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * ��ӡ�����
	 * 
	 * �����и����Լ������ݿ��ṹ����
	 * 
	 * @param rs
	 */
	public void print(ResultSet rs) {
		System.out.println("-----------------");
		System.out.println("��ѯ���:");
		System.out.println("-----------------");

		try {
			while (rs.next()) {
				System.out.println("content=" + rs.getString(1));
			}
		} catch (SQLException e) {
			System.out.println("��ʾʱ���ݿ����");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("��ʾ����");
			e.printStackTrace();
		}
	}

}