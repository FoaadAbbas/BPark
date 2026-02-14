package logic;

public class Request {
	private static int idCounter;
	private int id;
	private Message response;
	
	// How much time passed in miliseconds
	private int passedTime;

	// Is the request awaiting
	private boolean isAwaiting = true;

	public Request() {
		id = idCounter++;
	}
	
	public Message getResponse() {
		return response;
	}
	public void setResponse(Message response) {
		this.response = response;
	}
	
	public boolean isAwaiting() {
		return this.isAwaiting;
	}
	
	public void setIsAwaiting(boolean awaiting) {
		this.isAwaiting = awaiting;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getPassedTime() {
		return passedTime;
	}

	public void setPassedTime(int passedTime) {
		this.passedTime = passedTime;
	}
}
