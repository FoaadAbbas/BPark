package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ClientUI;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import logic.Faculty;
import logic.Student;

public class StudentFormController implements Initializable {
	private Student s;
		
	@FXML
	private Label lblName;
	@FXML
	private Label lblSurname;
	@FXML
	private Label lblFaculty;
	
	@FXML
	private TextField txtId;
	@FXML
	private TextField txtName;
	@FXML
	private TextField txtSurname;
	
	@FXML
	private Button btnclose=null;
	
	@FXML
	private ComboBox cmbFaculty;	
	
	ObservableList<String> list;
		
	public void loadStudent(Student s1) {
		this.s=s1;
		this.txtId.setText(s.getId());
		this.txtName.setText(s.getPName());
		this.txtSurname.setText(s.getLName());		
		this.cmbFaculty.setValue(s.getFc().getFName());
	}
	
	// creating list of Faculties
	private void setFacultyComboBox() {
		ArrayList<String> al = new ArrayList<String>();	
		al.add("ME");
		al.add("IE");
		al.add("SE");

		list = FXCollections.observableArrayList(al);
		cmbFaculty.setItems(list);
		
		list.add("Test");
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {	
		setFacultyComboBox();		
	}

	public void closeStudentForm(ActionEvent event) throws Exception {
		final Node source = (Node)event.getSource();
	    final Stage stage = (Stage)source.getScene().getWindow();
	    Parent root = FXMLLoader.load(getClass().getResource("/gui/AcademicFrame.fxml"));
		Scene scene = new Scene(root);

		scene.getStylesheets().add(getClass().getResource("/gui/AcademicFrame.css").toExternalForm());
		stage.setTitle("Academic Managment Tool");
		stage.setScene(scene);
		
		stage.show();
	}
	
	public void saveStudent(ActionEvent event) throws Exception {
		String[] strs = {"SaveStudent", s.getId(), txtId.getText(), txtName.getText(), txtSurname.getText(), cmbFaculty.getValue().toString()};
		ClientUI.chat.accept(String.join("\s", strs));
	}
}
