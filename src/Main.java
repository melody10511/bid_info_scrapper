import org.jdatepicker.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.BevelBorder;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class Main extends JFrame {
	
	private static Logger logger = Logger.getGlobal();
	
	JMenuBar menuBar;
	JButton settingMenu;
	JTabbedPane menuTabs;
	JComponent dataPanel;
	JComponent negoPanel;
	JComponent sortPanel;
	JComponent unsortPanel;
	JComponent updatePanel;
	
	public Main() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException {
		super();
		
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
		    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		}
		
		menuBar = new JMenuBar();
		settingMenu = new JButton("설정");
		settingMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OptionFrame of = new OptionFrame();
			}
		});
		menuBar.add(settingMenu);
		
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		
		d.setSize(d.getWidth() / 3, 100);
		menuTabs = new JTabbedPane();
		
		dataPanel = new DataPanel();
		negoPanel = new NegoPanel();
		sortPanel = new SortedPanel();
		unsortPanel = new UnsortedPanel();
		updatePanel = new UpdatePanel();
		
		menuTabs.addTab("데이터 조회", dataPanel);
		menuTabs.addTab("협상건 조회", negoPanel);
		menuTabs.addTab("분류건 조회", sortPanel);
		menuTabs.addTab("미분류건 조회", unsortPanel);
		menuTabs.addTab("업데이트 센터", updatePanel);
		
		d = Toolkit.getDefaultToolkit().getScreenSize();
		d.setSize(d.getWidth(), d.getHeight() - 50);
		this.setSize(d);
		this.setLayout(new GridLayout(1, 1));
		this.setTitle("입찰정보");
		this.setJMenuBar(menuBar);
		this.add(menuTabs);
		this.setVisible(true);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException {
		//Disable unnecessary error logs.
    	java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);
		
    	try {
    		FileHandler fh = new FileHandler("mylog.txt");
    		fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.WARNING);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
    	
    	Resources.initialize();
		Main m = new Main();
	}
}
