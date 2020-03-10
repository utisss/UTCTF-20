#include <sys/time.h>
#include <SDL2/SDL.h>
#include <cstdio>

#define MS_PER_SEC 1000000

class Timer {
    Uint64 mseconds = 0;
    Uint64 last_recorded_time;
public:
    void start() {
        mseconds = 0;
        
        struct timeval new_time;
        gettimeofday(&new_time, NULL);
        Uint64 new_time_ms = new_time.tv_sec*(uint64_t)MS_PER_SEC+new_time.tv_usec;
        last_recorded_time = new_time_ms;
    }

    void update() {
        struct timeval new_time;
        gettimeofday(&new_time, NULL);
        Uint64 new_time_ms = new_time.tv_sec*(uint64_t)MS_PER_SEC+new_time.tv_usec;
        auto span = new_time_ms - last_recorded_time;

        mseconds += span;
        last_recorded_time = new_time_ms;
    }

    void sync(double newTime) {
        mseconds = newTime * MS_PER_SEC;
    }

    double getTime(){
        update();
        return (double)mseconds / MS_PER_SEC;
    }
};