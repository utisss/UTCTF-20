#include <stdio.h>
#include <cstring>

#include "utils.h"
#include "app.h"
#include "data.h"
#include "enclave.h"

void info_print(const char* str) {
    printf("[INFO] %s\n", str);
}

void warning_print(const char* str) {
    printf("[WARNING] %s\n", str);
}

void error_print(const char* str) {
    printf("[ERROR] %s\n", str);
}

int is_error(int error_code) {
    char err_message[100];

    // check error case
    switch(error_code) {
        case RET_SUCCESS:
            return 0;

        case ERR_DATA_ALREADY_EXISTS:
            sprintf(err_message, "data already exists: delete file '%s' first.", DATA_FILE);
            break;

        case ERR_CANNOT_SAVE_DATA:
            strcpy(err_message, "Coud not save data.");
            break;

        case ERR_CANNOT_LOAD_DATA:
            strcpy(err_message, "Coud not load data.");
            break;

        case ERR_WRONG_MASTER_PASSWORD:
            strcpy(err_message, "Wrong master password."); 
            break;

        case ERR_ITEM_DOES_NOT_EXIST: 
            strcpy(err_message, "Item does not exist."); 
            break;
        case ERR_FAIL_SEAL:
            sprintf(err_message, "Fail to seal data."); 
            break;

        case ERR_FAIL_UNSEAL:
            sprintf(err_message, "Fail to unseal data."); 
            break;

        default:
            sprintf(err_message, "Unknown error."); 
    }

    // print error message
    error_print(err_message);
    return 1;
}




