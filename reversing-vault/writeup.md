1) The intended solution is to use static analysis to understand the binary.
2) After opening the binary in your favorite disassembler, you start at the entry point and find that the main loop does nothing.
3) Then you realize that this is an embedded system, so there may be some sort of timer/polling system in place.
4) This would require some sort of hardware init routine which can be found at 0x04c4
5) From here, you can find a function call to a init routine that includes an object and initializes a timer in interrupt mode.
6) If you keep digging through the binary, you find a function at 0x0556 that references the same object. 
7) Looking at a few data sheets, it become trivial to figure out that this function is reading from GPIO Ports E0-3.
8) At the end of the function, another function is called that clearly checks all of the previous button presses against some sort of list. 
9) With a little more digging, it's possible to find the function responsible for initializing the list that is being read from.
10) At that point, the challenge is solved and the flag can be assembled from the list of numbers that are inside the aforementioned function.
