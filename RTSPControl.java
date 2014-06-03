package com.bublcam.rtsptest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

// Test application that communicates with 
public class RTSPControl {
	// RTP Variables
	
	// UDP packet received from server
	private DatagramPacket rcvdp = null;
	// Socket used to send and receive UDP packets
	private DatagramSocket RTPsocket = null;
	// Port where client will receive the RTP packets
	private int RTP_RCV_PORT = 9000;
	
	// Timer used to receive data from the UDP socket
	Timer timer = null;
	
	// Buffer to receive the data
	byte[] buf = null;
	
	// RTSP Variables
	// State variables
	enum RTSPState {
		INIT,
		READY,
		PLAYING
	};
	
	// Request variables
	enum RTSPRequest {
		SETUP,
		DESCRIBE,
		PLAY,
		PAUSE, 
		TEARDOWN,
		OPTIONS
	};
		
	// Current state of the client
	private RTSPState state = null;
	
	// Socket used to send/receive RTSP messages
	private Socket RTSPSocket = null;
	
	// Input and Output stream filters
	// Used to write RTSP messages and send to server
	private BufferedReader RTSPBufferedReader = null;
	
	// Used to receive RTSP messages and data
	private BufferedWriter RTSPBufferedWriter = null;
	
	// Video file / Location to play
	private String videoFile = null;
	
	// Sequence number of RTSP messages within the session
	// Initially set to zero
	private int RTSPSeqNb = 0;
	private int RTSPSessionID = 0; // ID of RTSP sessions - given by RTSP server
	
	// Carriage return and new line to send to server
	private final String CRLF = "\r\n";
	
	// Store server port to communicate with server
	// Usually 554 for RTSP
	private int serverPort = 554;
	
	// Flag to establish that we have set up our parameters
	private boolean isSetup = false;
	
	// Store host name
	private String hostName = null;
	
	// For the audio and video track IDs issued from
	// the server
	private int audioTrackID = -1;
	private int videoTrackID = -1;
	
	private boolean videoSetupDone = false;
	private boolean audioSetupDone = false;
	
	// The type of streaming protocol extracted from the
	// server
	private String streamingProtocolVideo = null;
	private String streamingProtocolAudio = null;
	
	// The total number of tracks
	private int numberOfTracks = 0;
	
	// For our byte to hex conversion routine
	final char[] hexArray = "0123456789ABCDEF".toCharArray();
	// Constructor
	// serverHost - Alphabetical URL or IP Address
	// etc. www.bublcam.com or 192.168.0.100
	// fileName - Exact relevant path and filename to what we want to play
	// Set serverPort to -1 for default port of 554
	public RTSPControl(String serverHost, int serverPort, String fileName) {
		if (fileName == null)
			throw new IllegalArgumentException("RTSPTest: Filename must not be null");

		if (serverHost == null)
			throw new IllegalArgumentException("RTSPTest: Server host must not be null");
		
		if (serverPort < -1 || serverPort > 65535)
			throw new IllegalArgumentException("RTSPTest: Port must be between 0 and 65535");

		// Figure out the server port
		if (serverPort == -1) // default port
			this.serverPort = 554;
		else
			this.serverPort = serverPort;
		
		// Store host name
		hostName = new String(serverHost);
		// Store file name
		videoFile = new String(fileName);
		
		// Set to false then set up
		isSetup = false;
		setUpConnectionAndParameters();
	}
	
	// Constructor if given string URL
	public RTSPControl(String rtspURL) {
		
		// Parse out just the URL by itself (and port if applicable)
        String temp = "rtsp://";
        int locOfRtsp = rtspURL.indexOf(temp);
        
        if (locOfRtsp == -1) 
        	throw new IllegalArgumentException("Must give URL that begins with rtsp://");
        
        String parsedURL = rtspURL.substring(locOfRtsp + temp.length());
        this.hostName = parsedURL.substring(0, parsedURL.indexOf("/"));	
        
        // Check to see if there is a port specified.  If there isn't,
        // assume default part
        if (hostName.indexOf(':') != -1)
        	this.serverPort = Integer.parseInt(hostName.substring(hostName.indexOf(':') + 1));
        else
        	this.serverPort = 554;	        
        
        // Get the video file name now
        videoFile = parsedURL.substring(parsedURL.indexOf("/") + 1);
        
		isSetup = false;
		setUpConnectionAndParameters();
	}
	
