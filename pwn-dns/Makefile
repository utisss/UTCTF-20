CC=gcc

all:
	gcc -fno-stack-protector -fno-pie -no-pie -z execstack -o prog.o src/pwn.c

.PHONY: clean
clean:
	-rm *.o
