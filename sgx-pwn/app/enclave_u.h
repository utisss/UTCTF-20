#ifndef ENCLAVE_U_H__
#define ENCLAVE_U_H__

#include <stdint.h>
#include <wchar.h>
#include <stddef.h>
#include <string.h>
#include "sgx_edger8r.h" /* for sgx_status_t etc. */

#include "data.h"

#include <stdlib.h> /* for size_t */

#define SGX_CAST(type, item) ((type)(item))

#ifdef __cplusplus
extern "C" {
#endif

#ifndef OCALL_SAVE_DATA_DEFINED__
#define OCALL_SAVE_DATA_DEFINED__
int SGX_UBRIDGE(SGX_NOCONVENTION, ocall_save_data, (const uint8_t* sealed_data, size_t sealed_size));
#endif
#ifndef OCALL_LOAD_DATA_DEFINED__
#define OCALL_LOAD_DATA_DEFINED__
int SGX_UBRIDGE(SGX_NOCONVENTION, ocall_load_data, (uint8_t* sealed_data, size_t sealed_size));
#endif
#ifndef OCALL_IS_DATA_DEFINED__
#define OCALL_IS_DATA_DEFINED__
int SGX_UBRIDGE(SGX_NOCONVENTION, ocall_is_data, (void));
#endif

sgx_status_t ecall_save_data(sgx_enclave_id_t eid, int* retval, const char* flag, const char* password);
sgx_status_t ecall_get_flag(sgx_enclave_id_t eid, int* retval, const char* username, const char* password, int* pw_len, char* flag);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif
