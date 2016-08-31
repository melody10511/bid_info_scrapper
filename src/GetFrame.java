import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

public class GetFrame extends JFrame {
	ExecutorService es;
	Future state;
	Timer stateCheck;
	Timer auto;
	Parser parser;
	GetFrame f;
	
	JCheckBox autoCheck;
	DatePicker startDate;
	DatePicker endDate;
	JTextArea reps;
	JLabel count;
	JLabel code;
	JButton toggle;

	boolean automode;
	boolean running;
	String totalCount;
	String curCount;
	String site;
	
	public GetFrame(String site) {
		
		super(site + " 업데이트");
		this.site = site;
		f = this;
		
		stateCheck = new Timer();
		stateCheck.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (running) {
					boolean done = true;
					
					if (!state.isDone()) done = false;
					
					if (done) {
						toggle.doClick();
					}
				}
			}
		}, 2000, 2000);
		
		auto = new Timer();
		
		running = false;
		automode = false;
		totalCount = "0";
		curCount = "0";
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
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
		count = new JLabel(curCount + " / " + totalCount);
		code = new JLabel("-");
		
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
		JPanel coPanel = new JPanel();
		coPanel.add(count);
		JPanel codePanel = new JPanel();
		codePanel.add(code);
		
		datePanel.add(sdPanel);
		datePanel.add(edPanel);
		datePanel.add(rePanel);
		datePanel.add(coPanel);
		datePanel.add(codePanel);
		
		panel.add(autoCheck, BorderLayout.NORTH);
		panel.add(datePanel, BorderLayout.CENTER);
		panel.add(toggle, BorderLayout.SOUTH);
		
		this.add(panel);
		this.setSize(250, 250);
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}
	
	public void updateInfo(String c, boolean cur) {
		code.setText(c);
		if (cur) {
			int ind = Integer.parseInt(curCount);
			ind++;
			curCount = ind + "";
			count.setText(curCount + " / " + totalCount);
		}
		
		this.repaint();
	}
	
	private class UpdateListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (!running) {
				int repSec = 0;
				String sd = "";
				String ed = "";
				es = Executors.newFixedThreadPool(1);
				curCount = "0";
				
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
				
				if ((startDate.getModel().getValue() == null) || (endDate.getModel().getValue() == null)) {					
					JOptionPane.showMessageDialog(null, "날짜를 설정해주십시오.");
					return;
				}
				else {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					sd = sdf.format(startDate.getModel().getValue());
					ed = sdf.format(endDate.getModel().getValue());
				}
				
				try {
					if (site.equals("나라장터")) {
						parser = new NaraParser(sd, ed, "");
					}
					else if (site.equals("국방조달청")) {
						parser = new DapaParser(sd, ed, "", f);
					}
					else if (site.equals("한국마사회")) {
						parser = new LetsParser(sd, ed, "");
					}
					else if (site.equals("LH공사")) {
						sd = sd.replaceAll("-", "/");
						ed = ed.replaceAll("-", "/");
						parser = new LHParser(sd, ed, "");
					}
					else if (site.equals("도로공사")) {
						parser = new ExParser(sd, ed, "");
					}
					
					totalCount = "" + parser.getTotal();
					
					state = es.submit(parser);
					
				} catch (ClassNotFoundException | SQLException | IOException e1) {
					e1.printStackTrace();
				}
				
				count.setText(curCount + " / " + totalCount);
				running = true;
				toggle.setText("중지");
			}
			else if (running) {
				es.shutdownNow();
				running = false;
				toggle.setText("시작");
			}
		}
	}
}
