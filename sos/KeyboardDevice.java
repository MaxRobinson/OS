package sos;

public class KeyboardDevice implements Device{
    private int m_id;
    
    @Override
    public int getId() {
        return 0;
    }

    @Override
    public void setId(int id) {
        m_id = id;
    }

    @Override
    public boolean isSharable() {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isWriteable() {
        return false;
    }
    

    /**
     * read method
     * return a different number every time something is read.
     */
    @Override
    public int read(int addr) { 
        return (int)(Math.random()*10);
    }

    @Override
    public void write(int addr, int data) {
        // TODO Auto-generated method stub
        
    }

}
