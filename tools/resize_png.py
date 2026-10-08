import zlib, struct, sys

def read_png(path):
    data = open(path, 'rb').read()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos = 8
    idat = b''
    w = h = depth = ctype = None
    plte = None
    while pos < len(data):
        ln = struct.unpack('>I', data[pos:pos+4])[0]
        typ = data[pos+4:pos+8]
        body = data[pos+8:pos+8+ln]
        if typ == b'IHDR':
            w, h, depth, ctype = struct.unpack('>IIBB', body[:10])
        elif typ == b'IDAT':
            idat += body
        elif typ == b'PLTE':
            plte = body
        pos += 12 + ln
    assert depth == 8, 'only 8-bit supported, got %s' % depth
    channels = {0:1, 2:3, 3:1, 4:2, 6:4}[ctype]
    raw = zlib.decompress(idat)
    stride = w * channels
    out = bytearray(h * stride)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p+stride]); p += stride
        if f == 1:
            for i in range(channels, stride):
                line[i] = (line[i] + line[i-channels]) & 255
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i-channels] if i >= channels else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i-channels] if i >= channels else 0
                b = prev[i]
                c = prev[i-channels] if i >= channels else 0
                pp = a + b - c
                pa, pb, pc = abs(pp-a), abs(pp-b), abs(pp-c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out[y*stride:(y+1)*stride] = line
        prev = line
    return w, h, channels, bytes(out)

def write_png(path, w, h, channels, pix):
    ctype = {1:0, 2:4, 3:2, 4:6}[channels]
    stride = w * channels
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        raw += pix[y*stride:(y+1)*stride]
    def chunk(t, d):
        return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    ihdr = struct.pack('>IIBBBBB', w, h, 8, ctype, 0, 0, 0)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr)
                + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))

def resize(src, dst, target):
    w, h, ch, pix = read_png(src)
    if w <= target:
        print('no resize needed (%dx%d)' % (w, h)); return
    nw = nh = target
    ow, oh = w, h
    out = bytearray(nw * nh * ch)
    for y in range(nh):
        sy = y * oh // nh
        row = sy * ow * ch
        for x in range(nw):
            sx = x * ow // nw
            s = row + sx * ch
            d = (y * nw + x) * ch
            out[d:d+ch] = pix[s:s+ch]
    write_png(dst, nw, nh, ch, bytes(out))
    print('resized %dx%d -> %dx%d' % (w, h, nw, nh))

resize(sys.argv[1], sys.argv[2], int(sys.argv[3]))
