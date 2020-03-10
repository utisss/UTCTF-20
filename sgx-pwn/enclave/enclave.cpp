#include "enclave_t.h"
#include "string.h"

#include "enclave.h"
#include "data.h"

#include "sgx_tseal.h"
#include "sealing/sealing.h"

int ecall_save_data(const char* flag, const char* password) {

	sgx_status_t ocall_status, sealing_status;
	int ocall_ret;

	// 2. abort if data already exist
	ocall_status = ocall_is_data(&ocall_ret);
	if (ocall_ret != 0) {
		return ERR_DATA_ALREADY_EXISTS;
	}

	data_t * data = (data_t *)malloc(sizeof(data_t));
	strncpy(data->flag, flag, MAX_FLAG);
	strncpy(data->pass, password, MAX_PASS);
	// 4. seal data
	size_t sealed_size = sizeof(sgx_sealed_data_t) + sizeof(data_t);
	uint8_t* sealed_data = (uint8_t*)malloc(sealed_size);
	sealing_status = seal_data(data, (sgx_sealed_data_t*)sealed_data, sealed_size);
	if (sealing_status != SGX_SUCCESS) {
	return ERR_FAIL_SEAL;
	}
		

	// 5. save data
	ocall_status = ocall_save_data(&ocall_ret, sealed_data, sealed_size); 
	free(sealed_data);
	if (ocall_ret != 0 || ocall_status != SGX_SUCCESS) {
		return ERR_CANNOT_SAVE_DATA;
	}


	// 6. exit enclave
	return RET_SUCCESS;
}


/**
 * @brief			 Provides the data content. The sizes/length of 
 *						 pointers need to be specified, otherwise SGX will
 *						 assume a count of 1 for all pointers.
 *
 */
int ecall_get_flag(const char* username, const char* password, int * pw_len, char* flag) {

	//
	// OVERVIEW: 
	//	1. [ocall] load data
	//	2. unseal data
	//	3. verify master-password
	//	4. return data to app
	//	5. exit enclave
	//
	//
	sgx_status_t ocall_status, sealing_status;
	int ocall_ret;

	char password_cp[128];
	memcpy(password_cp, password, *pw_len);
	strtok(password_cp, "\n");

	// 1. load data
	size_t sealed_size = sizeof(sgx_sealed_data_t) + sizeof(data_t);
	uint8_t* sealed_data = (uint8_t*)malloc(sealed_size);
	ocall_status = ocall_load_data(&ocall_ret, sealed_data, sealed_size);
	if (ocall_ret != 0 || ocall_status != SGX_SUCCESS) {
		free(sealed_data);
		return ERR_CANNOT_LOAD_DATA;
	}


	// 2. unseal loaded data
	uint32_t plaintext_size = sizeof(data_t);
	data_t* unsealed_data = (data_t*)malloc(plaintext_size);
	sealing_status = unseal_data((sgx_sealed_data_t*)sealed_data, unsealed_data, plaintext_size);
	free(sealed_data);
	if (sealing_status != SGX_SUCCESS) {
		free(unsealed_data);
		return ERR_FAIL_UNSEAL;
	}
		
		
	// 3. verify master-password
	if (strcmp(unsealed_data->pass, password_cp) != 0) {
		free(unsealed_data);
		return ERR_WRONG_MASTER_PASSWORD;
	}

	// 4. return data to app
	strncpy(flag, unsealed_data->flag, MAX_FLAG);
	free(unsealed_data);


	// 5. exit enclave
	return RET_SUCCESS;
}

