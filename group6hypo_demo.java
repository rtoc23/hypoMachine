// Ryan O'Connell
// 301052966
// Operating Systems: Internals (CSCI465.001)

// HW1 / January 25th 2024 - February 7th 2024
// This is an instatiation of the HYPO Machine we
// discussed in class, and is a software representation
// of computer hardware.

// Ryan O'Connell / 301053966
// Aisosa Osifo /
// Kevin Pinto /

// HW2 / February 26th 2024 - April 5th 2024
// Extension of the Homework 1 HYPO Machine, including
// MTOPS virtual operating system structure.

import java.lang.*;
import java.io.*;

// Create hypo demo class
public class group6hypo_demo
{
	// Create hypo machine instance and call main method
	public static void main(String[] args) throws Exception
	{
		// Create a hypo machine
		group6hypo instance = new group6hypo();
		
		// Run the hypo machine (output goes to console)
		// TO HAVE HYPO ONLY WRITE TO FILE, COMMENT LINE BELOW
		instance.main(false);
		
		System.out.println("");
		
		// Run the hypo machine (output goes to file)
		// TO HAVE HYPO ONLY PRINT TO CONSOLE, COMMENT LINE BELOW
		// instance.main(true);
	}
}