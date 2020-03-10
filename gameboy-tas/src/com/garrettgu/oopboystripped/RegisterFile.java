package com.garrettgu.oopboystripped;

import java.io.Serializable;
import java.security.InvalidParameterException;

interface Readable {
    int read();
}

interface Writable {
    void write(int val);
}

interface ReadWritable extends Readable, Writable {}

interface Register extends ReadWritable { }

interface ShortRegister extends Register, Serializable {}

class LongRegister implements Register, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 2142281106792231516L;
    private int value;
    ShortRegister lowerByte = new ShortRegister() {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        public int read() {
            return LongRegister.this.value & 0xff;
        }

        @Override
        public void write(int val) {
            val &= 0xff; //truncate value to single byte
            LongRegister.this.value &= 0xff00; //reset lower byte
            LongRegister.this.value |= val; //set lower byte to value
        }

        public String toString() {
            return String.format("%02X", this.read());
        }
    };

    ShortRegister upperByte = new ShortRegister() {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        public int read() {
            return (LongRegister.this.value & 0xff00) >> 8;
        }

        @Override
        public void write(int val) {
            val = (val & 0xff) << 8; //truncate value to single byte and move it up a byte
            LongRegister.this.value &= 0x00FF; //reset upper byte
            LongRegister.this.value |= val; //set upper byte to value
        }

        public String toString() {
            return String.format("%02X", this.read());
        }
    };

    public int read() {
        return this.value;
    }

    public void write(int val){
        this.value = val & 0xffff;
    }

    public String toString() {
        return String.format("%04X", this.read());
    }
}

class FlagRegister implements ShortRegister {
    ShortRegister wrapped;
    public FlagRegister(ShortRegister toWrap) {
        this.wrapped = toWrap;
    }

    @Override
    public int read() {
        return wrapped.read() & 0xf0;
    }

    @Override
    public void write(int val) {
        wrapped.write(val & 0xf0);
    }

    public String toString() {
        return String.format("%04X", this.read());
    }
}

public class RegisterFile implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1620775094696906337L;
    public LongRegister AF, BC, DE, HL, SP, PC;
    public ShortRegister A, F, B, C, D, E, H, L;

    public static final int ZFLAG = 7;
    public static final int NFLAG = 6;
    public static final int HFLAG = 5;
    public static final int CFLAG = 4;

    public FlagSet flags;

    public class FlagSet implements Serializable{
        /**
         *
         */
        private static final long serialVersionUID = 888159977466716630L;

        private Register flagReg;

        boolean[] flagWritable = new boolean[8];

        private boolean ZWritable = true, NWritable = true, HWritable = true, CWritable = true;

        public void enableFlagWrites(boolean z, boolean n, boolean h, boolean c){
            flagWritable[ZFLAG] = z;
            flagWritable[NFLAG] = n;
            flagWritable[HFLAG] = h;
            flagWritable[CFLAG] = c;
        }

        public FlagSet(Register r) {
            this.flagReg = r;
        }

        public boolean getFlag(int flagNum) {
            if(flagNum < 4) throw new InvalidParameterException("bad flag number");

            return ((flagReg.read() >> flagNum) & 1) == 1;
        }

        public void setFlag(int flagNum, boolean val){
            if(flagNum < 4) throw new InvalidParameterException("bad flag number");

            if(flagWritable[flagNum]){
                int flags = flagReg.read();
                if(val){
                    flags |= 1 << flagNum;
                }else{
                    flags &= ~(1 << flagNum);
                }

                flagReg.write(flags);
            }
        }
    }

    public void dump() {
        System.out.println("AF = " + AF);
        System.out.println("BC = " + BC);
        System.out.println("DE = " + DE);
        System.out.println("HL = " + HL);
        System.out.println("SP = " + SP);
        System.out.println("PC = " + PC);
        System.out.println("A = " + A);
        System.out.println("F = " + F);
        System.out.println("B = " + B);
        System.out.println("C = " + C);
        System.out.println("D = " + D);
        System.out.println("E = " + E);
        System.out.println("H = " + H);
        System.out.println("L = " + L);
    }

    public RegisterFile(){
        AF = new LongRegister();
        A = AF.upperByte;
        AF.lowerByte = new FlagRegister(AF.lowerByte);
        F = AF.lowerByte;

        BC = new LongRegister();
        B = BC.upperByte;
        C = BC.lowerByte;

        DE = new LongRegister();
        D = DE.upperByte;
        E = DE.lowerByte;

        HL = new LongRegister();
        H = HL.upperByte;
        L = HL.lowerByte;

        SP = new LongRegister();
        PC = new LongRegister();

        flags = new FlagSet(F);
    }
}
