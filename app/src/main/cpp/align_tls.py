#!/usr/bin/env python3
import struct
import sys
import os

if len(sys.argv) < 2:
    print("Usage: align_tls.py <libdoom.so>")
    sys.exit(1)

path = sys.argv[1]

with open(path, "r+b") as f:
    hdr = f.read(16)
    if hdr[0:4] != b"\x7fELF":
        print("Not an ELF file")
        sys.exit(1)

    # 64-bit
    if hdr[4] != 2:
        print("Only 64-bit ELF supported")
        sys.exit(1)

    f.seek(32)
    phoff = struct.unpack("<Q", f.read(8))[0]
    f.seek(54)
    phentsize = struct.unpack("<H", f.read(2))[0]
    phnum = struct.unpack("<H", f.read(2))[0]

    for i in range(phnum):
        f.seek(phoff + i * phentsize)
        p_type = struct.unpack("<I", f.read(4))[0]
        if p_type == 7:  # PT_TLS
            # p_align is at offset 48 in 64-bit Elf64_Phdr
            f.seek(phoff + i * phentsize + 48)
            align = struct.unpack("<Q", f.read(8))[0]
            print(f"Found PT_TLS with align = {align}")
            if align < 64:
                print("Patching to 64...")
                f.seek(phoff + i * phentsize + 48)
                f.write(struct.pack("<Q", 64))
                print("Done")
            break
    else:
        print("No PT_TLS found")