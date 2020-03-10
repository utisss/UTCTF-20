#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <emscripten.h>
#include <stdint.h>
#include "aes.h"

EM_JS(char *, getString, (const char* str), {
    var retVal = eval(UTF8ToString(str));

    var length = lengthBytesUTF8(retVal)+1;
    var result = _malloc(length);
    stringToUTF8(retVal, result, length);

    return result;
})

EM_JS(void, win, (const char* str), {
    var flag = UTF8ToString(str);
    
    document.write("<center>Congratulations! The flag is: " + flag + "</center>");
})

EM_JS(void, lose, (), {
    document.write("<center>Try again!</center>");
})

int checkTrivial(char *flag) {
    if(strlen(flag) != 24){
        return 0;
    }

    char header[] = "utflag{";
    for(int i = 0; i < 7; i++){
        if(flag[i] != header[i]) {
            return 0;
        }
    }

    if(flag[23] != '}'){
        return 0;
    }

    return 1;
}

EMSCRIPTEN_KEEPALIVE
void verify_flag() {
    char formValue[] = {83, 88, 84, 66, 90, 82, 89, 67, 25, 80, 82, 67, 114, 91, 82, 90, 82, 89, 67, 117, 78, 126, 83, 31, 21, 81, 91, 86, 80, 21, 30, 25, 65, 86, 91, 66, 82, 55};
    for(int i = 0; i < 38; i++){
        formValue[i] ^= 55;
    }

    char *candidate = getString(formValue);

    char windowLocationHostname[] = {23, 9, 14, 4, 15, 23, 78, 12, 15, 3, 1, 20, 9, 15, 14, 78, 8, 15, 19, 20, 14, 1, 13, 5, 96};
    for(int i = 0; i < 25; i++){
        windowLocationHostname[i] ^= 96;
    }

    char *url = getString(windowLocationHostname);
    
    uint8_t key[16] = {25};

    for(int i = 0; i < strlen(url); i++){
        key[i % 16] ^= url[i];
    }

    uint8_t encryptedFlag[] = {0x0f, 0xae, 0xf8, 0x59, 0x84, 0xb1, 0x28, 0x67, 0x28, 0x18, 0x88, 0x17, 0x64, 0xd3, 0x25, 0x2a};

    if(checkTrivial(candidate)){
        uint8_t encrypted[16] = {0};
        aes_encrypt_block((uint8_t*)candidate + 7, key, encrypted);

        for(int i = 0; i < 16; i++){
            if(encryptedFlag[i] != encrypted[i]) {
                lose();
                return;
            }
        }

        win(candidate);
        return;
    }

    lose();
}