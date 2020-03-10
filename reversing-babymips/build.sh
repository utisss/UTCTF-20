!#/bin/bash
mips-linux-gnu-g++ -s main.cpp -o baby
/usr/mips-linux-gnu/bin/strip -s --remove-section .note.gnu.build-id baby

