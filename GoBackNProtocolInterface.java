

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

public class GoBackNProtocolInterface extends Frame implements ActionListener {
	   private Label hostName;    //Declare a Label component 
	   private TextField hostValue; // Declare a TextField component 
	   private Label senderPort;
	   private TextField senderPortNo;
	   private Label recPort;
	   private TextField recPortNo;
	   private Label sendFile;
	   private TextField sendFileLoc;
	   private Label recieveFile;
	   private TextField recieveFileLoc;
	   private Label packetLoss;
	   private TextField pLoss;
	   TextArea stats;   
	   
	   private Button start;   // Declare a Button component
	 
	   // Constructor to setup GUI components and event handlers
	   public GoBackNProtocolInterface () {
	      setLayout(new FlowLayout());
	 
	      hostName = new Label("Host Name(To which you will send)");  // construct the Label component
	      add(hostName);                    // "super" Frame adds Label
	      hostValue = new TextField("127.0.0.1", 10); // construct the TextField component
	      hostValue.setEditable(true);   // set to read-only
	      add(hostValue);   

	      packetLoss = new Label("Packet loss between [0-99]");
	      add(packetLoss);                
	      pLoss = new TextField("70", 10); 
	      pLoss.setEditable(true);   
	      add(pLoss); 
	      
	     
	      recPort = new Label("Reciever Port No (Receiver thread using port)");
	      add(recPort);
	      recPortNo = new TextField("8889", 7);
	      add(recPortNo);
	      
	      senderPort = new Label("Sender Port No (Sender thread using port)");
	      add(senderPort);
	      senderPortNo = new TextField("8889", 5);
	      add(senderPortNo);
	      
	      sendFile = new Label("Send File");
	      add(sendFile);                   
	      sendFileLoc = new TextField("sample.txt", 50);
	      hostValue.setEditable(true);   
	      add(sendFileLoc); 
	      
	      recieveFile = new Label("Receive File");
	      add(recieveFile);                
	      recieveFileLoc = new TextField("sample.txt", 50); 
	      recieveFileLoc.setEditable(true);   
	      add(recieveFileLoc); 
	      
	      Label statistics = new Label("-----------------------------------Sender Statistic----------------------------------");
		  add(statistics);
		  stats = new TextArea(8, 100);
		  stats.setEditable(false);
	      add(stats);
	      start = new Button("Start"); 
	      add(start);                  
	 
	      start.addActionListener(this);
	      setTitle("Go Back N Protocol"); 
	      setSize(400, 500);        
	      setVisible(true);         
	   }
	 
	   public static void main(String[] args) {
	      GoBackNProtocolInterface app = new GoBackNProtocolInterface();
	   }
	 
	   @Override
	   public void actionPerformed(ActionEvent evt) {
		   GoBackNRunner runner = new GoBackNRunner();
		   String result = runner.run(hostValue.getText(), senderPortNo.getText(), recPortNo.getText(), pLoss.getText(), sendFileLoc.getText(), recieveFileLoc.getText());
		   stats.setText(result);
	   }
	}
class StartTime {
    int timeGone;
    int startMseconds;
    int currentMseconds;
    int timeoutMseconds;

    StartTime(int timeoutInMseconds) {
        // work out current time in seconds and 
        Calendar cal = new GregorianCalendar();
        int sec = cal.get(Calendar.SECOND);  
        int min = cal.get(Calendar.MINUTE);           
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int milliSec = cal.get(Calendar.MILLISECOND);
        startMseconds = milliSec + (sec*1000) + (min *60000) + (hour*3600000);
        timeoutMseconds = (timeoutInMseconds);
    }

    int getTimeElapsed() {
        Calendar cal = new GregorianCalendar();
        int secElapsed = cal.get(Calendar.SECOND);
        int minElapsed = cal.get(Calendar.MINUTE);
        int hourElapsed = cal.get(Calendar.HOUR_OF_DAY);
        int milliSecElapsed = cal.get(Calendar.MILLISECOND);
        currentMseconds = milliSecElapsed + (secElapsed*1000) + (minElapsed *60000) + (hourElapsed * 3600000);
        timeGone = currentMseconds - startMseconds;
        return timeGone;
    }

