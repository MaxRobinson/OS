package sos;

import java.util.*;

/**
 * This class is the centerpiece of a simulation of the essential hardware of a
 * microcomputer. This includes a processor chip, RAM and I/O devices. It is
 * designed to demonstrate a simulated operating system (SOS).
 * 
 * @author Max Robinson
 * @author Connor Haas
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
    private void regDump() {
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
        //adjust starting PC register
        this.m_registers[PC] += this.getBASE();
        
        while (true) {
            int[] instr = this.m_RAM.fetch(this.m_registers[PC]);

            if (m_verbose == true) {
                regDump();
                printInstr(instr);
            }

            int physicalAddress;

            // Decode and execute
            switch (instr[0]) {
            case SET:
                this.m_registers[instr[1]] = instr[2];
                incrementPC();
                break;
            case ADD:
                this.m_registers[instr[1]] = this.m_registers[instr[2]]
                        + this.m_registers[instr[3]];
                incrementPC();
                break;
            case SUB:          
                this.m_registers[instr[1]] = this.m_registers[instr[2]]
                        - this.m_registers[instr[3]];
                incrementPC();
                break;
            case MUL:
                this.m_registers[instr[1]] = this.m_registers[instr[2]]
                        * this.m_registers[instr[3]];
                incrementPC();
                break;
            case DIV:
                this.m_registers[instr[1]] = this.m_registers[instr[2]]
                        / this.m_registers[instr[3]];
                incrementPC();
                break;
            case COPY:
                this.m_registers[instr[1]] = this.m_registers[instr[2]];
                incrementPC();
                break;
            case BRANCH:
                physicalAddress = this.adjustOffset(instr[1]);
                if (checkAddress(physicalAddress)) {
                    this.setPC(physicalAddress);
                } else {
                    return;
                }
                break;
            case BNE:
                if (this.m_registers[instr[1]] != this.m_registers[instr[2]]) {
                    physicalAddress = this.adjustOffset(instr[3]);
                    if (checkAddress(physicalAddress)) {
                        this.setPC(physicalAddress);
                    } else {
                        return;
                    }
                } else {
                    incrementPC();
                }
                break;
            case BLT:
                if (this.m_registers[instr[1]] < this.m_registers[instr[2]]) {
                    physicalAddress = this.adjustOffset(instr[3]);
                    if (checkAddress(physicalAddress)) {
                        this.setPC(physicalAddress);
                    } else {
                        return;
                    }
                } else {
                    incrementPC();
                }
                break;
            case POP:
                this.m_registers[instr[1]] = this.pop();
                incrementPC();
                break;
            case PUSH:
                this.push(this.m_registers[instr[1]]);
                incrementPC();
                break;
            case LOAD:
                physicalAddress = this.adjustOffset(this.m_registers[instr[2]]);
                if (checkAddress(physicalAddress)) {
                    this.m_registers[instr[1]] = this.m_RAM
                            .read(physicalAddress);
                } else {
                    return;
                }
                incrementPC();
                break;
            case SAVE:
                physicalAddress = this.adjustOffset(this.m_registers[instr[2]]);
                if (checkAddress(physicalAddress)) {
                    this.m_RAM.write(physicalAddress,
                            this.m_registers[instr[1]]);
                } else {
                    return;
                }
                incrementPC();
                break;
            case TRAP:
                return;
            default: // should never be reached
                System.out.println("?? ");
                break;
            }// switch
        }// while
    }// run

    /**
     * Pass in register that holds an address value and check to make sure that
     * that address is inside the Base and Limit Addresses.
     * 
     * @param address An already adjusted address
     * @return true if the address is between the base and limit addresses 
     *              inclusive.
     */
    public boolean checkAddress(int address) {
        if (address >= this.getBASE() && address <= this.getLIM()) {
            return true;
        }

        return false;
    }

    public void incrementPC() {
        this.setPC(this.getPC() + INSTRSIZE);
    }

    public void decrementSP() {
        this.setSP(this.getSP() - SPINCREMENT);
    }

    public void incrementSP() {
        this.setSP(this.getSP() + SPINCREMENT);
    }
    
    public int adjustOffset(int value) {
        return value + this.getBASE();
    }

    /**
     * Writes the value given to the current location of the Stack pointer in
     * RAM and then decrements the Stack pointer.
     * 
     * @param value
     */
    public void push(int value) {
        this.m_RAM.write(this.getSP(), value);
        decrementSP();
    }
    
    /**
     * First increments the Stack pointer, and then reads the value from the 
     * address of the stack pointer in RAM and returns that value
     * 
     * @return value
     */
    public int pop() {
        incrementSP();
        int value = this.m_RAM.read(this.getSP());
        return value;
    }

};// class CPU
