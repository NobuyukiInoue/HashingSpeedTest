#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "openssl/sha.h"

#define byte    unsigned char
#define ENOMEM   KRB5KRB_ERR_GENERIC


void sha256(char *string, char *outputBuffer)
{
    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256_CTX sha256;
    SHA256_Init(&sha256);
    SHA256_Update(&sha256, string, strlen(string));
    SHA256_Final(hash, &sha256);
    int i = 0;
    for(i = 0; i < SHA256_DIGEST_LENGTH; i++)
    {
        sprintf(outputBuffer + (i * 2), "%02x", hash[i]);
    }
    outputBuffer[64] = 0;
}

/*
int sha256_file(char *path, char *outputBuffer)
{
    FILE *file = fopen(path, "rb");
    if (!file)
        return -534;

    byte hash[SHA256_DIGEST_LENGTH];
    SHA256_CTX sha256;
    SHA256_Init(&sha256);
    const int bufSize = 32768;
    byte *buffer = malloc(bufSize);
    int bytesRead = 0;

    if (!buffer)
        return ENOMEM;

    while((bytesRead = fread(buffer, 1, bufSize, file))) {
        SHA256_Update(&sha256, buffer, bytesRead);
    }
    SHA256_Final(hash, &sha256);

    sha256_hash_string(hash, outputBuffer);
    fclose(file);
    free(buffer);

    return 0;
}
*/

int main(void)
{
    static unsigned char buffer[65];

    sha256("string", (char *)buffer);
    printf("%s\n", buffer);	

    return 0;
}
