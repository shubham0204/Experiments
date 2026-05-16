#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

typedef struct {
    const char* data;
    size_t length;
} string_t;

string_t str_new(const char* data, size_t len) {
    string_t s = {.data = data, .length = len};
    return s;
}

bool streq(const string_t* s1, const string_t* s2) {
    if (s1->length != s2->length) {
        return false;
    }
    for (int i = 0; i < s1->length; i++) {
        if (s1->data[i] != s2->data[i]) {
            return false;
        }
    }
    return true;
};

string_t str_from_bytearr(const uint8_t* arr) {
    int idx = 0;
    while (arr[idx] != '\0' && (idx < 32)) {
        idx++;
    }
    string_t str = {.data = (const char*)arr, .length = idx};
    return str;
}
