#ifndef AES_H
#define AES_H
#include <inttypes.h>

void aes_encrypt_block(uint8_t ptext[16], uint8_t key[16], uint8_t dest[16]);
void aes_decrypt_block(uint8_t ctext[16], uint8_t key[16], uint8_t dest[16]);

void aes_encrypt_pcbc(uint8_t *ptext, uint8_t key[16], uint8_t iv[16],
					  uint32_t num_blocks, uint8_t *dest);
void aes_decrypt_pcbc(uint8_t *ctext, uint8_t key[16], uint8_t iv[16],
					  uint32_t num_blocks, uint8_t *dest);
void aes_encrypt_cbc(uint8_t *ptext, uint8_t key[16], uint8_t iv[16],
					  uint32_t num_blocks, uint8_t *dest);
void aes_decrypt_cbc(uint8_t *ctext, uint8_t key[16], uint8_t iv[16],
					  uint32_t num_blocks, uint8_t *dest);
void aes_cbc_mac(uint8_t *ptext, uint8_t key[16], uint32_t num_blocks,
				      uint8_t dest[16]);
#endif