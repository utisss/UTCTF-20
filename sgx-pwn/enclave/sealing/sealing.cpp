#include "enclave_t.h"
#include "sgx_trts.h"
#include "sgx_tseal.h"

#include "data.h"
#include "sealing.h"

sgx_status_t seal_data(const data_t* data, sgx_sealed_data_t* sealed_data, size_t sealed_size) {
    return sgx_seal_data(0, NULL, sizeof(data_t), (uint8_t*)data, sealed_size, sealed_data);
}

sgx_status_t unseal_data(const sgx_sealed_data_t* sealed_data, data_t* plaintext, uint32_t plaintext_size) {
    return sgx_unseal_data(sealed_data, NULL, NULL, (uint8_t*)plaintext, &plaintext_size);
}

