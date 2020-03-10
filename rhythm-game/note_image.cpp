#include "note_image.h"

void NoteImage::render() {
    double trueFactor = explosionFactor; //apply some easing function
    int width = WIDTH * (1 + trueFactor);
    int height = HEIGHT * (1 + trueFactor);

    SDL_Rect rect = {x - width / 2, y - height / 2, width, height};
    
    int rgb = arrowImage.rgb;
    SDL_SetTextureColorMod(arrowImage.image, (rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);

    SDL_SetTextureAlphaMod(arrowImage.image, 255 - 255 * trueFactor);

    SDL_RenderCopyEx(renderer, arrowImage.image, NULL, &rect, arrowImage.rotation, NULL, SDL_FLIP_NONE);
}