#!/usr/bin/env python3
# =====================================================================
# 极简 Markdown -> PDF 生成器（纯标准库，无第三方依赖）
# ---------------------------------------------------------------------
# 为什么自己写：本机没有 pandoc / LaTeX / wkhtmltopdf / weasyprint，
# 且 Docker Hub 不可达（拉不到 pandoc 镜像），但需要一个**可独立阅读的
# 中文 PDF**。因此直接手写 PDF 1.4 结构，并嵌入系统自带的
# Arial Unicode.ttf（覆盖 CJK）作为 CIDFontType2 + Identity-H。
#
# 用法: python3 md2pdf.py <input.md> <output.pdf> <font.ttf> [标题]
# =====================================================================

import re
import struct
import sys
import zlib

# ------------------------- 页面与版式参数 -------------------------
PAGE_W, PAGE_H = 595.28, 841.89          # A4 (pt)
MARGIN_L, MARGIN_R = 56.7, 56.7          # 左右边距
MARGIN_T, MARGIN_B = 56.7, 56.7          # 上下边距
CONTENT_W = PAGE_W - MARGIN_L - MARGIN_R

FS_H1, FS_H2, FS_H3 = 19.0, 14.5, 12.0
FS_BODY, FS_CODE, FS_TABLE = 9.6, 8.0, 8.4
LEAD_BODY, LEAD_CODE, LEAD_TABLE = 14.0, 11.0, 12.0

# 内置 14 号字体的宽度表（Helvetica / Helvetica-Bold），用于纯 ASCII 粗排
HELV = {c: 556 for c in 'abcdefghijklmnopqrstuvwxyz'}
HELV.update({c: 556 for c in '0123456789'})
_HELV_W = {
    ' ': 278, '!': 278, '"': 355, '#': 556, '$': 556, '%': 889, '&': 667, "'": 191,
    '(': 333, ')': 333, '*': 389, '+': 584, ',': 278, '-': 333, '.': 278, '/': 278,
    ':': 278, ';': 278, '<': 584, '=': 584, '>': 584, '?': 556, '@': 1015,
    '[': 278, '\\': 278, ']': 278, '^': 469, '_': 556, '`': 333, '{': 334,
    '|': 260, '}': 334, '~': 584,
}
for _c in 'ABCDEFGHIJKLMNOPQRSTUVWXYZ':
    _HELV_W[_c] = 667
for _c in 'fjrt':
    _HELV_W[_c] = 333
for _c in 'il':
    _HELV_W[_c] = 222
for _c in 'mw':
    _HELV_W[_c] = 833
for _c in 'csz':
    _HELV_W[_c] = 500
for _c in 'bdeghknoqpuvxy':
    _HELV_W[_c] = 556
_HELV_W['A'] = 667


def latin_width(ch, size):
    return _HELV_W.get(ch, 556) * size / 1000.0


