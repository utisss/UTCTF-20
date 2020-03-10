#pragma once
#include <stdio.h>
#include <vector>
#include "note.h"
#include "lane.h"
#include "secret.h"
#include "const.h"
#include "files.h"

struct BeatMapNote {
    double time;
    int laneNo;
} __attribute__((packed));

void writeBeatMap(const char *path, std::vector<std::vector<Note *>> lanes) {
    FILE *fp;
    
    long totalNotes = 0;
    for(auto lane : lanes){
        totalNotes += lane.size();
    }

    BeatMapNote *allNotes = new BeatMapNote[totalNotes];
    long currentNoteIndex = 0;
    
    for(int i = 0; i < lanes.size(); i++) {
        for(Note *note : lanes[i]) {
            allNotes[currentNoteIndex++] = {note->time, i};
        }
    }

    fp = fopen(path, "w");
    if(fp == NULL) {
        printf("coult not open beatmap file for write: %s\n", path);
        exit(1);
    }
    fwrite(allNotes, sizeof(BeatMapNote), totalNotes, fp);
    fclose(fp);
}

#ifdef PRINT_HASH
struct Hashable{
    double time = 0;
    uint32_t verdict = BAD;
    uint32_t salt = 1111880203;
};
#endif

void readBeatMap(const char *path, std::vector<Lane *> &lanes) {
#ifdef PRINT_HASH
    Hashy::hash256_one_by_one hashers[4];
#endif

    FILE *fp;

    int size = getFileSize(path);
    if(size < 0) {
        printf("could not determine size of file: %s\n", path);
        exit(1);
    }
    
    fp = fopen(path, "rb");
    if(fp == NULL) {
        printf("could not open beatmap file for read: %s\n", path);
        exit(1);
    }

    long totalNotes = size / sizeof(BeatMapNote);

    BeatMapNote *allNotes = new BeatMapNote[totalNotes];

    int items = fread(allNotes, sizeof(BeatMapNote), totalNotes, fp);

    for(int i = 0 ; i < totalNotes; i++){
        BeatMapNote note = allNotes[i];
        lanes.at(note.laneNo)->futureNotes.push_back(new Note(note.time));

#ifdef PRINT_HASH
        Hashable info;
        info.time = note.time;
        info.verdict = PERFECT;

        hashers[note.laneNo].process((uint8_t *)&info, (uint8_t *)(&info + 1));
#endif
    }

#ifdef PRINT_HASH
    uint8_t temp[32];
    uint8_t all[32] = {0};
    for(int i = 0; i < 4; i++){
        hashers[i].finish();
        hashers[i].get_hash_bytes(temp, temp + 32);
        for(int i = 0; i < 32; i++){
            all[i] ^= temp[i];
        }
    }

    for(int i = 0; i < 32; i++){
        printf("%02x", all[i]);
    }

    printf("\n");

#ifdef GEN_SECRET

    createSecret(secret_path, "utflag{w0ah_d00d_u_got_some_mad_skillz_J4ebBxZpAS}", all);

#endif
#endif

    fclose(fp);
}