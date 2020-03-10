package com.garrettgu.oopboystripped;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.Serializable;

public class Joypad{
    private MMU mmu;
    private InterruptHandler interruptHandler;
    private int a;
    private int b;
    private int select;
    private int start;
    private int up;
    private int down;
    private int left;
    private int right;

    public Joypad(MMU mmu, InterruptHandler interruptHandler) {
        this.mmu = mmu;
        this.interruptHandler = interruptHandler;
        mmu.setJoypad(this);
        a = 1;
        b = 1;
        select = 1;
        start = 1;
        up = 1;
        down = 1;
        left = 1;
        right = 1;
    }
    public void keyPressed(int code) {
        // TODO Auto-generated method stub
        switch (code) {
            case KeyEvent.VK_LEFT:
                if (left == 1) {
                    interruptHandler.issueInterruptIfEnabled(InterruptHandler.JOYPAD);
                }
                left = 0;
                break;
            case KeyEvent.VK_RIGHT:
                if (right == 1) {
                    interruptHandler.issueInterruptIfEnabled(InterruptHandler.JOYPAD);
                }
                right = 0;
                break;
            case KeyEvent.VK_UP:
                if (up == 1) {
                    interruptHandler.issueInterruptIfEnabled(InterruptHandler.JOYPAD);
                }
                up = 0;
                break;
            case KeyEvent.VK_DOWN:
                if (down == 1) {
                    interruptHandler.issueInterruptIfEnabled(InterruptHandler.JOYPAD);
                }
                down = 0;
                break;
            case KeyEvent.VK_Z:
                if (a == 1) {
                    interruptHandler.issueInterruptIfEnabled(InterruptHandler.JOYPAD);
                }
                a = 0;
                break;
            case KeyEvent.VK_X:
                if (b == 1) {
                    interruptHandler.issueInterruptIfEnabled(InterruptHandler.JOYPAD);
                }
                b = 0;
                break;
            case KeyEvent.VK_ENTER:
                if (start == 1) {
                    interruptHandler.issueInterruptIfEnabled(InterruptHandler.JOYPAD);
                }
                start = 0;
                break;
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_BACK_SPACE:
                if (select == 1) {
                    interruptHandler.issueInterruptIfEnabled(InterruptHandler.JOYPAD);
                }
                select = 0;
                break;
        }

    }
    
    public void keyReleased(int code) {
        switch (code) {
            case KeyEvent.VK_LEFT:
                left = 1;
                break;
            case KeyEvent.VK_RIGHT:
                right = 1;
                break;
            case KeyEvent.VK_UP:
                up = 1;
                break;
            case KeyEvent.VK_DOWN:
                down = 1;
                break;
            case KeyEvent.VK_Z:
                a = 1;
                break;
            case KeyEvent.VK_X:
                b = 1;
                break;
            case KeyEvent.VK_ENTER:
                start = 1;
                break;
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_BACK_SPACE:
                select = 1;
                break;
        }

    }

    public int readDirections() {
        //System.out.println("reading buttons");
        return (down << 3) + (up << 2) + (left << 1) + right;
    }

    public int readButtons() {
        return (start << 3) + (select << 2) + (b << 1) + a;
    }

}
