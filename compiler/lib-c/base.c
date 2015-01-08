#include <stdio.h>

void Bool_print(int value) {
  if (value)
    printf("true\n");
  else
    printf("false\n");
}

void Char_print(char value) {
  printf("%c\n", value);
}

void Int_print(long long value) {
  printf("%lld\n", value);
}

void Float_print(double value) {
  printf("%.5f\n", value);
}

void Unit_print() {
  printf("()\n");
}
