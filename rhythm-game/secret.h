#pragma once
#include <stdio.h>
#include <SDL2/SDL.h>
#include <cstring>
#include "lane.h"
#include "files.h"
#include "aes.h"

void printHash(const uint8_t *hash) {
    for(int i = 0; i < 32; i++){
        printf("%02x", hash[i]);
    }
    printf("\n");
}

bool showSecret(SDL_Window *gWindow, std::vector<Lane *> lanes, const char *path) {
    FILE *fp;

    int size = getFileSize(path);
    if(size < 0) {
        printf("could not determine size of secret file: %s\n", path);
        return false;
    }
    
    fp = fopen(path, "rb");
    if(fp == NULL) {
        printf("could not open secret file for read: %s\n", path);
        return false;
    }
    int numBlocks = size / 16;

    uint8_t *secret = new uint8_t[16 * numBlocks];
    uint8_t *ptext = new uint8_t[16 * numBlocks];
    fread(secret, 16, numBlocks, fp);
    fclose(fp);

    uint8_t temp[32];
    uint8_t all[32] = {0};
    
    for(auto lane : lanes) {
        lane->getHash(temp);
        //printHash(temp);

        for(int i = 0; i < 32; i++){
            all[i] ^= temp[i];
        }
    }
    //printHash(all);

    decrypt_stream(secret, all, all + 16, numBlocks, ptext);

    char *message = new char[16 * numBlocks + 50];
    snprintf(message, 16 * numBlocks + 50, "Congratulations! The secret message is: %s", ptext);

    SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_INFORMATION, "Done", message, gWindow);

    delete[] secret;
    delete[] ptext;
    delete[] message;

    return true;
}

void createSecret(const char *path, const char *secret, uint8_t hash[32]){
    int ptextLen = strlen(secret);
    int numBlocks = (ptextLen + 15) / 16;

    uint8_t *ptext = new uint8_t[numBlocks * 16]();
    for(int i = 0; i < ptextLen; i++){
        ptext[i] = secret[i];
    }

    uint8_t *encrypted = new uint8_t[numBlocks * 16];

    encrypt_stream(ptext, hash, hash + 16, numBlocks, encrypted);

    FILE *fp = fopen(path, "w");
    fwrite(encrypted, 16, numBlocks, fp);
    fclose(fp);

    delete[] encrypted;
    delete[] ptext;
}