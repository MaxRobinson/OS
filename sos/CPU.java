package sos;

import java.util.*;

/**
 * This class is the centerpiece of a simulation of the essential hardware of a
 * microcomputer. This includes a processor chip, RAM and I/O devices. It is
 * designed to demonstrate a simulated operating system (SOS).
 * 
 * @author Max Robinson
 * @author Connor Haas
 * @author Bryce Matsuda
 * @author Jordan White
 * 
 * @see RAM
 * @see SOS
 * @see Program
 * @see Sim
 */

public class CPU {

    // ======================================================================
    // Constants
    // ----------------------------------------------------------------------

    // These constants define the instructions available on the chip
    public static final int SET = 0; /* set value of reg */
    public static final int ADD = 1; // put reg1 + reg2 into reg3
    public static final int SUB = 2; // put reg1 - reg2 into reg3
    public static final int MUL = 3; // put reg1 * reg2 into reg3
    public static final int DIV = 4; // put reg1 / reg2 into reg3
    public static final int COPY = 5; // copy reg1 to reg2
    public static final int BRANCH = 6; // goto address in reg
    public static final int BNE = 7; // branch if not equal
    public static final int BLT = 8; // branch if less than
    public static final int POP = 9; // load value from stack
    public static final int PUSH = 10; // save value to stack
    public static final int LOAD = 11; // load value from heap
    public static final int SAVE = 12; // save value to heap
    public static final int TRAP = 15; // system call

    // These constants define the indexes to each register
    public static final int R0 = 0; // general purpose registers
    public static final int R1 = 1;
    public static final int R2 = 2;
    public static final int R3 = 3;
    public static final int R4 = 4;
    public static final int PC = 5; // program counter
    public static final int SP = 6; // stack pointer
    public static final int BASE = 7; // bottom of currently accessible RAM
    public static final int LIM = 8; // top of accessible RAM
    public static final int NUMREG = 9; // number of registers

    // Misc constants
    public static final int NUMGENREG = PC; // the number of general registers
    public static final int INSTRSIZE = 4; // number of ints in a single instr +
                                           // args. (Set to a fixed value
                                           // for simplicity.)
    public static final int SPINCREMENT = 1;
    

    // ======================================================================
    // Member variables
    // ----------------------------------------------------------------------
    /**
     * specifies whether the CPU should output details of its work
     **/
    private boolean m_verbose = true;

    /**
     * This array contains all the registers on the "chip".
     **/
    private int m_registers[];

    /**
     * A pointer to the RAM used by this CPU
     * 
     * @see RAM
     **/
    private RAM m_RAM = null;

    // ======================================================================
    // Methods
    // ----------------------------------------------------------------------

    /**
     * CPU ctor
     * 
     * Intializes all member variables.
     */
    public CPU(RAM ram) {
        m_registers = new int[NUMREG];
        for (int i = 0; i < NUMREG; i++) {
            m_registers[i] = 0;
        }
        m_RAM = ram;

    }// CPU ctor

    /**
     * getPC
     * 
     * @return the value of the program counter
     */
    public int getPC() {
        return m_registers[PC];
    }

    /**
     * getSP
     * 
     * @return the value of the stack pointer
     */
    public int getSP() {
        return m_registers[SP];
    }

    /**
     * getBASE
     * 
     * @return the value of the base register
     */
    public int getBASE() {
        return m_registers[BASE];
    }

    /**
     * getLIMIT
     * 
     * @return the value of the limit register
     */
    public int getLIM() {
        return m_registers[LIM];
    }

    /**
     * getRegisters
     * 
     * @return the registers
     */
    public int[] getRegisters() {
        return m_registers;
    }

    /**
     * setPC
     * 
     * @param v
     *            the new value of the program counter
     */
    public void setPC(int v) {
        m_registers[PC] = v;
    }

    /**
     * setSP
     * 
     * @param v
     *            the new value of the stack pointer
     */
    public void setSP(int v) {
        m_registers[SP] = v;
    }

    /**
     * setBASE
     * 
     * @param v
     *            the new value of the base register
     */
    public void setBASE(int v) {
        m_registers[BASE] = v;
    }

    /**
     * setLIM
     * 
     * @param v
     *            the new value of the limit register
     */
    public void setLIM(int v) {
        m_registers[LIM] = v;
    }

    /**
     * regDump
     * 
     * Prints the values of the registers. Useful for debugging.
     */
    public void regDump() {
        for (int i = 0; i < NUMGENREG; i++) {
            System.out.print("r" + i + "=" + m_registers[i] + " ");
        }// for
        System.out.print("PC=" + m_registers[PC] + " ");
        System.out.print("SP=" + m_registers[SP] + " ");
        System.out.print("BASE=" + m_registers[BASE] + " ");
        System.out.print("LIM=" + m_registers[LIM] + " ");
        System.out.println("");
    }// regDump

