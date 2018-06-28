import f from 'lodash'
import assert from 'assert'

const lodash_try = fn => {
  try {
    return fn()
  } catch (e) {
    // ignore errors
  }
}

const present = val => {
  return (
    // do what the coffeescript `?` operator compiles to
    typeof val !== 'undefined' &&
    val !== null &&
    // AND (not "isEmpty" OR a primitive type)
    (!f.isEmpty(val) ||
      f.isNumber(val) ||
      f.isBoolean(val) ||
      f.isFunction(val))
  )
}

const presence = val => {
  if (present(val)) {
    return val
  }
}

const uniqBy = (arr, key) =>
  f.uniqWith(arr, (a, b) => f.isEqual(f.get(a, key), f.get(b, key)))

const uniqById = arr => uniqBy(arr, 'id')

const dehyphenUUID = uuid =>
  String(uuid)
    .split('-')
    .join('')

const enhyphenUUID = s => {
  if (f.isString(s))
    return [8, 12, 16, 20, 32]
      .reduce((m, i, n, a) => [...m, s.slice(a[n - 1], a[n])], [])
      .join('-')
}

const mixins = {
  try: lodash_try,
  present,
  presence,
  uniqBy,
  uniqById,
  dehyphenUUID,
  enhyphenUUID
}

export default mixins
// FOR USAGE IN CONSOLE: f.mixin(mixins)

// docs & tests…
if (process.env.NODE_ENV !== 'production') {
  const THROWING_FN = () => {
    throw new Error('WTF')
  }
  assert.equal(lodash_try(() => 23), 23)
  assert.strictEqual((() => lodash_try(THROWING_FN))(), undefined)
  assert.doesNotThrow(() => lodash_try(THROWING_FN))

  assert.equal(present({ a: 1 }), true)
  assert.equal(present([1]), true)
  assert.equal(present(true), true)
  assert.equal(present(false), true)
  assert.equal(present(function() {}), true)
  assert.equal(present({}), false)
  assert.equal(present([]), false)
  assert.equal(present(undefined), false)
  assert.equal(present(null), false)

  assert.equal(presence(23) || 42, 23)
  assert.equal(presence(null) || 42, 42)

  assert.deepEqual(
    uniqBy(
      [{ n: 1, name: 'one' }, { n: 2, name: 'two' }, { n: 1, name: 'eins' }],
      'n'
    ),
    [{ n: 1, name: 'one' }, { n: 2, name: 'two' }]
  )

  assert.deepEqual(
    uniqById([
      { id: 1, name: 'one' },
      { id: 2, name: 'two' },
      { id: 1, name: 'eins' }
    ]),
    [{ id: 1, name: 'one' }, { id: 2, name: 'two' }]
  )

  assert.equal(
    dehyphenUUID('2ea39047-e663-50d5-9080-838b75883704'),
    '2ea39047e66350d59080838b75883704'
  )

  assert.equal(
    enhyphenUUID('2ea39047e66350d59080838b75883704'),
    '2ea39047-e663-50d5-9080-838b75883704'
  )
}
