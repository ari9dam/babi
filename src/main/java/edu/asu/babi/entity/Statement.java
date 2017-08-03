/**Statement.java
 * 4:45:02 PM @author Arindam
 */
package edu.asu.babi.entity;


public class Statement extends Sentence{

	/**
	 * @param id
	 * @param text
	 */
	public Statement(String id, String text) {
		super(id, text);
	}

	@Override
	public String toString() {
		return super.getLineNumber()+ " " + super.getText()+"\n" ;
	}
	
}