    /**
     * printIntr
     * 
     * Prints a given instruction in a user readable format. Useful for
     * debugging.
     * 
     * @param instr
     *            the current instruction
     */
    public static void printInstr(int[] instr) {
        switch (instr[0]) {
        case SET:
            System.out.println("SET R" + instr[1] + " = " + instr[2]);
            break;
        case ADD:
            System.out.println("ADD R" + instr[1] + " = R" + instr[2] + " + R"
                    + instr[3]);
            break;
        case SUB:
            System.out.println("SUB R" + instr[1] + " = R" + instr[2] + " - R"
                    + instr[3]);
            break;
        case MUL:
            System.out.println("MUL R" + instr[1] + " = R" + instr[2] + " * R"
                    + instr[3]);
            break;
        case DIV:
            System.out.println("DIV R" + instr[1] + " = R" + instr[2] + " / R"
                    + instr[3]);
            break;
        case COPY:
            System.out.println("COPY R" + instr[1] + " = R" + instr[2]);
            break;
        case BRANCH:
            System.out.println("BRANCH @" + instr[1]);
            break;
        case BNE:
            System.out.println("BNE (R" + instr[1] + " != R" + instr[2] + ") @"
                    + instr[3]);
            break;
        case BLT:
            System.out.println("BLT (R" + instr[1] + " < R" + instr[2] + ") @"
                    + instr[3]);
            break;
        case POP:
            System.out.println("POP R" + instr[1]);
            break;
        case PUSH:
            System.out.println("PUSH R" + instr[1]);
            break;
        case LOAD:
            System.out.println("LOAD R" + instr[1] + " <-- @R" + instr[2]);
            break;
        case SAVE:
            System.out.println("SAVE R" + instr[1] + " --> @R" + instr[2]);
            break;
        case TRAP:
            System.out.print("TRAP ");
            break;
        default: // should never be reached
            System.out.println("?? ");
            break;
        }// switch

    }// printInstr

    /**
     * This method is the main run method for the CPU. 
     * It first adjusts the PC pointer to the offset amount, 
     * then continuously fetches instructions to be decoded and executed. 
     * If verbose mode is on, it will call the regDump() and printInstr() 
     * methods that are above. 
     */
    public void run() {
        while (true) {
            
            // Get first instruction
            int[] instr = m_RAM.fetch(getPC());
            
            // Point PC at next instruction
            if(checkAddress(getPC() + INSTRSIZE)) incrementPC();
            
            // Print commands to console
            if (m_verbose == true) {
                regDump();
                printInstr(instr);
            }

            // Offset address for branch instructions
            int physicalAddress;

            // Decode and execute
            switch (instr[0]) {

                // register = immediate
                case SET:
                    ifBadInstr(instr, 3);
                    m_registers[instr[1]] = instr[2];
                    break;
                    
                // register = register + register
                case ADD:
                    m_registers[instr[1]] = m_registers[instr[2]]
                            + m_registers[instr[3]];
                    break;
                    
    
                // register = register - register
                case SUB:          
                    m_registers[instr[1]] = m_registers[instr[2]]
                            - m_registers[instr[3]];
                    break;
    
                // register = register * register    
                case MUL:
                    m_registers[instr[1]] = m_registers[instr[2]]
                            * m_registers[instr[3]];
                    break;
                    
    
                // register = register / register    
                case DIV:
                    if(m_registers[instr[3]] == 0){
                        m_TH.interruptDivideByZero();
                    }
                    m_registers[instr[1]] = m_registers[instr[2]]
                            / m_registers[instr[3]];
                    break;
                 
                // register1 = register2   
                case COPY:
                    ifBadInstr(instr, 3);
                    m_registers[instr[1]] = m_registers[instr[2]];
                    break;
                    
                // Go to addr
                case BRANCH:
                    ifBadInstr(instr, 2);
                    ifBadInstr(instr, 3);
                    physicalAddress = adjustOffset(instr[1]);
                    
                    // Check valid address
                    if (checkAddress(physicalAddress)) {
                        setPC(physicalAddress);
                    } else {
                        m_TH.interruptIllegalMemoryAccess(instr[1]);
                    }
                    break;
                    
                // Go to addr if reg1 != reg2  
                case BNE:
                    if (m_registers[instr[1]] != m_registers[instr[2]]) {
                        physicalAddress = adjustOffset(instr[3]);
                 
                        // Check valid address
                        if (checkAddress(physicalAddress)) {
                            setPC(physicalAddress);
                        } else {
                            m_TH.interruptIllegalMemoryAccess(instr[3]);
                        }
                    }
                    break;
                    
                 // Go to addr if reg1 >= reg2      
                case BLT:
                    if (m_registers[instr[1]] < m_registers[instr[2]]) {
                        physicalAddress = adjustOffset(instr[3]);
    
                        // Check valid address
                        if (checkAddress(physicalAddress)) {
                            setPC(physicalAddress);
                        } else {
                            m_TH.interruptIllegalMemoryAccess(instr[3]);
                        }
                    }
                    break;
                    
                // pop first on stack    
                case POP:
                    ifBadInstr(instr, 2);
                    ifBadInstr(instr, 3);
                    m_registers[instr[1]] = pop();
                    break;
                
                // push onto stack    
                case PUSH:
                    ifBadInstr(instr, 2);
                    ifBadInstr(instr, 3);
                    push(m_registers[instr[1]]);
                    break;
                
                case LOAD:
                    ifBadInstr(instr, 3);
                    physicalAddress = adjustOffset(m_registers[instr[2]]);
                    if (checkAddress(physicalAddress)) {
                        m_registers[instr[1]] = m_RAM.read(physicalAddress);
                    } else {
                        return;
                    }
                    break;
                
                case SAVE:
                    ifBadInstr(instr, 3);
                    physicalAddress = adjustOffset(m_registers[instr[2]]);
                    if (checkAddress(physicalAddress)) {
                        m_RAM.write(physicalAddress, m_registers[instr[1]]);
                    } else {
                        return;
                    }
                    break;
                
                case TRAP:
                    ifBadInstr(instr, 1);
                    ifBadInstr(instr, 2);
                    ifBadInstr(instr, 3);
                	m_TH.systemCall();
                	break;
                
                default: // should never be reached
                    m_TH.interruptIllegalInstruction(instr);
                    System.out.println("?? ");
                    break;
            }// switch
        }// while
    }// run

