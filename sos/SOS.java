package sos;

import java.util.*;

/**
 * This class contains the simulated operating system (SOS). Realistically it
 * would run on the same processor (CPU) that it is managing but instead it uses
 * the real-world processor in order to allow a focus on the essentials of
 * operating system design using a high level programming language.
 * 
 * @author Max Robinson
 * @author Connor Haas
 * 
 */

public class SOS implements CPU.TrapHandler {
    // ======================================================================
    // Constants
    // ----------------------------------------------------------------------

    // These constants define the system calls this OS can currently handle
    public static final int SYSCALL_EXIT = 0; /* exit the current program */
    public static final int SYSCALL_OUTPUT = 1; /* outputs a number */
    public static final int SYSCALL_GETPID = 2; /* get current process id */
    public static final int SYSCALL_COREDUMP = 9; /*
                                                   * print process state and
                                                   * exit
                                                   */

    // ======================================================================
    // Member variables
    // ----------------------------------------------------------------------

    /**
     * This flag causes the SOS to print lots of potentially helpful status
     * messages
     **/
    public static final boolean m_verbose = false;

    /**
     * The CPU the operating system is managing.
     **/
    private CPU m_CPU = null;

    /**
     * The RAM attached to the CPU.
     **/
    private RAM m_RAM = null;

    /*
     * ======================================================================
     * Constructors & Debugging
     * ----------------------------------------------------------------------
     */

    /**
     * The constructor does nothing special
     */
    public SOS(CPU c, RAM r) {
        // Init member list
        m_CPU = c;
        m_RAM = r;
        m_CPU.registerTrapHandler(this);
    }// SOS ctor

    /**
     * Does a System.out.print as long as m_verbose is true
     **/
    public static void debugPrint(String s) {
        if (m_verbose) {
            System.out.print(s);
        }
    }

    /**
     * Does a System.out.println as long as m_verbose is true
     **/
    public static void debugPrintln(String s) {
        if (m_verbose) {
            System.out.println(s);
        }
    }

    /*
     * ======================================================================
     * Memory Block Management Methods
     * ----------------------------------------------------------------------
     */

    // None yet!

    /*
     * ======================================================================
     * Device Management Methods
     * ----------------------------------------------------------------------
     */

    // None yet!

    /*
     * ======================================================================
     * Process Management Methods
     * ----------------------------------------------------------------------
     */

    // None yet!

    /*
     * ======================================================================
     * Program Management Methods
     * ----------------------------------------------------------------------
     */

    /**
     * Takes in a program and exports the program to an array of ints which are
     * copied into RAM. The Base and Limit are set, and the Stack pointer is set
     * to the address of the Limit.
     * 
     * @param prog
     *            a program to be exported
     * @param allocSize
     *            the amount of memory that the program will need
     */
    public void createProcess(Program prog, int allocSize) {
        int[] programExport = prog.export();
        this.m_CPU.setBASE(17);
        this.m_CPU.setLIM(this.m_CPU.getBASE() + allocSize);
        this.m_CPU.setSP(this.m_CPU.getLIM());
        this.m_CPU.setPC(this.m_CPU.getBASE());

        int address = this.m_CPU.getBASE();
        for (int i = 0; i < programExport.length; ++i) {
            this.m_RAM.write(address + i, programExport[i]);
        }

        

    }// createProcess

    /*
     * ======================================================================
     * Interrupt Handlers
     * ----------------------------------------------------------------------
     */

    // None yet!

    /*
     * ======================================================================
     * System Calls
     * ----------------------------------------------------------------------
     */

    // <insert header comment here>
    public void systemCall() {
        // %%%REPLACE THESE LINES WITH APPROPRIATE CODE
        //System.out.println("TRAP handled!");
        
        int syscall = m_CPU.pop();
        
        switch(syscall){
            case SYSCALL_EXIT:
                syscallExit();
                break;
            case SYSCALL_OUTPUT:
                syscallOutput();
                break;
            case SYSCALL_GETPID:
                syscallGetpid();
                break;
            case SYSCALL_COREDUMP:
                syscallCoredump();
                break;
                
        }
        
//        System.exit(0);
    }

    public void interruptIllegalMemoryAccess(int addr){
    	errorMessage("Illegal Memory Access @: " + addr);
    	System.exit(0);
    }

    public void interruptDivideByZero() {
        errorMessage("Divided By Zero");
        System.exit(0);
    }

    public void interruptIllegalInstruction(int[] instr) {
        errorMessage("Illegal Instruction: " + instr[0] + " " + instr[1] + " "
                + instr[2] + " " + instr[3]);
        System.exit(0);
    }

    public void errorMessage(String message) {
        System.out.println("ERROR: " + message);
    }
    
    
    /* PRIVATE HELPER METHODS FOR SYS CALLS ********* */
    
    
    private void syscallExit(){
        System.exit(0);
    }
    
    private void syscallOutput(){
        int value = m_CPU.pop();
        System.out.println("OUTPUT: " + value);
    }
    
    private void syscallGetpid(){
        m_CPU.push(42);
    }
    
    private void syscallCoredump(){
        m_CPU.regDump();
        System.out.println("CORE DUMP, STACK: " + m_CPU.pop()+ ", "+ m_CPU.pop()
                + ", " + m_CPU.pop());
        syscallExit();
    }
};// class SOS
