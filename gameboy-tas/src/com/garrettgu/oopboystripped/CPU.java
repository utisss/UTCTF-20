package com.garrettgu.oopboystripped;

import java.io.Serializable;
import java.security.InvalidParameterException;

interface Lambda{
    int exec(CPU cpu);
}

public class CPU implements Serializable {

    private static final long serialVersionUID = 3042928203064585497L;
    MMU mem;
    RegisterFile regs = new RegisterFile();
    InterruptHandler interruptHandler = new InterruptHandler(this);
    private int clockCycleDelta;

    public CPU(MMU mem) {
        this.mem = mem;
        mem.setCPU(this);
    }

    public void setMMU(MMU mmu) {
        this.mem = mmu;
    }
    public int getClockCycles() {
        return clockCycles;
    }

    public int getClockCycleDelta() {
        return clockCycleDelta;
    }

    public boolean interrupted = false;
    public int pendingInterrupt = 0;
    private int clockCycles = 0;

    public static final int ZFLAG = RegisterFile.ZFLAG;
    public static final int NFLAG = RegisterFile.NFLAG;
    public static final int HFLAG = RegisterFile.HFLAG;
    public static final int CFLAG = RegisterFile.CFLAG;

    public static final int NOJUMP = -1;
    public static final int RELJUMP = 0;
    public static final int ABSJUMP = 1;

    public void coreDump() {
        int opcode = mem.readByte(regs.PC.read());
        Operation op = operations[opcode];
        int currentPC = regs.PC.read();
        System.out.println(Integer.toString(currentPC, 16) + ": " + op.description);
        System.out.println("Halted: " + halted);
        System.out.println("Interrupts: " + interruptHandler);
        regs.dump();
    }

    public void interrupt(int handle) {
        this.interrupted = true;
        pendingInterrupt = handle;
    }

    public void executeOneInstruction(boolean printOutput, boolean haltEnabled) {

        clockCycleDelta = 0;

        if(halted && haltEnabled) {
            clockCycleDelta = 4;
            serviceInterrupts();
            GameBoy.getInstance().clockTick(clockCycleDelta);
            return;
        }

        GameBoy.getInstance().resetClocks();

        int opcode = mem.slowReadByte(regs.PC.read());

        Operation op = operations[opcode];

        int currentPC = regs.PC.read();

        int result = op.execute(this);

        if(GameBoy.getInstance().getClocks() < this.clockCycleDelta) {
            GameBoy.getInstance().clockTick(this.clockCycleDelta - GameBoy.getInstance().getClocks());
        }else if(GameBoy.getInstance().getClocks() > this.clockCycleDelta){
            System.out.println("Invalid clock ticks: " + op.description);
            System.out.println("Expected: " + this.clockCycleDelta);
            System.out.println("Received: " + GameBoy.getInstance().getClocks());
        }

        if(printOutput) {
            System.out.println(Integer.toString(currentPC, 16) + ": " + op.description);

            System.out.println("result: " + Integer.toString(result, 16));

            System.out.println(clockCycleDelta);

            regs.dump();
        }
    }

    static class Operation{ //any operation that is not a jump
        String description;
        Lambda lambda;
        int ticks;
        int length; //length of operation in bytes
        String flagsAffected; //e.g. "- - - -"
        public Operation(String description, Lambda lambda, int length, String flagsAffected, int ticks){
            this.description = description;
            this.lambda = lambda;
            this.ticks = ticks;
            this.length = length;
            this.flagsAffected = flagsAffected;
        }

        // allows flags to be written to only when the instruction should affect the flag
        protected void handleFlagsWritable(CPU cpu) {
            final int[] flags = new int[] { ZFLAG, NFLAG, HFLAG, CFLAG };
            boolean[] writable = new boolean[8];

            for(int i = 0; i < flagsAffected.length(); i+= 2) {
                char descriptor = flagsAffected.charAt(i);

                int flag = flags[i / 2];

                switch(descriptor){
                    case '0':
                    case '1':
                    case '-':
                        writable[flag] = false;
                        break;
                    default:
                        writable[flag] = true;
                }
            }

            cpu.regs.flags.enableFlagWrites(writable[ZFLAG], writable[NFLAG], writable[HFLAG], writable[CFLAG]);
        }

        protected void handleFlagsValues(CPU cpu) {
            final int[] flags = new int[] { ZFLAG, NFLAG, HFLAG, CFLAG };
            cpu.regs.flags.enableFlagWrites(true, true, true, true);

            for(int i = 0; i < flagsAffected.length(); i+= 2) {
                char descriptor = flagsAffected.charAt(i);

                int flag = flags[i / 2];

                switch(descriptor){
                    case '0':
                        cpu.regs.flags.setFlag(flag, false);
                        break;
                    case '1':
                        cpu.regs.flags.setFlag(flag, true);
                        break;
                    case '-':
                    default:
                        break;
                }
            }
        }

        public int execute(CPU cpu) {
            handleFlagsWritable(cpu);

            int result = this.lambda.exec(cpu);

            handleFlagsValues(cpu);

            cpu.clockCycles += this.ticks;
            cpu.clockCycleDelta += this.ticks;
            cpu.regs.PC.write(cpu.regs.PC.read() + length);

            cpu.serviceInterrupts();

            return result;
        }
    }

    static class Jump extends Operation{ //this includes relative, conditional, calls, and returns
        int ticksIfJumped, ticksIfNotJumped;
        public Jump(String description, Lambda lambda, int length, String flagsAffected, int ticksIfJumped, int ticksIfNotJumped) {
            super(description, lambda, length, flagsAffected, ticksIfJumped);
            this.ticksIfJumped = ticksIfJumped;
            this.ticksIfNotJumped = ticksIfNotJumped;
        }

        public int execute(CPU cpu) {
            handleFlagsWritable(cpu);

            int result = this.lambda.exec(cpu);

            handleFlagsValues(cpu);

            if (result == RELJUMP || result == NOJUMP){
                //apparently offsets are calculated based on the future PC
                cpu.regs.PC.write(cpu.regs.PC.read() + length);
            }

            if(result == NOJUMP) {
                cpu.clockCycles += this.ticksIfNotJumped;
                cpu.clockCycleDelta += this.ticksIfNotJumped;
            }else{
                cpu.clockCycles += this.ticksIfJumped;
                cpu.clockCycleDelta += this.ticksIfJumped;
            }

            cpu.serviceInterrupts();

            return result;
        }
    }

    static class CB extends Operation {
        public CB() {
            super("CB", null, 2, "Z N H C", 4);
        }

        public int execute(CPU cpu) {
            int cbOpcode = cpu.mem.slowReadByte(cpu.regs.PC.read() + 1); //the cb opcode follows directly after cb

            Operation cbOperation = cbOperations[cbOpcode];

            int result = cbOperation.execute(cpu);
            //cpu clockCycles and clockCycleDelta are set by the cbOperation
            //PC is also advanced the correct amount by the cbOperation

            return result;
        }
    }

    enum Condition {
        NZ, Z, NC, C
    }

    void serviceInterrupts() {
        if(interrupted) {
            interrupted = false;
            this.halted = false;
            int interruptVector = pendingInterrupt;
            if(interruptVector != -1) {
                clockCycleDelta += 12;
                PUSH(regs.PC);
                regs.PC.write(interruptVector);
                interruptHandler.setInterruptsEnabled(false);
            }
        }
    }

    //represents an 8-bit immediate value. assumes it's placed right after PC
    Readable d8() {
        int value = mem.slowReadByte(regs.PC.read()+1);

        return new Readable() {
            @Override
            public int read() {
                return value;
            }
        };
    }

    Readable r8() {
        int value = (byte)mem.slowReadByte(regs.PC.read() + 1);

        return new Readable() {
            @Override
            public int read(){
                return value;
            }
        };
    }

    //represents an 8-bit signed immediate value, which is added to 0xff00
    Readable a8() {
        int value = 0xff00 + mem.slowReadByte(regs.PC.read()+1);

        return new Readable() {
            @Override
            public int read() {
                return value;
            }
        };
    }

    //represents a 16-bit immediate value right after PC
    Readable d16() {
        int value = mem.slowReadWord(regs.PC.read()+1);

        return new Readable() {
            @Override
            public int read() {
                return value;
            }
        };
    }

    Readable SPr8() {
        byte r8 = (byte)mem.slowReadByte(regs.PC.read()+1); //r8 is a signed byte value
        int spVal = regs.SP.read();
        int address = spVal + r8;

        //https://stackoverflow.com/questions/5159603/gbz80-how-does-ld-hl-spe-affect-h-and-c-flags
        if(r8 >= 0){
            regs.flags.setFlag(RegisterFile.HFLAG, (spVal & 0xF) + (r8 & 0xF) > 0xF);
            regs.flags.setFlag(RegisterFile.CFLAG, (spVal & 0xFF) + r8 > 0xFF);
        }else{
            regs.flags.setFlag(RegisterFile.HFLAG, (address & 0xF) <= (spVal & 0xF));
            regs.flags.setFlag(RegisterFile.CFLAG, (address & 0xFF) <= (spVal & 0xFF));
        }

        return new Readable() {
            @Override
            public int read() {
                return address;
            }
        };
    }

    //represents a 16-bit address right after PC
    Readable a16() {
        return d16();
    }

    // a wrapper around a register that automatically increments itself after being read or written to
    // selfIncrement(regs.HL) := (HL+)
    ReadWritable selfIncrement(LongRegister reg){
        return new ReadWritable() {
            boolean incremented = false;
            @Override
            public int read() {
                int val = reg.read();
                if(!incremented){
                    incremented = true;
                    reg.write(val + 1);
                }
                return val;
            }

            @Override
            public void write(int val) {
                if(!incremented){
                    incremented = true;
                    reg.write(val + 1);
                }else{
                    reg.write(val);
                }
            }
        };
    }

    //same as above, but decrements instead
    ReadWritable selfDecrement(LongRegister reg){
        return new ReadWritable() {
            boolean decremented = false;
            @Override
            public int read() {
                int val = reg.read();
                if(!decremented){
                    decremented = true;
                    reg.write(val - 1);
                }
                return val;
            }

            @Override
            public void write(int val) {
                if(!decremented){
                    decremented = true;
                    reg.write(val - 1);
                }else{
                    reg.write(val);
                }
            }
        };
    }

    boolean evaluateCondition(Condition c) {
        switch(c){
            case NZ:
                return !regs.flags.getFlag(ZFLAG);
            case Z:
                return regs.flags.getFlag(ZFLAG);
            case NC:
                return !regs.flags.getFlag(CFLAG);
            case C:
                return regs.flags.getFlag(CFLAG);
        }

        throw new InvalidParameterException("this shouldn't happen");
    }

    boolean halted = false;

    int XXX() {
        throw new IllegalArgumentException("invalid opcode");
    }

    int LD (Writable dest, Readable src){
        int val = src.read();

        //LD (a16), SP is a special case since it involves writing sixteen bits to memory
        if(dest instanceof MMU.Location && src == regs.SP){
            ((MMU.Location) dest).writeLong(val);
        }else {
            dest.write(val);
        }
        return val;
    }

    int PUSH(LongRegister reg) {
        int sp = regs.SP.read();

        sp--;
        mem.slowWriteByte(sp, reg.upperByte.read());

        sp--;
        mem.slowWriteByte(sp, reg.lowerByte.read());

        regs.SP.write(sp);

        return reg.read();
    }

    int POP(LongRegister reg){
        int sp = regs.SP.read();

        if(reg.lowerByte == regs.F) {
            //the lower nibble of F should always be 0
            reg.lowerByte.write(mem.slowReadByte(sp) & (~0xf));
        }else{
            reg.lowerByte.write(mem.slowReadByte(sp));
        }
        sp++;

        reg.upperByte.write(mem.slowReadByte(sp));
        sp++;

        regs.SP.write(sp);

        return reg.read();
    }

