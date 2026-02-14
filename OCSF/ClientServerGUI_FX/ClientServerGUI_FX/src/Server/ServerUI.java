package Server;

import javafx.application.Application;
import javafx.stage.Stage;
import logic.Faculty;
import logic.Student;

import java.util.Vector;
import gui.AcademicFrameController;
import gui.ServerPortFrameController;
import gui.StudentFormController;
import Server.EchoServer;

public class ServerUI extends Application {
	final public static int DEFAULT_PORT = 5555;
	public static Vector<Student> students=new Vector<Student>();

	public static void main( String args[] ) throws Exception
	   {   
		 launch(args);
	  } // end main
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub				  		
		ServerPortFrameController aFrame = new ServerPortFrameController(); // create StudentFrame
		 
		aFrame.start(primaryStage);
	}
	
	public static void runServer(String p)
	{
		 int port = 0; //Port to listen on

	        try
	        {
	        	port = Integer.parseInt(p); //Set port to 5555
	          
	        }
	        catch(Throwable t)
	        {
	        	System.out.println("ERROR - Could not connect!");
	        }
	    	
	        EchoServer sv = new EchoServer(port);
	        
	        try 
	        {
	          sv.listen(); //Start listening for connections
	        } 
	        catch (Exception ex) 
	        {
	          System.out.println("ERROR - Could not listen for clients!");
	        }
	}
	

}
