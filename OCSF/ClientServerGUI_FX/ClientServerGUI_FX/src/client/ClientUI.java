package client;
import javafx.application.Application;

import javafx.stage.Stage;
import logic.Faculty;
import logic.Student;

import java.util.Vector;
import gui.AcademicFrameController;
import gui.StudentFormController;
import client.ClientController;

public class ClientUI extends Application {
	public static ClientController chat; //only one instance

	public static void main( String args[] ) throws Exception
	   { 
		    launch(args);  
	   } // end main
	 
	@Override
	public void start(Stage primaryStage) throws Exception {
		 chat= new ClientController("localhost", 5555);
		// TODO Auto-generated method stub
						  		
		AcademicFrameController aFrame = new AcademicFrameController(); // create StudentFrame
		 
		aFrame.start(primaryStage);
	}
	
	
}
