
package com.lu.main;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 *
 */
public class MainFrame extends JFrame {
	JPanel jp1, jp2, jp3;//
	JLabel jlb1, jlb2, jlb3, jlb4;//
	JButton jb1, jb2, jbtnTran;//
	JTextField jtf;//
	JPasswordField jpf;//


	private String apkPath, strPath;

	/**android:debuggable="true"
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
//		new MainFrame().showUI();
//		String dir = System.getProperty("user.dir");
//		System.out.println(dir.substring(0, dir.lastIndexOf("\\")));
//		d();
//		b();
//		s();
		install();
	}
	static String apkName="veryfitplus";
	static String unsignApk = apkName+"_unsign";
	static String signApk = unsignApk+"_sign";
	private void showFile(boolean isMoban) {
		File directory = new File("");//设定为当前文件夹
		JFileChooser jf = new JFileChooser(directory.getAbsoluteFile());
		jf.showDialog(null, null);
		File fi = jf.getSelectedFile();
		if (null != fi) {
			if (isMoban) {
				apkPath = fi.getAbsolutePath();
				jlb3.setText(fi.getAbsolutePath());
			} else {
				strPath = fi.getAbsolutePath();
				jlb4.setText(strPath);
			}
		}

	}

	private static void d() {
		String dir = System.getProperty("user.dir");
		System.out.println(dir.substring(0, dir.lastIndexOf("\\")));
		File f=new File(dir.substring(0, dir.lastIndexOf("\\")),"\\apktool\\d.bat");
		System.out.println(f.getAbsolutePath());
		System.out.println(f.exists());
		callCmd(f.getAbsolutePath()+" "+apkName);
	}
	private static void b() {
		String dir = System.getProperty("user.dir");
//		System.out.println(dir.substring(0, dir.lastIndexOf("\\")));
		String apkToolDir = dir.substring(0, dir.lastIndexOf("\\"))+File.separator+"apktool";
		File f=new File(apkToolDir,"b.bat");
		System.out.println(f.getAbsolutePath());
		System.out.println(f.exists());
		callCmd(f.getAbsolutePath()+" "+apkName);
		
		File unsignApkFile = new File(apkToolDir,File.separator+apkName+File.separator+"dist"+File.separator+apkName+".apk");
		System.out.println(unsignApkFile.getAbsolutePath());
		File copy = new File(apkToolDir,unsignApk+".apk");
		System.out.println(copy.getAbsolutePath());
		copy.deleteOnExit();
		IOUtil.copyFile(unsignApkFile.getAbsolutePath(), copy.getAbsolutePath());
		System.out.println("b done.........");
	}
	private static void s() {
		String dir = System.getProperty("user.dir");
		System.out.println(dir.substring(0, dir.lastIndexOf("\\")));
		File f=new File(dir.substring(0, dir.lastIndexOf("\\")),"\\apktool\\s.bat");
		System.out.println(f.getAbsolutePath());
		System.out.println(f.exists());
		callCmd(f.getAbsolutePath()+" "+unsignApk);
		
		System.out.println("s done.........");
		
		
	}
	private static void install() {
//		String installCmd = "adb install -r "+signApk+".apk";
		
		String dir = System.getProperty("user.dir");
		String apkToolDir = dir.substring(0, dir.lastIndexOf("\\"))+File.separator+"apktool";
//		callCmd("cd "+apkToolDir);
		File f=new File(apkToolDir+File.separator+"i.bat");
		callCmd(f.getAbsolutePath()+" "+signApk+".apk");
		
		System.out.println("install done.........");
	}
	public static void  callCmd(String locationCmd){
	        try {
	        Process child = Runtime.getRuntime().exec(locationCmd);
	        InputStream in = child.getInputStream();
	        int c;
	        while ((c = in.read()) != -1) {
	            System.out.print((char)c);
	    }
	     in.close();
	     try {
	         child.waitFor();
	     } catch (InterruptedException e) {
	         e.printStackTrace();
	     }
	     System.out.println(" cmd done");
	   } catch (IOException e) {
	         e.printStackTrace();
	   }
	}
	private void showUI() {
		jp1 = new JPanel();
		jp2 = new JPanel();
		jp3 = new JPanel();
		jlb1 = new JLabel("apk文件:");
		jlb1.setSize(100, 150);
		jlb3 = new JLabel("             ");
		jlb4 = new JLabel("             ");
		jlb2 = new JLabel("apk文件:");
		jb1 = new JButton("路径");
		jbtnTran = new JButton("拷贝");
		jb1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showFile(true);
			}
		});
		jb1.setBounds(0, 0, 550, 50);
		jb2 = new JButton("excel文件");
		jb2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showFile(false);
			}
		});
		jbtnTran.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
			}
		});
		jtf = new JTextField(10);
		jpf = new JPasswordField(10);

		this.setLayout(new GridLayout(4, 1));

		jp1.add(jlb1);
		// jp1.add(jtf);
		jp1.add(jlb3);
		jp1.add(jb1);

		jp2.add(jlb2);
		jp2.add(jlb4);
		jp2.add(jb2);

		this.add(jp1);
		this.add(jp2);
		this.add(jp3);
		add(jbtnTran);

		this.setTitle("...");
		this.setSize(500, 300);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);

		this.setResizable(false);
	}

}
