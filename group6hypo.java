// Ryan O'Connell
// 301053966
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

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.lang.*;
import java.io.*;

public class group6hypo
{
	// Global error code declarations (HW1)
	final int OK = 0;
	final int HALT = 1;
	final int OPENERROR = -1;
	final int BADADDRESS = -2;
	final int NOEND = -3;
	final int BADPC = -4;
	final int BADMODE = -5;
	final int BADOPCODE = -6;
	final int DIVBYZERO = -7;
	final int STACKOVERFLOW = -8;
	final int STACKUNDERFLOW = -9;
	// Global error code additions (HW2)
	final int NOSPACE = -10;
	final int INVALIDSIZE = -11;
	
	// HW2 // Global constant declarations
	final int EOL = -1;
	final int DefaultPriority = 128;
	final int ReadyState = 1;
	int processID = 1;
	
	// HW2 // Global constants for initializePCB()
	final int NextAddress = 0;
	final int PID = 1;
	final int State = 2;
	final int WaitReason = 3;
	final int Priority = 4;
	final int StackStart = 5;
	final int StackSize = 6;
	//
	//	Message functions (7-9) not used in this project, so pickup at 7
	//
	final int GPR0 = 7;
	final int GPR1 = 8;
	final int GPR2 = 9;
	final int GPR3 = 10;
	final int GPR4 = 11;
	final int GPR5 = 12;
	final int GPR6 = 13;
	final int GPR7 = 14;
	final int SPindex = 15;
	final int PCindex = 16;
	final int PSRindex = 17;
	
	////
	// global hardware variables
	//
	// 					[     ]
	// *----[ MAR ]---->[ RAM ]---->[ MBR ]<----*
	// ^				[	  ]		  ||		|
	// |                   			  ||		|
	// |	   *----------------------*|		|
	// |       |					   |		|
	// |	   v					   v		|
	// |    [  0  ]			[     IR      ]		|
	// |    [  1  ]				|		|		|
	// |    [  2  ]				v		|		|
	// |    [  3  ]			[ decoder ] | 		|
	// |    [  4  ]						|		|
	// |    [  5  ]						v		|
	// |    [  6  ]					[ MAR ]		|
	// |    [  7  ]								v
	// |    [  SP ]<--(GPRs, not just 6)---->[ ALU ]
	// *<---[  PC ]
	//						[ PSR ] [ CLOCK ]
	////
	
	// Global declarations of hardware components
	long IR;
	long SP;
	long PC;
	long MAR;
	long MBR;
	long PSR;
	long CLOCK;
	long GPR[] = new long[8];
	long RAM[] = new long[10000];
	
	// HW2 // List declarations
	long RQ = EOL;
	long WQ = EOL;
	long OSFreeList = EOL;
	long userFreeList = EOL;
	
	// *****
	// NAME : main()
	// DESC : Call for and catch program filename, Load program,
	// 	  Assign program counter, Dump memory, Return output code.
	// INPT : Boolean value to determine if printed to console or file.
	// OUTP : Printed output of program either to monitor or file.
	// RTRN : OK - 0
	//        HALT = 1
	//	  OPENERROR = -1
	// 	  BADADDRESS = -2
	//        NOEND = -3
	// 	  BADPC = -4
	// 	  BADMODE = -5
	//	  BADOPCODE = -6
	//	  DIVBYZERO = -7
	//	  STACKOVERFLOW = -8
	//	  STACKUNDERFLOW = -9
	// *****
	public int main(boolean toFile) throws Exception
	{
		// Welcome!
		System.out.println("-------------");
		System.out.println("Group 6 HYPO");
		System.out.println("-------------");
		
		// Call and catch program filename
		Scanner consoleRead = new Scanner(System.in);
		System.out.println("Enter program filename...");
		System.out.print("> ");
		
		// Get filename from console
		String filename = consoleRead.nextLine();
		
		// Initialize system variables to 0
		initializeSystem();
		
		// Get output code from absoluteLoader
		// See final declarations
		long outputCode = absoluteLoader(filename);
		
		if(outputCode < 0)
			return (int)outputCode;
		
		// Assign output code to program counter
		PC = outputCode;
		
		// If user wants to write to file instead of console,
		if(toFile)
		{
			// Change printstream
			FileOutputStream f = new FileOutputStream("oconnell-hw1 console.txt");
			System.setOut(new PrintStream(f));
			
			// Rewrite header so it's included in file
			System.out.println("--------------");
			System.out.println("O'Connell HYPO");
			System.out.println("--------------");
			
			System.out.println("Enter program filename...");
			System.out.println("> "+filename);
		}
		
		System.out.println("--------------");
		// Dump memory on program load
		dumpMemory("After program load", 0, 38);
		
		// Execute program
		int executionCode = (int)CPU();
		
		System.out.println("--------------");
		// Dump memory post program
		dumpMemory("After program execution", 0, 38);
		
		System.out.println("--------------");
		printPCB(0);
		
		return(executionCode);
	}
	
	// *****
	// NAME : absoluteLoader()
	// DESC : Take filename from user. See if it exists as a file,
	// 	  load file content into main memory.
	// INPT : filename.txt
	// OUTP : N/A
	// RTRN : OK = 0 to 9999
	//	  OPENERROR = -1
	// 	  BADADDRESS = -2
	//        NOEND = -3
	// 	  BADPC = -4
	// *****
	public long absoluteLoader(String filename)
	{
		try 
		{
			// Attempt to open desired file and scanner in file
			File program = new File(filename);
			Scanner programReader = new Scanner(program);
			
			// While the program file has more input...
			while(programReader.hasNext())
			{
				// Store the address from the current line
				long address = programReader.nextLong();
				// Store the instruction from the current line
				long command = programReader.nextLong();
				
				// If the address is end of file indicator (EOFI)...
				if(address == -1)
				{
					// No need to close file in library I chose.
					// -----
					// If the instruction is valid...
					if(command < 10000 && command > -1)
						// Return the PC value associated with EOF
						return(command);
					// Otherwise, bad PC value error handle:
					else
					{
						System.out.println("ERROR: Invalid PC value associated with EOF indicator.");
						return(BADPC);
					}
				}
				
				// If address is valid...
				// (Between 0 and 9999, inclusive)
				else if(address < 10000 && address > -1)
				{
					// Store command in current memory address.
					RAM[(int)address] = command;
				}
				
				// Bad address error handle:
				else
				{
					System.out.println("ERROR: Memory address is unreachable.");
					return(BADADDRESS);
				}
			}
			// No EOF indicator error handle:
			System.out.println("ERROR: Reached end of file without indicator (-1).");
			return(NOEND);
		}
		// File does not exist error handle:
		catch(FileNotFoundException e)
		{
			System.out.println("ERROR: File not found.");
			return(OPENERROR);
		}
	}
	
