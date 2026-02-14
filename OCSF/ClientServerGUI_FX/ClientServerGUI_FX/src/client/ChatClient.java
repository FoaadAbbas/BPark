// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

package client;

import ocsf.client.*;
import client.*;
import common.ChatIF;
import logic.Faculty;
import logic.Message;
import logic.Request;
import logic.Student;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This class overrides some of the methods defined in the abstract
 * superclass in order to give more functionality to the client.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;
 * @author Fran&ccedil;ois B&eacute;langer
 * @version July 2000
 */
public class ChatClient extends AbstractClient
{
  //Instance variables **********************************************
  
  /**
   * The interface type variable.  It allows the implementation of 
   * the display method in the client.
   */
  ChatIF clientUI; 
  public static Student  s1 = new Student(null,null,null,new Faculty(null,null));
  public static List<Request> awaitingRequests;    

  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the chat client.
   *
   * @param host The server to connect to.
   * @param port The port number to connect on.
   * @param clientUI The interface type variable.
   */
	 
  public ChatClient(String host, int port, ChatIF clientUI) 
    throws IOException 
  {
    super(host, port); //Call the superclass constructor
    this.clientUI = clientUI;
    awaitingRequests = new ArrayList<>();      
    //openConnection();
  }

  //Instance methods ************************************************
    
  /**
   * This method handles all data that comes in from the server.
   *
   * @param msg The message from the server.
   */
	public void handleMessageFromServer(Object msg) 
	{
		System.out.println("--> handleMessageFromServer " + msg);

		Message msgFromServer = (Message)msg;
		
		System.out.println(msgFromServer.getActionName() + " = " + msgFromServer.getRequestId() + " ? " + msgFromServer.getObject());

		int reqId = msgFromServer.getRequestId();
		for(Request req : awaitingRequests) {
			if (req.getId() == reqId) {
				req.setResponse(msgFromServer);
				req.setIsAwaiting(false);
			}
		}
	}

  /**
   * This method handles all data coming from the UI            
   *
   * @param message The message from the UI.    
   */
  
  public Message sendMessageAwaited(Message msgToServer)  
  {
    try
    {
    	openConnection(); //in order to send more than one message
    	Request req = new Request();
    	msgToServer.setRequestId(req.getId());
    	awaitingRequests.add(req);
    	sendToServer(msgToServer);

		int timePassed = 0;
		while (req.isAwaiting()) {
			try {
				Thread.sleep(100);
				timePassed += 100;
			} catch (InterruptedException e) {
				System.out.println("Interrupted..");
			}
			
			if (timePassed > 5 * 1000) {
				System.out.println(String.format("Warning: request %d has timed out after 60 seconds...", req.getId()));
				awaitingRequests.remove(req);
				return null; //TODO: implement timeout exception
			}
		}

		return req.getResponse();
    }
    catch(IOException e)
    {
    	e.printStackTrace();
      	clientUI.display("Could not send message to server: Terminating client."+ e);
      	quit();
    }
	return null;
  }

  /**
   * This method terminates the client.
   */
  public void quit()
  {
    try
    {
      closeConnection();
    }
    catch(IOException e) {}
    System.exit(0);
  }
}
//End of ChatClient class
