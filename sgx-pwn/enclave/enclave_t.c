#include "enclave_t.h"

#include "sgx_trts.h" /* for sgx_ocalloc, sgx_is_outside_enclave */
#include "sgx_lfence.h" /* for sgx_lfence */

#include <errno.h>
#include <mbusafecrt.h> /* for memcpy_s etc */
#include <stdlib.h> /* for malloc/free etc */

#define CHECK_REF_POINTER(ptr, siz) do {	\
	if (!(ptr) || ! sgx_is_outside_enclave((ptr), (siz)))	\
		return SGX_ERROR_INVALID_PARAMETER;\
} while (0)

#define CHECK_UNIQUE_POINTER(ptr, siz) do {	\
	if ((ptr) && ! sgx_is_outside_enclave((ptr), (siz)))	\
		return SGX_ERROR_INVALID_PARAMETER;\
} while (0)

#define CHECK_ENCLAVE_POINTER(ptr, siz) do {	\
	if ((ptr) && ! sgx_is_within_enclave((ptr), (siz)))	\
		return SGX_ERROR_INVALID_PARAMETER;\
} while (0)

#define ADD_ASSIGN_OVERFLOW(a, b) (	\
	((a) += (b)) < (b)	\
)


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

static sgx_status_t SGX_CDECL sgx_ecall_save_data(void* pms)
{
	CHECK_REF_POINTER(pms, sizeof(ms_ecall_save_data_t));
	//
	// fence after pointer checks
	//
	sgx_lfence();
	ms_ecall_save_data_t* ms = SGX_CAST(ms_ecall_save_data_t*, pms);
	sgx_status_t status = SGX_SUCCESS;
	const char* _tmp_flag = ms->ms_flag;
	size_t _len_flag = ms->ms_flag_len ;
	char* _in_flag = NULL;
	const char* _tmp_password = ms->ms_password;
	size_t _len_password = ms->ms_password_len ;
	char* _in_password = NULL;

	CHECK_UNIQUE_POINTER(_tmp_flag, _len_flag);
	CHECK_UNIQUE_POINTER(_tmp_password, _len_password);

	//
	// fence after pointer checks
	//
	sgx_lfence();

	if (_tmp_flag != NULL && _len_flag != 0) {
		_in_flag = (char*)malloc(_len_flag);
		if (_in_flag == NULL) {
			status = SGX_ERROR_OUT_OF_MEMORY;
			goto err;
		}

		if (memcpy_s(_in_flag, _len_flag, _tmp_flag, _len_flag)) {
			status = SGX_ERROR_UNEXPECTED;
			goto err;
		}

		_in_flag[_len_flag - 1] = '\0';
		if (_len_flag != strlen(_in_flag) + 1)
		{
			status = SGX_ERROR_UNEXPECTED;
			goto err;
		}
	}
	if (_tmp_password != NULL && _len_password != 0) {
		_in_password = (char*)malloc(_len_password);
		if (_in_password == NULL) {
			status = SGX_ERROR_OUT_OF_MEMORY;
			goto err;
		}

		if (memcpy_s(_in_password, _len_password, _tmp_password, _len_password)) {
			status = SGX_ERROR_UNEXPECTED;
			goto err;
		}

		_in_password[_len_password - 1] = '\0';
		if (_len_password != strlen(_in_password) + 1)
		{
			status = SGX_ERROR_UNEXPECTED;
			goto err;
		}
	}

	ms->ms_retval = ecall_save_data((const char*)_in_flag, (const char*)_in_password);

err:
	if (_in_flag) free(_in_flag);
	if (_in_password) free(_in_password);
	return status;
}

