#include "enclave_u.h"
#include "sgx_urts.h"

#include <cstring>
#include <fstream>
#include <getopt.h>

#include "app.h"
#include "utils.h"
#include "data.h"
#include "enclave.h"
#include "stdio.h"

using namespace std;


// OCALLs implementation
int ocall_save_data(const uint8_t* sealed_data, const size_t sealed_size) {
		ofstream file(DATA_FILE, ios::out | ios::binary);
		if (file.fail()) {return 1;}
		file.write((const char*) sealed_data, sealed_size);
		file.close();
		return 0;
}

int ocall_is_data(void) {
		ifstream file(DATA_FILE, ios::in | ios::binary);
		if (file.fail()) {return 0;} // failure means no data found
		file.close();
		return 1;
}


int ocall_load_data(uint8_t* sealed_data, const size_t sealed_size) {
		ifstream file(DATA_FILE, ios::in | ios::binary);
		if (file.fail()) {return 1;}
		file.read((char*) sealed_data, sealed_size);
		file.close();
		return 0;
}

int main(int argc, char** argv) {

		char command[100] = "";
		char username[1024] = "";
		char password[128] = "";
		char flag[MAX_FLAG];
		sgx_enclave_id_t eid = 0;
		sgx_launch_token_t token = {0};
		int updated, ret;
		sgx_status_t ecall_status, enclave_status;

		enclave_status = sgx_create_enclave(ENCLAVE_FILE, SGX_DEBUG_FLAG, &token, &updated, &eid, NULL);
		if(enclave_status != SGX_SUCCESS) {
				error_print("Fail to initialize enclave."); 
				return -1;
		}
		info_print("Enclave successfully initialized.");

		while(true) {
			printf("\nWhat would you like to do?\n");
			printf("u: set your username\n");
			printf("r: recover data\n\n");
			fgets(command, 100, stdin);
			if(command[0] == 'u') {
				warning_print("We don't even use this for authentication!");
				printf("What would you like to change your username to?\n");
				fgets(username, 1024, stdin);
				printf("Your username has been changed to:\n");
				printf(username);
				printf("\n");
			}
			if(command[0] == 'r') {
				if(strlen(username) == 0) {
					error_print("You must set a username before recovering data.\n");
					continue;
				}
				printf("Please provide your password:\n");
				fgets(password, 128, stdin);
				int x = 128;
				ecall_status = ecall_get_flag(eid, &ret, username, password, &x, flag);
				if(ecall_status != SGX_SUCCESS || is_error(ret) ) {
					error_print("Enclave Failure.");
				}
				else {
					info_print("Data return successfully");
					printf("Did you actually think we would just print the flag?\n");
					printf("You'll need to try harder than that.\n");
				}
			}
			if(command[0] == 's') {
				printf("Set the password:\n");
				fgets(password, 128, stdin);
				strtok(password, "\n");
				printf("Set the flag:\n");
				fgets(flag, 128, stdin);
				strtok(flag, "\n");
				ecall_status = ecall_save_data(eid, &ret, flag, password);
				if(ecall_status != SGX_SUCCESS || is_error(ret) ) {
					error_print("Enclave Failure.");
				}
				else {
					info_print("Sealed successfully");
				}
			}
		}

		// destroy enclave
		enclave_status = sgx_destroy_enclave(eid);
		if(enclave_status != SGX_SUCCESS) {
				error_print("Fail to destroy enclave."); 
				return -1;
		}
		info_print("Enclave successfully destroyed.");

		info_print("Program exit success.");
		return 0;
}
