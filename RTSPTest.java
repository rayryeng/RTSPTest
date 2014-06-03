package com.bublcam.rtsptest;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class RTSPTest implements ActionListener{
	// GUI
	private JFrame mainFrame;
	private JButton setupButton;
	private JButton playButton;
	private JButton pauseButton;
	private JButton optionsButton;
	private JButton describeButton;	
	private JButton teardownButton;
	private JTextField textField;
	
	// Panel to place our push buttons
	private JPanel buttonPanel;
	
	// Panel to place our text field panel
	private JPanel textPanel;
	
	// Panel to place all of our objects in one
	private JPanel mainPanel;
	
	// Our RTSP Object
	private RTSPControl rtspControl; 
	
	private String hostName;
	private int portNumber;
	private String videoFile;
		
	public RTSPTest() {
		// Set up our GUI elements
		mainFrame = new JFrame("RTSP Test");
		
		setupButton = new JButton("Setup");
		playButton = new JButton("Play");
		pauseButton = new JButton("Pause");
		optionsButton = new JButton("Options");
		describeButton = new JButton("Describe");
		teardownButton = new JButton("Teardown");
		textField = new JTextField("rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov");
		
		buttonPanel = new JPanel();
		textPanel = new JPanel();
		mainPanel = new JPanel();
		
		// Set up buttons
		buttonPanel.setLayout(new GridLayout(1,0));
		buttonPanel.add(describeButton);
		buttonPanel.add(optionsButton);
		buttonPanel.add(setupButton);
		buttonPanel.add(playButton);
		buttonPanel.add(pauseButton);
		buttonPanel.add(teardownButton);
		
		setupButton.addActionListener(this);
		playButton.addActionListener(this);
		pauseButton.addActionListener(this);
		optionsButton.addActionListener(this);
		describeButton.addActionListener(this);
		teardownButton.addActionListener(this);
		textField.addActionListener(this);		
		
		// Set up text field
		textPanel.setLayout(new GridLayout(1,0));		
		textPanel.add(textField);
		
		// Set main panel
		mainPanel.setLayout(null);
		mainPanel.add(textPanel);
		mainPanel.add(buttonPanel);
		textPanel.setBounds(0, 0, 450, 25);
		buttonPanel.setBounds(0, 25, 450, 50);
		
		// Add listener so that we quit we close the window
	    mainFrame.addWindowListener(new WindowAdapter() {
	        public void windowClosing(WindowEvent e) {
	        	// Teardown the connection when we close the window
	        	if (rtspControl != null)
	        		rtspControl.RTSPTeardown();
	        	System.exit(0);
	        }
	     });	

		// Set up connection to default RTSP url before we even show GUI
		hostName = "184.72.239.149";
		portNumber = 554;
		videoFile = "vod/mp4:BigBuckBunny_115k.mov";
		rtspControl = new RTSPControl(hostName, portNumber, videoFile);
	    	    
		// Create frame and show
		mainFrame.getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainFrame.setSize(new Dimension(450, 100));
		mainFrame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == setupButton) {
			System.out.println("Setup Button Pressed!");
			if (rtspControl == null) {
				System.out.println("RTSP Object has not been created!");
				return;
			}
			// Keep issuing setup commands as long as there are tracks
			// to set up
			while (rtspControl.RTSPSetup() > 0); 
			//int numberTracksLeft = rtspControl.RTSPSetup();
			//if (numberTracksLeft > 0)
			//	System.out.println("There are " + numberTracksLeft + " tracks left to set up");
		}
		else if (e.getSource() == describeButton) {
			System.out.println("Describe Button Pressed!");			
			if (rtspControl == null) {
				System.out.println("RTSP Object has not been created!");
				return;
			}
			rtspControl.RTSPDescribe();
		}
		else if (e.getSource() == optionsButton) {
			System.out.println("Options Button Pressed!");
			if (rtspControl == null) {
				System.out.println("RTSP Object has not been created!");
				return;
			}
			rtspControl.RTSPOptions();
		}
		else if (e.getSource() == playButton) {
			System.out.println("Play Button Pressed!");
			if (rtspControl == null) {
				System.out.println("RTSP Object has not been created!");
				return;
			}
			rtspControl.RTSPPlay();
		}
		else if (e.getSource() == pauseButton) {
			System.out.println("Pause Button Pressed!");
			if (rtspControl == null) {
				System.out.println("RTSP Object has not been created!");
				return;
			}
			rtspControl.RTSPPause();
		}
		else if (e.getSource() == teardownButton) {
			System.out.println("Teardown Button Pressed!");
			if (rtspControl == null) {
				System.out.println("RTSP Object has not been created!");
				return;
			}
			rtspControl.RTSPTeardown();
		}
		else if (e.getSource() == textField) {
			// Grab the text from the field
			String rtspURL = textField.getText();
			System.out.println("String entered in text field: " + rtspURL);
			
			rtspControl = new RTSPControl(rtspURL);
			
			hostName = rtspControl.getRTSPURL();
			System.out.println("URL: " + hostName);
			portNumber = rtspControl.getServerPort();
			System.out.println("Port Number: " + portNumber);
			videoFile = rtspControl.getVideoFilename();
			System.out.println("Video Filename: " + videoFile);
		}
	}
	
	public static void main(String[] args) {
		RTSPTest rtspTest = new RTSPTest();
	}
}