    /**
     * Pass in register that holds an address value and check to make sure that
     * that address is inside the Base and Limit Addresses.
     * If outside, then make an interrupt call.
     * 
     * @param address An already adjusted address
     * @return true if the address is between the base and limit addresses 
     *              inclusive.
     */
    public boolean checkAddress(int address) {
        if (address >= getBASE() && address <= getLIM()) {
            return true;
        }
        
        m_TH.interruptIllegalMemoryAccess(address);
        return false;
    }

    private void incrementPC() {
        setPC(getPC() + INSTRSIZE);
    }

    private void decrementSP() {
        if(getSP() - SPINCREMENT > getBASE()){
            setSP(getSP() - SPINCREMENT);
        }
    }

    private void incrementSP() {
        if(getSP() + SPINCREMENT < getLIM()){
            setSP(getSP() + SPINCREMENT);
        }
    }
    
    private int adjustOffset(int value) {
        return value + getBASE();
    }

    /**
     * Writes the value given to the current location of the Stack pointer in
     * RAM and then decrements the Stack pointer.
     * 
     * @param value
     */
    public void push(int value) {
        decrementSP();
        m_RAM.write(getSP(), value);
    }
    
    /**
     * First increments the Stack pointer, and then reads the value from the 
     * address of the stack pointer in RAM and returns that value
     * 
     * @return value
     */
    public int pop() {
        int value = m_RAM.read(getSP());
        incrementSP();
        return value;
    }

    private void ifBadInstr(int instr[], int instrNumber){
        /*if(Arrays.equals(instr, new int[] {0, 0, 0, 0})) return;
        if(instr[instrNumber] != 99999){ 
            m_TH.interruptIllegalInstruction(instr);
        }*/
    }
    
    //======================================================================
    //Callback Interface
    //----------------------------------------------------------------------
    /**
     * TrapHandler
     *
     * This interface should be implemented by the operating system to allow the
     * simulated CPU to generate hardware interrupts and system calls.
     */
    public interface TrapHandler
    {
        void interruptIllegalMemoryAccess(int addr);
        void interruptDivideByZero();
        void interruptIllegalInstruction(int[] instr);
        void systemCall();
    };//interface TrapHandler


    
    /**
     * a reference to the trap handler for this CPU.  On a real CPU this would
     * simply be an address that the PC register is set to.
     */
    private TrapHandler m_TH = null;



    /**
     * registerTrapHandler
     *
     * allows SOS to register itself as the trap handler 
     */
    public void registerTrapHandler(TrapHandler th)
    {
        m_TH = th;
    }
    
};// class CPU

