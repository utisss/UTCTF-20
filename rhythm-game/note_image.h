#pragma once
#include <SDL2/SDL.h>
#include <stdio.h>
#include "drawable.h"
#include "note.h"

struct ArrowImage {
    SDL_Texture *image;
    int rotation;
    int rgb;
};

class NoteImage : public Drawable{
public:
    static const int WIDTH = 100;
    static const int HEIGHT = 100;

    ArrowImage arrowImage;
    Note *note;
    int x;
    int y;
    double explosionFactor = 0;
    double explosionStart = 0;
    NoteImage(SDL_Renderer *r, int x, int y, int rgb, Note *note, ArrowImage arrowImage) : 
        Drawable(r), x(x), y(y), note(note), arrowImage(arrowImage) {
        
    };

    virtual void render();
};