	public String getRTSPURL() {
		return new String(hostName);
	}
	
	public int getServerPort() {
		return this.serverPort;
	}
	
	public String getVideoFilename() {
		return new String(videoFile);
	}
	
	
	// Initialize TCP connection with server to exchange RTSP messages
	private void setUpConnectionAndParameters() {
		try {
			//System.out.println("Establishing TCP Connection to: " + hostName + " at Port: " + serverPort);
			InetAddress ServerIPAddr = InetAddress.getByName(hostName);
			RTSPSocket = new Socket(ServerIPAddr, serverPort);
		} catch (UnknownHostException e) {
			System.out.println("Could not find host");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Could not establish socket");
			e.printStackTrace();
		}
		
		// Set up buffers to read from and write to server
		try {
			//System.out.println("Set up read buffer");
			RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPSocket.getInputStream()));
		} catch (IOException e) {
			System.out.println("Could not create buffer to read from server");
			e.printStackTrace();
		}
		
		try {
			//System.out.println("Set up write buffer");
			RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPSocket.getOutputStream()));
		} catch (IOException e) {
			System.out.println("Could not create buffer to write to server");
			e.printStackTrace();
		}
		
		// Initialize state
		state = RTSPState.INIT;
				
		// Initialize buffer to capture enough bytes from the server
		buf = new byte[300000];
		
		// Initialize parameters
		audioTrackID = -1;
		videoTrackID = -1;
		RTSPSessionID = -1;
		numberOfTracks = 0;	
		isSetup = true;
		RTPsocket = null;
		videoSetupDone = false;
		videoSetupDone = false;
	}
	
	// Integer code - 0 for no more setup calls required
	// >= 1 - An additional setup is required
	// -1 - Invalid server response
	public int RTSPSetup() {
		if (state != RTSPState.INIT) {
			System.out.println("Client is already set up or is playing content");
			return 0; // We have already set up or are playing
		}
		
		if (numberOfTracks == 0) {
			System.out.println("No tracks to set up!");
			return 0;
		}
				
		// Increment RTSP sequence number
		RTSPSeqNb++;
		
		// Send SETUP message to server
		sendRTSPRequest(RTSPRequest.SETUP);
		
		// Wait for response code from server
		if (parseServerResponse() != 200) {
			System.out.println("Invalid Server Response");
			return -1;
		}
		else {			
			// Check to see what track IDs we have
			// If we have both, then we need to wait for another
			// setup call			
			// We aren't ready until we set up all of our
			// tracks
			if (numberOfTracks == 0) {
				// Change current RTSP state to READY
				System.out.println("Client is ready");
				state = RTSPState.READY;
				
				// Also establish our socket connection to the server
				if (RTPsocket == null) {
					try {				
						RTPsocket = new DatagramSocket(RTP_RCV_PORT);
						
						// Set timeout value to 100 msec
						// Disable for now
						//RTPsocket.setSoTimeout(100);
					}
					
					// Catch exception here
					catch (SocketException se) {
						System.out.println("Socket Exception: " + se);
						System.exit(0);
					}
				}
			}
			return numberOfTracks;
		}
	}
	
	public void RTSPPlay() {
		if (state != RTSPState.READY) {
			System.out.println("Client has not sent Setup Request yet");
			return;
		}
		
		// Increase the RTSP sequence number - This is the
		// next command to issue
		RTSPSeqNb++;
		
		// Send PLAY message to server
		sendRTSPRequest(RTSPRequest.PLAY);
		
		// Wait for response
		if (parseServerResponse() != 200)
			System.out.println("Invalid Server Response");
		else {
			System.out.println("Starting playback - Starting Timer event");
			// Set to play state
			state = RTSPState.PLAYING;
			
			if (RTPsocket != null) {				
				try {
					if (RTPsocket.isClosed())
						RTPsocket = new DatagramSocket(RTP_RCV_PORT);
				} catch (SocketException e) {
					System.out.println("Could not reconnect to socket");
					e.printStackTrace();
				}
			}
			
			// Initialize timer to receive events from server
			timer = new Timer();
			// Start it now, with 20 millisecond intervals
			timer.scheduleAtFixedRate(new RTSPTimerTask(), 0, 20);			
		}
	}
	
	public void RTSPPause() {
		if (state != RTSPState.PLAYING) {
			System.out.println("Client is not playing content right now");
			return;
		}
		
		// Increase the RTSP sequence number
		RTSPSeqNb++;
		
		// Send PAUSE message to server
		sendRTSPRequest(RTSPRequest.PAUSE);
		
		// Wait for response
		if (parseServerResponse() != 200)
			System.out.println("Invalid Server Response");
		else {
			state = RTSPState.READY;
			System.out.println("Pausing playback - Cancelling Timer event");
			timer.cancel();
		}
	}
	
	public void RTSPTeardown() {
		if (state == RTSPState.INIT) {
			System.out.println("Client is in initialize stage - No need to teardown");
			return;
		}
		
		// Increase RTSP Sequence number
		RTSPSeqNb++;
		
		// Send TEARDOWN message to the server
		sendRTSPRequest(RTSPRequest.TEARDOWN);
		
		// Wait for server response
		if (parseServerResponse() != 200)
			System.out.println("Invalid Server Response");
		else {
			System.out.println("Teardown - Changing Client state back to INIT");
			state = RTSPState.INIT;
			
			// Stop the timer event as well
			if (timer != null) {
				timer.cancel();	
				timer = null;
			}
			
			// Reset sequence number
			RTSPSeqNb = 0;
			
			// Reset all other parameters
			audioTrackID = -1;
			videoTrackID = -1;
			RTSPSessionID = -1;
			numberOfTracks = 0;		
			isSetup = false;
			buf = null;
			if (!RTPsocket.isClosed())
				RTPsocket.close();
			RTPsocket = null;
			videoSetupDone = false;
			audioSetupDone = false;
		}			
	}
	
	// Send a request to see what the options are
	public void RTSPOptions() {
		if (state != RTSPState.INIT) {
			System.out.println("Must be in INIT stage before requesting Options");
			return;
		}
		
		if (!isSetup)
			setUpConnectionAndParameters();
		
		// Increase RTSP Sequence Number
		RTSPSeqNb++;
		
		// Send OPTIONS message to the server
		sendRTSPRequest(RTSPRequest.OPTIONS);
		
		// Wait for server response
		if (parseServerResponse() != 200)
			System.out.println("Invalid Server Response");
		else {
			System.out.println("Options Request succeeded");
			// We don't need to change the state
		}
	}
	
	// Send a request for DESCRIBING what is available
	public void RTSPDescribe() {
		if (state != RTSPState.INIT) {
			System.out.println("Must be in INIT stage before requesting DESCRIBE");
			return;
		}
		
		if (!isSetup)
			setUpConnectionAndParameters();
		
		// Increase RTSP Sequence Number
		RTSPSeqNb++;
		
		// Send OPTIONS message to the server
		sendRTSPRequest(RTSPRequest.DESCRIBE);
		
		// Wait for server response
		if (parseServerResponse() != 200)
			System.out.println("Invalid Server Response");
		else {
			System.out.println("Describe Request succeeded");
			// We don't need to change the state
		}
	}
	
	// Goes through the received string from the server
	// Also allows us to parse through and see if there is track
	// information and the type of transport this server supports
	public int parseServerResponse() {
		int replyCode = 0;
		
		try {
			// Read first line - Status line.  If all goes well
			// this should give us: RTSP/1.0 200 OK
			String statusLine = RTSPBufferedReader.readLine();
			
			if (statusLine == null) {
				System.out.println("Could not communiate with server");
				return -1;
			}
			
			System.out.println(statusLine);
			
			// Tokenize the string and grab the next token, which is
			// the status code
			StringTokenizer tokens = new StringTokenizer(statusLine, " \t\n\r\f");
			tokens.nextToken(); // Gives us RTSP/1.0
			// Give us reply code
			replyCode = Integer.parseInt(tokens.nextToken());
			System.out.println("*** Reply Code: " + replyCode);
			// If the reply code is 200, then we are solid
			if (replyCode == 200) { // begin if
				// Cycle through the rest of the lines, delimited by \n
				// and print to the screen.  Also grab the relevant information
				// we need
				//for (String line = RTSPBufferedReader.readLine(); line != null; 
				//		line = RTSPBufferedReader.readLine()) { // begin for
				
				// NEW: First we need to wait to see if we are ready to read
				String line;
				try {
					while (!RTSPBufferedReader.ready())
						continue;
				}
				catch(IOException e) {
					System.out.println("Could not read from read buffer");
					//e.printStackTrace();
					return -1;
				}
				
				// If we are, while there is still data in the buffer...
				while (RTSPBufferedReader.ready()) { // begin for
					line = RTSPBufferedReader.readLine();
					if (line == null) // Also check to see if we have reached the end
						break;		  // of the stream
					
					System.out.println(line);
					// Tokenize
					// Also includes semi-colons
					tokens = new StringTokenizer(line, " \t\n\r\f;:");
					
					// Now go through each token
					while (tokens.hasMoreTokens()) { // begin while1
						// Grab token
						String part = tokens.nextToken();
						
						// When we are using DESCRIBE - We are looking for the
						// track numbers - This will allow us to request
						// that particular stream of data so we can pipe it
						// into our decoder buffers
						
						// Usually looks like this:
						// m=audio num RTP/AVP num2
						// or
						// m=video num RTP/AVP num2
						if (part.equals("m=audio") && audioTrackID == -1) { // begin if2
							// Skip next token
							tokens.nextToken();
							streamingProtocolAudio = new String(tokens.nextToken());
							
							// Now advance each line until we hit "a=control"
							while (true) { // begin while2
								line = RTSPBufferedReader.readLine();
								System.out.println(line);
								if (line.indexOf("a=control") != -1 || line == null)
									break;
							} // end while2
							
							if (line == null) { // begin if3
								System.out.println("Could not find a=control String");
								return replyCode;
							} // end if3
							
							// Once we hit "a=control", get the track number
							StringTokenizer controlTokens = new StringTokenizer(line, " \t\n\r\f;:");
							// Skip over a=control
							controlTokens.nextToken();
							// This should now contain our trackID
							String trackID = controlTokens.nextToken();
							this.audioTrackID = Integer.parseInt(trackID.substring(8, 9));	
							System.out.println("*** Audio Track: " + audioTrackID);
							numberOfTracks++;
							
							// Break out of this loop and continue reading the other lines
							break;
						} // end if2
						else if (part.equals("m=video") && videoTrackID == -1) { // begin if2
							// Skip next token
							tokens.nextToken();
							streamingProtocolVideo = new String(tokens.nextToken());
							
							// Now advance each line until we hit "a=control"
							while (true) { // begin while2
								line = RTSPBufferedReader.readLine();
								System.out.println(line);
								if (line.indexOf("a=control") != -1 || line == null)
									break;
							} // end while2
							
							if (line == null) { // begin if3
								System.out.println("Could not find a=control String");
								return replyCode;
							} // end if3
							
							// Once we hit "a=control", get the track number
							StringTokenizer controlTokens = new StringTokenizer(line, " \t\n\r\f;:");
							// Skip over a=control
							controlTokens.nextToken();
							// This should now contain our trackID
							String trackID = controlTokens.nextToken();
							this.videoTrackID = Integer.parseInt(trackID.substring(8, 9));
							System.out.println("*** Video Track: " + videoTrackID);
							numberOfTracks++;
							
							// Break out of this loop and continue reading other lines
							break;
						} // end if2
						
						// Extract the Session ID for subsequent setups
						else if (part.equals("Session") && RTSPSessionID == -1) { // begin if2
							this.RTSPSessionID = Integer.parseInt(tokens.nextToken());
							System.out.println("*** Session ID: " + RTSPSessionID);							
							// Break out of this loop and continue reading other lines
							break;
						} // end if2
					} // end while1	
				} // end for
			} // end if
		} catch (IOException e) {
			System.out.println("Could not read in string from buffer");
			e.printStackTrace();
		}		
		return replyCode;
	}
	
	public void sendRTSPRequest(RTSPRequest request) {
		// Set up base String
		String requestType;
		StringBuilder stringToSend = new StringBuilder();
		switch (request) {
		case SETUP:
			requestType = new String("SETUP");
			break;
		case DESCRIBE:
			requestType = new String("DESCRIBE");
			break;
		case PLAY:
			requestType = new String("PLAY");
			break;
		case TEARDOWN:
			requestType = new String("TEARDOWN");
			break;
		case PAUSE:
			requestType = new String("PAUSE");
			break;
		case OPTIONS:
			requestType = new String("OPTIONS");
			break;
		default:
			throw new IllegalArgumentException("Invalid request type");			
		}
		
		// This handles actual strings we are going to send to the server
		switch(request) {
		case SETUP:
			// We should set up the video track first
			if (videoSetupDone && audioSetupDone)
				System.out.println("Setup already established");
			else {
				try {
					if (videoTrackID != -1 && !videoSetupDone) {
						System.out.println("*** Setting up Video Track: ");
						stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort + 
								"/" + videoFile + "/trackID=" + videoTrackID + " RTSP/1.0" + CRLF);
						stringToSend.append("CSeq: " + RTSPSeqNb + CRLF);
						
						if (RTSPSessionID != -1)
							stringToSend.append("Session: " + RTSPSessionID + CRLF);
						
						stringToSend.append("Transport: " + streamingProtocolVideo + ";unicast;client_port=" + 
								RTP_RCV_PORT + "-" + (RTP_RCV_PORT+1) + CRLF + CRLF);
						RTSPBufferedWriter.write(stringToSend.toString());
						System.out.println(stringToSend.toString());
						RTSPBufferedWriter.flush();
						numberOfTracks--;
						videoSetupDone = true;
					}
					
					// After, set up the audio track
					else if (audioTrackID != -1 && !audioSetupDone) {
						System.out.println("*** Setting up Audio Track: ");
						stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort + 
								"/" + videoFile + "/trackID=" + audioTrackID + " RTSP/1.0" + CRLF);
						stringToSend.append("CSeq: " + RTSPSeqNb + CRLF);
						
						if (RTSPSessionID != -1)
							stringToSend.append("Session: " + RTSPSessionID + CRLF);	
						
						stringToSend.append("Transport: " + streamingProtocolAudio + ";unicast;client_port=" + 
								RTP_RCV_PORT + "-" + (RTP_RCV_PORT+1) + CRLF + CRLF);
						RTSPBufferedWriter.write(stringToSend.toString());
						System.out.println(stringToSend.toString());
						RTSPBufferedWriter.flush();
						numberOfTracks--;
						audioSetupDone = true;
					}
				} catch(IOException e) {
					System.out.println("Could not write to write buffer");
					e.printStackTrace();
				}
			}
			break;
		case DESCRIBE: // Make sure we call DESCRIBE first
		case PLAY: // Case when we wish to issue a PLAY request
		case PAUSE: // Same for PAUSE and TEARDOWN
		case TEARDOWN: // Also same for OPTIONS
		case OPTIONS:
			try {
				stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort + 
						"/" + videoFile + " RTSP/1.0" + CRLF);
				// Send sequence number
				// If there is no session ID, this is the last thing we send 
				if (RTSPSessionID == -1)
					stringToSend.append("CSeq: " + RTSPSeqNb + CRLF + CRLF);
				// Send session number if applicable
				else {
					stringToSend.append("CSeq: " + RTSPSeqNb + CRLF);
					stringToSend.append("Session: " + RTSPSessionID + CRLF + CRLF);
				}
				RTSPBufferedWriter.write(stringToSend.toString());
				System.out.println(stringToSend.toString());
				RTSPBufferedWriter.flush();
			}
			catch (IOException e) {
				System.out.println("Could not write to write buffer");
				e.printStackTrace();
			}
			break;
		default:
			throw new RuntimeException("Invalid Client State");
		}
 	}
	
	// Task that gets invoked after every 20 msec
	private class RTSPTimerTask extends TimerTask {

		@Override
		// Every 20 seconds, read from the socket connection		
		public void run() {
			// Construct a DatagramPacket
			rcvdp = new DatagramPacket(buf, buf.length);

			try {
				// Receive the data
				if (!RTPsocket.isClosed()) {
					RTPsocket.receive(rcvdp);
				
					byte[] data = rcvdp.getData();
					int length = rcvdp.getLength();
					System.out.println("Data Received: ");
				
					// Print out its length
					System.out.println("Length of data: " + length);
				
					// Print the data itself
					System.out.println("Data: ");
					String hexChars = bytesToHex(data, length);
					System.out.println(hexChars);
				}
			} catch (SocketException se) { // We need to catch here if we decide to invoke TEARDOWN
										   // while we are waiting for data coming from the DatagramSocket
				System.out.println("Socket connection closed");
				this.cancel();
			}
			catch (IOException e) {
				System.out.println("Could not read from socket");
				e.printStackTrace();
				this.cancel();
				return;
			}			
		}		
	}
	
	// Convenience method to convert a byte array into a string
	
	//private String bytesToHex(byte[] bytes) {
	//	return bytesToHex(bytes, bytes.length);
	//}
	
	private String bytesToHex(byte[] bytes, int length) {
	    char[] hexChars = new char[length * 2];
	    for ( int j = 0; j < length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
