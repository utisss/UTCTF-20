#pragma once

//in-memory representation of a note (may not be visible)
class Note {
public:
    double time = -1;
    Note(double time): time(time) {}
};