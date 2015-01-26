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

public class SOS {
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
     * Takes in a program and exports the program to an array of ints which 
     * are copied into RAM. The Base and Limit are set, and the Stack pointer
     * is set to the address of the Limit. 
     * 
     * @param prog      a program to be exported
     * @param allocSize the amount of memory that the program will need
     */
    public void createProcess(Program prog, int allocSize) {
        int[] programExport = prog.export();
        this.m_CPU.setBASE(4);
        this.m_CPU.setLIM(this.m_CPU.getBASE() + allocSize);

        int address = this.m_CPU.getBASE();
        for (int i = 0; i < programExport.length; ++i) {
            this.m_RAM.write(address + i, programExport[i]);
        }

        this.m_CPU.setSP(this.m_CPU.getLIM());

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

    // None yet!

};// class SOS
