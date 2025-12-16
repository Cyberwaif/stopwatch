#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <sys/mman.h>

typedef struct { void *ptr; size_t size; int is_mmap; } Block;

static volatile int running = 1;

static void on_signal(int sig) { (void)sig; running = 0; }

static void touch_pages(uint8_t *p, size_t n) {
    size_t step = 4096;
    for (size_t i = 0; i < n; i += step) p[i] = 1;
}

int main(int argc, char **argv) {
    size_t target_mb = 256;
    size_t block_kb = 1024;
    unsigned int interval_ms = 100;
    int use_mmap = 0;
    int infinite = 0;

    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "-m") && i + 1 < argc) target_mb = (size_t)strtoull(argv[++i], NULL, 10);
        else if (!strcmp(argv[i], "-b") && i + 1 < argc) block_kb = (size_t)strtoull(argv[++i], NULL, 10);
        else if (!strcmp(argv[i], "-i") && i + 1 < argc) interval_ms = (unsigned int)strtoul(argv[++i], NULL, 10);
        else if (!strcmp(argv[i], "--mmap")) use_mmap = 1;
        else if (!strcmp(argv[i], "--infinite")) infinite = 1;
    }

    signal(SIGINT, on_signal);
    signal(SIGTERM, on_signal);

    size_t target_bytes = target_mb * 1024ULL * 1024ULL;
    size_t block_size = block_kb * 1024ULL;
    if (block_size == 0) block_size = 1024;
    Block *blocks = NULL;
    size_t blocks_count = 0, blocks_cap = 0;
    size_t allocated = 0;

    while (running && (infinite || allocated < target_bytes)) {
        size_t remaining = infinite ? block_size : (target_bytes - allocated);
        size_t next = remaining < block_size ? remaining : block_size;
        if (next == 0) break;

        void *ptr = NULL;
        if (use_mmap) {
            ptr = mmap(NULL, next, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
            if (ptr == MAP_FAILED) ptr = NULL;
        } else {
            ptr = malloc(next);
        }

        if (!ptr) {
            if (block_size > 64 * 1024) { block_size /= 2; continue; }
            break;
        }

        touch_pages((uint8_t *)ptr, next);

        if (blocks_count == blocks_cap) {
            size_t new_cap = blocks_cap ? blocks_cap * 2 : 64;
            Block *nb = (Block *)realloc(blocks, new_cap * sizeof(Block));
            if (!nb) { if (use_mmap) munmap(ptr, next); else free(ptr); break; }
            blocks = nb; blocks_cap = new_cap;
        }
        blocks[blocks_count].ptr = ptr;
        blocks[blocks_count].size = next;
        blocks[blocks_count].is_mmap = use_mmap;
        blocks_count++;
        allocated += next;

        fprintf(stderr, "allocated=%zuMB progress=%d%%\n", allocated / (1024 * 1024), (int)((allocated * 100ULL) / (target_bytes ? target_bytes : allocated)));
        usleep(interval_ms * 1000U);
    }

    fprintf(stderr, "final_allocated=%zuMB blocks=%zu\n", allocated / (1024 * 1024), blocks_count);
    fprintf(stderr, "press any key to release and exit...\n");
    int ch;
    do {
        ch = getchar();
        if (!running) break;
        if (ch == EOF) usleep(100000);
    } while (running && ch == EOF);

    for (size_t i = 0; i < blocks_count; i++) {
        if (blocks[i].is_mmap) munmap(blocks[i].ptr, blocks[i].size); else free(blocks[i].ptr);
    }
    free(blocks);
    return 0;
}
