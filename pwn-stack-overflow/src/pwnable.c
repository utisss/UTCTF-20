#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

void get_flag();

int main() {
  printf("I really like strings! Please give me a good one!\n");
  char buffer_123[100];
  gets(buffer_123);
  printf("Thanks for the string\n");
  return 1;
}

void get_flag(int x) {
  if(x != 0xdeadbeef) {
    return;
  }
  char*  args[2] = {"/bin/sh", NULL};
  execve(args[0], args, NULL);
}
