import f from 'lodash'

const uniqBy = (arr, key) =>
  f.uniqWith(arr, (a, b) => f.isEqual(f.get(a, key), f.get(b, key)))

export default {
  uniqBy,
  uniqById: arr => uniqBy(arr, 'id')
}
