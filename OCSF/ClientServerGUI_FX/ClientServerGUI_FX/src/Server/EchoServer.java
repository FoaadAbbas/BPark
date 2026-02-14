// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 
package Server;

import java.io.*;
import java.util.Vector;

import logic.Faculty;
import logic.Message;
import logic.Student;
import ocsf.server.*;

/**
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;re
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Paul Holden
 * @version July 2000
 */

public class EchoServer extends AbstractServer 
{
  //Class variables *************************************************
  
  /**
   * The default port to listen on.
   */
  //final public static int DEFAULT_PORT = 5555;
  
  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the echo server.
   *
   * @param port The port number to connect on.
   * 
   */
 public static Student [] students=new Student[4];
	public EchoServer(int port) 
	{
		super(port);
	}

  //Instance methods ************************************************
  
	/**
	 * This method handles any messages received from the client.
	 *
	 * @param msg The message received from the client.
	 * @param client The connection from which the message originated.
	 * @param 
	 */
	public void handleMessageFromClient(Object msg, ConnectionToClient client)
	{
		Message msgFromClient = (Message)msg;
		
		System.out.println("Message received: " + msgFromClient.getActionName() + " from " + client + " message id: " + msgFromClient.getRequestId());

		try {
			// Handle the action. Usually coming with a response back
			Message msgFromServer = handleAction(msgFromClient, client);

			// Once done, we can send a reply back to the client
			if (msgFromServer != null) {
				msgFromServer.setRequestId(msgFromClient.getRequestId());
				client.sendToClient(msgFromServer);
			}
		} catch (IOException e) {
			System.err.println("Could not send to client");
			System.err.println(e);
		}

		
		
// if (msgSplt[0].equals("FindStudent")) {
// 	int flag=0;
// 		for(int i=0;i<4;i++) {
// 			if(students[i].getId().equals(msgSplt[1]))
// 			{ 
// 				System.out.println("Server Found");
// 				this.sendToAllClients(students[i].toString());
// 				flag=1;
// 			}
		
// 		}
// 		if (flag!=1) {
// 			System.out.println("Not Found");
// 			this.sendToAllClients("Error student not found 1");
// 		}
// } else if (msgSplt[0].equals("SaveStudent")) {
// 	int flag=0;
// 	for (Student student : students) {
// 		if (student.getId().equals(msgSplt[1])) {
// 			student.setId(msgSplt[2]);
// 			student.setPName(msgSplt[3]);
// 			student.setLName(msgSplt[4]);
// 			student.setFc(Faculty.getFaculty(msgSplt[5]));
			
// 			this.sendToAllClients(student.toString());
			
// 			flag = 1;
// 		}
// 	}
// 	if (flag!=1) {
// 			System.out.println("Not Found");
// 			this.sendToAllClients("Error student not found 2");
// 		}
// } else {
// 	this.sendToAllClients("Error no such action");
// }

			
	}
	
	public Message handleAction(Message msgFromClient, ConnectionToClient client) {
		String actionName = msgFromClient.getActionName();
		// Find and handle any action from client
		if (actionName.equals("FindStudent")) {
			return findStudent(msgFromClient, client);
		} else if (actionName.equals("SaveStudent")) {
			return saveStudent(msgFromClient, client);
		}
		else {
			return msgFromClient.errorReply("Found no such action! " + msgFromClient.getActionName());
		}
	}

	public Message findStudent(Message msgFromClient, ConnectionToClient client) {
		for (Student student : students) {
			if (student.getId().equals(msgFromClient.getObject())) {
				return msgFromClient.reply(student);
			}
		}

		return msgFromClient.errorReply("Not found student with name: " + msgFromClient.getObject());
	}
	
	public Message saveStudent(Message msgFromClient, ConnectionToClient client) {
	
	 	return msgFromClient.errorReply("Not found student with name: " + msgFromClient.getObject());
	}
	
	/**
	 * This method overrides the one in the superclass.  Called
	 * when the server starts listening for connections.
	 */
	protected void serverStarted()
	{
		System.out.println ("Server listening for connections on port " + getPort());
		students[0]=new Student("12345","Yossi","Cohen",new Faculty("SE","9901000"));
		students[1]=new Student("66666","Yossefa","Levi",new Faculty("IE","9901123"));
		students[2]=new Student("77777","moshe","galili",Faculty.getFaculty("SE"));
		students[3]=new Student("77778","moran","galil",Faculty.getFaculty("SE")); 
		

	}
	/**
	 * This method overrides the one in the superclass.  Called
	 * when the server stops listening for connections.
	 */
	protected void serverStopped()  {
		System.out.println ("Server has stopped listening for connections.");
	}  
}
//End of EchoServer class
