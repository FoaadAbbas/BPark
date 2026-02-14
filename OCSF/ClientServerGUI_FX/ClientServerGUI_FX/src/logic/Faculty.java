package logic;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;


public class Faculty implements Serializable {

	private static HashMap<String, Faculty> faculties=new HashMap<String, Faculty>();
	private String fName;
	private String fPhone;
	/**
	 * @param fname
	 * @param fphone
	 */
	public Faculty(String fname, String fphone) {
		//super();
		this.fName = fname;
		this.fPhone = fphone;
		//Faculty.faculties.put(fname, this);
		faculties.put(fname, this);
	}

	public static Faculty getFaculty(String name){
		
		//return Faculty.faculties.get(name);
		return faculties.get(name);
	}
	
	
	/**
	 * @return the fName
	 */
	public String getFName() {
		return fName;
	}
	/**
	 * @param name the fName to set
	 */
	public void setFName(String name) {
		fName = name;
	}
	/**
	 * @return the fPhone
	 */
	public String getFPhone() {
		return fPhone;
	}
	/**
	 * @param phone the fPhone to set
	 */
	public void setFPhone(String phone) {
		fPhone = phone;
	}
	
	public String toString(){
		return String.format("%s %s\n",fName,fPhone);
	}	
	
	public static Collection<String> getFaculties(){
		return faculties.keySet();
	}
	
}
