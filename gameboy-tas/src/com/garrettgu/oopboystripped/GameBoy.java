package com.garrettgu.oopboystripped;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;

public class GameBoy {
    static GameBoy instance = new GameBoy();
    public static GameBoy getInstance() {
        return instance;
    }
    
    MMU mmu = new MMU();
    CPU cpu = new CPU(mmu);
    Joypad joypad = new Joypad(mmu, cpu.interruptHandler);
    private long clockTicks = 0;
    private long time = 0;
    
    public long getClocks() {
        return clockTicks;
    }
    
    public void resetClocks() {
        clockTicks = 0;
    }
    
    public void clockTick(long ticks) {
        time += ticks;
        clockTicks += ticks;
    }
    
    private int getKeyEvent(char c) {
        switch(c) {
            case 'u': return KeyEvent.VK_UP;
            case 'd': return KeyEvent.VK_DOWN;
            case 'l': return KeyEvent.VK_LEFT;
            case 'r': return KeyEvent.VK_RIGHT;
            case 'a': return KeyEvent.VK_Z;
            case 'b': return KeyEvent.VK_X;
            case 's': return KeyEvent.VK_ENTER;
            case 'e': return KeyEvent.VK_SHIFT;
            default: return KeyEvent.VK_0;
        }
    }
    
    public void main() throws Exception{
        
        Scanner fin = new Scanner(System.in);

        File file = new File("utctf.gb");
        new FileInputStream(file).read(mmu.mem);
        
        cpu.regs.AF.write(0x1b0);
        cpu.regs.BC.write(0x13);
        cpu.regs.DE.write(0xd8);
        cpu.regs.HL.write(0x14d);
        cpu.regs.SP.write(0xfffe);
        cpu.regs.PC.write(0x100);
        
        int currentKey = KeyEvent.VK_0;
        
        System.out.println("Welcome to the UTCTF Game Boy TAS! ");
        
        for(int i = 0; i < 12; i++){
            System.out.print("Please enter your next command: ");
            String key = fin.next();
            int duration = fin.nextInt();
            
            if(duration > 40000){
                System.out.println("Duration cannot exceed 40000");
                System.exit(0);
            }
            
            int nextKey = getKeyEvent(key.charAt(0));
            if(nextKey != currentKey){
                joypad.keyReleased(currentKey);
                joypad.keyPressed(nextKey);
                currentKey = nextKey;
            }
            
            time = 0;
            while(time < duration){
                cpu.executeOneInstruction(false, false);
            }
        }
        
        System.out.println("You ran out of commands");
    }
    
    public static void main(String[] args) throws Exception{
        instance.main();
    }
}
