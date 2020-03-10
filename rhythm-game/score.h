#pragma once
#include "drawable.h"
#include <SDL2/SDL_ttf.h>
#include <SDL2/SDL.h>

#define PERFECT 0
#define AMAZING 1
#define GREAT 2
#define BAD 3

class Score : public Drawable {
public:
    long pointVal = 0;
    int counts[4] = {0};
    int combo = 0;
    int maxCombo = 0;
    int previousHit = -1;

    TTF_Font *font;
    Score(SDL_Renderer *r, TTF_Font *f): Drawable(r), font(f) {};

    void render();
    void drawText(char *text, int x, int y, double scale = 1.0);

    const char *describeHit(int type){
        switch(type){
            case PERFECT: return "PERFECT!";
            case AMAZING: return "Amazing!";
            case GREAT: return "Great!";
            case BAD: return "Bad.";
            default: return "";
        }
    }
};