	// *****
	// NAME : initializeSystem()
	// DESC : Initalize all hardware components to zero.
	// INPT : N/A
	// OUTP : N/A
	// RTRN : N/A
	// *****
	// HW2 //
	// *****
	// NAME : initializeSystem() [revision]
	// DESC : Initialize userFreeList, OSFreeList, and create null process.
	// INPT : N/A
	// OUTP : N/A 
	// RTRN : N/A
	// AUTH	: Ryan O'Connell
	// *****
	public void initializeSystem()
	{
		// Set special registers to 0.
		IR = 0;
		SP = 0;
		PC = 0;
		MAR = 0;
		MBR = 0;
		PSR = 0;
		CLOCK = 0;

		// Set GPRs to 0.
		for(int i = 0; i < GPR.length; i++)
			GPR[i] = 0;

		// Set all RAM addresses to 0.
		for(int k = 0; k < RAM.length; k++)
			RAM[k] = 0;

		// Set userFreeList start address to 3000 (beginning of heap).
		// Basically, set one big block in userFreeList.
		userFreeList = 3000;
		// Set the nextAddress field to EOL (only block in heap).
		RAM[(int)userFreeList] = EOL;
		// Set the size of the block equal to 3000 (fills entire heap).
		RAM[(int)userFreeList + 1] = 3000;

		// Set OSFreeList start address to 6000 (beginning of OS mem).
		// Basically, set one big block in OSFreeList
		OSFreeList = 6000;
		// Set the nextAddress field to EOL (only block in OS mem).
		RAM[(int)OSFreeList] = EOL;
		// Set the size of the block equal to 4000 (fills entire OS mem).
		RAM[(int)OSFreeList + 1] = 4000;

		// createProcess(null program, 0 priority)
	}
	
	// *****
	// NAME : CPU()
	// DESC : Capture, decode, and execute lines of 
	//	  filename.txt from absoluteLoader.
	// INPT : N/A
	// OUTP : N/A
	// RTRN : OK = 0
	//        HALT = 1
	//        BADADDRESS = -2
	//	  BADMODE = -3
	//	  BADOPCODE = -6
	//	  DIVBYZERO = -7
	//	  STACKOVERFLOW = -8
	//	  STACKUNDERFLOW = -9
	// *****
	public long CPU()
	{
		//// BEGIN FETCH CYCLE
		
		// While (no error) and (no halt)
		while(PC >= 0)
		{
			// If PC address is valid...
			if(PC < 10000 && PC > -1)
			{
				// Move PC address into MAR
				MAR = PC;
				// Increment PC by one
				PC++;
				// Store the value of where PC pointed in RAM into MBR
				MBR = RAM[(int)MAR];
			}
			// ...otherwise throw invalid address error
			else
			{
				System.out.println("ERROR: Memory address is unreachable.");
				return(BADADDRESS);
			}
			
			// Insert instruction from main memory into IR
			IR = MBR;
			
			//// END FETCH CYCLE
			//// ------------------
			//// BEGIN DECODE CYCLE
			
			// Retrieve opcode as one or two digits
			long opcode = IR / 10000;

			// Isolate remaining four digits
			// Retrieve mode1 as one digit
			long fourDigitInstruction = IR % 10000;
			long mode1 = fourDigitInstruction / 1000;
			
			// Isolate remaining three digits
			// Retrieve operand1 as one digit
			long threeDigitInstruction = IR % 1000;
			long operand1 = threeDigitInstruction / 100;
			
			// Isolate remaining two digits
			// Retrieve mode2 as one digit
			long twoDigitInstruction = IR % 100;
			long mode2 = twoDigitInstruction / 10;
			
			// Isolate remaining digit
			// Retrieve operand2 as one digit
			long oneDigitInstruction = IR % 10;
			long operand2 = oneDigitInstruction / 1;
			
			// If GPR is invalid (outside of 0 - 7)...
			// Bad address error handle:
			if((operand1 > 7 || operand2 > 7) || (operand1 < 0 || operand2 < 0))
			{
				System.out.println("ERROR: Memory address is unreachable.");
				return(BADADDRESS);
			}
			
			// If MODE is invalid (outside of 0 - 6)...
			// Bad address error handle:
			if((mode1 > 6 || mode2 > 6) || (mode1 < 0 || mode2 < 0))
			{
				System.out.println("ERROR: Memory address is unreachable.");
				return(BADADDRESS);
			}
			
			//// END DECODE CYCLE
			//// -------------------
			//// BEGIN EXECUTE CYCLE
			
			// Hold the values and addresses associated with both operands
			long operand1Value;
			long operand1Address;
			long operand2Value;
			long operand2Address;
			
			// Hold the return code, address, and value in operand1params, operand2params
			long[] operand1params;
			long[] operand2params;

			switch((int)opcode)
			{
				// HALT  
				case 0: 
					// Don't end program, end CPU cycle
					CLOCK += 12;
					return(HALT);
					
				// ADD
				case 1:
					// This is a two instruction command
					// Will need access to mode1, 2, operand1, 2
					// -----------------------------------------
					
					// Get parameters (return code, address, value) for operand1
					operand1params = fetchOperand(mode1, operand1);
					
					// Check for errors with model1 and operand1
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand1params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand1Value = operand1params[2];
						operand1Address = operand1params[1];
					}
					
					// Get parameters (return code, address, value) for operand2
					operand2params = fetchOperand(mode2, operand2);
					
					// Check for errors with model2 and operand2
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand2params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand2Value = operand2params[2];
						operand2Address = operand2params[1];
					}
					
					// Add the two operand values.
					long sum = operand1Value + operand2Value;
					
					// If in register mode... (operand is in register)
					if(mode1 == 1)
						GPR[(int)operand1] = sum;
					
					// If in immediate mode...
					else if(mode1 == 6)
					{
						System.out.println("ERROR: Destination operand cannot be in immediate mode.");
						return(BADMODE);
					}
					
					// If in any other mode, store sum in RAM at operand 1 position.
					else
						RAM[(int)operand1Address] = sum;
					
					CLOCK += 3;
					break;
				
				// SUBTRACTION
				case 2:
					// This is a two instruction command
					// Will need access to mode1, 2, operand1, 2
					// -----------------------------------------
					
					// Get parameters (return code, address, value) for operand1
					operand1params = fetchOperand(mode1, operand1);
					
					// Check for errors with model1 and operand1
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand1params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand1Value = operand1params[2];
						operand1Address = operand1params[1];
					}
					
