#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct String String;
struct String {
  long long size;
  char *chars;
};

typedef struct IntArray IntArray;
struct IntArray {
  long long size;
  long long capacity;
  long long *ints;
};

// Simple ===

// void debugString(String *string) {
//   printf("String { %lld \"%s\" }\n", string->size, string->chars);
// }

void readLine(String *result) {
  int maxSize = 256;
  char *line = malloc(maxSize * sizeof(char));

  int i = 0;
  int c;
  for (; (c = getchar()) != EOF && c != '\n'; ++i) {
    if (i == maxSize) {
      maxSize *= 2;
      line = realloc(line, maxSize);
    }

    line[i] = c;
  }

  line[i] = '\0';

  result->size = i;
  result->chars = line;
}

long long readInt() {
  long long value;
  scanf(" %lld", &value);
  return value;
}

void print_Bool(int value) {
  if (value)
    printf("true");
  else
    printf("false");
}

void print_Char(char value) {
  printf("%c", value);
}

void print_String(String *string) {
  printf("%s", string->chars);
}

void print_Int(long long value) {
  printf("%lld", value);
}

void print_Float(double value) {
  printf("%.5f", value);
}

void print_Unit() {
  printf("{}");
}

void print_IntArray(IntArray *array) {
  printf("IntArray(");
  long long i;
  for (i = 0; i < array->size; ++i) {
    if (i == 0)
      printf("%lld", array->ints[i]);
    else
      printf(", %lld", array->ints[i]);
  }
  printf(")\n");
}

// Arrays ===

long long capacityFor(long long size) {
  long long capacity = 1;

  while (capacity <= size)
    capacity *= 2;

  return capacity;
}

long long *newArray(long long size) {
  long long *array = malloc(size * sizeof(long long));
  long long *p = array;
  long long *end = &array[size];

  for (; p < end; ++p)
    *p = 0;

  return array;
}

void copyInts(long long *dest, long long *source, long long size) {
  if (dest != NULL && source != NULL) {
    long long *p1 = dest;
    long long *p2 = source;
    long long *end = &source[size];

    for (; p2 < end; ++p1, ++p2)
      *p1 = *p2;
  }
}

// String ===

void newString(String *result, long long size, char *chars) {
  result->size = size;
  result->chars = chars;
}

long long String_size_String_(String *this) {
  return this->size;
}

char String_apply_String_Int(String *this, long long index) {
  if (0 <= index && index < this->size) {
    return this->chars[index];
  }
  else {
    printf("String index %lld out of bounds.\n", index);
    exit(1);
  }
  return 0;
}

void String_reverse_String_(String *result, String *this) {
  long long size = this->size;
  long long nrOfChars = size + 1;
  char *source = this->chars;
  char *dest = malloc(nrOfChars * sizeof(char));

  char *p1 = dest;
  char *p2 = &source[size - 1];

  for (; source <= p2; ++p1, --p2)
    *p1 = *p2;

  dest[size] = '\0';

  result->size = size;
  result->chars = dest;
}

void String_$plus_String_String(String *result, String *this, String *that) {
  long long size = this->size + that->size;
  long long nrOfChars = size + 1;
  char *dest = malloc(nrOfChars * sizeof(char));

  char *p1, *p2, *end;
  
  p1 = dest;
  p2 = this->chars;
  end = &this->chars[this->size];

  for (; p2 <= end; ++p1, ++p2)
    *p1 = *p2;
  
  p1 = &dest[this->size];
  p2 = that->chars;
  end = &that->chars[that->size];

  for (; p2 <= end; ++p1, ++p2)
    *p1 = *p2;

  dest[size] = '\0';

  result->size = size;
  result->chars = dest;
}

// IntArray ===

void IntArrayCompanion_apply_IntArrayCompanion_Int(IntArray *result, int companion, long long size) {
  result->size = size;
  result->capacity = capacityFor(size);
  result->ints = newArray(result->capacity);
}

long long IntArray_size_IntArray_(IntArray *this) {
  return this->size;
}

long long IntArray_capacity_IntArray_(IntArray *this) {
  return this->capacity;
}

long long IntArray_apply_IntArray_Int(IntArray *this, long long index) {
  if (0 <= index && index < this->size) {
    return this->ints[index];
  }
  else {
    printf("IntArray index %lld out of bounds.\n", index);
    exit(1);
  }

  return 0;
}

void IntArray_update_IntArray_Int_Int(IntArray *this, long long index, long long elem) {
  if (0 <= index && index < this->size) {
    this->ints[index] = elem;
  }
  else {
    printf("IntArray index %lld out of bounds.\n", index);
    exit(1);
  }
}  

void IntArray_resize_IntArray_Int(IntArray *this, long long size) {
  long long capacity = capacityFor(size);

  if (this->capacity != capacity) {
    long long oldCapacity = this->capacity;

    this->size = size;
    this->capacity = capacity;
    this->ints = realloc(this->ints, capacity * sizeof(long long));

    long long *p = &this->ints[oldCapacity];
    long long *end = &this->ints[capacity];

    for (; p < end; ++p)
      *p = 0;
  }
}

void IntArray_$plus$equal_IntArray_Int(IntArray *this, long long elem) {
  long long size = this->size + 1;

  if (this->capacity < size)
    IntArray_resize_IntArray_Int(this, size);
  else
    this->size = size;

  this->ints[size - 1] = elem;
}

void IntArray_$plus$plus$equal_IntArray_IntArray(IntArray *this, IntArray *elems) {
  long long arraySize = this->size;
  long long elemsSize = elems->size;
  long long *elemsInts = elems->ints;
  long long size = arraySize + elemsSize;

  if (this->capacity < size)
    IntArray_resize_IntArray_Int(this, size);
  else
    this->size = size;

  copyInts(this->ints + arraySize, elems->ints, elemsSize);
}

void IntArray_clear_IntArray(IntArray *this) {
  IntArray_resize_IntArray_Int(this, 0);
}

int main() {
  return 0;
}