    int ADD(Register dest, Readable src){
        int op1 = src.read(), op2 = dest.read();

        int halfMask = (dest instanceof LongRegister)? 0xfff : 0xf;
        int fullMask = (dest instanceof LongRegister)? 0xffff : 0xff;

        int sum = op1 + op2;
        int result = sum & fullMask;

        regs.flags.setFlag(ZFLAG, (result == 0));
        if(dest == regs.SP) { //SP is the only operand that takes a negative src
            int r8 = op1;
            int spVal = op2;
            int address = result;
            //copied from SPr8()
            if(r8 >= 0){
                regs.flags.setFlag(RegisterFile.HFLAG, (spVal & 0xF) + (r8 & 0xF) > 0xF);
                regs.flags.setFlag(RegisterFile.CFLAG, (spVal & 0xFF) + r8 > 0xFF);
            }else{
                regs.flags.setFlag(RegisterFile.HFLAG, (address & 0xF) <= (spVal & 0xF));
                regs.flags.setFlag(RegisterFile.CFLAG, (address & 0xFF) <= (spVal & 0xFF));
            }
        }else {
            regs.flags.setFlag(CFLAG, (sum != result));
            regs.flags.setFlag(HFLAG, ((op1 & halfMask) + (op2 & halfMask) > halfMask));
        }

        dest.write(result);

        return result;
    }

    int ADC(Register dest, Readable src){
        int op1 = src.read(), op2 = dest.read();

        int halfMask = (dest instanceof LongRegister)? 0xfff : 0xf;
        int fullMask = (dest instanceof LongRegister)? 0xffff : 0xff;

        int carry = regs.flags.getFlag(CFLAG)? 1: 0;

        int sum = op1 + op2 + carry;
        int result = sum & fullMask;

        regs.flags.setFlag(ZFLAG, (result == 0));
        regs.flags.setFlag(CFLAG, (sum != result));
        regs.flags.setFlag(HFLAG, ((op1 & halfMask) + (op2 & halfMask) + carry > halfMask));

        dest.write(result);

        return result;
    }

    //saves result in A
    int SUB(Readable toSubtract){
        int op1 = regs.A.read();
        int op2 = toSubtract.read();

        int diff = op1 - op2;
        int result = diff & 0xff;

        regs.flags.setFlag(ZFLAG, (result == 0));
        regs.flags.setFlag(CFLAG, (diff < 0)); //set if needed borrow
        regs.flags.setFlag(HFLAG, ((op1 & 0xf) - (op2 & 0xf) < 0)); //set if needs borrow from 4th bit
        //seems like GBCPUman is wrong?

        regs.A.write(result);

        return result;
    }

    //result in A
    int SBC(Readable toSubtract){
        int op1 = regs.A.read();
        int op2 = toSubtract.read();

        int carry = regs.flags.getFlag(CFLAG)? 1: 0;

        int diff = op1 - op2 - carry;
        int result = diff & 0xff;

        regs.flags.setFlag(ZFLAG, (result == 0));
        regs.flags.setFlag(CFLAG, (diff < 0)); //set if needed borrow
        regs.flags.setFlag(HFLAG, ((op1 & 0xf) - (op2 & 0xf) - carry < 0)); //set if needs borrow from 4th bit
        //seems like GBCPUman is wrong?

        regs.A.write(result);

        return result;
    }

    //result in A
    int AND(Readable op){
        int op1 = regs.A.read();
        int op2 = op.read();

        if((op2 & 0xff) != op2) throw new InvalidParameterException("operand must be byte");

        int result = op1 & op2;

        regs.flags.setFlag(ZFLAG, (result == 0));

        regs.A.write(result);

        return result;
    }

    //result in A
    int OR(Readable op){
        int op1 = regs.A.read();
        int op2 = op.read();

        if((op2 & 0xff) != op2) throw new InvalidParameterException("operand must be byte");

        int result = op1 | op2;

        regs.flags.setFlag(ZFLAG, (result == 0));

        regs.A.write(result);

        return result;
    }

    //result in A
    int XOR(Readable op) {
        int op1 = regs.A.read();
        int op2 = op.read();

        if((op2 & 0xff) != op2) throw new InvalidParameterException("operand must be byte");

        int result = op1 ^ op2;

        regs.flags.setFlag(ZFLAG, (result == 0));

        regs.A.write(result);

        return result;
    }

    //result discarded
    int CP(Readable n) {
        int originalA = regs.A.read();

        int result = SUB(n);

        regs.A.write(originalA);

        return result;
    }

    //increments toInc
    int INC(ReadWritable toInc){
        int original = toInc.read();
        int result = original+1;

        int fullMask = (toInc instanceof LongRegister) ? 0xffff: 0xff;
        int halfMask = (toInc instanceof LongRegister) ? 0xff: 0xf;
        regs.flags.setFlag(ZFLAG, ((result & fullMask) == 0));
        regs.flags.setFlag(HFLAG, ((original & halfMask) + 1) > halfMask);
        //apparently C-flag is not affected

        toInc.write(result);

        return result;
    }

    //decrements toDec
    int DEC(ReadWritable toDec){
        int original = toDec.read();
        int result = original - 1;

        int halfMask = (toDec instanceof LongRegister) ? 0xff: 0xf;
        regs.flags.setFlag(ZFLAG, (result == 0));
        regs.flags.setFlag(HFLAG, (original & halfMask) < 1); //needs borrow from bit 4
        //C not affected

        toDec.write(result);

        return result;
    }

    //swaps upper and lower nibbles of op, which is a byte
    int SWAP(ReadWritable op){
        int original = op.read();

        if((original & 0xff) != original) throw new InvalidParameterException("operand must be byte");

        int upperNibble = (original & 0xf0) >> 4;
        int lowerNibble = (original & 0xf);

        int result = (lowerNibble << 4) | upperNibble;

        regs.flags.setFlag(ZFLAG, (result == 0));

        op.write(result);

        return result;
    }

    //this link is about the z80, not the GB z80
    //http://z80-heaven.wikidot.com/instructions-set:daa
    //this link is wrong
    //https://www.reddit.com/r/EmuDev/comments/4ycoix/a_guide_to_the_gameboys_halfcarry_flag/
    //this link might be wrong
    //https://ehaskins.com/2018-01-30%20Z80%20DAA/
    //this link works
    //https://forums.nesdev.com/viewtopic.php?f=20&t=15944
    int DAA() {
        int original = regs.A.read();
        int result = original;

        //pseudocode from https://forums.nesdev.com/viewtopic.php?f=20&t=15944
        if(!regs.flags.getFlag(NFLAG)){
            if(regs.flags.getFlag(CFLAG) || original > 0x99) {
                result += 0x60;
                regs.flags.setFlag(CFLAG, true);
            }
            if(regs.flags.getFlag(HFLAG) || (original & 0x0f) > 0x09) {
                result += 0x6;
            }
        }else{
            if(regs.flags.getFlag(CFLAG)) {
                result -= 0x60;
            }
            if(regs.flags.getFlag(HFLAG)) {
                result -= 0x6;
            }
        }

        result &= 0xff;

        regs.flags.setFlag(ZFLAG, (result == 0));

        regs.A.write(result);

        return result;
    }

    int CPL() {
        int original = regs.A.read();
        int result = (~original) & 0xff;

        regs.A.write(result);

        return result;
    }

    int CCF() {
        regs.flags.setFlag(CFLAG, !regs.flags.getFlag(CFLAG));

        return 0;
    }

    int SCF() {
        regs.flags.setFlag(CFLAG, true);

        return 0;
    }

    int NOP(){
        return 0;
    }

    int HALT() {
        halted = true;

        return 0;
    }

    int STOP() {
        halted = true;

        return 0;
    }

    int DI() {
        this.interruptHandler.setInterruptsEnabled(false);
        return 0;
    }

    int EI() {
        this.interruptHandler.setInterruptsEnabled(true);
        return 1;
    }

    int RLCA() {
        return RLC(regs.A);
    }

    //rotates op left by one bit, puts 7th bit in C
    int RLC(ReadWritable op){
        int original = op.read();
        int bit7 = (original >> 7) & 1;

        regs.flags.setFlag(CFLAG, bit7 == 1);

        int result = (original << 1) | bit7;

        regs.flags.setFlag(ZFLAG, result == 0);

        op.write(result);

        return result;
    }

    int RLA(){
        return RL(regs.A);
    }

    //rotates op left, with C treated as bit 8
    int RL(ReadWritable op) {
        int original = op.read();
        int bit7 = (original >> 7) & 1;

        int carryBit = (regs.flags.getFlag(CFLAG)? 1 : 0);

        int result = ((original << 1) | carryBit) & 0xff;

        regs.flags.setFlag(CFLAG, bit7 == 1);
        regs.flags.setFlag(ZFLAG, result == 0);

        op.write(result);

        return result;
    }

    int RRCA(){
        return RRC(regs.A);
    }

    //rotates op right, C holds original 0th bit
    int RRC(ReadWritable op){
        int original = op.read();
        int bit0 = original & 1;

        regs.flags.setFlag(CFLAG, bit0 == 1);

        int result = (original >> 1) | (bit0 << 7);

        regs.flags.setFlag(ZFLAG, result == 0);

        op.write(result);

        return result;
    }

    int RRA(){
        return RR(regs.A);
    }

    //rotates op right, with C treated as the -1th bit
    int RR(ReadWritable op){
        int original = op.read();
        int bit0 = original & 1;

        int carryBit = (regs.flags.getFlag(CFLAG)? 1 : 0);

        int result = (original >> 1) | (carryBit << 7);

        regs.flags.setFlag(CFLAG, bit0 == 1);
        regs.flags.setFlag(ZFLAG, result == 0);

        op.write(result);

        return result;
    }

    int SLA(ReadWritable op){
        int original = op.read();
        int bit7 = (original >> 7) & 1;

        int result = (original << 1) & 0xff;

        regs.flags.setFlag(CFLAG, bit7 == 1);
        regs.flags.setFlag(ZFLAG, result == 0);

        op.write(result);

        return result;
    }

    int SRA(ReadWritable op) {
        int original = op.read();
        int bit0 = original & 1;
        int bit7 = (original >> 7) & 1;

        int result = ((original >> 1) | (bit7 << 7)) & 0xff;

        regs.flags.setFlag(CFLAG, bit0 == 1);
        regs.flags.setFlag(ZFLAG, result == 0);

        op.write(result);

        return result;
    }

    int SRL(ReadWritable op) {
        int original = op.read();
        int bit0 = original & 1;

        int result = (original >> 1);

        regs.flags.setFlag(CFLAG, bit0 == 1);
        regs.flags.setFlag(ZFLAG, result == 0);

        op.write(result);

        return result;
    }

    int BIT(int bitnum, Readable op) {
        int val = op.read();

        regs.flags.setFlag(ZFLAG, ((val >> bitnum) & 1) == 0);

        return val;
    }

    int SET(int bitnum, ReadWritable op) {
        int val = op.read();

        val |= (1 << bitnum);

        op.write(val);

        return val;
    }

    int RES(int bitnum, ReadWritable op) {
        int val = op.read();

        val &= ~(1 << bitnum);

        op.write(val);

        return val;
    }

    int JP(Readable jumpLocation) {
        int location = jumpLocation.read();

        regs.PC.write(location);

        return ABSJUMP;
    }

    int JP(Condition cond, Readable jumpLocation) {
        if(evaluateCondition(cond)){
            return JP(jumpLocation);
        }else{
            return NOJUMP;
        }
    }

    int JR(Readable offset){
        int location = regs.PC.read() + (byte)offset.read(); //the offset is signed

        regs.PC.write(location);

        return RELJUMP;
    }

    int JR(Condition cond, Readable offset) {
        if(evaluateCondition(cond)){
            return JR(offset);
        }else{
            return NOJUMP;
        }
    }

    int CALL(Readable jumpLocation) {
        int nextPC = regs.PC.read() + 3; //CALL is 3 bytes long

        LongRegister temp = new LongRegister();
        temp.write(nextPC);

        //push next PC onto stack
        PUSH(temp);

        return JP(jumpLocation);
    }

    int CALL(Condition cond, Readable jumpLocation) {
        if(evaluateCondition(cond)){
            return CALL(jumpLocation);
        }else{
            return NOJUMP;
        }
    }

    //push next pc onto stack and jump to n
    int RST(int n){ //n = 0, 8, 16, 24, 32, ... 56
        LongRegister nextPC = new LongRegister();
        nextPC.write(regs.PC.read() + 1); //an RST instruction is one byte long

        PUSH(nextPC);

        LongRegister temp = new LongRegister();
        temp.write(n);

        return JP(temp);
    }