# ============================ TrueType 解析 ============================
class TTF:
    """只解析 PDF 嵌入所需的表：head / hhea / maxp / hmtx / cmap / loca / glyf。"""

    def __init__(self, path):
        self.data = open(path, 'rb').read()
        d = self.data
        numTables = struct.unpack('>H', d[4:6])[0]
        self.tables = {}
        for i in range(numTables):
            off = 12 + i * 16
            tag, _cs, o, l = struct.unpack('>4sIII', d[off:off + 16])
            self.tables[tag.decode('latin1')] = (o, l)

        ho, _ = self.tables['head']
        self.unitsPerEm = struct.unpack('>H', d[ho + 18:ho + 20])[0]
        self.indexToLocFormat = struct.unpack('>h', d[ho + 50:ho + 52])[0]
        self.xMin, self.yMin, self.xMax, self.yMax = struct.unpack('>4h', d[ho + 36:ho + 44])

        mo, _ = self.tables['maxp']
        self.numGlyphs = struct.unpack('>H', d[mo + 4:mo + 6])[0]

        hho, _ = self.tables['hhea']
        self.ascent = struct.unpack('>h', d[hho + 4:hho + 6])[0]
        self.descent = struct.unpack('>h', d[hho + 6:hho + 8])[0]
        self.numHMetrics = struct.unpack('>H', d[hho + 34:hho + 36])[0]

        self.cmap = self._parse_cmap()
        self.advances = self._parse_hmtx()

    # ---- cmap: 取 (3,1) format 4 ----
    def _parse_cmap(self):
        d = self.data
        co, _ = self.tables['cmap']
        n = struct.unpack('>H', d[co + 2:co + 4])[0]
        sub = None
        for i in range(n):
            pid, eid, off = struct.unpack('>HHI', d[co + 4 + i * 8:co + 12 + i * 8])
            if (pid, eid) == (3, 1):
                sub = co + off
                break
        if sub is None:
            raise RuntimeError('未找到 (3,1) cmap 子表')
        fmt = struct.unpack('>H', d[sub:sub + 2])[0]
        if fmt != 4:
            raise RuntimeError(f'仅支持 cmap format 4，实际 {fmt}')
        segX2 = struct.unpack('>H', d[sub + 6:sub + 8])[0]
        seg = segX2 // 2
        ends = struct.unpack(f'>{seg}H', d[sub + 14:sub + 14 + segX2])
        starts = struct.unpack(f'>{seg}H', d[sub + 16 + segX2:sub + 16 + 2 * segX2])
        deltas = struct.unpack(f'>{seg}h', d[sub + 16 + 2 * segX2:sub + 16 + 3 * segX2])
        rangeOffPos = sub + 16 + 3 * segX2
        rangeOffs = struct.unpack(f'>{seg}H', d[rangeOffPos:rangeOffPos + segX2])
        self._cmap_parsed = (ends, starts, deltas, rangeOffs, rangeOffPos)
        return (ends, starts, deltas, rangeOffs, rangeOffPos)

    def gid(self, cp):
        ends, starts, deltas, rangeOffs, rangeOffPos = self._cmap_parsed
        d = self.data
        for i in range(len(ends)):
            if cp <= ends[i]:
                if cp < starts[i]:
                    return 0
                if rangeOffs[i] == 0:
                    return (cp + deltas[i]) & 0xFFFF
                addr = rangeOffPos + i * 2 + rangeOffs[i] + (cp - starts[i]) * 2
                g = struct.unpack('>H', d[addr:addr + 2])[0]
                if g == 0:
                    return 0
                return (g + deltas[i]) & 0xFFFF
        return 0

    # ---- hmtx: 每个 glyph 的 advance width ----
    def _parse_hmtx(self):
        d = self.data
        ho, _ = self.tables['hmtx']
        n = self.numGlyphs
        long_n = self.numHMetrics
        adv = []
        for i in range(min(long_n, n)):
            adv.append(struct.unpack('>H', d[ho + i * 4:ho + i * 4 + 2])[0])
        if len(adv) < n:
            last = adv[-1] if adv else 0
            adv.extend([last] * (n - len(adv)))
        return adv

    def advance(self, gid, size):
        if gid >= len(self.advances):
            return size * 0.5
        return self.advances[gid] * size / self.unitsPerEm

    # ---- 子集化（保留原始 GID 编号，仅把未用字形的 loca 区间清空）----
    def build_subset_font(self, used_gids):
        """返回精简后的完整 sfnt 字节。

        策略：**不重排 GID**。把未使用字形的 loca 起止设为相同值（长度 0），
        已使用字形原样保留。这样：
          * CIDToGIDMap 仍可用 Identity（CID == 原始 GID），映射绝对正确；
          * 复合字形的子字形索引无需重写；
          * 字体体积可大幅下降（未用轮廓被丢弃）。
        """
        d = self.data
        numTables = struct.unpack('>H', d[4:6])[0]
        lo, _ = self.tables['loca']
        go, _ = self.tables['glyf']

        def raw_index(g):
            if self.indexToLocFormat == 0:
                return struct.unpack('>H', d[lo + g * 2:lo + g * 2 + 2])[0] * 2
            return struct.unpack('>I', d[lo + g * 4:lo + g * 4 + 4])[0]

        # 收集依赖闭包（复合字形引用的子字形必须保留）
        need = set(used_gids) | {0}
        frontier = list(need)
        while frontier:
            g = frontier.pop()
            if g >= self.numGlyphs:
                continue
            st, en = raw_index(g), raw_index(g + 1)
            if en <= st:
                continue
            blob = d[go + st:go + en]
            if len(blob) >= 2 and struct.unpack('>h', blob[:2])[0] < 0:
                pp = 10
                while pp + 4 <= len(blob):
                    flags, gi = struct.unpack('>HH', blob[pp:pp + 4])
                    if gi not in need:
                        need.add(gi)
                        frontier.append(gi)
                    pp += 4
                    pp += 4 if (flags & 0x0001) else 2
                    if flags & 0x0008:
                        pp += 2
                    elif flags & 0x0040:
                        pp += 4
                    elif flags & 0x0080:
                        pp += 8
                    if not (flags & 0x0020):
                        break

        # 重建 glyf + loca（long 格式）
        out = bytearray()
        offsets = [0]
        for g in range(self.numGlyphs):
            if g in need:
                st, en = raw_index(g), raw_index(g + 1)
                blob = bytearray(d[go + st:go + en])
                if len(blob) % 4:
                    blob.extend(b'\x00' * (4 - len(blob) % 4))
                out.extend(blob)
            offsets.append(len(out))
        new_glyf = bytes(out)
        new_loca = b''.join(struct.pack('>I', o) for o in offsets)

        # 组装新 sfnt：逐表复制，替换 loca/glyf/head 的 indexToLocFormat
        keep = [t for t in self.tables if t not in ('loca', 'glyf')]
        entries = []
        for t in keep:
            o, l = self.tables[t]
            body = bytearray(d[o:o + l])
            if t == 'head':
                body[50:52] = struct.pack('>h', 1)      # indexToLocFormat = 1 (long)
            entries.append((t, bytes(body)))
        entries.append(('loca', new_loca))
        entries.append(('glyf', new_glyf))
        entries.sort(key=lambda x: x[0])

        n = len(entries)
        searchRange = 16 * (2 ** (n.bit_length() - 1))
        entrySelector = n.bit_length() - 1
        rangeShift = 16 * n - searchRange

        hdr = struct.pack('>IHHHH', 0x00010000, n, searchRange, entrySelector, rangeShift)
        # 目录 + 数据
        dir_size = 12 + n * 16
        offset = dir_size
        table_dir = bytearray()
        blob = bytearray()
        for t, body in entries:
            pad = (-len(body)) % 4
            table_dir += struct.pack('>4sIII', t.encode('latin1'),
                                     sum(1 for _ in ()) or 0, offset, len(body))
            blob += body + b'\x00' * pad
            offset += len(body) + pad
        # 修正 checksum 占位（置 0，阅读器通常不校验嵌入字体 checksum）
        out2 = bytearray(hdr) + table_dir + blob
        return bytes(out2)

    # ---- loca/glyf 子集化：抽取指定 gid 集合的轮廓数据 ----
    def glyph_data(self, gids):
        """返回 (glyf_bytes, loca_offsets, gid_remap)
        做法：按 gid 升序把需要的字形连续拼接，重建 loca。
        复合字形（composite）引用的子字形也必须一并纳入。
        """
        d = self.data
        lo, _ = self.tables['loca']
        go, _ = self.tables['glyf']

        def raw_index(g):
            if self.indexToLocFormat == 0:
                return struct.unpack('>H', d[lo + g * 2:lo + g * 2 + 2])[0] * 2
            return struct.unpack('>I', d[lo + g * 4:lo + g * 4 + 4])[0]

        # 闭包：递归收集复合字形的依赖
        need = set(gids)
        frontier = list(gids)
        while frontier:
            g = frontier.pop()
            s, e = raw_index(g), raw_index(g + 1)
            if e <= s:
                continue
            blob = d[go + s:go + e]
            if len(blob) >= 2:
                ncont = struct.unpack('>h', blob[:2])[0]
                if ncont < 0:   # 复合字形
                    p = 10
                    while p + 4 <= len(blob):
                        flags, gi = struct.unpack('>HH', blob[p:p + 4])
                        if gi not in need:
                            need.add(gi)
                            frontier.append(gi)
                        p += 4
                        p += 4 if (flags & 0x0001) else 2   # ARG_1_AND_2_ARE_WORDS
                        if flags & 0x0008:
                            p += 2
                        elif flags & 0x0040:
                            p += 4
                        elif flags & 0x0080:
                            p += 8
                        if not (flags & 0x0020):
                            break
        ordered = sorted(need)
        remap = {g: i for i, g in enumerate(ordered)}
        out = bytearray()
        loca = [0]
        for g in ordered:
            s, e = raw_index(g), raw_index(g + 1)
            blob = bytearray(d[go + s:go + e])
            if len(blob) >= 2 and struct.unpack('>h', blob[:2])[0] < 0:
                # 重写复合字形的子字形索引
                p = 10
                while p + 4 <= len(blob):
                    flags, gi = struct.unpack('>HH', blob[p:p + 4])
                    ng = remap.get(gi, 0)
                    blob[p + 2:p + 4] = struct.pack('>H', ng)
                    p += 4
                    p += 4 if (flags & 0x0001) else 2
                    if flags & 0x0008:
                        p += 2
                    elif flags & 0x0040:
                        p += 4
                    elif flags & 0x0080:
                        p += 8
                    if not (flags & 0x0020):
                        break
            if len(blob) % 4:
                blob.extend(b'\x00' * (4 - len(blob) % 4))
            out.extend(blob)
            loca.append(len(out))
        return bytes(out), loca, remap


