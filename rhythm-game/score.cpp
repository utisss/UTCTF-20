#include "score.h"
#include "const.h"
#include <SDL2/SDL_ttf.h>
#include <cstdio>

void Score::drawText(char *text, int x, int y, double scale) {
    SDL_Rect rect = {x, y, 0, 0};
    TTF_SizeText(font, text, &rect.w, &rect.h);

    rect.w *= scale;
    rect.h *= scale;

    SDL_Color color = {0, 0, 0};
    SDL_Surface *surface = TTF_RenderText_Solid(font, text, color);
    SDL_Texture *texture = SDL_CreateTextureFromSurface(renderer, surface);

    SDL_RenderCopy(renderer, texture, NULL, &rect);

    SDL_DestroyTexture(texture);
    SDL_FreeSurface(surface);
}

void Score::render(){
    char *description = (char *)describeHit(previousHit);
    drawText(description, SCREEN_WIDTH * 5 / 8, SCREEN_HEIGHT / 2 - 150, 2);

    char temp[50];
    snprintf(temp, 50, "Score:     %ld", pointVal);
    drawText(temp, SCREEN_WIDTH * 5 / 8, SCREEN_HEIGHT / 2 - 90);

    snprintf(temp, 50, "Combo:     %d", combo);
    drawText(temp, SCREEN_WIDTH * 5 / 8, SCREEN_HEIGHT / 2 - 60);

    snprintf(temp, 50, "Max Combo: %d", maxCombo);
    drawText(temp, SCREEN_WIDTH * 5 / 8, SCREEN_HEIGHT / 2 - 30);

    snprintf(temp, 50, "Perfect: %d", counts[PERFECT]);
    drawText(temp, SCREEN_WIDTH * 5 / 8, SCREEN_HEIGHT / 2 + 30);
    snprintf(temp, 50, "Amazing: %d", counts[AMAZING]);
    drawText(temp, SCREEN_WIDTH * 5 / 8, SCREEN_HEIGHT / 2 + 60);
    snprintf(temp, 50, "Great:   %d", counts[GREAT]);
    drawText(temp, SCREEN_WIDTH * 5 / 8, SCREEN_HEIGHT / 2 + 90);
    snprintf(temp, 50, "Bad:     %d", counts[BAD]);
    drawText(temp, SCREEN_WIDTH * 5 / 8, SCREEN_HEIGHT / 2 + 120);
}