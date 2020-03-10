#include "enclave_u.h"
#include <errno.h>

typedef struct ms_ecall_save_data_t {
	int ms_retval;
	const char* ms_flag;
	size_t ms_flag_len;
	const char* ms_password;
	size_t ms_password_len;
} ms_ecall_save_data_t;

typedef struct ms_ecall_get_flag_t {
	int ms_retval;
	const char* ms_username;
	size_t ms_username_len;
	const char* ms_password;
	int* ms_pw_len;
	char* ms_flag;
} ms_ecall_get_flag_t;

typedef struct ms_ocall_save_data_t {
	int ms_retval;
	const uint8_t* ms_sealed_data;
	size_t ms_sealed_size;
} ms_ocall_save_data_t;

typedef struct ms_ocall_load_data_t {
	int ms_retval;
	uint8_t* ms_sealed_data;
	size_t ms_sealed_size;
} ms_ocall_load_data_t;

typedef struct ms_ocall_is_data_t {
	int ms_retval;
} ms_ocall_is_data_t;

static sgx_status_t SGX_CDECL enclave_ocall_save_data(void* pms)
{
	ms_ocall_save_data_t* ms = SGX_CAST(ms_ocall_save_data_t*, pms);
	ms->ms_retval = ocall_save_data(ms->ms_sealed_data, ms->ms_sealed_size);

	return SGX_SUCCESS;
}

static sgx_status_t SGX_CDECL enclave_ocall_load_data(void* pms)
{
	ms_ocall_load_data_t* ms = SGX_CAST(ms_ocall_load_data_t*, pms);
	ms->ms_retval = ocall_load_data(ms->ms_sealed_data, ms->ms_sealed_size);

	return SGX_SUCCESS;
}

static sgx_status_t SGX_CDECL enclave_ocall_is_data(void* pms)
{
	ms_ocall_is_data_t* ms = SGX_CAST(ms_ocall_is_data_t*, pms);
	ms->ms_retval = ocall_is_data();

	return SGX_SUCCESS;
}

static const struct {
	size_t nr_ocall;
	void * table[3];
} ocall_table_enclave = {
	3,
	{
		(void*)enclave_ocall_save_data,
		(void*)enclave_ocall_load_data,
		(void*)enclave_ocall_is_data,
	}
};
sgx_status_t ecall_save_data(sgx_enclave_id_t eid, int* retval, const char* flag, const char* password)
{
	sgx_status_t status;
	ms_ecall_save_data_t ms;
	ms.ms_flag = flag;
	ms.ms_flag_len = flag ? strlen(flag) + 1 : 0;
	ms.ms_password = password;
	ms.ms_password_len = password ? strlen(password) + 1 : 0;
	status = sgx_ecall(eid, 0, &ocall_table_enclave, &ms);
	if (status == SGX_SUCCESS && retval) *retval = ms.ms_retval;
	return status;
}

sgx_status_t ecall_get_flag(sgx_enclave_id_t eid, int* retval, const char* username, const char* password, int* pw_len, char* flag)
{
	sgx_status_t status;
	ms_ecall_get_flag_t ms;
	ms.ms_username = username;
	ms.ms_username_len = username ? strlen(username) + 1 : 0;
	ms.ms_password = password;
	ms.ms_pw_len = pw_len;
	ms.ms_flag = flag;
	status = sgx_ecall(eid, 1, &ocall_table_enclave, &ms);
	if (status == SGX_SUCCESS && retval) *retval = ms.ms_retval;
	return status;
}

