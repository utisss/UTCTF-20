#include <stdio.h>

int main() {
   char str[50];
   int xor;

   printf("enter xor key: ");
   scanf("%d", &xor);

   printf("enter message: ");
   scanf("%49s", str);

   int len = strlen(str);

   printf("[");
   for(int i = 0; i < len; i++){
      printf("%d", xor ^ str[i]);
      if(i != len - 1) {
         printf(", ");
      }else{
         printf("]\n");
      }
   }

   printf("%d\n", strlen(str));
}
