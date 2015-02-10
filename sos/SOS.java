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
 * @author Jason Vanderwerf
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
    public static final int SYSCALL_OPEN = 3; /* access a device */
    public static final int SYSCALL_CLOSE = 4; /* release a device */
    public static final int SYSCALL_READ = 5; /* get input from device */
    public static final int SYSCALL_WRITE = 6; /* send output to device */
    public static final int SYSCALL_COREDUMP = 9; /*
                                                   * print process state and
                                                   * exit
                                                   */
    // Error codes for system calls
    public static final int SYSCALL_SUCCESS = 0; 
    public static final int SYSCALL_OPEN_ERROR = -3;
    public static final int SYSCALL_CLOSE_ERROR = -4;
    public static final int SYSCALL_READ_ERROR = -5;
    public static final int SYSCALL_WRITE_ERROR = -6;
    

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

    /**
     * Holds the current process.
     */
    private ProcessControlBlock m_currProcess = null;

    /**
     * 
     */
    private Vector<DeviceInfo> m_devices = null;

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
        m_currProcess = new ProcessControlBlock(42);
        m_devices = new Vector<DeviceInfo>();
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

    /**
     * Handles finding the systemCall that was made and executing the correct
     * system call. Receives the systemCall value by popping the top element off
     * of the stack.
     */
    public void systemCall() {

        int syscall = m_CPU.pop();

        switch (syscall) {
            case SYSCALL_EXIT:
                syscallExit();
                break;
            case SYSCALL_OUTPUT:
                syscallOutput();
                break;
            case SYSCALL_GETPID:
                syscallGetpid();
                break;
            case SYSCALL_OPEN:
                syscallOpen();
                break;
            case SYSCALL_CLOSE:
                syscallClose();
                break;
            case SYSCALL_READ:
                syscallRead();
                break;
            case SYSCALL_WRITE:
                syscallWrite();
                break;
            case SYSCALL_COREDUMP:
                syscallCoredump();
                break;

        }
    }

    /*
     * ======================================================================
     * Interrupts
     * ----------------------------------------------------------------------
     */
    
    /**
     * interuptIllegalMemoryAccess
     * 
     * Passes a string which is to be printed out via the errorMessage
     * method that contains the type of error and the address where the error
     * occurred. 
     * calls System.exit
     * 
     * @param addr   the address that was trying to be access that is illegal
     */
    public void interruptIllegalMemoryAccess(int addr) {
        errorMessage("Illegal Memory Access @: " + addr);
        System.exit(0);
    }
    
    /**
     * interruptDivideByZero
     * 
     * Passes a string which is to be printed out via the errorMessage 
     * method. 
     */
    public void interruptDivideByZero() {
        errorMessage("Divided By Zero");
        System.exit(0);
    }

    /**
     * interruptIllegalInstruction
     * 
     * Passes a string which is to be printed out via the errorMessage 
     * method. The message to be printed contains the instruction that was 
     * illegal. 
     * 
     * @param instr     the instruction that was illegal
     */
    public void interruptIllegalInstruction(int[] instr) {
        errorMessage("Illegal Instruction: " + instr[0] + " " + instr[1] + " "
                + instr[2] + " " + instr[3]);
        System.exit(0);
    }

    /**
     * Prints a given error message that is passed in with the prefix ERROR.
     * 
     * @param message
     *            a given error message for printing
     */
    public void errorMessage(String message) {
        System.out.println("ERROR: " + message);
    }

    /*
     * ======================================================================
     * System Call Methods
     * ----------------------------------------------------------------------
     */

    /* PRIVATE HELPER METHODS FOR SYS CALLS ********* */
    /**
     * Executes a system exit command
     */
    private void syscallExit() {
        System.exit(0);
    }

    /**
     * syscallOutput
     * 
     * This method pops a value off of the calling process' stack and then 
     * prints that value to the console. 
     */
    private void syscallOutput() {
        int value = m_CPU.pop();
        System.out.println("OUTPUT: " + value);
    }

    /**
     * syscallGetpid
     * 
     * Pushes the value of the current process ID onto the calling process' 
     * stack. 
     */
    private void syscallGetpid() {
        int pid = m_currProcess.getProcessId();
        m_CPU.push(pid);
    }

    /**
     * syscallOpen
     * 
     * Pop's the device number to be opened off of the calling process' stack 
     * and looks to see if a device with that device number exists. 
     * If a device with that number does not exists, a syscallError is made. 
     * If the device is found the following conditions are then checked: 
     *      - Is the device in use && not sharable. 
     *      - Is the device already open. 
     * If any of these cases are true, a syscallError is made.
     * Otherwise, the current process is added to the deviceInfo
     *      A syscallSuccess is then made.
     */
    private void syscallOpen() {
        int deviceNumber = m_CPU.pop();
        DeviceInfo deviceInfo = findDeviceInfo(deviceNumber);
        
        //If device was not found, or not sharable, or not available, error.
        if(deviceInfo == null){
            syscallError(SYSCALL_OPEN_ERROR);
            return;
        }
        
        //Can the device be accessed. Is it already open
        if ( (!deviceInfo.getDevice().isSharable() 
                && !deviceInfo.getDevice().isAvailable())
                || alreadyOpen(deviceInfo) ){
            syscallError(SYSCALL_OPEN_ERROR);
            return;
        }
        
        //add current process to device process list. 
        deviceInfo.addProcess(m_currProcess);
        syscallSuccess();
    }//syscallOpen
    
    /**
     * syscallClose
     * 
     * Pop's the device number to be opened off of the calling process' stack 
     * and looks to see if a device with that device number exists.
     * If a device with that number does not exists, a syscallError is made. 
     * If the device is found the following conditions are then checked:
     *      - Is the device not already open
     * If any of these cases are true, a syscallError is made.
     * Otherwise, the current process is removed from the deviceInfo
     *      A syscallSuccess is then made.
     */
    private void syscallClose() {
        int deviceNumber = m_CPU.pop();
        DeviceInfo deviceInfo = findDeviceInfo(deviceNumber);
        
        //If device was not found, or not open yet, error
        if(deviceInfo == null){
            syscallError(SYSCALL_CLOSE_ERROR);
            return;
        }
        
        //Is the device not already open
        if(!alreadyOpen(deviceInfo)){
            syscallError(SYSCALL_CLOSE_ERROR);
            return;
        }
        
        //remove current process from device process list. 
        deviceInfo.removeProcess(m_currProcess);
        syscallSuccess();
    }//syscallClose
    
    /**
     * syscallRead
     * 
     * Pop's the device number to be opened off of the calling process' stack 
     * and looks to see if a device with that device number exists.
     * If a device with that number does not exists, a syscallError is made. 
     * If the device is found the following conditions are then checked:
     *      - Is the device not already open
     *      - Is the device not readable 
     * If any of these cases are true, a syscallError is made.
     * Otherwise, a value is read in from the device, and the value is then 
     * pushed onto the calling process' stack. 
     *      A syscallSuccess is then made.
     */
    private void syscallRead() {
        int address = m_CPU.pop();
        int deviceNumber = m_CPU.pop();
        
        //Get device info 
        DeviceInfo deviceInfo = findDeviceInfo(deviceNumber);
        
        //if device was not found, or not open yet, error.
        if(deviceInfo == null){
            syscallError(SYSCALL_READ_ERROR);
            return;
        }
        
        //get device and read in value.
        Device device = deviceInfo.getDevice(); 
        
        //check ability to read
        if(!alreadyOpen(deviceInfo) || !device.isReadable()){
            syscallError(SYSCALL_READ_ERROR);
            return;
        }
        
        int value = device.read(address);
        
        //push value onto calling process' stack
        m_CPU.push(value);
        syscallSuccess();
    }//syscallRead

    /**
     * Pop data, address, and device id off the calling process's stack and 
     * retrieve the Device Object Associated with the device ID.
     */
    /**
     * syscallRead
     * 
     * Pops: 
     *  - data
     *  - address
     *  - device id 
     * off the calling process' stack.
     * Looks to see if a device with that device number exists.
     * If a device with that number does not exists, a syscallError is made.
     * The Device Object associated with the device ID is retrieved. 
     * If the device is found the following conditions are then checked:
     *      - Is the device not already open
     *      - Is the device not writable
     * If any of these cases are true, a syscallError is made.
     * Otherwise, the data that was passed in is written to the device, also 
     * passing in the address.  
     *      A syscallSuccess is then made.
     */
    private void syscallWrite() {
        int data = m_CPU.pop();
        int address = m_CPU.pop();
        int deviceNumber = m_CPU.pop();
        
        //Get device info and the actual device from the info
        DeviceInfo deviceInfo = findDeviceInfo(deviceNumber);
        
        //if device was not found, or not open yet, error.
        if(deviceInfo == null){
            syscallError(SYSCALL_WRITE_ERROR);
            return;
        }
        
        Device device = deviceInfo.getDevice();
        
        if(!alreadyOpen(deviceInfo) || !device.isWriteable()){
            syscallError(SYSCALL_WRITE_ERROR);
            return;
        }
        
         
        device.write(address, data);
        syscallSuccess();
    }//syscallWrite
    
    /**
     * syscallCoredump
     * 
     * Calls the CPU regDump function to provide debugging information. In
     * addition, prints to the console the top 3 things on the stack.
     * 
     * WARNING: If there are not three things on the stack, the pop function is
     * written such that it will not go beyond the limit of the program. This
     * means that there is a chance that an entry on the stack can be popped
     * multiple times, or whatever was at the base of the stack could be read,
     * even if there is nothing on the stack. THIS ONLY MATTERS IF THERE ARE
     * FEWER THAN 3 THINGS ON THE STACK.
     */
    private void syscallCoredump() {
        m_CPU.regDump();
        System.out.println("CORE DUMP, STACK: " + m_CPU.pop() + ", "
                + m_CPU.pop() + ", " + m_CPU.pop());
        syscallExit();
    }
    
    
    // SYS CALL HELPER METHODS
    
    /**
     * findDeviceInfo 
     * 
     * Checks if the device number passed in is a device number that is in the 
     * list of devices that we have access to. 
     * 
     * @param deviceNumber
     * @return deviceInfo for device number given
     *              IF device not found, return null. 
     */
    private DeviceInfo findDeviceInfo(int deviceNumber){
      
        //run through the list of devices to see if there is an id match
        for(int i = 0; i < m_devices.size(); i++){
            if(m_devices.get(i).getId() == deviceNumber ){
                DeviceInfo deviceInfo = m_devices.get(i);
                return deviceInfo;
            }
        }
        return null;
    }
    
    /**
     * syscallSuccess 
     * 
     * A helper method that pushes the SYSCALL_SUCCESS value onto the calling
     * process' stack
     */
    private void syscallSuccess(){
        m_CPU.push(SYSCALL_SUCCESS);
    }
    
    /**
     * syscallError
     * 
     * A helper method that pushes the errorCode passed in onto the calling 
     * process' stack
     * 
     * @param errorCode  The value of the error code that is to be reported.
     */
    private void syscallError(int errorCode){
        m_CPU.push(errorCode);
    }
    
    /**
     * alreadyOpen
     * 
     * Checks to see if a given device is already open by the current process.
     * This is done by checking if the deviceInfo process list already contains
     * the current process in the list. 
     * 
     * @param deviceInfo    the deviceInfo that is being checked. 
     * @return  true if the device is already open, false if it is not. 
     */
    private boolean alreadyOpen(DeviceInfo deviceInfo){
        if(deviceInfo.containsProcess(m_currProcess)){
            return true;
        }
        return false;
    }//alreadyOpen
    
    

    // ======================================================================
    // Inner Classes
    // ----------------------------------------------------------------------

    /**
     * class ProcessControlBlock
     * 
     * This class contains information about a currently active process.
     */
    private class ProcessControlBlock {
        /**
         * a unique id for this process
         */
        private int processId = 0;

        /**
         * constructor
         * 
         * @param pid
         *            a process id for the process. The caller is responsible
         *            for making sure it is unique.
         */
        public ProcessControlBlock(int pid) {
            this.processId = pid;
        }

        /**
         * @return the current process' id
         */
        public int getProcessId() {
            return this.processId;
        }

    }// class ProcessControlBlock

    /**
     * class DeviceInfo
     * 
     * This class contains information about a device that is currently
     * registered with the system.
     */
    private class DeviceInfo {
        /** every device has a unique id */
        private int id;
        /** a reference to the device driver for this device */
        private Device device;
        /** a list of processes that have opened this device */
        private Vector<ProcessControlBlock> procs;

        /**
         * constructor
         * 
         * @param d
         *            a reference to the device driver for this device
         * @param initID
         *            the id for this device. The caller is responsible for
         *            guaranteeing that this is a unique id.
         */
        public DeviceInfo(Device d, int initID) {
            this.id = initID;
            this.device = d;
            d.setId(initID);
            this.procs = new Vector<ProcessControlBlock>();
        }

        /** @return the device's id */
        public int getId() {
            return this.id;
        }

        /** @return this device's driver */
        public Device getDevice() {
            return this.device;
        }

        /** Register a new process as having opened this device */
        public void addProcess(ProcessControlBlock pi) {
            procs.add(pi);
        }

        /** Register a process as having closed this device */
        public void removeProcess(ProcessControlBlock pi) {
            procs.remove(pi);
        }

        /** Does the given process currently have this device opened? */
        public boolean containsProcess(ProcessControlBlock pi) {
            return procs.contains(pi);
        }

        /** Is this device currently not opened by any process? */
        public boolean unused() {
            return procs.size() == 0;
        }

    }// class DeviceInfo

    /*
     * ======================================================================
     * Device Management Methods
     * ----------------------------------------------------------------------
     */

    /**
     * registerDevice
     * 
     * adds a new device to the list of devices managed by the OS
     * 
     * @param dev
     *            the device driver
     * @param id
     *            the id to assign to this device
     * 
     */
    public void registerDevice(Device dev, int id) {
        m_devices.add(new DeviceInfo(dev, id));
    }// registerDevice

};// class SOS
