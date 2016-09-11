import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

public class MonthCheckFrame extends JFrame {
	String[] columns = { "개찰일시", "사이트", "데이터베이스", "차수" };
	
	// For SQL setup.
	Connection con;
	java.sql.Statement st;
	ResultSet rs;
	
	ExecutorService es;
	Timer stateCheck;
	Timer auto;
	ArrayList<Future> states;
	
	boolean automode;
	boolean running;
	ArrayList<String> months;
	
	JCheckBox autoCheck;
	DatePicker startDate;
	DatePicker endDate;
	JTextArea reps;
	JTable table;
	String site;
	JButton toggle;
	
	public MonthCheckFrame(String site) {
		super(site + " 월별조회");
		this.site = site;
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		states = new ArrayList<Future>();
		stateCheck = new Timer();
		stateCheck.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (running) {
					boolean done = true;
					for (int i = 0; i < states.size(); i++) {
						Future f = states.get(i);
						if (!f.isDone()) done = false;
						System.out.println(f.isDone());
					}
					if (done || states.size() == 0) {
						states.clear();
						toggle.doClick();
					}
				}
			}
		}, 2000, 2000);
		
		auto = new Timer();

		automode = false;
		running = false;
		months = new ArrayList<String>();
		
		autoCheck = new JCheckBox("자동");
		startDate = new JDatePicker(Calendar.getInstance().getTime());
		startDate.setTextfieldColumns(12);
		startDate.setTextEditable(true);
		endDate = new JDatePicker(Calendar.getInstance().getTime());
		endDate.setTextfieldColumns(12);
		endDate.setTextEditable(true);
		reps = new JTextArea(1, 5);
		reps.setText("60");
		toggle = new JButton("시작");
		toggle.addActionListener(new UpdateListener());
		
		JPanel datePanel = new JPanel();
		datePanel.setLayout(new BoxLayout(datePanel, BoxLayout.PAGE_AXIS));
		
		JPanel sdPanel = new JPanel();
		sdPanel.add(new JLabel("시작일자 : "));
		sdPanel.add((JComponent) startDate);
		JPanel edPanel = new JPanel();
		edPanel.add(new JLabel("종료일자 : "));
		edPanel.add((JComponent) endDate);
		JPanel rePanel = new JPanel();
		rePanel.add(new JLabel("조회간격 : "));
		rePanel.add(reps);
		rePanel.add(new JLabel("초"));
		
		datePanel.add(sdPanel);
		datePanel.add(edPanel);
		datePanel.add(rePanel);
		datePanel.add(toggle);
		
		table = new JTable(new DefaultTableModel(columns, 0));
		JScrollPane scroll = new JScrollPane(table);
		
		panel.add(autoCheck, BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		panel.add(datePanel, BorderLayout.SOUTH);
		
		this.add(panel);
		this.setSize(300, 410);
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}
	
	private class UpdateListener implements ActionListener {
		public ArrayList<String> queryByMonth(String sd, String ed) throws ParseException {
			SimpleDateFormat forq = new SimpleDateFormat("yyyy-MM");
			Calendar sdate = Calendar.getInstance(); 
			sdate.setTime(forq.parse(sd));
			Calendar edate = Calendar.getInstance();
			edate.setTime(forq.parse(ed));
			ArrayList<String> dates = new ArrayList<String>();
			do {
				dates.add(forq.format(sdate.getTime()));
				sdate.add(Calendar.MONTH, 1);
				if (sdate.equals(edate)) {
					dates.add(forq.format(sdate.getTime()));
				}
			} while (edate.after(sdate) && dates.size() < 12);
			return dates;
		}
		
		public void actionPerformed(ActionEvent e) {
			if (!running) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				DefaultTableModel tm = (DefaultTableModel) table.getModel();
				String sd = "";
				String ed = "";
				es = Executors.newFixedThreadPool(12);
				
				if ((startDate.getModel().getValue() == null) || (endDate.getModel().getValue() == null)) {					
					JOptionPane.showMessageDialog(null, "날짜를 설정해주십시오.");
					return;
				}
				else {
					sd = sdf.format(startDate.getModel().getValue());
					ed = sdf.format(endDate.getModel().getValue());
				}
				
				if (Resources.isInteger(reps.getText()) && !reps.equals("0")) {
					int rep = Integer.parseInt(reps.getText());
					if (!automode && autoCheck.isSelected()) {
						auto.scheduleAtFixedRate(new TimerTask() {
							public void run() {
								if (!running) {
									automode = true;
									toggle.doClick();
								}
								if (!autoCheck.isSelected()) {
									automode = false;
									auto.cancel();
								}
							}
						}, rep * 1000, rep * 1000);
					}
				}
				
				try {
					months = queryByMonth(sd, ed);
					Class.forName("com.mysql.jdbc.Driver");
					con = DriverManager.getConnection("jdbc:mysql://localhost/bid_db_2?useUnicode=true&characterEncoding=euckr", Resources.DB_ID, Resources.DB_PW);
					st = con.createStatement();
					rs = null;
					
					tm.setRowCount(0); // Empty the table.
					if (site.equals("나라장터")) {
						for (String sm : months) {
							NaraParser parser = new NaraParser(sd, ed, "결과");
							
							int dbcount = 0;
							int svcount = 0;
							sm += "-01";
							Calendar sc = Calendar.getInstance();
							sc.setTime(sdf.parse(sm));
							sc.add(Calendar.MONTH, 1);
							sc.add(Calendar.DAY_OF_MONTH, -1);
							String em = sdf.format(sc.getTime());
							parser.setDate(sm, em);
							
							rs = st.executeQuery("SELECT COUNT(*) FROM narabidinfo WHERE 예정개찰일시 BETWEEN \""+sm+" 00:00:00\" AND \""+em+" 23:59:59\" AND 목록=1;");
							if (rs.next()) {
								dbcount = rs.getInt(1);
							}
							
							svcount = parser.getTotal();
							
							sm = sm.substring(0, sm.length()-3);
							int diff = svcount - dbcount;
							tm.addRow(new Object[] { sm, svcount, dbcount, diff });
							
							if (diff > 0) {
								Future f = es.submit(parser);
								states.add(f);
							}
						}
					}
					else if (site.equals("국방조달청")) {
						for (String sm : months) {
							DapaParser parser = new DapaParser(sd, ed, "", null);
							
							int dbcount = 0;
							int svcount = 0;
							sm += "-01";
							Calendar sc = Calendar.getInstance();
							sc.setTime(sdf.parse(sm));
							sc.add(Calendar.MONTH, 1);
							sc.add(Calendar.DAY_OF_MONTH, -1);
							String em = sdf.format(sc.getTime());
							parser.setDate(sm, em);
							parser.setOption("협상결과");
							
							rs = st.executeQuery("SELECT COUNT(*) FROM dapabidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;");
							if (rs.next()) {
								dbcount = rs.getInt(1);
							}
							rs = st.executeQuery("SELECT COUNT(*) FROM dapanegoinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;");
							if (rs.next()) {
								dbcount += rs.getInt(1);
							}
							
							svcount = parser.getTotal();
							
							sm = sm.substring(0, sm.length()-3);
							int diff = svcount - dbcount;
							tm.addRow(new Object[] { sm, svcount, dbcount, diff });
							
							if (diff > 0) {
								Future f = es.submit(parser);
								states.add(f);
							}
						}
					}
					else if (site.equals("한국마사회")) {
						for (String sm : months) {
							LetsParser parser = new LetsParser(sd, ed, "");
							
							int dbcount = 0;
							int svcount = 0;
							sm += "-01";
							Calendar sc = Calendar.getInstance();
							sc.setTime(sdf.parse(sm));
							sc.add(Calendar.MONTH, 1);
							sc.add(Calendar.DAY_OF_MONTH, -1);
							String em = sdf.format(sc.getTime());
							parser.setDate(sm, em);
							parser.setOption("결과");
							
							rs = st.executeQuery("SELECT COUNT(*) FROM letsrunbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;");
							if (rs.next()) {
								dbcount = rs.getInt(1);
							}
							svcount = parser.getTotal();
							
							sm = sm.substring(0, sm.length()-3);
							int diff = svcount - dbcount;
							tm.addRow(new Object[] { sm, svcount, dbcount, diff });
							
							if (diff > 0) {
								Future f = es.submit(parser);
								states.add(f);
							}
						}
					}
					else if (site.equals("도로공사")) {
						for (String sm : months) {
							ExParser parser = new ExParser(sd, ed, "");
							
							int dbcount = 0;
							int svcount = 0;
							sm += "-01";
							Calendar sc = Calendar.getInstance();
							sc.setTime(sdf.parse(sm));
							sc.add(Calendar.MONTH, 1);
							sc.add(Calendar.DAY_OF_MONTH, -1);
							String em = sdf.format(sc.getTime());
							parser.setDate(sm, em);
							
							rs = st.executeQuery("SELECT COUNT(*) FROM exbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;");
							if (rs.next()) {
								dbcount = rs.getInt(1);
							}
							svcount = parser.getTotal();
							
							sm = sm.substring(0, sm.length()-3);
							int diff = svcount - dbcount;
							tm.addRow(new Object[] { sm, svcount, dbcount, diff });
							
							if (diff > 0) {
								Future f = es.submit(parser);
								states.add(f);
							}
						}
					}
				} catch (ClassNotFoundException | SQLException | ParseException | IOException e1) {
					e1.printStackTrace();
				}
				
				running = true;
				toggle.setText("중지");
			}
			else if (running) {
				states.clear();
				es.shutdownNow();
				toggle.setText("시작");
				running = false;
			}
		}
	}
}