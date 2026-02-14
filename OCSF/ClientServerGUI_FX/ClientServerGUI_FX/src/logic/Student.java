package logic;

import java.io.Serializable;

public class Student implements Serializable {

	private String id;
	private String pName;
	private String lName;
	private Faculty fc;
	/**
	 * @param id
	 * @param name
	 * @param name2
	 * @param fc
	 */
	public Student(String id, String name, String name2, Faculty fc) {
		super();
		this.id = id;
		pName = name;
		lName = name2;
		this.fc = fc;
	}
	/**
	 * @return the fc
	 */
	public Faculty getFc() {
		return fc;
	}
	/**
	 * @param fc the fc to set
	 */
	public void setFc(Faculty fc) {
		this.fc = fc;
		//System.out.println("Faculty set to "+fc.getFName());
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
		//System.out.println("ID set to "+id);
	}
	/**
	 * @return the lName
	 */
	public String getLName() {
		return lName;
	}
	/**
	 * @param name the lName to set
	 */
	public void setLName(String name) {
		lName = name;
		//System.out.println("Last name set to "+name);
	}
	/**
	 * @return the pName
	 */
	public String getPName() {
		return pName;
	}
	/**
	 * @param name the pName to set
	 */
	public void setPName(String name) {
		pName = name;
		//System.out.println("Private name set to "+name);
	}
	
	
	public String toString(){
		return String.format("%s %s %s %s\n",id,pName,lName,fc.toString());
	}
	
}
