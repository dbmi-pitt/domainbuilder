package edu.pitt.dbmi.misc;

import edu.pitt.dbmi.domainbuilder.util.UnixCrypt;

public class UsernameCreator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String user = "";
		String pass = "";
		
		System.out.println(user+" = "+UnixCrypt.crypt("PW",pass));


	}

}
