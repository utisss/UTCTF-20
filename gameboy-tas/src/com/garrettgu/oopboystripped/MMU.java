package com.garrettgu.oopboystripped;

import java.io.Serializable;

public class MMU implements Serializable {
    public byte[] mem = new byte[0xFFFF+1];
    
    private Joypad joypad;
    
    private int charsPrinted = 0;

    private CPU cpu;
    
    public void setCPU(CPU cpu){
        this.cpu = cpu;
    }

    public CPU getCPU() {
        return this.cpu;
    }

    public int slowReadByte(int location) {
        GameBoy.getInstance().clockTick(4);
        return readByte(location);
    }

    public void setJoypad(Joypad joypad) {
        this.joypad = joypad;
    }

    public int readByte(int location) {
        if(location == 0xFF00){ //joypad input
            if (BitOps.extract(mem[0xFF00] & 0xff, 5, 5) == 0) {
                return joypad.readButtons();
            }

            if(BitOps.extract(mem[0xFF00] & 0xff, 4, 4) == 0) {
                return joypad.readDirections();
            }
            else {
                return 0xFF;
            }
        }
        
        return mem[location & 0xffff] & 0xff;
    }

    public int slowReadWord(int location) {
        return (slowReadByte(location+1) << 8) + slowReadByte(location);
    }

    public int readWord(int location) {
        return (readByte(location+1) << 8) + readByte(location);
    }

    public void slowWriteByte(int location, int toWrite) {
        GameBoy.getInstance().clockTick(4);
        writeByte(location, toWrite);
    }

    public void writeByte(int location, int toWrite){
        
        if (location == 0xff01) { //simplified serial port
            System.out.printf("%c", toWrite & 0xff);
            
            charsPrinted++;
            if(charsPrinted > 30) {
                System.out.println("\noutput limit exceeded");
                System.exit(0);
            }
            return;
        }
        
        if(location < 0xff80) {
            return;
        }
        
        if(location == 0xffff){
            cpu.interruptHandler.handleIE(toWrite & 0xFF);
        }
        
        mem[location] = (byte)(toWrite & 0xFF);
    }

    public void writeWord(int location, int toWrite) {
        toWrite &= 0xffff;
        writeByte(location, toWrite & 0xff);
        writeByte(location + 1, toWrite >> 8);
    }

    //basically an abstraction of the various addressing modes
    class Location implements ReadWritable{
        private int address;

        public Location(int address){
            this.address = address;
        }

        @Override
        public int read() {
            return MMU.this.slowReadByte(address);
        }

        @Override
        public void write(int val) {
            MMU.this.slowWriteByte(address, val);
        }

        public void writeLong(int val) {
            MMU.this.slowWriteByte(address, val & 0xff);
            MMU.this.slowWriteByte(address + 1, (val >> 8) & 0xff);
        }
    }

    public Location shortRegisterLocation(Register r) {
        return new Location(0xff00 + r.read());
    }

    public Location registerLocation(Readable r) {
        return new Location(r.read());
    }

    public ReadWritable a8Location(Register pc){
        int address = 0xff00;
        address += slowReadByte(pc.read()+1);

        return new Location(address);
    }

    public ReadWritable a16Location(Register pc) {
        int address = slowReadWord(pc.read()+1);

        return new Location(address);
    }

    public void writeBytes(int location, byte[] sequence) {
        for(int i = 0; i < sequence.length; i++){
            writeByte(location + i, sequence[i]);
        }
    }
}
