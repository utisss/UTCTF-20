# UTCTF Rhythm Game
## Dependencies
SDL, SDL Mixer, SDL TTF

## Playing the default map
For Windows, replace ./play with play.exe.
```
./play
```

## Playing alternative maps
```
./play skystrike
```

## Map format
Maps are made up of three files. An .ogg file for audio, a .map file for the 
notes, and a .secret file containing an encrypted secret. The secret is 
automatically printed out when the player earns a perfect score on the map. All 
map files are contained within the assets/ folder.

## CTF Solution
There are two intended solutions: You can either patch the binary or reverse 
engineer it. 

Patching the binary seems simpler: You focus on the Lane::hit function, which
contains some code that handles note verdicts (perfect, amazing, great, bad),
then you can change the behavior to return immediately if there was no note in 
the lane's queue, then if there was a note in the queue act as if the verdict
was "perfect" automatically. Then, you can either use an auto keypress program 
to spam keypresses or also patch the binary to call Lane::hit on every lane on 
every frame (this is harder).

Reverse engineering the binary is also designed to be doable, since all of 
the symbols are left unstripped. The code is provided here as a cheatsheet if
you are attempting to reverse engineer the binary. 

A third, unintended, solution was to write a script to play the game 
automatically. This was unintended because I thought that the game was resource
intensive and strict enough with timing that writing a script was not feasible 
for the entire 4-minute-long song. I also thought that the visual effects would 
make it harder to write a script, but apparently this was possible. 

## Included songs
Clutterfunk - Waterflame https://www.newgrounds.com/audio/listen/505816  
Commons deed https://creativecommons.org/licenses/by-nc-sa/3.0/

Skystrike - Hinkik https://creativecommons.org/licenses/by-sa/3.0/  
Commons deed https://creativecommons.org/licenses/by-sa/3.0/