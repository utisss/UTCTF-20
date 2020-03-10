#ifndef UTIL_H_
#define UTIL_H_

#include "data.h"

void info_print(const char* str);

void warning_print(const char* str);

void error_print(const char* str);

void print_data(const data_t* data);

int is_error(int error_code);

void show_help();

void show_version();


#endif // UTIL_H_