    boolean timeout() {
        getTimeElapsed();
        if (timeGone >= timeoutMseconds) {
            return true;
        } else {
            return false;
        }
    }
}
class Sender {
	String stats = "";
	
	public String goBackSender() throws IOException {
		System.out.println("Sending the file :"+fileName);
		File file = new File(fileName);
		DatagramSocket senderDataSocket = new DatagramSocket();
		InetAddress inetAddress = InetAddress.getByName(hostName);

		InputStream inFromFile = new FileInputStream(file);
		int sequenceNumber = 0;
		byte[] fileByteArray = new byte[(int) file.length()];
		inFromFile.read(fileByteArray);
		StartTime timer = new StartTime(0);

		boolean lastMessageFlag = false;
		int ackSequenceNumber = 0;
		int lastAckedSequenceNumber = 0;
		boolean lastAcknowledgedFlag = false;

		int retransmissionCounter = 0;
		int windowSize = 128;
		Long startTime = System.currentTimeMillis();
		Vector<byte[]> sentMessageList = new Vector<byte[]>();
		for (int i = 0; i < fileByteArray.length; i = i + 1021) {
			sequenceNumber += 1;
			byte[] message = new byte[1024];
			message[0] = (byte) (sequenceNumber >> 8);
			message[1] = (byte) (sequenceNumber);
			if ((i + 1021) >= fileByteArray.length) {
				lastMessageFlag = true;
				message[2] = (byte) (1);
			} else {
				lastMessageFlag = false;
				message[2] = (byte) (0);
			}
			if (!lastMessageFlag) {
				for (int j = 0; j != 1021; j++) {
					message[j + 3] = fileByteArray[i + j];
				}
			} else if (lastMessageFlag) {
				for (int j = 0; j < (fileByteArray.length - i); j++) {
					message[j + 3] = fileByteArray[i + j];
				}
			}

			DatagramPacket sendPacket = new DatagramPacket(message, message.length, inetAddress, port);
			sentMessageList.add(message);

			while (true) {
				if ((sequenceNumber - windowSize) > lastAckedSequenceNumber) {
					boolean ackRecievedCorrect = false;
					boolean ackPacketReceived = false;
					while (!ackRecievedCorrect) {
						byte[] ack = new byte[2];
						DatagramPacket ackpack = new DatagramPacket(ack, ack.length);
						try {
							senderDataSocket.setSoTimeout(50);
							senderDataSocket.receive(ackpack);
							ackSequenceNumber = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
							ackPacketReceived = true;
						} catch (SocketTimeoutException e) {
							ackPacketReceived = false;
						}

						if (ackPacketReceived) {
							if (ackSequenceNumber >= (lastAckedSequenceNumber + 1)) {
								lastAckedSequenceNumber = ackSequenceNumber;
							}
							System.out.println("Acknowledgment recieved for sequence Number = " + ackSequenceNumber);
							ackRecievedCorrect = true;
							break;
						} else {
							System.out.println("Resending: Sequence Number to receiver" + sequenceNumber);
							for (int y = 0; y < (sequenceNumber - lastAckedSequenceNumber); y++) {
								byte[] resendMessage = new byte[1024];
								resendMessage = sentMessageList.get(y + lastAckedSequenceNumber);
								DatagramPacket resendDataPacket = new DatagramPacket(resendMessage, resendMessage.length,
										inetAddress, port);
								senderDataSocket.send(resendDataPacket);
								retransmissionCounter += 1;
							}
						}
					}
				} else {
					break;
				}
			}
			if (checkToSend()) {
				senderDataSocket.send(sendPacket);
			}else{
				retransmissionCounter += 1;
				senderDataSocket.send(sendPacket);
			}
			System.out.println("Sent Packet: Sequence number is = " + sequenceNumber + "and is last message= " + lastMessageFlag);
			while (true) {
				boolean ackPacketReceived = false;
				byte[] ack = new byte[2];
				DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

				try {
					senderDataSocket.setSoTimeout(10);
					senderDataSocket.receive(ackpack);
					ackSequenceNumber = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
					ackPacketReceived = true;
				} catch (SocketTimeoutException e) {
					ackPacketReceived = false;
					break;
				}
				if (ackPacketReceived) {
					if (ackSequenceNumber >= (lastAckedSequenceNumber + 1)) {
						lastAckedSequenceNumber = ackSequenceNumber;
						System.out.println("Acknowledgment recieved with: Sequence number = " + ackSequenceNumber);
					}
				}
			}
		}
		while (true) {
				if (sequenceNumber > lastAckedSequenceNumber) {
					boolean ackRecievedCorrect = false;
					boolean ackPacketReceived = false;
					while (!ackRecievedCorrect) {
						byte[] ack = new byte[2];
						DatagramPacket ackpack = new DatagramPacket(ack, ack.length);
						try {
							senderDataSocket.setSoTimeout(50);
							senderDataSocket.receive(ackpack);
							ackSequenceNumber = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
							ackPacketReceived = true;
						} catch (SocketTimeoutException e) {
							ackPacketReceived = false;
						}

						if (ackPacketReceived) {
							if (ackSequenceNumber >= (lastAckedSequenceNumber + 1)) {
								lastAckedSequenceNumber = ackSequenceNumber;
							}
							System.out.println("Acknowledgment recieved for sequence Number = " + ackSequenceNumber);
							ackRecievedCorrect = true;
							break;
						} else {
							System.out.println("Resending: Sequence Number to receiver" + sequenceNumber);
							for (int y = 0; y < (sequenceNumber - lastAckedSequenceNumber); y++) {
								byte[] resendMessage = new byte[1024];
								resendMessage = sentMessageList.get(y + lastAckedSequenceNumber);
								DatagramPacket resendDataPacket = new DatagramPacket(resendMessage, resendMessage.length,
										inetAddress, port);
								senderDataSocket.send(resendDataPacket);
								retransmissionCounter += 1;
							}
						}
					}
				} else {
					break;
				}
			}
		while (!lastAcknowledgedFlag) {

			boolean ackRecievedCorrect = false;
			boolean ackPacketReceived = false;

			while (!ackRecievedCorrect) {
				byte[] ack = new byte[2];
				DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

				try {
					senderDataSocket.setSoTimeout(50);
					senderDataSocket.receive(ackpack);
					ackSequenceNumber = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
					ackPacketReceived = true;
				} catch (SocketTimeoutException e) {
					ackPacketReceived = false;
				}
				if (lastMessageFlag) {
					lastAcknowledgedFlag = true;
					break;
				}
				if (ackPacketReceived) {
					System.out.println("Acknowledgment recieved with: Sequence number = " + ackSequenceNumber);
					if (ackSequenceNumber >= (lastAckedSequenceNumber + 1)) {
						lastAckedSequenceNumber = ackSequenceNumber;
					}
					ackRecievedCorrect = true;
					break;
				} else {
					for (int j = 0; j != (sequenceNumber - lastAckedSequenceNumber); j++) {
						byte[] resendMessage = new byte[1024];
						resendMessage = sentMessageList.get(j + lastAckedSequenceNumber);
						DatagramPacket resendPacket = new DatagramPacket(resendMessage, resendMessage.length, inetAddress,
								port);
						senderDataSocket.send(resendPacket);
						System.out.println("Resending: Sequence Number to receiver= " + lastAckedSequenceNumber);
						retransmissionCounter += 1;
					}
				}
			}
		}

		senderDataSocket.close();
        Long endTime = System.currentTimeMillis();		
		int fileSizeKB = (fileByteArray.length) / 1024;		
		stats += "File : "+fileName+"\n";
		stats += "File size (KB):"+fileSizeKB+"\n";
		stats += "Time taken to send file is (in ms) : "+(endTime-startTime)+"\n";
		stats += "Number of packets lost in complete prcess : "+retransmissionCounter;
		System.out.println("File " + fileName + " has been sent");
		System.out.println("File size : " + fileSizeKB + "KiloByte and time taken is : " + (endTime-startTime));
		System.out.println("Number of retransmissions in complete process: " + retransmissionCounter);
		return stats;
	}

