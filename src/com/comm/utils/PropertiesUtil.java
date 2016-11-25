package com.comm.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class PropertiesUtil {
	// �����ļ���·��
	static String profilepath = "config.properties";
	/**
	 * ���þ�̬����
	 */
	private static Properties props = new Properties();
	static {
		FileInputStream fis = null;
		try {
			File proFile = new File(profilepath);
			if (!proFile.exists()) {
				proFile.createNewFile();
			}
			fis = new FileInputStream(profilepath);
			props.load(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			System.exit(-1);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * ��ȡ�����ļ�����Ӧ����ֵ
	 * 
	 * @param key
	 *            ����
	 * @return String
	 */
	public static String getKeyValue(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	}

	/**
	 * ��������key��ȡ������ֵvalue
	 * 
	 * @param filePath
	 *            �����ļ�·��
	 * @param key
	 *            ����
	 */
	public static String readValue(String filePath, String key) {
		Properties props = new Properties();
		InputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(filePath));
			props.load(in);
			String value = props.getProperty(key);
			System.out.println(key + "����ֵ�ǣ�" + value);
			in.close();
			return value;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * ���£�����룩һ��properties��Ϣ(���������ֵ) ����������Ѿ����ڣ����¸�������ֵ�� ��������������ڣ�����һ�Լ�ֵ��
	 * 
	 * @param keyname
	 *            ����
	 * @param keyvalue
	 *            ��ֵ
	 */
	public static void writeProperties(String keyname, String keyvalue) {
		OutputStream fos = null;
		try {
			// ���� Hashtable �ķ��� put��ʹ�� getProperty �����ṩ�����ԡ�
			// ǿ��Ҫ��Ϊ���Եļ���ֵʹ���ַ���������ֵ�� Hashtable ���� put �Ľ����
			fos = new FileOutputStream(profilepath);
			props.setProperty(keyname, keyvalue);
			// ���ʺ�ʹ�� load �������ص� Properties ���еĸ�ʽ��
			// ���� Properties ���е������б�����Ԫ�ضԣ�д�������
			props.store(fos, "Update '" + keyname + "' value");
		} catch (IOException e) {
			System.err.println("�����ļ����´���");
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * ����properties�ļ��ļ�ֵ�� ����������Ѿ����ڣ����¸�������ֵ�� ��������������ڣ�����һ�Լ�ֵ��
	 * 
	 * @param keyname
	 *            ����
	 * @param keyvalue
	 *            ��ֵ
	 */
	public void updateProperties(String keyname, String keyvalue) {
		OutputStream fos = null;
		try {
			props.load(new FileInputStream(profilepath));
			// ���� Hashtable �ķ��� put��ʹ�� getProperty �����ṩ�����ԡ�
			// ǿ��Ҫ��Ϊ���Եļ���ֵʹ���ַ���������ֵ�� Hashtable ���� put �Ľ����
			fos = new FileOutputStream(profilepath);
			props.setProperty(keyname, keyvalue);
			// ���ʺ�ʹ�� load �������ص� Properties ���еĸ�ʽ��
			// ���� Properties ���е������б�����Ԫ�ضԣ�д�������
			props.store(fos, "Update '" + keyname + "' value");
		} catch (IOException e) {
			System.err.println("�����ļ����´���");
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	// ���Դ���
	// public static void main(String[] args) {
	// readValue("mail.properties", "MAIL_SERVER_PASSWORD");
	// writeProperties("first", "hello world");
	// System.out.println("�������");
	// }

}
