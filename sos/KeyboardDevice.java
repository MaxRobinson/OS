package sos;

/**
 * This class "simulates"(provides random output)a simple, 
 * sharable read-only device. 
 * 
 * @author Max Robinson, Jason Vanderwerf
 * 
 * @see Sim
 * @see CPU
 * @see SOS
 * @see Device
 */
public class KeyboardDevice implements Device{
    private int m_id = -999;            // the OS assigned device ID
    
    /**
     * getId
     *
     * @return the device id of this device
     */
    @Override
    public int getId() {
        return 0;
    }

    /**
     * setId
     *
     * sets the device id of this device
     *
     * @param id the new id
     */
    @Override
    public void setId(int id) {
        m_id = id;
    }

    /**
     * isSharable
     *
     * This device can not be used simultaneously by multiple processes
     *
     * @return true
     */
    @Override
    public boolean isSharable() {
        return false;
    }

    /**
     * isAvailable
     *
     * this device is available if no requests are currently being processed
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * isReadable
     *
     * @return whether this device can be read from (true/false)
     */
    @Override
    public boolean isReadable() {
        return true;
    }

    /**
     * isWriteable
     *
     * @return whether this device can be written to (true/false)
     */
    @Override
    public boolean isWriteable() {
        return false;
    }
    

    /**
     * read
     * 
     * @return a different number every time something is read.
     */
    @Override
    public int read(int addr) { 
        return (int)(Math.random()*10);
    }//read 

    /**
     * write
     *
     * not implemented
     * 
     */
    @Override
    public void write(int addr, int data) {
        //This method should never be called
        return;
    }//write 

}//class KeyboardDevice
