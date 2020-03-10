package com.garrettgu.oopboystripped;

import java.io.Serializable;
import java.util.HashMap;

public class InterruptHandler implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -2641142498470471980L;

    private CPU cpu;

    public static final int VBLANK = 0x0040;
    public static final int LCDC = 0x0048;
    public static final int TIMER_OVERFLOW = 0x0050;
    public static final int SERIAL_COMPLETION = 0x0058;
    public static final int JOYPAD = 0x0060;

    public HashMap<Integer, Boolean> specificEnabled = new HashMap<>();
    {
        specificEnabled.put(VBLANK, false);
        specificEnabled.put(LCDC, false);
        specificEnabled.put(TIMER_OVERFLOW, false);
        specificEnabled.put(SERIAL_COMPLETION, false);
        specificEnabled.put(JOYPAD, false);
    }

    private boolean interruptsEnabled = false; // this is the IME flag

    public void setInterruptsEnabled(boolean interruptsEnabled){
        this.interruptsEnabled = interruptsEnabled;
    }

    public void setSpecificEnabled(int handle, boolean enabled){
        specificEnabled.put(handle, enabled);
    }

    public InterruptHandler(CPU cpu){
        this.cpu = cpu;
    }

    public boolean issueInterruptIfEnabled(int handle){
        if(!interruptsEnabled) {
            cpu.interrupt(-1);
            return false;
        }
        if(!specificEnabled.getOrDefault(handle, false)) {
            return false;
        }

        cpu.interrupt(handle);

        return true;
    }

    public boolean handleIF(int IFflag){
        //read IFflag bit by bit based on priority and issue an interrupt if needed
        //lower priority simultaneous interrupts are ignored
        if((IFflag & 1) == 1){ //VBLANK requested
            return this.issueInterruptIfEnabled(InterruptHandler.VBLANK);
        }
        IFflag >>= 1;
        if((IFflag & 1) == 1){
            return this.issueInterruptIfEnabled(InterruptHandler.LCDC);
        }
        IFflag >>= 1;
        if((IFflag & 1) == 1){
            return this.issueInterruptIfEnabled(InterruptHandler.TIMER_OVERFLOW);
        }
        IFflag >>= 1;
        if((IFflag & 1) == 1){
            return this.issueInterruptIfEnabled(InterruptHandler.SERIAL_COMPLETION);
        }
        IFflag >>= 1;
        if((IFflag & 1) == 1){
            this.issueInterruptIfEnabled(InterruptHandler.JOYPAD);
        }

        return false;
    }

    public void handleIE(int IEflag) {
        //read IEflag bit by bit and set each interrupt enable depending on IEflag value
        this.setSpecificEnabled(InterruptHandler.VBLANK, (IEflag & 1) == 1); //enable vblank if bit 0 is 1
        IEflag >>= 1;
        this.setSpecificEnabled(InterruptHandler.LCDC, (IEflag & 1) == 1); //ditto
        IEflag >>= 1;
        this.setSpecificEnabled(InterruptHandler.TIMER_OVERFLOW, (IEflag & 1) == 1);
        IEflag >>= 1;
        this.setSpecificEnabled(InterruptHandler.SERIAL_COMPLETION, (IEflag & 1) == 1);
        IEflag >>= 1;
        this.setSpecificEnabled(InterruptHandler.JOYPAD, (IEflag & 1) == 1);
    }

    public String toString() {
        if(!interruptsEnabled){
            return "IME OFF";
        }else{
            return specificEnabled.toString();
        }
    }
}