	private boolean checkToSend() {
		//int rand = (int) (Math.random() % 100);
		//return rand < ploss;
		float fNum = ((float)ploss)/100;
		boolean send = (Math.random() > fNum);		
		return send;
	}
	
	private String hostName;
	private int port;
	private String fileName;
	private int ploss;

	public Sender(String shost, int port, String sfile, int ploss) {
		this.fileName = sfile;
		this.port = port;
		this.hostName = shost;
		this.ploss = ploss;
	}
}
class GoBackNRunner {

	public String run(String shost, String sPort, String rPort, String plos, String sfile, String rfile) {
		String stats="";
		try {
			int sport = Integer.parseInt(sPort);
			int rport = Integer.parseInt(rPort);
			int ploss = Integer.parseInt(plos);
			Sender sender = new Sender(shost, sport, sfile, ploss);
			Receiver receiver = new Receiver(rfile, rport);
			Thread recieverThread = new Thread(receiver);
			recieverThread.start();
			Thread.sleep(1000);
			stats =  sender.goBackSender();

		} catch (Exception e) {
			System.out.println("Error occured while booting up...");
		}
		return stats;
	}
}
class Receiver implements Runnable{

	 private int port;
    private String fileName;
    
   public Receiver(String file, int port) {
   	this.fileName = file;
   	this.port = port;
	}
   public void receive() throws IOException {

   	DatagramSocket dsocket = new DatagramSocket(port);
       InetAddress address;
       File file = new File(fileName);
       FileOutputStream outToFile = new FileOutputStream(file);

       boolean lastMessageFlag = false;
       int seqNum = 0;
       int lastSeqNum = 0;

       while (!lastMessageFlag) {
           byte[] message = new byte[1024];
           byte[] fileArr = new byte[1021];
           DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
           dsocket.setSoTimeout(0);
           dsocket.receive(receivedPacket);
           message = receivedPacket.getData();
           address = receivedPacket.getAddress();
           port = receivedPacket.getPort();

           seqNum = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
           if ((message[2] & 0xff) == 1) {
               lastMessageFlag = true;
           } else {
               lastMessageFlag = false;
           }
           if (seqNum == (lastSeqNum + 1)) {
               lastSeqNum = seqNum;
               for (int i=3; i < 1024 ; i++) {
                   fileArr[i-3] = message[i];
               }
               outToFile.write(fileArr);
               System.out.println("Received: Sequence number = " + seqNum +", Flag = " + lastMessageFlag);
               sendAcknowlegment(lastSeqNum, dsocket, address, port);
               if (lastMessageFlag) {
                   outToFile.close();
               } 
           } else {
               if (seqNum < (lastSeqNum + 1)) {
                   sendAcknowlegment(seqNum, dsocket, address, port);
               } else {
                   sendAcknowlegment(lastSeqNum, dsocket, address, port);
               }
           }
       }
       dsocket.close();
       System.out.println("File " + fileName + " has been received.");
	}

   public static void sendAcknowlegment(int lastSeqNum, DatagramSocket socket, InetAddress address, int port) throws IOException {
       byte[] ackPac = new byte[2];
       ackPac[0] = (byte)(lastSeqNum >> 8);
       ackPac[1] = (byte)(lastSeqNum);
       DatagramPacket acknowledgement = new  DatagramPacket(ackPac, ackPac.length, address, port);
       socket.send(acknowledgement);
       System.out.println("Sent ack: Sequence Number = " + lastSeqNum);
   }
	@Override
	public void run() {
		try {
			receive();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