# ============================ Markdown 解析 ============================
def parse_markdown(md):
    """把 Markdown 转成块列表：('h1'|'h2'|'h3'|'p'|'li'|'code'|'table'|'hr'|'quote', payload)"""
    lines = md.split('\n')
    blocks, i = [], 0
    while i < len(lines):
        ln = lines[i]
        s = ln.strip()

        if s.startswith('```'):
            i += 1
            buf = []
            while i < len(lines) and not lines[i].strip().startswith('```'):
                buf.append(lines[i])
                i += 1
            i += 1
            blocks.append(('code', buf))
            continue
        if re.match(r'^#{1,6}\s', s):
            lvl = len(re.match(r'^(#+)', s).group(1))
            kind = 'h1' if lvl == 1 else ('h2' if lvl == 2 else 'h3')
            blocks.append((kind, s[lvl:].strip()))
            i += 1
            continue
        if re.match(r'^(-{3,}|\*{3,}|_{3,})$', s):
            blocks.append(('hr', None))
            i += 1
            continue
        if s.startswith('|') and i + 1 < len(lines) and re.match(r'^\|[\s:\-|]+\|$', lines[i + 1].strip()):
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                if not re.match(r'^\|[\s:\-|]+\|$', lines[i].strip()):
                    cells = [c.strip() for c in lines[i].strip().strip('|').split('|')]
                    rows.append(cells)
                i += 1
            blocks.append(('table', rows))
            continue
        if s.startswith('>'):
            buf = []
            while i < len(lines) and lines[i].strip().startswith('>'):
                buf.append(lines[i].strip().lstrip('>').strip())
                i += 1
            blocks.append(('quote', ' '.join(buf)))
            continue
        if re.match(r'^([-*+]|\d+\.)\s', s):
            buf = []
            while i < len(lines) and re.match(r'^([-*+]|\d+\.)\s', lines[i].strip()):
                buf.append(re.sub(r'^([-*+]|\d+\.)\s+', '', lines[i].strip()))
                i += 1
            blocks.append(('li', buf))
            continue
        if not s:
            i += 1
            continue
        buf = [s]
        i += 1
        while i < len(lines) and lines[i].strip() and not re.match(
                r'^(#{1,6}\s|```|\||>|([-*+]|\d+\.)\s|-{3,}$)', lines[i].strip()):
            buf.append(lines[i].strip())
            i += 1
        blocks.append(('p', ' '.join(buf)))
    return blocks


