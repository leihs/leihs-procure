import f from 'lodash'

const uniqBy = (arr, key) =>
  f.uniqWith(arr, (a, b) => f.isEqual(f.get(a, key), f.get(b, key)))

const dehyphenUUID = uuid =>
  String(uuid)
    .split('-')
    .join('')

const enhyphenUUID = s =>
  [
    s.slice(0, 8),
    s.slice(8, 12),
    s.slice(12, 16),
    s.slice(16, 20),
    s.slice(20)
  ].join('-')

export default {
  uniqBy,
  uniqById: arr => uniqBy(arr, 'id'),
  dehyphenUUID,
  enhyphenUUID
}