					// Get parameters (return code, address, value) for operand2
					operand2params = fetchOperand(mode2, operand2);
					
					// Check for errors with model2 and operand2
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand2params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand2Value = operand2params[2];
						operand2Address = operand2params[1];
					}
					
					// Subtract the two operand values.
					long difference = operand1Value - operand2Value;
					
					// If in register mode... (operand is in register)
					if(mode1 == 1)
						GPR[(int)operand1] = difference;
					
					// If in immediate mode...
					else if(mode1 == 6)
					{
						System.out.println("ERROR: Destination operand cannot be in immediate mode.");
						return(BADMODE);
					}
					
					// If in any other mode, store difference in RAM at operand 1 position.
					else
						RAM[(int)operand1Address] = difference;
					
					CLOCK += 3;
					break;
					
				// MULTIPLY
				case 3:
					// This is a two instruction command
					// Will need access to mode1, 2, operand1, 2
					// -----------------------------------------
					
					// Get parameters (return code, address, value) for operand1
					operand1params = fetchOperand(mode1, operand1);
					
					// Check for errors with model1 and operand1
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand1params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand1Value = operand1params[2];
						operand1Address = operand1params[1];
					}
					
					// Get parameters (return code, address, value) for operand2
					operand2params = fetchOperand(mode2, operand2);
					
					// Check for errors with model2 and operand2
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand2params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand2Value = operand2params[2];
						operand2Address = operand2params[1];
					}
					
					// Multiply the two operand values.
					long product = operand1Value * operand2Value;
					
					// If in register mode... (operand is in register)
					if(mode1 == 1)
						GPR[(int)operand1] = product;
					
					// If in immediate mode...
					else if(mode1 == 6)
					{
						System.out.println("ERROR: Destination operand cannot be in immediate mode.");
						return(BADMODE);
					}
					
					// If in any other mode, store product in RAM at operand 1 position.
					else
						RAM[(int)operand1Address] = product;
					
					CLOCK += 6;
					break;
					
				// DIVIDE
				case 4:
					// This is a two instruction command
					// Will need access to mode1, 2, operand1, 2
					// -----------------------------------------
					
					// Get parameters (return code, address, value) for operand1
					operand1params = fetchOperand(mode1, operand1);
					
					// Check for errors with model1 and operand1
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand1params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand1Value = operand1params[2];
						operand1Address = operand1params[1];
					}
					
					// Get parameters (return code, address, value) for operand2
					operand2params = fetchOperand(mode2, operand2);
					
					// Check for errors with model2 and operand2
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand2params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand2Value = operand2params[2];
						operand2Address = operand2params[1];
					}
					
					// If the divisor is zero, this is a fatal error and needs to be caught
					if(operand2Value == 0)
					{
						System.out.println("ERROR: Attempt to divide by zero.");
						return(DIVBYZERO);
					}
					
					// Divide the two operand values.
					long quotient = operand1Value / operand2Value;
					
					// If in register mode... (operand is in register)
					if(mode1 == 1)
						GPR[(int)operand1] = quotient;
					
					// If in immediate mode...
					else if(mode1 == 6)
					{
						System.out.println("ERROR: Destination operand cannot be in immediate mode.");
						return(BADMODE);
					}
					
					// If in any other mode, store quotient in RAM at operand 1 position.
					else
						RAM[(int)operand1Address] = quotient;
					
					CLOCK += 6;
					break;
					
				// MOVE
				case 5: 
					// This is a two instruction command
					// Will need access to mode1, 2, operand1, 2
					// -----------------------------------------
					
					// Get parameters (return code, address, value) for operand1
					operand1params = fetchOperand(mode1, operand1);
					
					// Check for errors with model1 and operand1
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand1params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand1Value = operand1params[2];
						operand1Address = operand1params[1];
					}
					
					// Get parameters (return code, address, value) for operand2
					operand2params = fetchOperand(mode2, operand2);
					
					// Check for errors with model2 and operand2
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand2params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand2Value = operand2params[2];
						operand2Address = operand2params[1];
					}
					
					// If in register mode... (operand is in register)
					if(mode1 == 1)
						GPR[(int)operand1] = operand2Value;
					
					// If in immediate mode...
					else if(mode1 == 6)
					{
						System.out.println("ERROR: Destination operand cannot be in immediate mode.");
						return(BADMODE);
					}
					
					// If in any other mode, store sum in RAM at operand 1 position.
					else
						RAM[(int)operand1Address] = operand2Value;

					CLOCK += 2;
					break;
					
				// BRANCH
				case 6:
					// This is a one instruction command
					// Will need access only to an address [0000 - 9999]
					// -------------------------------------------------
					
					// If address is valid, jump to value in that address
					if(PC < 10000 && PC > -1)
						PC = RAM[(int)PC];
					else
					{
						System.out.println("ERROR: Memory address is unreachable.");
						return(BADADDRESS);
					}

					CLOCK += 2;
					break;
					
				// BRANCH ON MINUS
				case 7:
					// This is a one instruction command
					// Will need access to mode1, operand1
					// -----------------------------------
					
					// Get parameters (address, value, and return code) for operand 1
					operand1params = fetchOperand(mode1, operand1);
					
					// Check for errors with model1 and operand1
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand1params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand1Value = operand1params[2];
						operand1Address = operand1params[1];
					}
					
					// If value is less than 0...
					if(operand1Value < 0)
					{
						// ...and address is valid, jump to value in address
						if(PC > -1 && PC < 10000)
							PC = RAM[(int)PC];
						else
						{
							System.out.println("ERROR: Memory address is unreachable.");
							return(BADADDRESS);
						}
					}
					// Otherwise step on.
					else
						PC = PC + 1;
						
					CLOCK += 4;
					break;
					
				// BRANCH ON PLUS
				case 8:
					// This is a one instruction command
					// Will need access to mode1, operand1
					// -----------------------------------
					
					// Get parameters (address, value, and return code) for operand 1
					operand1params = fetchOperand(mode1, operand1);
					
					
					// Check for errors with model1 and operand1
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand1params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand1Value = operand1params[2];
						operand1Address = operand1params[1];
					}
					
					// If value is greater than 0...
					if(operand1Value > 0)
					{
						// ...and address is valid, jump to value in address
						if(PC > -1 && PC < 10000)
							PC = RAM[(int)PC];
						else
						{
							System.out.println("ERROR: Memory address is unreachable.");
							return(BADADDRESS);
						}
					}
					// Otherwise, step on.
					else
						PC = PC + 1;
					
					CLOCK += 4;
					break;
					
				// BRANCH ON ZERO
				case 9:
					// This is a one instruction command
					// Will need access to mode1, operand1
					// -----------------------------------
					
					// Get parameters (address, value, and return code) for operand 1
					operand1params = fetchOperand(mode1, operand1);
					
					// Check for errors with model1 and operand1
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand1params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand1Value = operand1params[2];
						operand1Address = operand1params[1];
					}
					
					// If value is zero...
					if(operand1Value == 0)
					{
						// ...and address is valid, jump to value in address
						if(PC > -1 && PC < 10000)
							PC = RAM[(int)PC];
						else
						{
							System.out.println("ERROR: Memory address is unreachable.");
							return(BADADDRESS);
						}
					}
					// Otherwise, step on.
					else
						PC = PC + 1;
					
					CLOCK += 4;
					break;
					
				// PUSH
				case 10:
					// This is a one instruction command
					// Will need access to mode1, operand1
					// -----------------------------------
					
					// Get parameters (address, value, and return code) for operand 1
					operand1params = fetchOperand(mode1, operand1);
					
					// Check for errors with model1 and operand1
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand1params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand1Value = operand1params[2];
						operand1Address = operand1params[1];
					}
					
					if(SP > 9999)
					{
						System.out.println("ERROR: Stack overflow.");
						return(STACKOVERFLOW);
					}
						
					SP = SP + 1;
					
					RAM[(int)SP] = operand1Value;
					
					CLOCK += 2;
					break;
					
				// POP 
				case 11:
					// This is a one instruction command
					// Will need access to mode1, operand1
					// -----------------------------------
					
					// Get parameters (address, value, and return code) for operand 1
					operand1params = fetchOperand(mode1, operand1);
					
					// Check for errors with model1 and operand1
					// If an error comes back, it's a bad address error.
					// If no error comes through, we can continue (only return OK after operation)
					if(operand1params[0] != 0)
						return(BADADDRESS);
					else
					{
						operand1Value = operand1params[2];
						operand1Address = operand1params[1];
					}
					
					if(SP < 0)
					{
						System.out.println("ERROR: Stack underflow.");
						return(STACKUNDERFLOW);
					}
					
					RAM[(int)operand1Address] = RAM[(int)SP];
					
					SP = SP - 1;
					
					CLOCK += 2;
					break;
					
				// SYSTEM CALL
				case 12:
					if(PC < 0 || PC > 9999)
					{
						System.out.println("ERROR: Memory address is unreachable.");
						return(BADADDRESS);
					}

					System.out.println("System Call not implemented.");

					CLOCK += 12;
					break;
					
				// DEFAULT
				default:
					System.out.println("ERROR: Opcode out of range.");
					return(BADOPCODE);
			}
		}
		//// END EXECUTE CYCLE
		return(OK);
	}
	
	// *****
	// NAME : fetchOperand()
	// DESC : Get the address and value of an operand,
	// 	  given the mode and register requested of it.
	// INPT : operand mode (mode)
	//	  operand register (register)
	// OUTP : return code (array position 0)
	//	  operand address (array position 1)
	//	  operand value (array position 2)
	// RTRN : OK = 0
	//        BADADDRESS = -2
	//	  BADMODE = -3
	// *****
	public long[] fetchOperand(long mode, long register)
	{
		long returnCode = OK;
		long operandAddress = 0;
		long operandValue = 0;
		
		switch((int)mode)
		{
			// UNUSED MODE
			case 0:
				break;
			// REGISTER MODE
			case 1:
				// Used when operand is in a register.
				// -------------------------------------------
				// Address is not in use, set to invalid param
				// Get operand value from register 
				operandAddress = -1;
				operandValue = GPR[(int)register];
				break;
				
			// REGISTER DEFERRED MODE
			case 2:
				// Used when operand is in main memory and the address is in a register.
				// ---------------------------------------------------------------------
				// Get memory address from register
				operandAddress = GPR[(int)register];
				// If obtained address is valid (0 - 9999 inclusive)...
				if(operandAddress < 10000 && operandAddress > -1)
					// Set value to figure stored in memory address
					operandValue = RAM[(int)operandAddress];
				// ...Otherwise throw bad address error:
				else
				{
					System.out.println("ERROR: Memory address is unreachable.");
					returnCode = BADADDRESS;
				}
				break;
				
			// AUTOINCREMENT MODE
			case 3:
				// Used in the same case as deferred mode, but increments addr. content by one.
				// ----------------------------------------------------------------------------
				// Get memory address from register
				operandAddress = GPR[(int)register];
				// If obtained address is valid (0 - 9999 inclusive)...
				if(operandAddress < 10000 && operandAddress > -1)
					// Set value to figure stored in memory address
					operandValue = RAM[(int)operandAddress];
				// ...Otherwise throw bad address error:
				else
				{
					System.out.println("ERROR: Memory address is unreachable.");
					returnCode = BADADDRESS;
				}
				// Increment value stored in GPR by one
				GPR[(int)register] = (GPR[(int)register]+1);
				break;
				
			// AUTODECREMENT MODE
			case 4:
				// Used in the same case as deferred mode, but increments addr. content by one.
				// ----------------------------------------------------------------------------
				// Decrement value stored in GPR by one
				GPR[(int)register] = (GPR[(int)register]-1);
				// Get memory address from register
				operandAddress = GPR[(int)register];
				// If obtained address is valid (0 - 9999 inclusive)...
				if(operandAddress < 10000 && operandAddress > -1)
					// Set value to figure stored in memory address
					operandValue = RAM[(int)operandAddress];
				// ...Otherwise throw bad address error:
				else
				{
					System.out.println("ERROR: Memory address is unreachable.");
					returnCode = BADADDRESS;
				}
				break;
				
			// DIRECT MODE
			case 5:
				// Used when operand address is stored in PC.
				// ------------------------------------------
				// Grab address from program counter
				operandAddress = RAM[(int)PC++];
				// Increment PC after grab
				// PC++;
				// If obtained address is valid (0 - 9999 inclusive)...
				if(operandAddress < 10000 && operandAddress > -1)
					// Set value to figure stored in memory address
					operandValue = RAM[(int)operandAddress];
				// ...Otherwise throw bad address error:
				else
				{
					System.out.println("ERROR: Memory address is unreachable.");
					returnCode = BADADDRESS;
				}
				break;
				
			// IMMEDIATE MODE
			case 6:
				// Used when operand value is already in IR.
				// -----------------------------------------
				// Address is used, set to invalid param
				operandAddress = -1;
				// Grab value from PC address in RAM
				operandValue = RAM[(int)PC++];
				// Increment PC
				// PC++;
				break;
			
			// DEFAULT
			default:
				System.out.println("ERROR: Invalid mode parameter (valid : 0-6).");
				returnCode = BADMODE;
		}
		
		// Package return code, address, value into 1d array.
		long[] arr = {returnCode, operandAddress, operandValue};
		
		return arr;
	}
	
	// *****
	// NAME : dumpMemory()
	// DESC : Show the content of all memory,
	// 	  Kind of like a print statement.
	// INPT : String of when function is executed (label)
	//	  Starting address of when to print memory (startAddr)
	//	  How much memory to dump (size)
	// OUTP : N/A
	// RTRN : N/A
	// *****
	public void dumpMemory(String label, int startAddr, int size)
	{
		// Print the label which identifies when the memory dump is happening
		System.out.println(label);
		System.out.println("------------------------------------------------------------------------------------");
		// Print the GPRs as a list (plus stack pointer and program counter)
		System.out.println("GPRs:\t\tG0\tG1\tG2\tG3\tG4\tG5\tG6\tG7\tSP\tPC");
		// Print the contents of the GPRs (plus stack pointer and program counter)
		System.out.println("\t\t"+GPR[0]+"\t"+GPR[1]+"\t"+GPR[2]+"\t"+GPR[3]+"\t"+GPR[4]+"\t"+GPR[5]+"\t"+GPR[6]+"\t"+GPR[7]+"\t"+SP+"\t"+PC);
		// Build key for address number
		System.out.println("Address:\t+0\t+1\t+2\t+3\t+4\t+5\t+6\t+7\t+8\t+9\t");
		
		// Get start and predicted end addresses
		int addr = startAddr;
		int end = startAddr + size;
		
		while(addr < end)
		{
			System.out.print(addr+"\t\t");
			for(int i = 0; i < 10; i++)
			{
				if(addr < end)
					System.out.print(RAM[addr++]+"\t");
				else
					break;
			}
			System.out.println("");
		}
		
		// Print clock and psr content
		System.out.println("Other:\t\tCLOCK\tPSR");
		System.out.println("\t\t"+CLOCK+"\t"+PSR);
	}
	
	// HW2 //
	// *****
	// NAME : createProcess()
	// DESC : Given a filename and priority, create a process
	// INPT : String filename (filename),
	//	  long priority (priority)
	// OUTP : N/A 
	// RTRN : OK = 0
	// AUTH	: Ryan O'Connell
	// *****
	public long createProcess(String filename, long priority)
	{
		// Call to allocate memory in OS block
		// Size is 18 as PCB has 18 components
		int currentPCBptr = (int)allocateOSMemory(18);
		
		// If returned address is negative, error was thrown
		// Return error code (no message needed)
		if(currentPCBptr < 0)
			return currentPCBptr;
		
		// Initialize PCB addresses to 0
		// Set PID, default priority, state to ready, and nextAddr to EOL
		initializePCB(currentPCBptr);
		
		// Attempt to load given program
		long PCoutput = absoluteLoader(filename);
		
		// If returned address is negative, error was thrown
		// Return error code (no message needed)
		if(PCoutput < 0)
			return PCoutput;
		
		// Allocate stack in user memory block
		// Stack is always size 20
		long stackPtr = allocateUserMemory(20);
		
		// If returned address is negative, error was thrown
		// Free the already allocated OS memory
		// (Begins at currentPCBptr, always size 18)
		// Then return error code (no message needed)
		if(stackPtr < 0)
		{
			// Free allocated PCB space
			freeOSMemory(currentPCBptr, 18);
			return(stackPtr);
		}
		
		// Set Stack Pointer in PCB = stackPtr + stack size (static 20)
		RAM[currentPCBptr + SPindex] = stackPtr + 20;
		// Set StackStart addr in PCB = stackPtr
		RAM[currentPCBptr + StackStart] = stackPtr;
		// Set StackSize in PCB = stack size (static 20)
		RAM[currentPCBptr + StackSize] = 20;
		
		// Set priority in PCB equal to priority in method call
		RAM[currentPCBptr + Priority] = priority;
		
		// Dump program area
		dumpMemory("Create process output:", currentPCBptr, 18);
		
		// Print PCB 
		printPCB(currentPCBptr);
		
		// Insert PCB into Ready Queue
		insertPCBintoReadyQueue(currentPCBptr);
		
		return(OK);
	}
	
	// HW2 // 
	// *****
	// NAME : initializePCB()
	// DESC : Set all RAM addresses in PCB range to 0
	// INPT : Int start location of PCB
	//	  (int type prevents need for constant typecasting)
	// OUTP : N/A
	// RTRN : N/A
	// AUTH	: Ryan O'Connell
	// *****
	public void initializePCB(int PCBptr)
	{
		//// Initialize all addresses in PCB to zero
		// RAM[PCBptr + NextAddress] = 0 ---> RAM[PCBptr + 0] = 0
		// RAM[PCBptr + PID] = 0 ---> RAM[PCBptr + 1] = 0
		// ...
		// RAM[PCBptr + PSRindex] = 0 ---> RAM[PCBptr + 17] = 0
		////
		for(int i = 0; i < 18; i++)
			RAM[(int)PCBptr + i] = 0;
		
		// Set PCB PID field = current processID, then increment
		RAM[PCBptr + PID] = processID++;
		// Set PCB Priority field = defaultPriority (128)
		RAM[PCBptr + Priority] = DefaultPriority;
		// Set PCB State field = readyState (1)
		RAM[PCBptr + State] = ReadyState;
		// Set PCB NextAddress field = End of List (-1)
		RAM[PCBptr + NextAddress] = EOL;
	}
	
	// HW2 // 
	// *****
	// NAME : printPCB()
	// DESC : Print all elements of PCB list
	// INPT : Int start position of PCB
	//	  (int type prevents need for constant typecasting)
	// OUTP : Values of all PCB components
	// RTRN : N/A
	// AUTH	: Ryan O'Connell
	// *****
	public void printPCB(int PCBptr)
	{
		// Print some information which identifies where the PCB dump starts
		System.out.println("Printing PCB beginning at index "+PCBptr);
		System.out.println("------------------------------------------------------------------------------------");
		// Headers for the PCB address, next address, PID, state, priority, stack start addr., and stack size
		System.out.println("Process Info:\tNext PCB Address : "+RAM[PCBptr + NextAddress]+
						   " \\ PID : "+RAM[PCBptr + PID]+
						   " \\ State : "+RAM[PCBptr + State]+
						   " \\ Priority : "+RAM[PCBptr + Priority]);
		// Print stack information
		System.out.println("Stack Info:\tStack Address : "+RAM[PCBptr + StackStart]+
						   " \\ Stack Size : "+RAM[PCBptr + StackSize]);
		// Print special registers
		System.out.println("Special Reg.:\tSP : "+RAM[PCBptr + SPindex]+
						   " \\ PC : "+RAM[PCBptr + PCindex]+
						   " \\ PSR : "+RAM[PCBptr + PSRindex]); 
		// Headers for the GPR contents, plus SP and PC
		System.out.println("GPRs:\t\tG0 : "+RAM[PCBptr + GPR0]+
						   " \\ G1 : "+RAM[PCBptr + GPR1]+
						   " \\ G2 : "+RAM[PCBptr + GPR2]+
						   " \\ G3 : "+RAM[PCBptr + GPR3]+
						   " \\ G4 : "+RAM[PCBptr + GPR4]+
						   " \\ G5 : "+RAM[PCBptr + GPR5]+
						   " \\ G6 : "+RAM[PCBptr + GPR6]+
						   " \\ G7 : "+RAM[PCBptr + GPR7]);
	}
	
	// HW2 // 
	// *****
	// NAME : allocateOSMemory()
	// DESC : Dedicate a block of memory from OS chunk (6000 - 9999)
	// INPT : Int size of block requested
	//	  (Int to prevent typcasting from long)
	// OUTP : Error code or start address of allocated block
	// RTRN : OK = >0 
	// 	  NOSPACE = -10
	//	  INVALIDSIZE = -11;
	// AUTH	: Ryan O'Connell
	// *****
	public long allocateOSMemory(int sizeRequest)
	{
		// If pointer goes to end of list, no space is left. Throw error.
		if(OSFreeList == EOL)
		{
			System.out.println("ERROR: No suitable block found in OS memory.");
			return NOSPACE;
		}
		// If requested size is < 0, cannot allocate. Throw error.
		// Also, if requested size is > 4000, cannot allocate.
		if(sizeRequest < 0 || sizeRequest > 4000)
		{
			System.out.println("ERROR: Memory size requested is invalid.");
			return INVALIDSIZE;
		}
		// Cannot allocate only one cell, discreetly bump to two.
		if(sizeRequest == 1)
		{
			sizeRequest = 2;
		}
		
		// Set the current start address pointer equal to OSFreeList.
		int currentPCBptr = (int)OSFreeList;
		// Set the start address pointer of the previous PCB equal to EOL.
		int previousPCBptr = EOL;
		
		// While the current start address pointer is not equal to EOL...
		while(currentPCBptr != EOL)
		{
			// If the size string of the current PCB is the same size as the request block...
			if(RAM[currentPCBptr + 1] == sizeRequest)
			{
				// And if the current PCB address is equal to the OSFreeList address...
				if(currentPCBptr == OSFreeList)
				{
					// OSFreeList is updated to the next address string in the current PCB
					// (OSFreeList is now equal to the start address of the next item in the linked list)
					OSFreeList = RAM[currentPCBptr];
					// The next address string of the current PCB is set to EOL since it no longer should be in the free list
					RAM[currentPCBptr] = EOL;
					// Return the start address of the current PCB
					return currentPCBptr;
				}
				// And the requested size is less than the free size...
				else
				{
					// Move the current next address string into the previous PCB's next address string
					RAM[previousPCBptr] = RAM[currentPCBptr];
					// Update the current PCBs next address string to EOL (disconnect from linked list)
					RAM[currentPCBptr] = EOL;
					// Return start address of current PCB
					return currentPCBptr;
				}
			}
			// Otherwise, if size of current PCB is more than the request size (PCB is bigger than needed)...
			else if(RAM[currentPCBptr + 1] > sizeRequest)
			{
				// And if the current PCB address is equal to the OSFreeList address...
				if(currentPCBptr == OSFreeList)
				{
					// Move the current PCB start address [sizeRequest] locations forward
					RAM[currentPCBptr + sizeRequest] = RAM[currentPCBptr];
					// Set the size of this new free block equal to the size of the original PCB minus what was requested
					RAM[currentPCBptr + sizeRequest + 1] = RAM[currentPCBptr + 1] - sizeRequest;
					// Update OSFreeList to point to the newly created PCB (disconnect from linked list)
					OSFreeList = currentPCBptr + sizeRequest;
					// Set the current PCB next address to EOL (disconnect from linked list)
					RAM[currentPCBptr] = EOL;
					// Return current PCB start address 
					return currentPCBptr;
				}
				// And the PCB address is not equal to OSFreeList...
				else
				{
					// Move the current PCB start address [sizeRequest] locations forward
					RAM[currentPCBptr + sizeRequest] = RAM[currentPCBptr];
					// Set the size of this new free block equal to the size of the original PCB minus what was requested
					RAM[currentPCBptr + sizeRequest + 1] = RAM[currentPCBptr + 1] - sizeRequest;
					// Update previous PCB to point to the start address of the new block
					RAM[previousPCBptr] = currentPCBptr + sizeRequest;
					// Set the current PCB next address to EOL (disconnect from linked list)
					RAM[currentPCBptr] = EOL;
					// Return current PCB start address 
					return currentPCBptr;
				}
			}
			// Otherwise, if the PCB size is less than the request size...
			else
			{
				// MOVE ONTO THE NEXT PCB
				// Store the current PCB start address in previous PCB start address
				previousPCBptr = currentPCBptr;
				// Set current PCB start address equal to the next address string, stored in first position of PCB
				// (currentPCB start address = next PCB start address)
				currentPCBptr = (int)RAM[currentPCBptr];
			}
		}
		
		// If reached, no block of necessary size was found. Throw error
		System.out.println("ERROR: No suitable block found in OS memory.");
		return NOSPACE;
	}
	
	// HW2 // 
	// *****
	// NAME : allocateUserMemory()
	// DESC : Dedicate a block of memory from User chunk (3000 - 5999)
	// INPT : Int size of block requested
	//	  (Int to prevent typcasting from long)
	// OUTP : Error code or start address of allocated block
	// RTRN : OK = >0 
	// 	  NOSPACE = -10
	// AUTH	: Ryan O'Connell
	// *****
	public long allocateUserMemory(int sizeRequest)
	{
		// If pointer goes to end of list, no space is left. Throw error.
		if(userFreeList == EOL)
		{
			System.out.println("ERROR: No suitable block found in OS memory.");
			return NOSPACE;
		}
		// If requested size is < 0, cannot allocate. Throw error.
		if(sizeRequest < 0)
		{
			System.out.println("ERROR: Memory size requested is invalid.");
			return INVALIDSIZE;
		}
		// Cannot allocate only one cell, discreetly bump to two.
		if(sizeRequest == 1)
		{
			sizeRequest = 2;
		}
		
		// Set the current start address pointer to userFreeList.
		int currentPCBptr = (int)userFreeList;
		// Set the start address pointer of the previous PCB to EOL.
		int previousPCBptr = EOL;
		
		// While the current start address pointer is not equal to EOL...
		while(currentPCBptr != EOL)
		{
			// If the current PCB size string is equal to the requested size...
			if(RAM[currentPCBptr + 1] == sizeRequest)
			{
				// And if the start address of the current PCB is equal to userFreeList...
				if(currentPCBptr == userFreeList)
				{
					// Store the next PCB start address in userFreeList
					userFreeList = RAM[currentPCBptr];
					// Set the current PCB's next address to EOL (removal from linked list)
					RAM[currentPCBptr] = EOL;
					// Return current PCB start address
					return currentPCBptr;
				}
				// Otherwise, if the start address of the PCB is NOT equal to userFreeList...
				else
				{
					// Move the current PCB's next address string into the previous PCB next address string
					RAM[previousPCBptr] = RAM[currentPCBptr];
					// Set the current PCB's next address to EOL (removal from linked list)
					RAM[currentPCBptr] = EOL;
					// Return current PCB start address
					return currentPCBptr;
				}
			}
			// Otherwise, if the PCB's size string is larger than the requested size (PCB bigger than needed)...
			else if(RAM[currentPCBptr + 1] > sizeRequest)
			{
				// And if the current PCB start address is equal to userFreeList...
				if(currentPCBptr == userFreeList)
				{
					// Move the current PCB next address [sizeRequest] indexes down (new PCB)
					RAM[currentPCBptr + sizeRequest] = RAM[currentPCBptr];
					// Set the new PCB's size equal to the size of the current PCB minus what was just used
					RAM[currentPCBptr + sizeRequest + 1] = RAM[currentPCBptr + 1] - sizeRequest;
					// Update userFreeList to begin at this new PCB start address
					userFreeList = currentPCBptr + sizeRequest;
					// Set the current PCB's next address to EOL (removal from linked list)
					RAM[currentPCBptr] = EOL;
					// Return current PCB start address
					return currentPCBptr;
				}
				// Otherwise, if the current PCB start address is NOT equal to userFreeList...
				else
				{
					// Move the current PCB next address [sizeRequest] indexes down (new PCB)
					RAM[currentPCBptr + sizeRequest] = RAM[currentPCBptr];
					// Set the new PCB's size equal to the size of the current PCB minus what was just used
					RAM[currentPCBptr + sizeRequest + 1] = RAM[currentPCBptr + 1] - sizeRequest;
					// Move the address of the new PCB into the previous PCB's next address string
					RAM[previousPCBptr] = currentPCBptr + sizeRequest;
					// Set the current PCB's next address to EOL (removal from linked list)
					RAM[currentPCBptr] = EOL;
					// Return current PCB start address
					return currentPCBptr;
				}
			}
			// Otherwise, if the PCB size is less than the request size...
			else
			{
				// MOVE ONTO THE NEXT PCB
				// Store the current PCB start address in previous PCB start address
				previousPCBptr = currentPCBptr;
				// Set current PCB start address equal to the next address string, stored in first position of PCB 
				// (currentPCB start address = next PCB start address)
				currentPCBptr = (int)RAM[currentPCBptr];
			}
		}
		
		// If reached, no block of necessary size was found. Throw error
		System.out.println("ERROR: No suitable block found in OS memory.");
		return NOSPACE;
	}

	// HW2 // 
	// *****
	// NAME : freeOSMemory()
	// DESC : Free [size] cells of memory beginning at RAM[ptr].
	//	  This method handles OSMemory field, indexes 6000 - 9999.
	// INPT : int ptr [start address of occupied memory, soon to be free block]
	//	  (int type prevents need for constant typecasting)
	//	  long size [length of occupied memory]
	// OUTP : Output code
	// RTRN : OK = 0
	//        BADSPACE = 10
	// 	  INVALIDSIZE = -11
	// AUTH	: Ryan O'Connell
	// *****
	public long freeOSMemory(int ptr, long size)
	{
		// If start address is outside of valid OS memory range
		// Throw invalid size error
		if(ptr < 6000 || ptr > 9999)
		{
			System.out.println("ERROR: Memory size requested is invalid.");
			return INVALIDSIZE;
		}
		
		// Cannot allocate only one cell. Bump to two.
		if(size == 1)
		{
			size = 2;
		}
		
		// If size is invalid (<0 or overflows OS memory)
		// Throw invalid size error
		else if(size < 1 || ptr + size >= 9999)
		{
			System.out.println("ERROR: Memory size requested is invalid.");
			return INVALIDSIZE;
		}
		
		// Insert the freed PCB into the beginning of the OS memory field
		
		// PCBNextAddress = the current start of the OSFreeList
		RAM[ptr] = OSFreeList;
		// PCBSize = the size asked to be freed by user
		RAM[ptr + 1] = size;
		// The new OSFreeList start index is the recently inserted PCB start index
		OSFreeList = ptr;
		
		// All went well
		return(OK);
	}
	
	// HW2 // 
	// *****
	// NAME : freeUserMemory()
	// DESC : Free [size] cells of memory beginning at RAM[ptr]. 
	//	  This method handles userMemory field, indexes 3000 - 5999.
	// INPT : int ptr [start address of occupied memory, soon to be free block]
	//	  (int type prevents need for constant typecasting)
	//	  long size [length of occupied memory]
	// OUTP : Output code
	// RTRN : OK = 0
	//        BADSPACE = 10
	// 	  INVALIDSIZE = -11
	// AUTH	: Ryan O'Connell
	// *****
	public long freeUserMemory(int ptr, long size)
	{
		// If start address is outside of valid user memory range
		// Throw invalid size error
		if(ptr < 3000 || ptr > 5999)
		{
			System.out.println("ERROR: Memory size requested is invalid.");
			return INVALIDSIZE;
		}
		
		// Cannot allocate only one cell. Bump to two.
		if(size == 1)
		{
			size = 2;
		}
		
		// If size is invalid (<0 or overflows free memory)
		// Throw invalid size error
		else if(size < 1 || ptr + size >= 5999)
		{
			System.out.println("ERROR: Memory size requested is invalid.");
			return INVALIDSIZE;
		}
		
		// Insert the freed PCB into the beginning of the free memory field
		
		// PCBNextAddress = the current start of the userFreeList
		RAM[ptr] = userFreeList;
		// PCBSize = the size asked to be freed by user
		RAM[ptr + 1] = size;
		// The new userFreeList start index is the recently inserted PCB start index
		userFreeList = ptr;
		
		// All went well
		return(OK);
	}

	// HW2 // 
	// *****
	// NAME : insertPCBintoReadyQueue()
	// DESC : Insert a PCB beginning at PCBptr into RQ
	// INPT : int PCBptr denotes beginning address of PCB to be inserted
	//	  (int type prevents need for constant typecasting)
	// OUTP : Output code
	// RTRN : OK = 0
	//	  BADSPACE = 10
	// 	  INVALIDSIZE = -11
	// AUTH	: Ryan O'Connell
	// *****
	public long insertPCBintoReadyQueue(int PCBptr)
	{
		int previousPCBptr = EOL;
		int currentPCBptr = (int)RQ;
		
		// If size is invalid (<0 or overflows free memory)
		// Throw invalid size error
		if(PCBptr < 6000 || PCBptr > 9999)
		{
			System.out.println("ERROR: Memory size requested is invalid.");
			return INVALIDSIZE;
		}
		
		// Set process state to ready
		RAM[PCBptr + State] = ReadyState;
		// Set the next process pointer address to EOL
		RAM[PCBptr + NextAddress] = EOL;
		
		// If RQ is EOL...
		if(RQ == EOL)
		{
			// Then RQ is empty. Insert PCB into slot one and exit
			RQ = PCBptr;
			return(OK);
		}
		
		// Otherwise, while the currentPCBptr isn't EOL...
		// (While we still have PCBs to look through)
		while(currentPCBptr != EOL)
		{
			// If the insertion PCB has a higher priority than the index PCB...
			if(RAM[PCBptr + Priority] > RAM[currentPCBptr + Priority])
			{
				// And the previous PCB's address was EOL...
				if(previousPCBptr == EOL)
				{
					// We're still at the start of the list. Enter PCB at start.
					// Change the insertion PCB's next address to the current RQ address
					RAM[PCBptr + NextAddress] = RQ;
					// Update RQ address to insertion PCB address. Then return
					RQ = PCBptr;
					return(OK);
				}
				// Otherwise, we insert in the middle of the list.
				else
				{
					// Set the insertion PCB's next address equal to the previous PCB's next address
					RAM[PCBptr + NextAddress] = RAM[previousPCBptr + NextAddress];
					// Set the previous PCB's next address to the insertion PCB's start address. Then return
					RAM[previousPCBptr + NextAddress] = PCBptr;
					return(OK);
				}
			}
			// Otherwise, go to the next PCB to check priority.
			else
			{
				previousPCBptr = currentPCBptr;
				currentPCBptr = (int)RAM[currentPCBptr + NextAddress];
			}
		}
		
		// If reached, reached end of list.
		// Add insertion PCB at end of list. No need to update next address. Then return
		RAM[previousPCBptr + NextAddress] = PCBptr;
		return(OK);
	}
	
	// HW2 // 
	// *****
	// NAME : insertPCBintoWaitQueue()
	// DESC : Insert a PCB beginning at PCBptr at position one in WQ
	// INPT : int PCBptr denotes beginning address of PCB to be inserted
	//		  (int type prevents need for constant typecasting)
	// OUTP : Output code
	// RTRN : OK = 0
	// 		  INVALIDSIZE = -11
	// AUTH	: Ryan O'Connell
	// *****
	public long insertPCBintoWaitQueue(int PCBptr)
	{
		// Insertion PCB always gets to go first.
		// If PCB start address is out of range (not in heap or OS mem)
		// Throw invalid size error, then return
		if(PCBptr < 3000 || PCBptr > 9999)
		{
			System.out.println("ERROR: Memory size requested is invalid.");
			return INVALIDSIZE;
		}
		
		// Set the PCB state to Waiting
		RAM[PCBptr + State] = WaitState;
		// Set inserted PCB's next address to EOL
		RAM[PCBptr + NextAddress] = WQ;
		
		// Update WQ to begin at PCB start address, then return
		WQ = PCBptr;
		
		return(OK);
	}
	
	// HW2 // 
	// *****
	// NAME : printGivenQueue()
	// DESC : Given a queue start address, print PCBs in queue.
	// INPT : int queuePtr [start address of occupied memory, soon to be free block]
	//		  (int type prevents need for constant typecasting)
	// OUTP : Given queue contents.
	// RTRN : OK = 0
	// AUTH	: Ryan O'Connell
	// *****
	public long printGivenQueue(int queuePtr)
	{
		// First PCB starts at the start address of the given queue.
		int currentPCBptr = queuePtr;
		
		// If the first PCB has no next address, the queue is empty.
		if(currentPCBptr == EOL)
		{
			// Display the queue is empty, then return.
			System.out.println("Given queue contains no PCBs.");
			return(OK);
		}
		
		// Otherwise, while the current PCB doesn't point to EOL...
		while(currentPCBptr != EOL)
		{
			// Print the PCB.
			printPCB(currentPCBptr);
			// Set the currentPCBptr equal to the next address pointer.
			currentPCBptr = (int)RAM[currentPCBptr + NextAddress];
		}
		
		// Return OK when out of PCBs.
		return(OK);
	}

	// HW2 // 
	// *****
	// NAME : selectFirstFromRQ()
	// DESC : Take the first process from the ready queue and have it enter running state
	// INPT : N/A
	// OUTP : N/A
	// RTRN : currentPCBptr (PCB start address)
	// AUTH	: Ryan O'Connell
	// *****
	public long selectFirstFromRQ()
	{
		// Get the first PCB start address, which begins at RQ
		// (int to avoid typcasting)
		int currentPCBptr = (int)RQ;
		
		// If it isn't EOL, take it out of the list
		if(RQ != EOL)
			// Update RQ to start at next PCB
			RQ = RAM[currentPCBptr + NextAddress];
		
		// Update the current PCB's next address to point to EOL
		RAM[currentPCBptr + NextAddress] = EOL;
		
		// Return start address
		return currentPCBptr;
	}
}

