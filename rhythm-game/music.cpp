#include "music.h"

void mix_handler(void *udata, Uint8 *, int len) {
    Music *music = (Music *)udata;
    
    double timerTime = music->timer.getTime();
    double musicTime = (double)music->samples / music->frequency;
    
    music->timer.sync((timerTime + musicTime) / 2);

    music->samples += len;
}

Music::Music(const char *path)  {
    music = Mix_LoadMUS(path);
    if(!music) {
        printf("loading music: %s\n", Mix_GetError());
    }

    Mix_SetPostMix(mix_handler, this);
    Mix_Volume(-1, 16);
}
