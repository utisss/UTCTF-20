#ifndef AES_H
#define AES_H
#include <inttypes.h>

void encrypt_block(uint8_t ptext[16], uint8_t key[16], uint8_t dest[16]);
void aes_decrypt_block(uint8_t ctext[16], uint8_t key[16], uint8_t dest[16]);

void encrypt_stream(uint8_t *ptext, uint8_t key[16], uint8_t iv[16],
					  uint32_t num_blocks, uint8_t *dest);
void decrypt_stream(uint8_t *ctext, uint8_t key[16], uint8_t iv[16],
					  uint32_t num_blocks, uint8_t *dest);
#endif