def strip_inline(t):
    """去掉 Markdown 行内标记，保留纯文本。"""
    t = re.sub(r'!\[([^\]]*)\]\([^)]*\)', r'\1', t)          # 图片
    t = re.sub(r'\[([^\]]*)\]\(([^)]*)\)', r'\1', t)          # 链接
    t = re.sub(r'`([^`]*)`', r'\1', t)                        # 行内代码
    t = t.replace('**', '').replace('__', '')
    t = re.sub(r'(?<!\w)\*([^*]+)\*(?!\w)', r'\1', t)
    t = re.sub(r'(?<!\w)_([^_]+)_(?!\w)', r'\1', t)
    t = t.replace('&nbsp;', ' ').replace('&amp;', '&').replace('&lt;', '<').replace('&gt;', '>')
    return t


# ============================ PDF 构造 ============================
class PDF:
    # 是否对字体做子集化。默认 False（嵌入完整字体）。
    # 原因：自写的子集化器需同时重建 loca/glyf 并保证 hmtx 一致，
    # 实测容易被阅读器拒绝渲染（页面空白）。嵌入完整字体 + Identity
    # CIDToGIDMap 最稳，代价是 PDF 体积约等于字体体积。
    SUBSET = True

    def __init__(self, font_path, font_name='AUCJK'):
        self.ttf = TTF(font_path)
        self.font_name = font_name
        self.pages = []          # 每页: list of content-stream 片段
        self.used_gids = set([0])
        self._char_gid = {}      # char -> 原始 gid（供 ToUnicode 使用）
        self._cur = None

    # ---------- 文本编码 ----------
    def enc(self, text, size, bold=False):
        """把字符串编码为 Identity-H 的 2 字节/glyph 序列，并返回宽度。"""
        out = bytearray()
        w = 0.0
        for ch in text:
            g = self.ttf.gid(ord(ch))
            if g == 0 and ord(ch) not in (0x20,):
                g = 0
            self.used_gids.add(g)
            out += struct.pack('>H', g)
            if g:
                w += self.ttf.advance(g, size)
            elif 32 <= ord(ch) < 127:
                w += latin_width(ch, size)
            else:
                w += size * 0.5
        return bytes(out), w

    def text_width(self, text, size):
        _, w = self.enc(text, size)
        return w

    # ---------- 排版 ----------
    def wrap(self, text, size, width):
        """按可用宽度折行；中文逐字折，ASCII 按词折。"""
        lines, cur = [], ''
        for ch in text:
            if ch == '\n':
                lines.append(cur)
                cur = ''
                continue
            trial = cur + ch
            if self.text_width(trial, size) > width and cur:
                # 尽量在空格处断行
                if ch not in '，。、；：）】》”’!?,.;:)]}' and ' ' in cur[:-1]:
                    idx = cur.rfind(' ')
                    lines.append(cur[:idx])
                    cur = cur[idx + 1:] + ch
                else:
                    lines.append(cur)
                    cur = ch
            else:
                cur = trial
        if cur:
            lines.append(cur)
        return lines or ['']

    def wrap_code(self, line, size, width):
        """等宽文本折行：按字符数估算宽度，优先在空格/|/&&/; 等边界断开。"""
        if not line:
            return ['']
        cw = size * 0.60
        maxn = max(int(width / cw), 10)
        out = []
        rest = line
        while rest:
            if len(rest) <= maxn:
                out.append(rest)
                break
            seg = rest[:maxn]
            cut = -1
            for pat in ('&&', '|', ';', ' ', ',', '/'):
                k = seg.rfind(pat)
                if k > cut:
                    cut = k + (len(pat) if pat != ' ' else 0)
            if cut < maxn // 3:
                cut = maxn
            out.append(rest[:cut])
            rest = rest[cut:]
        return out

    # ---------- 页面 ----------
    def new_page(self):
        self._cur = []
        self.pages.append(self._cur)
        return self._cur

    def emit(self, s):
        if self._cur is None:
            self.new_page()
        self._cur.append(s)

    def draw(self, x, y, text, size=FS_BODY, gray=0.0):
        data, _ = self.enc(text, size)
        if not data:
            return
        self.emit(f"BT {gray:.3f} g /{self.font_name} {size:.2f} Tf "
                  f"1 0 0 1 {x:.2f} {y:.2f} Tm <{data.hex()}> Tj ET")

    def rect(self, x, y, w, h, gray=0.9):
        self.emit(f"{gray:.3f} g {x:.2f} {y:.2f} {w:.2f} {h:.2f} re f")

    # ---------- 主渲染 ----------
    def render(self, blocks, title):
        y = PAGE_H - MARGIN_T
        self.new_page()

        def need(h):
            nonlocal y
            if y - h < MARGIN_B:
                self.new_page()
                y = PAGE_H - MARGIN_T

        for kind, payload in blocks:
            if kind == 'hr':
                need(12)
                self.emit(f"0.7 g {MARGIN_L:.2f} {y:.2f} m "
                          f"{PAGE_W - MARGIN_R:.2f} {y:.2f} l S")
                y -= 10
                continue

            if kind == 'h1':
                need(FS_H1 + 26)
                y -= 6
                self.draw(MARGIN_L, y, strip_inline(payload), FS_H1, 0.0)
                y -= 6
                self.emit(f"0.25 g {MARGIN_L:.2f} {y:.2f} m "
                          f"{PAGE_W - MARGIN_R:.2f} {y:.2f} l S")
                y -= FS_H1 + 8
                continue

            if kind == 'h2':
                need(FS_H2 + 18)
                y -= 8
                self.draw(MARGIN_L, y, strip_inline(payload), FS_H2, 0.05)
                y -= FS_H2 + 7
                continue

            if kind == 'h3':
                need(FS_H3 + 14)
                y -= 5
                self.draw(MARGIN_L, y, strip_inline(payload), FS_H3, 0.15)
                y -= FS_H3 + 5
                continue

            if kind == 'p':
                for line in self.wrap(strip_inline(payload), FS_BODY, CONTENT_W):
                    need(LEAD_BODY)
                    self.draw(MARGIN_L, y, line, FS_BODY, 0.0)
                    y -= LEAD_BODY
                y -= 3
                continue

            if kind == 'li':
                for item in payload:
                    txt = strip_inline(item)
                    lines = self.wrap(txt, FS_BODY, CONTENT_W - 14)
                    for k, line in enumerate(lines):
                        need(LEAD_BODY)
                        if k == 0:
                            self.draw(MARGIN_L + 3, y, '•', FS_BODY, 0.0)
                        self.draw(MARGIN_L + 14, y, line, FS_BODY, 0.0)
                        y -= LEAD_BODY
                y -= 3
                continue

            if kind == 'quote':
                txt = strip_inline(payload)
                lines = self.wrap(txt, FS_BODY, CONTENT_W - 16)
                h = len(lines) * LEAD_BODY + 6
                need(h)
                self.rect(MARGIN_L, y - h + LEAD_BODY - 2, CONTENT_W, h, 0.94)
                yy = y
                for line in lines:
                    self.draw(MARGIN_L + 8, yy, line, FS_BODY, 0.2)
                    yy -= LEAD_BODY
                y = yy - 5
                continue

            if kind == 'code':
                # 代码块按可用宽度折行（等宽近似：每字符 fs*0.6pt）
                wrapped = []
                for line in payload:
                    wrapped.extend(self.wrap_code(line, FS_CODE, CONTENT_W - 10))
                need(len(wrapped) * LEAD_CODE + 8)
                for line in wrapped:
                    need(LEAD_CODE)
                    self.draw(MARGIN_L + 4, y, line, FS_CODE, 0.1)
                    y -= LEAD_CODE
                y -= 5
                continue

            if kind == 'table':
                y = self.render_table(payload, y, need)
                continue

        return self.pages

    def render_table(self, rows, y, need):
        if not rows:
            return y
        ncol = max(len(r) for r in rows)
        rows = [r + [''] * (ncol - len(r)) for r in rows]
        # 列宽：按内容长度加权，但用「渲染后的实际文本宽度」而非字符数，
        # 并对标签型窄表（2~3 列）显式保证首列不被压得过窄。
        weights = []
        for c in range(ncol):
            mx = 0.0
            for r in rows:
                for line in self.wrap(strip_inline(r[c]), FS_TABLE, 10000):
                    mx = max(mx, self.text_width(line, FS_TABLE))
            weights.append(max(mx, 18.0))
        tot = sum(weights)
        widths = [CONTENT_W * w / tot for w in weights]
        if ncol == 2:
            # 形如「项目 | 内容」的元数据表：首列给足标签宽度
            w0 = max(self.text_width(strip_inline(r[0]), FS_TABLE) for r in rows) + 14
            w0 = min(max(w0, 70.0), CONTENT_W * 0.32)
            widths = [w0, CONTENT_W - w0]
        elif ncol == 3:
            w0 = max(self.text_width(strip_inline(r[0]), FS_TABLE) for r in rows) + 14
            w0 = min(max(w0, 62.0), CONTENT_W * 0.26)
            rest = CONTENT_W - w0
            t2 = weights[1] + weights[2]
            widths = [w0, rest * weights[1] / t2, rest * weights[2] / t2]
        fs = FS_TABLE if ncol <= 6 else FS_TABLE - 1.2

        for ri, row in enumerate(rows):
            # 计算本行高
            cell_lines = []
            for c in range(ncol):
                t = strip_inline(row[c])
                cell_lines.append(self.wrap(t, fs, widths[c] - 8))
            h = max(len(cl) for cl in cell_lines) * LEAD_TABLE + 5
            need(h)
            if ri == 0:
                self.rect(MARGIN_L, y - h + LEAD_TABLE - 3, CONTENT_W, h, 0.88)
            elif ri % 2 == 0:
                self.rect(MARGIN_L, y - h + LEAD_TABLE - 3, CONTENT_W, h, 0.965)
            x = MARGIN_L
            for c in range(ncol):
                yy = y
                for line in cell_lines[c]:
                    self.draw(x + 4, yy, line, fs, 0.05 if ri == 0 else 0.0)
                    yy -= LEAD_TABLE
                x += widths[c]
            y -= h
            self.emit(f"0.82 g {MARGIN_L:.2f} {y:.2f} m "
                      f"{PAGE_W - MARGIN_R:.2f} {y:.2f} l S")
        return y - 6

    # ---------- 输出 ----------
    def save(self, path):
        if self.SUBSET:
            subset_font = self.ttf.build_subset_font(sorted(self.used_gids))
            n = self.ttf.numGlyphs          # 保留原始 GID 数量
            font_program = zlib.compress(subset_font)
            length1 = len(subset_font)
            cmap_stream = zlib.compress(b''.join(struct.pack('>H', i) for i in range(n)))
            remap = {g: g for g in range(n)}
        else:
            # 嵌入完整字体：保留原始表顺序与原始 GID，CID == GID（Identity）。
            raw = self.ttf.data
            font_program = zlib.compress(raw)
            length1 = len(raw)
            n = self.ttf.numGlyphs
            cmap_stream = zlib.compress(b''.join(struct.pack('>H', i) for i in range(n)))
            remap = {g: g for g in range(n)}

        objs = {}

        def add(num, body):
            objs[num] = body

        # 1 Catalog / 2 Pages / 3 Font / 4 DescendantFont / 5 Descriptor
        # 6 FontFile2 / 7 CIDToGIDMap / 8 Encoder(ToUnicode)
        page_ids = []
        next_id = 20
        for p in self.pages:
            page_ids.append(next_id)
            next_id += 1

        kids = ' '.join(f'{i} 0 R' for i in page_ids)
        add(1, f'<< /Type /Catalog /Pages 2 0 R >>')
        add(2, f'<< /Type /Pages /Count {len(page_ids)} /Kids [{kids}] >>')

        scale = 1000.0 / self.ttf.unitsPerEm
        add(3, f'''<< /Type /Font /Subtype /Type0 /BaseFont /{self.font_name}
 /Encoding /Identity-H /DescendantFonts [4 0 R] /ToUnicode 8 0 R >>''')
        add(4, f'''<< /Type /Font /Subtype /CIDFontType2 /BaseFont /{self.font_name}
 /CIDSystemInfo << /Registry (Adobe) /Ordering (Identity) /Supplement 0 >>
 /FontDescriptor 5 0 R /DW 1000 /W [{self._w_array(remap, scale)}]
 /CIDToGIDMap 7 0 R >>''')
        add(5, f'''<< /Type /FontDescriptor /FontName /{self.font_name} /Flags 4
 /FontBBox [{int(self.ttf.xMin*scale)} {int(self.ttf.yMin*scale)} {int(self.ttf.xMax*scale)} {int(self.ttf.yMax*scale)}]
 /ItalicAngle 0 /Ascent {int(self.ttf.ascent*scale)} /Descent {int(self.ttf.descent*scale)}
 /CapHeight {int(self.ttf.ascent*scale)} /StemV 80 /FontFile2 6 0 R >>''')
        add(6, ('stream', font_program, f'/Length1 {length1}'))
        add(7, ('stream', cmap_stream, ''))

        # ToUnicode CMap
        tounicode = self._tounicode(remap)
        add(8, ('stream', zlib.compress(tounicode.encode('latin1')), ''))

        for idx, pid in enumerate(page_ids):
            content = '\n'.join(self.pages[idx]).encode('latin1')
            cs = zlib.compress(content)
            add(pid, ('stream', cs, ''))
            add(pid + 1000, f'''<< /Type /Page /Parent 2 0 R
 /MediaBox [0 0 {PAGE_W:.2f} {PAGE_H:.2f}]
 /Resources << /Font << /{self.font_name} 3 0 R >> >>
 /Contents {pid} 0 R >>''')

        # 重新指向：Pages 的 Kids 要用 page 对象号
        kids2 = ' '.join(f'{i+1000} 0 R' for i in page_ids)
        add(2, f'<< /Type /Pages /Count {len(page_ids)} /Kids [{kids2}] >>')

        self._write(path, objs)

    def _w_array(self, remap, scale):
        """W 数组：每个 CID 的宽度。"""
        parts = []
        inv = {v: k for k, v in remap.items()}
        cids = sorted(inv.keys())
        for c in cids:
            g = inv[c]
            adv = self.ttf.advances[g] if g < len(self.ttf.advances) else 1000
            parts.append(f'{c} [{int(adv*scale)}]')
        return ' '.join(parts)

    def _tounicode(self, remap):
        lines = ['/CIDInit /ProcSet findresource begin', '12 dict begin', 'begincmap',
                 '/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def',
                 '/CMapName /Adobe-Identity-UCS def', '/CMapType 2 def',
                 '1 begincodespacerange', '<0000> <FFFF>', 'endcodespacerange']
        # 依据编码阶段收集的 char->原始 gid 映射，换算成 CID(=子集序号) -> Unicode
        pairs = []
        for ch, g in self._char_gid.items():
            cid = remap.get(g)
            if cid is None:
                continue
            pairs.append((cid, ord(ch)))
        pairs.sort()
        for i in range(0, len(pairs), 100):
            chunk = pairs[i:i + 100]
            lines.append(f'{len(chunk)} beginbfchar')
            for cid, cp in chunk:
                if cp > 0xFFFF:
                    hi = 0xD800 + ((cp - 0x10000) >> 10)
                    lo = 0xDC00 + ((cp - 0x10000) & 0x3FF)
                    lines.append(f'<{cid:04X}> <{hi:04X}{lo:04X}>')
                else:
                    lines.append(f'<{cid:04X}> <{cp:04X}>')
            lines.append('endbfchar')
        lines += ['endcmap', 'CMapName currentdict /CMap defineresource pop', 'end', 'end']
        return '\n'.join(lines)

    def _write(self, path, objs):
        # 关联 char->gid 映射（供 ToUnicode）
        buf = bytearray(b'%PDF-1.4\n%\xe2\xe3\xcf\xd3\n')
        offsets = {}
        nums = sorted(objs.keys())
        for num in nums:
            body = objs[num]
            offsets[num] = len(buf)
            if isinstance(body, tuple) and body[0] == 'stream':
                _, data, extra = body
                head = (f'{num} 0 obj\n<< /Length {len(data)} /Filter /FlateDecode {extra} >>\n'
                        f'stream\n').encode('latin1')
                buf += head + data + b'\nendstream\nendobj\n'
            else:
                buf += f'{num} 0 obj\n{body}\nendobj\n'.encode('latin1')
        xref = len(buf)
        maxn = max(nums) + 1
        buf += f'xref\n0 {maxn}\n'.encode('latin1')
        buf += b'0000000000 65535 f \n'
        for i in range(1, maxn):
            if i in offsets:
                buf += f'{offsets[i]:010d} 00000 n \n'.encode('latin1')
            else:
                buf += b'0000000000 65535 f \n'
        buf += (f'trailer\n<< /Size {maxn} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n').encode('latin1')
        open(path, 'wb').write(buf)
        return len(buf)


def main():
    if len(sys.argv) < 4:
        print(__doc__)
        sys.exit(1)
    src, dst, font = sys.argv[1], sys.argv[2], sys.argv[3]
    md = open(src, encoding='utf-8').read()
    blocks = parse_markdown(md)
    pdf = PDF(font)
    # 记录 char->gid 供 ToUnicode 使用
    orig_enc = pdf.enc

    def enc2(text, size, bold=False):
        for ch in text:
            pdf._char_gid[ch] = pdf.ttf.gid(ord(ch))
        return orig_enc(text, size, bold)

    pdf.enc = enc2
    pdf.render(blocks, '')
    size = pdf.save(dst)
    print(f'  ✅ PDF 已生成: {dst}  ({size} 字节, {len(pdf.pages)} 页, 使用字形 {len(pdf.used_gids)} 个)')


if __name__ == '__main__':
    main()
