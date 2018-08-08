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

const UUID_REGEX_STR = [
  '(00000000-0000-0000-0000-000000000000)',
  '([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89aAbB][a-fA-F0-9]{3}-[a-fA-F0-9]{12})'
].join('|')

const UUID_REGEX = new RegExp(`^${UUID_REGEX_STR}$`)

const isUUID = s => UUID_REGEX.test(s)

const dehyphenUUID = uuid =>
  !f.isString(uuid) ? undefined : uuid.split('-').join('')

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
  isUUID,
  dehyphenUUID,
  enhyphenUUID,
  UUID_REGEX
}

export default mixins
// FOR USAGE IN CONSOLE: f.mixin(mixins)

// docs & tests…
if (process.env.NODE_ENV !== 'production') {
  const THROWING_FN = () => {
    throw new Error('WTF')
  }
  assert.strictEqual(lodash_try(() => 23), 23)
  assert.strictEqual((() => lodash_try(THROWING_FN))(), undefined)
  assert.doesNotThrow(() => lodash_try(THROWING_FN))

  assert.strictEqual(present({ a: 1 }), true)
  assert.strictEqual(present([1]), true)
  assert.strictEqual(present(true), true)
  assert.strictEqual(present(false), true)
  assert.strictEqual(present(function() {}), true)
  assert.strictEqual(present({}), false)
  assert.strictEqual(present([]), false)
  assert.strictEqual(present(undefined), false)
  assert.strictEqual(present(null), false)

  assert.strictEqual(presence(23) || 42, 23)
  assert.strictEqual(presence(null) || 42, 42)

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

  assert.strictEqual(
    dehyphenUUID('2ea39047-e663-50d5-9080-838b75883704'),
    '2ea39047e66350d59080838b75883704'
  )
  assert.strictEqual(dehyphenUUID(), undefined)

  assert.strictEqual(
    enhyphenUUID('2ea39047e66350d59080838b75883704'),
    '2ea39047-e663-50d5-9080-838b75883704'
  )
  assert.strictEqual(enhyphenUUID(), undefined)
}
