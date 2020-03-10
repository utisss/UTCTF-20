#pragma once
#include <SDL2/SDL.h>
#include <vector>
#include "drawable.h"

class Scene: public std::vector<Drawable *>, public Drawable {
public:
    Scene(SDL_Renderer *r) : Drawable(r) {}

    virtual void render();
};