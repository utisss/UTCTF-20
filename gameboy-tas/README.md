# Game Boy TAS
* **Event:** UTCTF 2020
* **Problem: Type:** Binary Exploitation
* **Difficulty:** Medium/Hard

## Background
Unlike newer CPUs, the Game Boy's LR35902 processor follows a strict timing 
specification. Each operation would take exactly 4n cycles, where n typically 
means the number of memory operations required to execute the instruction (the
Game Boy's speed was limited by the memory speed). Knowing the ISA of the
LR35902 is necessary to complete this challenge.

#### Solution
Reading the ROM disassembly, we find that the ROM is essentially a large loop. 
There is some initialization code at 00h, followed by a joypad handler at 60h,
some conditional branching at 150h, and a jump to HRAM at around 2000h. The
initialization code adds some code to HRAM past the stack pointer so that 
control flow returns to 150h. 

What's really weird is the joypad interrupt handler. Instead of ending in an
IRET like a normal program, it ends in a jump to 100h, which is the start of
program code. This means that the return address which was pushed when the 
interrupt was issued is actually never popped, allowing the attacker to write
an arbitrary amount of data to HRAM, which is later executed. 

The difficult part then, is writing some shellcode that will output the flag
from ROM but is made entirely up of 2-byte sequences, each of which is within 
the range of normal program execution. Then we can either modify the given
emulator or calculate manually what amount of time we should wait so that the
desired PC is pushed onto the stack. A working shellcode is provided within 
soln.txt. 