#include <stdio.h>
#include <string.h>

void main() {
	welcome();
	while(1) {
		do_move();
	}	
}

void welcome() {
	printf("Welcome to the game of Zurk!\n");
	printf("You awaken in a cave dimly lit by torches.\n");
	printf("The cave two tunnels, one going east and the other going west.\n");
}

void do_move() {
	printf("What would you like to do?\n");
	char move[50];
	fgets(move, 50, stdin);
	move[strcspn(move, "\n")] = 0;
	if(strcmp(move, "go west") == 0) {
		printf("You move west to arrive in a cave dimly lit by torches.\n");
		printf("The cave two tunnels, one going east and the other going west.\n");
	}
	else if(strcmp(move, "go east") == 0) {
		printf("You move east to arrive in a cave dimly lit by torches.\n");
		printf("The cave two tunnels, one going east and the other going west.\n");
	}
	else {
		printf(move);
		printf(" is not a valid instruction.\n");
	}
}
