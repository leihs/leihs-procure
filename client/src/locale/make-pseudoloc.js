// NOTE: https://en.wikipedia.org/wiki/Pseudolocalization

const f = require('lodash')
const fs = require('fs')
const path = require('path')
const pseudoloc = require('pseudoloc')

const orgLang = require('./de.json');

// pseudoloc.option.delimiter = null

function convertString(str, prepend = '[!!', append = '!!]', override) {
  pseudoloc.option.prepend = prepend
  pseudoloc.option.append = append
  pseudoloc.option.override = override
  return pseudoloc.str(str)
}

const recurseObj = (arg, fn) =>
  f.fromPairs(
    f.map(arg, (v, k) => [k, !f.isObject(v) ? fn(v) : recurseObj(v, fn)])
  )

function makePseudoloc(translations, filename, type = 'zalgo') {
  if (!f.includes(['zalgo', 'empty'], type))
    throw Error('invalid type: ' + type)

  const pseudoLang = recurseObj(orgLang, (v, k) =>
    !f.isString(v)
      ? v
      : type === 'empty'
        ? convertString(v, '', '', '_')
        : convertString(v)
  )

  fs.writeFileSync(
    path.join(path.dirname(__filename), filename),
    JSON.stringify(pseudoLang, 0, 2)
  )
}

makePseudoloc(orgLang, 'xx.json', 'empty')
makePseudoloc(orgLang, 'zz.json', 'zalgo')
