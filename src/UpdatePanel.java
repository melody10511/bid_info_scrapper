import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class UpdatePanel extends JPanel {
	JButton naraButton;
	JButton naraDayButton;
	JButton naraMonthButton;
	JButton dapaButton;
	JButton dapaDayButton;
	JButton dapaMonthButton;
	JButton lhButton;
	//JButton lhDayButton;
	//JButton lhMonthButton;
	JButton runButton;
	JButton runDayButton;
	JButton runMonthButton;
	JButton exButton;
	JButton exDayButton;
	JButton exMonthButton;
	JButton railButton;
	JButton railDayButton;
	JButton railMonthButton;
		
	public UpdatePanel() throws IOException {
		super();
		
		this.setLayout(new GridLayout(0, 3, 20, 20));
		this.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
			
		initializePanel("��������");
		initializePanel("��������û");
		initializePanel("LH����");
		initializePanel("�ѱ�����ȸ");
		initializePanel("���ΰ���");
		initializePanel("ö������");
	}
	
	public void initializePanel(String site) throws IOException {
		String path = "";
		if (site.equals("��������")) path = "./logos/nara.PNG";
		else if (site.equals("��������û")) path = "./logos/dapa.PNG";
		else if (site.equals("LH����")) path = "./logos/lh.GIF";
		else if (site.equals("���ΰ���")) path = "./logos/ex.PNG";
		else if (site.equals("�ѱ�����ȸ")) path = "./logos/letsrun.PNG";
		else if (site.equals("ö������")) path = "./logos/korail.GIF";
		
		BufferedImage logo = ImageIO.read(new File(path));
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setBackground(Color.white);
		JPanel buttonPanel = new JPanel();
		JLabel label = new JLabel(new ImageIcon(logo));
		
		JButton updateButton = new JButton("������Ʈ");
		updateButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
		updateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GetFrame g = new GetFrame(site);
			}
		});
		buttonPanel.add(updateButton);
		if (!site.equals("LH����")) {
			JButton dayButton = new JButton("���ں���ȸ");
			dayButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
			dayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					DayCheckFrame d = new DayCheckFrame(site);
				}
			});
			JButton monthButton = new JButton("������ȸ");
			monthButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
			monthButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					MonthCheckFrame m = new MonthCheckFrame(site);
				}
			});
			
			buttonPanel.add(monthButton);
			buttonPanel.add(dayButton);
		}
		
		mainPanel.add(label, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		mainPanel.setBorder(BorderFactory.createEtchedBorder());
		
		this.add(mainPanel);
	}
}