static sgx_status_t SGX_CDECL sgx_ecall_get_flag(void* pms)
{
	CHECK_REF_POINTER(pms, sizeof(ms_ecall_get_flag_t));
	//
	// fence after pointer checks
	//
	sgx_lfence();
	ms_ecall_get_flag_t* ms = SGX_CAST(ms_ecall_get_flag_t*, pms);
	sgx_status_t status = SGX_SUCCESS;
	const char* _tmp_username = ms->ms_username;
	size_t _len_username = ms->ms_username_len ;
	char* _in_username = NULL;
	const char* _tmp_password = ms->ms_password;
	size_t _len_password = 1024;
	char* _in_password = NULL;
	int* _tmp_pw_len = ms->ms_pw_len;
	size_t _len_pw_len = sizeof(int);
	int* _in_pw_len = NULL;
	char* _tmp_flag = ms->ms_flag;
	size_t _len_flag = 64;
	char* _in_flag = NULL;

	CHECK_UNIQUE_POINTER(_tmp_username, _len_username);
	CHECK_UNIQUE_POINTER(_tmp_password, _len_password);
	CHECK_UNIQUE_POINTER(_tmp_pw_len, _len_pw_len);
	CHECK_UNIQUE_POINTER(_tmp_flag, _len_flag);

	//
	// fence after pointer checks
	//
	sgx_lfence();

	if (_tmp_username != NULL && _len_username != 0) {
		_in_username = (char*)malloc(_len_username);
		if (_in_username == NULL) {
			status = SGX_ERROR_OUT_OF_MEMORY;
			goto err;
		}

		if (memcpy_s(_in_username, _len_username, _tmp_username, _len_username)) {
			status = SGX_ERROR_UNEXPECTED;
			goto err;
		}

		_in_username[_len_username - 1] = '\0';
		if (_len_username != strlen(_in_username) + 1)
		{
			status = SGX_ERROR_UNEXPECTED;
			goto err;
		}
	}
	if (_tmp_password != NULL && _len_password != 0) {
		if ( _len_password % sizeof(*_tmp_password) != 0)
		{
			status = SGX_ERROR_INVALID_PARAMETER;
			goto err;
		}
		_in_password = (char*)malloc(_len_password);
		if (_in_password == NULL) {
			status = SGX_ERROR_OUT_OF_MEMORY;
			goto err;
		}

		if (memcpy_s(_in_password, _len_password, _tmp_password, _len_password)) {
			status = SGX_ERROR_UNEXPECTED;
			goto err;
		}

	}
	if (_tmp_pw_len != NULL && _len_pw_len != 0) {
		if ( _len_pw_len % sizeof(*_tmp_pw_len) != 0)
		{
			status = SGX_ERROR_INVALID_PARAMETER;
			goto err;
		}
		_in_pw_len = (int*)malloc(_len_pw_len);
		if (_in_pw_len == NULL) {
			status = SGX_ERROR_OUT_OF_MEMORY;
			goto err;
		}

		if (memcpy_s(_in_pw_len, _len_pw_len, _tmp_pw_len, _len_pw_len)) {
			status = SGX_ERROR_UNEXPECTED;
			goto err;
		}

	}
	if (_tmp_flag != NULL && _len_flag != 0) {
		if ( _len_flag % sizeof(*_tmp_flag) != 0)
		{
			status = SGX_ERROR_INVALID_PARAMETER;
			goto err;
		}
		if ((_in_flag = (char*)malloc(_len_flag)) == NULL) {
			status = SGX_ERROR_OUT_OF_MEMORY;
			goto err;
		}

		memset((void*)_in_flag, 0, _len_flag);
	}

	ms->ms_retval = ecall_get_flag((const char*)_in_username, (const char*)_in_password, _in_pw_len, _in_flag);
	if (_in_flag) {
		if (memcpy_s(_tmp_flag, _len_flag, _in_flag, _len_flag)) {
			status = SGX_ERROR_UNEXPECTED;
			goto err;
		}
	}

err:
	if (_in_username) free(_in_username);
	if (_in_password) free(_in_password);
	if (_in_pw_len) free(_in_pw_len);
	if (_in_flag) free(_in_flag);
	return status;
}

SGX_EXTERNC const struct {
	size_t nr_ecall;
	struct {void* ecall_addr; uint8_t is_priv; uint8_t is_switchless;} ecall_table[2];
} g_ecall_table = {
	2,
	{
		{(void*)(uintptr_t)sgx_ecall_save_data, 0, 0},
		{(void*)(uintptr_t)sgx_ecall_get_flag, 0, 0},
	}
};

SGX_EXTERNC const struct {
	size_t nr_ocall;
	uint8_t entry_table[3][2];
} g_dyn_entry_table = {
	3,
	{
		{0, 0, },
		{0, 0, },
		{0, 0, },
	}
};


