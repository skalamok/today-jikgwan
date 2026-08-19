/**
 * REQ-F-204 사진에서 촬영 일시만 읽는다.
 *
 * 서버는 업로드 즉시 위치를 포함한 메타데이터를 지운다(REQ-NF-007). 그래서 촬영 시각도
 * 서버에서는 읽을 수 없다. 브라우저에서 지우기 전에 읽어 경기 후보를 고르는 데만 쓴다.
 *
 * 라이브러리를 넣지 않고 직접 읽는다. 필요한 것은 DateTimeOriginal 하나뿐이라
 * 의존성을 더할 값어치가 없다.
 */

/** JPEG 의 APP1(Exif) 구간만 훑어 DateTimeOriginal 을 찾는다 */
function findExifDate(view) {
  if (view.getUint16(0, false) !== 0xffd8) return null      // SOI 가 아니면 JPEG 이 아니다

  let offset = 2
  while (offset < view.byteLength) {
    if (view.getUint8(offset) !== 0xff) return null
    const marker = view.getUint8(offset + 1)
    const size = view.getUint16(offset + 2, false)

    if (marker === 0xe1) {                                   // APP1
      const exif = offset + 4
      if (view.getUint32(exif, false) !== 0x45786966) return null   // "Exif"
      const tiff = exif + 6
      const little = view.getUint16(tiff, false) === 0x4949
      const ifd0 = tiff + view.getUint32(tiff + 4, little)
      const found = readIfd(view, tiff, ifd0, little)
      if (found) return found
      return null
    }
    if (marker === 0xda) return null                         // 이미지 데이터 시작. 더 볼 것이 없다
    offset += 2 + size
  }
  return null
}

/** IFD 를 훑는다. ExifIFDPointer(0x8769) 를 만나면 그 안까지 들어간다 */
function readIfd(view, tiff, dir, little, depth = 0) {
  if (depth > 2) return null
  const count = view.getUint16(dir, little)
  for (let i = 0; i < count; i++) {
    const entry = dir + 2 + i * 12
    const tag = view.getUint16(entry, little)
    if (tag === 0x9003 || tag === 0x0132) {                  // DateTimeOriginal · DateTime
      const len = view.getUint32(entry + 4, little)
      const at = tiff + view.getUint32(entry + 8, little)
      let s = ''
      for (let j = 0; j < len - 1; j++) s += String.fromCharCode(view.getUint8(at + j))
      return s
    }
    if (tag === 0x8769) {
      const sub = readIfd(view, tiff, tiff + view.getUint32(entry + 8, little), little, depth + 1)
      if (sub) return sub
    }
  }
  return null
}

/**
 * 파일에서 촬영 일시를 읽어 ISO 문자열로 돌려준다. 없으면 null.
 * EXIF 는 "2026:08:18 21:40:00" 꼴이라 앞의 콜론만 바꿔 준다.
 */
export async function readTakenAt(file) {
  if (!file || !/jpe?g$/i.test(file.type.split('/')[1] || '')) return null
  try {
    const head = await file.slice(0, 128 * 1024).arrayBuffer()   // 헤더만 읽으면 된다
    const raw = findExifDate(new DataView(head))
    if (!raw) return null
    const m = raw.match(/^(\d{4}):(\d{2}):(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/)
    if (!m) return null
    return `${m[1]}-${m[2]}-${m[3]}T${m[4]}:${m[5]}:${m[6]}`
  } catch {
    return null                                              // 못 읽으면 수동 선택으로 넘어간다
  }
}
