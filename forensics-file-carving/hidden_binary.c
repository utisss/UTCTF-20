#include <stdio.h>

int main() {
    char greet[] = "Ah, you found me!";
	char string[41] = {117, 116, 102, 108, 97, 103, 123, 50, 102, 98, 101, 57, 
        97, 100, 99, 50, 97, 100, 56, 57, 99, 55, 49, 100, 97, 52, 56, 99, 97, 
        98, 101, 57, 48, 97, 49, 50, 49, 99, 48, 125};
	printf("%s\n", greet);
	printf("%s\n", string);
    return 0;
}
