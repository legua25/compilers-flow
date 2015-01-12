#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

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

// predef ===

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
  printf(")");
}

// Int ===

long long IntCompanion_random_IntCompanion_Int_Int(int companion, long long from, long long to) {
  if (!(from < to)) {
    printf("Invalid range [%lld, %lld).\n", from, to);
    exit(1);
  }

  return from + ((((long long) (rand() & ~(1 << 31))) << 32) + rand()) % (to - from);
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
  if (!(0 <= index && index < this->size)) {
    printf("String index %lld out of bounds.\n", index);
    exit(1);
  }

  return this->chars[index];
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

void String_substring_String_Int_Int(String *result, String *this, long long from, long long to) {
  if (!(0 <= from && from <= to && to <= this->size)) {
    printf("Invalid substring range [%lld, %lld).\n", from, to);
    exit(1);
  }

  long long size = to - from;
  long long nrOfChars = size + 1;
  char *dest = malloc(nrOfChars * sizeof(char));

  char *p1 = dest;
  char *p2 = &this->chars[from];
  char *end = &this->chars[to];

  for (; p2 <= end; ++p1, ++p2)
    *p1 = *p2;

  dest[size] = '\0';

  result->size = size;
  result->chars = dest;
}

// IntArray ===

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

void IntArrayCompanion_apply_IntArrayCompanion_Int(IntArray *result, int companion, long long size) {
  if (!(size >= 0)) {
    printf("Invalid IntArray size %lld.\n", size);
    exit(1);
  }

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
  if (!(0 <= index && index < this->size)) {
    printf("IntArray index %lld out of bounds.\n", index);
    exit(1);
  }

  return this->ints[index];
}

void IntArray_update_IntArray_Int_Int(IntArray *this, long long index, long long elem) {
  if (!(0 <= index && index < this->size)) {
    printf("IntArray index %lld out of bounds.\n", index);
    exit(1);
  }

  this->ints[index] = elem;
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
