# Wasm Fans Only
* **Event:** UTCTF 2020
* **Problem: Type:** Web/RevEng
* **Difficulty:** Medium/Hard

## Background
WebAssembly is a new bytecode designed around a stack machine architecture
designed to be ran within a virtual machine in a browser. Since the 
bytecode is relatively new, reverse engineering tools are a little sparse.
However, due to wasm being a stack machine architecture (no registers) and
having fairly high-level control flow mechanisms, it is relatively easy to
read in disassembly format, also known as wat. 

#### Solution
The webassembly file gets the inputted password and generates the key 
based on some strings it obtains from eval()ing some Javascript over the 
bridge. The intended solution was to reverse engineer the wasm by 
conversion to wat format and comparing to the symbol table. Some function
names like verify_flag and aes_encrypt_block were intentionally left in
the symbol table. What's unfortunate is the lack of decent tooling for 
wasm at the moment; if this challenge were x86 it would be trivial. 

Here is the a nice writeup of the reverse engineering process: 
https://ctftime.org/writeup/18640