    //pop two bytes from stack & jump there
    int RET(){
        LongRegister temp = new LongRegister();

        POP(temp);

        return JP(temp);
    }

    int RET(Condition cond){
        if(evaluateCondition(cond)){
            return RET();
        }else{
            return NOJUMP;
        }
    }

    //return while enabling interrupts
    int RETI(){
        EI();

        return RET();
    }

    static Operation[] operations = new Operation[256];
    {
        operations[0x0] = new Operation("NOP", CPU::NOP, 1, "- - - -", 4);
        operations[0x1] = new Operation("LD BC,d16", (CPU cpu) -> cpu.LD(cpu.regs.BC, cpu.d16()), 3, "- - - -", 12);
        operations[0x2] = new Operation("LD (BC),A", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(cpu.regs.BC), cpu.regs.A), 1, "- - - -", 8);
        operations[0x3] = new Operation("INC BC", (CPU cpu) -> cpu.INC(cpu.regs.BC), 1, "- - - -", 8);
        operations[0x4] = new Operation("INC B", (CPU cpu) -> cpu.INC(cpu.regs.B), 1, "Z 0 H -", 4);
        operations[0x5] = new Operation("DEC B", (CPU cpu) -> cpu.DEC(cpu.regs.B), 1, "Z 1 H -", 4);
        operations[0x6] = new Operation("LD B,d8", (CPU cpu) -> cpu.LD(cpu.regs.B, cpu.d8()), 2, "- - - -", 8);
        operations[0x7] = new Operation("RLCA", CPU::RLCA, 1, "0 0 0 C", 4);
        operations[0x8] = new Operation("LD (a16),SP", (CPU cpu) -> cpu.LD(cpu.mem.a16Location(cpu.regs.PC), cpu.regs.SP), 3, "- - - -", 20);
        operations[0x9] = new Operation("ADD HL,BC", (CPU cpu) -> cpu.ADD(cpu.regs.HL, cpu.regs.BC), 1, "- 0 H C", 8);
        operations[0xa] = new Operation("LD A,(BC)", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.mem.registerLocation(cpu.regs.BC)), 1, "- - - -", 8);
        operations[0xb] = new Operation("DEC BC", (CPU cpu) -> cpu.DEC(cpu.regs.BC), 1, "- - - -", 8);
        operations[0xc] = new Operation("INC C", (CPU cpu) -> cpu.INC(cpu.regs.C), 1, "Z 0 H -", 4);
        operations[0xd] = new Operation("DEC C", (CPU cpu) -> cpu.DEC(cpu.regs.C), 1, "Z 1 H -", 4);
        operations[0xe] = new Operation("LD C,d8", (CPU cpu) -> cpu.LD(cpu.regs.C, cpu.d8()), 2, "- - - -", 8);
        operations[0xf] = new Operation("RRCA", CPU::RRCA, 1, "0 0 0 C", 4);
        operations[0x10] = new Operation("STOP", CPU::STOP, 2, "- - - -", 4);
        operations[0x11] = new Operation("LD DE,d16", (CPU cpu) -> cpu.LD(cpu.regs.DE, cpu.d16()), 3, "- - - -", 12);
        operations[0x12] = new Operation("LD (DE),A", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(cpu.regs.DE), cpu.regs.A), 1, "- - - -", 8);
        operations[0x13] = new Operation("INC DE", (CPU cpu) -> cpu.INC(cpu.regs.DE), 1, "- - - -", 8);
        operations[0x14] = new Operation("INC D", (CPU cpu) -> cpu.INC(cpu.regs.D), 1, "Z 0 H -", 4);
        operations[0x15] = new Operation("DEC D", (CPU cpu) -> cpu.DEC(cpu.regs.D), 1, "Z 1 H -", 4);
        operations[0x16] = new Operation("LD D,d8", (CPU cpu) -> cpu.LD(cpu.regs.D, cpu.d8()), 2, "- - - -", 8);
        operations[0x17] = new Operation("RLA", CPU::RLA, 1, "0 0 0 C", 4);
        operations[0x18] = new Jump("JR r8", (CPU cpu) -> cpu.JR(cpu.d8()), 2, "- - - -", 12, 12);
        operations[0x19] = new Operation("ADD HL,DE", (CPU cpu) -> cpu.ADD(cpu.regs.HL, cpu.regs.DE), 1, "- 0 H C", 8);
        operations[0x1a] = new Operation("LD A,(DE)", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.mem.registerLocation(cpu.regs.DE)), 1, "- - - -", 8);
        operations[0x1b] = new Operation("DEC DE", (CPU cpu) -> cpu.DEC(cpu.regs.DE), 1, "- - - -", 8);
        operations[0x1c] = new Operation("INC E", (CPU cpu) -> cpu.INC(cpu.regs.E), 1, "Z 0 H -", 4);
        operations[0x1d] = new Operation("DEC E", (CPU cpu) -> cpu.DEC(cpu.regs.E), 1, "Z 1 H -", 4);
        operations[0x1e] = new Operation("LD E,d8", (CPU cpu) -> cpu.LD(cpu.regs.E, cpu.d8()), 2, "- - - -", 8);
        operations[0x1f] = new Operation("RRA", CPU::RRA, 1, "0 0 0 C", 4);
        operations[0x20] = new Jump("JR NZ,r8", (CPU cpu) -> cpu.JR(Condition.NZ, cpu.d8()), 2, "- - - -", 12, 8);
        operations[0x21] = new Operation("LD HL,d16", (CPU cpu) -> cpu.LD(cpu.regs.HL, cpu.d16()), 3, "- - - -", 12);
        operations[0x22] = new Operation("LD (HL+),A", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(selfIncrement(cpu.regs.HL)), cpu.regs.A), 1, "- - - -", 8);
        operations[0x23] = new Operation("INC HL", (CPU cpu) -> cpu.INC(cpu.regs.HL), 1, "- - - -", 8);
        operations[0x24] = new Operation("INC H", (CPU cpu) -> cpu.INC(cpu.regs.H), 1, "Z 0 H -", 4);
        operations[0x25] = new Operation("DEC H", (CPU cpu) -> cpu.DEC(cpu.regs.H), 1, "Z 1 H -", 4);
        operations[0x26] = new Operation("LD H,d8", (CPU cpu) -> cpu.LD(cpu.regs.H, cpu.d8()), 2, "- - - -", 8);
        operations[0x27] = new Operation("DAA", CPU::DAA, 1, "Z - 0 C", 4);
        operations[0x28] = new Jump("JR Z,r8", (CPU cpu) -> cpu.JR(Condition.Z, cpu.d8()), 2, "- - - -", 12, 8);
        operations[0x29] = new Operation("ADD HL,HL", (CPU cpu) -> cpu.ADD(cpu.regs.HL, cpu.regs.HL), 1, "- 0 H C", 8);
        operations[0x2a] = new Operation("LD A,(HL+)", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.mem.registerLocation(selfIncrement(cpu.regs.HL))), 1, "- - - -", 8);
        operations[0x2b] = new Operation("DEC HL", (CPU cpu) -> cpu.DEC(cpu.regs.HL), 1, "- - - -", 8);
        operations[0x2c] = new Operation("INC L", (CPU cpu) -> cpu.INC(cpu.regs.L), 1, "Z 0 H -", 4);
        operations[0x2d] = new Operation("DEC L", (CPU cpu) -> cpu.DEC(cpu.regs.L), 1, "Z 1 H -", 4);
        operations[0x2e] = new Operation("LD L,d8", (CPU cpu) -> cpu.LD(cpu.regs.L, cpu.d8()), 2, "- - - -", 8);
        operations[0x2f] = new Operation("CPL", CPU::CPL, 1, "- 1 1 -", 4);
        operations[0x30] = new Jump("JR NC,r8", (CPU cpu) -> cpu.JR(Condition.NC, cpu.d8()), 2, "- - - -", 12, 8);
        operations[0x31] = new Operation("LD SP,d16", (CPU cpu) -> cpu.LD(cpu.regs.SP, cpu.d16()), 3, "- - - -", 12);
        operations[0x32] = new Operation("LD (HL-),A", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(selfDecrement(cpu.regs.HL)), cpu.regs.A), 1, "- - - -", 8);
        operations[0x33] = new Operation("INC SP", (CPU cpu) -> cpu.INC(cpu.regs.SP), 1, "- - - -", 8);
        operations[0x34] = new Operation("INC (HL)", (CPU cpu) -> cpu.INC(cpu.mem.registerLocation(cpu.regs.HL)), 1, "Z 0 H -", 12);
        operations[0x35] = new Operation("DEC (HL)", (CPU cpu) -> cpu.DEC(cpu.mem.registerLocation(cpu.regs.HL)), 1, "Z 1 H -", 12);
        operations[0x36] = new Operation("LD (HL),d8", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(cpu.regs.HL), cpu.d8()), 2, "- - - -", 12);
        operations[0x37] = new Operation("SCF", CPU::SCF, 1, "- 0 0 1", 4);
        operations[0x38] = new Jump("JR C(cond),r8", (CPU cpu) -> cpu.JR(Condition.C, cpu.d8()), 2, "- - - -", 12, 8);
        operations[0x39] = new Operation("ADD HL,SP", (CPU cpu) -> cpu.ADD(cpu.regs.HL, cpu.regs.SP), 1, "- 0 H C", 8);
        operations[0x3a] = new Operation("LD A,(HL-)", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.mem.registerLocation(selfDecrement(cpu.regs.HL))), 1, "- - - -", 8);
        operations[0x3b] = new Operation("DEC SP", (CPU cpu) -> cpu.DEC(cpu.regs.SP), 1, "- - - -", 8);
        operations[0x3c] = new Operation("INC A", (CPU cpu) -> cpu.INC(cpu.regs.A), 1, "Z 0 H -", 4);
        operations[0x3d] = new Operation("DEC A", (CPU cpu) -> cpu.DEC(cpu.regs.A), 1, "Z 1 H -", 4);
        operations[0x3e] = new Operation("LD A,d8", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.d8()), 2, "- - - -", 8);
        operations[0x3f] = new Operation("CCF", CPU::CCF, 1, "- 0 0 C", 4);
        operations[0x40] = new Operation("LD B,B", (CPU cpu) -> cpu.LD(cpu.regs.B, cpu.regs.B), 1, "- - - -", 4);
        operations[0x41] = new Operation("LD B,C", (CPU cpu) -> cpu.LD(cpu.regs.B, cpu.regs.C), 1, "- - - -", 4);
        operations[0x42] = new Operation("LD B,D", (CPU cpu) -> cpu.LD(cpu.regs.B, cpu.regs.D), 1, "- - - -", 4);
        operations[0x43] = new Operation("LD B,E", (CPU cpu) -> cpu.LD(cpu.regs.B, cpu.regs.E), 1, "- - - -", 4);
        operations[0x44] = new Operation("LD B,H", (CPU cpu) -> cpu.LD(cpu.regs.B, cpu.regs.H), 1, "- - - -", 4);
        operations[0x45] = new Operation("LD B,L", (CPU cpu) -> cpu.LD(cpu.regs.B, cpu.regs.L), 1, "- - - -", 4);
        operations[0x46] = new Operation("LD B,(HL)", (CPU cpu) -> cpu.LD(cpu.regs.B, cpu.mem.registerLocation(cpu.regs.HL)), 1, "- - - -", 8);
        operations[0x47] = new Operation("LD B,A", (CPU cpu) -> cpu.LD(cpu.regs.B, cpu.regs.A), 1, "- - - -", 4);
        operations[0x48] = new Operation("LD C,B", (CPU cpu) -> cpu.LD(cpu.regs.C, cpu.regs.B), 1, "- - - -", 4);
        operations[0x49] = new Operation("LD C,C", (CPU cpu) -> cpu.LD(cpu.regs.C, cpu.regs.C), 1, "- - - -", 4);
        operations[0x4a] = new Operation("LD C,D", (CPU cpu) -> cpu.LD(cpu.regs.C, cpu.regs.D), 1, "- - - -", 4);
        operations[0x4b] = new Operation("LD C,E", (CPU cpu) -> cpu.LD(cpu.regs.C, cpu.regs.E), 1, "- - - -", 4);
        operations[0x4c] = new Operation("LD C,H", (CPU cpu) -> cpu.LD(cpu.regs.C, cpu.regs.H), 1, "- - - -", 4);
        operations[0x4d] = new Operation("LD C,L", (CPU cpu) -> cpu.LD(cpu.regs.C, cpu.regs.L), 1, "- - - -", 4);
        operations[0x4e] = new Operation("LD C,(HL)", (CPU cpu) -> cpu.LD(cpu.regs.C, cpu.mem.registerLocation(cpu.regs.HL)), 1, "- - - -", 8);
        operations[0x4f] = new Operation("LD C,A", (CPU cpu) -> cpu.LD(cpu.regs.C, cpu.regs.A), 1, "- - - -", 4);
        operations[0x50] = new Operation("LD D,B", (CPU cpu) -> cpu.LD(cpu.regs.D, cpu.regs.B), 1, "- - - -", 4);
        operations[0x51] = new Operation("LD D,C", (CPU cpu) -> cpu.LD(cpu.regs.D, cpu.regs.C), 1, "- - - -", 4);
        operations[0x52] = new Operation("LD D,D", (CPU cpu) -> cpu.LD(cpu.regs.D, cpu.regs.D), 1, "- - - -", 4);
        operations[0x53] = new Operation("LD D,E", (CPU cpu) -> cpu.LD(cpu.regs.D, cpu.regs.E), 1, "- - - -", 4);
        operations[0x54] = new Operation("LD D,H", (CPU cpu) -> cpu.LD(cpu.regs.D, cpu.regs.H), 1, "- - - -", 4);
        operations[0x55] = new Operation("LD D,L", (CPU cpu) -> cpu.LD(cpu.regs.D, cpu.regs.L), 1, "- - - -", 4);
        operations[0x56] = new Operation("LD D,(HL)", (CPU cpu) -> cpu.LD(cpu.regs.D, cpu.mem.registerLocation(cpu.regs.HL)), 1, "- - - -", 8);
        operations[0x57] = new Operation("LD D,A", (CPU cpu) -> cpu.LD(cpu.regs.D, cpu.regs.A), 1, "- - - -", 4);
        operations[0x58] = new Operation("LD E,B", (CPU cpu) -> cpu.LD(cpu.regs.E, cpu.regs.B), 1, "- - - -", 4);
        operations[0x59] = new Operation("LD E,C", (CPU cpu) -> cpu.LD(cpu.regs.E, cpu.regs.C), 1, "- - - -", 4);
        operations[0x5a] = new Operation("LD E,D", (CPU cpu) -> cpu.LD(cpu.regs.E, cpu.regs.D), 1, "- - - -", 4);
        operations[0x5b] = new Operation("LD E,E", (CPU cpu) -> cpu.LD(cpu.regs.E, cpu.regs.E), 1, "- - - -", 4);
        operations[0x5c] = new Operation("LD E,H", (CPU cpu) -> cpu.LD(cpu.regs.E, cpu.regs.H), 1, "- - - -", 4);
        operations[0x5d] = new Operation("LD E,L", (CPU cpu) -> cpu.LD(cpu.regs.E, cpu.regs.L), 1, "- - - -", 4);
        operations[0x5e] = new Operation("LD E,(HL)", (CPU cpu) -> cpu.LD(cpu.regs.E, cpu.mem.registerLocation(cpu.regs.HL)), 1, "- - - -", 8);
        operations[0x5f] = new Operation("LD E,A", (CPU cpu) -> cpu.LD(cpu.regs.E, cpu.regs.A), 1, "- - - -", 4);
        operations[0x60] = new Operation("LD H,B", (CPU cpu) -> cpu.LD(cpu.regs.H, cpu.regs.B), 1, "- - - -", 4);
        operations[0x61] = new Operation("LD H,C", (CPU cpu) -> cpu.LD(cpu.regs.H, cpu.regs.C), 1, "- - - -", 4);
        operations[0x62] = new Operation("LD H,D", (CPU cpu) -> cpu.LD(cpu.regs.H, cpu.regs.D), 1, "- - - -", 4);
        operations[0x63] = new Operation("LD H,E", (CPU cpu) -> cpu.LD(cpu.regs.H, cpu.regs.E), 1, "- - - -", 4);
        operations[0x64] = new Operation("LD H,H", (CPU cpu) -> cpu.LD(cpu.regs.H, cpu.regs.H), 1, "- - - -", 4);
        operations[0x65] = new Operation("LD H,L", (CPU cpu) -> cpu.LD(cpu.regs.H, cpu.regs.L), 1, "- - - -", 4);
        operations[0x66] = new Operation("LD H,(HL)", (CPU cpu) -> cpu.LD(cpu.regs.H, cpu.mem.registerLocation(cpu.regs.HL)), 1, "- - - -", 8);
        operations[0x67] = new Operation("LD H,A", (CPU cpu) -> cpu.LD(cpu.regs.H, cpu.regs.A), 1, "- - - -", 4);
        operations[0x68] = new Operation("LD L,B", (CPU cpu) -> cpu.LD(cpu.regs.L, cpu.regs.B), 1, "- - - -", 4);
        operations[0x69] = new Operation("LD L,C", (CPU cpu) -> cpu.LD(cpu.regs.L, cpu.regs.C), 1, "- - - -", 4);
        operations[0x6a] = new Operation("LD L,D", (CPU cpu) -> cpu.LD(cpu.regs.L, cpu.regs.D), 1, "- - - -", 4);
        operations[0x6b] = new Operation("LD L,E", (CPU cpu) -> cpu.LD(cpu.regs.L, cpu.regs.E), 1, "- - - -", 4);
        operations[0x6c] = new Operation("LD L,H", (CPU cpu) -> cpu.LD(cpu.regs.L, cpu.regs.H), 1, "- - - -", 4);
        operations[0x6d] = new Operation("LD L,L", (CPU cpu) -> cpu.LD(cpu.regs.L, cpu.regs.L), 1, "- - - -", 4);
        operations[0x6e] = new Operation("LD L,(HL)", (CPU cpu) -> cpu.LD(cpu.regs.L, cpu.mem.registerLocation(cpu.regs.HL)), 1, "- - - -", 8);
        operations[0x6f] = new Operation("LD L,A", (CPU cpu) -> cpu.LD(cpu.regs.L, cpu.regs.A), 1, "- - - -", 4);
        operations[0x70] = new Operation("LD (HL),B", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(cpu.regs.HL), cpu.regs.B), 1, "- - - -", 8);
        operations[0x71] = new Operation("LD (HL),C", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(cpu.regs.HL), cpu.regs.C), 1, "- - - -", 8);
        operations[0x72] = new Operation("LD (HL),D", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(cpu.regs.HL), cpu.regs.D), 1, "- - - -", 8);
        operations[0x73] = new Operation("LD (HL),E", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(cpu.regs.HL), cpu.regs.E), 1, "- - - -", 8);
        operations[0x74] = new Operation("LD (HL),H", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(cpu.regs.HL), cpu.regs.H), 1, "- - - -", 8);
        operations[0x75] = new Operation("LD (HL),L", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(cpu.regs.HL), cpu.regs.L), 1, "- - - -", 8);
        operations[0x76] = new Operation("HALT", CPU::HALT, 1, "- - - -", 4);
        operations[0x77] = new Operation("LD (HL),A", (CPU cpu) -> cpu.LD(cpu.mem.registerLocation(cpu.regs.HL), cpu.regs.A), 1, "- - - -", 8);
        operations[0x78] = new Operation("LD A,B", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.regs.B), 1, "- - - -", 4);
        operations[0x79] = new Operation("LD A,C", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.regs.C), 1, "- - - -", 4);
        operations[0x7a] = new Operation("LD A,D", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.regs.D), 1, "- - - -", 4);
        operations[0x7b] = new Operation("LD A,E", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.regs.E), 1, "- - - -", 4);
        operations[0x7c] = new Operation("LD A,H", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.regs.H), 1, "- - - -", 4);
        operations[0x7d] = new Operation("LD A,L", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.regs.L), 1, "- - - -", 4);
        operations[0x7e] = new Operation("LD A,(HL)", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.mem.registerLocation(cpu.regs.HL)), 1, "- - - -", 8);
        operations[0x7f] = new Operation("LD A,A", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.regs.A), 1, "- - - -", 4);
        operations[0x80] = new Operation("ADD A,B", (CPU cpu) -> cpu.ADD(cpu.regs.A, cpu.regs.B), 1, "Z 0 H C", 4);
        operations[0x81] = new Operation("ADD A,C", (CPU cpu) -> cpu.ADD(cpu.regs.A, cpu.regs.C), 1, "Z 0 H C", 4);
        operations[0x82] = new Operation("ADD A,D", (CPU cpu) -> cpu.ADD(cpu.regs.A, cpu.regs.D), 1, "Z 0 H C", 4);
        operations[0x83] = new Operation("ADD A,E", (CPU cpu) -> cpu.ADD(cpu.regs.A, cpu.regs.E), 1, "Z 0 H C", 4);
        operations[0x84] = new Operation("ADD A,H", (CPU cpu) -> cpu.ADD(cpu.regs.A, cpu.regs.H), 1, "Z 0 H C", 4);
        operations[0x85] = new Operation("ADD A,L", (CPU cpu) -> cpu.ADD(cpu.regs.A, cpu.regs.L), 1, "Z 0 H C", 4);
        operations[0x86] = new Operation("ADD A,(HL)", (CPU cpu) -> cpu.ADD(cpu.regs.A, cpu.mem.registerLocation(cpu.regs.HL)), 1, "Z 0 H C", 8);
        operations[0x87] = new Operation("ADD A,A", (CPU cpu) -> cpu.ADD(cpu.regs.A, cpu.regs.A), 1, "Z 0 H C", 4);
        operations[0x88] = new Operation("ADC A,B", (CPU cpu) -> cpu.ADC(cpu.regs.A, cpu.regs.B), 1, "Z 0 H C", 4);
        operations[0x89] = new Operation("ADC A,C", (CPU cpu) -> cpu.ADC(cpu.regs.A, cpu.regs.C), 1, "Z 0 H C", 4);
        operations[0x8a] = new Operation("ADC A,D", (CPU cpu) -> cpu.ADC(cpu.regs.A, cpu.regs.D), 1, "Z 0 H C", 4);
        operations[0x8b] = new Operation("ADC A,E", (CPU cpu) -> cpu.ADC(cpu.regs.A, cpu.regs.E), 1, "Z 0 H C", 4);
        operations[0x8c] = new Operation("ADC A,H", (CPU cpu) -> cpu.ADC(cpu.regs.A, cpu.regs.H), 1, "Z 0 H C", 4);
        operations[0x8d] = new Operation("ADC A,L", (CPU cpu) -> cpu.ADC(cpu.regs.A, cpu.regs.L), 1, "Z 0 H C", 4);
        operations[0x8e] = new Operation("ADC A,(HL)", (CPU cpu) -> cpu.ADC(cpu.regs.A, cpu.mem.registerLocation(cpu.regs.HL)), 1, "Z 0 H C", 8);
        operations[0x8f] = new Operation("ADC A,A", (CPU cpu) -> cpu.ADC(cpu.regs.A, cpu.regs.A), 1, "Z 0 H C", 4);
        operations[0x90] = new Operation("SUB B", (CPU cpu) -> cpu.SUB(cpu.regs.B), 1, "Z 1 H C", 4);
        operations[0x91] = new Operation("SUB C", (CPU cpu) -> cpu.SUB(cpu.regs.C), 1, "Z 1 H C", 4);
        operations[0x92] = new Operation("SUB D", (CPU cpu) -> cpu.SUB(cpu.regs.D), 1, "Z 1 H C", 4);
        operations[0x93] = new Operation("SUB E", (CPU cpu) -> cpu.SUB(cpu.regs.E), 1, "Z 1 H C", 4);
        operations[0x94] = new Operation("SUB H", (CPU cpu) -> cpu.SUB(cpu.regs.H), 1, "Z 1 H C", 4);
        operations[0x95] = new Operation("SUB L", (CPU cpu) -> cpu.SUB(cpu.regs.L), 1, "Z 1 H C", 4);
        operations[0x96] = new Operation("SUB (HL)", (CPU cpu) -> cpu.SUB(cpu.mem.registerLocation(cpu.regs.HL)), 1, "Z 1 H C", 8);
        operations[0x97] = new Operation("SUB A", (CPU cpu) -> cpu.SUB(cpu.regs.A), 1, "Z 1 H C", 4);
        operations[0x98] = new Operation("SBC B", (CPU cpu) -> cpu.SBC(cpu.regs.B), 1, "Z 1 H C", 4);
        operations[0x99] = new Operation("SBC C", (CPU cpu) -> cpu.SBC(cpu.regs.C), 1, "Z 1 H C", 4);
        operations[0x9a] = new Operation("SBC D", (CPU cpu) -> cpu.SBC(cpu.regs.D), 1, "Z 1 H C", 4);
        operations[0x9b] = new Operation("SBC E", (CPU cpu) -> cpu.SBC(cpu.regs.E), 1, "Z 1 H C", 4);
        operations[0x9c] = new Operation("SBC H", (CPU cpu) -> cpu.SBC(cpu.regs.H), 1, "Z 1 H C", 4);
        operations[0x9d] = new Operation("SBC L", (CPU cpu) -> cpu.SBC(cpu.regs.L), 1, "Z 1 H C", 4);
        operations[0x9e] = new Operation("SBC (HL)", (CPU cpu) -> cpu.SBC(cpu.mem.registerLocation(cpu.regs.HL)), 1, "Z 1 H C", 8);
        operations[0x9f] = new Operation("SBC A", (CPU cpu) -> cpu.SBC(cpu.regs.A), 1, "Z 1 H C", 4);
        operations[0xa0] = new Operation("AND B", (CPU cpu) -> cpu.AND(cpu.regs.B), 1, "Z 0 1 0", 4);
        operations[0xa1] = new Operation("AND C", (CPU cpu) -> cpu.AND(cpu.regs.C), 1, "Z 0 1 0", 4);
        operations[0xa2] = new Operation("AND D", (CPU cpu) -> cpu.AND(cpu.regs.D), 1, "Z 0 1 0", 4);
        operations[0xa3] = new Operation("AND E", (CPU cpu) -> cpu.AND(cpu.regs.E), 1, "Z 0 1 0", 4);
        operations[0xa4] = new Operation("AND H", (CPU cpu) -> cpu.AND(cpu.regs.H), 1, "Z 0 1 0", 4);
        operations[0xa5] = new Operation("AND L", (CPU cpu) -> cpu.AND(cpu.regs.L), 1, "Z 0 1 0", 4);
        operations[0xa6] = new Operation("AND (HL)", (CPU cpu) -> cpu.AND(cpu.mem.registerLocation(cpu.regs.HL)), 1, "Z 0 1 0", 8);
        operations[0xa7] = new Operation("AND A", (CPU cpu) -> cpu.AND(cpu.regs.A), 1, "Z 0 1 0", 4);
        operations[0xa8] = new Operation("XOR B", (CPU cpu) -> cpu.XOR(cpu.regs.B), 1, "Z 0 0 0", 4);
        operations[0xa9] = new Operation("XOR C", (CPU cpu) -> cpu.XOR(cpu.regs.C), 1, "Z 0 0 0", 4);
        operations[0xaa] = new Operation("XOR D", (CPU cpu) -> cpu.XOR(cpu.regs.D), 1, "Z 0 0 0", 4);
        operations[0xab] = new Operation("XOR E", (CPU cpu) -> cpu.XOR(cpu.regs.E), 1, "Z 0 0 0", 4);
        operations[0xac] = new Operation("XOR H", (CPU cpu) -> cpu.XOR(cpu.regs.H), 1, "Z 0 0 0", 4);
        operations[0xad] = new Operation("XOR L", (CPU cpu) -> cpu.XOR(cpu.regs.L), 1, "Z 0 0 0", 4);
        operations[0xae] = new Operation("XOR (HL)", (CPU cpu) -> cpu.XOR(cpu.mem.registerLocation(cpu.regs.HL)), 1, "Z 0 0 0", 8);
        operations[0xaf] = new Operation("XOR A", (CPU cpu) -> cpu.XOR(cpu.regs.A), 1, "Z 0 0 0", 4);
        operations[0xb0] = new Operation("OR B", (CPU cpu) -> cpu.OR(cpu.regs.B), 1, "Z 0 0 0", 4);
        operations[0xb1] = new Operation("OR C", (CPU cpu) -> cpu.OR(cpu.regs.C), 1, "Z 0 0 0", 4);
        operations[0xb2] = new Operation("OR D", (CPU cpu) -> cpu.OR(cpu.regs.D), 1, "Z 0 0 0", 4);
        operations[0xb3] = new Operation("OR E", (CPU cpu) -> cpu.OR(cpu.regs.E), 1, "Z 0 0 0", 4);
        operations[0xb4] = new Operation("OR H", (CPU cpu) -> cpu.OR(cpu.regs.H), 1, "Z 0 0 0", 4);
        operations[0xb5] = new Operation("OR L", (CPU cpu) -> cpu.OR(cpu.regs.L), 1, "Z 0 0 0", 4);
        operations[0xb6] = new Operation("OR (HL)", (CPU cpu) -> cpu.OR(cpu.mem.registerLocation(cpu.regs.HL)), 1, "Z 0 0 0", 8);
        operations[0xb7] = new Operation("OR A", (CPU cpu) -> cpu.OR(cpu.regs.A), 1, "Z 0 0 0", 4);
        operations[0xb8] = new Operation("CP B", (CPU cpu) -> cpu.CP(cpu.regs.B), 1, "Z 1 H C", 4);
        operations[0xb9] = new Operation("CP C", (CPU cpu) -> cpu.CP(cpu.regs.C), 1, "Z 1 H C", 4);
        operations[0xba] = new Operation("CP D", (CPU cpu) -> cpu.CP(cpu.regs.D), 1, "Z 1 H C", 4);
        operations[0xbb] = new Operation("CP E", (CPU cpu) -> cpu.CP(cpu.regs.E), 1, "Z 1 H C", 4);
        operations[0xbc] = new Operation("CP H", (CPU cpu) -> cpu.CP(cpu.regs.H), 1, "Z 1 H C", 4);
        operations[0xbd] = new Operation("CP L", (CPU cpu) -> cpu.CP(cpu.regs.L), 1, "Z 1 H C", 4);
        operations[0xbe] = new Operation("CP (HL)", (CPU cpu) -> cpu.CP(cpu.mem.registerLocation(cpu.regs.HL)), 1, "Z 1 H C", 8);
        operations[0xbf] = new Operation("CP A", (CPU cpu) -> cpu.CP(cpu.regs.A), 1, "Z 1 H C", 4);
        operations[0xc0] = new Jump("RET NZ", (CPU cpu) -> cpu.RET(Condition.NZ), 1, "- - - -", 20, 8);
        operations[0xc1] = new Operation("POP BC", (CPU cpu) -> cpu.POP(cpu.regs.BC), 1, "- - - -", 12);
        operations[0xc2] = new Jump("JP NZ,a16", (CPU cpu) -> cpu.JP(Condition.NZ, cpu.a16()), 3, "- - - -", 16, 12);
        operations[0xc3] = new Jump("JP a16", (CPU cpu) -> cpu.JP(cpu.a16()), 3, "- - - -", 16, 16);
        operations[0xc4] = new Jump("CALL NZ,a16", (CPU cpu) -> cpu.CALL(Condition.NZ, cpu.a16()), 3, "- - - -", 24, 12);
        operations[0xc5] = new Operation("PUSH BC", (CPU cpu) -> cpu.PUSH(cpu.regs.BC), 1, "- - - -", 16);
        operations[0xc6] = new Operation("ADD A,d8", (CPU cpu) -> cpu.ADD(cpu.regs.A, cpu.d8()), 2, "Z 0 H C", 8);
        operations[0xc7] = new Jump("RST 00H", (CPU cpu) -> cpu.RST(0x00), 1, "- - - -", 16, 16);
        operations[0xc8] = new Jump("RET Z", (CPU cpu) -> cpu.RET(Condition.Z), 1, "- - - -", 20, 8);
        operations[0xc9] = new Jump("RET", CPU::RET, 1, "- - - -", 16, 16);
        operations[0xca] = new Jump("JP Z,a16", (CPU cpu) -> cpu.JP(Condition.Z, cpu.a16()), 3, "- - - -", 16, 12);
        operations[0xcb] = new CB();
        operations[0xcc] = new Jump("CALL Z,a16", (CPU cpu) -> cpu.CALL(Condition.Z, cpu.a16()), 3, "- - - -", 24, 12);
        operations[0xcd] = new Jump("CALL a16", (CPU cpu) -> cpu.CALL(cpu.a16()), 3, "- - - -", 24, 24);
        operations[0xce] = new Operation("ADC A,d8", (CPU cpu) -> cpu.ADC(cpu.regs.A, cpu.d8()), 2, "Z 0 H C", 8);
        operations[0xcf] = new Jump("RST 08H", (CPU cpu) -> cpu.RST(0x08), 1, "- - - -", 16, 16);
        operations[0xd0] = new Jump("RET NC", (CPU cpu) -> cpu.RET(Condition.NC), 1, "- - - -", 20, 8);
        operations[0xd1] = new Operation("POP DE", (CPU cpu) -> cpu.POP(cpu.regs.DE), 1, "- - - -", 12);
        operations[0xd2] = new Jump("JP NC,a16", (CPU cpu) -> cpu.JP(Condition.NC, cpu.a16()), 3, "- - - -", 16, 12);
        operations[0xd3] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xd4] = new Jump("CALL NC,a16", (CPU cpu) -> cpu.CALL(Condition.NC, cpu.a16()), 3, "- - - -", 24, 12);
        operations[0xd5] = new Operation("PUSH DE", (CPU cpu) -> cpu.PUSH(cpu.regs.DE), 1, "- - - -", 16);
        operations[0xd6] = new Operation("SUB d8", (CPU cpu) -> cpu.SUB(cpu.d8()), 2, "Z 1 H C", 8);
        operations[0xd7] = new Jump("RST 10H", (CPU cpu) -> cpu.RST(0x10), 1, "- - - -", 16, 16);
        operations[0xd8] = new Jump("RET C(cond)", (CPU cpu) -> cpu.RET(Condition.C), 1, "- - - -", 20, 8);
        operations[0xd9] = new Jump("RETI", CPU::RETI, 1, "- - - -", 16, 16);
        operations[0xda] = new Jump("JP C(cond),a16", (CPU cpu) -> cpu.JP(Condition.C, cpu.a16()), 3, "- - - -", 16, 12);
        operations[0xdb] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xdc] = new Jump("CALL C(cond),a16", (CPU cpu) -> cpu.CALL(Condition.C, cpu.a16()), 3, "- - - -", 24, 12);
        operations[0xdd] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xde] = new Operation("SBC d8", (CPU cpu) -> cpu.SBC(cpu.d8()), 2, "Z 1 H C", 8);
        operations[0xdf] = new Jump("RST 18H", (CPU cpu) -> cpu.RST(0x18), 1, "- - - -", 16, 16);
        operations[0xe0] = new Operation("LD (a8),A", (CPU cpu) -> cpu.LD(cpu.mem.a8Location(cpu.regs.PC), cpu.regs.A), 2, "- - - -", 12);
        operations[0xe1] = new Operation("POP HL", (CPU cpu) -> cpu.POP(cpu.regs.HL), 1, "- - - -", 12);
        operations[0xe2] = new Operation("LD (C),A", (CPU cpu) -> cpu.LD(cpu.mem.shortRegisterLocation(cpu.regs.C), cpu.regs.A), 1, "- - - -", 8);
        operations[0xe3] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xe4] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xe5] = new Operation("PUSH HL", (CPU cpu) -> cpu.PUSH(cpu.regs.HL), 1, "- - - -", 16);
        operations[0xe6] = new Operation("AND d8", (CPU cpu) -> cpu.AND(cpu.d8()), 2, "Z 0 1 0", 8);
        operations[0xe7] = new Jump("RST 20H", (CPU cpu) -> cpu.RST(0x20), 1, "- - - -", 16, 16);
        operations[0xe8] = new Operation("ADD SP,r8", (CPU cpu) -> cpu.ADD(cpu.regs.SP, cpu.r8()), 2, "0 0 H C", 16);
        operations[0xe9] = new Jump("JP HL", (CPU cpu) -> cpu.JP(cpu.regs.HL), 1, "- - - -", 4, 4);
        operations[0xea] = new Operation("LD (a16),A", (CPU cpu) -> cpu.LD(cpu.mem.a16Location(cpu.regs.PC), cpu.regs.A), 3, "- - - -", 16);
        operations[0xeb] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xec] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xed] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xee] = new Operation("XOR d8", (CPU cpu) -> cpu.XOR(cpu.d8()), 2, "Z 0 0 0", 8);
        operations[0xef] = new Jump("RST 28H", (CPU cpu) -> cpu.RST(0x28), 1, "- - - -", 16, 16);
        operations[0xf0] = new Operation("LD A,(a8)", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.mem.a8Location(cpu.regs.PC)), 2, "- - - -", 12);
        operations[0xf1] = new Operation("POP AF", (CPU cpu) -> cpu.POP(cpu.regs.AF), 1, "Z N H C", 12);
        operations[0xf2] = new Operation("LD A,(C)", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.mem.shortRegisterLocation(cpu.regs.C)), 1, "- - - -", 8);
        operations[0xf3] = new Operation("DI", CPU::DI, 1, "- - - -", 4);
        operations[0xf4] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xf5] = new Operation("PUSH AF", (CPU cpu) -> cpu.PUSH(cpu.regs.AF), 1, "- - - -", 16);
        operations[0xf6] = new Operation("OR d8", (CPU cpu) -> cpu.OR(cpu.d8()), 2, "Z 0 0 0", 8);
        operations[0xf7] = new Jump("RST 30H", (CPU cpu) -> cpu.RST(0x30), 1, "- - - -", 16, 16);
        operations[0xf8] = new Operation("LD HL,SP+r8", (CPU cpu) -> cpu.LD(cpu.regs.HL, cpu.SPr8()), 2, "0 0 H C", 12);
        operations[0xf9] = new Operation("LD SP,HL", (CPU cpu) -> cpu.LD(cpu.regs.SP, cpu.regs.HL), 1, "- - - -", 8);
        operations[0xfa] = new Operation("LD A,(a16)", (CPU cpu) -> cpu.LD(cpu.regs.A, cpu.mem.a16Location(cpu.regs.PC)), 3, "- - - -", 16);
        operations[0xfb] = new Operation("EI", CPU::EI, 1, "- - - -", 4);
        operations[0xfc] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xfd] = new Operation("XXX", CPU::XXX, 0, "- - - -", 0);
        operations[0xfe] = new Operation("CP d8", (CPU cpu) -> cpu.CP(cpu.d8()), 2, "Z 1 H C", 8);
        operations[0xff] = new Jump("RST 38H", (CPU cpu) -> cpu.RST(0x38), 1, "- - - -", 16, 16);

    }

    static Operation[] cbOperations = new Operation[256];
    {
        cbOperations[0x0] = new Operation("RLC B", (CPU cpu) -> cpu.RLC(cpu.regs.B), 2, "Z 0 0 C", 8);
        cbOperations[0x1] = new Operation("RLC C", (CPU cpu) -> cpu.RLC(cpu.regs.C), 2, "Z 0 0 C", 8);
        cbOperations[0x2] = new Operation("RLC D", (CPU cpu) -> cpu.RLC(cpu.regs.D), 2, "Z 0 0 C", 8);
        cbOperations[0x3] = new Operation("RLC E", (CPU cpu) -> cpu.RLC(cpu.regs.E), 2, "Z 0 0 C", 8);
        cbOperations[0x4] = new Operation("RLC H", (CPU cpu) -> cpu.RLC(cpu.regs.H), 2, "Z 0 0 C", 8);
        cbOperations[0x5] = new Operation("RLC L", (CPU cpu) -> cpu.RLC(cpu.regs.L), 2, "Z 0 0 C", 8);
        cbOperations[0x6] = new Operation("RLC (HL)", (CPU cpu) -> cpu.RLC(cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 0 C", 16);
        cbOperations[0x7] = new Operation("RLC A", (CPU cpu) -> cpu.RLC(cpu.regs.A), 2, "Z 0 0 C", 8);
        cbOperations[0x8] = new Operation("RRC B", (CPU cpu) -> cpu.RRC(cpu.regs.B), 2, "Z 0 0 C", 8);
        cbOperations[0x9] = new Operation("RRC C", (CPU cpu) -> cpu.RRC(cpu.regs.C), 2, "Z 0 0 C", 8);
        cbOperations[0xa] = new Operation("RRC D", (CPU cpu) -> cpu.RRC(cpu.regs.D), 2, "Z 0 0 C", 8);
        cbOperations[0xb] = new Operation("RRC E", (CPU cpu) -> cpu.RRC(cpu.regs.E), 2, "Z 0 0 C", 8);
        cbOperations[0xc] = new Operation("RRC H", (CPU cpu) -> cpu.RRC(cpu.regs.H), 2, "Z 0 0 C", 8);
        cbOperations[0xd] = new Operation("RRC L", (CPU cpu) -> cpu.RRC(cpu.regs.L), 2, "Z 0 0 C", 8);
        cbOperations[0xe] = new Operation("RRC (HL)", (CPU cpu) -> cpu.RRC(cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 0 C", 16);
        cbOperations[0xf] = new Operation("RRC A", (CPU cpu) -> cpu.RRC(cpu.regs.A), 2, "Z 0 0 C", 8);
        cbOperations[0x10] = new Operation("RL B", (CPU cpu) -> cpu.RL(cpu.regs.B), 2, "Z 0 0 C", 8);
        cbOperations[0x11] = new Operation("RL C", (CPU cpu) -> cpu.RL(cpu.regs.C), 2, "Z 0 0 C", 8);
        cbOperations[0x12] = new Operation("RL D", (CPU cpu) -> cpu.RL(cpu.regs.D), 2, "Z 0 0 C", 8);
        cbOperations[0x13] = new Operation("RL E", (CPU cpu) -> cpu.RL(cpu.regs.E), 2, "Z 0 0 C", 8);
        cbOperations[0x14] = new Operation("RL H", (CPU cpu) -> cpu.RL(cpu.regs.H), 2, "Z 0 0 C", 8);
        cbOperations[0x15] = new Operation("RL L", (CPU cpu) -> cpu.RL(cpu.regs.L), 2, "Z 0 0 C", 8);
        cbOperations[0x16] = new Operation("RL (HL)", (CPU cpu) -> cpu.RL(cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 0 C", 16);
        cbOperations[0x17] = new Operation("RL A", (CPU cpu) -> cpu.RL(cpu.regs.A), 2, "Z 0 0 C", 8);
        cbOperations[0x18] = new Operation("RR B", (CPU cpu) -> cpu.RR(cpu.regs.B), 2, "Z 0 0 C", 8);
        cbOperations[0x19] = new Operation("RR C", (CPU cpu) -> cpu.RR(cpu.regs.C), 2, "Z 0 0 C", 8);
        cbOperations[0x1a] = new Operation("RR D", (CPU cpu) -> cpu.RR(cpu.regs.D), 2, "Z 0 0 C", 8);
        cbOperations[0x1b] = new Operation("RR E", (CPU cpu) -> cpu.RR(cpu.regs.E), 2, "Z 0 0 C", 8);
        cbOperations[0x1c] = new Operation("RR H", (CPU cpu) -> cpu.RR(cpu.regs.H), 2, "Z 0 0 C", 8);
        cbOperations[0x1d] = new Operation("RR L", (CPU cpu) -> cpu.RR(cpu.regs.L), 2, "Z 0 0 C", 8);
        cbOperations[0x1e] = new Operation("RR (HL)", (CPU cpu) -> cpu.RR(cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 0 C", 16);
        cbOperations[0x1f] = new Operation("RR A", (CPU cpu) -> cpu.RR(cpu.regs.A), 2, "Z 0 0 C", 8);
        cbOperations[0x20] = new Operation("SLA B", (CPU cpu) -> cpu.SLA(cpu.regs.B), 2, "Z 0 0 C", 8);
        cbOperations[0x21] = new Operation("SLA C", (CPU cpu) -> cpu.SLA(cpu.regs.C), 2, "Z 0 0 C", 8);
        cbOperations[0x22] = new Operation("SLA D", (CPU cpu) -> cpu.SLA(cpu.regs.D), 2, "Z 0 0 C", 8);
        cbOperations[0x23] = new Operation("SLA E", (CPU cpu) -> cpu.SLA(cpu.regs.E), 2, "Z 0 0 C", 8);
        cbOperations[0x24] = new Operation("SLA H", (CPU cpu) -> cpu.SLA(cpu.regs.H), 2, "Z 0 0 C", 8);
        cbOperations[0x25] = new Operation("SLA L", (CPU cpu) -> cpu.SLA(cpu.regs.L), 2, "Z 0 0 C", 8);
        cbOperations[0x26] = new Operation("SLA (HL)", (CPU cpu) -> cpu.SLA(cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 0 C", 16);
        cbOperations[0x27] = new Operation("SLA A", (CPU cpu) -> cpu.SLA(cpu.regs.A), 2, "Z 0 0 C", 8);
        cbOperations[0x28] = new Operation("SRA B", (CPU cpu) -> cpu.SRA(cpu.regs.B), 2, "Z 0 0 C", 8);
        cbOperations[0x29] = new Operation("SRA C", (CPU cpu) -> cpu.SRA(cpu.regs.C), 2, "Z 0 0 C", 8);
        cbOperations[0x2a] = new Operation("SRA D", (CPU cpu) -> cpu.SRA(cpu.regs.D), 2, "Z 0 0 C", 8);
        cbOperations[0x2b] = new Operation("SRA E", (CPU cpu) -> cpu.SRA(cpu.regs.E), 2, "Z 0 0 C", 8);
        cbOperations[0x2c] = new Operation("SRA H", (CPU cpu) -> cpu.SRA(cpu.regs.H), 2, "Z 0 0 C", 8);
        cbOperations[0x2d] = new Operation("SRA L", (CPU cpu) -> cpu.SRA(cpu.regs.L), 2, "Z 0 0 C", 8);
        cbOperations[0x2e] = new Operation("SRA (HL)", (CPU cpu) -> cpu.SRA(cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 0 C", 16);
        cbOperations[0x2f] = new Operation("SRA A", (CPU cpu) -> cpu.SRA(cpu.regs.A), 2, "Z 0 0 C", 8);
        cbOperations[0x30] = new Operation("SWAP B", (CPU cpu) -> cpu.SWAP(cpu.regs.B), 2, "Z 0 0 0", 8);
        cbOperations[0x31] = new Operation("SWAP C", (CPU cpu) -> cpu.SWAP(cpu.regs.C), 2, "Z 0 0 0", 8);
        cbOperations[0x32] = new Operation("SWAP D", (CPU cpu) -> cpu.SWAP(cpu.regs.D), 2, "Z 0 0 0", 8);
        cbOperations[0x33] = new Operation("SWAP E", (CPU cpu) -> cpu.SWAP(cpu.regs.E), 2, "Z 0 0 0", 8);
        cbOperations[0x34] = new Operation("SWAP H", (CPU cpu) -> cpu.SWAP(cpu.regs.H), 2, "Z 0 0 0", 8);
        cbOperations[0x35] = new Operation("SWAP L", (CPU cpu) -> cpu.SWAP(cpu.regs.L), 2, "Z 0 0 0", 8);
        cbOperations[0x36] = new Operation("SWAP (HL)", (CPU cpu) -> cpu.SWAP(cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 0 0", 16);
        cbOperations[0x37] = new Operation("SWAP A", (CPU cpu) -> cpu.SWAP(cpu.regs.A), 2, "Z 0 0 0", 8);
        cbOperations[0x38] = new Operation("SRL B", (CPU cpu) -> cpu.SRL(cpu.regs.B), 2, "Z 0 0 C", 8);
        cbOperations[0x39] = new Operation("SRL C", (CPU cpu) -> cpu.SRL(cpu.regs.C), 2, "Z 0 0 C", 8);
        cbOperations[0x3a] = new Operation("SRL D", (CPU cpu) -> cpu.SRL(cpu.regs.D), 2, "Z 0 0 C", 8);
        cbOperations[0x3b] = new Operation("SRL E", (CPU cpu) -> cpu.SRL(cpu.regs.E), 2, "Z 0 0 C", 8);
        cbOperations[0x3c] = new Operation("SRL H", (CPU cpu) -> cpu.SRL(cpu.regs.H), 2, "Z 0 0 C", 8);
        cbOperations[0x3d] = new Operation("SRL L", (CPU cpu) -> cpu.SRL(cpu.regs.L), 2, "Z 0 0 C", 8);
        cbOperations[0x3e] = new Operation("SRL (HL)", (CPU cpu) -> cpu.SRL(cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 0 C", 16);
        cbOperations[0x3f] = new Operation("SRL A", (CPU cpu) -> cpu.SRL(cpu.regs.A), 2, "Z 0 0 C", 8);
        cbOperations[0x40] = new Operation("BIT 0,B", (CPU cpu) -> cpu.BIT(0, cpu.regs.B), 2, "Z 0 1 -", 8);
        cbOperations[0x41] = new Operation("BIT 0,C", (CPU cpu) -> cpu.BIT(0, cpu.regs.C), 2, "Z 0 1 -", 8);
        cbOperations[0x42] = new Operation("BIT 0,D", (CPU cpu) -> cpu.BIT(0, cpu.regs.D), 2, "Z 0 1 -", 8);
        cbOperations[0x43] = new Operation("BIT 0,E", (CPU cpu) -> cpu.BIT(0, cpu.regs.E), 2, "Z 0 1 -", 8);
        cbOperations[0x44] = new Operation("BIT 0,H", (CPU cpu) -> cpu.BIT(0, cpu.regs.H), 2, "Z 0 1 -", 8);
        cbOperations[0x45] = new Operation("BIT 0,L", (CPU cpu) -> cpu.BIT(0, cpu.regs.L), 2, "Z 0 1 -", 8);
        cbOperations[0x46] = new Operation("BIT 0,(HL)", (CPU cpu) -> cpu.BIT(0, cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 1 -", 12);
        cbOperations[0x47] = new Operation("BIT 0,A", (CPU cpu) -> cpu.BIT(0, cpu.regs.A), 2, "Z 0 1 -", 8);
        cbOperations[0x48] = new Operation("BIT 1,B", (CPU cpu) -> cpu.BIT(1, cpu.regs.B), 2, "Z 0 1 -", 8);
        cbOperations[0x49] = new Operation("BIT 1,C", (CPU cpu) -> cpu.BIT(1, cpu.regs.C), 2, "Z 0 1 -", 8);
        cbOperations[0x4a] = new Operation("BIT 1,D", (CPU cpu) -> cpu.BIT(1, cpu.regs.D), 2, "Z 0 1 -", 8);
        cbOperations[0x4b] = new Operation("BIT 1,E", (CPU cpu) -> cpu.BIT(1, cpu.regs.E), 2, "Z 0 1 -", 8);
        cbOperations[0x4c] = new Operation("BIT 1,H", (CPU cpu) -> cpu.BIT(1, cpu.regs.H), 2, "Z 0 1 -", 8);
        cbOperations[0x4d] = new Operation("BIT 1,L", (CPU cpu) -> cpu.BIT(1, cpu.regs.L), 2, "Z 0 1 -", 8);
        cbOperations[0x4e] = new Operation("BIT 1,(HL)", (CPU cpu) -> cpu.BIT(1, cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 1 -", 12);
        cbOperations[0x4f] = new Operation("BIT 1,A", (CPU cpu) -> cpu.BIT(1, cpu.regs.A), 2, "Z 0 1 -", 8);
        cbOperations[0x50] = new Operation("BIT 2,B", (CPU cpu) -> cpu.BIT(2, cpu.regs.B), 2, "Z 0 1 -", 8);
        cbOperations[0x51] = new Operation("BIT 2,C", (CPU cpu) -> cpu.BIT(2, cpu.regs.C), 2, "Z 0 1 -", 8);
        cbOperations[0x52] = new Operation("BIT 2,D", (CPU cpu) -> cpu.BIT(2, cpu.regs.D), 2, "Z 0 1 -", 8);
        cbOperations[0x53] = new Operation("BIT 2,E", (CPU cpu) -> cpu.BIT(2, cpu.regs.E), 2, "Z 0 1 -", 8);
        cbOperations[0x54] = new Operation("BIT 2,H", (CPU cpu) -> cpu.BIT(2, cpu.regs.H), 2, "Z 0 1 -", 8);
        cbOperations[0x55] = new Operation("BIT 2,L", (CPU cpu) -> cpu.BIT(2, cpu.regs.L), 2, "Z 0 1 -", 8);
        cbOperations[0x56] = new Operation("BIT 2,(HL)", (CPU cpu) -> cpu.BIT(2, cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 1 -", 12);
        cbOperations[0x57] = new Operation("BIT 2,A", (CPU cpu) -> cpu.BIT(2, cpu.regs.A), 2, "Z 0 1 -", 8);
        cbOperations[0x58] = new Operation("BIT 3,B", (CPU cpu) -> cpu.BIT(3, cpu.regs.B), 2, "Z 0 1 -", 8);
        cbOperations[0x59] = new Operation("BIT 3,C", (CPU cpu) -> cpu.BIT(3, cpu.regs.C), 2, "Z 0 1 -", 8);
        cbOperations[0x5a] = new Operation("BIT 3,D", (CPU cpu) -> cpu.BIT(3, cpu.regs.D), 2, "Z 0 1 -", 8);
        cbOperations[0x5b] = new Operation("BIT 3,E", (CPU cpu) -> cpu.BIT(3, cpu.regs.E), 2, "Z 0 1 -", 8);
        cbOperations[0x5c] = new Operation("BIT 3,H", (CPU cpu) -> cpu.BIT(3, cpu.regs.H), 2, "Z 0 1 -", 8);
        cbOperations[0x5d] = new Operation("BIT 3,L", (CPU cpu) -> cpu.BIT(3, cpu.regs.L), 2, "Z 0 1 -", 8);
        cbOperations[0x5e] = new Operation("BIT 3,(HL)", (CPU cpu) -> cpu.BIT(3, cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 1 -", 12);
        cbOperations[0x5f] = new Operation("BIT 3,A", (CPU cpu) -> cpu.BIT(3, cpu.regs.A), 2, "Z 0 1 -", 8);
        cbOperations[0x60] = new Operation("BIT 4,B", (CPU cpu) -> cpu.BIT(4, cpu.regs.B), 2, "Z 0 1 -", 8);
        cbOperations[0x61] = new Operation("BIT 4,C", (CPU cpu) -> cpu.BIT(4, cpu.regs.C), 2, "Z 0 1 -", 8);
        cbOperations[0x62] = new Operation("BIT 4,D", (CPU cpu) -> cpu.BIT(4, cpu.regs.D), 2, "Z 0 1 -", 8);
        cbOperations[0x63] = new Operation("BIT 4,E", (CPU cpu) -> cpu.BIT(4, cpu.regs.E), 2, "Z 0 1 -", 8);
        cbOperations[0x64] = new Operation("BIT 4,H", (CPU cpu) -> cpu.BIT(4, cpu.regs.H), 2, "Z 0 1 -", 8);
        cbOperations[0x65] = new Operation("BIT 4,L", (CPU cpu) -> cpu.BIT(4, cpu.regs.L), 2, "Z 0 1 -", 8);
        cbOperations[0x66] = new Operation("BIT 4,(HL)", (CPU cpu) -> cpu.BIT(4, cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 1 -", 12);
        cbOperations[0x67] = new Operation("BIT 4,A", (CPU cpu) -> cpu.BIT(4, cpu.regs.A), 2, "Z 0 1 -", 8);
        cbOperations[0x68] = new Operation("BIT 5,B", (CPU cpu) -> cpu.BIT(5, cpu.regs.B), 2, "Z 0 1 -", 8);
        cbOperations[0x69] = new Operation("BIT 5,C", (CPU cpu) -> cpu.BIT(5, cpu.regs.C), 2, "Z 0 1 -", 8);
        cbOperations[0x6a] = new Operation("BIT 5,D", (CPU cpu) -> cpu.BIT(5, cpu.regs.D), 2, "Z 0 1 -", 8);
        cbOperations[0x6b] = new Operation("BIT 5,E", (CPU cpu) -> cpu.BIT(5, cpu.regs.E), 2, "Z 0 1 -", 8);
        cbOperations[0x6c] = new Operation("BIT 5,H", (CPU cpu) -> cpu.BIT(5, cpu.regs.H), 2, "Z 0 1 -", 8);
        cbOperations[0x6d] = new Operation("BIT 5,L", (CPU cpu) -> cpu.BIT(5, cpu.regs.L), 2, "Z 0 1 -", 8);
        cbOperations[0x6e] = new Operation("BIT 5,(HL)", (CPU cpu) -> cpu.BIT(5, cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 1 -", 12);
        cbOperations[0x6f] = new Operation("BIT 5,A", (CPU cpu) -> cpu.BIT(5, cpu.regs.A), 2, "Z 0 1 -", 8);
        cbOperations[0x70] = new Operation("BIT 6,B", (CPU cpu) -> cpu.BIT(6, cpu.regs.B), 2, "Z 0 1 -", 8);
        cbOperations[0x71] = new Operation("BIT 6,C", (CPU cpu) -> cpu.BIT(6, cpu.regs.C), 2, "Z 0 1 -", 8);
        cbOperations[0x72] = new Operation("BIT 6,D", (CPU cpu) -> cpu.BIT(6, cpu.regs.D), 2, "Z 0 1 -", 8);
        cbOperations[0x73] = new Operation("BIT 6,E", (CPU cpu) -> cpu.BIT(6, cpu.regs.E), 2, "Z 0 1 -", 8);
        cbOperations[0x74] = new Operation("BIT 6,H", (CPU cpu) -> cpu.BIT(6, cpu.regs.H), 2, "Z 0 1 -", 8);
        cbOperations[0x75] = new Operation("BIT 6,L", (CPU cpu) -> cpu.BIT(6, cpu.regs.L), 2, "Z 0 1 -", 8);
        cbOperations[0x76] = new Operation("BIT 6,(HL)", (CPU cpu) -> cpu.BIT(6, cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 1 -", 12);
        cbOperations[0x77] = new Operation("BIT 6,A", (CPU cpu) -> cpu.BIT(6, cpu.regs.A), 2, "Z 0 1 -", 8);
        cbOperations[0x78] = new Operation("BIT 7,B", (CPU cpu) -> cpu.BIT(7, cpu.regs.B), 2, "Z 0 1 -", 8);
        cbOperations[0x79] = new Operation("BIT 7,C", (CPU cpu) -> cpu.BIT(7, cpu.regs.C), 2, "Z 0 1 -", 8);
        cbOperations[0x7a] = new Operation("BIT 7,D", (CPU cpu) -> cpu.BIT(7, cpu.regs.D), 2, "Z 0 1 -", 8);
        cbOperations[0x7b] = new Operation("BIT 7,E", (CPU cpu) -> cpu.BIT(7, cpu.regs.E), 2, "Z 0 1 -", 8);
        cbOperations[0x7c] = new Operation("BIT 7,H", (CPU cpu) -> cpu.BIT(7, cpu.regs.H), 2, "Z 0 1 -", 8);
        cbOperations[0x7d] = new Operation("BIT 7,L", (CPU cpu) -> cpu.BIT(7, cpu.regs.L), 2, "Z 0 1 -", 8);
        cbOperations[0x7e] = new Operation("BIT 7,(HL)", (CPU cpu) -> cpu.BIT(7, cpu.mem.registerLocation(cpu.regs.HL)), 2, "Z 0 1 -", 12);
        cbOperations[0x7f] = new Operation("BIT 7,A", (CPU cpu) -> cpu.BIT(7, cpu.regs.A), 2, "Z 0 1 -", 8);
        cbOperations[0x80] = new Operation("RES 0,B", (CPU cpu) -> cpu.RES(0, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0x81] = new Operation("RES 0,C", (CPU cpu) -> cpu.RES(0, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0x82] = new Operation("RES 0,D", (CPU cpu) -> cpu.RES(0, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0x83] = new Operation("RES 0,E", (CPU cpu) -> cpu.RES(0, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0x84] = new Operation("RES 0,H", (CPU cpu) -> cpu.RES(0, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0x85] = new Operation("RES 0,L", (CPU cpu) -> cpu.RES(0, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0x86] = new Operation("RES 0,(HL)", (CPU cpu) -> cpu.RES(0, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0x87] = new Operation("RES 0,A", (CPU cpu) -> cpu.RES(0, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0x88] = new Operation("RES 1,B", (CPU cpu) -> cpu.RES(1, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0x89] = new Operation("RES 1,C", (CPU cpu) -> cpu.RES(1, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0x8a] = new Operation("RES 1,D", (CPU cpu) -> cpu.RES(1, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0x8b] = new Operation("RES 1,E", (CPU cpu) -> cpu.RES(1, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0x8c] = new Operation("RES 1,H", (CPU cpu) -> cpu.RES(1, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0x8d] = new Operation("RES 1,L", (CPU cpu) -> cpu.RES(1, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0x8e] = new Operation("RES 1,(HL)", (CPU cpu) -> cpu.RES(1, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0x8f] = new Operation("RES 1,A", (CPU cpu) -> cpu.RES(1, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0x90] = new Operation("RES 2,B", (CPU cpu) -> cpu.RES(2, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0x91] = new Operation("RES 2,C", (CPU cpu) -> cpu.RES(2, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0x92] = new Operation("RES 2,D", (CPU cpu) -> cpu.RES(2, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0x93] = new Operation("RES 2,E", (CPU cpu) -> cpu.RES(2, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0x94] = new Operation("RES 2,H", (CPU cpu) -> cpu.RES(2, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0x95] = new Operation("RES 2,L", (CPU cpu) -> cpu.RES(2, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0x96] = new Operation("RES 2,(HL)", (CPU cpu) -> cpu.RES(2, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0x97] = new Operation("RES 2,A", (CPU cpu) -> cpu.RES(2, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0x98] = new Operation("RES 3,B", (CPU cpu) -> cpu.RES(3, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0x99] = new Operation("RES 3,C", (CPU cpu) -> cpu.RES(3, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0x9a] = new Operation("RES 3,D", (CPU cpu) -> cpu.RES(3, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0x9b] = new Operation("RES 3,E", (CPU cpu) -> cpu.RES(3, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0x9c] = new Operation("RES 3,H", (CPU cpu) -> cpu.RES(3, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0x9d] = new Operation("RES 3,L", (CPU cpu) -> cpu.RES(3, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0x9e] = new Operation("RES 3,(HL)", (CPU cpu) -> cpu.RES(3, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0x9f] = new Operation("RES 3,A", (CPU cpu) -> cpu.RES(3, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xa0] = new Operation("RES 4,B", (CPU cpu) -> cpu.RES(4, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xa1] = new Operation("RES 4,C", (CPU cpu) -> cpu.RES(4, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xa2] = new Operation("RES 4,D", (CPU cpu) -> cpu.RES(4, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xa3] = new Operation("RES 4,E", (CPU cpu) -> cpu.RES(4, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xa4] = new Operation("RES 4,H", (CPU cpu) -> cpu.RES(4, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xa5] = new Operation("RES 4,L", (CPU cpu) -> cpu.RES(4, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xa6] = new Operation("RES 4,(HL)", (CPU cpu) -> cpu.RES(4, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xa7] = new Operation("RES 4,A", (CPU cpu) -> cpu.RES(4, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xa8] = new Operation("RES 5,B", (CPU cpu) -> cpu.RES(5, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xa9] = new Operation("RES 5,C", (CPU cpu) -> cpu.RES(5, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xaa] = new Operation("RES 5,D", (CPU cpu) -> cpu.RES(5, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xab] = new Operation("RES 5,E", (CPU cpu) -> cpu.RES(5, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xac] = new Operation("RES 5,H", (CPU cpu) -> cpu.RES(5, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xad] = new Operation("RES 5,L", (CPU cpu) -> cpu.RES(5, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xae] = new Operation("RES 5,(HL)", (CPU cpu) -> cpu.RES(5, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xaf] = new Operation("RES 5,A", (CPU cpu) -> cpu.RES(5, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xb0] = new Operation("RES 6,B", (CPU cpu) -> cpu.RES(6, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xb1] = new Operation("RES 6,C", (CPU cpu) -> cpu.RES(6, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xb2] = new Operation("RES 6,D", (CPU cpu) -> cpu.RES(6, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xb3] = new Operation("RES 6,E", (CPU cpu) -> cpu.RES(6, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xb4] = new Operation("RES 6,H", (CPU cpu) -> cpu.RES(6, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xb5] = new Operation("RES 6,L", (CPU cpu) -> cpu.RES(6, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xb6] = new Operation("RES 6,(HL)", (CPU cpu) -> cpu.RES(6, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xb7] = new Operation("RES 6,A", (CPU cpu) -> cpu.RES(6, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xb8] = new Operation("RES 7,B", (CPU cpu) -> cpu.RES(7, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xb9] = new Operation("RES 7,C", (CPU cpu) -> cpu.RES(7, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xba] = new Operation("RES 7,D", (CPU cpu) -> cpu.RES(7, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xbb] = new Operation("RES 7,E", (CPU cpu) -> cpu.RES(7, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xbc] = new Operation("RES 7,H", (CPU cpu) -> cpu.RES(7, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xbd] = new Operation("RES 7,L", (CPU cpu) -> cpu.RES(7, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xbe] = new Operation("RES 7,(HL)", (CPU cpu) -> cpu.RES(7, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xbf] = new Operation("RES 7,A", (CPU cpu) -> cpu.RES(7, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xc0] = new Operation("SET 0,B", (CPU cpu) -> cpu.SET(0, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xc1] = new Operation("SET 0,C", (CPU cpu) -> cpu.SET(0, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xc2] = new Operation("SET 0,D", (CPU cpu) -> cpu.SET(0, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xc3] = new Operation("SET 0,E", (CPU cpu) -> cpu.SET(0, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xc4] = new Operation("SET 0,H", (CPU cpu) -> cpu.SET(0, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xc5] = new Operation("SET 0,L", (CPU cpu) -> cpu.SET(0, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xc6] = new Operation("SET 0,(HL)", (CPU cpu) -> cpu.SET(0, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xc7] = new Operation("SET 0,A", (CPU cpu) -> cpu.SET(0, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xc8] = new Operation("SET 1,B", (CPU cpu) -> cpu.SET(1, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xc9] = new Operation("SET 1,C", (CPU cpu) -> cpu.SET(1, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xca] = new Operation("SET 1,D", (CPU cpu) -> cpu.SET(1, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xcb] = new Operation("SET 1,E", (CPU cpu) -> cpu.SET(1, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xcc] = new Operation("SET 1,H", (CPU cpu) -> cpu.SET(1, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xcd] = new Operation("SET 1,L", (CPU cpu) -> cpu.SET(1, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xce] = new Operation("SET 1,(HL)", (CPU cpu) -> cpu.SET(1, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xcf] = new Operation("SET 1,A", (CPU cpu) -> cpu.SET(1, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xd0] = new Operation("SET 2,B", (CPU cpu) -> cpu.SET(2, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xd1] = new Operation("SET 2,C", (CPU cpu) -> cpu.SET(2, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xd2] = new Operation("SET 2,D", (CPU cpu) -> cpu.SET(2, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xd3] = new Operation("SET 2,E", (CPU cpu) -> cpu.SET(2, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xd4] = new Operation("SET 2,H", (CPU cpu) -> cpu.SET(2, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xd5] = new Operation("SET 2,L", (CPU cpu) -> cpu.SET(2, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xd6] = new Operation("SET 2,(HL)", (CPU cpu) -> cpu.SET(2, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xd7] = new Operation("SET 2,A", (CPU cpu) -> cpu.SET(2, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xd8] = new Operation("SET 3,B", (CPU cpu) -> cpu.SET(3, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xd9] = new Operation("SET 3,C", (CPU cpu) -> cpu.SET(3, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xda] = new Operation("SET 3,D", (CPU cpu) -> cpu.SET(3, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xdb] = new Operation("SET 3,E", (CPU cpu) -> cpu.SET(3, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xdc] = new Operation("SET 3,H", (CPU cpu) -> cpu.SET(3, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xdd] = new Operation("SET 3,L", (CPU cpu) -> cpu.SET(3, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xde] = new Operation("SET 3,(HL)", (CPU cpu) -> cpu.SET(3, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xdf] = new Operation("SET 3,A", (CPU cpu) -> cpu.SET(3, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xe0] = new Operation("SET 4,B", (CPU cpu) -> cpu.SET(4, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xe1] = new Operation("SET 4,C", (CPU cpu) -> cpu.SET(4, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xe2] = new Operation("SET 4,D", (CPU cpu) -> cpu.SET(4, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xe3] = new Operation("SET 4,E", (CPU cpu) -> cpu.SET(4, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xe4] = new Operation("SET 4,H", (CPU cpu) -> cpu.SET(4, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xe5] = new Operation("SET 4,L", (CPU cpu) -> cpu.SET(4, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xe6] = new Operation("SET 4,(HL)", (CPU cpu) -> cpu.SET(4, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xe7] = new Operation("SET 4,A", (CPU cpu) -> cpu.SET(4, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xe8] = new Operation("SET 5,B", (CPU cpu) -> cpu.SET(5, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xe9] = new Operation("SET 5,C", (CPU cpu) -> cpu.SET(5, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xea] = new Operation("SET 5,D", (CPU cpu) -> cpu.SET(5, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xeb] = new Operation("SET 5,E", (CPU cpu) -> cpu.SET(5, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xec] = new Operation("SET 5,H", (CPU cpu) -> cpu.SET(5, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xed] = new Operation("SET 5,L", (CPU cpu) -> cpu.SET(5, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xee] = new Operation("SET 5,(HL)", (CPU cpu) -> cpu.SET(5, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xef] = new Operation("SET 5,A", (CPU cpu) -> cpu.SET(5, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xf0] = new Operation("SET 6,B", (CPU cpu) -> cpu.SET(6, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xf1] = new Operation("SET 6,C", (CPU cpu) -> cpu.SET(6, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xf2] = new Operation("SET 6,D", (CPU cpu) -> cpu.SET(6, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xf3] = new Operation("SET 6,E", (CPU cpu) -> cpu.SET(6, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xf4] = new Operation("SET 6,H", (CPU cpu) -> cpu.SET(6, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xf5] = new Operation("SET 6,L", (CPU cpu) -> cpu.SET(6, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xf6] = new Operation("SET 6,(HL)", (CPU cpu) -> cpu.SET(6, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xf7] = new Operation("SET 6,A", (CPU cpu) -> cpu.SET(6, cpu.regs.A), 2, "- - - -", 8);
        cbOperations[0xf8] = new Operation("SET 7,B", (CPU cpu) -> cpu.SET(7, cpu.regs.B), 2, "- - - -", 8);
        cbOperations[0xf9] = new Operation("SET 7,C", (CPU cpu) -> cpu.SET(7, cpu.regs.C), 2, "- - - -", 8);
        cbOperations[0xfa] = new Operation("SET 7,D", (CPU cpu) -> cpu.SET(7, cpu.regs.D), 2, "- - - -", 8);
        cbOperations[0xfb] = new Operation("SET 7,E", (CPU cpu) -> cpu.SET(7, cpu.regs.E), 2, "- - - -", 8);
        cbOperations[0xfc] = new Operation("SET 7,H", (CPU cpu) -> cpu.SET(7, cpu.regs.H), 2, "- - - -", 8);
        cbOperations[0xfd] = new Operation("SET 7,L", (CPU cpu) -> cpu.SET(7, cpu.regs.L), 2, "- - - -", 8);
        cbOperations[0xfe] = new Operation("SET 7,(HL)", (CPU cpu) -> cpu.SET(7, cpu.mem.registerLocation(cpu.regs.HL)), 2, "- - - -", 16);
        cbOperations[0xff] = new Operation("SET 7,A", (CPU cpu) -> cpu.SET(7, cpu.regs.A), 2, "- - - -", 8);


    }
}
