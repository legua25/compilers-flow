#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void error(char* message) {
  printf("%s\n", message);
  exit(1);
}

typedef struct String String;
struct String {
  long long size;
  char * chars;
};

typedef struct IntArray IntArray;
struct IntArray {
  long long size;
  long long capacity;
  long long * ints;
};

// Simple ===

String readLine() {
  int maxSize = 256;
  char line[maxSize];
  fgets(line, maxSize, stdin);
  
  int i = 0;
  int c;
  for (; i < maxSize && (c = getchar()) != EOF && c != '\n'; ++i)
    line[i] = c;

  if (i < maxSize)
    line[i] = '\0';

  String string = { i, line };

  return string;
}

long long readInt() {
  long long value;
  scanf(" %lld", &value);
  return value;
}

void printLine_Bool(int value) {
  if (value)
    printf("true\n");
  else
    printf("false\n");
}

void printLine_Char(char value) {
  printf("%c\n", value);
}

void printLine_String(String * string) {
  printf("%s\n", string->chars);
}

void printLine_Int(long long value) {
  printf("%lld\n", value);
}

void printLine_Float(double value) {
  printf("%.5f\n", value);
}

void printLine_Unit() {
  printf("{}\n");
}

void printLine_IntArray(IntArray * array) {
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

long long * newArray(long long size) {
  long long * array = malloc(size * sizeof(long long));
  long long * p = array;
  long long * end = array + size;
  
  for (; p != end; ++p)
    *p = 0;
  
  return array;
}

void copy(long long * dest, long long * source, long long size) {
  if (dest != NULL && source != NULL) {
    long long * p1 = dest;
    long long * p2 = source;
    long long * end = source + size;
    
    for (; p2 != end; ++p1, ++p2)
      *p1 = *p2;
  }
}

// String ===

void initializeString(String * string, long long size, char * chars) {
  string->size = size;
  string->chars = chars;
}

long long String_size_(String * string) {
  return string->size;
}

char String_apply_Int(String * string, long long index) {
  if (0 <= index && index < string->size)
    return string->chars[index];

  return '\0';
}

// IntArray ===

void initializeIntArray(IntArray * array, long long size) {
  array->size = size;
  array->capacity = capacityFor(size);
  array->ints = newArray(array->capacity);
}

long long IntArray_size_(IntArray * array) {
  return array->size;
}

long long IntArray_capacity_(IntArray * array) {
  return array->capacity;
}

long long IntArray_apply_Int(IntArray * array, long long index) {
  if (0 <= index && index < array->size)
    return array->ints[index];
  
  return 0;
}

void IntArray_update_Int_Int(IntArray * array, long long index, long long elem) {
  if (0 <= index && index < array->size)
    array->ints[index] = elem;
}  

void IntArray_resize_Int(IntArray * array, long long size) {
  long long capacity = capacityFor(size);

  if (array->capacity != capacity) {
    long long * ints = newArray(capacity);
    copy(ints, array->ints, size);

    free(array->ints);

    array->size = size;
    array->capacity = capacity;
    array->ints = ints;
  }
}

void IntArray_$plus$equal_Int(IntArray * array, long long elem) {
  long long size = array->size + 1;

  if (array->capacity < size)
    IntArray_resize_Int(array, size);
  else
    array->size = size;

  array->ints[size - 1] = elem;
}

void IntArray_$plus$plus$equal_IntArray(IntArray * array, IntArray * elems) {
  long long arraySize = array->size;
  long long elemsSize = elems->size;
  long long * elemsInts = elems->ints;
  long long size = arraySize + elemsSize;

  if (array->capacity < size)
    IntArray_resize_Int(array, size);
  else
    array->size = size;

  copy(array->ints + arraySize, elems->ints, elemsSize);
}

void IntArray_clear(IntArray * array) {
  free(array->ints);
  
  array->size = 0;
  array->capacity = 0;
  array->ints = NULL;
}
