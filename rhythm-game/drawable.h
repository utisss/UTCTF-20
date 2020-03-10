#pragma once
#include <SDL2/SDL.h>

class Drawable {
    Drawable() {};
public:
    SDL_Renderer *renderer;
    Drawable(SDL_Renderer *r) : renderer(r) {};

    virtual void render();
};