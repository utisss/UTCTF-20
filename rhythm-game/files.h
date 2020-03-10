#pragma once
#include <cstdio>
#include <sys/stat.h>

int getFileSize(const char *filename) {
    struct stat stat_buf;
    int rc = stat(filename, &stat_buf);
    return rc == 0? stat_buf.st_size : -1;
}