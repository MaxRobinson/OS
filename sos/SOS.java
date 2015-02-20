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
 * @author Davis Achong
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
    public static final int SYSCALL_EXEC    = 7;    /* spawn a new process */
    public static final int SYSCALL_YIELD   = 8;    /* yield the CPU to another process */
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
    
    // Error Codes
    public static final int ERROR_NOT_ENOUGH_SPACE = -1;
    public static final int ERROR_NO_PROCESSES = -1;
    

    // ======================================================================
    // Member variables
    // ----------------------------------------------------------------------

    /**
     * This flag causes the SOS to print lots of potentially helpful status
     * messages
     **/
    public static final boolean m_verbose = true;

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
     * Holds list of registered devices
     */
    private Vector<DeviceInfo> m_devices = null;
    
    /**
     * Holds a vector of PROGRAM Objects that are available to the OS
     */
    private Vector<Program> m_programs = null;
    
    /**
     * Holds the position where the next program will be loaded.
     * *NOTE: is increased by the size of that program's address space 
     * each time a process is created.
     */
    private int m_nextLoadPos = 0;
    
    /**
     * Specifies the id that will be assigned to the next process that is
     * loaded. 
     * *NOTE: each time variable is used it should be incremented
     */
    private int m_nextProcessID = 1001;
    
    /**
     * List of all of the processes currently loaded into RAM that are in 
     * one of the major states (Ready, Running, or Blocked).
     * This is the process table for SOS. 
     */
    private Vector<ProcessControlBlock> m_processes = null;
    

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
        m_currProcess = null;//This needs to be changed ***
        m_devices = new Vector<DeviceInfo>();
        m_programs = new Vector<Program>();
        m_nextLoadPos = m_CPU.getBASE();
        m_nextProcessID = 1001;
        m_processes = new Vector<ProcessControlBlock>();
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

    /*
     * ======================================================================
     * Process Management Methods
     * ----------------------------------------------------------------------
     */

    /**
     * printProcessTable      **DEBUGGING**
     *
     * prints all the processes in the process table
     */
    private void printProcessTable()
    {
        debugPrintln("");
        debugPrintln("Process Table (" + m_processes.size() + " processes)");
        debugPrintln("======================================================================");
        for(ProcessControlBlock pi : m_processes)
        {
            debugPrintln("    " + pi);
        }//for
        debugPrintln("----------------------------------------------------------------------");

    }//printProcessTable

    /**
     * removeCurrentProcess
     * 
     * Remove the current Process from the process list and schedule a new
     * process to run.
     */
    public void removeCurrentProcess()
    {
        //debug
        debugPrintln("Removing process with id " 
                + m_currProcess.getProcessId() + " at " + m_CPU.getBASE());
        
        m_processes.remove(m_currProcess);
        scheduleNewProcess();
    }//removeCurrentProcess

    /**
     * getRandomProcess
     *
     * selects a non-Blocked process at random from the ProcessTable.
     *
     * @return a reference to the ProcessControlBlock struct of the selected process
     * -OR- null if no non-blocked process exists
     */
    ProcessControlBlock getRandomProcess()
    {
        //Calculate a random offset into the m_processes list
        int offset = ((int)(Math.random() * 2147483647)) % m_processes.size();
            
        //Iterate until a non-blocked process is found
        ProcessControlBlock newProc = null;
        for(int i = 0; i < m_processes.size(); i++)
        {
            newProc = m_processes.get((i + offset) % m_processes.size());
            if ( ! newProc.isBlocked())
            {
                return newProc;
            }
        }//for

        return null;        // no processes are Ready
    }//getRandomProcess
    
    /**
     * scheduleNewProcess
     * 
     * Checks to see if there are any more processes that need to be scheduled
     * If there are, it selects a random process to switch to. 
     * If the process selected is the same as the current process return to 
     * continue execution of that process, without a control switch. 
     * 
     * If different, Save the current process(If one exists) and then switch
     * the current process to the new process. Restore the cpu to that 
     * process' state before returning. 
     * 
     */
    public void scheduleNewProcess()
    {
        //check if any process available
        if(m_processes.size() == 0){
            debugPrintln("No more processes to run.  Stopping.");
            System.exit(ERROR_NO_PROCESSES);
        }
        
        //Select random process. If no non-blocked processes found, return
        ProcessControlBlock newProcess = getRandomProcess();
        if(newProcess == null){
            return;
        }
        
        //Check if the process selected has the same Process ID as current Proc.
        if(newProcess.getProcessId() == m_currProcess.getProcessId()){
            return;
        }
        
        //If a process is currently running, save the process
        if(m_currProcess != null){
            m_currProcess.save(m_CPU);
        }
        
        //set new current process
        m_currProcess = newProcess;
        
        debugPrintln("Switched to process " + m_currProcess.getProcessId());
        
        //restore the cpu values for this process.
        m_currProcess.restore(m_CPU);

    }//scheduleNewProcess

    /**
     * addProgram
     *
     * registers a new program with the simulated OS that can be used when the
     * current process makes an Exec system call.  (Normally the program is
     * specified by the process via a filename but this is a simulation so the
     * calling process doesn't actually care what program gets loaded.)
     *
     * @param prog  the program to add
     *
     */
    public void addProgram(Program prog)
    {
        m_programs.add(prog);
    }//addProgram

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
        
        //Check if enough space to put program
        if(allocSize >= m_RAM.getSize() - m_nextLoadPos){
            System.out.println("ERROR: Not enough space!");
            System.exit(ERROR_NOT_ENOUGH_SPACE);
        }
        
        int[] programExport = prog.export();
        
        //if process running. Save the process
        if(m_currProcess != null){
            m_currProcess.save(m_CPU);
        }

        //use nextLoadPosition to place process
        this.m_CPU.setBASE(m_nextLoadPos);
        this.m_CPU.setLIM(this.m_CPU.getBASE() + allocSize);
        this.m_CPU.setSP(this.m_CPU.getLIM());
        this.m_CPU.setPC(this.m_CPU.getBASE());
        
        //write program to memory
        int address = this.m_CPU.getBASE();
        for (int i = 0; i < programExport.length; ++i) {
            this.m_RAM.write(address + i, programExport[i]);
        }
        
        //set the nextLoadPosition as 1 instruction above limit
        //TODO: Make a helper method? 
        m_nextLoadPos = m_CPU.getLIM() + m_CPU.INSTRSIZE;
        
        //create new process control block and add that to list of processes
        ProcessControlBlock newProcess = 
                new ProcessControlBlock(m_nextProcessID);
        m_processes.add(newProcess);
        
        //Debug!
        debugPrintln("Installed program of size " + allocSize 
                + " with process id " + m_nextProcessID +" at "
                + m_CPU.getBASE());
        
        //increment PROCESS ID
        incrementNextProcessID();
        
        //set the current process to the newly made process
        m_currProcess = m_processes.get(m_processes.size()-1);
        
    }// createProcess
    
    
    /*
     * =============================
     * PROGRAM MANAGEMENT HELPER METHODS
     * -----------------------------
     */
    private void incrementNextProcessID(){
        m_nextProcessID += 1;
    }

    /*
     * ======================================================================
     * Interrupt Handlers
     * ----------------------------------------------------------------------
     */

    // None yet!
    
    
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
            case SYSCALL_EXEC:
                syscallExec();
                break;
            case SYSCALL_YIELD:
                syscallYield();
                break;
            case SYSCALL_COREDUMP:
                syscallCoredump();
                break;

        }
    }

    /*
     * ======================================================================
     * System Call Methods
     * ----------------------------------------------------------------------
     */

    /* ********PRIVATE HELPER METHODS FOR SYS CALLS ********* */
    /**
     * Executes a system exit command
     */
    private void syscallExit() {
        removeCurrentProcess();
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
     * If the device is not sharable the process is added to the deviceInfo 
     * and then the current process is blocked. After Blocking a new process is
     * selected to run. 
     * If the device is already open, error.
     *      A syscallSuccess is then made.
     *      
     */
    private void syscallOpen() {
        //Get Device ID
        int deviceNumber = m_CPU.pop();

        DeviceInfo deviceInfo = findDeviceInfo(deviceNumber);
        
        //Add the current process to the device to indicate that it's using 
        //the device, If it exists
        if(deviceInfo != null){
            
            //if you've not opened it yet
            if(!alreadyOpen(deviceInfo)){
                
                //check if device is sharable if another process is using it
                if(deviceInfo.getDevice().isSharable() || deviceInfo.unused()){
                    deviceInfo.addProcess(m_currProcess);
                    syscallSuccess();
                }
                else{
                    //Device is not sharable and is being used
                    //Add the process to the list, and block current process
                    deviceInfo.addProcess(m_currProcess);
                    m_currProcess.block(m_CPU, deviceInfo.getDevice(),
                            SYSCALL_OPEN, 0);
                    syscallSuccess();
                    scheduleNewProcess();
                }
            }
            else{
                //Device Already Open!
                syscallError(SYSCALL_OPEN_ERROR);
            }
        }
        else{
            //DeviceInfo is NULL
            syscallError(SYSCALL_OPEN_ERROR);
        }
    }//syscallOpen
    
    /**
     * syscallClose
     * 
     * Pop's the device number to be opened off of the calling process' stack 
     * and looks to see if a device with that device number exists.
     * If a device with that number does not exists, a syscallError is made. 
     * If the device is found the following conditions are then checked:
     *      - Is the device not already open
     * If the device is not already open, Error.  
     * Otherwise, the current process is removed from the deviceInfo, and an
     * already running process is selected that is blocked, and is unblocked.
     *      A syscallSuccess is then made.
     */
    private void syscallClose() {
        
        //Get Device ID
        int deviceNumber = m_CPU.pop();
        
        DeviceInfo deviceInfo = findDeviceInfo(deviceNumber);
        
        if(deviceInfo != null){
            
            //if we're trying to close a device that's actually open
            if(alreadyOpen(deviceInfo)){
                deviceInfo.removeProcess(m_currProcess);
                
                //unblock any process that is waiting for this device
                ProcessControlBlock waitingProcess = 
                        selectBlockedProcess(deviceInfo.device,SYSCALL_OPEN,0);
                
                //if not null unblock process
                if(waitingProcess != null){
                    waitingProcess.unblock();
                }
                
                syscallSuccess();
            }
            else{
                //device is not open!
                syscallError(SYSCALL_CLOSE_ERROR);
            }
        }
        else{
            //Device Info is null
            syscallError(SYSCALL_CLOSE_ERROR);
        }
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
     * syscallExec
     *
     * creates a new process.  The program used to create that process is chosen
     * semi-randomly from all the programs that have been registered with the OS
     * via {@link #addProgram}.  Limits are put into place to ensure that each
     * process is run an equal number of times.  If no programs have been
     * registered then the simulation is aborted with a fatal error.
     *
     */
    private void syscallExec()
    {
        //If there is nothing to run, abort.  This should never happen.
        if (m_programs.size() == 0)
        {
            System.err.println("ERROR!  syscallExec has no programs to run.");
            System.exit(-1);
        }
        
        //find out which program has been called the least and record how many
        //times it has been called
        int leastCallCount = m_programs.get(0).callCount;
        for(Program prog : m_programs)
        {
            if (prog.callCount < leastCallCount)
            {
                leastCallCount = prog.callCount;
            }
        }

        //Create a vector of all programs that have been called the least number
        //of times
        Vector<Program> cands = new Vector<Program>();
        for(Program prog : m_programs)
        {
            cands.add(prog);
        }
        
        //Select a random program from the candidates list
        Random rand = new Random();
        int pn = rand.nextInt(m_programs.size());
        Program prog = cands.get(pn);

        //Determine the address space size using the default if available.
        //Otherwise, use a multiple of the program size.
        int allocSize = prog.getDefaultAllocSize();
        if (allocSize <= 0)
        {
            allocSize = prog.getSize() * 2;
        }

        //Load the program into RAM
        createProcess(prog, allocSize);

        //Adjust the PC since it's about to be incremented by the CPU
        m_CPU.setPC(m_CPU.getPC() - CPU.INSTRSIZE);

    }//syscallExec


    /**
     * syscallYield
     * 
     * Schedules a new process to run instead of the currently running process.
     */
    private void syscallYield()
    {
        scheduleNewProcess();
    }//syscallYield

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
    
    /**
     * selectBlockedProcess
     *
     * select a process to unblock that might be waiting to perform a given
     * action on a given device.  This is a helper method for system calls
     * and interrupts that deal with devices.
     *
     * @param dev   the Device that the process must be waiting for
     * @param op    the operation that the process wants to perform on the
     *              device.  Use the SYSCALL constants for this value.
     * @param addr  the address the process is reading from.  If the
     *              operation is a Write or Open then this value can be
     *              anything
     *
     * @return the process to unblock -OR- null if none match the given criteria
     */
    public ProcessControlBlock selectBlockedProcess(Device dev, int op, int addr)
    {
        ProcessControlBlock selected = null;
        for(ProcessControlBlock pi : m_processes)
        {
            if (pi.isBlockedForDevice(dev, op, addr))
            {
                selected = pi;
                break;
            }
        }//for

        return selected;
    }//selectBlockedProcess
    
    
    
    //
    // SYS CALL HELPER METHODS
    //
    
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
        //Instance Variables
        /**
         * a unique id for this process
         */
        private int processId = 0;
        
        /**
         * These are the process' current registers.  If the process is in the
         * "running" state then these are out of date
         */
        private int[] registers = null;
        
        /**
         * If this process is blocked a reference to the Device is stored here
         */
        private Device blockedForDevice = null;
        
        /**
         * If this process is blocked a reference to the type of I/O operation
         * is stored here (use the SYSCALL constants defined in SOS)
         */
        private int blockedForOperation = -1;
        
        /**
         * If this process is blocked reading from a device, the requested
         * address is stored here.
         */
        private int blockedForAddr = -1;
        //END INSTANCE VARIABLES

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
        
        /**
         * save
         *
         * saves the current CPU registers into this.registers
         *
         * @param cpu  the CPU object to save the values from
         */
        public void save(CPU cpu)
        {
            int[] regs = cpu.getRegisters();
            this.registers = new int[CPU.NUMREG];
            for(int i = 0; i < CPU.NUMREG; i++)
            {
                this.registers[i] = regs[i];
            }
        }//save
        
        /**
         * restore
         *
         * restores the saved values in this.registers to the current CPU's
         * registers
         *
         * @param cpu  the CPU object to restore the values to
         */
        public void restore(CPU cpu)
        {
            int[] regs = cpu.getRegisters();
            for(int i = 0; i < CPU.NUMREG; i++)
            {
                regs[i] = this.registers[i];
            }

        }//restore
        
        /**
         * block
         *
         * blocks the current process to wait for I/O.  The caller is
         * responsible for calling {@link CPU#scheduleNewProcess}
         * after calling this method.
         *
         * @param cpu   the CPU that the process is running on
         * @param dev   the Device that the process must wait for
         * @param op    the operation that the process is performing on the
         *              device.  Use the SYSCALL constants for this value.
         * @param addr  the address the process is reading from (for SYSCALL_READ)
         * 
         */
        public void block(CPU cpu, Device dev, int op, int addr)
        {
            blockedForDevice = dev;
            blockedForOperation = op;
            blockedForAddr = addr;
            
        }//block
        
        /**
         * unblock
         *
         * moves this process from the Blocked (waiting) state to the Ready
         * state. 
         *
         */
        public void unblock()
        {
            blockedForDevice = null;
            blockedForOperation = -1;
            blockedForAddr = -1;
            
        }//unblock
        
        /**
         * isBlocked
         *
         * @return true if the process is blocked
         */
        public boolean isBlocked()
        {
            return (blockedForDevice != null);
        }//isBlocked
        
        /**
         * isBlockedForDevice
         *
         * Checks to see if the process is blocked for the given device,
         * operation and address.  If the operation is not an open, the given
         * address is ignored.
         *
         * @param dev   check to see if the process is waiting for this device
         * @param op    check to see if the process is waiting for this operation
         * @param addr  check to see if the process is reading from this address
         *
         * @return true if the process is blocked by the given parameters
         */
        public boolean isBlockedForDevice(Device dev, int op, int addr)
        {
            if ( (blockedForDevice == dev) && (blockedForOperation == op) )
            {
                if (op == SYSCALL_OPEN)
                {
                    return true;
                }

                if (addr == blockedForAddr)
                {
                    return true;
                }
            }//if

            return false;
        }//isBlockedForDevice
        
        /**
         * toString       **DEBUGGING**
         *
         * @return a string representation of this class
         */
        public String toString()
        {
            String result = "Process id " + processId + " ";
            if (isBlocked())
            {
                result = result + "is BLOCKED: ";
            }
            else if (this == m_currProcess)
            {
                result = result + "is RUNNING: ";
            }
            else
            {
                result = result + "is READY: ";
            }

            if (registers == null)
            {
                result = result + "<never saved>";
                return result;
            }
            
            for(int i = 0; i < CPU.NUMGENREG; i++)
            {
                result = result + ("r" + i + "=" + registers[i] + " ");
            }//for
            result = result + ("PC=" + registers[CPU.PC] + " ");
            result = result + ("SP=" + registers[CPU.SP] + " ");
            result = result + ("BASE=" + registers[CPU.BASE] + " ");
            result = result + ("LIM=" + registers[CPU.LIM] + " ");

            return result;
        }//toString
        
        /**
         * compareTo              
         *
         * compares this to another ProcessControlBlock object based on the BASE addr
         * register.  Read about Java's Collections class for info on
         * how this method can be quite useful to you.
         */
        public int compareTo(ProcessControlBlock pi)
        {
            return this.registers[CPU.BASE] - pi.registers[CPU.BASE];
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
};// class SOS
