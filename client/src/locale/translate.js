import f from 'lodash'

import translations from './translations.json'

const translate = key => {
  const fallback = `⟪${key}⟫`
  // 'foo.bar.baz' => [ 'foo.bar.baz', 'bar.baz', 'baz' ]
  const paths = key.split('.').map((i, n, a) => a.slice(n).join('.'))
  const results = paths
    .map(k => f.get(translations, k))
    .concat([fallback])
    .filter(r => f.isString(r) || f.isNumber(r))
  return f.first(results)
}
export default translate