sgx_status_t SGX_CDECL ocall_save_data(int* retval, const uint8_t* sealed_data, size_t sealed_size)
{
	sgx_status_t status = SGX_SUCCESS;
	size_t _len_sealed_data = sealed_size;

	ms_ocall_save_data_t* ms = NULL;
	size_t ocalloc_size = sizeof(ms_ocall_save_data_t);
	void *__tmp = NULL;


	CHECK_ENCLAVE_POINTER(sealed_data, _len_sealed_data);

	if (ADD_ASSIGN_OVERFLOW(ocalloc_size, (sealed_data != NULL) ? _len_sealed_data : 0))
		return SGX_ERROR_INVALID_PARAMETER;

	__tmp = sgx_ocalloc(ocalloc_size);
	if (__tmp == NULL) {
		sgx_ocfree();
		return SGX_ERROR_UNEXPECTED;
	}
	ms = (ms_ocall_save_data_t*)__tmp;
	__tmp = (void *)((size_t)__tmp + sizeof(ms_ocall_save_data_t));
	ocalloc_size -= sizeof(ms_ocall_save_data_t);

	if (sealed_data != NULL) {
		ms->ms_sealed_data = (const uint8_t*)__tmp;
		if (_len_sealed_data % sizeof(*sealed_data) != 0) {
			sgx_ocfree();
			return SGX_ERROR_INVALID_PARAMETER;
		}
		if (memcpy_s(__tmp, ocalloc_size, sealed_data, _len_sealed_data)) {
			sgx_ocfree();
			return SGX_ERROR_UNEXPECTED;
		}
		__tmp = (void *)((size_t)__tmp + _len_sealed_data);
		ocalloc_size -= _len_sealed_data;
	} else {
		ms->ms_sealed_data = NULL;
	}
	
	ms->ms_sealed_size = sealed_size;
	status = sgx_ocall(0, ms);

	if (status == SGX_SUCCESS) {
		if (retval) *retval = ms->ms_retval;
	}
	sgx_ocfree();
	return status;
}

sgx_status_t SGX_CDECL ocall_load_data(int* retval, uint8_t* sealed_data, size_t sealed_size)
{
	sgx_status_t status = SGX_SUCCESS;
	size_t _len_sealed_data = sealed_size;

	ms_ocall_load_data_t* ms = NULL;
	size_t ocalloc_size = sizeof(ms_ocall_load_data_t);
	void *__tmp = NULL;

	void *__tmp_sealed_data = NULL;

	CHECK_ENCLAVE_POINTER(sealed_data, _len_sealed_data);

	if (ADD_ASSIGN_OVERFLOW(ocalloc_size, (sealed_data != NULL) ? _len_sealed_data : 0))
		return SGX_ERROR_INVALID_PARAMETER;

	__tmp = sgx_ocalloc(ocalloc_size);
	if (__tmp == NULL) {
		sgx_ocfree();
		return SGX_ERROR_UNEXPECTED;
	}
	ms = (ms_ocall_load_data_t*)__tmp;
	__tmp = (void *)((size_t)__tmp + sizeof(ms_ocall_load_data_t));
	ocalloc_size -= sizeof(ms_ocall_load_data_t);

	if (sealed_data != NULL) {
		ms->ms_sealed_data = (uint8_t*)__tmp;
		__tmp_sealed_data = __tmp;
		if (_len_sealed_data % sizeof(*sealed_data) != 0) {
			sgx_ocfree();
			return SGX_ERROR_INVALID_PARAMETER;
		}
		memset(__tmp_sealed_data, 0, _len_sealed_data);
		__tmp = (void *)((size_t)__tmp + _len_sealed_data);
		ocalloc_size -= _len_sealed_data;
	} else {
		ms->ms_sealed_data = NULL;
	}
	
	ms->ms_sealed_size = sealed_size;
	status = sgx_ocall(1, ms);

	if (status == SGX_SUCCESS) {
		if (retval) *retval = ms->ms_retval;
		if (sealed_data) {
			if (memcpy_s((void*)sealed_data, _len_sealed_data, __tmp_sealed_data, _len_sealed_data)) {
				sgx_ocfree();
				return SGX_ERROR_UNEXPECTED;
			}
		}
	}
	sgx_ocfree();
	return status;
}

sgx_status_t SGX_CDECL ocall_is_data(int* retval)
{
	sgx_status_t status = SGX_SUCCESS;

	ms_ocall_is_data_t* ms = NULL;
	size_t ocalloc_size = sizeof(ms_ocall_is_data_t);
	void *__tmp = NULL;


	__tmp = sgx_ocalloc(ocalloc_size);
	if (__tmp == NULL) {
		sgx_ocfree();
		return SGX_ERROR_UNEXPECTED;
	}
	ms = (ms_ocall_is_data_t*)__tmp;
	__tmp = (void *)((size_t)__tmp + sizeof(ms_ocall_is_data_t));
	ocalloc_size -= sizeof(ms_ocall_is_data_t);

	status = sgx_ocall(2, ms);

	if (status == SGX_SUCCESS) {
		if (retval) *retval = ms->ms_retval;
	}
	sgx_ocfree();
	return status;
}

