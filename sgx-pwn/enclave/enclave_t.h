#ifndef ENCLAVE_T_H__
#define ENCLAVE_T_H__

#include <stdint.h>
#include <wchar.h>
#include <stddef.h>
#include "sgx_edger8r.h" /* for sgx_ocall etc. */

#include "data.h"

#include <stdlib.h> /* for size_t */

#define SGX_CAST(type, item) ((type)(item))

#ifdef __cplusplus
extern "C" {
#endif

int ecall_save_data(const char* flag, const char* password);
int ecall_get_flag(const char* username, const char* password, int* pw_len, char* flag);

sgx_status_t SGX_CDECL ocall_save_data(int* retval, const uint8_t* sealed_data, size_t sealed_size);
sgx_status_t SGX_CDECL ocall_load_data(int* retval, uint8_t* sealed_data, size_t sealed_size);
sgx_status_t SGX_CDECL ocall_is_data(int* retval);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif
