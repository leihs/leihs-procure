import f from 'lodash'
import { isDev } from '../env'

// real langs
import en from './en.json'
import de from './de.json'

// dev langs
import xx from './xx.json'
import zz from './zz.json'
// dev lang stuff
const DEV_LANGS = isDev ? { xx, zz } : null
const store = window.sessionStorage
const SELECTED_LANG = store.getItem('LEIHS_DEV_FAKE_USER_LANG')
const fakeLang = l => {
  store.setItem('LEIHS_DEV_FAKE_USER_LANG', l)
  window.location.reload()
}
window && (window.fakeLang = fakeLang)

const LANGS = { en, de, ...DEV_LANGS }
const DEFAULT_LANG = 'de'

const translate = key => {
  const translations = LANGS[SELECTED_LANG] || LANGS[DEFAULT_LANG]
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
