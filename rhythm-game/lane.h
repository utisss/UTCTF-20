#pragma once
#include "drawable.h"
#include "note.h"
#include "note_image.h"
#include "const.h"
#include "music.h"
#include "score.h"
#include "picosha2.h"
#include <SDL2/SDL.h>
#include <deque>

class Lane : public Drawable {
    static const int SPEED = 1000; //pixels per second
    static constexpr double EXPLOSION_SPEED = 4; // 1/second
    static const int BOTTOM_PADDING = 50; //target's offset from bottom of screen

    int x = 0;
    Music *toTrack = NULL;
    ArrowImage arrowImage = {NULL, 0};
    Score *score = NULL;
    std::deque<NoteImage *> viewableNotes; //sorted
    std::deque<NoteImage *> explodingNotes;

    Hashy::hash256_one_by_one hasher;

    inline void handleVerdict(int);

public:
    int count = 0;
    std::deque<Note *> futureNotes; //sorted
    bool finished = false;

    Lane(SDL_Renderer *r, Music *music, ArrowImage arrowImage, int x, Score *s) : 
        Drawable(r), toTrack(music), arrowImage(arrowImage), x(x), score(s) { };

    virtual void render();

    static int calculateNoteY(double noteTime, double now);

    void updateViewable();

    void getHash(uint8_t dest[32]);

    void hit();
};